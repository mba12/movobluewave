//
//  passwordResetAlert.swift
//  Wave
//
//  Created by Rudy Yukich on 6/9/15.
//
//

import Foundation
import UIKit


class PasswordResetAlert: ResetPasswordDelegate, PasswordChangeDelegate {
    static var passwordResetEmail : UITextField?
    static var passwordResetPass : UITextField?
    
    static func resetPasswordDialog(viewcontroller: UIViewController) {
        let alert : UIAlertController = UIAlertController(title: "Forgot Password", message: nil, preferredStyle: UIAlertControllerStyle.Alert)
        
        
        alert.addTextFieldWithConfigurationHandler({ emailTextField in
            emailTextField.placeholder = "Please enter email address"
            emailTextField.font =  UIFont(name: "Gotham", size: 14)
            PasswordResetAlert.passwordResetEmail = emailTextField
        })
        
        alert.addAction(UIAlertAction(title: "OK", style: UIAlertActionStyle.Default, handler: { alert in
            //handle email address
            var setEmail = false
            if let email = PasswordResetAlert.passwordResetEmail?.text {
                if (isValidEmail(email)) {
                    //then perform password reset
                    resetUserPassword(email, delegate: PasswordResetAlert())
                    setEmail = true
                }
                
            }
            
            if (!setEmail) {
                //present error message
                let error = UIAlertController(title: "Error", message: "Invalid email address, user unknown, or no connection to server", preferredStyle: UIAlertControllerStyle.Alert)
                error.addAction(UIAlertAction(title: "OK", style: UIAlertActionStyle.Default, handler: nil))
                viewcontroller.presentViewController(error, animated: true, completion: nil)
            }
            
        }))
        
        alert.addAction(UIAlertAction(title: "Cancel", style: UIAlertActionStyle.Cancel, handler: nil))
        
        
        viewcontroller.presentViewController(alert, animated: true, completion: nil)
        
        
    }
    static func presentSetPasswordDialog(viewcontroller: UIViewController, userEmail: String, oldPassword: String) {
        
        let alert : UIAlertController = UIAlertController(title: "Change Password", message: nil, preferredStyle: UIAlertControllerStyle.Alert)
        
        
        alert.addTextFieldWithConfigurationHandler({ passwordTextField in
            passwordTextField.placeholder = "Please enter new password"
            passwordTextField.font =  UIFont(name: "Gotham", size: 14)
            passwordTextField.secureTextEntry = true
            PasswordResetAlert.passwordResetPass = passwordTextField
        })
        
        alert.addAction(UIAlertAction(title: "OK", style: UIAlertActionStyle.Default, handler: { alert in
            //handle email address
            var setPassword = false
            if let pass = PasswordResetAlert.passwordResetPass?.text {
//WARN: Password requirements check?
                //then perform password reset
                changeUserPassword(userEmail, oldPassword: oldPassword, newPassword: pass, delegate: PasswordResetAlert())
                setPassword = true
                
                
            }
            
            if (!setPassword) {
                //present error message
                let error = UIAlertController(title: "Error", message: "Failed to set new password.", preferredStyle: UIAlertControllerStyle.Alert)
                error.addAction(UIAlertAction(title: "OK", style: UIAlertActionStyle.Default, handler: nil))
                viewcontroller.presentViewController(error, animated: true, completion: nil)
            }
            
        }))
        
        viewcontroller.presentViewController(alert, animated: true, completion: nil)
        
    }
    
    func resetPassword(success: Bool) {
        if (success) {
            //present success message
            let error = UIAlertController(title: "Success", message: "Please check your email to receive your temporary password.", preferredStyle: UIAlertControllerStyle.Alert)
            error.addAction(UIAlertAction(title: "OK", style: UIAlertActionStyle.Default, handler: nil))
            
            if let controller = getDisplayedViewController() {
                controller.presentViewController(error, animated: true, completion: nil)
            }
            
            
        } else {
            //present error message
            let error = UIAlertController(title: "Error", message: "Invalid email address, user unknown, or no connection to server.", preferredStyle: UIAlertControllerStyle.Alert)
            error.addAction(UIAlertAction(title: "OK", style: UIAlertActionStyle.Default, handler: nil))
            
            if let controller = getDisplayedViewController() {
                controller.presentViewController(error, animated: true, completion: nil)
            }
            
        }
        
    }
    
    func passwordChanged(success: Bool) {
        if (success) {
            //present success
            let error = UIAlertController(title: "Success", message: "Please login with your new password.", preferredStyle: UIAlertControllerStyle.Alert)
            error.addAction(UIAlertAction(title: "OK", style: UIAlertActionStyle.Default, handler: nil))
            
            if let controller = getDisplayedViewController() {
                controller.presentViewController(error, animated: true, completion: nil)
            }
            
        } else {
            //present error message
            let error = UIAlertController(title: "Error", message: "Invalid email address, user unknown, or no connection to server.", preferredStyle: UIAlertControllerStyle.Alert)
            error.addAction(UIAlertAction(title: "OK", style: UIAlertActionStyle.Default, handler: nil))
            
            if let controller = getDisplayedViewController() {
                controller.presentViewController(error, animated: true, completion: nil)
            }
            
        }
        
    }
}

