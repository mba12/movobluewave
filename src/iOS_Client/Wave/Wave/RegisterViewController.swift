//
//  RegisterViewController.swift
//  Wave
//
//  Created by Phil Gandy on 5/11/15.
//

import Foundation
import UIKit

class RegisterViewController: KeyboardSlideViewController, UIPickerViewDelegate {
    
    
    @IBOutlet weak var emailText: UITextField!
    
    @IBOutlet weak var usernameText: UITextField!
    
    @IBOutlet weak var passText: UITextField!
    
    @IBOutlet weak var confirmPassText: UITextField!
    
    @IBOutlet weak var birthdate: UITextField!
    
    @IBOutlet weak var termsAndConditionsTextView: UITextView!
    
    var modalPopover : Bool = false

    var datePicker : UIDatePicker
    var datePickerToolbar : UIToolbar
    var datePickerFirstResponder : Bool = false
    
    var birthdateDate : NSDate!
    
    
    required init(coder aDecoder: NSCoder) {
        datePicker = UIDatePicker()
        datePickerToolbar = UIToolbar()
        super.init(coder: aDecoder)
    }
    
    
    @IBAction func register(sender: UIButton){
        //        dismissViewControllerAnimated(true, completion: nil)
        let ref = Firebase(url: UserData.getFirebase())
        //auth with email and pass that are in the input UI
        
        var email = emailText.text
        var password = passText.text
        var confirmPass = confirmPassText.text
        var username = usernameText.text
        
        var birthday = birthdateDate
        
        var validation = true
        
        if ( !isValidEmail(email) ) {
            validation = false
            println("Email validation failed")
        }
        
//WARN: bad email validation check
//this isn't really a good check for valid email address
//        if((email=="")){
//            validation = false
//        }
// NOTE: see new function above
        
        if((password=="")){
            validation = false
        }
        if ((username=="")) {
            validation = false
        }
        if(password != confirmPass){
            validation = false
        }
        
        if (!isValidBirthDate(birthday)) {
            validation = false
        }
        
        if (validation) {
            ref.createUser(email, password: password,
                withValueCompletionBlock: { error, user in
                    
                    if (error != nil) {
                        // There was an error creating the account
                        // There was an error logging in to this account
                        
                        if (self.modalPopover) {
                            self.modalPopover = false
                            self.dismissViewControllerAnimated(true, completion: {self.presentError()})
                        } else {
                            self.presentError()
                        }
                        
                        
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
                                    
                                    
                                    var userentry = UserData.getOrCreateUserData().createUser(email, pw: password, uid: authData.uid, birth: birthday, heightfeet: nil, heightinches: nil, weightlbs: nil, gender: nil, fullName: nil, user: username, ref: stringRef)
                                    
                                    UserData.saveContext()
                                    
                                    UserData.getOrCreateUserData().loadUser(userentry)
                                    
                                    UserData.getOrCreateUserData().saveMetaDataToFirebase()

                                    let url = NSURL(string: "https://orders.getmovo.com/verify/user-signup?fullname=" + username + "&email=" + email)
                                    
                                    let task = NSURLSession.sharedSession().dataTaskWithURL(url!) {(data, response, error) in
                                        println(NSString(data: data, encoding: NSUTF8StringEncoding))
                                    }
                                    
                                    task.resume()
                                    
                                    /*
                                    // Send User a Validation Email
                                    var sender: RegistrationEmailSender = RegistrationEmailSender()
                                    sender.sendRegistrationEmail(username, em:email)
                                    */
                                    
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
            //present login message
            let loginpop = UIAlertController(title: "Connecting", message: "Please wait...", preferredStyle: UIAlertControllerStyle.Alert)
            self.modalPopover = true
            self.presentViewController(loginpop, animated: true, completion: nil)
            
        } else {
            let alertController = UIAlertController(title: "Error", message:
                "Invalid registration information, Please Try Again", preferredStyle: UIAlertControllerStyle.Alert)
            alertController.addAction(UIAlertAction(title: "Dismiss", style: UIAlertActionStyle.Default,handler: nil))
            
            self.presentViewController(alertController, animated: true, completion: nil)
        }
    }
    
    
    override func viewDidLoad() {
        //set up birthdate picker
        datePicker.addTarget(self, action: Selector("dateSelection:"), forControlEvents: UIControlEvents.ValueChanged)
        datePicker.datePickerMode = UIDatePickerMode.Date
        //datePicker.maximumDate = NSDate().dateByAddingTimeInterval(-60*60*24) //yesterday
        birthdate.inputView = datePicker
        birthdate.addTarget(self, action: Selector("birthdateResponder:"), forControlEvents: UIControlEvents.EditingDidBegin)
        birthdate.addTarget(self, action: Selector("birthdateResponderEnd:"), forControlEvents: UIControlEvents.EditingDidEnd)
        
        datePickerToolbar.sizeToFit()

        offsetModifier = -(birthdate.frame.origin.y + 2*birthdate.frame.height)
        
        let htmlString : NSString = "<center><font face='Gotham' color='white'>By proceeding, you also agree to Movo's <a href='http://www.getmovo.com/terms'>Terms of Service </a> and <a href='http://www.getmovo.com/privacy'>Privacy Policy</a>.</font></center>"

        let encodedString = NSAttributedString(data: htmlString.dataUsingEncoding(NSUnicodeStringEncoding)!, options: [NSDocumentTypeDocumentAttribute: NSHTMLTextDocumentType], documentAttributes: nil, error: nil)
        termsAndConditionsTextView.attributedText = encodedString
    }
    

    
    func dateSelection(sender: UIDatePicker) {
        
        println("Got Date")
        
        
    }
    
    
    func birthdateResponder(sender: UITextField) {
        datePickerFirstResponder = true
        
    }
    
    func birthdateResponderEnd(sender: UITextField) {
        var ndf = NSDateFormatter()
        ndf.dateStyle = NSDateFormatterStyle.MediumStyle
        birthdate.text =   ndf.stringFromDate(datePicker.date)
        birthdateDate = datePicker.date
        if (datePickerFirstResponder) {
                datePickerToolbar.removeFromSuperview()
        }
        datePickerFirstResponder = false
        
        
    }
    
    func resignDateKeyboard(sender: UIBarButtonItem) {
        if (datePickerFirstResponder) {
            birthdate.resignFirstResponder()
            datePickerToolbar.removeFromSuperview()
        }
    }

    
    
    
    override func keyboardWillShow(notification: NSNotification) {
        super.keyboardWillShow(notification)
        println("In keyboard will show")
        
        if (datePickerFirstResponder) {
            datePickerToolbar.removeFromSuperview()
            var keyboardSize = (notification.userInfo?[UIKeyboardFrameBeginUserInfoKey] as? NSValue)?.CGRectValue()
            var windowheight = self.view.frame.height
            datePickerToolbar = UIToolbar(frame: CGRectMake(0, windowheight-keyboardSize!.height-44 , keyboardSize!.width, 44))
            datePickerToolbar.sizeToFit()
            var flex = UIBarButtonItem(barButtonSystemItem: UIBarButtonSystemItem.FlexibleSpace, target: self, action: nil)
            var button = UIBarButtonItem(title: "Done", style: UIBarButtonItemStyle.Done, target:self, action:Selector("resignDateKeyboard:"))
            
            datePickerToolbar.setItems([flex, button], animated: true)
            self.view.addSubview(datePickerToolbar)
            
        }
        
    }
    
    
    @IBAction func cancelButtonPressed(sender: AnyObject) {
        dismissViewControllerAnimated(true, completion: nil)
    }
    
    
    func presentError() {
        NSLog("User Creation failed")
        let alertController = UIAlertController(title: "Error", message:
            "Failed to create account: email address may already be in use, check internet connection and please try again", preferredStyle: UIAlertControllerStyle.Alert)
        alertController.addAction(UIAlertAction(title: "Dismiss", style: UIAlertActionStyle.Default,handler: nil))
        
        self.presentViewController(alertController, animated: true, completion: nil)
        
    }
}

