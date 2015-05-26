//
//  MyProfileViewController.swift
//  Wave
//
//  Created by Phil Gandy on 5/11/15.
//

import Foundation
import UIKit

class MyProfileViewController:  UIViewController{
    @IBOutlet weak var cancel: UIButton!
    
    @IBOutlet weak var fullName: UITextField!
    
    
    @IBOutlet weak var birthdate: UITextField!
    
    @IBOutlet weak var heightInches: UITextField!
    @IBOutlet weak var heightFt: UITextField!
    @IBOutlet weak var weight: UITextField!
    
    @IBOutlet weak var gender: UITextField!
    
    
    override func viewDidLoad() {
  	  super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        
        if let fn = UserData.getOrCreateUserData().getCurrentName() {
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
        
        
    }
    
    
    
    @IBOutlet weak var saveChanges: UIButton!
    
    
    @IBAction func saveChanges(sender: UIButton){
        //call the sets on all of the name changes


        UserData.getOrCreateUserData().setCurrentName(fullName.text)
        UserData.getOrCreateUserData().setCurrentHeightFeet(heightFt.text.toInt()!)
        UserData.getOrCreateUserData().setCurrentHeightInches(heightInches.text.toInt()!)
        UserData.getOrCreateUserData().setCurrentWeight(weight.text.toInt()!)
        UserData.getOrCreateUserData().setCurrentGender(gender.text)
        UserData.getOrCreateUserData().saveMetaDataToFirebase()
        
    }
    
    
    @IBAction func cancel(sender: UIButton){
        dismissViewControllerAnimated(true, completion: nil)

    }
    
 
    
    
}

