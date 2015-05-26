//
//  RegisterViewController.swift
//  Wave
//
//  Created by Phil Gandy on 5/11/15.
//

import Foundation
import UIKit

class RegisterViewController: UIViewController{
    
    
    @IBOutlet weak var emailText: UITextField!
    
    @IBOutlet weak var usernameText: UITextField!
    
    @IBOutlet weak var passText: UITextField!
    
    @IBOutlet weak var confirmPassText: UITextField!
    
    
    
    @IBAction func register(sender: UIButton){
        //        dismissViewControllerAnimated(true, completion: nil)
        let ref = Firebase(url: UserData.getFirebase())
        //auth with email and pass that are in the input UI
        
        var email = emailText.text
        var password = passText.text
        var confirmPass = confirmPassText.text
        var emailCheck = false
        var passCheck = false
        var passConfirmCheck = false
        
//WARN: bad email validation check
//this isn't really a good check for valid email address
        if(!(email=="")){
            emailCheck = true
        }
        if(!(password=="")){
            passCheck = true
        }
        if(password == confirmPass){
            passConfirmCheck = true
        }
        ref.createUser(email, password: password,
            withValueCompletionBlock: { error, user in
                
                if (error != nil) {
                    // There was an error creating the account
                    // There was an error logging in to this account
                    NSLog("User Creation failed")
                    let alertController = UIAlertController(title: "Error", message:
                        "Create failed, Please Try Again", preferredStyle: UIAlertControllerStyle.Alert)
                    alertController.addAction(UIAlertAction(title: "Dismiss", style: UIAlertActionStyle.Default,handler: nil))
                    
                    self.presentViewController(alertController, animated: true, completion: nil)
                    
                } else {
                    // We created a new user account
                    
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
                                
                                ///
                                
                                
                                
                                var vc = self.storyboard?.instantiateViewControllerWithIdentifier("MyLifeViewController") as! MyLifeViewController
                                
                                self.presentViewController(vc, animated: true, completion: nil)
                                
                                
                                
                            }
                    })
                }
        })
        
        
        
        
    }
    
    
    
    
    
    @IBAction func cancelButtonPressed(sender: AnyObject) {
        dismissViewControllerAnimated(true, completion: nil)
    }
    
    
    
}

