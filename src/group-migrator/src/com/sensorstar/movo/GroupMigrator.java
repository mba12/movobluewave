package com.sensorstar.movo;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Types;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Map;
import java.util.Iterator;

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

//import java.util.logging.ConsoleHandler;
//import java.util.logging.FileHandler;
//import java.util.logging.Handler;
//import java.util.logging.Level;
//import java.util.logging.Logger;


import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;


public class GroupMigrator implements Runnable{

//	final static Logger logger = Logger.getLogger("GM");

	/* Time between saving checkpoint time and checking for new users */
	private static long CHECKPOINT_INTERVAL = 36*1000;
	private static long CONNECTION_CREATED = 0;
	
	private static int SQL_BATCH_DELAY = 10;
	private static int SQL_BATCH_SIZE = 10;
	private static int SQL_MAX_BATCH_WAIT = 10*1000;// in Milliseconds
	private static int SQL_MAX_CONNECTION_RESET = 10*60*1000;// in Milliseconds

	
	
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

	private static String DB_URL = "jdbc:mysql://173.194.239.157:3306/movogroups?useSSL=true&requireSSL=true";

	// private static String DB_URL = "jdbc:mysql://173.194.241.127:3306/movogroups?useSSL=true&requireSSL=true";

	// test
	static String keyStorePassword = "r87p-Y?72*uXqW$aSZGU"; // keystore
	static String trustStorePassword = "r87p-Y?72*uXqW$aSZGU";  // truststore
	private static String username = "movogroups";
	private static String password = "H8$E=?3*ADXFt4Ld7-jw";

	// private static String keyStorePassword = "keystore"; // keystore
	// private static String trustStorePassword = "truststore";  // truststore

	//	private Map<String,String> latest_sync_for_users;
	private String checkpoint;
	private String most_recent_sync;

	private ConcurrentLinkedQueue<StepInterval> sql_message_queue;
	
	private Set<String> users;
	
	private void loadOrCreateQueue(){
				
		InputStream file;
		try {//try to load user map
			file = new FileInputStream("sql_message_queue.ser");
			InputStream buffer = new BufferedInputStream(file);
		    ObjectInput input = new ObjectInputStream (buffer);
		    
		    sql_message_queue = (ConcurrentLinkedQueue<StepInterval>)input.readObject();
		    input.close();
		    
		    System.out.println("Loaded queue of size:" + sql_message_queue.size());
		    
		} catch (IOException | ClassNotFoundException e) { //assume it is our first run and make a new one
			System.out.println("No sql found - creating new one.");
			sql_message_queue = new ConcurrentLinkedQueue<StepInterval>();			
		}
	      
	}
	
	private void saveQueue(){
		try(
				OutputStream file = new FileOutputStream("sql_message_queue.ser");
			    OutputStream buffer = new BufferedOutputStream(file);
			    ObjectOutput output = new ObjectOutputStream(buffer);
		    ){
				System.out.println("Saving queue of size: " + sql_message_queue.size());
				synchronized(sql_message_queue){
					output.writeObject(sql_message_queue);
				}
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	GroupMigrator(){

		InputStream file;
		try {//try to load user map
			file = new FileInputStream("checkpoint.ser");
			InputStream buffer = new BufferedInputStream(file);
		    ObjectInput input = new ObjectInputStream (buffer);
		    
		    checkpoint = (String)input.readObject();
		    input.close();
		    
		    System.out.println("Loading checkpoint time:" + checkpoint);
		    
		} catch (IOException | ClassNotFoundException e) { //assume it is our first run and make a new one

			System.out.println("No checkpoint found - starting from beginning.");
			checkpoint = "0000-00-00T00:00:00Z";
			
		}
	      
		users = new HashSet<String>(); 
		most_recent_sync = checkpoint;
		loadOrCreateQueue();
	}
	
	public GroupMigrator(String checkpoint_option_val) {
		checkpoint = checkpoint_option_val;
		users = new HashSet<String>(); 
		most_recent_sync = checkpoint;
		System.out.println("Loading checkpoint time provided by user:" + checkpoint);
		loadOrCreateQueue();
	}

	public void update(){
		/* save checkpoint time */
		try(
			OutputStream file = new FileOutputStream("checkpoint.ser");
		    OutputStream buffer = new BufferedOutputStream(file);
		    ObjectOutput output = new ObjectOutputStream(buffer);
	    ){
			System.out.println("Saving checkpoint time: " + checkpoint );
			System.out.println("Next checkpoint time: " + most_recent_sync );
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
		
		System.out.println("New Users: " + new_users.size() );
		
		/* add listeners for new users */ 
		addListenersToUsers(new_users);
		
	}

	private Map<String,String> checkParameters(MultivaluedMap<String, String> queryParameters) {

		Map<String,String> parameters = new HashMap<>();

		Iterator<String> it = queryParameters.keySet().iterator();

		while(it.hasNext()){
			String theKey = it.next();
			parameters.put(theKey,queryParameters.getFirst(theKey));
		}

		for (String key : parameters.keySet()) {
			System.out.println("Key = " + key + " - " + parameters.get(key));
		}

		return parameters;

	}

	/**
	 * This is a workaround since the current firebase setup is normalized on user_id and only
	 * the REST implementation supports shallow queries
	 * @return a set of users
	 */
	private Set<String> getUsers(){
		Set<String> users = new HashSet<String>();
		
		Client client = Client.create();

		WebResource webResource = client
		   .resource(FB_URL+"/users.json?auth="+FB_SECRET+"&shallow=true");

		ClientResponse response = webResource.accept("application/json")
                   .get(ClientResponse.class);

		if(response == null) {
			System.out.println("response from Firebase is null .... \n");
		} else {
			System.out.println("response from Firebase is NOT null .... \n");
			MultivaluedMap<String, String> map = response.getHeaders();
			checkParameters(map);

			int st = response.getStatus();
			int len = response.getLength();
			System.out.println("Status and length: " + st + " :: " + len);
		}

		if (response.getStatus() != 200) {
		   throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}

		String output = response.getEntity(String.class);

		System.out.println("Output from Server .... \n");
		// System.out.println(output);
		
		try {
			JSONObject obj = new JSONObject(output);
			JSONArray arr  = obj.names();
			for (int i = 0; i < arr.length(); i++){
				users.add(arr.get(i).toString());
			}

		} catch (JSONException e) { e.printStackTrace(); }
		
		return users;
		
	}
	
	
	/* Get User ID from DataSnapshot without traversing fb nodes*/
	private String getFirebaseIdFromRef(DataSnapshot ds){
		 
		int user_start_idx = FB_URL.length() + "users/".length();
		String url_to_search = ds.getRef().toString();
		String user_id = url_to_search.substring(user_start_idx, url_to_search.length());
		user_id = user_id.substring(0, user_id.indexOf('/'));
		
		return user_id;
	}
	
	private static class StepInterval implements java.io.Serializable{

		private static final long serialVersionUID = 2954534620324883606L;
		
		public String sync_start_time;  
		public String sync_end_time;  
		private String firebase_id_fk;
		public String year;
		public String month;
		public String day;
		public String hour;
		public String start_minute;
		public String end_minute;
		public int steps;
		public String device_id;
		public String sync_id;

		public void setFirebase_id_fk(String id) {
			try {
				this.firebase_id_fk = URLDecoder.decode(id, "UTF-8");
			} catch (java.io.UnsupportedEncodingException e) {
				this.firebase_id_fk = "error"; // Deal with this in a better way
			}
		}

		public String getFirebase_id_fk() {
			return this.firebase_id_fk;
		}
		
		public String toString(){
			return "StepInterval(FbID: "+firebase_id_fk +"\t Year:"+ year + "\tMonth:" + month + "\tDay:" + day+ "\thour:" + hour+ "\tSm:"+start_minute+ "\tEm: "+ end_minute +"\tSteps:   "+steps + "\tDID:"+device_id+ "\tSID: "+ sync_id+")";
		}
	}
	
	private void processSync(DataSnapshot sync){
		System.out.println("Processing Sync");
		
		List<StepInterval> steps_synced = new ArrayList<StepInterval>();
		String sync_start_time = (String)sync.child("starttime").getValue();

		String sync_end_time = (String)sync.child("endtime").getValue();
		System.out.println("Endtime: " + sync_end_time);

		synchronized(most_recent_sync){
			if(most_recent_sync.compareTo(sync_end_time) < 0){
				most_recent_sync = sync_end_time;
			}
		}

		String user_id = getFirebaseIdFromRef(sync);
		
		try {
			System.out.println("Sync received for user: " + URLDecoder.decode(user_id, "UTF-8"));
		} catch (java.io.UnsupportedEncodingException ue) {
			System.out.println("Exception decoding username: " + user_id);
			ue.printStackTrace();
		}

		long childCount = sync.getChildrenCount();
		boolean hasSteps = sync.hasChild("steps");
		System.out.println("Number of Children: " + childCount);
		System.out.println("Has 'steps' as child: " + hasSteps);

		Iterable<DataSnapshot> children = sync.getChildren();
		for(DataSnapshot c: children) {
			System.out.println("Child: " + c.getKey() );
		}

		System.out.println("Starting iteration of data of sync from: " + user_id);

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
							
							si.sync_start_time = sync_start_time;
							si.sync_end_time = sync_end_time;
							si.setFirebase_id_fk(user_id);
							
							//Formated so each has a proper number of 
							si.year = year.getKey();
							si.month = month.getKey();
							si.day = day.getKey();


							si.steps = Integer.parseInt((String) step_data.child("count").getValue());
							si.device_id = (String) step_data.child("deviceid").getValue();
							si.sync_id = (String) step_data.child("syncid").getValue();

							SimpleDateFormat time_parser = new SimpleDateFormat("'T'HH:mm:ss'Z'");
							try {
								
								String start_time_val = (String) step_data.child("starttime").getValue();
								Calendar step_start_date = Calendar.getInstance();
								step_start_date.setTime(time_parser.parse(start_time_val));
								si.hour = String.format("%02d", step_start_date.get(Calendar.HOUR_OF_DAY));
								si.start_minute = String.format("%02d", step_start_date.get(Calendar.MINUTE));
								
								String end_time_val = (String) step_data.child("endtime").getValue();
								Calendar step_end_date = Calendar.getInstance();
								step_end_date.setTime(time_parser.parse(end_time_val));
								si.end_minute = String.format("%02d", step_start_date.get(Calendar.MINUTE));
								
							} catch (ParseException e) { e.printStackTrace(); }
							//steps_synced.add(si);
							sql_message_queue.add(si);
							System.out.println("Added to queue: " + si.toString());
						}
					}
				}
			}
			
		}
		
//		System.out.println("This Sync has: " + steps_synced.size() + " step intervals\n");
//		try {
//			if(steps_synced.size() !=0)
//				addSyncToDb(steps_synced);
//		} catch (SQLException e) {e.printStackTrace();}
	}
	
	private void processMeta(DataSnapshot meta){
		System.out.println("Processing Meta");
		System.out.println("Value:\n"+meta.getValue());

		String firebase_id_fk = getFirebaseIdFromRef(meta);
		
		String currentBirthdate	= null;
		String currentEmail		= null;
		String currentFullName	= null;
		String currentGender	= null;
		String currentHeight1	= null;
		String currentHeight2	= null;
		String currentUID		= null;
		String currentUsername	= null;
		String currentWeight	= null;

		Iterable<DataSnapshot> children = meta.getChildren();

		for(DataSnapshot c: children){
			if(c.getKey() == "currentBirthdate"){
				currentBirthdate = (String) c.getValue();
				
			}else if(c.getKey() == "currentEmail"){
				currentEmail = (String) c.getValue();
				
			}else if(c.getKey() == "currentFullName"){
				currentFullName = (String) c.getValue();
				
			}else if(c.getKey() == "currentGender"){
				currentGender = (String) c.getValue();
				
			}else if(c.getKey() == "currentHeight1"){
				currentHeight1 = (String) c.getValue();
				
			}else if(c.getKey() == "currentHeight2"){
				currentHeight2 = (String) c.getValue();
				
			}else if(c.getKey() == "currentUID"){
				currentUID = (String) c.getValue();
				
			}else if(c.getKey() == "currentUsername"){
				currentUsername = (String) c.getValue();
				
			}else if(c.getKey() == "currentWeight"){
				currentWeight = (String) c.getValue();
			}
		}
		
		Connection conn;
		try {
			conn = DriverManager.getConnection(DB_URL+"&noAccessToProcedureBodies=true", username, password);
			CallableStatement proc_stmt = conn.prepareCall("{ call BB_ADD_UPDATE_USER(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }");

			proc_stmt.setString(1, URLDecoder.decode(firebase_id_fk, "UTF-8"));
			
			if(currentBirthdate != null){ 	proc_stmt.setLong(2, Long.parseLong(currentBirthdate));
			}else{							proc_stmt.setNull(2,Types.BIGINT);}

			if(currentEmail != null){ 		proc_stmt.setString(3, URLDecoder.decode(currentEmail, "UTF-8"));
			}else{							proc_stmt.setNull(3,Types.VARCHAR);}
			
			if(currentFullName != null){ 	proc_stmt.setString(4, URLDecoder.decode(currentFullName, "UTF-8"));
			}else{							proc_stmt.setNull(4,Types.VARCHAR);}
			
			if(currentGender != null){ 		proc_stmt.setString(5, URLDecoder.decode(currentGender, "UTF-8"));
			}else{							proc_stmt.setNull(5,Types.VARCHAR);}
			
			if(currentHeight1 != null){ 	proc_stmt.setString(6, URLDecoder.decode(currentHeight1, "UTF-8"));
			}else{							proc_stmt.setNull(6,Types.TINYINT);}
			
			if(currentHeight2 != null){ 	proc_stmt.setString(7, URLDecoder.decode(currentHeight2, "UTF-8"));
			}else{							proc_stmt.setNull(7,Types.TINYINT);}
			
			//currentUID
			proc_stmt.setNull(8,Types.VARCHAR);
			
			if(currentUsername != null){ 	proc_stmt.setString(9, URLDecoder.decode(currentUsername, "UTF-8"));
			}else{							proc_stmt.setNull(9,Types.VARCHAR);}
			
			if(currentWeight != null){ 	proc_stmt.setString(10, currentWeight);
			}else{							proc_stmt.setNull(10,Types.TINYINT);}
			
			proc_stmt.executeQuery();
			
			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
		} catch (java.io.UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		
	}
	
	public void run() {
		
		Connection conn = null;
		int cur_batch_size = 0;
		long latest_added_batch = 0;

		System.out.println("Starting queue listener loop");
		CallableStatement proc_stmt = null;
		try{
	        while(!Thread.currentThread().isInterrupted()){
	        	
	        	StepInterval si=sql_message_queue.peek();

	        	if(si != null){ // queue isn't empty 
					System.out.println("Adding to Batch: "+si.toString());
	
	        		try {

						try {
							if (conn == null) {
								conn = DriverManager.getConnection(DB_URL+"&noAccessToProcedureBodies=true", username, password);
								CONNECTION_CREATED = System.currentTimeMillis();
							}
							if(cur_batch_size++ == 0) proc_stmt = conn.prepareCall("{ call BB_REALTIME_INSERT(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }");

						} catch (SQLException e) {
							System.out.println("MBA: NULL CONNECTION.");
							e.printStackTrace();
						}


	        			proc_stmt.setString(1, si.getFirebase_id_fk());
		    		    proc_stmt.setString(2, si.year);
						proc_stmt.setString(3, String.format("%02d", (Integer.parseInt(si.month))));

						// NOTE: The month - 1 was needed when using java calendar but
						//       not when doing straight database inserts
						// proc_stmt.setString(3, String.format("%02d", (Integer.parseInt(si.month)-1)));

						proc_stmt.setString(4, si.day);
		    		    proc_stmt.setString(5, si.hour);
		    		    proc_stmt.setString(6, si.start_minute);
		    		    proc_stmt.setString(7, si.end_minute);
		    		    proc_stmt.setInt(8, si.steps);
		    		    proc_stmt.setString(9, si.device_id);
		    		    proc_stmt.setString(10, si.sync_id);
		    		    proc_stmt.setString(11, si.sync_start_time);
		    		    proc_stmt.setString(12, si.sync_end_time);
		    		    proc_stmt.addBatch();
		    		    
		    		    
		    		    if(cur_batch_size == SQL_BATCH_SIZE){
		    				System.out.println("Sending Batch of size: " + cur_batch_size);
	
		    		    	proc_stmt.executeBatch();
							proc_stmt.close();
							proc_stmt = null;
		    		    	cur_batch_size= 0;

							if (System.currentTimeMillis() - CONNECTION_CREATED > SQL_MAX_CONNECTION_RESET ) {
								// Need to reset the current connection
								// to avoid automatic reset
								conn.close();
								conn = null;
							}

				        	Thread.sleep(SQL_BATCH_DELAY);
		    		    }
		    		    
		    		    sql_message_queue.poll(); //remove first element
		    		    latest_added_batch = System.currentTimeMillis();
		    		    
					} catch (SQLException e) {
						e.printStackTrace();
					}
	        	
	        		
	        	} else { // idle while there are no msgs

		        	if( cur_batch_size!=0 && System.currentTimeMillis() - latest_added_batch > SQL_MAX_BATCH_WAIT ){
	    				System.out.println("Sending Batch of size: " + cur_batch_size);

	    		    	try {
							proc_stmt.executeBatch();
							proc_stmt.close();
							proc_stmt = null;
							if (System.currentTimeMillis() - CONNECTION_CREATED > SQL_MAX_CONNECTION_RESET ) {
								// Need to reset the current connection
								// to avoid automatic reset
								conn.close();
								conn = null;
							}

						} catch (SQLException e) {e.printStackTrace();}
	    		    	cur_batch_size= 0;
			        	Thread.sleep(SQL_BATCH_DELAY);
	    		    }
		        	
		        	Thread.sleep(SQL_BATCH_DELAY);
	        	}

	        	Thread.sleep(10);
				// Deal with long idle times with no syncs
				if (cur_batch_size==0 && System.currentTimeMillis() - CONNECTION_CREATED > SQL_MAX_CONNECTION_RESET ) {
					// Need to reset the current connection
					// to avoid automatic reset
					if(conn != null) {
						conn.close();
					}
					conn = null;
				}

	        }
		} catch (InterruptedException e) {
	    	System.out.println("Stopping message thread...");
			Thread.currentThread().interrupt();
		}  catch (SQLException e) {
			System.out.println("Stopping message thread...SQL EXCEPTION");
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
		
        try {
			conn.close();
		} catch (SQLException e) {e.printStackTrace();}
        saveQueue();
    }

	
	/**
	 * 
	 * @param userRef - reference 
	 * @param users - list of users to add listeners to
	 */
	private void addListenersToUsers(Set<String> users){
		
		for(String u: users){
			System.out.println("Adding listener to user: " + u);
			
			// User Metadata Listener
			final Firebase userMetaRef = new Firebase(FB_URL+"/users/"+u+ "/metadata");
			userMetaRef.authWithCustomToken(FB_SECRET, new AuthResultHandler() {
			    public void onAuthenticated(AuthData authData) { 
			    	
			    	System.out.println("Authenticated.");
			    	
			    	Query meta_query = userMetaRef;

					meta_query.addValueEventListener(new ValueEventListener() {
						public void onCancelled(FirebaseError arg0) {}
						public void onDataChange(DataSnapshot meta) {
							processMeta(meta);							
						}
					});
			    }
			    public void onAuthenticationError(FirebaseError firebaseError) { System.err.println("Not Authenticated."); }
			});
			
			// User Sync Listener 
			final Firebase userSyncRef = new Firebase(FB_URL+"/users/"+u+ "/sync");
			System.out.println(userSyncRef.toString());
			
			userSyncRef.authWithCustomToken(FB_SECRET, new AuthResultHandler() {
			    public void onAuthenticated(AuthData authData) { 
			    	
			    	System.out.println("Authenticated.");
			    	
			    	Query sync_query = userSyncRef.orderByChild("endtime").startAt(checkpoint);

					sync_query.addChildEventListener(new ChildEventListener() {
						
						// When there is a new sync add it to our SQL DB
						public void onChildAdded(DataSnapshot sync, String arg1) {
							processSync(sync);
						}
						
						public void onCancelled(FirebaseError arg0) {}
						public void onChildChanged(DataSnapshot sync, String arg1) {
							processSync(sync);
						}
						public void onChildMoved(DataSnapshot arg0, String arg1) {}
						public void onChildRemoved(DataSnapshot arg0) {}
					});
			    }
			    public void onAuthenticationError(FirebaseError firebaseError) { System.err.println("Not Authenticated."); }
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

		options.addOption("sqlBatchSize",		true, 	"size of batches to be sent" );
		options.addOption("sqlBatchDelay",		true, 	"minumum time in ms between sending batches" );
		options.addOption("sqlMaxBatchWait",	true, 	"max time to wait before sending a batche" );

		
		
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
	    	System.out.println("User defined checkpointInterval: "+ CHECKPOINT_INTERVAL);
	    }
	    
	    if (cmd.hasOption("useSSL") || USING_GAE_SQL){
			System.out.println("User defined useSSL: "+ true);
		}
		
		if (cmd.hasOption("keyStore")){
			System.setProperty("javax.net.ssl.keyStore",			cmd.getOptionValue("keyStore"));
			System.out.println("User defined keyStore: "+ cmd.getOptionValue("keyStore"));
		}else{
			System.setProperty("javax.net.ssl.keyStore",			System.getProperty("user.dir")+"/keystore");
			System.out.println("No keyStore set.");
		}
		
		if (cmd.hasOption("keyStorePass")){
			System.setProperty("javax.net.ssl.keyStorePassword",	cmd.getOptionValue("keyStorePass"));
			System.out.println("User defined keyStorePass: "+ cmd.getOptionValue("keyStorePass"));
		}else{
			// System.setProperty("javax.net.ssl.keyStorePassword",	"movomovo");
			System.setProperty("javax.net.ssl.keyStorePassword",	keyStorePassword);
			System.out.println("No keyStore pass set.");
		}
		
		if (cmd.hasOption("trustStore")){
			System.setProperty("javax.net.ssl.trustStore",			cmd.getOptionValue("trustStore"));
			System.out.println("User defined trustStore: "+ cmd.getOptionValue("trustStore"));
		}else{
			System.setProperty("javax.net.ssl.trustStore",			System.getProperty("user.dir")+"/truststore");
			System.out.println("No truststore set.");
		}
		
		if (cmd.hasOption("trustStorePass")){
			System.setProperty("javax.net.ssl.trustStorePassword",	cmd.getOptionValue("trustStorePass"));
			System.out.println("User defined trustStorePassword: "+ cmd.getOptionValue("trustStorePass"));
		}else{
			// System.setProperty("javax.net.ssl.trustStorePassword",	"movomovo");
			System.setProperty("javax.net.ssl.trustStorePassword",	trustStorePassword);
			System.out.println("No truststore pass set.");
		}

		try {
			Class.forName("com.mysql.jdbc.Driver");
		}catch (ClassNotFoundException e) {e.printStackTrace();}

		if (cmd.hasOption("FirebaseURL")){
			FB_URL = cmd.getOptionValue("FirebaseURL");
			System.out.println("User defined FirebaseURL: "+ cmd.getOptionValue("FirebaseURL"));
		}
		
		if (cmd.hasOption("FirebaseSecret")){
			FB_SECRET = cmd.getOptionValue("FirebaseSecret");
			System.out.println("User defined FirebaseSecret: "+ cmd.getOptionValue("FirebaseSecret"));
		}
		
		if (cmd.hasOption("MysqlURL")){
			DB_URL = cmd.getOptionValue("MysqlURL");
			System.out.println("User defined MysqlURL: "+ cmd.getOptionValue("MysqlURL"));

		}
		
		if (cmd.hasOption("sqlBatchSize")){
			SQL_BATCH_SIZE = Integer.parseInt(cmd.getOptionValue("sqlBatchSize"));
			System.out.println("User defined sqlBatchSize: "+ cmd.getOptionValue("sqlBatchSize"));
		}
		
		if (cmd.hasOption("sqlBatchDelay")){
			SQL_BATCH_DELAY = Integer.parseInt(cmd.getOptionValue("sqlBatchDelay"));
			System.out.println("User defined sqlBatchDelay: "+ cmd.getOptionValue("sqlBatchDelay"));
		}
		
		if (cmd.hasOption("sqlMaxBatchWait")){
			SQL_MAX_BATCH_WAIT = Integer.parseInt(cmd.getOptionValue("sqlMaxBatchWait"));
			System.out.println("User defined sqlMaxBatchWait: "+ cmd.getOptionValue("sqlMaxBatchWait"));
		}

		GroupMigrator gm = null;
		if (cmd.hasOption("checkpoint")){
			gm = new GroupMigrator(cmd.getOptionValue("checkpoint")); 
		}else{
			gm = new GroupMigrator(); 
		}

		// start SQL Queue Thread
		final Thread msg_thread = new Thread(gm);
		msg_thread.start();

		Runtime.getRuntime().addShutdownHook(new Thread() {
	        public void run() {
	            try {
	                System.out.println("Shutting down ...");
	                /* Save out Queue */
	                msg_thread.interrupt();
	                Thread.sleep(10000);


	            } catch (InterruptedException e) {
	                e.printStackTrace();
	            }
	        }
	    });
		
		
		while(true){
			
			gm.update();
				 
			try {
				Thread.sleep(CHECKPOINT_INTERVAL);
				boolean alive = msg_thread.isAlive();
				if(!alive) {
					msg_thread.start();
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
		}
		
		
	}
}
