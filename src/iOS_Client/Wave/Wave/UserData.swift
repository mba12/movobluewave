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
    private var currentUserEntry : UserEntry?
    

    static let currentFireBaseRef:String = "https://ss-movo-wave-v2.firebaseio.com/"

    
    private init(){
        //init vars
        
        if (loadDefaultUser()) {
            NSLog("Success loading default user")
            
        } else {
            NSLog("Failed to load default user")
        }
/*
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
  */
     
    }
    
    static func getOrCreateUserData() -> UserData{
        if (_UserData==nil){
          _UserData = UserData()
            
        }
        return _UserData!
    }
    
    static func disposeUserData(){
        _UserData = nil
        
    }
    
    static func getFirebase()->String{
        return currentFireBaseRef
    }
    
    //WARNING: height should be in a single unit!! - RY
    //it is a bad idea to be tracking height across two units (i.e. use feet (double) or meters (double) or inches (int/double) or cm (int or double) but do not split, it will just cause headaches
    //it would be much better, additionally, if we used SI units under the hood... (i.e. meters, kilograms), but imperial units if we must.
    
    
    func loadDefaultUser() -> Bool {
        
        if let currentUser = UserData.getOrCreateCurrentUser() {
            return loadUser(currentUser)
        }
        
        return false
        
    }
    
    func loadUser(user: CurrentUser) -> Bool {
        //unwrap the userentry from the current user pointer!
        if let userentry = user.user {
            return loadUser(userentry)
        }
        
        return false
    }
    
    
    func loadUser(user: UserEntry) -> Bool {
        
        currentUserEntry = user
        if let DBCurrentUser : CurrentUser = UserData.getOrCreateCurrentUser() {            DBCurrentUser.user = user
            UserData.saveContext()
            return true
        }

        return false
    }
    
    
    static func getOrCreateCurrentUser() -> CurrentUser? {
        var createNewCurrentUser = false
        let fetchRequest = NSFetchRequest(entityName: "CurrentUser")
        if let fetchResults = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [CurrentUser] {
            if (fetchResults.count == 1) {
                return fetchResults[0]
            } else if (fetchResults.count > 1) {
                var toRtn = fetchResults[0]
                clearExcessItems(fetchResults)
                return toRtn
            } else {
                createNewCurrentUser = true
            }
            
        } else {
            //then
            createNewCurrentUser = true
        }
        
        if (createNewCurrentUser) {
            if let newCurrentUser : CurrentUser = NSEntityDescription.insertNewObjectForEntityForName("CurrentUser", inManagedObjectContext: (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!) as? CurrentUser {
            
                UserData.saveContext()
                return newCurrentUser
            }
            
        }
        
        //safety catchall, should not be reached unless there is something wrong with the managed context
        return nil
    }
    
    static func saveContext() {
        (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.save(nil)
    }
    
    func createUser(email:String, pw:String, uid:String?, birth:NSDate?, heightfeet:Int?, heightinches:Int?, weightlbs:Int?, gender:String?, fullName:String?, user:String?, ref:String) -> UserEntry {
        
        
        let appDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
        let managedContext = appDelegate.managedObjectContext
        var newItem = NSEntityDescription.insertNewObjectForEntityForName("UserEntry", inManagedObjectContext: appDelegate.managedObjectContext!) as! UserEntry
        
        newItem.id = uid
        newItem.email = email
        newItem.pw = pw
        newItem.birthdate = birth
        if let hf = heightfeet {
            newItem.heightfeet = Int16(hf)
        }
        if let hi = heightinches {
            newItem.heightinches = Int16(hi)
        }
        if let w = weightlbs {
            newItem.weight = Int16(w)
        }
        newItem.gender = gender
        newItem.fullname = fullName
        newItem.username = user
        newItem.reference = ref
        
        appDelegate.managedObjectContext!.save(nil)
        return newItem
        
    }
//    
//    func logInDifferentUser(){
//        if let uid = UserData.getOrCreateUserData().getCurrentUID() {
//            let predicate = NSPredicate(format:"%@ == id",uid)
//            let fetchRequestDupeCheck = NSFetchRequest(entityName: "UserEntry")
//            fetchRequestDupeCheck.predicate = predicate
//            if let fetchResults = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequestDupeCheck, error: nil) as? [UserEntry] {
//                if(fetchResults.count == 1){
//                    
//                    
//                    
//                    var nameUp = ["currentFullName": String(fetchResults[0].fullname)]
//                    var birthTime:NSTimeInterval = fetchResults[0].birthdate.timeIntervalSince1970
//                    var birthLong:Int32 = Int32(birthTime);
//                    var birthUp  = ["currentBirthdate" : String(birthLong)] //dates are saved as long on firebase
//                    var heightFtUp = ["currentHeight1": String(fetchResults[0].heightfeet)]
//                    var heightInchesUp = ["currentHeight2": String(fetchResults[0].heightinches)]
//                    var weightUp = ["currentWeight": String(fetchResults[0].weight)]
//                    var genderUp = ["currentGender": String(fetchResults[0].gender)]
//                }
//            }
//        }
//    }
    
    func getCurrentUID() -> String? {
        if let UID = currentUserEntry?.id {
            return UID
        }
        return nil
    }
    
    func setCurrentUID(uid:String) {
        if let cue : UserEntry = currentUserEntry {
            cue.id = uid
            UserData.saveContext()
        }
    }
    
    func getCurrentEmail() -> String? {
        if let email = currentUserEntry?.email {
            return email
        }
        return nil
    }
    func setCurrentEmail(email:String) {
        if let cue : UserEntry = currentUserEntry {
            cue.email = email
            UserData.saveContext()
        }
    }
    func getCurrentPW() -> String? {
        if let pw = currentUserEntry?.pw {
            return pw
        }
        return nil
        
    }
    func setCurrentPW(password:String){
        if let cue : UserEntry = currentUserEntry {
            cue.pw = password
            UserData.saveContext()
        }
    }
    //date object
    func getCurrentBirthdate() -> NSDate? {
        if let bd = currentUserEntry?.birthdate {
            return bd
        }
        return nil
        
    }
    func setCurrentBirthdate(birthDate:NSDate){
        if let cue : UserEntry = currentUserEntry {
            cue.birthdate = birthDate
            UserData.saveContext()
        }
    }
    
    func getCurrentHeightFeet() -> Int? {
        if let hf = currentUserEntry?.heightfeet {
            return Int(hf)
        }
        return nil
        
    }
    func setCurrentHeightFeet(heightfeet:Int) {
        if let cue : UserEntry = currentUserEntry {
            cue.heightfeet = Int16(heightfeet)
            UserData.saveContext()
        }
    }
    func getCurrentHeightInches() -> Int? {
        if let hi = currentUserEntry?.heightinches {
            return Int(hi)
        }
        return nil
        
    }
    func setCurrentHeightInches(heightinches:Int){
        if let cue : UserEntry = currentUserEntry {
            cue.heightinches = Int16(heightinches)
            UserData.saveContext()
        }
    }
    func getCurrentWeight() -> Int? {
        if let wlbs = currentUserEntry?.weight {
            return Int(wlbs)
        }
        return nil
        
    }
    func setCurrentWeight(weightlbs:Int){
        if let cue : UserEntry = currentUserEntry {
            cue.weight = Int16(weightlbs)
            UserData.saveContext()
        }
    }
    func getCurrentGender() -> String? {
        if let g = currentUserEntry?.gender {
            return g
        }
        return nil
        
    }
    func setCurrentGender(gender:String){
        if let cue : UserEntry = currentUserEntry {
            cue.gender = gender
            UserData.saveContext()
        }
    }
    func getCurrentName() -> String? {
        if let fn = currentUserEntry?.fullname {
            return fn
        }
        return nil
        
    }
    func setCurrentName(name:String){
        if let cue : UserEntry = currentUserEntry {
            cue.fullname = name
            UserData.saveContext()
        }
    }
    
    func getCurrentUserName() -> String? {
        if let un = currentUserEntry?.username {
            return un
        }
        return nil
        
    }
    func setCurrentUsername(username:String){
        if let cue : UserEntry = currentUserEntry {
            cue.username = username
            UserData.saveContext()
        }
    }
    func getCurrentUserRef() -> String? {
        if let ref = currentUserEntry?.reference {
            return ref
        }
        return nil
    }
    func setCurrentUserRef(ref:String){
        if let cue : UserEntry = currentUserEntry {
            cue.reference = ref
            UserData.saveContext()
        }
    }

    
}


