//
//  LoginViewController.swift
//  Wave
//
//  Created by Phil Gandy on 5/11/15.
//

import Foundation
import UIKit

class LoginViewController: UIViewController{
    
    @IBOutlet weak var emailText: UITextField!
    
    @IBOutlet weak var passText: UITextField!
    
    
    @IBAction func login(sender: UIButton){
        
        let ref = Firebase(url: "https://ss-movo-wave-v2.firebaseio.com")
        //auth with email and pass that are in the input UI
        
        var email = emailText.text
        var password = passText.text
        var emailCheck = false
        var passCheck = false
        if(!(email=="")){
            emailCheck = true
        }
        if(!(password=="")){
            passCheck = true
        }
        
        if(emailCheck && passCheck){
            ref.authUser(email, password: password,
                withCompletionBlock: { error, authData in
                    
                    if error != nil {
                        // There was an error logging in to this account
                        NSLog("Login failed")
                        let alertController = UIAlertController(title: "Error", message:
                            "Login failed, Please Try Again", preferredStyle: UIAlertControllerStyle.Alert)
                        alertController.addAction(UIAlertAction(title: "Dismiss", style: UIAlertActionStyle.Default,handler: nil))
                        
                        self.presentViewController(alertController, animated: true, completion: nil)
                        
                    } else {
                        NSLog("We logged in as %@: %@",email, authData.uid)
                        var vc = self.storyboard?.instantiateViewControllerWithIdentifier("MyLifeViewController") as! MyLifeViewController
                        
                        self.presentViewController(vc, animated: true, completion: nil)
                        
                        
                    }
            })
            
            
            
        }
        
        
        
    }
    
    
    
}