//
//  MyProfileViewController.swift
//  Wave
//
//  Created by Phil Gandy on 5/11/15.
//

import Foundation
import UIKit

class MyProfileViewController:  KeyboardSlideViewController, UIPickerViewDataSource, UIPickerViewDelegate, ImageUpdateDelegate,  UIImagePickerControllerDelegate, UINavigationControllerDelegate, ImageSourceSelectionDelegate {
    @IBOutlet weak var cancel: UIButton!
    
    @IBOutlet weak var fullName: UITextField!
    
    
    @IBOutlet weak var birthdate: UITextField!
    
    @IBOutlet weak var heightInches: UITextField!
    @IBOutlet weak var heightFt: UITextField!
    @IBOutlet weak var weight: UITextField!
    
    @IBOutlet weak var gender: UITextField!
    
    @IBOutlet weak var cancelButton: UIButton!
    
    @IBOutlet weak var profilePicture: UIImageView!
    
    var genderPicker : UIPickerView
    var genderPickerData = ["Female","Male"]
    
    var datePicker : UIDatePicker
    var datePickerToolbar : UIToolbar
    var datePickerFirstResponder : Bool = false
    var genderPickerFirstResponder : Bool = false

    var birthdateDate : NSDate!

    required init(coder aDecoder: NSCoder) {
        genderPicker = UIPickerView()
        datePicker = UIDatePicker()
        datePickerToolbar = UIToolbar()
        super.init(coder: aDecoder)
    }
    
    override func viewDidLoad() {
  	  super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        
        if let fn = UserData.getOrCreateUserData().getCurrentFullName() {
            if (fn != "Error") {
                fullName.text = fn
            } else {
                //fullName.placeholder = "Add"
            }
        }
        
        if let hf = UserData.getOrCreateUserData().getCurrentHeightFeet() {
            heightFt.text = String(Int(hf))
        
        }
        
        if let hi = UserData.getOrCreateUserData().getCurrentHeightInches() {
            heightInches.text = String(Int(hi))
        }
        
        if let w = UserData.getOrCreateUserData().getCurrentWeight() {
            weight.text = String(Int(w))
        }

        if let g = UserData.getOrCreateUserData().getCurrentGender() {
            if (g != "Error") {
                gender.text = g
            } else {
                //gender.placeholder = "Add"
            }
            
            if (g == "Male") {
                genderPicker.selectRow(1, inComponent: 0, animated: false)
            }
        }
        
        if let bd = UserData.getOrCreateUserData().getCurrentBirthdate() {
            var ndf = NSDateFormatter()
            ndf.dateStyle = NSDateFormatterStyle.MediumStyle
            birthdate.text =   ndf.stringFromDate(bd)
            datePicker.date = bd
            birthdateDate = bd
        } else {
            datePicker.date = NSDate()
            birthdateDate = datePicker.date
        }
        
        
        offsetModifier = -(cancelButton.frame.origin.y - cancelButton.frame.height)
        
        
        //set up gender picker
        genderPicker.dataSource = self
        genderPicker.delegate = self
        gender.inputView = genderPicker
        
        
        //set up birthdate picker
        datePicker.addTarget(self, action: Selector("dateSelection:"), forControlEvents: UIControlEvents.ValueChanged)
        datePicker.datePickerMode = UIDatePickerMode.Date
        //datePicker.maximumDate = NSDate().dateByAddingTimeInterval(-60*60*24) //yesterday
        birthdate.inputView = datePicker
        birthdate.addTarget(self, action: Selector("birthdateResponder:"), forControlEvents: UIControlEvents.EditingDidBegin)
        birthdate.addTarget(self, action: Selector("birthdateResponderEnd:"), forControlEvents: UIControlEvents.EditingDidEnd)
        
        
        gender.addTarget(self, action: Selector("genderResponder:"), forControlEvents: UIControlEvents.EditingDidBegin)
        gender.addTarget(self, action: Selector("genderResponderEnd:"), forControlEvents: UIControlEvents.EditingDidEnd)
        
        
        datePickerToolbar.sizeToFit()
        UserData.getImageForDate(nil, callbackDelegate: self, thumbnail: false)
        
        
    }
    
    override func viewWillAppear(animated: Bool) {
        super.viewWillAppear(animated)
        datePickerFirstResponder = false
        genderPickerFirstResponder = false
        UserData.getImageForDate(nil, callbackDelegate: self, thumbnail: false)
    }
    
    
    
    @IBOutlet weak var saveChanges: UIButton!
    
    
    @IBAction func saveChanges(sender: UIButton){
        //call the sets on all of the name changes
        var validation = true

        
        if (!isValidBirthDate(birthdateDate)) {
            validation = false
        }
        
        if (validation) {
            UserData.getOrCreateUserData().setCurrentFullName(fullName.text)
            UserData.getOrCreateUserData().setCurrentHeightFeet(heightFt.text.toInt()!)
            UserData.getOrCreateUserData().setCurrentHeightInches(heightInches.text.toInt()!)
            UserData.getOrCreateUserData().setCurrentWeight(weight.text.toInt()!)
            UserData.getOrCreateUserData().setCurrentGender(gender.text)
            UserData.getOrCreateUserData().setCurrentBirthdate(datePicker.date)
            UserData.getOrCreateUserData().saveMetaDataToFirebase()
            dismissViewControllerAnimated(true, completion: nil)
        } else {
            let alertController = UIAlertController(title: "Error", message:
                "Invalid profile information, please correct and try again", preferredStyle: UIAlertControllerStyle.Alert)
            alertController.addAction(UIAlertAction(title: "Dismiss", style: UIAlertActionStyle.Default,handler: nil))
            
            self.presentViewController(alertController, animated: true, completion: nil)
            
        }
    }
    
    
    @IBAction func cancel(sender: UIButton){
        dismissViewControllerAnimated(true, completion: nil)

    }
    
    
    
    
    
    func dateSelection(sender: UIDatePicker) {
        
        println("Got Date")
        
        
    }
    
    
    func birthdateResponder(sender: UITextField) {
        datePickerFirstResponder = true
        
    }
    
    func genderResponder(sender: UITextField) {
        genderPickerFirstResponder = true
    }
    
    func genderResponderEnd(sender: UITextField) {
        genderPickerFirstResponder = false
        datePickerToolbar.removeFromSuperview()
        
    }
    
    func birthdateResponderEnd(sender: UITextField) {
        var ndf = NSDateFormatter()
        ndf.dateStyle = NSDateFormatterStyle.MediumStyle
        birthdate.text =   ndf.stringFromDate(datePicker.date)
        birthdateDate = datePicker.date
        datePickerFirstResponder = false
        datePickerToolbar.removeFromSuperview()
        
    }
    
    func resignDateKeyboard(sender: UIBarButtonItem) {
        if (datePickerFirstResponder) {
            birthdate.resignFirstResponder()
            datePickerToolbar.removeFromSuperview()
        } else if (genderPickerFirstResponder) {
            gender.resignFirstResponder()
            datePickerToolbar.removeFromSuperview()
        }
    }
    
    
    func numberOfComponentsInPickerView(pickerView: UIPickerView) -> Int {
        return 1
    }
    
    func pickerView(pickerView: UIPickerView, numberOfRowsInComponent component: Int) -> Int {
        return genderPickerData.count
    }
    func pickerView(pickerView: UIPickerView, titleForRow row: Int, forComponent component: Int) -> String! {
        return genderPickerData[row]
    }
    
    func pickerView(pickerView: UIPickerView, didSelectRow row: Int, inComponent component: Int) {
        gender.text = genderPickerData[row]
        gender.resignFirstResponder()
    }
    
    override func keyboardWillShow(notification: NSNotification) {
        super.keyboardWillShow(notification)
        println("In keyboard will show")
        
        if (datePickerFirstResponder || genderPickerFirstResponder) {
            datePickerToolbar.removeFromSuperview()
            var keyboardSize = (notification.userInfo?[UIKeyboardFrameBeginUserInfoKey] as? NSValue)?.CGRectValue()
            var windowheight = self.view.frame.height
            datePickerToolbar = UIToolbar(frame: CGRectMake(0, windowheight-keyboardSize!.height-44 , keyboardSize!.width, 44))
            datePickerToolbar.sizeToFit()
            var flex = UIBarButtonItem(barButtonSystemItem: UIBarButtonSystemItem.FlexibleSpace, target: self, action: nil)
            var button = UIBarButtonItem(title: "Done", style: UIBarButtonItemStyle.Done, target:self, action:Selector("resignDateKeyboard:"))
            
            datePickerToolbar.setItems([flex, button], animated: true)
            self.view.addSubview(datePickerToolbar)
            
            if (genderPickerFirstResponder) {
                if let g = UserData.getOrCreateUserData().getCurrentGender() {
                    gender.text = g
                    
                    if (g == "Male") {
                        genderPicker.selectRow(1, inComponent: 0, animated: false)
                    }
                }
            }

            
        }

    }
    
    
    func updatedImage(date: NSDate?, newImage: UIImage?) {
        var setImage = false
        if (date == nil) {
            if let image = newImage {
                //then we have a new profile image
                dispatch_async(dispatch_get_main_queue(),  {
                    self.profilePicture.image = image
                })
                
            } else {
                dispatch_async(dispatch_get_main_queue(),  {
                    self.profilePicture.image = UIImage(named: "user_icon_cir")
                })                    
            }
            
        }
        
    }
    
    @IBAction func updateProfilePictureClick(sender: AnyObject) {
        ImageSourceSelection.pickImageSource(self, delegate: self, location: self.profilePicture.frame)
    }
    
    
    func didSelectSource(useCamera : Bool) {
        var imagePicker = UIImagePickerController()
        if (useCamera) {
            //will need to do an alert view with button options
            if (UIImagePickerController.isSourceTypeAvailable( UIImagePickerControllerSourceType.Camera)) {
                imagePicker.sourceType = UIImagePickerControllerSourceType.Camera
                imagePicker.showsCameraControls = true
            }
        }
        
        imagePicker.delegate = self
        self.presentViewController(imagePicker, animated: true, completion: nil)
        
        
        
    }
    
    
    func imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [NSObject : AnyObject]) {
        
        picker.dismissViewControllerAnimated(true, completion: {
            
            /*
            println("showing spinner")
            var spinner = showSpinner("Uploading Image", "Please wait...")
            
            self.presentViewController(spinner, animated: true, completion: nil)
            */
            
        
            if let image = info[UIImagePickerControllerOriginalImage] as? UIImage {
                UserData.storeImage(image, rawData: nil, date: nil, pushToFirebase: true, callbackDelegate: self)
                    
            }
            
            /*
            spinner.dismissViewControllerAnimated(true, completion: nil)
            */
        })
        
    }
    
}

