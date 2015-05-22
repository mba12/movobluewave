//
//  UserData.swift
//  Wave
//
//  Created by Phil Gandy on 5/17/15.
//

import Foundation
import CoreData
import UIKit

private var _UserData:UserData? = nil


class UserData {
        //vars
    var currentUID:String? = nil
    var currentEmail:String? = nil
    var currentPW:String? = nil
    var currentBirthDate:NSDate? = nil
    var currentHeightFeet:Int? = nil
    var currentHeightInches:Int? = nil
    var currentWeight:Int? = nil
    var currentGender:String? = nil
    var currentFullName:String? = nil
    var currentUsername:String? = nil
    var currentUserRef:String? = nil
    static let currentFireBaseRef:String? = "https://ss-movo-wave-v2.firebaseio.com/"

    
    private init(){
        //init vars
        currentUID = "Error"
        currentEmail = "Error"
        currentPW = "Error"
        currentBirthDate = nil
        currentHeightFeet = 0
        currentHeightInches = 0
        currentWeight = 0
        currentGender = "Error"
        currentFullName = "Error"
        currentUsername = "Error"
        currentUserRef = "Error"
        
     
    }
    
    static func getOrCreateUserData() -> UserData{
        if (_UserData==nil){
          _UserData = UserData()
            
        }
        return _UserData!
    }
    
    static func disposeUserData(){
        _UserData = nil
        
        var myNSInt : NSInteger = NSInteger(Int(1))
        var myNSString : NSString = NSString(string: String("Hello world"))
        var myDateExample : NSDate = NSDateFormatter().dateFromString("2015-05-13T03:40:00Z")!
    }
    
    static func getFirebase()->String{
        return currentFireBaseRef!
    }
    func createUser(String uid:String, String email:String, String pw:String, NSDate birth:NSDate, Int height1:Int, Int height2:Int, Int weight:Int, String gender:String, String fullName:String, String user:String, String ref:String){
        let appDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
        let managedContext = appDelegate.managedObjectContext
        var newItem = NSEntityDescription.insertNewObjectForEntityForName("UserEntry", inManagedObjectContext: appDelegate.managedObjectContext!) as! UserEntry
        
        newItem.id = uid
        newItem.email = email
        newItem.pw = pw
        newItem.birthdate = birth
        newItem.heightfeet = Int16(height1)
        newItem.heightinches = Int16(height2)
        newItem.weight = Int16(weight)
        newItem.gender = gender
        newItem.fullname = fullName
        newItem.username = user
        newItem.reference = ref
        
        appDelegate.managedObjectContext!.save(nil)

        setCurrentUID(String: uid)
        setCurrentEmail(String: email)
        setCurrentPW(String: pw)
        setCurrentBirthdate(NSDate: birth)
        setCurrentHeightFeet(Int: height1)
        setCurrentHeightInches(Int: height2)
        setCurrentWeight(Int: weight)
        setCurrentGender(String: gender)
        setCurrentName(String: fullName)
        setCurrentUsername(String: user)
        setCurrentUserRef(String: ref)
        
        
        
    }
    
    func getCurrentUID() -> String{
        return currentUID!
    }
    func setCurrentUID(String newUID:String){
        
        currentUID = newUID
    }
    
    func getCurrentEmail() -> String{
        return currentEmail!
    }
    func setCurrentEmail(String newEmail:String){
        currentEmail = newEmail
    }
    func getCurrentPW() -> String{
        return currentPW!
    }
    func setCurrentPW(String newPW:String){
        currentPW = newPW
    }
    //date object
    func getCurrentBirthdate() -> NSDate{
        return currentBirthDate!
    }
    func setCurrentBirthdate(NSDate newDate:NSDate){
        currentBirthDate = newDate
    }
    func getCurrentHeightFeet() -> Int{
        return currentHeightFeet!
    }
    func setCurrentHeightFeet(Int newHeight:Int){
        currentHeightFeet = newHeight
    }
    func getCurrentHeightInches() -> Int{
        return currentHeightInches!
    }
    func setCurrentHeightInches(Int newHeight:Int){
        currentHeightInches = newHeight
    }
    func getCurrentWeight() -> Int{
        return currentWeight!
    }
    func setCurrentWeight(Int newWeight:Int){
        currentWeight = newWeight
    }
    func getCurrentGender() -> String{
        return currentGender!
    }
    func setCurrentGender(String newGender:String){
        currentGender = newGender
    }
    func getCurrentName() -> String{
        return currentFullName!
    }
    func setCurrentName(String newName:String){
        currentFullName = newName
    }
    func getCurrentUserName() -> String{
        return currentUsername!
    }
    func setCurrentUsername(String newUsername:String){
        currentUsername = newUsername
    }
    func getCurrentUserRef() -> String{
        return currentUserRef!
    }
    func setCurrentUserRef(String newUserRef:String){
        currentUserRef = newUserRef
    }
    
    
    
    
}


