//
//  KeyboardSlideViewController.swift
//  Wave
//
//  Created by Rudy Yukich on 5/26/15.
//
//

import Foundation
import UIKit

class KeyboardSlideViewController : UIViewController, UITextFieldDelegate {
    
    
    var offsetModifier : CGFloat = 0.0

    override func viewDidAppear(animated: Bool) {
        super.viewDidAppear(animated)
        registerForKeyboardNotifications()
    }
    
    override func viewWillDisappear(animated: Bool) {
        super.viewWillDisappear(animated)
        deregisterForKeyboardNotifications()
    }
    
    
    func registerForKeyboardNotifications() {
        
        NSNotificationCenter.defaultCenter().addObserver(self, selector: Selector("keyboardWillShow:"), name: UIKeyboardWillShowNotification, object: nil)
        
        NSNotificationCenter.defaultCenter().addObserver(self, selector: Selector("keyboardWillBeHidden:"), name: UIKeyboardWillHideNotification, object: nil)
        
        
    }
    
    
    func deregisterForKeyboardNotifications() {
        NSNotificationCenter.defaultCenter().removeObserver(self, name: UIKeyboardWillShowNotification, object: nil)
        NSNotificationCenter.defaultCenter().removeObserver(self, name: UIKeyboardWillHideNotification, object: nil)
    
    }
    
    func keyboardWillShow(notification : NSNotification) {
        if let keyboardSize = (notification.userInfo?[UIKeyboardFrameBeginUserInfoKey] as? NSValue)?.CGRectValue() {
            UIView.beginAnimations(nil, context: nil)
            UIView.setAnimationDuration(0.3)
            var rect = self.view.frame
            
            if ((keyboardSize.height+offsetModifier > 0) ) {
                rect.origin.y -= (keyboardSize.height + offsetModifier)
                rect.size.height += (keyboardSize.height + offsetModifier)
            }
            
            self.view.frame = rect
            UIView.commitAnimations()
        }
    }
    
    func keyboardWillBeHidden(notification : NSNotification) {
        if let keyboardSize = (notification.userInfo?[UIKeyboardFrameBeginUserInfoKey] as? NSValue)?.CGRectValue() {
            UIView.beginAnimations(nil, context: nil)
            UIView.setAnimationDuration(0.3)
            var rect = self.view.frame
            
            if ( (keyboardSize.height+offsetModifier > 0) ) {
                rect.origin.y += (keyboardSize.height + offsetModifier)
                rect.size.height -= (keyboardSize.height + offsetModifier)
            }
            
            self.view.frame = rect
            UIView.commitAnimations()
        }
        
    }
    
    func textFieldShouldReturn(textField: UITextField) -> Bool {
        textField.resignFirstResponder()
        return true
    }
    
    
}
