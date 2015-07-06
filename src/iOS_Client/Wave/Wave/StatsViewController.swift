//
//  StatsViewController.swift
//  Wave
//
//  Created by Phil Gandy on 5/21/15.
//
//

import Foundation
import CoreData
import UIKit
import Charts


class StatsViewController: UIViewController, FBUpdateDelegate  {
    let cal:NSCalendar =  NSLocale.currentLocale().objectForKey(NSLocaleCalendar) as! NSCalendar
    var todayDate:Int = 0
    var todayMonth:Int = 0
    var todayYear:Int = 0
    //    var days:Int? = nil
    
    var stepsCount : [Int] = [Int]()
    
    var date = NSDate()
    
    var stepsDataSet : LineChartDataSet?
    var milesDataSet : LineChartDataSet?
    var caloriesDataSet : LineChartDataSet?
    var xVals : [String]?
    var updatingDataLock = false
    
    @IBOutlet weak var chartView: LineChartView!
    @IBOutlet weak var containerView: UIView!
    @IBOutlet weak var stepsCountLabel: UILabel!
    @IBOutlet weak var stepsAvg: UILabel!
    @IBOutlet weak var milesCountLabel: UILabel!
    @IBOutlet weak var milesAvg: UILabel!
    @IBOutlet weak var caloriesCountLabel: UILabel!
    @IBOutlet weak var caloriesAvg: UILabel!
    @IBOutlet weak var caloriesButtonDisplay: UIButton!
    @IBOutlet weak var milesButtonDisplay: UIButton!
    @IBOutlet weak var stepsButtonDisplay: UIButton!
    
    
    @IBOutlet weak var dateLabel: UILabel!
    
    @IBOutlet weak var backButton: UIButton!
    
    @IBOutlet weak var forwardButton: UIButton!
    
    let managedObjectContext = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext
    
    override func viewDidLoad() {
        super.viewDidLoad()
        let days = cal.rangeOfUnit(.CalendarUnitDay,
            inUnit: .CalendarUnitMonth,
            forDate: date)
        // let todayDate = cal.ordinalityOfUnit(.CalendarUnitDay, inUnit: .CalendarUnitDay, forDate: date)
        todayDate = cal.component(.CalendarUnitDay , fromDate: date)
        todayMonth = cal.component(.CalendarUnitMonth , fromDate: date)
        todayYear = cal.component(.CalendarUnitYear , fromDate: date)
        
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }
    
    override func viewDidAppear(animated: Bool) {
        super.viewDidAppear(animated)
        
        
        if let var fbUserRef:String = UserData.getOrCreateUserData().getCurrentUserRef() as String? {
            
            if(fbUserRef=="Error"){
                //no user is logged in
                NSLog("No user logged in")
            } else {
                NSLog("Grabbing user steps from firebase")
                dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0), {
                    retrieveFBDataForYM(self.todayYear, self.todayMonth, self)
                    
                })
            }
        } else {
            //firebase ref is null from coredata
            NSLog("MyLife we shouldn't enter this block, coredata should never be null")
        }
        
        resetDate(0)
    }
    
    func loadDataForYM(year: Int, month: Int) {
        
        NSLog("Entered loadDataForYM " + String(year) + "-" + String(month))
        
        var totalActualDays : Double = 0.0  // For when we are in the current month and the month is not yet finished
        
        stepsCount = [Int]()
        var caloriesList : [Double] = [Double]()
        var numDays = cal.rangeOfUnit(.CalendarUnitDay,
                                      inUnit: .CalendarUnitMonth,
                                      forDate: date).toRange()!.endIndex-1
        
        NSLog("Number of Days: " + String(numDays))
        
        //need to set up today logic like myLifeView
        for(var i=1;i<(numDays+1);i++){
            if let dateStart : NSDate = YMDLocalToNSDate(year, month, i) {
                stepsCount.append(stepsForDayStarting(dateStart))
                caloriesList.append(caloriesForDayStarting(dateStart))
            }
        }
        
        
        if (cal.component(.CalendarUnitMonth, fromDate: NSDate()) == month) {
            totalActualDays = Double(cal.component(.CalendarUnitDay , fromDate: NSDate()))
        } else {
            totalActualDays = Double(stepsCount.count)
        }
        
        var days:[String] = []
        var stepsYVals:[ChartDataEntry] = []
        var milesYVals: [ChartDataEntry] = []
        var caloriesYVals: [ChartDataEntry] = []
        
        var stepsAvgC = 0.0
        var stepsTotal = 0
        var milesAvgC = 0.0
        var milesTotal = 0.0
        var caloriesAvgC = 0.0
        var caloriesTotal = 0.0
        var height = 5.5
        
        //collect height
        if let heightft = UserData.getOrCreateUserData().getCurrentHeightFeet() {
            if (heightft > 0) {
                height = Double(heightft)
                if let heightin = UserData.getOrCreateUserData().getCurrentHeightInches() {
                    height += Double(heightin)/12.0
                }
            }
            
        }
        
        // Figure out total number of days in this month
        
        
        // What day is today and is there a difference between today and total
        
        NSLog("Building data points: " + String(stepsCount.count))
        //loop through and add steps for each day.
        //WARN: Only using simple calculator until user profile exists
        for (var i=0;i<stepsCount.count;i++){
            var stepCount = stepsCount[i]
            stepsTotal += stepCount
            days.append(String(i+1))
            NSLog("Inside Building data points: " + String(i+1))
            stepsYVals.append(ChartDataEntry(value: Float(stepCount), xIndex:i))
            
            var miles = Calculator.calculate_distance(stepCount, height: Int(height*12.0))
            
            milesTotal += miles
            milesYVals.append(ChartDataEntry(value: Float(miles), xIndex:i))
            
            var calories = caloriesList[i]
            caloriesTotal += calories
            caloriesYVals.append(ChartDataEntry(value: Float(calories), xIndex:i))
        }
        
        stepsAvgC = Double(stepsTotal)/totalActualDays
        caloriesAvgC = caloriesTotal/totalActualDays
        milesAvgC = milesTotal/totalActualDays
        
        xVals = days
        
        stepsDataSet = LineChartDataSet(yVals: stepsYVals, label: "Steps taken per day")
        stepsDataSet!.setColor(UIColor.redColor())
        stepsDataSet!.drawCircleHoleEnabled = false
        stepsDataSet!.drawCirclesEnabled = false
        stepsDataSet!.drawValuesEnabled = false
        stepsDataSet!.drawFilledEnabled = true
        stepsDataSet!.lineWidth = 3
        stepsDataSet!.highlightColor = UIColor.blueColor()
        stepsDataSet!.highlightLineWidth = 1
        
        milesDataSet = LineChartDataSet(yVals: milesYVals, label: "Miles traveled per day")
        milesDataSet!.setColor(UIColor.redColor())
        milesDataSet!.drawCircleHoleEnabled = false
        milesDataSet!.drawCirclesEnabled = false
        milesDataSet!.drawValuesEnabled = false
        milesDataSet!.drawFilledEnabled = true
        milesDataSet!.lineWidth = 3
        milesDataSet!.highlightColor = UIColor.blueColor()
        milesDataSet!.highlightLineWidth = 1
        
        caloriesDataSet = LineChartDataSet(yVals: caloriesYVals, label: "Calories burned per day")
        caloriesDataSet!.setColor(UIColor.redColor())
        caloriesDataSet!.drawCircleHoleEnabled = false
        caloriesDataSet!.drawCirclesEnabled = false
        caloriesDataSet!.drawValuesEnabled = false
        caloriesDataSet!.drawFilledEnabled = true
        caloriesDataSet!.lineWidth = 3
        caloriesDataSet!.highlightColor = UIColor.blueColor()
        caloriesDataSet!.highlightLineWidth = 1
        
        chartView.xAxis.labelHeight = 0
        chartView.descriptionText = ""
        
        dispatch_async(dispatch_get_main_queue(), {
            
            if let milestring = floatCommaNumberFormatter(1).stringFromNumber(milesAvgC) {
                self.milesAvg.text = milestring
            } else {
                self.milesAvg.text = "0.0"
            }
            
            if let milestring = floatCommaNumberFormatter(1).stringFromNumber(milesTotal) {
                self.milesCountLabel.text = milestring
            } else {
                self.milesCountLabel.text = "0.0"
            }
            
            if let stepstring = floatCommaNumberFormatter(0).stringFromNumber(stepsAvgC) {
                 self.stepsAvg.text = stepstring
            } else {
                self.stepsAvg.text = "0"
            }
            
            if let stepstring = floatCommaNumberFormatter(0).stringFromNumber(stepsTotal) {
                self.stepsCountLabel.text = stepstring
            } else {
                self.stepsCountLabel.text = "0"
            }
            
            if let caloriestring = floatCommaNumberFormatter(1).stringFromNumber(caloriesAvgC) {
                self.caloriesAvg.text = caloriestring
            } else {
                self.caloriesAvg.text = "0.0"
            }
            
            if let caloriestring = floatCommaNumberFormatter(1).stringFromNumber(caloriesTotal) {
                self.caloriesCountLabel.text = caloriestring
            } else {
                self.caloriesCountLabel.text = "0.0"
            }
            
        
        
            self.displayStepsChart()
             self.updatingDataLock = false
        })
        
    }
    
    func displayStepsChart(){
/*
        if let cvd = chartView.data {
            if let dataset = stepsDataSet {
                chartView.clearValues()
                cvd.addDataSet(dataset)
                chartView.fitScreen()
                chartView.setNeedsDisplay()
            }
        }
  */
        
        chartView.clearValues()
        chartView.data = LineChartData(xVals: xVals, dataSet: stepsDataSet)
        chartView.gridBackgroundColor = UIColor.whiteColor()
        chartView.setNeedsDisplay()
        
        stepsButtonDisplay.setBackgroundImage(UIImage(named:"StatsToggleSel"), forState: UIControlState.Normal)
        caloriesButtonDisplay.setBackgroundImage(UIImage(named:"StatsToggle"), forState: UIControlState.Normal)
        milesButtonDisplay.setBackgroundImage(UIImage(named:"StatsToggle"), forState: UIControlState.Normal)
        
    }

    
    func displayMilesChart() {
        chartView.clearValues()
        chartView.data = LineChartData(xVals: xVals, dataSet: milesDataSet)
        chartView.gridBackgroundColor = UIColor.whiteColor()

        chartView.setNeedsDisplay()
        
        stepsButtonDisplay.setBackgroundImage(UIImage(named:"StatsToggle"), forState: UIControlState.Normal)
        caloriesButtonDisplay.setBackgroundImage(UIImage(named:"StatsToggle"), forState: UIControlState.Normal)
        milesButtonDisplay.setBackgroundImage(UIImage(named:"StatsToggleSel"), forState: UIControlState.Normal)
        
    }
    
    func displayCaloriesChart() {
        /*
        if let cvd = chartView.data {
            if let dataset = caloriesDataSet {
                chartView.clearValues()
                cvd.addDataSet(dataset)
                chartView.fitScreen()
                chartView.setNeedsDisplay()
            }
        }
*/
        chartView.clearValues()
        chartView.data = LineChartData(xVals: xVals, dataSet: caloriesDataSet)
        chartView.gridBackgroundColor = UIColor.whiteColor()

        chartView.setNeedsDisplay()
        
        stepsButtonDisplay.setBackgroundImage(UIImage(named:"StatsToggle"), forState: UIControlState.Normal)
        caloriesButtonDisplay.setBackgroundImage(UIImage(named:"StatsToggleSel"), forState: UIControlState.Normal)
        milesButtonDisplay.setBackgroundImage(UIImage(named:"StatsToggle"), forState: UIControlState.Normal)
        
    }
    
    @IBAction func stepsButtonPress(sender: AnyObject) {
        displayStepsChart()
    }
    
    @IBAction func milesButtonPress(sender: AnyObject) {
        displayMilesChart()
    }

    
    @IBAction func caloriesButtonPress(sender: AnyObject) {
        displayCaloriesChart()
        
    }
    
    @IBAction func backButtonPress(sender: AnyObject) {
        
        if(!updatingDataLock){
            updatingDataLock = true
            resetDate(-1)
            
        }
    }
    
    @IBAction func forewardButtonPress(sender: AnyObject) {
        if(!updatingDataLock){
            updatingDataLock = true
             resetDate(1)
        }
       
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
            self.dateLabel.text = monthName + " " + yearName
        })
        
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), {
            self.loadDataForYM(self.todayYear, month: self.todayMonth)
        })
        
        
        
        if let var fbUserRef:String = UserData.getOrCreateUserData().getCurrentUserRef() as String? {
            
            if(fbUserRef=="Error"){
                //no user is logged in
                NSLog("No user logged in")
            } else {
                NSLog("Grabbing user steps from firebase")
                dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0), {
                    retrieveFBDataForYM(self.todayYear, self.todayMonth, self)
                   
                    
                })
            }
        } else {
            //firebase ref is null from coredata
            NSLog("MyLife we shouldn't enter this block, coredata should never be null")
        }
//        backButton.enabled = true
        
//        forwardButton.enabled = true

        
        
    }
    
    
    func UpdatedDataFromFirebase() {
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0), {
            self.loadDataForYM(self.todayYear, month: self.todayMonth)
        })
    }
    
    
    
}