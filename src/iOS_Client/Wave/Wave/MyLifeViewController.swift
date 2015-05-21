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
    
    @IBOutlet weak var containerView: UIView!
    
    @IBOutlet weak var collectionViewHost: UIView!
    
    @IBOutlet weak var collectionView: UICollectionView!
    // Retreive the managedObjectContext from AppDelegate
    let managedObjectContext = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext
    var todayDate:Int = 0
    var todayMonth:Int = 0
    var todayYear:Int = 0
//    var days:Int? = nil
    var date = NSDate()
    
    //for passing context to daily view
    var selectedDay:Int = 0
    var selectedMonth:Int = 0
    var selectedYear:Int = 0
    
    
    
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
        
        
        
    }
    
    
    override func viewWillAppear(animated: Bool) {
        super.viewWillAppear(animated)
//         Create a new fetch request using the LogItem entity
                if let var fbUserRef:String = UserData.getOrCreateUserData().getCurrentUserRef() as String?{
        
                    if(fbUserRef=="Error"){
                        //no user is logged in
                        NSLog("No user logged in")
                    } else {
                        NSLog("Grabbing user steps from firebase")
                        retrieveFBDataForYMD(todayYear, todayMonth, todayDate)
                        
                    }
                } else {
                    //firebase ref is null from coredata
                    NSLog("MyLife we shouldn't enter this block, coredata should never be null")
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

        
        //This display should be built from LOCAL TIME
        let cellCount = collectionView.numberOfItemsInSection(0)
        let cellDateNumber = abs(indexPath.row - cellCount)
        cell.textLabel?.text = "\(cellDateNumber)"

        if let dateStart : NSDate = YMDLocalToNSDate(todayYear, todayMonth, cellDateNumber) {
            cell.textLabel2?.text = String(stepsForDayStarting(dateStart))
            
        } else {
            cell.textLabel2?.text = "0"
            
        }
        
        
        cell.imageView?.image = UIImage(named: "datebgwide")
        
        return cell
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
        selectedDay =  collectionView.numberOfItemsInSection(0) - indexPath.row
        
        //temporary
        selectedMonth = todayMonth
        selectedYear = todayYear
        
        //perform segue
        performSegueWithIdentifier("ShowDailyView", sender: self)

    }
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        if let name = segue.identifier {
            if (name == "ShowDailyView") {
                if let dailyVC = segue.destinationViewController as? DailyViewController {   
                    dailyVC.currentDate = YMDLocalToNSDate(selectedYear, selectedMonth, selectedDay)
                }
                
            }
        }
    }
    
}


