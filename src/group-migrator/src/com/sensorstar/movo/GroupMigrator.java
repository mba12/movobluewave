package com.sensorstar.movo;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.firebase.client.*;
import com.firebase.client.Firebase.AuthResultHandler;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import org.apache.log4j.Logger;


public class GroupMigrator {

	final static Logger logger = Logger.getLogger(GroupMigrator.class);

	
	/* Time between saving checkpoint time and checking for new users */
	private static long CHECKPOINT_INTERVAL = 36*1000;  
	
	/* Sensorstar Local Debug Defaults */
//	private static final String FB_URL = "https://ss-movo-wave-v2.firebaseio.com";
//	private static final String FB_SECRET = "jBMdrOwNCfJ37NzcXt6IM4d7AddeojCJg2Z9KnuF";
//	private static final String DB_URL = "jdbc:mysql://localhost:3306/movogroups?user=root";
//	private static final boolean USING_GAE_SQL = false;
	
	/* Debug Defaults */
	// private static String FB_URL = "https://movowave-debug.firebaseio.com/";
	// private static String FB_SECRET = "3HFJlhjThUhC9QrP4zAq4PNcaXH8IWYqM8cCWmnR";

	// private static String DB_URL = "jdbc:mysql://173.194.247.177:3306/movogroups?user=root&useSSL=true";
	private static final boolean USING_GAE_SQL = true;
	
	/* Production Defaults */
    private static String FB_URL = "https://movowave.firebaseio.com/";
    private static String FB_SECRET = "0paTj5f0KHzLBnwIyuc1eEvq4tXZ3Eik9Joqrods";
	// private static String DB_URL = "jdbc:mysql://173.194.247.177:3306/movogroups?user=root&useSSL=true"; // prod ?
	private static String DB_URL = "jdbc:mysql://173.194.239.157:3306/movogroups?user=root&useSSL=true";  // test
//	static String keyStorePassword = "r87p-Y?72*uXqW$aSZGU"; // keystore
//	static String trustStorePassword = "^59nhzfX@8!VPbuKMA=V";  // truststore
	static String keyStorePassword = "movomovo"; // keystore
	static String trustStorePassword = "movomovo";  // truststore
	private static String username = "movogroups";
	private static String password = "H8$E=?3*ADXFt4Ld7-jw";

	// private static String keyStorePassword = "keystore"; // keystore
	// private static String trustStorePassword = "truststore";  // truststore

	//	private Map<String,String> latest_sync_for_users;
	private String checkpoint;
	private String most_recent_sync;

	private Set<String> users;
	
	
	GroupMigrator(){

		InputStream file;
		try {//try to load user map
			file = new FileInputStream("checkpoint.ser");
			InputStream buffer = new BufferedInputStream(file);
		    ObjectInput input = new ObjectInputStream (buffer);
		    //latest_sync_for_users = (HashMap<String,String>)input.readObject();
		    
		    checkpoint = (String)input.readObject();
		    input.close();
		    
		    logger.info("Loading checkpoint time:" + checkpoint);
		    
		} catch (IOException | ClassNotFoundException e) { //assume it is our first run and make a new one
			//latest_sync_for_users = new HashMap<String,String>();
			logger.info("No checkpoint found - starting from beginning.");
			checkpoint = "0000-00-00T00:00:00Z";
			
		}
	      
		users = new HashSet<String>(); 
//		most_recent_sync = "0000-00-00T00:00:00Z";
		most_recent_sync = checkpoint;
	}
	
	public GroupMigrator(String checkpoint_option_val) {
		checkpoint = checkpoint_option_val;
		users = new HashSet<String>(); 
//		most_recent_sync = "0000-00-00T00:00:00Z";
		most_recent_sync = checkpoint;		
		logger.info("Loading checkpoint time provided by user:" + checkpoint);

	}

	public void update(){
		/* save checkpoint time */
		try(
			OutputStream file = new FileOutputStream("checkpoint.ser");
		    OutputStream buffer = new BufferedOutputStream(file);
		    ObjectOutput output = new ObjectOutputStream(buffer);
	    ){
			logger.info("Saving checkpoint time: " + checkpoint );
			logger.info("Next checkpoint time: " + most_recent_sync );
			synchronized(checkpoint){
				output.writeObject(checkpoint);
				
				synchronized(most_recent_sync){
					checkpoint = most_recent_sync;
				}
			}
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		/* update our user list and find new users */
		Set<String> updated_users = getUsers();
		Set<String> new_users = new HashSet<String>(updated_users);
		new_users.removeAll(users);
		users = updated_users;
		
		logger.info("New Users: " + new_users );
		
		/* add listeners for new users */ 
		addListenersToUsers(new_users);
		
	}

	/**
	 * This is a workaround since the current firebase setup is normalized on user_id and only the REST implementation supports shallow queries  
	 * @return a set of users
	 */
	private Set<String> getUsers(){
		Set<String> users = new HashSet<String>();
		
		Client client = Client.create();

		WebResource webResource = client
		   .resource(FB_URL+"/users.json?auth="+FB_SECRET+"&shallow=true");

		ClientResponse response = webResource.accept("application/json")
                   .get(ClientResponse.class);

		if (response.getStatus() != 200) {
		   throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}

		String output = response.getEntity(String.class);

		//logger.info("Output from Server .... \n");
		//logger.info(output);
		
		try {
			JSONObject obj = new JSONObject(output);
			JSONArray arr  = obj.names();
			for (int i = 0; i < arr.length(); i++){
				users.add(arr.get(i).toString());
			}

		} catch (JSONException e) { e.printStackTrace(); }
		
		return users;
		
	}
		
	private static class StepInterval{

		public String sync_end_time; // used to  
		public String firebase_id_fk;
		public int year;
		public int month;
		public int day;
		public int hour;
		public int start_minute;
		public int end_minute;
		public int steps;
		public String device_id;
		public String sync_id;
		
		public String toString(){
			return "StepInterval(FbID: "+firebase_id_fk +"\t"+ year + "-" + month+ "-" + day+ "\thour:" + hour+ "\tSm:"+start_minute+ "\tEm: "+ end_minute +"\tSteps:   "+steps + "\tDID:"+device_id+ "\tSID: "+ sync_id+")"; 
		}
	}
	
	private void processSync(DataSnapshot sync){
		logger.debug("Processing Sync");
		
		List<StepInterval> steps_synced = new ArrayList<StepInterval>();
		
		String sync_end_time = (String)sync.child("endtime").getValue();
		
		synchronized(most_recent_sync){
			if(most_recent_sync.compareTo(sync_end_time) < 0){
				most_recent_sync = sync_end_time;
			}
		}

		
		
		/* Get User ID from DataSnapshot without traversing fb nodes*/ 
		int user_start_idx = FB_URL.length() + "users/".length();
		String url_to_search = sync.getRef().toString();
		String user_id = url_to_search.substring(user_start_idx, url_to_search.length());
		user_id = user_id.substring(0, user_id.indexOf('/'));

		Iterable<DataSnapshot> years = sync.child("steps").getChildren();
		for(DataSnapshot year : years){
			
			Iterable<DataSnapshot> months = year.getChildren();
			for(DataSnapshot month : months){
				
				Iterable<DataSnapshot> days = month.getChildren();
				for(DataSnapshot day : days){
					
					Iterable<DataSnapshot> times = day.getChildren();
					for(DataSnapshot time : times){
						
						Iterable<DataSnapshot> steps = time.getChildren();
						for(DataSnapshot step_data : steps){
							StepInterval si = new StepInterval();
							
							si.sync_end_time = sync_end_time;
							si.firebase_id_fk = user_id; 
							si.year = Integer.parseInt(year.getKey());
							si.month = Integer.parseInt(month.getKey());
							si.day = Integer.parseInt(day.getKey());

							si.steps = Integer.parseInt((String) step_data.child("count").getValue());
							si.device_id = (String) step_data.child("deviceid").getValue();
							si.sync_id = (String) step_data.child("syncid").getValue();
							
							SimpleDateFormat time_parser = new SimpleDateFormat("'T'HH:mm:ss'Z'");
							try {
								
								String start_time_val = (String) step_data.child("starttime").getValue();
								Calendar step_start_date = Calendar.getInstance();
								step_start_date.setTime(time_parser.parse(start_time_val));
								si.hour = step_start_date.get(Calendar.HOUR_OF_DAY);
								si.start_minute = step_start_date.get(Calendar.MINUTE);
								
								String end_time_val = (String) step_data.child("endtime").getValue();
								Calendar step_end_date = Calendar.getInstance();
								step_end_date.setTime(time_parser.parse(end_time_val));
								si.end_minute = step_end_date.get(Calendar.MINUTE);
								
							} catch (ParseException e) { e.printStackTrace(); }
							
							
							logger.debug(si);
							steps_synced.add(si);

						}					
					}
				}
			}
			
		}
		
		logger.debug("This Sync has: "+steps_synced.size()+" step intervals\n");

		try {
			if(steps_synced.size() !=0)
				addSyncToDb(steps_synced);
		} catch (SQLException e) {e.printStackTrace();}
	}
	
	private static final String query = " insert ignore into steps (firebase_id_fk, full_date, full_date_str, year, month, day, hour, start_minute, end_minute, steps, device_id, sync_id, created_ts, updated_ts)"
	        + " values (?, ?, ?, ?, ?, ? , ? , ? , ? , ? , ? , ? ,?, ?)";
	private void addSyncToDb(List<StepInterval> stepIntervals) throws SQLException{
		logger.debug("Adding Sync to DB");
		Connection conn = DriverManager.getConnection(DB_URL);
		
		for(StepInterval si: stepIntervals){
			
			//Calendar calendar = Calendar.getInstance();	
			//calendar.set(Calendar.YEAR,  		si.year);
			//calendar.set(Calendar.MONTH, 		si.month);
			//calendar.set(Calendar.DAY_OF_MONTH, si.day);
			//calendar.set(Calendar.HOUR_OF_DAY, 	si.hour);
			//calendar.set(Calendar.MINUTE, 		si.start_minute);
			//java.sql.Timestamp startDate = new java.sql.Timestamp(calendar.getTime().getTime());
			
			
			//Build  full_date_str
			//SimpleDateFormat time_parser = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
			//String full_date_time_string = time_parser.format(calendar.getTime());
			
			SimpleDateFormat time_parser = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss'Z'");
			java.sql.Timestamp sync_end_time = null;
			try {
				sync_end_time = new java.sql.Timestamp(time_parser.parse(si.sync_end_time).getTime());
			} catch (ParseException e) {
				sync_end_time = new java.sql.Timestamp(0L); // shouldn't ever happen
				e.printStackTrace();
			}

			PreparedStatement preparedStmt = conn.prepareStatement(query);
			preparedStmt.setString (1, 	si.firebase_id_fk);
			//preparedStmt.setTimestamp(2, startDate);
			preparedStmt.setTimestamp(2, sync_end_time);
			
			//preparedStmt.setString(3, 	full_date_time_string);
			preparedStmt.setString(3, 	si.sync_end_time);
			preparedStmt.setInt(4, 		si.year);
			preparedStmt.setInt(5, 		si.month);
			preparedStmt.setInt(6, 		si.day);
			preparedStmt.setInt(7, 		si.hour);
			preparedStmt.setInt(8, 		si.start_minute);
			preparedStmt.setInt(9, 		si.end_minute);
			preparedStmt.setInt(10,		si.steps);
			preparedStmt.setString(11, 	si.device_id);
			preparedStmt.setString(12, 	si.sync_id);
			preparedStmt.setTimestamp(13, null);
			preparedStmt.setTimestamp(14, null);

			preparedStmt.execute();  
		}
		conn.close();
	}
	
	/**
	 * 
	 * @param userRef - reference 
	 * @param users - list of users to add listeners to
	 */
	private void addListenersToUsers(Set<String> users){
		
		for(String u: users){
			logger.debug("Adding listener to user: "+u);
			
			final Firebase userRef = new Firebase(FB_URL+"/users/"+u+ "/sync");
			logger.debug(userRef);
			userRef.authWithCustomToken(FB_SECRET, new AuthResultHandler() {
			    public void onAuthenticated(AuthData authData) { 
			    	logger.debug("Authenticated.");
			    	
			    	Query sync_query = userRef.orderByChild("endtime").startAt(checkpoint);;
					sync_query.addChildEventListener(new ChildEventListener() {
						
						// When there is a new sync add it to our SQL DB
						public void onChildAdded(DataSnapshot sync, String arg1) {
							processSync(sync);
						}
						
						public void onCancelled(FirebaseError arg0) {}
						public void onChildChanged(DataSnapshot arg0, String arg1) {}
						public void onChildMoved(DataSnapshot arg0, String arg1) {}
						public void onChildRemoved(DataSnapshot arg0) {}
					});
			    }
			    public void onAuthenticationError(FirebaseError firebaseError) { logger.error("Not Authenticated."); }
			});	
			
			
		}
	}
	
	
	public static void main(String[] args) throws org.apache.commons.cli.ParseException{
		/* setup command line options */
		Options options = new Options();
		options.addOption("checkpoint", 		true, 	"sets a checkpoint time to start looking for syncs (YYYY-MM-DDThh:dd:ssZ format). By default it looks for a checkpoint.ser file and if none is found it uses YYYY0000-00-00T00:00:00Z.");
		options.addOption("checkpointInterval", true, 	"Sets how often to checkpoint & look for new users (default is once per hour).");

		options.addOption("help",				false, 	"print this message" );
		options.addOption("useSSL",				false, 	"use SSL for MySQL (needed for GAE)" );
		options.addOption("keyStore", 			true, 	"sets where to look for our keyStore to access Firebase and GAE SQL instance. Default location is <current_location>/keystore.");
		options.addOption("keyStorePass",		true, 	"password for keystore" );
		options.addOption("trustStore", 		true, 	"sets where to look for our keyStore to access Firebase and GAE SQL instance. Default location is <current_location>/truststore.");
		options.addOption("trustStorePass",		true, 	"password for truststore" );
		
		options.addOption("FirebaseURL", 		true, 	"URL to connect to Firebase Instance. Default is:" + FB_URL);
		options.addOption("FirebaseSecret", 	true, 	"Auth Cookie to connect to Firebase Instance.");
		options.addOption("MysqlURL", 			true, 	"URL to connect to MySQL Server. Default is:" + DB_URL);

		
		
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse( options, args);

	    
	    if(cmd.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			
			String header = "Listens for new syncs from Firebase instance and inserts them into SQL database.\n\n";
			String footer = "\n";
			formatter.printHelp( "GroupMigrator", header, options, footer );
			return;
		}
	    
	    if(cmd.hasOption("checkpointInterval")) {
	    	CHECKPOINT_INTERVAL = Long.parseLong(cmd.getOptionValue("checkpointInterval"));
	    }
	    
		try {
			Class.forName("com.mysql.jdbc.Driver");
			
			if (cmd.hasOption("useSSL") || USING_GAE_SQL){ 
				//Class.forName("org.mariadb.jdbc.Driver"); 
				
				if (cmd.hasOption("keyStore")){
					System.setProperty("javax.net.ssl.keyStore",			cmd.getOptionValue("keyStore"));
				}else{
					System.setProperty("javax.net.ssl.keyStore",			System.getProperty("user.dir")+"/keystore");
				}
				
				if (cmd.hasOption("keyStorePass")){
					System.setProperty("javax.net.ssl.keyStorePassword",	cmd.getOptionValue("keyStorePass"));
				}else{
					// System.setProperty("javax.net.ssl.keyStorePassword",	"movomovo");
					System.setProperty("javax.net.ssl.keyStorePassword",	keyStorePassword);

				}
				
				if (cmd.hasOption("trustStore")){
					System.setProperty("javax.net.ssl.trustStore",			cmd.getOptionValue("trustStore"));
				}else{
					System.setProperty("javax.net.ssl.trustStore",			System.getProperty("user.dir")+"/truststore");
				}
				
				if (cmd.hasOption("trustStorePass")){
					System.setProperty("javax.net.ssl.trustStorePassword",	cmd.getOptionValue("trustStorePass"));
				}else{
					// System.setProperty("javax.net.ssl.trustStorePassword",	"movomovo");
					System.setProperty("javax.net.ssl.trustStorePassword",	trustStorePassword);

				}

				Class.forName("com.mysql.jdbc.Driver");

			}
		} catch (ClassNotFoundException e) {e.printStackTrace();}

		if (cmd.hasOption("FirebaseURL")){
			FB_URL = cmd.getOptionValue("FirebaseURL");
		}
		
		if (cmd.hasOption("FirebaseSecret")){
			FB_SECRET = cmd.getOptionValue("FirebaseSecret");
		}
		
		if (cmd.hasOption("MysqlURL")){
			DB_URL = cmd.getOptionValue("MysqlURL");
		}

		/* */
		GroupMigrator gm = null;
		if (cmd.hasOption("checkpoint")){
			gm = new GroupMigrator(cmd.getOptionValue("checkpoint")); 
		}else{
			gm = new GroupMigrator(); 
		}
		
		
		while(true){
			
			gm.update();
				 
			try {
				Thread.sleep(CHECKPOINT_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
		}
		
		
	}
}
