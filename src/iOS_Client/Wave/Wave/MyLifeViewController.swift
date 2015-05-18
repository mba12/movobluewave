//
//  MyLifeViewController.swift
//  Wave
//
//  Created by Phil Gandy on 5/11/15.
//  Copyright (c) 2015 Phil Gandy. All rights reserved.
//

import Foundation
import UIKit
import CoreData


//@objc(StepEntry)

class MyLifeViewController: UIViewController, UICollectionViewDelegateFlowLayout, UICollectionViewDataSource {
    let cal:NSCalendar =  NSLocale.currentLocale().objectForKey(NSLocaleCalendar) as! NSCalendar
    var userID = ""
    
    
    //singleton experiment. Can't resolve variable?

    
    @IBOutlet weak var collectionViewHost: UIView!
    // Retreive the managedObjectContext from AppDelegate
    let managedObjectContext = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext
    
    
    
    @IBOutlet weak var collectionView: UICollectionView!
    override func viewDidLoad() {
        super.viewDidLoad()
        let date = NSDate()
        //let cal = NSCalendar(calendarIdentifier:NSCalendarIdentifierGregorian)!
        let days = cal.rangeOfUnit(.CalendarUnitDay,
            inUnit: .CalendarUnitMonth,
            forDate: date)
        // let todayDate = cal.ordinalityOfUnit(.CalendarUnitDay, inUnit: .CalendarUnitDay, forDate: date)
        let todayDate = cal.component(.CalendarUnitDay , fromDate: date)
        let todayMonth = cal.component(.CalendarUnitMonth , fromDate: date)
        let todayYear = cal.component(.CalendarUnitYear , fromDate: date)
        
        
        collectionView!.registerClass(CollectionViewCell.self, forCellWithReuseIdentifier: "CollectionViewCell")
        collectionView!.backgroundColor = UIColor.clearColor()
        collectionViewHost.backgroundColor = UIColor.clearColor()
        
        //Firebase calls
        
        //        var urlString = "https://ss-movo-wave-v2.firebaseio.com/users/simplelogin:7/steps/"
        //        urlString += String(todayYear) + "/"
        //        urlString += String(todayMonth) + "/"
        //        //urlString += String(todayDate)
        //        urlString += "12"
        //        NSLog("%@",urlString)
        //        var myRootRef = Firebase(url:urlString )
        //        // Read data and react to changes
        //        myRootRef.observeEventType(.Value, withBlock: {
        //            snapshot in
        //            println("\(snapshot.key) -> \(snapshot.value)")
        //            })
//        let ref = Firebase(url: "https://ss-movo-wave-v2.firebaseio.com")
//        ref.authUser("philg@sensorstar.com", password: "t",
//            withCompletionBlock: { error, authData in
//                
//                if error != nil {
//                    // There was an error logging in to this account
//                    NSLog("Login failed")
//                } else {
//                    // We are now logged in
//                    NSLog("We logged in as philg: %@",authData.uid)
//                    self.userID = authData.uid
//                    
//                    //            var todayFirebaseRef = Firebase(url:"https://ss-movo-wave-v2.firebaseio.com/users/simplelogin:7/steps/2015/4/15/")
//                    //            // Attach a closure to read the data at our posts reference
//                    //            todayFirebaseRef.observeEventType(.Value, withBlock: { snapshot in
//                    //                println(snapshot.value)
//                    //            }, withCancelBlock: { error in
//                    //                println(error.description)
//                    //                })
//                    
//                    
//                    
//                }
//        })
        println(managedObjectContext)
        //get coredata item
        //        var newItem = NSEntityDescription.insertNe wObjectForEntityForName("StepEntry", inManagedObjectContext: self.managedObjectContext!) as! StepEntry
        //        newItem.count = 50
        //        newItem.guid = "aaaaAaaaa"
        //
        
        // Create a new fetch request using the LogItem entity
        
        if let var fbUserRef:String = UserData.getOrCreateUserData().getCurrentUserRef() as String?{
            if(fbUserRef=="Error"){
                
            }else{
                fbUserRef = fbUserRef + "/steps/2015/4/15"
                var fb = Firebase(url:fbUserRef)
                fb.observeEventType(.Value, withBlock: { snapshot in
                    NSLog("FBRef %@",fbUserRef)
                    
                    
                      var itr = snapshot.children
                            while let rest = itr.nextObject() as? FDataSnapshot {
                                //                    println(rest.value)
                                var itr2 = rest.children
                                while let rest2 = itr2.nextObject() as? FDataSnapshot{
                                    //                    println(rest2.value)
                                    
                                    
                                    var stepsChild:FDataSnapshot = rest2.childSnapshotForPath("count")
                                    println(stepsChild.value)
                                    var newItem = NSEntityDescription.insertNewObjectForEntityForName("StepEntry", inManagedObjectContext: self.managedObjectContext!) as! StepEntry
                                    //            var countInt = rest2.childSnapshotForPath("count").valueInExportFormat() as? NSNumber
                                    newItem.count = (rest2.childSnapshotForPath("count").valueInExportFormat() as? String)!
                                    
                                    //                    newItem.syncid = (rest2.childSnapshotForPath("syncid").valueInExportFormat() as? String)!
                                    newItem.deviceid = (rest2.childSnapshotForPath("deviceid").valueInExportFormat() as? String)!
                                    newItem.starttime = (rest2.childSnapshotForPath("starttime").valueInExportFormat() as? String)!
                                    newItem.endtime = (rest2.childSnapshotForPath("endtime").valueInExportFormat() as? String)!
                                    //            println(newItem.count)
                                    
                                    let fetchRequest = NSFetchRequest(entityName: "StepEntry")
                                    if let fetchResults = self.managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [StepEntry] {
                                        //              NSLog("CoreData: %@",fetchResults[0].count)
                                    }
                                    
                                }
                            }
                            
                            
                    
                    
                    
                    }, withCancelBlock: { error in
                        println(error.description)
                })
                
                
            }
            
            
            
        }else{
            //firebase ref is null from coredata
        }
       
        
        //this is an example of pulling data from a single day
//        var todayFirebaseRef = Firebase(url:"https://ss-movo-wave-v2.firebaseio.com/users/simplelogin:7/steps/2015/4/15/")
//        // Attach a closure to read the data at our posts reference
//        todayFirebaseRef.observeEventType(.Value, withBlock: { snapshot in
//            //        println(snapshot.value)
//            
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
//                    var newItem = NSEntityDescription.insertNewObjectForEntityForName("StepEntry", inManagedObjectContext: self.managedObjectContext!) as! StepEntry
//                    //            var countInt = rest2.childSnapshotForPath("count").valueInExportFormat() as? NSNumber
//                    newItem.count = (rest2.childSnapshotForPath("count").valueInExportFormat() as? String)!
//                    
//                    //                    newItem.syncid = (rest2.childSnapshotForPath("syncid").valueInExportFormat() as? String)!
//                    newItem.deviceid = (rest2.childSnapshotForPath("deviceid").valueInExportFormat() as? String)!
//                    newItem.starttime = (rest2.childSnapshotForPath("starttime").valueInExportFormat() as? String)!
//                    newItem.endtime = (rest2.childSnapshotForPath("endtime").valueInExportFormat() as? String)!
//                    //            println(newItem.count)
//                    
//                    let fetchRequest = NSFetchRequest(entityName: "StepEntry")
//                    if let fetchResults = self.managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [StepEntry] {
//                        //              NSLog("CoreData: %@",fetchResults[0].count)
//                    }
//                    
//                }
//            }
//            
//            
//            //                var valueStr:String  = String(stringInterpolationSegment: snapshot.value)
//            //                cell.textLabel2?.text = valueStr
//            //                println(valueStr)
//            }, withCancelBlock: { error in
//                println(error.description)
//        })
        
        
        
        
        
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
        cell.backgroundColor = UIColor(patternImage: UIImage(named:"splash.png")!)
        //cell.textLabel?.text = "\(indexPath.section):\(indexPath.row)"
        
        //cell.textLabel?.text = "\(indexPath.row)"
        //Do calculations for reverse calendar
        let cellCount = collectionView.numberOfItemsInSection(0)
        let cellDateNumber = abs(indexPath.row - cellCount)
        cell.textLabel?.text = "\(cellDateNumber)"
        
        //this block should pull data from coredata instead of doing live firebase calls
        let fetchRequest = NSFetchRequest(entityName: "StepEntry")
        if let fetchResults = self.managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [StepEntry] {
            if(fetchResults.count > 0){
                            NSLog("Fetch Results length: %i result: %@", fetchResults.count, fetchResults)
                                NSLog("CoreData: %@",fetchResults[0].count)
                                cell.textLabel2?.text = fetchResults[0].count
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
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    
}