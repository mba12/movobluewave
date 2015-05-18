//
//  RegisterViewController.swift
//  Wave
//
//  Created by Phil Gandy on 5/11/15.
//  Copyright (c) 2015 Phil Gandy. All rights reserved.
//

import Foundation
import UIKit

class RegisterViewController: UIViewController{
    
    
    @IBOutlet weak var emailText: UITextField!
    
    @IBOutlet weak var usernameText: UITextField!
    
    @IBOutlet weak var passText: UITextField!
    
    @IBOutlet weak var comfPassText: UITextField!
    
    
    
    @IBAction func register(sender: UIButton){
//        dismissViewControllerAnimated(true, completion: nil)
        let ref = Firebase(url: "https://ss-movo-wave-v2.firebaseio.com")
        //auth with email and pass that are in the input UI
        
        var email = emailText.text
        var password = passText.text
        var passComf = comfPassText.text
        var emailCheck = false
        var passCheck = false
        var passComfCheck = false
        
        if(!(email=="")){
            emailCheck = true
        }
        if(!(password=="")){
            passCheck = true
        }
        if(password == passComf){
            passComfCheck = true
        }
//        ref.createUser(<#email: String!#>, password: <#String!#>, withCompletionBlock: <#((NSError!) -> Void)!##(NSError!) -> Void#>)
        
//        if(emailCheck && passCheck && passComfCheck){
//            ref.createUser(email, password: password,
//                withCompletionBlock: { error, authData in
//                    
//                    if error != nil {
//                        // There was an error logging in to this account
//                        NSLog("User Creation failed")
//                        let alertController = UIAlertController(title: "Error", message:
//                            "Create failed, Please Try Again", preferredStyle: UIAlertControllerStyle.Alert)
//                        alertController.addAction(UIAlertAction(title: "Dismiss", style: UIAlertActionStyle.Default,handler: nil))
//                        
//                        self.presentViewController(alertController, animated: true, completion: nil)
//                        
//                    } else {
//                        NSLog("We logged in as %@: %@",email, authData.uid)
//                        var vc = self.storyboard?.instantiateViewControllerWithIdentifier("MyLifeViewController") as! MyLifeViewController
//                        
//                        self.presentViewController(vc, animated: true, completion: nil)
//                        
//                        
//                    }
//            })
        
            
            
//        }
        
        
        
//    }

        
        
    }
    
    
    
}