package com.sensorstar.movo;

import java.io.*;
import java.net.URLDecoder;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
import org.apache.http.HttpException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.firebase.client.*;
import com.firebase.client.Firebase.AuthResultHandler;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.logging.SimpleFormatter;

import javax.ws.rs.core.MultivaluedMap;


public class GroupMigrator implements Runnable{

	static private FileHandler fileTxt;
	static private SimpleFormatter formatterTxt;
	// get the global logger to configure it
	final static private Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	// suppress the logging output to the console
	final static private Logger rootLogger = Logger.getLogger("");
	final static private Handler[] handlers = rootLogger.getHandlers();

	static Thread msg_thread;


	/* Time between saving checkpoint time and checking for new users */
	private static long CHECKPOINT_INTERVAL = 36*1000;
	private static long CONNECTION_CREATED = 0;
	
	private static int GETUSER_MAX_ATTEMPS = 5;
	private static long GETUSER_TIMEOUT = 30*1000;
	
	private static int SQL_BATCH_DELAY = 10;
	private static int SQL_BATCH_SIZE = 10;
	private static int SQL_MAX_BATCH_WAIT = 10*1000;// in Milliseconds
	private static int SQL_MAX_CONNECTION_RESET = 10*60*1000;// in Milliseconds

	private static String home = System.getProperty("user.home");
	private static File db_log = new File(home + "/realtime/dbheartbeat.txt");
	private static File main_log = new File(home + "/realtime/mainheartbeat.txt");

	/* Sensorstar Local Debug Defaults */
//	private static final String FB_URL = "https://ss-movo-wave-v2.firebaseio.com";
//	private static final String FB_SECRET = "jBMdrOwNCfJ37NzcXt6IM4d7AddeojCJg2Z9KnuF";
//	private static final String DB_URL = "jdbc:mysql://localhost:3306/movogroups?user=root";
//	private static final boolean USING_GAE_SQL = false;
//	private static String keyStorePassword = "keystore"; // keystore
//	private static String trustStorePassword = "truststore";  // truststore

	/* Debug Defaults */
	// private static String FB_URL = "https://movowave-debug.firebaseio.com/";
	// private static String FB_SECRET = "3HFJlhjThUhC9QrP4zAq4PNcaXH8IWYqM8cCWmnR";

	// private static String DB_URL = "jdbc:mysql://173.194.247.177:3306/movogroups?user=root&useSSL=true";
	private static final boolean USING_GAE_SQL = true;

	/* Production Defaults */
    private static String FB_URL = "https://movowave.firebaseio.com/";
    private static String FB_SECRET = "0paTj5f0KHzLBnwIyuc1eEvq4tXZ3Eik9Joqrods";
	private static String DB_URL = "jdbc:mysql://173.194.241.127:3306/movogroups?useSSL=true&requireSSL=true";
	static String keyStorePassword = "r87p-Y?72*uXqW$aSZGU"; // keystore
	static String trustStorePassword = "r87p-Y?72*uXqW$aSZGU";  // truststore
	private static String username = "movogroups";
	private static String password = "H8$E=?3*ADXFt4Ld7-jw";

	// Google Test Database
	// private static String DB_URL = "jdbc:mysql://173.194.247.177:3306/movogroups?user=root&useSSL=true"; // Google test
	// private static String DB_URL = "jdbc:mysql://173.194.239.157:3306/movogroups?useSSL=true&requireSSL=true";


	//	private Map<String,String> latest_sync_for_users;
	private String checkpoint;
	private String most_recent_sync;

	private ConcurrentLinkedQueue<StepInterval> sql_message_queue;
	
	private Set<String> users;

	static public void loggerSetup() {

		if (handlers[0] instanceof ConsoleHandler) {
			rootLogger.removeHandler(handlers[0]);
		}

		logger.setLevel(Level.INFO);
		try {
			fileTxt = new FileHandler("groupmonitor.log");
		} catch (IOException e) {
			e.printStackTrace();
		}

		// create a TXT formatter
		formatterTxt = new SimpleFormatter();
		fileTxt.setFormatter(formatterTxt);
		logger.addHandler(fileTxt);

	}
	
	private void loadOrCreateQueue(){
				
		InputStream file;
		try {//try to load user map
			file = new FileInputStream("sql_message_queue.ser");
			InputStream buffer = new BufferedInputStream(file);
		    ObjectInput input = new ObjectInputStream (buffer);
		    
		    sql_message_queue = (ConcurrentLinkedQueue<StepInterval>)input.readObject();
		    input.close();

			logger.log(Level.INFO, "Loaded queue of size:" + sql_message_queue.size());
		    // logger.log( Level.INFO, "Loaded queue of size:" + sql_message_queue.size());
		    
		} catch (IOException | ClassNotFoundException e) { //assume it is our first run and make a new one
			logger.log( Level.INFO, "No sql found - creating new one.", e );
			logger.log( Level.INFO, e.toString(), e );
			sql_message_queue = new ConcurrentLinkedQueue<StepInterval>();			
		}
	      
	}
	
	private void saveQueue(){
		try(
				OutputStream file = new FileOutputStream("sql_message_queue.ser");
			    OutputStream buffer = new BufferedOutputStream(file);
			    ObjectOutput output = new ObjectOutputStream(buffer);
		    ){
				logger.log( Level.INFO, "Saving queue of size: " + sql_message_queue.size());
				synchronized(sql_message_queue){
					output.writeObject(sql_message_queue);
				}
				output.close();
			} catch (IOException e) {
				logger.log( Level.SEVERE, e.toString(), e );
			}
	}

	public static void dbConnectionHeartBeat(boolean status){

		try{
			if(!db_log.exists()){
				logger.log( Level.INFO, "Created new heartbeat file." );
				db_log.createNewFile();
			}

			FileWriter fileWriter = new FileWriter(db_log, false);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			bufferedWriter.write( status?String.valueOf(System.currentTimeMillis()):"0" ); // date +"%s"
			bufferedWriter.close();

		} catch(IOException e) {
			logger.log( Level.SEVERE, "COULD NOT LOG DB Connection HEARTBEAT!!" );
			logger.log( Level.SEVERE, e.toString(), e );
		}
	}

	public static void mainThreadHeartBeat(boolean status){

		try{
			if(!main_log.exists()){
				logger.log( Level.INFO, "Created new main thread file." );
				main_log.createNewFile();
			}

			FileWriter fileWriter = new FileWriter(main_log, false);

			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			bufferedWriter.write( status?String.valueOf(System.currentTimeMillis()):"0" ); // date +"%s"
			bufferedWriter.close();

		} catch(IOException e) {
			logger.log(Level.SEVERE, "COULD NOT LOG Main Thread HEARTBEAT!!" );
			logger.log(Level.SEVERE, e.toString(), e);
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

			logger.log( Level.INFO, "Loading checkpoint time: " + checkpoint);


		} catch (IOException | ClassNotFoundException e) { //assume it is our first run and make a new one
			logger.log( Level.INFO, "No checkpoint found - starting from beginning.");
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
		logger.log( Level.INFO, "Loading checkpoint time provided by user:" + checkpoint);
		loadOrCreateQueue();
	}

	public void update() throws InterruptedException{

		int get_user_attempt = 0;
		while(GETUSER_MAX_ATTEMPS == -1 || get_user_attempt<GETUSER_MAX_ATTEMPS){
			try{
				/* update our user list and find new users */
				Set<String> updated_users = getUsers();
				Set<String> new_users = new HashSet<String>(updated_users);
				new_users.removeAll(users);
				users = updated_users;
				
				logger.log( Level.INFO, "New Users: " + new_users.size());
				
				/* add listeners for new users */ 
				addListenersToUsers(new_users);

				/* save checkpoint time */
				try(
					OutputStream file = new FileOutputStream("checkpoint.ser");
				    OutputStream buffer = new BufferedOutputStream(file);
				    ObjectOutput output = new ObjectOutputStream(buffer);
			    ){
					
					logger.log( Level.INFO, "Saving checkpoint time: " + checkpoint );
					logger.log( Level.INFO, "Next checkpoint time: " + most_recent_sync);

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
				
				break;
			}catch(HttpException e){
				logger.log( Level.INFO,"Trying again");
				get_user_attempt++;
				Thread.sleep(GETUSER_TIMEOUT);
			}
		}

	}

	private static Map<String,String> checkParameters(MultivaluedMap<String, String> queryParameters) {

		Map<String,String> parameters = new HashMap<>();

		Iterator<String> it = queryParameters.keySet().iterator();

		while(it.hasNext()){
			String theKey = it.next();
			parameters.put(theKey,queryParameters.getFirst(theKey));
		}

		for (String key : parameters.keySet()) {
			logger.log( Level.INFO, "Key = " + key + " - " + parameters.get(key));
		}

		return parameters;

	}

	/**
	 * This is a workaround since the current firebase setup is normalized on user_id and only
	 * the REST implementation supports shallow queries
	 * @return a set of users
	 */

	public static Set<String> getUsers() throws HttpException{

		Set<String> users = new HashSet<String>();
		
		Client client = Client.create();

		WebResource webResource = client
		   .resource(FB_URL+"/users.json?auth="+FB_SECRET+"&shallow=true");

		ClientResponse response = webResource.accept("application/json")
                   .get(ClientResponse.class);

		if(response == null) {
			logger.log( Level.INFO,"response from Firebase is null .... \n");
		} else {
			logger.log( Level.INFO,"response from Firebase is NOT null .... \n");
			MultivaluedMap<String, String> map = response.getHeaders();
			checkParameters(map);

			int st = response.getStatus();
			int len = response.getLength();
			logger.log( Level.INFO,"Status and length: " + st + " :: " + len);
		}

		if (response.getStatus() != 200) {
			logger.log( Level.WARNING, "Failed : HTTP error code : " + response.getStatus());
			throw new HttpException("Failed : HTTP error code : " + response.getStatus());
		}

		String output = response.getEntity(String.class);

		logger.log( Level.INFO,"Output from Server .... \n");
		// logger.log( Level.INFO, output);
		
		try {
			JSONObject obj = new JSONObject(output);
			JSONArray arr  = obj.names();
			for (int i = 0; i < arr.length(); i++){
				users.add(arr.get(i).toString());
			}

		} catch (JSONException e) { e.printStackTrace(); }
		
		return users;
	}
	
	/**
	 * REST Protocol
	 * @param user
	 * @return
	 * @throws HttpException
	 */
	public static Set<String> getSyncsForUser(String user) throws HttpException{
		Set<String> syncs = new HashSet<String>();
		
		Client client = Client.create();

		WebResource webResource = client
		   .resource(FB_URL+"/users/"+user+"/sync.json?auth="+FB_SECRET+"&shallow=true");

		ClientResponse response = webResource.accept("application/json")
                   .get(ClientResponse.class);

		if(response == null) {
			logger.log( Level.INFO,"response from Firebase is null .... \n");
		} else {
			logger.log( Level.INFO,"response from Firebase is NOT null .... \n");
			MultivaluedMap<String, String> map = response.getHeaders();
			checkParameters(map);

			int st = response.getStatus();
			int len = response.getLength();
			logger.log( Level.INFO,"Status and length: " + st + " :: " + len);
		}

		if (response.getStatus() != 200) {
			logger.log( Level.WARNING, "Failed : HTTP error code : " + response.getStatus());
			throw new HttpException("Failed : HTTP error code : " + response.getStatus());
		}

		String output = response.getEntity(String.class);

		logger.log( Level.INFO,"Output from Server .... \n");
		// System.out.println(output);
		
		try {
			JSONObject obj = new JSONObject(output);
			JSONArray arr  = obj.names();
			for (int i = 0; i < arr.length(); i++){
				syncs.add(arr.get(i).toString());
			}

		} catch (JSONException e) { e.printStackTrace(); }
		
		return syncs;
	}
	
	/**
	 * REST Protocol
	 * @param userID
	 * @param syncID
	 * @return
	 * @throws HttpException
	 */
	public static String getSyncForUser(String userID,String syncID) throws HttpException{
		Set<String> syncs = new HashSet<String>();
		
		Client client = Client.create();

		WebResource webResource = client
		   .resource(FB_URL+"/users/"+userID+"/sync/"+syncID+".json?auth="+FB_SECRET+"");

		ClientResponse response = webResource.accept("application/json")
                   .get(ClientResponse.class);

		if(response == null) {
			logger.log( Level.INFO,"response from Firebase is null .... \n");
		} else {
			logger.log( Level.INFO,"response from Firebase is NOT null .... \n");
			MultivaluedMap<String, String> map = response.getHeaders();
			checkParameters(map);

			int st = response.getStatus();
			int len = response.getLength();
			logger.log( Level.INFO,"Status and length: " + st + " :: " + len);
		}

		if (response.getStatus() != 200) {
			logger.log( Level.WARNING, "Failed : HTTP error code : " + response.getStatus());
			throw new HttpException("Failed : HTTP error code : " + response.getStatus());
		}

		return response.getEntity(String.class);

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
		logger.log( Level.INFO, "Processing Sync");
		
		List<StepInterval> steps_synced = new ArrayList<StepInterval>();
		String sync_start_time = (String)sync.child("starttime").getValue();

		String sync_end_time = (String)sync.child("endtime").getValue();
		// logger.log( Level.INFO, "Endtime: " + sync_end_time);
		logger.log( Level.INFO, "Endtime: " + sync_end_time);


		synchronized(most_recent_sync){
			if(most_recent_sync.compareTo(sync_end_time) < 0){
				most_recent_sync = sync_end_time;
			}
		}

		String user_id = getFirebaseIdFromRef(sync);
		
		try {
			// logger.log( Level.INFO, "Sync received for user: " + URLDecoder.decode(user_id, "UTF-8"));
			logger.log( Level.INFO, "Sync received for user: " + URLDecoder.decode(user_id, "UTF-8"));
		} catch (java.io.UnsupportedEncodingException ue) {
			logger.log( Level.INFO, "Exception decoding username: " + user_id);
			ue.printStackTrace();
		}

		long childCount = sync.getChildrenCount();
		boolean hasSteps = sync.hasChild("steps");
		// logger.log( Level.INFO, "Number of Children: " + childCount);
		// logger.log( Level.INFO, "Has 'steps' as child: " + hasSteps);

		logger.log( Level.INFO, "Number of Children: " + childCount);
		logger.log( Level.INFO, "Has 'steps' as child: " + hasSteps);

		Iterable<DataSnapshot> children = sync.getChildren();
		for(DataSnapshot c: children) {
			logger.log( Level.INFO, "Child: " + c.getKey() );
		}

		logger.log( Level.INFO, "Starting iteration of data of sync from: " + user_id);

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
							// logger.log( Level.INFO, "Added to queue: " + si.toString());
							logger.log( Level.INFO, "Added to queue: " + si.toString());

						}
					}
				}
			}
			// logger.log( Level.INFO, "Sync added - " + sql_message_queue.size() + " sql inserts to be processed");
			logger.log( Level.INFO, "Sync added - " + sql_message_queue.size() + " sql inserts to be processed");
		}
		
//		logger.log( Level.INFO, "This Sync has: " + steps_synced.size() + " step intervals\n");
//		try {
//			if(steps_synced.size() !=0)
//				addSyncToDb(steps_synced);
//		} catch (SQLException e) {e.printStackTrace();}
	}

	/** Conversion from firebase to sql */
	static public class PropertyMapper {
		final String key;
		final int index;
		final int type;

		/** Sets up a new mapping to query index and type.
		 *
		 * @param key
		 * @param index
		 * @param type
         */
		public PropertyMapper(final String key, final int index, final int type ) {
			this.key = key;
			this.index = index;
			this.type = type;
		}

		/** Sets the value on the statement using the appropriate conversion. Overide for custom things.
		 *
		 * @param statement
		 * @param values
         * @throws SQLException
         */
		public void setValue( final CallableStatement statement, final Map<String,String> values ) throws SQLException {
			final String value = values.get( key );

			try {
				switch (type) {
					case Types.BIGINT:
					case Types.TINYINT:
						statement.setLong(index, Long.parseLong(value));
						break;

					case Types.VARCHAR:
						statement.setString(index, URLDecoder.decode(value, "UTF-8"));
						break;

					default:
						throw new SQLException("Unsupported type " + type);
				}
			} catch (NumberFormatException | UnsupportedEncodingException | NullPointerException e) {
				statement.setNull(index, type);
			}
		}
	}

	/** Specialized subclass to supply default username */
	static class UsernameMapper extends PropertyMapper {
		public UsernameMapper(final String key, final int index, final int type ) {
			super( key, index, type );
		}

		@Override
		/** Try to extract first part of email as default username if username is null or 'Error'.
		 *
		 */
		public void setValue( final CallableStatement statement, final Map<String,String> values ) throws SQLException {
			String value = values.get( key );

			if( value != null && ! value.equals("Error") ) {
				super.setValue( statement, values );
			} else {
				try {
					String email = URLDecoder.decode( values.get( "currentEmail" ), "UTF-8" );
					statement.setString( index, email.split( "@")[ 0 ] );
				} catch (UnsupportedEncodingException|NullPointerException e ) {
					statement.setNull( index, type );
				}
			}
		}
	}

	/** Conversion mappings from firebase to SQL */
	final static PropertyMapper[] conversionMappers = new PropertyMapper[]{
		new PropertyMapper( "firebase_id_fk", 1, Types.VARCHAR ),
		new PropertyMapper( "currentBirthday", 2, Types.BIGINT ),
		new PropertyMapper( "currentEmail", 3, Types.VARCHAR ),
		new PropertyMapper( "currentFullName", 4, Types.VARCHAR ),
		new PropertyMapper( "currentGender", 5, Types.VARCHAR ),
		new PropertyMapper( "currentHeight1", 6, Types.TINYINT ),
		new PropertyMapper( "currentHeight2", 7, Types.TINYINT ),
		new PropertyMapper( "currentUID", 8, Types.VARCHAR ),
		new UsernameMapper( "currentUsername", 9, Types.VARCHAR ),
		new PropertyMapper( "currentWeight", 10, Types.TINYINT ),
	};

	private void processMeta(DataSnapshot meta){
		// logger.log( Level.INFO, "Processing Meta");
		// logger.log( Level.INFO, "Value:\n"+meta.getValue());
		logger.log( Level.INFO, "Processing Meta");
		logger.log( Level.INFO, "Value:\n"+meta.getValue());

		String firebase_id_fk = getFirebaseIdFromRef(meta);

		Iterable<DataSnapshot> children = meta.getChildren();

		Map<String, String> values = new HashMap<>();

		values.put( "firebase_id_fk", firebase_id_fk );

		/* extract data */
		for(DataSnapshot child: children) {
			values.put(child.getKey(), (String) child.getValue());
		}
		
		Connection conn;
		try {
			conn = DriverManager.getConnection(DB_URL+"&noAccessToProcedureBodies=true", username, password);
			CallableStatement proc_stmt = conn.prepareCall("{ call BB_ADD_UPDATE_USER(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }");

			/* convert data to SQL statement */
			for(PropertyMapper mapper : conversionMappers ) {
				mapper.setValue( proc_stmt, values );
			}
			
			proc_stmt.executeQuery();
			
			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		
	}
	
	public void run() {
		
		Connection conn = null;
		int cur_batch_size = 0;
		long latest_added_batch = 0;

		// logger.log( Level.INFO, "Starting queue listener loop");
		logger.log( Level.INFO, "Starting queue listener loop");
		CallableStatement proc_stmt = null;
		try{
	        while(!Thread.currentThread().isInterrupted()){
	        	
	        	StepInterval si=sql_message_queue.peek();

	        	if(si != null){ // queue isn't empty 
					// logger.log( Level.INFO, "Adding to Batch: "+si.toString());
					logger.log( Level.INFO, "Adding to Batch: "+si.toString());
	
	        		try {

						try {
							if (conn == null || conn.isClosed()) {
								conn = DriverManager.getConnection(DB_URL+"&noAccessToProcedureBodies=true", username, password);
								CONNECTION_CREATED = System.currentTimeMillis();
								GroupMigrator.dbConnectionHeartBeat(true);
							}
							if(cur_batch_size++ == 0) proc_stmt = conn.prepareCall("{ call BB_REALTIME_INSERT(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }");

						} catch (SQLException e) {
							// logger.log( Level.INFO, "MBA: NULL CONNECTION.");
							logger.log(Level.INFO, "MBA: NULL CONNECTION.");
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
		    				// logger.log( Level.INFO, "Sending Batch of size: " + cur_batch_size);
							logger.log( Level.INFO, "Sending Batch of size: " + cur_batch_size);
	
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
						// e.printStackTrace();
						logger.log( Level.SEVERE, e.getMessage(), e);
					}
	        	
	        		
	        	} else { // idle while there are no msgs

		        	if( cur_batch_size!=0 && System.currentTimeMillis() - latest_added_batch > SQL_MAX_BATCH_WAIT ){
	    				// logger.log( Level.INFO, "Sending Batch of size: " + cur_batch_size);
						logger.log( Level.INFO, "Sending Batch of size: " + cur_batch_size);

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
					Thread.sleep(10);
					GroupMigrator.dbConnectionHeartBeat(true);
				}

	        }
		} catch (InterruptedException e) {
	    	// logger.log( Level.INFO, "Stopping message thread...");
			logger.log( Level.INFO, "Stopping message thread...");
			Thread.currentThread().interrupt();
		}  catch (SQLException e) {
			// logger.log( Level.INFO, "Stopping message thread...SQL EXCEPTION");
			logger.log( Level.INFO, "Stopping message thread...SQL EXCEPTION");
			logger.log( Level.SEVERE, e.getMessage(), e);
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
		
        try {
			conn.close();
		} catch (SQLException e) {e.printStackTrace(); logger.log( Level.SEVERE, e.getMessage(), e);}
        saveQueue();
		GroupMigrator.dbConnectionHeartBeat(false);
    }

	
	/**
	 * 
	 * @param userRef - reference 
	 * @param users - list of users to add listeners to
	 */
	private void addListenersToUsers(Set<String> users){
		
		for(String u: users){
			// logger.log( Level.INFO, "Adding listener to user: " + u);
			logger.log( Level.INFO, "Adding listener to user: " + u);
			
			// User Metadata Listener
			final Firebase userMetaRef = new Firebase(FB_URL+"/users/"+u+ "/metadata");
			userMetaRef.authWithCustomToken(FB_SECRET, new AuthResultHandler() {
			    public void onAuthenticated(AuthData authData) { 
			    	
			    	// logger.log( Level.INFO, "Authenticated.");
					logger.log( Level.INFO, "Authenticated.");
			    	
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
			// logger.log( Level.INFO, userSyncRef.toString());
			logger.log( Level.INFO, userSyncRef.toString());

			userSyncRef.authWithCustomToken(FB_SECRET, new AuthResultHandler() {
			    public void onAuthenticated(AuthData authData) { 
			    	
			    	// logger.log( Level.INFO, "Authenticated.");
					logger.log( Level.INFO, "Authenticated.");
			    	
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

		options.addOption("getUserMaxAttemps", 	true, 	"Sets max number of times to attempt to get users from firebase, -1 means it tries indefinitely (default = 5). ");
		options.addOption("getUserTimeout", 	true, 	"Sets timeout between attemps to connect to firebase RestApi (default = 30seconds).");

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
	    	logger.log( Level.INFO, "User defined checkpointInterval: "+ CHECKPOINT_INTERVAL);
			logger.log(Level.INFO, "User defined checkpointInterval: " + CHECKPOINT_INTERVAL);
	    }
	    
	    if(cmd.hasOption("getUserTimeout")) {
	    	GETUSER_TIMEOUT = Long.parseLong(cmd.getOptionValue("getUserTimeout"));
	    	logger.log( Level.INFO, "User defined getUserTimeout: "+ CHECKPOINT_INTERVAL);
	    }
	    
	    if (cmd.hasOption("useSSL") || USING_GAE_SQL){
			logger.log( Level.INFO, "User defined useSSL: "+ true);
		}
	    
	    if (cmd.hasOption("useSSL") || USING_GAE_SQL){
			// logger.log( Level.INFO, "User defined useSSL: "+ true);
			logger.log( Level.INFO, "User defined useSSL: "+ true);
		}
		
		if (cmd.hasOption("keyStore")){
			System.setProperty("javax.net.ssl.keyStore",			cmd.getOptionValue("keyStore"));
			logger.log( Level.INFO, "User defined keyStore: "+ cmd.getOptionValue("keyStore"));
		}else{
			System.setProperty("javax.net.ssl.keyStore",			System.getProperty("user.dir")+"/keystore");
			logger.log( Level.INFO, "No keyStore set.");
		}
		
		if (cmd.hasOption("keyStorePass")){
			System.setProperty("javax.net.ssl.keyStorePassword",	cmd.getOptionValue("keyStorePass"));
			logger.log( Level.INFO, "User defined keyStorePass: "+ cmd.getOptionValue("keyStorePass"));
		}else{
			// System.setProperty("javax.net.ssl.keyStorePassword",	"movomovo");
			System.setProperty("javax.net.ssl.keyStorePassword",	keyStorePassword);
			logger.log( Level.INFO, "No keyStore pass set.");
		}
		
		if (cmd.hasOption("trustStore")){
			System.setProperty("javax.net.ssl.trustStore",			cmd.getOptionValue("trustStore"));
			logger.log( Level.INFO, "User defined trustStore: "+ cmd.getOptionValue("trustStore"));
		}else{
			System.setProperty("javax.net.ssl.trustStore",			System.getProperty("user.dir")+"/truststore");
			logger.log( Level.INFO, "No truststore set.");
		}
		
		if (cmd.hasOption("trustStorePass")){
			System.setProperty("javax.net.ssl.trustStorePassword",	cmd.getOptionValue("trustStorePass"));
			logger.log( Level.INFO, "User defined trustStorePassword: "+ cmd.getOptionValue("trustStorePass"));
		}else{
			// System.setProperty("javax.net.ssl.trustStorePassword",	"movomovo");
			System.setProperty("javax.net.ssl.trustStorePassword",	trustStorePassword);
			logger.log( Level.INFO, "No truststore pass set.");
		}

		try {
			Class.forName("com.mysql.jdbc.Driver");
		}catch (ClassNotFoundException e) {e.printStackTrace();}

		if (cmd.hasOption("FirebaseURL")){
			FB_URL = cmd.getOptionValue("FirebaseURL");
			logger.log( Level.INFO, "User defined FirebaseURL: "+ cmd.getOptionValue("FirebaseURL"));
		}
		
		if (cmd.hasOption("FirebaseSecret")){
			FB_SECRET = cmd.getOptionValue("FirebaseSecret");
			logger.log( Level.INFO, "User defined FirebaseSecret: "+ cmd.getOptionValue("FirebaseSecret"));
		}
		
		if (cmd.hasOption("MysqlURL")){
			DB_URL = cmd.getOptionValue("MysqlURL");
			logger.log( Level.INFO, "User defined MysqlURL: "+ cmd.getOptionValue("MysqlURL"));

		}
		
		if (cmd.hasOption("sqlBatchSize")){
			SQL_BATCH_SIZE = Integer.parseInt(cmd.getOptionValue("sqlBatchSize"));
			logger.log( Level.INFO, "User defined sqlBatchSize: "+ cmd.getOptionValue("sqlBatchSize"));
		}
		
		if (cmd.hasOption("sqlBatchDelay")){
			SQL_BATCH_DELAY = Integer.parseInt(cmd.getOptionValue("sqlBatchDelay"));
			logger.log( Level.INFO, "User defined sqlBatchDelay: "+ cmd.getOptionValue("sqlBatchDelay"));
		}
		
		if (cmd.hasOption("sqlMaxBatchWait")){
			SQL_MAX_BATCH_WAIT = Integer.parseInt(cmd.getOptionValue("sqlMaxBatchWait"));
			logger.log( Level.INFO, "User defined sqlMaxBatchWait: "+ cmd.getOptionValue("sqlMaxBatchWait"));
		}

		GroupMigrator.loggerSetup();
		GroupMigrator gm = null;
		if (cmd.hasOption("checkpoint")){
			gm = new GroupMigrator(cmd.getOptionValue("checkpoint")); 
		}else{
			gm = new GroupMigrator(); 
		}

		// start SQL Queue Thread
		msg_thread = new Thread(gm);
		msg_thread.start();

		Runtime.getRuntime().addShutdownHook(new Thread() {
	        public void run() {
	            try {
	                logger.log( Level.INFO, "Shutting down ...");
	                /* Save out Queue */
	                msg_thread.interrupt();
	                Thread.sleep(10000);


	            } catch (InterruptedException e) {
	                e.printStackTrace();
	            }
				GroupMigrator.mainThreadHeartBeat(false);
	        }
	    });

		GroupMigrator.mainThreadHeartBeat(true);
		long mainThreadTime = System.currentTimeMillis();
		while(true){

			try {

				gm.update();
				logger.log( Level.INFO, "SQL: "+gm.sql_message_queue.size() + " sql inserts to be processed");

				// Heartbeat check every ten minutes
				if (System.currentTimeMillis() - mainThreadTime > 600000) {
					GroupMigrator.mainThreadHeartBeat(true);
					mainThreadTime = System.currentTimeMillis();
				}

				try {
					Thread.sleep(CHECKPOINT_INTERVAL);
					boolean alive = msg_thread.isAlive();
					if(!alive) {
						msg_thread = new Thread(gm);
						msg_thread.start();
					}

				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}

			} catch (Exception e) {
				e.printStackTrace();
				// logger.log( Level.INFO, "Exception in main thread: " + e.getMessage());
				logger.log( Level.SEVERE, "Exception in main thread: " + e.getMessage(), e);
			}
			
		}
		GroupMigrator.mainThreadHeartBeat(false);
		
	}
}