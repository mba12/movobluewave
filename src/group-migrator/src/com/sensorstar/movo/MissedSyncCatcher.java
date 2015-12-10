package com.sensorstar.movo;

import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.sun.jersey.api.client.ClientResponse;

public class MissedSyncCatcher {

	
	/* Production Defaults */
    private static String FB_URL = "https://movowave.firebaseio.com/";
    private static String FB_SECRET = "0paTj5f0KHzLBnwIyuc1eEvq4tXZ3Eik9Joqrods";
    
    
    public static void processSyncJsonData(String jsonDataForSync){
    	try {
			JSONObject obj = new JSONObject(jsonDataForSync);
			JSONArray head_arr  = obj.names();
			
			String starttime = null;
			String endtime = null;
			
			for (int i = 0; i < head_arr.length(); i++){
				String curKey = head_arr.get(i).toString();
				Object curVal = obj.get(curKey);
				System.out.println(curKey);
				System.out.println();
				
				if(curKey.equals("starttime")){
					starttime=(String) curVal;
				}else if(curKey.equals("endtime")){
					endtime=(String) curVal;
				}else if(curKey.equals("steps")){
					JSONObject years = (JSONObject) curVal;
					JSONArray years_arr = years.names();
					
					for (int y_i = 0; y_i < years_arr.length(); y_i++){ // loop through years
						String curYear = years_arr.get(y_i).toString();
						System.out.println("curYear: "+ curYear);

						JSONObject months = (JSONObject) years.get(curYear);
						JSONArray months_arr = months.names();
						
						for (int m_i = 0; m_i < months_arr.length(); m_i++){ // loop through months
							String curMonth = months_arr.get(m_i).toString();
							System.out.println("curMonth: "+ curMonth);

							JSONObject days = (JSONObject) months.get(curMonth);
							JSONArray days_arr = days.names();
							for (int d_i = 0; d_i < days_arr.length(); d_i++){ // loop through days
								String curDay = days_arr.get(d_i).toString();
								System.out.println("curDay: "+ curDay);
								
								JSONObject hours = (JSONObject) days.get(curDay);
								JSONArray hours_arr = hours.names();
								for (int h_i = 0; h_i < hours_arr.length(); h_i++){ // loop through hours
									String curHour = hours_arr.get(h_i).toString();
									System.out.println("curHour: "+ curHour);
									
									JSONObject guid = (JSONObject) hours.get(curHour);
									JSONArray guid_arr = guid.names();
									for (int g_i = 0; g_i < guid_arr.length(); g_i++){ // loop through guids(at least I think these are guids?)
										String curGuid = guid_arr.get(g_i).toString();
										System.out.println("curGuid: "+ curGuid);
										
										JSONObject syncVals = (JSONObject) guid.get(curGuid);
										JSONArray keys = syncVals.names();
										for (int k_i = 0; k_i < keys.length(); k_i++){ // loop through sync vals
											String curSyncKey = keys.get(k_i).toString();
											System.out.println("curSyncKey: "+ curSyncKey);
										}
									}

								}
								
								
								
							}
						}
						

					}

					
				}else{
					System.err.println("Uhoh.... no key for:  "+ curKey );
				}
			}

		} catch (JSONException e) { e.printStackTrace(); }
    }
    
	public static void main(String[] args) {
		
		
		try {
			//get list of users
//			Set<String> users = GroupMigrator.getUsers();
			
			Set<String> users = new HashSet<String>();
			users.add("simplelogin:4");
			
			for(String user: users){
				// get syncs from the firebase side
				Set<String> fb_syncs = GroupMigrator.getSyncsForUser(user);
				
				
				System.out.println("Syncs:\n"+ fb_syncs);
				
				
				
				// DOES NOT WORK
				Firebase userSyncRef = new Firebase(FB_URL+"/users/"+user+ "/sync/"+"81B427CF-4E68-4650-9138-A933A46CA536");
				userSyncRef.addListenerForSingleValueEvent(new ValueEventListener(){

					@Override
					public void onCancelled(FirebaseError arg0) {
						// TODO Auto-generated method stub
						System.out.println("onCancelled");						

					}

					@Override
					public void onDataChange(DataSnapshot arg0) {
						System.out.println("key: "+ arg0.getKey());
						System.out.println("value: "+ arg0.getValue());						
					}
					

				});
				
				//String json = GroupMigrator.getSyncForUser("simplelogin:4","81B427CF-4E68-4650-9138-A933A46CA536");
				String json = GroupMigrator.getSyncForUser("simplelogin:4","813A56C5-DBFA-47FB-B791-D95650C3AC86");
				System.out.println("REST Output:\n"+ json);
				processSyncJsonData(json);
				
				
				
			}
				

				
			
			
			
			
		} catch (HttpException e) {

			
			e.printStackTrace();
		}

	}

}
