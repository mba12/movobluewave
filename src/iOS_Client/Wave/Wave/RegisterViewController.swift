//
//  RegisterViewController.swift
//  Wave
//
//  Created by Phil Gandy on 5/11/15.
//

import Foundation
import UIKit

class RegisterViewController: KeyboardSlideViewController {
    
    
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
                                //in this case, the user does not exist locally
                                //so we need to create a new local user copy
                                //and log that one in
                                
                                //so we should retrieve the user info
                                var stringRef = UserData.getFirebase() + "users/"
                                stringRef = stringRef + authData.uid
                                
                                
                                var userentry = UserData.getOrCreateUserData().createUser(email, pw: password, uid: authData.uid, birth: nil, heightfeet: nil, heightinches: nil, weightlbs: nil, gender: nil, fullName: nil, user: nil, ref: stringRef)
                                
                                UserData.saveContext()
                                
                                UserData.getOrCreateUserData().loadUser(userentry)
                                
                                UserData.getOrCreateUserData().saveMetaDataToFirebase()
                                
                                self.performSegueWithIdentifier("tabBar", sender: self)
                                if let application = (UIApplication.sharedApplication().delegate as? AppDelegate) {
                                    if let tabbarVC = application.tabBarController {
                                        println("setting selected index")
                                        tabbarVC.selectedIndex = 1
                                    }
                                }
                                
    
                                
                            }
                    })
                }
        })
        
        
        
        
    }
    
    
    
    
    
    @IBAction func cancelButtonPressed(sender: AnyObject) {
        dismissViewControllerAnimated(true, completion: nil)
    }
    
    
    
}

