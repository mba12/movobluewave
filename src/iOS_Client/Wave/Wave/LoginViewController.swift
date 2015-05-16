//
//  LoginViewController.swift
//  Wave
//
//  Created by Phil Gandy on 5/11/15.
//  Copyright (c) 2015 Phil Gandy. All rights reserved.
//

import Foundation
import UIKit

class LoginViewController: UIViewController{
    
    
    
    
    @IBAction func login(sender: UIButton){
        
        let ref = Firebase(url: "https://ss-movo-wave-v2.firebaseio.com")
        //auth with email and pass that are in the input UI
        
        ref.authUser("philg@sensorstar.com", password: "t",
            withCompletionBlock: { error, authData in
                
                if error != nil {
                    // There was an error logging in to this account
                    NSLog("Login failed")
                } else {
                    // We are now logged in
                    NSLog("We logged in as philg: %@",authData.uid)
//                    self.userID = authData.uid
                    
                }
        })

        
        dismissViewControllerAnimated(true, completion: nil)
        
    }
    
    
    
}