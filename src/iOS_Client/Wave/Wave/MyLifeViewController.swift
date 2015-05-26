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
    
    @IBOutlet weak var dateLabel: UILabel!
    
    @IBOutlet weak var forwardButton: UIButton!
    @IBOutlet weak var backwardButton: UIButton!
    
    // Retreive the managedObjectContext from AppDelegate
    let managedObjectContext = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext
    var todayDate:Int = 0
    var todayMonth:Int = 0
    var todayYear:Int = 0
//    var days:Int? = nil
    
    var date : NSDate = NSDate()
    //for passing context to daily view
    var selectedDay:Int = 0
    var selectedMonth:Int = 0
    var selectedYear:Int = 0
    
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
         date = NSDate()
//        let date = NSDate()
        //let cal = NSCalendar(calendarIdentifier:NSCalendarIdentifierGregorian)!
        // let todayDate = cal.ordinalityOfUnit(.CalendarUnitDay, inUnit: .CalendarUnitDay, forDate: date)
        todayDate = cal.component(.CalendarUnitDay , fromDate: date)
        todayMonth = cal.component(.CalendarUnitMonth , fromDate: date)
        todayYear = cal.component(.CalendarUnitYear , fromDate: date)
        
        self.collectionView!.registerClass(CollectionViewCell.self, forCellWithReuseIdentifier: "CollectionViewCell")
        self.collectionView!.backgroundColor = UIColor.clearColor()
        self.collectionViewHost.backgroundColor = UIColor.clearColor()
        
        resetDate(0)
        
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
                        retrieveFBDataForYMDGMT(todayYear, todayMonth, todayDate)
                        
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
        //let cal = NSCalendar(calendarIdentifier:NSCalendarIdentifierGregorian)!
        var days = cal.rangeOfUnit(.CalendarUnitDay,
            inUnit: .CalendarUnitMonth,
            forDate: date).toRange()!.endIndex
        
        if (cal.component(.CalendarUnitMonth, fromDate: NSDate()) == todayMonth) {
            days = cal.component(.CalendarUnitDay , fromDate: NSDate())
        }
        
        return days
    }
    
    func collectionView(collectionView: UICollectionView, cellForItemAtIndexPath indexPath: NSIndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCellWithReuseIdentifier("CollectionViewCell", forIndexPath: indexPath) as! CollectionViewCell

        
        
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
        if (cal.component(.CalendarUnitMonth, fromDate: NSDate()) == todayMonth && (collectionView.numberOfItemsInSection(0) - indexPath.row) == cal.component(.CalendarUnitDay, fromDate: NSDate())) {
                cell.imageView?.image = UIImage(named: "datebgwide")
                cell.textLabel?.text = "Today"
        
        } else {
        
            cell.imageView?.image = UIImage(named: "datebgcircle")
        }
        
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
    
    @IBAction func forwardButtonPress(sender: AnyObject) {
        
        resetDate(1)
    }
    
    @IBAction func backButtonPress(sender: AnyObject) {
        resetDate(-1)
    }
    
    func resetDate(monthIncrement: Int) {

        
        var year = todayYear
        var month : Int = todayMonth
        month += monthIncrement
        while (month <= 0) {
            month += 12
            year -= 1
        }
        while (month > 12) {
            month -= 12
            year += 1
        }
        
        todayMonth = month
        todayYear = year
        
        date = YMDLocalToNSDate(todayYear, todayMonth, 1)!
        if (cal.component(.CalendarUnitMonth, fromDate: NSDate()) == todayMonth) {
            //then turn off the forward button
            forwardButton.enabled = false
            forwardButton.hidden = true

        } else {
            //turn on the forward button
            forwardButton.enabled = true
            forwardButton.hidden = false
        }
        
        //set the text label
        let dateFormat = NSDateFormatter()
        dateFormat.dateStyle = NSDateFormatterStyle.LongStyle
        let monthName : String = dateFormat.monthSymbols[month-1] as! String
        let yearName : String = String(cal.component(.CalendarUnitYear, fromDate: date))
        dispatch_async(dispatch_get_main_queue(), {
            self.dateLabel.text = monthName + ", " + yearName
            self.collectionView.reloadData()
        })
        
    }
    
    
}


