package com.sensorstar.movo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.http.HttpException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.sensorstar.movo.GroupMigrator.StepInterval;
import com.sun.jersey.api.client.ClientResponse;

public class MissedSyncCatcher implements Runnable{
	
	static Object conn_sync = new Object();
	static Connection conn;
	
	static protected Thread msg_thread;

	
	/* Time between saving checkpoint time and checking for new users */
	protected static long CHECKPOINT_INTERVAL = 36*1000;
	protected static long CONNECTION_CREATED = 0;
	
	protected static int GETUSER_MAX_ATTEMPS = 5;
	protected static long GETUSER_TIMEOUT = 30*1000;
	
	protected static int SQL_BATCH_DELAY = 10;
	protected static int SQL_BATCH_SIZE = 10;
	protected static int SQL_MAX_BATCH_WAIT = 10*1000;// in Milliseconds
	protected static int SQL_MAX_CONNECTION_RESET = 10*60*1000;// in Milliseconds

	protected static File db_log = new File( "./dbheartbeat.txt");
	protected static File main_log = new File( "./mainheartbeat.txt");

	/* Sensorstar Local Debug Defaults */
//	protected static final String FB_URL = "https://ss-movo-wave-v2.firebaseio.com";
//	protected static final String FB_SECRET = "jBMdrOwNCfJ37NzcXt6IM4d7AddeojCJg2Z9KnuF";
//	protected static final String DB_URL = "jdbc:mysql://localhost:3306/movogroups?user=root";
//	protected static final boolean USING_GAE_SQL = false;
//	protected static String keyStorePassword = "keystore"; // keystore
//	protected static String trustStorePassword = "truststore";  // truststore

	/* Debug Defaults */
	// protected static String FB_URL = "https://movowave-debug.firebaseio.com/";
	// protected static String FB_SECRET = "3HFJlhjThUhC9QrP4zAq4PNcaXH8IWYqM8cCWmnR";

	// protected static String DB_URL = "jdbc:mysql://173.194.247.177:3306/movogroups?user=root&useSSL=true";
	protected static final boolean USING_GAE_SQL = true;

	/* Production Defaults */
	protected static String FB_URL = "https://movowave.firebaseio.com/";
	protected static String FB_SECRET = "0paTj5f0KHzLBnwIyuc1eEvq4tXZ3Eik9Joqrods";
	protected static String DB_URL = "jdbc:mysql://173.194.241.127:3306/movogroups?useSSL=true&requireSSL=true";
	protected static String keyStorePassword = "r87p-Y?72*uXqW$aSZGU"; // keystore
	protected static String trustStorePassword = "r87p-Y?72*uXqW$aSZGU";  // truststore
	protected static String username = "movogroups";
//	protected static String password = "H8$E=?3*ADXFt4Ld7-jw";
	protected static String password = "movomovo";
	
	
	
	final static protected Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	InputStream file;
	ConcurrentLinkedQueue<StepInterval> sql_message_queue;
	
	MissedSyncCatcher(){
		loadOrCreateQueue();
	}
    
	protected void loadOrCreateQueue(){
		
		//sql_message_queue = new ConcurrentLinkedQueue<StepInterval>();			

		
		try {//try to load user map
			file = new FileInputStream("sql_message_queue.ser");
			InputStream buffer = new BufferedInputStream(file);
		    ObjectInput input = new ObjectInputStream (buffer);
		    
		    sql_message_queue = (ConcurrentLinkedQueue<StepInterval>)input.readObject();
		    input.close();

			System.out.println( "Loaded queue of size:" + sql_message_queue.size());
		    // System.out.println( "Loaded queue of size:" + sql_message_queue.size());
		    
		} catch (IOException | ClassNotFoundException e) { //assume it is our first run and make a new one
			System.out.println( "No insert queue found - creating new one." + e );
			sql_message_queue = new ConcurrentLinkedQueue<StepInterval>();			
		}
	      
	}
	
	public void run() {
		
		conn = null;
		int cur_batch_size = 0;
		long latest_added_batch = 0;

		// System.out.println( "Starting queue listener loop");
		System.out.println( "Starting queue listener loop");
		CallableStatement proc_stmt = null;
		try{
	        while(!Thread.currentThread().isInterrupted()){
	        	
	        	StepInterval si=sql_message_queue.peek();

	        	if(si != null){ // queue isn't empty 
					// System.out.println( "Adding to Batch: "+si.toString());
					System.out.println( "Adding to Batch: "+si.toString());
	
	        		try {

						try {
							synchronized(conn_sync){
							if (conn == null || conn.isClosed()) {
								conn = DriverManager.getConnection(DB_URL+"&noAccessToProcedureBodies=true", username, password);
								CONNECTION_CREATED = System.currentTimeMillis();
								dbConnectionHeartBeat(true);
							}
							}
							if(cur_batch_size++ == 0) proc_stmt = conn.prepareCall("{ call BB_REALTIME_INSERT(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }");

						} catch (SQLException e) {
							// System.out.println( "MBA: NULL CONNECTION.");
							System.out.println( "MBA: NULL CONNECTION.");
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
		    				// System.out.println( "Sending Batch of size: " + cur_batch_size);
							System.out.println( "Sending Batch of size: " + cur_batch_size);
							
							System.out.println("======\nBATCH:\n"+proc_stmt);
							
		    		    	int[] res = proc_stmt.executeBatch();
		    		    	System.out.println("Result: "+Arrays.toString(res));
		    		    	
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
						System.err.println("Could not insert batch.");
						e.printStackTrace();
						System.err.println( e.getMessage());
						e.printStackTrace();
					}
	        	
	        		
	        	} else { // idle while there are no msgs

		        	if( cur_batch_size!=0 && System.currentTimeMillis() - latest_added_batch > SQL_MAX_BATCH_WAIT ){
	    				// System.out.println( "Sending Batch of size: " + cur_batch_size);
						System.out.println( "Sending Batch of size: " + cur_batch_size);

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
					dbConnectionHeartBeat(true);
				}

	        }
		} catch (InterruptedException e) {
	    	// System.out.println( "Stopping message thread...");
			System.out.println( "Stopping message thread...");
			Thread.currentThread().interrupt();
		}  catch (SQLException e) {
			// System.out.println( "Stopping message thread...SQL EXCEPTION");
			System.out.println( "Stopping message thread...SQL EXCEPTION");
			System.err.println( e.getMessage());
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
		
       try {
			conn.close();
		} catch (SQLException e) {e.printStackTrace(); System.err.println( e.getMessage());}
       saveQueue();
       dbConnectionHeartBeat(false);
   }
	
	
	public static void dbConnectionHeartBeat(boolean status){

		try{
			if(!db_log.exists()){
				System.out.println( "Created new heartbeat file." );
				db_log.createNewFile();
			}

			FileWriter fileWriter = new FileWriter(db_log, false);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			bufferedWriter.write( status?String.valueOf(System.currentTimeMillis()):"0" ); // date +"%s"
			bufferedWriter.close();

		} catch(IOException e) {
			System.err.println( "COULD NOT LOG DB Connection HEARTBEAT!!" );
			System.err.println( e.toString() );
		}
	}
	
	private void saveQueue(){
		try(
				OutputStream file = new FileOutputStream("sql_message_queue.ser");
			    OutputStream buffer = new BufferedOutputStream(file);
			    ObjectOutput output = new ObjectOutputStream(buffer);
		    ){
				System.out.println( "Saving queue of size: " + sql_message_queue.size());
				synchronized(sql_message_queue){
					output.writeObject(sql_message_queue);
				}
				output.close();
			} catch (IOException e) {
				System.err.println( e.toString());
				e.printStackTrace();
			}
	}
	
	
    public void processSyncJsonData(String user, String jsonDataForSync){
    	try {
			JSONObject obj = new JSONObject(jsonDataForSync);
			JSONArray head_arr  = obj.names();
			
			String sync_starttime = null;
			String sync_endtime = null;
			
			for (int i = 0; i < head_arr.length(); i++){
				String curKey = head_arr.get(i).toString();
				Object curVal = obj.get(curKey);
//				System.out.println(curKey);
//				System.out.println();
				
				if(curKey.equals("starttime")){
					sync_starttime=(String) curVal;
				}else if(curKey.equals("endtime")){
					sync_endtime=(String) curVal;
				}else if(curKey.equals("steps")){
					
					
					JSONObject years;
					try{
						years = (JSONObject) curVal;
					}catch(ClassCastException e){
						System.err.printf("ERROR: problem casting at %s\nJSON: %s\n\n",user,curVal);
						e.printStackTrace();
						continue;
					}
					JSONArray years_arr = years.names();
					
					for (int y_i = 0; y_i < years_arr.length(); y_i++){ // loop through years
						String curYear = years_arr.get(y_i).toString();
						//System.out.println("curYear: "+ curYear);

						JSONObject months;
						try{
							months = (JSONObject) years.get(curYear);
						}catch(ClassCastException e){
							System.err.printf("ERROR: problem casting at %s\nJSON: %s\n\n",user,years.get(curYear));
							e.printStackTrace();
							continue;
						}
						JSONArray months_arr = months.names();
						
						for (int m_i = 0; m_i < months_arr.length(); m_i++){ // loop through months
							String curMonth = months_arr.get(m_i).toString();
							//System.out.println("curMonth: "+ curMonth);

							JSONObject days;
							try{
								days = (JSONObject) months.get(curMonth);
							}catch(ClassCastException e){
								System.err.printf("ERROR: problem casting at %s\nJSON: %s\n\n",user,months.get(curMonth));
								e.printStackTrace();
								continue;
							}
							JSONArray days_arr = days.names();
							for (int d_i = 0; d_i < days_arr.length(); d_i++){ // loop through days
								String curDay = days_arr.get(d_i).toString();
								//System.out.println("curDay: "+ curDay);
								
								JSONObject hours;
								try{
									hours = (JSONObject) days.get(curDay);
								}catch(ClassCastException e){
									System.err.printf("ERROR: problem casting at %s\nJSON: %s\n\n",user,days.get(curDay));
									e.printStackTrace();
									continue;
								}
								JSONArray hours_arr = hours.names();
								for (int h_i = 0; h_i < hours_arr.length(); h_i++){ // loop through hours
									String curHour = hours_arr.get(h_i).toString();
									//System.out.println("curHour: "+ curHour);
																		
									JSONObject devid;
									try{
										devid = (JSONObject) hours.get(curHour);
									}catch(ClassCastException e){
										System.err.printf("ERROR: problem casting at %s\nJSON: %s\n\n",user,hours.get(curHour));
										e.printStackTrace();
										continue;
									}
									JSONArray devid_arr = devid.names();
									for (int g_i = 0; g_i < devid_arr.length(); g_i++){ // loop through devids(at least I think these are devids?)
										String curDevId = devid_arr.get(g_i).toString();
										//System.out.println("curDevid: "+ curDevId);
										
										try{
										
											StepInterval si = new StepInterval();
											si.year = curYear;
											si.month = curMonth;
											si.day = curDay;
											si.hour = curHour;
											
											si.sync_start_time = sync_starttime;
											si.sync_end_time = sync_endtime;
											
											String start_time_val = null;
											String end_time_val = null;
											
											JSONObject syncVals;
											try{
												syncVals = (JSONObject) devid.get(curDevId);
											}catch(ClassCastException e){
												System.err.printf("ERROR: problem casting at %s\nJSON: %s\n\n",user,devid.get(curDevId));
												e.printStackTrace();
												continue;
											}
											JSONArray keys = syncVals.names();
											for (int k_i = 0; k_i < keys.length(); k_i++){ // loop through sync vals
												String curSyncKey = keys.get(k_i).toString();
												String curSyncVal = syncVals.get(curSyncKey).toString();
												//System.out.println("curSyncKey: "+ curSyncKey);
												//System.out.println("curSyncVal: "+ curSyncVal);
												
												if(curSyncKey.equals("count")){
													si.steps = Integer.parseInt(curSyncVal);
												}else if(curSyncKey.equals("deviceid")){
													si.device_id = curSyncVal;
												}else if(curSyncKey.equals("endtime")){
													end_time_val = curSyncVal;
												}else if(curSyncKey.equals("starttime")){
													start_time_val = curSyncVal;
												}else if(curSyncKey.equals("syncid")){
													si.sync_id = curSyncVal;
												}else{
													System.err.println("Uhoh... No SyncVal:"+ curSyncVal);
												}
												
											}
										
											si.setFirebase_id_fk(user);
											
											//Formated so each has a proper number of 
											
											SimpleDateFormat time_parser = new SimpleDateFormat("'T'HH:mm:ss'Z'");
											try {
												
//												String start_time_val =si.sync_start_time;
												Calendar step_start_date = Calendar.getInstance();
												step_start_date.setTime(time_parser.parse(start_time_val));
												si.hour = String.format("%02d", step_start_date.get(Calendar.HOUR_OF_DAY));
												si.start_minute = String.format("%02d", step_start_date.get(Calendar.MINUTE));
												
//												String end_time_val = si.sync_end_time;
												Calendar step_end_date = Calendar.getInstance();
												step_end_date.setTime(time_parser.parse(end_time_val));
												si.end_minute = String.format("%02d", step_end_date.get(Calendar.MINUTE));
												
											} catch (ParseException e) { e.printStackTrace(); }
											
											System.out.println(si);
											
											sql_message_queue.add(si);
											
											
										
											
											
										}catch(NumberFormatException e){
											System.err.println("ERROR: Could not parse sync for:"+user+"\n"+ jsonDataForSync);
											System.err.println(e);
										}
										
									}

								}
								
								
								
							}
						}
						

					}

					
				}else{
					System.err.println("Uhoh.... no key for:  "+ curKey + " when processing: "+ user +"\nJSON: "+ jsonDataForSync);
				}
			}

		} catch (JSONException e) { e.printStackTrace(); }
    }
    
    
    
    public Set<String> getSqlSyncsForUser(String user_id){
    	/*
    	 * SELECT DISTINCT(sync_id) FROM sync_history WHERE firebase_id_fk='simplelogin:4';
    	 */
    	
    	Set<String> syncs = new HashSet<String>();
    	//Connection conn = null;
    	try {
    		do{
    			synchronized(conn_sync){
    			if (conn == null || conn.isClosed()) {
					conn = DriverManager.getConnection(DB_URL+"&noAccessToProcedureBodies=true", username, password);
					CONNECTION_CREATED = System.currentTimeMillis();
					dbConnectionHeartBeat(true);
				}
    			}
    			
    			if(conn==null){
	    			System.err.println("SQL connection is down");
	    			try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						break;
					}
    			}
    		}while(conn==null);
    		PreparedStatement query_sql_syncs_for_user  = conn.prepareStatement("SELECT sync_id FROM sync_history WHERE firebase_id_fk=? GROUP BY sync_id;");
    		query_sql_syncs_for_user.setString(1, user_id);
    		
    		System.out.println("Query: "+query_sql_syncs_for_user);
    		
    		ResultSet syncs_for_user = query_sql_syncs_for_user.executeQuery();
    		while(syncs_for_user.next()){
    			syncs.add(syncs_for_user.getString(1));
    		}
		} catch (SQLException e) {
			System.err.println("Unable to get Syncs for: " + user_id);
			e.printStackTrace();
		}
		return syncs;
    }
    
    public void checkAllSyncs(){
    	try {
			//get list of users
			Set<String> users = GroupMigrator.getUsers();
			
			//Set<String> users = new HashSet<String>();
			//users.add("simplelogin:4");
			
			for(String user: users){
				// get syncs from the firebase side
				Set<String> fb_syncs = GroupMigrator.getFirebaseSyncsForUser(user);
				Set<String> sql_syncs = getSqlSyncsForUser(user);

				//System.out.println("FB Syncs:\n"+ fb_syncs);
				//System.out.println("SQL Syncs:\n"+ sql_syncs);


				fb_syncs.removeAll(sql_syncs);
				Set<String> missing_syncs = fb_syncs; //just a rename
				System.out.println("Missing Syncs:\n"+ missing_syncs);
				

				
				System.out.printf("\n\nFound %d missing sync for %s\n\n\n",missing_syncs.size(),user);
				
				for(String sync : missing_syncs){
					
					System.out.println("Processing: "+user+ "\t\t"+  sync);
					String json = GroupMigrator.getSyncForUser(user,sync);
					processSyncJsonData(user,json);

				}
				System.out.println("\n\n\n\n\n\n\n\n\n DONE PROCESSING MISSING SYNCS FOR: "+user+" \n\n\n\n\n\n\n\n\n");
				
			}
				

				
			
			
			
			
		} catch (HttpException e) {

			
			e.printStackTrace();
		}
    }
    
    
	public static void main(String[] args) throws org.apache.commons.cli.ParseException {
		
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
	    	System.out.println( "User defined checkpointInterval: "+ CHECKPOINT_INTERVAL);
			System.out.println( "User defined checkpointInterval: " + CHECKPOINT_INTERVAL);
	    }
	    
	    if(cmd.hasOption("getUserTimeout")) {
	    	GETUSER_TIMEOUT = Long.parseLong(cmd.getOptionValue("getUserTimeout"));
	    	System.out.println( "User defined getUserTimeout: "+ CHECKPOINT_INTERVAL);
	    }
	    
	    if (cmd.hasOption("useSSL") || USING_GAE_SQL){
			System.out.println( "User defined useSSL: "+ true);
		}
	    
	    if (cmd.hasOption("useSSL") || USING_GAE_SQL){
			// System.out.println( "User defined useSSL: "+ true);
			System.out.println( "User defined useSSL: "+ true);
		}
		
		if (cmd.hasOption("keyStore")){
			System.setProperty("javax.net.ssl.keyStore",			cmd.getOptionValue("keyStore"));
			System.out.println( "User defined keyStore: "+ cmd.getOptionValue("keyStore"));
		}else{
			System.setProperty("javax.net.ssl.keyStore",			System.getProperty("user.dir")+"/keystore");
			System.out.println( "No keyStore set.");
		}
		
		if (cmd.hasOption("keyStorePass")){
			System.setProperty("javax.net.ssl.keyStorePassword",	cmd.getOptionValue("keyStorePass"));
			System.out.println( "User defined keyStorePass: "+ cmd.getOptionValue("keyStorePass"));
		}else{
			// System.setProperty("javax.net.ssl.keyStorePassword",	"movomovo");
			System.setProperty("javax.net.ssl.keyStorePassword",	keyStorePassword);
			System.out.println( "No keyStore pass set.");
		}
		
		if (cmd.hasOption("trustStore")){
			System.setProperty("javax.net.ssl.trustStore",			cmd.getOptionValue("trustStore"));
			System.out.println( "User defined trustStore: "+ cmd.getOptionValue("trustStore"));
		}else{
			System.setProperty("javax.net.ssl.trustStore",			System.getProperty("user.dir")+"/truststore");
			System.out.println( "No truststore set.");
		}
		
		if (cmd.hasOption("trustStorePass")){
			System.setProperty("javax.net.ssl.trustStorePassword",	cmd.getOptionValue("trustStorePass"));
			System.out.println( "User defined trustStorePassword: "+ cmd.getOptionValue("trustStorePass"));
		}else{
			// System.setProperty("javax.net.ssl.trustStorePassword",	"movomovo");
			System.setProperty("javax.net.ssl.trustStorePassword",	trustStorePassword);
			System.out.println( "No truststore pass set.");
		}

		try {
			Class.forName("com.mysql.jdbc.Driver");
		}catch (ClassNotFoundException e) {e.printStackTrace();}

		if (cmd.hasOption("FirebaseURL")){
			FB_URL = cmd.getOptionValue("FirebaseURL");
			System.out.println( "User defined FirebaseURL: "+ cmd.getOptionValue("FirebaseURL"));
		}
		
		if (cmd.hasOption("FirebaseSecret")){
			FB_SECRET = cmd.getOptionValue("FirebaseSecret");
			System.out.println( "User defined FirebaseSecret: "+ cmd.getOptionValue("FirebaseSecret"));
		}
		
		if (cmd.hasOption("MysqlURL")){
			DB_URL = cmd.getOptionValue("MysqlURL");
			System.out.println( "User defined MysqlURL: "+ cmd.getOptionValue("MysqlURL"));

		}
		
		if (cmd.hasOption("sqlBatchSize")){
			SQL_BATCH_SIZE = Integer.parseInt(cmd.getOptionValue("sqlBatchSize"));
			System.out.println( "User defined sqlBatchSize: "+ cmd.getOptionValue("sqlBatchSize"));
		}
		
		if (cmd.hasOption("sqlBatchDelay")){
			SQL_BATCH_DELAY = Integer.parseInt(cmd.getOptionValue("sqlBatchDelay"));
			System.out.println( "User defined sqlBatchDelay: "+ cmd.getOptionValue("sqlBatchDelay"));
		}
		
		if (cmd.hasOption("sqlMaxBatchWait")){
			SQL_MAX_BATCH_WAIT = Integer.parseInt(cmd.getOptionValue("sqlMaxBatchWait"));
			System.out.println( "User defined sqlMaxBatchWait: "+ cmd.getOptionValue("sqlMaxBatchWait"));
		}
		
		
		MissedSyncCatcher msc = new MissedSyncCatcher();
		
		

		//////////////
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
	        public void run() {
	            try {
	                System.out.println( "Shutting down ...");
	                /* Save out Queue */
	                msg_thread.interrupt();
	                Thread.sleep(10000);


	            } catch (InterruptedException e) {
	                e.printStackTrace();
	            }
	        }
	    });
		
		
		// start SQL Queue Thread
		msg_thread = new Thread(msc);
		msg_thread.start();
		
		msc.checkAllSyncs();
		long mainThreadTime = System.currentTimeMillis();

		while(true){

			try {

				if (System.currentTimeMillis() - mainThreadTime > 5000) {
					System.out.println("SQL: "+msc.sql_message_queue.size() + " sql inserts to be processed");
					mainThreadTime = System.currentTimeMillis();
				}


			} catch (Exception e) {
				e.printStackTrace();
				// logger.log( Level.INFO, "Exception in main thread: " + e.getMessage());
				System.err.println("Exception in main thread: " + e.getMessage());
				e.printStackTrace();
			}
			
		}
	}

}
