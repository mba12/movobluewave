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
        fullName.text = "hello"
        var nameTxt = UserData.getOrCreateUserData().getCurrentUserName()
//        var birthUp  = 
        var heightFtTxt = UserData.getOrCreateUserData().getCurrentHeightFeet()
        var heightInchesTxt = UserData.getOrCreateUserData().getCurrentHeightInches()
        var weightTxt = UserData.getOrCreateUserData().getCurrentWeight()
        var genderTxt = UserData.getOrCreateUserData().getCurrentGender()
        
        
        fullName.text = nameTxt
        
        
    }
    
    
    
    @IBOutlet weak var saveChanges: UIButton!
    
    
    @IBAction func saveChanges(sender: UIButton){
        
        saveMetadataToCoreData(fullName.text)
        
    }
    
    
    @IBAction func cancel(sender: UIButton){
        dismissViewControllerAnimated(true, completion: nil)

    }
    
 
    
    
}

