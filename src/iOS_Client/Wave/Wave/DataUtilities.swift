//
//  DataUtilities.swift
//  Wave
//
//  Created by Rudy Yukich on 5/21/15.
//
//

import Foundation
import UIKit
import CoreData

let photoMaximumSizeChunk : Int = 1000000

func YMDLocalToNSDate(year: Int, month: Int, day: Int) -> NSDate? {
    let calendar = NSCalendar.currentCalendar()
    calendar.timeZone = NSTimeZone.localTimeZone()
    let startTimeComponents = NSDateComponents()
    startTimeComponents.setValue(day, forComponent: NSCalendarUnit.Day)
    startTimeComponents.setValue(year, forComponent: NSCalendarUnit.Year)
    startTimeComponents.setValue(month, forComponent: NSCalendarUnit.Month)
    startTimeComponents.setValue(0, forComponent: NSCalendarUnit.Hour)
    startTimeComponents.setValue(0, forComponent: NSCalendarUnit.Minute)
    startTimeComponents.setValue(0, forComponent: NSCalendarUnit.Second)
    
    return calendar.dateFromComponents(startTimeComponents)
}

func YMDGMTToNSDate(year: Int, month: Int, day: Int) -> NSDate? {
    let calendar = NSCalendar.currentCalendar()
    calendar.timeZone = NSTimeZone(forSecondsFromGMT: 0)
    let startTimeComponents = NSDateComponents()
    startTimeComponents.setValue(day, forComponent: NSCalendarUnit.Day)
    startTimeComponents.setValue(year, forComponent: NSCalendarUnit.Year)
    startTimeComponents.setValue(month, forComponent: NSCalendarUnit.Month)
    startTimeComponents.setValue(0, forComponent: NSCalendarUnit.Hour)
    startTimeComponents.setValue(0, forComponent: NSCalendarUnit.Minute)
    startTimeComponents.setValue(0, forComponent: NSCalendarUnit.Second)
    
    return calendar.dateFromComponents(startTimeComponents)
    
    
    
}




func stepsForDayStarting(dateStart: NSDate) -> Int {
    let dateStop = dateStart.dateByAddingTimeInterval(60*60*24); //24hrs
    return getStepsForTimeInterval(dateStart, dateStop: dateStop)
}


func caloriesForDayStarting(dateStart: NSDate) -> Double {
    let dateStop = dateStart.dateByAddingTimeInterval(60*60*24)
    return getCaloriesForTimeInterval(dateStart, dateStop: dateStop)
}

func getStepsForTimeInterval(dateStart:NSDate, dateStop:NSDate) -> Int {
    
    var totalStepsForInterval : Int = 0
    if let uid = UserData.getOrCreateUserData().getCurrentUID() {
        let predicate = NSPredicate(format:"%@ <= starttime AND %@ >= endtime AND %@ == user", dateStart, dateStop, uid)
        
        let fetchRequest = NSFetchRequest(entityName: "StepEntry")
        fetchRequest.predicate = predicate
        if let fetchResults = (try? (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequest)) as? [StepEntry] {
            if(fetchResults.count > 0){
                print("Count %i",fetchResults.count)
                let resultsCount = fetchResults.count
                for(var i=0;i<(resultsCount);i++){
                    //                    println("Adding steps up for %i %i",cellDateNumber, Int(fetchResults[i].count))
                    totalStepsForInterval = totalStepsForInterval + Int(fetchResults[i].count)
                }
            }
        }
        
    }
    
    return totalStepsForInterval

}

func getCaloriesForTimeInterval(dateStart:NSDate, dateStop:NSDate) -> Double {
    
    var totalCalForTimeInterval : Double = 0.0
    if let uid = UserData.getOrCreateUserData().getCurrentUID() {
        let predicate = NSPredicate(format:"%@ <= starttime AND %@ >= endtime AND %@ == user", dateStart, dateStop, uid)
        
        let fetchRequest = NSFetchRequest(entityName: "StepEntry")
        fetchRequest.predicate = predicate
        if let fetchResults = (try? (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequest)) as? [StepEntry] {
            if(fetchResults.count > 0){
                print("Count %i",fetchResults.count)
                var resultsCount = fetchResults.count
                for(var i=0;i<(resultsCount);i++){
                    //                    println("Adding steps up for %i %i",cellDateNumber, Int(fetchResults[i].count))
                    totalCalForTimeInterval += calcCaloriesForTimeInterval(Int(fetchResults[i].count), duration: fetchResults[i].endtime.timeIntervalSinceDate(fetchResults[i].starttime))
                    
                }
            }
        }
        
    }
    
    return totalCalForTimeInterval
    
    
}

func calcCaloriesForTimeInterval(steps: Int, duration: NSTimeInterval) -> Double {
    var ret : Double = 0.0
    var height = 0.0
    var birthYear = 0
    var minutes = 0
    
    //calculate minutes
    
    minutes = Int(Double(duration) / 60.0)
    
    
    //collect height
    if let heightft = UserData.getOrCreateUserData().getCurrentHeightFeet() {
        height = Double(heightft)
        if let heightin = UserData.getOrCreateUserData().getCurrentHeightInches() {
            height += Double(heightin)/12.0
        }
        
    }
    
    //collect weight
    let weight : Int? = UserData.getOrCreateUserData().getCurrentWeight()
    
    //collect gender
    let gender : String? = UserData.getOrCreateUserData().getCurrentGender()

    //collect birthYear
    if let birthDate : NSDate = UserData.getOrCreateUserData().getCurrentBirthdate() {
        birthYear = NSCalendar.currentCalendar().component(NSCalendarUnit.Year, fromDate: birthDate)
        
        let test = NSCalendar.currentCalendar().component(NSCalendarUnit.Year, fromDate: NSDate())
        
        //reject <1 year old as not being valid with the detailed calorie equation
        if (birthYear == test) {
            birthYear = 0
        }
    }
    
    //validate derivative values
    var valid : Bool = true
    let bypassAdvancedCalculator = true
    if (bypassAdvancedCalculator) {
        //WARN: valid set to false under all instances
        //because at launch the more complicated
        //calorie calculator results in questionable
        //values
        valid = false
        
    }
    
    //check weight
    if let w = weight {
        //weight must be >0
        if (w <= 0) {
            valid = false
        }
    } else {
        valid = false
    }
    
    //check height
    if (height < 2.0) {
        valid = false
    }
    
    //check birthdate
    if (birthYear < 1900) {
        valid = false
    }
    
    //check gender
    if let g = gender {
        //default female; accept Male
    } else {
        valid = false
    }
    
    if (minutes <= 0) {
        //minutes must be positive
        valid = false
    }
    
    if (valid) {
        ret = Calculator.calculate_calories(steps, height: Int(height*12.0), weight: weight!, gender: gender!, birthYear: birthYear, minutes: minutes)
    } else {
        ret = Calculator.simple_calculate_calories(int: steps)
    }
    
    return ret
}


func dateFormatFBRootNode( dateIn:NSDate)->String{
    let dateFormatter = NSDateFormatter()
    dateFormatter.dateFormat = "yyyy/MM/dd"
    dateFormatter.timeZone = NSTimeZone(forSecondsFromGMT: 0)
    dateFormatter.calendar = NSCalendar(calendarIdentifier: NSCalendarIdentifierISO8601)!
    dateFormatter.locale = NSLocale(localeIdentifier: "en_US_POSIX")
    let dateStringOut = dateFormatter.stringFromDate(dateIn)
    return dateStringOut
}

func dateFormatFBTimeNode( dateIn:NSDate)->String{
    let dateFormatter = NSDateFormatter()
    dateFormatter.dateFormat = "'T'HH:mm:ss'Z"
    dateFormatter.timeZone = NSTimeZone(forSecondsFromGMT: 0)
    dateFormatter.calendar = NSCalendar(calendarIdentifier: NSCalendarIdentifierISO8601)!
    dateFormatter.locale = NSLocale(localeIdentifier: "en_US_POSIX")
    let dateStringOut = dateFormatter.stringFromDate(dateIn)
    return dateStringOut
}	

func dateToStringFormat( dateIn:NSDate)->String{
    let dateFormatter = NSDateFormatter()
    dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z"
    dateFormatter.timeZone = NSTimeZone(forSecondsFromGMT: 0)
    dateFormatter.calendar = NSCalendar(calendarIdentifier: NSCalendarIdentifierISO8601)!
    dateFormatter.locale = NSLocale(localeIdentifier: "en_US_POSIX")
    let dateStringOut = dateFormatter.stringFromDate(dateIn)
    return dateStringOut
}


func createDateFromString(String isoString:String) -> NSDate{
    let form = NSDateFormatter()
    form.dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z"
    form.timeZone = NSTimeZone(forSecondsFromGMT: 0)
    form.calendar = NSCalendar(calendarIdentifier: NSCalendarIdentifierISO8601)!
    form.locale = NSLocale(localeIdentifier: "en_US_POSIX")
    
    let dateReturn = form.dateFromString(isoString)
    return dateReturn!
    
    
}




func uploadSyncResultsToFirebase(syncUid: String, whence: NSDate){
    
    var firebaseURL = UserData.getFirebase()
    if let uid = UserData.getOrCreateUserData().getCurrentUID() {
        firebaseURL = firebaseURL + "users/"
        
        firebaseURL = firebaseURL + uid
        let firebaseStepsURL = firebaseURL + "/steps/"
        let refSteps = Firebase(url:firebaseStepsURL)
        var firebaseSyncURL = firebaseURL + "/sync/"
        firebaseSyncURL = firebaseSyncURL + syncUid
        firebaseSyncURL = firebaseSyncURL + "/"
        let refSync = Firebase(url:firebaseSyncURL)
        
        /* This isn't really what syncStart & syncStop should mean */
        let syncStart = ["starttime":dateToStringFormat(whence)]
        let syncStop = ["endtime":dateToStringFormat(NSDate())]
        
        refSync.updateChildValues(syncStart)
        refSync.updateChildValues(syncStop)
        
        
        
        let predicate = NSPredicate(format:"0 == ispushed AND %@ == user", uid)
        let fetchRequestSteps = NSFetchRequest(entityName: "StepEntry")
        fetchRequestSteps.predicate = predicate
        if let fetchResults = (try? (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequestSteps)) as? [StepEntry] {
            
            
            if(fetchResults.count > 0){
                print("Count %i",fetchResults.count)
                let resultsCount = fetchResults.count
                for(var i=0;i<(resultsCount);i++){
                    let dateStringFB = dateFormatFBRootNode(fetchResults[i].starttime)
                    let dateStringTimeOnly = dateFormatFBTimeNode(fetchResults[i].starttime)
                    var appendString = dateStringFB
                    let step = fetchResults[i]
                    
                    /* 
                        Firebase format change to:
                            Y/M/Day/Time/serial/steps/{meta}

                    */
                    
                    appendString = appendString + "/"
                    appendString = appendString + dateStringTimeOnly
                    appendString = appendString + "/"
                    appendString = appendString + step.serialnumber
                    appendString = appendString + "/"
                    
                    var syncAppend = "steps/"
                    syncAppend = syncAppend + appendString
                    //                    syncAppend = syncAppend + dateStringFB
                    //                    syncAppend = syncAppend + "/"
                    //                    syncAppend =
                    //                    syncAppend = syncAppend + dateStringTimeOnly
                    
                    
                    //appendString = appendString + fetchResults[i].syncid
                    
                    let daySyncRef = refSync.childByAppendingPath(syncAppend)
                    let dayRef = refSteps.childByAppendingPath(appendString)
                    

                    let stepFields = ["count":String(step.count),"deviceid": step.serialnumber, "syncid":step.syncid, "starttime": dateFormatFBTimeNode(step.starttime),"endtime": dateFormatFBTimeNode(step.endtime)]
                    
                    
                    
                    
                    dayRef.updateChildValues(stepFields, withCompletionBlock: {
                        (error:NSError?, ref:Firebase!) in
                        if (error != nil) {
                            print("Steps could not be saved to FB.")
                        } else {
                            print("Steps saved successfully to FB!")
                            step.ispushed = true
                            UserData.saveContext()
                        }
                    })
                    
                    daySyncRef.updateChildValues(stepFields, withCompletionBlock: {
                            (error:NSError?, ref:Firebase!) in
                        if (error != nil) {
                            print("Sync could not be saved to FB.")
                        } else {
                            print("Sync saved successfully to FB!")
                            }
                        })


                    
                    
                    
                    NSLog("%@",step.starttime)
                    
                    
                    
                    //                    totalStepsForToday = totalStepsForToday + Int(fetchResults[i].count)
                }
            }else{
                //no new steps, nothing to upload
            }
        } else {
            //error grabbing steps from coredata
        }
    }
}


func insertStepsFromFirebase(FDataSnapshot stepSnapshot:FDataSnapshot, String isoDate:String) -> Bool {
    var newsteps = false
    if let uid = UserData.getOrCreateUserData().getCurrentUID() {
        var valid = true
        var countInt : Int16 = 0
        var isoStart : String = ""
        var isoStop : String = ""
        var serial : String = ""
        var syncId : String = ""
        
        if let countString = stepSnapshot.childSnapshotForPath("count").valueInExportFormat() as? NSString {
            countInt = Int16(countString.integerValue)
        } else {
            valid = false
        }
        
        if let isoStartS = stepSnapshot.childSnapshotForPath("starttime").valueInExportFormat() as? String {
            isoStart = isoDate + isoStartS
        } else {
            valid = false
        }
        
        if let isoStopS = stepSnapshot.childSnapshotForPath("endtime").valueInExportFormat() as? String {
            isoStop = isoDate + isoStopS
        }

        if let ser = stepSnapshot.childSnapshotForPath("deviceid").valueInExportFormat() as? String {
            serial = ser
        } else {
            valid = false
        }
        
        if let sync = stepSnapshot.childSnapshotForPath("syncid").valueInExportFormat() as? String {
            syncId = sync
        } else {
            valid = false
        }
        
        if (!valid) {
            //nothing valid received
            return false
        }
        var startTime = createDateFromString(String: isoStart)

        var stopTime = createDateFromString(String: isoStop)
        
        
        
        //RKY/PG Handle date roll over by assuming that all devices will report timestamps <= 24hrs, so a
        //date truncated end time that is "before" the date truncated start time indicates that we have rolled
        //over a 24hr period, and should add 24hrs worth of seconds to the date expanded stopTime in order to 
        //extract the correct stopTime.
        if(stopTime.laterDate(startTime) == startTime) {
            stopTime = stopTime.dateByAddingTimeInterval(60*60*24)
        }
        
        var isDuplicate : Bool = false
        var entry : StepEntry?
        
        (isDuplicate, entry) = duplicateDataCheck(serial, startTime: startTime, stopTime: stopTime)
        
        
        if(!isDuplicate){
            NSLog("Unique entry found, adding to coredata")
            NSLog("Steps: %i Serial: %@ Start: %@ Stop: %@",countInt, serial,startTime,stopTime)
            var newItem = NSEntityDescription.insertNewObjectForEntityForName("StepEntry", inManagedObjectContext:(UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!) as! StepEntry
            newItem.count = countInt
            newItem.user = uid
            newItem.syncid = syncId
            newItem.starttime = startTime
            newItem.endtime = stopTime
            newItem.serialnumber = serial
            newItem.ispushed = true
            newsteps = true
            
        } else if let oldItem = entry {
            //Firebase wants to replace our local DB object with new information
            
            //NSLog("Duplicate entry found, not adding to coredata")
            //NSLog("Steps: %i Serial: %@ Start: %@ Stop: %@",countInt, serial,startTime,stopTime)
            if (countInt > oldItem.count) {
                oldItem.count = countInt
                oldItem.user = uid
                oldItem.syncid = syncId
                oldItem.starttime = startTime
                oldItem.endtime = stopTime
                oldItem.serialnumber = serial
                oldItem.ispushed = true
                newsteps = true
            }
            
        }
        UserData.saveContext()
    }

    return newsteps
}


func retrieveFBDataForYM(Year: Int, Month: Int, updateCallback: FBUpdateDelegate?) {
    var days = daysInMonth(Year, Month: Month)
    
    for (var i = 1; i <= days; i++) {
        retrieveFBDataForYMDGMT(Year, Month: Month, Day: i, updateCallback: updateCallback)
        
    }
    UserData.getOrCreateUserData().downloadMetaData()
    
}


func retrieveFBDataForYMDGMT(Year: Int, Month: Int, Day: Int, updateCallback: FBUpdateDelegate?) {
    if let fbUserRef:String = UserData.getOrCreateUserData().getCurrentUserRef() as String?{
        var newsteps = false
        let year:String = String(Year)
        var month:String = ""
        if(Month<10){
            month = "0" + (String(Month))
        }else{
            month = String(Month)
        }
        var day : String = ""
        if (Day < 10) {
            day = "0" + (String(Day))
        } else {
            day = String(Day)
        }
        
        var fbDayRef = fbUserRef + "/steps/"
        fbDayRef = fbDayRef + year
        fbDayRef = fbDayRef + "/"
        fbDayRef = fbDayRef + month
        fbDayRef = fbDayRef + "/"
        fbDayRef = fbDayRef + day
        
        var iso8601String:String = year
        iso8601String = iso8601String + "-"
        iso8601String = iso8601String + month
        iso8601String = iso8601String + "-"
        iso8601String = iso8601String + day
        
        
        /*
        Firebase format change to:
        Y/M/Day/Time/serial/steps/{meta}
        
        */
        
        let fbDay = Firebase(url:fbDayRef)
        fbDay.observeSingleEventOfType(.Value, withBlock: { snapshot in
            let dayItr = snapshot.children
            while let daySnap = dayItr.nextObject() as? FDataSnapshot{
                //dayItr has the individual hours, daySnap iterates through it
                //calling .key on it will return the start time value
                //calling children on it will return the set of serial numbers on that hour
                let serialStepSetItr = daySnap.children
                //this should be each day
                while let rest = serialStepSetItr.nextObject() as? FDataSnapshot {
                    if(rest.hasChildren()){
                        let new = insertStepsFromFirebase(FDataSnapshot: rest, String:iso8601String)
                        if (new) {
                            newsteps = new
                        }
                    }
                        
            
                }
                
            }
            if let callback = updateCallback {
                if (newsteps) {
                    callback.UpdatedDataFromFirebase()
                }
            }
            
            }, withCancelBlock: { error in
                print(error.description)
        })
    }
    //update our local metadata as well

    
}


func insertSyncDataInDB(serial: String, data: [WaveStep], syncStartTime: NSDate)  -> String? {
    var count:Int = 0
    var uuid = NSUUID().UUIDString
    var syncUid = uuid
    var entry : StepEntry?
    var duplicate : Bool = false
    if let uid = UserData.getOrCreateUserData().getCurrentUID() {
        for step in data {
            count += step.steps
            (duplicate, entry) = duplicateDataCheck(serial, waveStep: step)
            if(!duplicate){
                var newItem = NSEntityDescription.insertNewObjectForEntityForName("StepEntry", inManagedObjectContext: (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!) as! StepEntry
                newItem.count = Int16(step.steps)
                newItem.user = uid
                newItem.syncid = syncUid
                newItem.starttime = step.start
                newItem.endtime = step.end
                newItem.serialnumber = String(serial)
                newItem.ispushed = false
            } else if let oldItem = entry {
                
                //we found an olditem with fewer steps
                //update the steps for the old item and set it to push again
                //otherwise, drop it
                if (Int16(step.steps) > oldItem.count) {
                    oldItem.count = Int16(step.steps)
                    oldItem.ispushed = false
                    
                }
            }
            
            print("step: "+String(step.steps))
        }
        
        
        var syncItem = NSEntityDescription.insertNewObjectForEntityForName("SyncEntry", inManagedObjectContext: (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!) as! SyncEntry
        syncItem.guid = syncUid
        syncItem.starttime = syncStartTime
        syncItem.endtime = NSDate()
        syncItem.user = uid
        syncItem.status = false
        UserData.saveContext()
        
        return syncUid
    } else {
        return nil
    }
}


func duplicateDataCheck(serial: NSString, startTime: NSDate, stopTime: NSDate) -> (Bool, StepEntry?) {
    //should also check for current user
    var isDuplicate : Bool = false
    var entry : StepEntry?
    if let currentUserId = UserData.getOrCreateUserData().getCurrentUID() {
        let predicate = NSPredicate(format:"%@ == starttime AND %@ == endtime AND %@ == serialnumber AND %@ == user", startTime, stopTime, serial, currentUserId)
    
        let fetchRequestDupeCheck = NSFetchRequest(entityName: "StepEntry")
        fetchRequestDupeCheck.predicate = predicate
        if let fetchResults = (try? (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequestDupeCheck)) as? [StepEntry] {
            if(fetchResults.count > 0){
            
                isDuplicate = true
                entry = fetchResults[0]
            }
        }
    }
    return (isDuplicate, entry)
}

func duplicateDataCheck(serial:String, waveStep: WaveStep )-> (Bool, StepEntry?) {
    return duplicateDataCheck(serial , startTime: waveStep.start, stopTime: waveStep.end)
}

//WARN: needs return function, and both need to be called correctly!
func uploadMetadataToFirebase() {
    
    if let uid = UserData.getOrCreateUserData().getCurrentUID() {
        var ref = UserData.getFirebase()
        ref = ref + "users/"
        ref = ref + uid
        ref = ref + "/"
        ref = ref + "metadata/"
        
        let fbMetadataRef = Firebase(url: ref)
        
        let predicate = NSPredicate(format:"%@ == id",uid)
        let fetchRequestDupeCheck = NSFetchRequest(entityName: "UserEntry")
        fetchRequestDupeCheck.predicate = predicate
        if let fetchResults = (try? (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequestDupeCheck)) as? [UserEntry] {
            if(fetchResults.count == 1){
                
                NSLog("Updaing values at %@",ref)
                if let fn = fetchResults[0].fullname {
                    let nameUp = ["currentFullName": String(fn)]
                    fbMetadataRef.updateChildValues(nameUp)
                }
                
                if let bt = fetchResults[0].birthdate {
                    let birthTime:NSTimeInterval = bt.timeIntervalSince1970
                    let birthLong:Int32 = Int32(birthTime);
                    let birthUp  = ["currentBirthdate" : String(birthLong)] //dates are saved as long on firebase
                    fbMetadataRef.updateChildValues(birthUp)
                }

                
                
                let hf = fetchResults[0].heightfeet
                let heightFtUp = ["currentHeight1": String(hf)]
                fbMetadataRef.updateChildValues(heightFtUp)
                
                
                let hi = fetchResults[0].heightinches
                let heightInchesUp = ["currentHeight2": String(hi)]
                fbMetadataRef.updateChildValues(heightInchesUp)
                
                
                let w = fetchResults[0].weight
                let weightUp = ["currentWeight": String(w)]
                fbMetadataRef.updateChildValues(weightUp)
                
                
                if let g = fetchResults[0].gender {
                    let genderUp = ["currentGender": String(g)]
                    fbMetadataRef.updateChildValues(genderUp)
                }
                
                
            }
        }
    }
}


func showSpinner(title: String, message: String) -> UIAlertController {
    let activityAlert : UIAlertController = UIAlertController(title: title, message: message, preferredStyle: UIAlertControllerStyle.Alert)
    
    
    //(title: title, message: message, delegate: nil, cancelButtonTitle: nil)
    
    let indicator : UIActivityIndicatorView = UIActivityIndicatorView(activityIndicatorStyle: UIActivityIndicatorViewStyle.WhiteLarge)
    indicator.center = CGPointMake(activityAlert.view.bounds.size.width / 2, activityAlert.view.bounds.size.height - 50);
    indicator.startAnimating()
    activityAlert.view.addSubview(indicator)
    return activityAlert
}

func showAlertView(title: String, message: String) -> UIAlertView {
    let activityAlert : UIAlertView = UIAlertView(title: title, message: message, delegate: nil, cancelButtonTitle: "OK")
    activityAlert.show()
    return activityAlert
    
}

func fetchUserList() -> [UserEntry]? {
    let fetchRequest = NSFetchRequest(entityName: "UserEntry")
    var userList : [UserEntry]?
    //                fetchRequest.predicate = predicate
    if let fetchResults = (try? (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequest)) as? [UserEntry] {
        
        userList = fetchResults
    }
    
    return userList
}


func fetchUserByEmail(email: String) -> UserEntry? {
    let fetchRequest = NSFetchRequest(entityName: "UserEntry")
    let predicate = NSPredicate(format:"%@ == email", email)
    fetchRequest.predicate = predicate
    
    if let fetchResults = (try? (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequest)) as? [UserEntry] {
    //three cases:
    
    //count == 1, the best case
        if (fetchResults.count == 1) {
            return fetchResults[0]
        } else if (fetchResults.count >= 1) {
            //bad case -- somehow we ended up with multiple users on the same email address
            //we should cull all but [0] to 'fix' the DB
            let toRtn = fetchResults[0]
            clearExcessItems(fetchResults)
            return toRtn
        }
    }
    //none found or error
    return nil
    
}

func clearExcessItems(array: [NSManagedObject]) {
    let short = array[1..<array.count]
    for i in short {
        (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.deleteObject(i)
    }
    UserData.saveContext()
}



//Attempt login based on email selection

//since we allow offline operation, assume that the user information is correct

func login(email: String) -> Bool {
    
    //auth with email and pass that are in the input UI

    if let userentry : UserEntry = fetchUserByEmail(email) {
        UserData.getOrCreateUserData().loadUser(userentry)
        return checkAuth()
    }
    return false
}

//for any current user, attempt to FB authenticates
//for now, we will stub this to return true because 
//the intended functionality isn't clear
func checkAuth() -> Bool {
    if let password = UserData.getOrCreateUserData().getCurrentPW() {
        if let email = UserData.getOrCreateUserData().getCurrentEmail() {
            login(email, password: password)
        }
    }
    return true
}


func isKnownDevice(uid: String, serial: String) -> Bool {
    if let uid = UserData.getOrCreateUserData().getCurrentUID() {
        let fetchRequest = NSFetchRequest(entityName: "KnownWaves")
        let predicate = NSPredicate(format:"%@ == user AND %@ == serialnumber", uid, serial)
        fetchRequest.predicate = predicate
        
        if let fetchResults = (try? (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequest)) as? [KnownWaves] {
            
            if (fetchResults.count > 0) {
                if (fetchResults.count == 1) {
                    //do nothing
                } else {
                    print("WARNING TOO MANY DEVICES")
                    clearExcessItems(fetchResults)
                }
                
                return true
            }
        }
    }
    return false
}

func addToKnownDevices(serial : String) {
    if let uid = UserData.getOrCreateUserData().getCurrentUID() {
        let fetchRequest = NSFetchRequest(entityName: "KnownWaves")
        let predicate = NSPredicate(format:"%@ == user AND %@ == serialnumber", uid, serial)
        fetchRequest.predicate = predicate
        
         if let fetchResults = (try? (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequest)) as? [KnownWaves] {
            
            if (fetchResults.count > 0) {
                if (fetchResults.count == 1) {
                    //do nothing

                } else {
                    print("WARNING TOO MANY DEVICES")
                    clearExcessItems(fetchResults)
                }
            } else {
                //insert new item
                
                let knownWave = NSEntityDescription.insertNewObjectForEntityForName("KnownWaves", inManagedObjectContext: (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!) as! KnownWaves
                knownWave.user = uid
                knownWave.serialnumber = serial
                UserData.saveContext()
            }
            
        }
        
    }
    
}


func isValidBirthDate(birthdate: NSDate) -> Bool {
    let cdate = NSDate()
    let cal = NSCalendar.currentCalendar()
    
    let cyear = cal.component(NSCalendarUnit.Year, fromDate: cdate)
    let year = cal.component(NSCalendarUnit.Year, fromDate: birthdate)
    
    let cmonth = cal.component(NSCalendarUnit.Month, fromDate: cdate)
    let month = cal.component(NSCalendarUnit.Month, fromDate: birthdate)
    
    let cday = cal.component(NSCalendarUnit.Day, fromDate: cdate)
    let day = cal.component(NSCalendarUnit.Day, fromDate: birthdate)
    
    //have we already had our birthday
    var birthday = 0
    
    if (cmonth >= month) {
        if (cmonth == month) {
            if (cday >= day) {
                birthday = 1
            }
        } else {
            birthday = 1
        }
        
    }
    
    //given this, current age is:  cyear - year + birthday - 1
    //this will yield 1 less than cyear - year unless a birthday has occured
    let age = cyear - year + birthday - 1
    
    return (age >= 13) ? true : false
}


func isToday(date: NSDate) -> Bool {
    let cdate = NSDate()
    let cal = NSCalendar.currentCalendar()
    
    let cyear = cal.component(NSCalendarUnit.Year, fromDate: cdate)
    let year = cal.component(NSCalendarUnit.Year, fromDate: date)
    
    let cmonth = cal.component(NSCalendarUnit.Month, fromDate: cdate)
    let month = cal.component(NSCalendarUnit.Month, fromDate: date)
    
    let cday = cal.component(NSCalendarUnit.Day, fromDate: cdate)
    let day = cal.component(NSCalendarUnit.Day, fromDate: date)
    
    if (cyear == year && cmonth == month && cday == day) {
        return true
    }
    
    return false
}

func floatCommaNumberFormatter(decimals: Int) -> NSNumberFormatter {
    let formatter = NSNumberFormatter()
    formatter.numberStyle = NSNumberFormatterStyle.DecimalStyle
    formatter.maximumFractionDigits = decimals
    
    return formatter
}

protocol ResetPasswordDelegate {
    func resetPassword(success: Bool)
}

func resetUserPassword(userEmail: String, delegate: ResetPasswordDelegate?){
    let fbRef = UserData.getFirebase()
    let ref = Firebase(url: fbRef)
    ref.resetPasswordForUser(userEmail, withCompletionBlock: { error in
        
        if error != nil {
        // There was an error processing the request
            if let d = delegate {
                d.resetPassword(false)
            }
        } else {
        // Password reset sent successfully
            if let d = delegate {
                d.resetPassword(true)
            }
        }
    })
}

protocol PasswordChangeDelegate {
    func passwordChanged(success: Bool)
}

func changeUserPassword(userEmail: String, oldPassword: String, newPassword: String, delegate: PasswordChangeDelegate?) {
    let ref = Firebase(url: UserData.currentFireBaseRef)
    ref.changePasswordForUser(userEmail, fromOld: oldPassword,
        toNew: newPassword, withCompletionBlock: { error in
            
            if error != nil {
            // There was an error processing the request
                var failure = "Failed with error" + error.description
                if let d = delegate {
                    d.passwordChanged(false)
                }
                
            } else {
            // Password changed successfully
                if let d = delegate {
                    d.passwordChanged(true)
                }
            }
        })
}


func setupNotificationSet() {

    
    
    
    //cancel existing notifications
    UIApplication.sharedApplication().cancelAllLocalNotifications()
    
    
    //schedule notifications
    let today = NSDate()
    let days = [2, 4, 6, 7]
    let text = ["Sync your Wave to find out how far you've come.",
        "Don't forget to sync and update your Movo calendar.",
        "Where have your steps taken you? Sync your Wave now.",
        "You will lose data if you do not sync at least once a week."]
    for (var i = 0; i<4; i++) {
        let notificationDate = today.dateByAddingTimeInterval(Double(days[i])*60*60*24)
        let notification = UILocalNotification()
        notification.fireDate = notificationDate
        notification.alertBody = text[i]
        UIApplication.sharedApplication().scheduleLocalNotification(notification)
    }
    
    
}


func isValidEmail(testStr:String) -> Bool {
    let emailRegEx:String = "[A-Z0-9a-z._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,6}"
    
    let emailTest = NSPredicate(format:"SELF MATCHES %@", emailRegEx)
    return emailTest.evaluateWithObject(testStr)
}


func getDisplayedViewController() -> UIViewController? {
    var controller : UIViewController?
    if let window = UIApplication.sharedApplication().keyWindow {
        controller = window.rootViewController
        if (controller != nil) {
            while (controller!.presentedViewController != nil) {
                controller = controller!.presentedViewController
            }
        }
    }
    return controller
}


func login(email: String, password: String) {
    
    let ref = Firebase(url: UserData.getFirebase())
    //auth with email and pass that are in the input UI
    ref.authUser(email, password: password,
        withCompletionBlock: { error, authData in
            
            if error != nil {
                // There was an error logging in to this account
                NSLog("Login failed")
                let alertController = UIAlertController(title: "Error", message:
                    "Login failed.  Please try again, operating in offline mode", preferredStyle: UIAlertControllerStyle.Alert)
                alertController.addAction(UIAlertAction(title: "OK", style: UIAlertActionStyle.Default,handler: nil))
                if let controller = getDisplayedViewController() {
                    controller.presentViewController(alertController, animated: true, completion: nil)
                }
                
            } else {
                let providerData:NSDictionary = authData.providerData
                let tempPasswordBool = providerData["isTemporaryPassword"] as! Bool
                
                if(tempPasswordBool){
                    //true if password is temp, do password reset prompt.
                    if let controller = getDisplayedViewController() {
                        PasswordResetAlert.presentSetPasswordDialog(controller, userEmail: email, oldPassword: password)
                    }
                }
                authData.uid
                
                
                /* this logic isn't quite right */
                /* This is a successful login, but we can assume that the current user may not exist and even if it does, it may not have values set correctly */
                
                /* So what do we need to do? */
                
                /* 1 - get the user entry that corresponds to this email */
                if let userentry : UserEntry = fetchUserByEmail(email) {
                    //in this case, the user is an existing user
                    //accept the new password
                    //and attempt to load the user
                    userentry.pw = password
                    UserData.saveContext()
                    UserData.getOrCreateUserData().loadUser(userentry)
                    
                } else {
                    //in this case, the user does not exist locally
                    //so we need to create a new local user copy
                    //and log that one in
                    
                    //so we should retrieve the user info
                    var stringRef = UserData.getFirebase() + "users/"
                    stringRef = stringRef + authData.uid
                    
                    
                    let userentry = UserData.getOrCreateUserData().createUser(email, pw: password, uid: authData.uid, birth: nil, heightfeet: nil, heightinches: nil, weightlbs: nil, gender: nil, fullName: nil, user: nil, ref: stringRef)
                    
                    UserData.saveContext()
                    
                    UserData.getOrCreateUserData().loadUser(userentry)
                    
                    
                }
                
                
                /*
                UserData.getOrCreateUserData().setCurrentUID(authData.uid)
                UserData.getOrCreateUserData().setCurrentEmail(email)
                UserData.getOrCreateUserData().setCurrentPW(password)
                var stringRef = UserData.getFirebase() + "users/"
                stringRef = stringRef + authData.uid
                
                UserData.getOrCreateUserData().setCurrentUserRef(stringRef)
                */
                
                UserData.getOrCreateUserData().downloadMetaData()
                
                
                
            }
    })
    
    
}



func daysInMonth(Year: Int, Month: Int) -> Int {
    var days = 31
    var cal = NSCalendar.currentCalendar()
    if let date = YMDLocalToNSDate(Year, month: Month, day: 1) {
        days = cal.rangeOfUnit(.Day,
            inUnit: .Month,
            forDate: date).toRange()!.endIndex-1
    }

    return days
}

