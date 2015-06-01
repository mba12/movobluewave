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
    var calendar = NSCalendar.currentCalendar()
    calendar.timeZone = NSTimeZone.localTimeZone()
    var startTimeComponents = NSDateComponents()
    startTimeComponents.setValue(day, forComponent: NSCalendarUnit.CalendarUnitDay)
    startTimeComponents.setValue(year, forComponent: NSCalendarUnit.CalendarUnitYear)
    startTimeComponents.setValue(month, forComponent: NSCalendarUnit.CalendarUnitMonth)
    startTimeComponents.setValue(0, forComponent: NSCalendarUnit.CalendarUnitHour)
    startTimeComponents.setValue(0, forComponent: NSCalendarUnit.CalendarUnitMinute)
    startTimeComponents.setValue(0, forComponent: NSCalendarUnit.CalendarUnitSecond)
    
    return calendar.dateFromComponents(startTimeComponents)
}

func YMDGMTToNSDate(year: Int, month: Int, day: Int) -> NSDate? {
    var calendar = NSCalendar.currentCalendar()
    calendar.timeZone = NSTimeZone(forSecondsFromGMT: 0)
    var startTimeComponents = NSDateComponents()
    startTimeComponents.setValue(day, forComponent: NSCalendarUnit.CalendarUnitDay)
    startTimeComponents.setValue(year, forComponent: NSCalendarUnit.CalendarUnitYear)
    startTimeComponents.setValue(month, forComponent: NSCalendarUnit.CalendarUnitMonth)
    startTimeComponents.setValue(0, forComponent: NSCalendarUnit.CalendarUnitHour)
    startTimeComponents.setValue(0, forComponent: NSCalendarUnit.CalendarUnitMinute)
    startTimeComponents.setValue(0, forComponent: NSCalendarUnit.CalendarUnitSecond)
    
    return calendar.dateFromComponents(startTimeComponents)
    
    
    
}




func stepsForDayStarting(dateStart: NSDate) -> Int {
    var dateStop = dateStart.dateByAddingTimeInterval(60*60*24); //24hrs
    return getStepsForTimeInterval(dateStart, dateStop)
}


func caloriesForDayStarting(dateStart: NSDate) -> Double {
    var dateStop = dateStart.dateByAddingTimeInterval(60*60*24)
    return getCaloriesForTimeInterval(dateStart, dateStop)
}

func getStepsForTimeInterval(dateStart:NSDate, dateStop:NSDate) -> Int {
    
    var totalStepsForInterval : Int = 0
    if let uid = UserData.getOrCreateUserData().getCurrentUID() {
        let predicate = NSPredicate(format:"%@ <= starttime AND %@ >= endtime AND %@ == user", dateStart, dateStop, uid)
        
        let fetchRequest = NSFetchRequest(entityName: "StepEntry")
        fetchRequest.predicate = predicate
        if let fetchResults = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [StepEntry] {
            if(fetchResults.count > 0){
                println("Count %i",fetchResults.count)
                var resultsCount = fetchResults.count
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
        if let fetchResults = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [StepEntry] {
            if(fetchResults.count > 0){
                println("Count %i",fetchResults.count)
                var resultsCount = fetchResults.count
                for(var i=0;i<(resultsCount);i++){
                    //                    println("Adding steps up for %i %i",cellDateNumber, Int(fetchResults[i].count))
                    totalCalForTimeInterval += calcCaloriesForTimeInterval(Int(fetchResults[i].count), fetchResults[i].endtime.timeIntervalSinceDate(fetchResults[i].starttime))
                    
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
    var weight : Int? = UserData.getOrCreateUserData().getCurrentWeight()
    
    //collect gender
    var gender : String? = UserData.getOrCreateUserData().getCurrentGender()

    //collect birthYear
    if let birthDate : NSDate = UserData.getOrCreateUserData().getCurrentBirthdate() {
        birthYear = NSCalendar.currentCalendar().component(NSCalendarUnit.CalendarUnitYear, fromDate: birthDate)
        
        var test = NSCalendar.currentCalendar().component(NSCalendarUnit.CalendarUnitYear, fromDate: NSDate())
        
        //reject <1 year old as not being valid with the detailed calorie equation
        if (birthYear == test) {
            birthYear = 0
        }
    }
    
    //validate derivative values
    var valid : Bool = true
    
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
    var dateFormatter = NSDateFormatter()
    dateFormatter.dateFormat = "yyyy/MM/dd"
    dateFormatter.timeZone = NSTimeZone(forSecondsFromGMT: 0)
    dateFormatter.calendar = NSCalendar(calendarIdentifier: NSCalendarIdentifierISO8601)!
    dateFormatter.locale = NSLocale(localeIdentifier: "en_US_POSIX")
    var dateStringOut = dateFormatter.stringFromDate(dateIn)
    return dateStringOut
}

func dateFormatFBTimeNode( dateIn:NSDate)->String{
    var dateFormatter = NSDateFormatter()
    dateFormatter.dateFormat = "'T'HH:mm:ss'Z"
    dateFormatter.timeZone = NSTimeZone(forSecondsFromGMT: 0)
    dateFormatter.calendar = NSCalendar(calendarIdentifier: NSCalendarIdentifierISO8601)!
    dateFormatter.locale = NSLocale(localeIdentifier: "en_US_POSIX")
    var dateStringOut = dateFormatter.stringFromDate(dateIn)
    return dateStringOut
}	

func dateToStringFormat( dateIn:NSDate)->String{
    var dateFormatter = NSDateFormatter()
    dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z"
    dateFormatter.timeZone = NSTimeZone(forSecondsFromGMT: 0)
    dateFormatter.calendar = NSCalendar(calendarIdentifier: NSCalendarIdentifierISO8601)!
    dateFormatter.locale = NSLocale(localeIdentifier: "en_US_POSIX")
    var dateStringOut = dateFormatter.stringFromDate(dateIn)
    return dateStringOut
}


func createDateFromString(String isoString:String) -> NSDate{
    let form = NSDateFormatter()
    form.dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z"
    form.timeZone = NSTimeZone(forSecondsFromGMT: 0)
    form.calendar = NSCalendar(calendarIdentifier: NSCalendarIdentifierISO8601)!
    form.locale = NSLocale(localeIdentifier: "en_US_POSIX")
    
    var dateReturn = form.dateFromString(isoString)
    return dateReturn!
    
    
}




func uploadSyncResultsToFirebase(syncUid: String, whence: NSDate){
    
    var firebaseURL = UserData.getFirebase()
    if let uid = UserData.getOrCreateUserData().getCurrentUID() {
        firebaseURL = firebaseURL + "users/"
        
        firebaseURL = firebaseURL + uid
        var firebaseStepsURL = firebaseURL + "/steps/"
        var refSteps = Firebase(url:firebaseStepsURL)
        var firebaseSyncURL = firebaseURL + "/sync/"
        firebaseSyncURL = firebaseSyncURL + syncUid
        firebaseSyncURL = firebaseSyncURL + "/"
        var refSync = Firebase(url:firebaseSyncURL)
        
        /* This isn't really what syncStart & syncStop should mean */
        var syncStart = ["starttime":dateToStringFormat(whence)]
        var syncStop = ["endtime":dateToStringFormat(NSDate())]
        
        refSync.updateChildValues(syncStart)
        refSync.updateChildValues(syncStop)
        
        
        
        let predicate = NSPredicate(format:"0 == ispushed AND %@ == user", uid)
        let fetchRequestSteps = NSFetchRequest(entityName: "StepEntry")
        fetchRequestSteps.predicate = predicate
        if let fetchResults = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequestSteps, error: nil) as? [StepEntry] {
            
            
            if(fetchResults.count > 0){
                println("Count %i",fetchResults.count)
                var resultsCount = fetchResults.count
                for(var i=0;i<(resultsCount);i++){
                    var dateStringFB = dateFormatFBRootNode(fetchResults[i].starttime)
                    var dateStringTimeOnly = dateFormatFBTimeNode(fetchResults[i].starttime)
                    var appendString = dateStringFB
                    let step = fetchResults[i]
                    appendString = appendString + "/"
                    appendString = appendString + fetchResults[i].syncid
                    appendString = appendString + "/"
                    appendString = appendString + dateStringTimeOnly
                    appendString = appendString + "/"
                    
                    var syncAppend = "steps/"
                    syncAppend = syncAppend + appendString
                    //                    syncAppend = syncAppend + dateStringFB
                    //                    syncAppend = syncAppend + "/"
                    //                    syncAppend =
                    //                    syncAppend = syncAppend + dateStringTimeOnly
                    
                    var daySyncRef = refSync.childByAppendingPath(syncAppend)
                    var dayRef = refSteps.childByAppendingPath(appendString)
                    

                    var stepFields = ["count":String(step.count),"deviceid":String(step.serialnumber),"starttime": dateFormatFBTimeNode(step.starttime),"endtime": dateFormatFBTimeNode(step.endtime)]
                    
                    
                    
                    
                    dayRef.updateChildValues(stepFields, withCompletionBlock: {
                        (error:NSError?, ref:Firebase!) in
                        if (error != nil) {
                            println("Steps could not be saved to FB.")
                        } else {
                            println("Steps saved successfully to FB!")
                            step.ispushed = true
                            UserData.saveContext()
                        }
                    })
                    
                    daySyncRef.updateChildValues(stepFields, withCompletionBlock: {
                            (error:NSError?, ref:Firebase!) in
                        if (error != nil) {
                            println("Sync could not be saved to FB.")
                        } else {
                            println("Sync saved successfully to FB!")
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


func insertStepsFromFirebase(FDataSnapshot daySnapshot:FDataSnapshot, String syncId:String, String isoDate:String){
    
    if let uid = UserData.getOrCreateUserData().getCurrentUID() {
        var stepsChild:FDataSnapshot = daySnapshot.childSnapshotForPath("count")
        //        println(stepsChild.value)
        var countString = daySnapshot.childSnapshotForPath("count").valueInExportFormat() as? NSString
        var countInt:Int16 = Int16(countString!.integerValue)
        var isoStart:String = isoDate + (daySnapshot.childSnapshotForPath("starttime").valueInExportFormat() as? String)!
        var isoStop:String = isoDate + (daySnapshot.childSnapshotForPath("endtime").valueInExportFormat() as? String)!
        var serial:String = (daySnapshot.childSnapshotForPath("deviceid").valueInExportFormat() as? String)!
        var startTime = createDateFromString(String: isoStart)
        var stopTime = createDateFromString(String: isoStop)
        
        var isDuplicate : Bool = duplicateDataCheck(serial, startTime, stopTime)
        
        
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
            
        }else{
            //NSLog("Duplicate entry found, not adding to coredata")
            //NSLog("Steps: %i Serial: %@ Start: %@ Stop: %@",countInt, serial,startTime,stopTime)
        }
        UserData.saveContext()
    }

    
}


func retrieveFBDataForYMDGMT(Year: Int, Month: Int, Day: Int, updateCallback: FBUpdateDelegate?) {
    if let var fbUserRef:String = UserData.getOrCreateUserData().getCurrentUserRef() as String?{
        var year:String = String(Year)
        var month:String = ""
        if(Month<10){
            month = "0" + (String(Month))
        }else{
            month = String(Day)
        }
        var fbMonthRef = fbUserRef + "/steps/"
        fbMonthRef = fbMonthRef + year
        fbMonthRef = fbMonthRef + "/"
        fbMonthRef = fbMonthRef + month
        fbMonthRef = fbMonthRef + "/"
        var iso8601String:String = year
        iso8601String = iso8601String + "-"
        iso8601String = iso8601String + month
        iso8601String = iso8601String + "-"
        
        
        var fbMonth = Firebase(url:fbMonthRef)
        fbMonth.observeSingleEventOfType(.Value, withBlock: { snapshot in
            var monthItr = snapshot.children
            while let monthSnap = monthItr.nextObject() as? FDataSnapshot{
                //monthSnap grabs the individual days, calling .key on it will return the day #
                var isoDate = iso8601String
                isoDate = isoDate + monthSnap.key
                var dayItr = monthSnap.children
                while let rest = dayItr.nextObject() as? FDataSnapshot {
                    
                    //this is the root node for a steps object
                    var syncId = rest.key
                    //                    NSLog("Syncid is %@",syncId!)
                    var itr2 = rest.children
                    while let daySnap = itr2.nextObject() as? FDataSnapshot{
                        //this steps into the node title and gets the objects
                        //                            isoDate = isoDate + daySnap.key
                        if(daySnap.hasChildren()){
                            insertStepsFromFirebase(FDataSnapshot: daySnap, String:syncId, String:isoDate)
                        }
                        
                    }
                }
                
            }
            if let callback = updateCallback {
                callback.UpdatedDataFromFirebase()
            }
            
            }, withCancelBlock: { error in
                println(error.description)
        })
    }
    //update our local metadata as well
    UserData.getOrCreateUserData().downloadMetaData()
    
}


func insertSyncDataInDB(serial: String, data: [WaveStep], syncStartTime: NSDate)  -> String? {
    var count:Int = 0
    var uuid = NSUUID().UUIDString
    var syncUid = uuid
    if let uid = UserData.getOrCreateUserData().getCurrentUID() {
        for step in data {
            count += step.steps
            if(!duplicateDataCheck(serial, step)){
                var newItem = NSEntityDescription.insertNewObjectForEntityForName("StepEntry", inManagedObjectContext: (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!) as! StepEntry
                newItem.count = Int16(step.steps)
                newItem.user = uid
                newItem.syncid = syncUid
                newItem.starttime = step.start
                newItem.endtime = step.end
                newItem.serialnumber = String(serial)
                newItem.ispushed = false
            }
            
            println("step: "+String(step.steps))
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


func duplicateDataCheck(serial: NSString, startTime: NSDate, stopTime: NSDate) -> Bool {
    //should also check for current user
    var isDuplicate : Bool = false
    if let currentUserId = UserData.getOrCreateUserData().getCurrentUID() {
        let predicate = NSPredicate(format:"%@ == starttime AND %@ == endtime AND %@ == serialnumber AND %@ == user", startTime, stopTime, serial, currentUserId)
    
        let fetchRequestDupeCheck = NSFetchRequest(entityName: "StepEntry")
        fetchRequestDupeCheck.predicate = predicate
        if let fetchResults = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequestDupeCheck, error: nil) as? [StepEntry] {
            if(fetchResults.count > 0){
            
                isDuplicate = true
            
            }
        }
    }
    return isDuplicate
}

func duplicateDataCheck(serial:String, waveStep: WaveStep )->Bool{
    return duplicateDataCheck(serial , waveStep.start, waveStep.end)
}

//WARN: needs return function, and both need to be called correctly!
func uploadMetadataToFirebase() {
    
    if let uid = UserData.getOrCreateUserData().getCurrentUID() {
        var ref = UserData.getFirebase()
        ref = ref + "users/"
        ref = ref + uid
        ref = ref + "/"
        ref = ref + "metadata/"
        
        var fbMetadataRef = Firebase(url: ref)
        
        let predicate = NSPredicate(format:"%@ == id",uid)
        let fetchRequestDupeCheck = NSFetchRequest(entityName: "UserEntry")
        fetchRequestDupeCheck.predicate = predicate
        if let fetchResults = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequestDupeCheck, error: nil) as? [UserEntry] {
            if(fetchResults.count == 1){
                
                NSLog("Updaing values at %@",ref)
                if let fn = fetchResults[0].fullname {
                    var nameUp = ["currentFullName": String(fn)]
                    fbMetadataRef.updateChildValues(nameUp)
                }
                
                if let bt = fetchResults[0].birthdate {
                    var birthTime:NSTimeInterval = bt.timeIntervalSince1970
                    var birthLong:Int32 = Int32(birthTime);
                    var birthUp  = ["currentBirthdate" : String(birthLong)] //dates are saved as long on firebase
                    fbMetadataRef.updateChildValues(birthUp)
                }

                
                
                let hf = fetchResults[0].heightfeet
                var heightFtUp = ["currentHeight1": String(hf)]
                fbMetadataRef.updateChildValues(heightFtUp)
                
                
                let hi = fetchResults[0].heightinches
                var heightInchesUp = ["currentHeight2": String(hi)]
                fbMetadataRef.updateChildValues(heightInchesUp)
                
                
                let w = fetchResults[0].weight
                var weightUp = ["currentWeight": String(w)]
                fbMetadataRef.updateChildValues(weightUp)
                
                
                if let g = fetchResults[0].gender {
                    var genderUp = ["currentGender": String(g)]
                    fbMetadataRef.updateChildValues(genderUp)
                }
                
                
            }
        }
    }
}


func showSpinner(title: String, message: String) -> UIAlertController {
    var activityAlert : UIAlertController = UIAlertController(title: title, message: message, preferredStyle: UIAlertControllerStyle.Alert)
    
    
    //(title: title, message: message, delegate: nil, cancelButtonTitle: nil)
    
    var indicator : UIActivityIndicatorView = UIActivityIndicatorView(activityIndicatorStyle: UIActivityIndicatorViewStyle.WhiteLarge)
    indicator.center = CGPointMake(activityAlert.view.bounds.size.width / 2, activityAlert.view.bounds.size.height - 50);
    indicator.startAnimating()
    activityAlert.view.addSubview(indicator)
    return activityAlert
}

func showAlertView(title: String, message: String) -> UIAlertView {
    var activityAlert : UIAlertView = UIAlertView(title: title, message: message, delegate: nil, cancelButtonTitle: "OK")
    activityAlert.show()
    return activityAlert
    
}

func fetchUserList() -> [UserEntry]? {
    let fetchRequest = NSFetchRequest(entityName: "UserEntry")
    var userList : [UserEntry]?
    //                fetchRequest.predicate = predicate
    if let fetchResults = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [UserEntry] {
        
        userList = fetchResults
    }
    
    return userList
}


func fetchUserByEmail(email: String) -> UserEntry? {
    let fetchRequest = NSFetchRequest(entityName: "UserEntry")
    let predicate = NSPredicate(format:"%@ == email", email)
    fetchRequest.predicate = predicate
    
    if let fetchResults = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [UserEntry] {
    //three cases:
    
    //count == 1, the best case
        if (fetchResults.count == 1) {
            return fetchResults[0]
        } else if (fetchResults.count >= 1) {
            //bad case -- somehow we ended up with multiple users on the same email address
            //we should cull all but [0] to 'fix' the DB
            var toRtn = fetchResults[0]
            clearExcessItems(fetchResults)
            return toRtn
        }
    }
    //none found or error
    return nil
    
}

func clearExcessItems(array: [NSManagedObject]) {
    var short = array[1..<array.count]
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
        return true
    }
    return false
}

//for any current user, see if FB authenticates
//for now, we will stub this to return true because 
//the intended functionality isn't clear
func checkAuth() -> Bool {
    
    return true
}


func isKnownDevice(uid: String, serial: String) -> Bool {
    if let uid = UserData.getOrCreateUserData().getCurrentUID() {
        let fetchRequest = NSFetchRequest(entityName: "KnownWaves")
        let predicate = NSPredicate(format:"%@ == user AND %@ == serialnumber", uid, serial)
        fetchRequest.predicate = predicate
        
        if let fetchResults = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [KnownWaves] {
            
            if (fetchResults.count > 0) {
                if (fetchResults.count == 1) {
                    //do nothing
                } else {
                    println("WARNING TOO MANY DEVICES")
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
        
         if let fetchResults = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [KnownWaves] {
            
            if (fetchResults.count > 0) {
                if (fetchResults.count == 1) {
                    //do nothing

                } else {
                    println("WARNING TOO MANY DEVICES")
                    clearExcessItems(fetchResults)
                }
            } else {
                //insert new item
                
                var knownWave = NSEntityDescription.insertNewObjectForEntityForName("KnownWaves", inManagedObjectContext: (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!) as! KnownWaves
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
    
    let cyear = cal.component(NSCalendarUnit.CalendarUnitYear, fromDate: cdate)
    let year = cal.component(NSCalendarUnit.CalendarUnitYear, fromDate: birthdate)
    
    let cmonth = cal.component(NSCalendarUnit.CalendarUnitMonth, fromDate: cdate)
    let month = cal.component(NSCalendarUnit.CalendarUnitMonth, fromDate: birthdate)
    
    let cday = cal.component(NSCalendarUnit.CalendarUnitDay, fromDate: cdate)
    let day = cal.component(NSCalendarUnit.CalendarUnitDay, fromDate: birthdate)
    
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
    
    let cyear = cal.component(NSCalendarUnit.CalendarUnitYear, fromDate: cdate)
    let year = cal.component(NSCalendarUnit.CalendarUnitYear, fromDate: date)
    
    let cmonth = cal.component(NSCalendarUnit.CalendarUnitMonth, fromDate: cdate)
    let month = cal.component(NSCalendarUnit.CalendarUnitMonth, fromDate: date)
    
    let cday = cal.component(NSCalendarUnit.CalendarUnitDay, fromDate: cdate)
    let day = cal.component(NSCalendarUnit.CalendarUnitDay, fromDate: date)
    
    if (cyear == year && cmonth == month && cday == day) {
        return true
    }
    
    return false
}
