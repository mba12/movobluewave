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
    }
    
    
    
    @IBOutlet weak var saveChanges: UIButton!
    
    
    @IBAction func saveChanges(sender: UIButton){
        var ref = UserData.getOrCreateUserData().getFirebase()
        ref = ref + "users/"
        ref = ref + UserData.getOrCreateUserData().getCurrentUID()
        ref = ref + "/"
        ref = ref + "metadata/"
        
        var fbMetadataRef = Firebase(url: ref)
        
        var nameUp = ["currentFullName": String(fullName.text)]
        var birthUp  = ["currentBirthdate" : String("Error")]
        var heightFtUp = ["currentHeight1": String(heightFt.text)]
        var heightInchesUp = ["currentHeight2": String(heightInches.text)]
        var weightUp = ["currentWeight": String(weight.text)]
        var genderUp = ["currentGender": String("Gender")]
        
        NSLog("Updaing values at %@",ref)
        fbMetadataRef.updateChildValues(nameUp)
        fbMetadataRef.updateChildValues(birthUp)
        fbMetadataRef.updateChildValues(heightFtUp)
        fbMetadataRef.updateChildValues(heightInchesUp)
        fbMetadataRef.updateChildValues(weightUp)
        fbMetadataRef.updateChildValues(genderUp)
        
//        uploadMetadataToFirebase(ref, nameUp)
        
    }
    
    
    @IBAction func cancel(sender: UIButton){
        dismissViewControllerAnimated(true, completion: nil)

    }
    
 
    
    
}

