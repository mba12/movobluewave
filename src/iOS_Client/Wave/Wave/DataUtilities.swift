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




func stepsForDayStarting(dateStart: NSDate) -> Int {
    var dateStop = dateStart.dateByAddingTimeInterval(60*60*24); //24hrs
    return getStepsForTimeInterval(NSDate: dateStart, NSDate: dateStop)
}



func getStepsForTimeInterval(NSDate dateStart:NSDate, NSDate dateStop:NSDate) -> Int {
    
    var totalStepsForInterval : Int = 0
    let predicate = NSPredicate(format:"%@ <= starttime AND %@ >= endtime AND %@ == user", dateStart, dateStop, UserData.getOrCreateUserData().getCurrentUID())
    
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
    
    return totalStepsForInterval

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
    
    var firebaseURL = UserData.getOrCreateUserData().getFirebase()
    firebaseURL = firebaseURL + "users/"
    firebaseURL = firebaseURL + UserData.getOrCreateUserData().getCurrentUID()
    var firebaseStepsURL = firebaseURL + "/steps/"
    var refSteps = Firebase(url:firebaseStepsURL)
    var firebaseSyncURL = firebaseURL + "/sync/"
    firebaseSyncURL = firebaseSyncURL + syncUid
    firebaseSyncURL = firebaseSyncURL + "/"
    var refSync = Firebase(url:firebaseSyncURL)
    
    
    var syncStart = ["starttime":dateToStringFormat(whence)]
    var syncStop = ["endtime":dateToStringFormat(NSDate())]
    
    refSync.updateChildValues(syncStart)
    refSync.updateChildValues(syncStop)
    
    
    
    let predicate = NSPredicate(format:"%@ == syncid", syncUid)
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
                
                var count = ["count": String(fetchResults[i].count)]
                var deviceid = ["deviceid": String(fetchResults[i].serialnumber)]
                var starttime = ["starttime": dateFormatFBTimeNode(fetchResults[i].starttime)]
                var endtime = ["endtime": dateFormatFBTimeNode(fetchResults[i].endtime)]

                
                dayRef.updateChildValues(count)
                dayRef.updateChildValues(deviceid)
                dayRef.updateChildValues(starttime)
                dayRef.updateChildValues(endtime)
                
                daySyncRef.updateChildValues(count)
                daySyncRef.updateChildValues(deviceid)
                daySyncRef.updateChildValues(starttime)
                daySyncRef.updateChildValues(endtime)
                
                
                
                
                
                NSLog("%@",fetchResults[i].starttime)
                
                
                
                //                    totalStepsForToday = totalStepsForToday + Int(fetchResults[i].count)
            }
            
        }else{
            //no new steps, nothing to upload
        }
    } else {
        //error grabbing steps from coredata
    }
}


func insertStepsFromFirebase(FDataSnapshot daySnapshot:FDataSnapshot, String syncId:String, String isoDate:String){
    
    
    var stepsChild:FDataSnapshot = daySnapshot.childSnapshotForPath("count")
    //        println(stepsChild.value)
    var countString = daySnapshot.childSnapshotForPath("count").valueInExportFormat() as? NSString
    var countInt:Int16 = Int16(countString!.integerValue)
    var isoStart:String = isoDate + (daySnapshot.childSnapshotForPath("starttime").valueInExportFormat() as? String)!
    var isoStop:String = isoDate + (daySnapshot.childSnapshotForPath("endtime").valueInExportFormat() as? String)!
    var serialnumber:String = (daySnapshot.childSnapshotForPath("deviceid").valueInExportFormat() as? String)!
    var startTime = createDateFromString(String: isoStart)
    var stopTime = createDateFromString(String: isoStop)
    
    var isDuplicate = false
    
    let predicate = NSPredicate(format:"%@ == starttime AND %@ == endtime AND %@ == serialnumber", startTime, stopTime, serialnumber)
    
    let fetchRequestDupeCheck = NSFetchRequest(entityName: "StepEntry")
    fetchRequestDupeCheck.predicate = predicate
    if let fetchResults = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequestDupeCheck, error: nil) as? [StepEntry] {
        if(fetchResults.count > 0){
            //                NSLog("Duplicate found")
            
            for(var i = 0 ; i < fetchResults.count ; i++){
                //                    NSLog("Duplicate %i",fetchResults[i].count)
                
            }
            isDuplicate = true
            
        }
    }
    
    if(!isDuplicate){
        NSLog("Unique entry found, adding to coredata")
        NSLog("Steps: %i Serial: %@ Start: %@ Stop: %@",countInt, serialnumber,startTime,stopTime)
        var newItem = NSEntityDescription.insertNewObjectForEntityForName("StepEntry", inManagedObjectContext:(UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!) as! StepEntry
        newItem.count = countInt
        newItem.user = UserData.getOrCreateUserData().getCurrentUID()
        newItem.syncid = syncId
        newItem.starttime = startTime
        newItem.endtime = stopTime
        newItem.serialnumber = serialnumber
        
        
        
        
        
    }else{
        //            NSLog("Duplicate entry found, not adding to coredata")
    }
    (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.save(nil)
    
    
    
}


func retrieveFBDataForYMD(Year: Int, Month: Int, Day: Int) {
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
            
            
            }, withCancelBlock: { error in
                println(error.description)
        })
    }
    
}


