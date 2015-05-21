//
//  MyLifeViewController.swift
//  Wave
//
//  Created by Phil Gandy on 5/11/15.
//

import Foundation
import UIKit
import CoreData


//@objc(StepEntry)

class MyLifeViewController: UIViewController, UICollectionViewDelegateFlowLayout, UICollectionViewDataSource, UICollectionViewDelegate {
    let cal:NSCalendar =  NSLocale.currentLocale().objectForKey(NSLocaleCalendar) as! NSCalendar
    //    var userID = ""
    
    
    //singleton experiment. Can't resolve variable?
    
    
    @IBOutlet weak var collectionViewHost: UIView!
    
    @IBOutlet weak var collectionView: UICollectionView!
    // Retreive the managedObjectContext from AppDelegate
    let managedObjectContext = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext
    var todayDate:Int = 0
    var todayMonth:Int = 0
    var todayYear:Int = 0
//    var days:Int? = nil
    var date = NSDate()
    
    
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
//        let date = NSDate()
        //let cal = NSCalendar(calendarIdentifier:NSCalendarIdentifierGregorian)!
        let days = cal.rangeOfUnit(.CalendarUnitDay,
            inUnit: .CalendarUnitMonth,
            forDate: date)
        // let todayDate = cal.ordinalityOfUnit(.CalendarUnitDay, inUnit: .CalendarUnitDay, forDate: date)
        todayDate = cal.component(.CalendarUnitDay , fromDate: date)
        todayMonth = cal.component(.CalendarUnitMonth , fromDate: date)
        todayYear = cal.component(.CalendarUnitYear , fromDate: date)
        
        
        self.collectionView!.registerClass(CollectionViewCell.self, forCellWithReuseIdentifier: "CollectionViewCell")
        self.collectionView!.backgroundColor = UIColor.clearColor()
        self.collectionViewHost.backgroundColor = UIColor.clearColor()
        
        println(managedObjectContext)
        
        // Create a new fetch request using the LogItem entity
//        if let var fbUserRef:String = UserData.getOrCreateUserData().getCurrentUserRef() as String?{
//            
//            if(fbUserRef=="Error"){
//                NSLog("No user logged in, logging in as philg@sensorstar.com")
//                //                let predicate = NSPredicate(format:"%@ >= starttime AND %@ <= endtime AND %@ == user", dateStop, dateStart,UserData.getOrCreateUserData().getCurrentUID())
//                
//                let fetchRequest = NSFetchRequest(entityName: "UserEntry")
//                //                fetchRequest.predicate = predicate
//                if let fetchResults = self.managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [UserEntry] {
//                    
//                    if(fetchResults.count > 0){
//                        UserData.getOrCreateUserData().createUser(
//                            String: fetchResults[0].id,
//                            String: fetchResults[0].email,
//                            String: fetchResults[0].pw,
//                            NSDate: fetchResults[0].birthdate,
//                            Int: Int(fetchResults[0].heightfeet),
//                            Int: Int(fetchResults[0].heightinches),
//                            Int: Int(fetchResults[0].weight),
//                            String: fetchResults[0].gender,
//                            String: fetchResults[0].fullname,
//                            String: fetchResults[0].username,
//                            String: fetchResults[0].reference)
////                        UserData.getOrCreateUserData().createUser(String: fetchResults[0].id, String: fetchResults[0].email, String: fetchResults[0].pw, NSDate:fetchResults[0].birthdate, Int: fetchResults[0].heightinches, Int: 0, Int: 0, String: "Male", String: "Phil Gandy", String: "pgandy", String: "")
//                        
////                        CreateUser(String uid:String, String email:String, String pw:String, NSDate birth:NSDate, Int height1:Int, Int height2:Int, Int weight:Int, String gender:String, String fullName:String, String user:String, String ref:String){
//                        
//                    }else{
//                        let ref = Firebase(url: "https://ss-movo-wave-v2.firebaseio.com")
//                        ref.authUser("9@9.com", password: "9",
//                            withCompletionBlock: { error, authData in
//                                
//                                if error != nil {
//                                    // There was an error logging in to this account
//                                    NSLog("Login failed")
//                                } else {
//                                    // We are now logged in
//                                    NSLog("We logged in as 9: %@",authData.uid)
//                                    //                                    self.userID = authData.uid
//                                    var ref = "https://ss-movo-wave-v2.firebaseio.com"
//                                    ref = ref + "/users/"
//                                    ref = ref + authData.uid
//                                    UserData.getOrCreateUserData().createUser(String: authData.uid, String: "9@9.com", String: "9", NSDate: NSDate(), Int: 0, Int: 0, Int: 0, String: "Male", String: "Phil Gandy", String: "pgandy", String: ref)
//                                    
//                                    //                            self.retrieveData()
//                                    
//                                    
//                                }
//                        })
//
//                    }
//                } else {
//                    
//                }
//
//                
//                
//                
//                
//                
//            } else {
////                retrieveData()
//                
//            }
//        } else {
//            //firebase ref is null from coredata
//            NSLog("MyLife we shuldn't enter this block, coredata should never be null")
//        }
        
        
    }
    
    func insertStepsFromFirebase(FDataSnapshot daySnapshot:FDataSnapshot, String syncId:String, String isoDate:String){
        
        
        var stepsChild:FDataSnapshot = daySnapshot.childSnapshotForPath("count")
        println(stepsChild.value)
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
        if let fetchResults = self.managedObjectContext!.executeFetchRequest(fetchRequestDupeCheck, error: nil) as? [StepEntry] {
            if(fetchResults.count > 0){
                NSLog("Duplicate found")

                for(var i = 0 ; i < fetchResults.count ; i++){
                    NSLog("Duplicate %i",fetchResults[i].count)
                    
                }
                isDuplicate = true
                
            }
        }
        
        if(!isDuplicate){
            NSLog("Unique entry found, adding to coredata")
            NSLog("Steps: %i Serial: %@ Start: %@ Stop: %@",countInt, serialnumber,startTime,stopTime)
            var newItem = NSEntityDescription.insertNewObjectForEntityForName("StepEntry", inManagedObjectContext: self.managedObjectContext!) as! StepEntry
            newItem.count = countInt
            newItem.user = UserData.getOrCreateUserData().getCurrentUID()
            newItem.syncid = syncId
            newItem.starttime = startTime
            newItem.endtime = stopTime
            newItem.serialnumber = serialnumber
            
            
            
            
//            let fetchRequest = NSFetchRequest(entityName: "StepEntry")
//            if let fetchResults = self.managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [StepEntry] {
//                //              NSLog("CoreData: %@",fetchResults[0].count)
//            }
        }else{
            NSLog("Duplicate entry found, not adding to coredata")
        }
        self.managedObjectContext!.save(nil)

        
        
    }
    
    
    func retrieveData() {
        if let var fbUserRef:String = UserData.getOrCreateUserData().getCurrentUserRef() as String?{
            var year:String = String(todayYear)
            var month:String = String(todayMonth)
            
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
            fbMonth.observeEventType(.Value, withBlock: { snapshot in
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
                            self.insertStepsFromFirebase(FDataSnapshot: daySnap, String:syncId, String:isoDate)
                            
                        }
                    }

                }
                
                
                }, withCancelBlock: { error in
                    println(error.description)
            })

        
        
            
            
            
//            fbUserRef = fbUserRef + "/steps/2015/4/15"
//            var fb = Firebase(url:fbUserRef)
//            fb.observeEventType(.Value, withBlock: { snapshot in
//                NSLog("FBRef %@",fbUserRef)
//                
//                
//                var itr = snapshot.children
//                while let rest = itr.nextObject() as? FDataSnapshot {
//                    //this is the root node for a steps object
//                    var syncId = rest.key
////                    NSLog("Syncid is %@",syncId!)
//                    var itr2 = rest.children
//                    while let daySnap = itr2.nextObject() as? FDataSnapshot{
//                        //this steps into the node title and gets the objects
//                        self.insertStepsFromFirebase(FDataSnapshot: daySnap, String:syncId)
//                        
//                    }
//                }
//                }, withCancelBlock: { error in
//                    println(error.description)
//            })
            
        }
        
    }
    
    func numberOfSectionsInCollectionView(collectionView: UICollectionView) -> Int {
        return 1
    }
    
    func collectionView(collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        let date = NSDate()
        //let cal = NSCalendar(calendarIdentifier:NSCalendarIdentifierGregorian)!
        let days = cal.rangeOfUnit(.CalendarUnitDay,
            inUnit: .CalendarUnitMonth,
            forDate: date)
        let todayDate = cal.component(.CalendarUnitDay , fromDate: date)
        
        
        
        return todayDate
    }
    
    func collectionView(collectionView: UICollectionView, cellForItemAtIndexPath indexPath: NSIndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCellWithReuseIdentifier("CollectionViewCell", forIndexPath: indexPath) as! CollectionViewCell
        cell.backgroundColor = UIColor(patternImage: UIImage(named:"splash")!)
        cell.textLabel?.text = "\(indexPath.section):\(indexPath.row)"
        
        cell.textLabel?.text = "\(indexPath.row)"
        //Do calculations for reverse calendar
        
        
        //This won't work//
        //This doesn't account for local time or GMT
        /*
        let cellCount = collectionView.numberOfItemsInSection(0)
        let cellDateNumber = abs(indexPath.row - cellCount)
        cell.textLabel?.text = "\(cellDateNumber)"
        
        var year:String = String(todayYear)
        var month:String = String(todayMonth)
        var iso8601String:String = year
        iso8601String = iso8601String + "-"
        iso8601String = iso8601String + month
        iso8601String = iso8601String + "-"

        var isoStartString = iso8601String + String(cellDateNumber)
        isoStartString = isoStartString + "T"
        isoStartString = isoStartString + "00:00:00Z"
        var isoStopString = iso8601String + String(cellDateNumber)
        isoStopString = isoStopString + "T"
        isoStopString = isoStopString + "23:59:59Z"
        
        var dateStart = createDateFromString(String: isoStartString)
        var dateStop = createDateFromString(String: isoStopString)
        */
        
        //This display should be built from LOCAL TIME
        let cellCount = collectionView.numberOfItemsInSection(0)
        let cellDateNumber = abs(indexPath.row - cellCount)
        var calendar = NSCalendar.currentCalendar()
        calendar.timeZone = NSTimeZone.localTimeZone()
        var startTimeComponents = NSDateComponents()
        startTimeComponents.setValue(cellDateNumber, forComponent: NSCalendarUnit.CalendarUnitDay)
        startTimeComponents.setValue(todayYear, forComponent: NSCalendarUnit.CalendarUnitYear)
        startTimeComponents.setValue(todayMonth, forComponent: NSCalendarUnit.CalendarUnitMonth)
        startTimeComponents.setValue(0, forComponent: NSCalendarUnit.CalendarUnitHour)
        startTimeComponents.setValue(0, forComponent: NSCalendarUnit.CalendarUnitMinute)
        startTimeComponents.setValue(0, forComponent: NSCalendarUnit.CalendarUnitSecond)
        
        var dateStart : NSDate = calendar.dateFromComponents(startTimeComponents)!
        
        var dateStop = dateStart.dateByAddingTimeInterval(60*60*24); //24hrs
        
        
        
        let predicate = NSPredicate(format:"%@ <= starttime AND %@ >= endtime AND %@ == user", dateStart, dateStop, UserData.getOrCreateUserData().getCurrentUID())

        let fetchRequest = NSFetchRequest(entityName: "StepEntry")
        fetchRequest.predicate = predicate
        if let fetchResults = self.managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [StepEntry] {
            
            var totalStepsForToday = 0
            if(fetchResults.count > 0){
                println("Count %i",fetchResults.count)
                var resultsCount = fetchResults.count
                for(var i=0;i<(resultsCount);i++){
//                    println("Adding steps up for %i %i",cellDateNumber, Int(fetchResults[i].count))
                    totalStepsForToday = totalStepsForToday + Int(fetchResults[i].count)
                }
                
                var countString = String(totalStepsForToday)
                cell.textLabel2?.text = countString
            }else{
                cell.textLabel2?.text = "0"
            }
        } else {
            cell.textLabel2?.text = "0"
        }
        
        //this will pull steps for a day in april and display them.
        //        var urlString = "https://ss-movo-wave-v2.firebaseio.com/users/simplelogin:7/steps/2015/4/"
        //        urlString = urlString + String(cellDateNumber)
        //        var todayFirebaseRef = Firebase(url:urlString)
        //        // Attach a closure to read the data at our posts reference
        //        todayFirebaseRef.observeEventType(.Value, withBlock: { snapshot in
        //
        //            let children = snapshot.hasChildren()
        //            if(children){
        //            var itr = snapshot.children
        //            while let rest = itr.nextObject() as? FDataSnapshot {
        //                //                    println(rest.value)
        //                var itr2 = rest.children
        //                while let rest2 = itr2.nextObject() as? FDataSnapshot{
        //                    //                    println(rest2.value)
        //
        //
        //                    var stepsChild:FDataSnapshot = rest2.childSnapshotForPath("count")
        //                    println(stepsChild.value)
        //
        //                    var todaysSteps = (rest2.childSnapshotForPath("count").valueInExportFormat() as? String)!
        //                    cell.textLabel2?.text = todaysSteps
        //
        //                }
        //            }
        //            }else{
        //                cell.textLabel2?.text = "0"
        //            }
        
        
        //                var valueStr:String  = String(stringInterpolationSegment: snapshot.value)
        //                cell.textLabel2?.text = valueStr
        //                println(valueStr)
        //            }, withCancelBlock: { error in
        //                println(error.description)
        //        })
        
        
        
        cell.imageView?.image = UIImage(named: "datebgwide")
        
        return cell
    }
    
    
    
    func createDateFromString(String isoString:String) -> NSDate{
        let form = NSDateFormatter()
        form.dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z"
//        form.timeZone = NSTimeZone(forSecondsFromGMT: 0)
        form.calendar = NSCalendar(calendarIdentifier: NSCalendarIdentifierISO8601)!
        form.locale = NSLocale(localeIdentifier: "en_US_POSIX")
        
        var dateReturn = form.dateFromString(isoString)
        return dateReturn!
        
        
    }

    override func viewDidAppear(animated: Bool) {
        dispatch_async(dispatch_get_main_queue(), {
            self.collectionView.reloadData()
        })
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    
    func collectionView(collectionView: UICollectionView, didSelectItemAtIndexPath indexPath: NSIndexPath) {
        performSegueWithIdentifier("ShowDailyView", sender: self)
    }
    
    
}