package com.sensorstar.movo;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.firebase.client.AuthData;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.Firebase.AuthResultHandler;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class ListenerMemoryTest {

	private static final String FB_URL = "https://ss-movo-wave-v2.firebaseio.com";
	private static final String FB_SECRET = "jBMdrOwNCfJ37NzcXt6IM4d7AddeojCJg2Z9KnuF";
	
	private static ArrayList<String> getUsers(){
		ArrayList<String> users = new ArrayList<String>();
		
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
			
			int st = response.getStatus();
			int len = response.getLength();
			System.out.println("Status and length: " + st + " :: " + len);
		}

		if (response.getStatus() != 200) {
		   throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}

		String output = response.getEntity(String.class);

		try {
			JSONObject obj = new JSONObject(output);
			JSONArray arr  = obj.names();
			for (int i = 0; i < arr.length(); i++){
				users.add(arr.get(i).toString());
			}

		} catch (JSONException e) { e.printStackTrace(); }
		
		return users;
		
	}
	
	private void addListenersToUsers(Set<String> users){
		
		for(String u: users){
			System.out.println("Adding listener to user: " + u);
			
			
		}
	}
	
	
	
	public static void main(String[] args) throws InterruptedException{
		ArrayList<String> users = getUsers();
		
		int N = 50000;
		for(int i =0; i<N; i++){
			
			final Firebase userRef = new Firebase(FB_URL+"/users/"+users.get(i)+ "/sync");
			System.out.println(userRef.toString());
			userRef.authWithCustomToken(FB_SECRET, new AuthResultHandler() {
			    public void onAuthenticated(AuthData authData) { 
			    	System.out.println("Authenticated.");
			    	
			    	Query sync_query = userRef;
					sync_query.addChildEventListener(new ChildEventListener() {
						
						// When there is a new sync add it to our SQL DB
						public void onChildAdded(DataSnapshot sync, String arg1) {
							System.out.println("onChildAdded");
						}
						
						public void onCancelled(FirebaseError arg0) {}
						public void onChildChanged(DataSnapshot arg0, String arg1) {}
						public void onChildMoved(DataSnapshot arg0, String arg1) {}
						public void onChildRemoved(DataSnapshot arg0) {}
					});
			    }
			    public void onAuthenticationError(FirebaseError firebaseError) { System.out.println("Not Authenticated."); System.exit(1);}
			});
		}
			
			
		while(true){
			Runtime runtime = Runtime.getRuntime();

			NumberFormat format = NumberFormat.getInstance();

			StringBuilder sb = new StringBuilder();
			long maxMemory = runtime.maxMemory();
			long allocatedMemory = runtime.totalMemory();
			long freeMemory = runtime.freeMemory();

			System.out.println("free memory: " + format.format(freeMemory / 1024) + "<br/>");
			System.out.println("allocated memory: " + format.format(allocatedMemory / 1024) + "<br/>");
			System.out.println("max memory: " + format.format(maxMemory / 1024) + "<br/>");
			System.out.println("total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024) + "<br/>");
			System.out.println("");
			Thread.sleep(2000);
		}
	}
	
	
}
