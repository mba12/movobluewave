//
//  LoginViewController.swift
//  Wave
//
//  Created by Phil Gandy on 5/11/15.
//

import Foundation
import UIKit

class LoginViewController: KeyboardSlideViewController {
    
    @IBOutlet weak var emailText: UITextField!
    
    @IBOutlet weak var passText: UITextField!
    
    
    @IBAction func login(sender: UIButton){
        
        let ref = Firebase(url: UserData.getFirebase())
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
                        var providerData:NSDictionary = authData.providerData
                        let tempPasswordBool = providerData["isTemporaryPassword"] as! Bool
                        
                        if(tempPasswordBool){
                            //true if password is temp, do password reset prompt.
                            PasswordResetAlert.presentSetPasswordDialog(self, userEmail: email, oldPassword: password)
                        }
                        authData.uid
                        
                        
                        /* this logic isn't quite right */
                        /* This is a successful login, but we can assume that the current user may not exist and even if it does, it may not have values set correctly */
                        
                        /* So what do we need to do? */
                        
                        /* 1 - get the user entry that corresponds to this email */
                        if let userentry : UserEntry = fetchUserByEmail(email) {
                            //in this case, the user is an existing user
                            //accept the new password
                            //and attempt to load the user
                            userentry.pw = password
                            UserData.saveContext()
                            UserData.getOrCreateUserData().loadUser(userentry)
                            
                        } else {
                            //in this case, the user does not exist locally
                            //so we need to create a new local user copy
                            //and log that one in
                            
                            //so we should retrieve the user info
                            var stringRef = UserData.getFirebase() + "users/"
                            stringRef = stringRef + authData.uid

                            
                            var userentry = UserData.getOrCreateUserData().createUser(email, pw: password, uid: authData.uid, birth: nil, heightfeet: nil, heightinches: nil, weightlbs: nil, gender: nil, fullName: nil, user: nil, ref: stringRef)
                            
                            UserData.saveContext()
                            
                            UserData.getOrCreateUserData().loadUser(userentry)
                            
                            
                        }
                        
                        
                        /*
                        UserData.getOrCreateUserData().setCurrentUID(authData.uid)
                        UserData.getOrCreateUserData().setCurrentEmail(email)
                        UserData.getOrCreateUserData().setCurrentPW(password)
                        var stringRef = UserData.getFirebase() + "users/"
                        stringRef = stringRef + authData.uid
                        
                        UserData.getOrCreateUserData().setCurrentUserRef(stringRef)
                        */
                        NSLog("We logged in as %@: %@",email, authData.uid)

                        
                        self.performSegueWithIdentifier("tabBar", sender: self)
                        if let application = (UIApplication.sharedApplication().delegate as? AppDelegate) {
                            if let tabbarVC = application.tabBarController {
                                println("setting selected index")
                                tabbarVC.selectedIndex = 1
                            }
                        }
                        UserData.getOrCreateUserData().downloadMetaData()

                        
                                                
                    }
            })
            
            
            
        }
        
        
        
    }
    
    @IBAction func cancelButtonPress(sender: AnyObject) {
        dismissViewControllerAnimated(true, completion: nil)
    }
    
    @IBAction func forgotPasswordButtonPress(sender: AnyObject) {
        PasswordResetAlert.resetPasswordDialog(self)
        
    }
    
}