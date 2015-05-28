//
//  MyProfileViewController.swift
//  Wave
//
//  Created by Phil Gandy on 5/11/15.
//

import Foundation
import UIKit

class MyProfileViewController:  KeyboardSlideViewController, UIPickerViewDataSource, UIPickerViewDelegate {
    @IBOutlet weak var cancel: UIButton!
    
    @IBOutlet weak var fullName: UITextField!
    
    
    @IBOutlet weak var birthdate: UITextField!
    
    @IBOutlet weak var heightInches: UITextField!
    @IBOutlet weak var heightFt: UITextField!
    @IBOutlet weak var weight: UITextField!
    
    @IBOutlet weak var gender: UITextField!
    
    @IBOutlet weak var cancelButton: UIButton!
    
    
    var genderPicker : UIPickerView
    var genderPickerData = ["Female","Male"]
    
    var datePicker : UIDatePicker
    var datePickerToolbar : UIToolbar
    var datePickerFirstResponder : Bool = false


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
            fullName.text = fn
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
            gender.text = g
        }
        
        if let bd = UserData.getOrCreateUserData().getCurrentBirthdate() {
            birthdate.text = bd.description
            datePicker.date = bd
        }
        offsetModifier = -(cancelButton.frame.origin.y - cancelButton.frame.height)
        
        
        //set up gender picker
        genderPicker.dataSource = self
        genderPicker.delegate = self
        gender.inputView = genderPicker
        
        
        //set up birthdate picker
        datePicker.addTarget(self, action: Selector("dateSelection:"), forControlEvents: UIControlEvents.ValueChanged)
        datePicker.datePickerMode = UIDatePickerMode.Date
        datePicker.maximumDate = NSDate().dateByAddingTimeInterval(-60*60*24) //yesterday
        birthdate.inputView = datePicker
        birthdate.addTarget(self, action: Selector("birthdateResponder:"), forControlEvents: UIControlEvents.EditingDidBegin)
        birthdate.addTarget(self, action: Selector("birthdateResponderEnd:"), forControlEvents: UIControlEvents.EditingDidEnd)
        
        datePickerToolbar.sizeToFit()
        
        
        
    }
    
    override func viewWillAppear(animated: Bool) {
        super.viewWillAppear(animated)
        datePickerFirstResponder = false
    }
    
    
    
    @IBOutlet weak var saveChanges: UIButton!
    
    
    @IBAction func saveChanges(sender: UIButton){
        //call the sets on all of the name changes


        UserData.getOrCreateUserData().setCurrentFullName(fullName.text)
        UserData.getOrCreateUserData().setCurrentHeightFeet(heightFt.text.toInt()!)
        UserData.getOrCreateUserData().setCurrentHeightInches(heightInches.text.toInt()!)
        UserData.getOrCreateUserData().setCurrentWeight(weight.text.toInt()!)
        UserData.getOrCreateUserData().setCurrentGender(gender.text)
        UserData.getOrCreateUserData().setCurrentBirthdate(datePicker.date)
        UserData.getOrCreateUserData().saveMetaDataToFirebase()
        
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
    
    func birthdateResponderEnd(sender: UITextField) {
        birthdate.text = datePicker.date.description
        datePickerFirstResponder = false
        
    }
    
    func resignDateKeyboard(sender: UIBarButtonItem) {
        if (datePickerFirstResponder) {
            birthdate.resignFirstResponder()
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
    
}

