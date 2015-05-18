//
//  UserData.swift
//  Wave
//
//  Created by Phil Gandy on 5/17/15.
//

import Foundation
import CoreData

private var _UserData:UserData? = nil


class UserData {
        //vars
    var currentUID:String? = nil
    var currentEmail:String? = nil
    var currentPW:String? = nil
    var currentBirthDate:String? = nil
    var currentHeight1:Int? = nil
    var currentHeight2:Int? = nil
    var currentWeight:Int? = nil
    var currentGender:String? = nil
    var currentFullName:String? = nil
    var currentUsername:String? = nil
    var currentUserRef:String? = nil

    
    private init(){
        //init vars
        currentUID = "Error"
        currentEmail = "Error"
        currentPW = "Error"
        currentBirthDate = "Error"
        currentHeight1 = 0
        currentHeight2 = 0
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
    
    
    func getCurrentUID() -> String{
        return currentUID!
    }
    func setCurrentUID(String newUID:String){
//        var newItem = NSEntityDescription.insertNewObjectForEntityForName("UserEntry", inManagedObjectContext: self.managedObjectContext!) as! UserEntry
//        //            var countInt = rest2.childSnapshotForPath("count").valueInExportFormat() as? NSNumber
//        newItem.id = newUID
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
//    func getCurrentBirthdate() -> String{
//        return currentUID!
//    }
//    func setCurrentBirthdate(String newUID:String){
//        currentUID = newUID
//    }
    func getCurrentHeight1() -> Int{
        return currentHeight1!
    }
    func setCurrentHeight1(Int newHeight:Int){
        currentHeight1 = newHeight
    }
    func getCurrentHeight2() -> Int{
        return currentHeight1!
    }
    func setCurrentHeight2(Int newHeight:Int){
        currentHeight1 = newHeight
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


