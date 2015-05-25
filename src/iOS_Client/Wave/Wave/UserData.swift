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
    static let currentFireBaseRef:String = "https://ss-movo-wave-v2.firebaseio.com/"

    
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
        return currentFireBaseRef
    }
    
    //WARNING: height should be in a single unit!! - RY
    //it is a bad idea to be tracking height across two units (i.e. use feet (double) or meters (double) or inches (int/double) or cm (int or double) but do not split, it will just cause headaches
    //it would be much better, additionally, if we used SI units under the hood... (i.e. meters, kilograms), but imperial units if we must.
    
    
    func loadUser(user: UserEntry) {
        setCurrentUID(user.id)
        setCurrentEmail(user.email)
        setCurrentPW(user.pw)
        setCurrentBirthdate(user.birthdate)
        setCurrentHeightFeet(Int(user.heightfeet))
        setCurrentHeightInches(Int(user.heightinches))
        setCurrentWeight(Int(user.weight))
        setCurrentGender(user.gender)
        setCurrentName(user.fullname)
        setCurrentUsername(user.username)
        setCurrentUserRef(user.reference)
        
        
    }
    
    
    func createUser(uid:String, email:String, pw:String, birth:NSDate, heightfeet:Int, heightinches:Int, weightlbs:Int, gender:String, fullName:String, user:String, ref:String){
        let appDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
        let managedContext = appDelegate.managedObjectContext
        var newItem = NSEntityDescription.insertNewObjectForEntityForName("UserEntry", inManagedObjectContext: appDelegate.managedObjectContext!) as! UserEntry
        
        newItem.id = uid
        newItem.email = email
        newItem.pw = pw
        newItem.birthdate = birth
        newItem.heightfeet = Int16(heightfeet)
        newItem.heightinches = Int16(heightinches)
        newItem.weight = Int16(weightlbs)
        newItem.gender = gender
        newItem.fullname = fullName
        newItem.username = user
        newItem.reference = ref
        
        appDelegate.managedObjectContext!.save(nil)

    
        
    }
    
    func logInDifferentUser(){
        let predicate = NSPredicate(format:"%@ == id",UserData.getOrCreateUserData().getCurrentUID())
        let fetchRequestDupeCheck = NSFetchRequest(entityName: "UserEntry")
        fetchRequestDupeCheck.predicate = predicate
        if let fetchResults = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequestDupeCheck, error: nil) as? [UserEntry] {
            if(fetchResults.count == 1){
                
                
                
                var nameUp = ["currentFullName": String(fetchResults[0].fullname)]
                var birthTime:NSTimeInterval = fetchResults[0].birthdate.timeIntervalSince1970
                var birthLong:Int32 = Int32(birthTime);
                var birthUp  = ["currentBirthdate" : String(birthLong)] //dates are saved as long on firebase
                var heightFtUp = ["currentHeight1": String(fetchResults[0].heightfeet)]
                var heightInchesUp = ["currentHeight2": String(fetchResults[0].heightinches)]
                var weightUp = ["currentWeight": String(fetchResults[0].weight)]
                var genderUp = ["currentGender": String(fetchResults[0].gender)]
            }
        }
    }
    
    func getCurrentUID() -> String{
        return currentUID!
    }
    func setCurrentUID(uid:String){
        
        currentUID = uid
    }
    
    func getCurrentEmail() -> String{
        return currentEmail!
    }
    func setCurrentEmail(email:String){
        currentEmail = email
    }
    func getCurrentPW() -> String{
        return currentPW!
    }
    func setCurrentPW(password:String){
        currentPW = password
    }
    //date object
    func getCurrentBirthdate() -> NSDate{
        return currentBirthDate!
    }
    func setCurrentBirthdate(birthDate:NSDate){
        currentBirthDate = birthDate
    }
    func getCurrentHeightFeet() -> Int{
        return currentHeightFeet!
    }
    func setCurrentHeightFeet(heightfeet:Int){
        currentHeightFeet = heightfeet
    }
    func getCurrentHeightInches() -> Int{
        return currentHeightInches!
    }
    func setCurrentHeightInches(heightinches:Int){
        currentHeightInches = heightinches
    }
    func getCurrentWeight() -> Int{
        return currentWeight!
    }
    func setCurrentWeight(weightlbs:Int){
        currentWeight = weightlbs
    }
    func getCurrentGender() -> String{
        return currentGender!
    }
    func setCurrentGender(gender:String){
        currentGender = gender
    }
    func getCurrentName() -> String{
        return currentFullName!
    }
    func setCurrentName(name:String){
        currentFullName = name
    }
    func getCurrentUserName() -> String{
        return currentUsername!
    }
    func setCurrentUsername(username:String){
        currentUsername = username
    }
    func getCurrentUserRef() -> String{
        return currentUserRef!
    }
    func setCurrentUserRef(ref:String){
        currentUserRef = ref
    }
    
    
    
    
}


