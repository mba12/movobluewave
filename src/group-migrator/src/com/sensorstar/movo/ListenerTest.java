package com.sensorstar.movo;

import com.firebase.client.AuthData;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.firebase.client.Firebase.AuthResultHandler;

public class ListenerTest {


	private static final String FB_URL = "https://ss-movo-wave-v2.firebaseio.com";
	private static final String FB_SECRET = "jBMdrOwNCfJ37NzcXt6IM4d7AddeojCJg2Z9KnuF";
	
	public static void main(String[] args) {

		
		final Firebase dummy_location = new Firebase(FB_URL+"/blah");
		System.out.println(dummy_location.toString());
		
		dummy_location.authWithCustomToken(FB_SECRET, new AuthResultHandler() {
		    public void onAuthenticated(AuthData authData) { 
		    	System.out.println("Authenticated.");

		    	
		    	Query sync_query = dummy_location;

				sync_query.addChildEventListener(new ChildEventListener() {
					
					// When there is a new sync add it to our SQL DB
					public void onChildAdded(DataSnapshot sync, String arg1) {
						System.out.println("Dummy added at location -  " + sync.getKey() +"\n"+ sync.getValue().toString());
						
						
					}
					
					public void onCancelled(FirebaseError arg0) {}
					public void onChildChanged(DataSnapshot sync, String arg1) {
						System.out.println("Dummy changed at location -  " + sync.getKey() +"\n"+ sync.getValue().toString());
					}
					public void onChildMoved(DataSnapshot arg0, String arg1) {}
					public void onChildRemoved(DataSnapshot arg0) {}
				});
		    }
		    public void onAuthenticationError(FirebaseError firebaseError) { System.err.println("Not Authenticated."); }
		});
		
		
		while(true){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
