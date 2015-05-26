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

class StatsViewController: UIViewController  {
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
        
        resetDate(0)
        
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }
    
    override func viewDidAppear(animated: Bool) {
        super.viewDidAppear(animated)
        
        
        
        
    }
    
    func loadDataForYM(year: Int, month: Int) {
        stepsCount = [Int]()
        var numDays = cal.rangeOfUnit(.CalendarUnitDay,
            inUnit: .CalendarUnitMonth,
            forDate: date).toRange()!.endIndex
        
        if (cal.component(.CalendarUnitMonth, fromDate: NSDate()) == month) {
            numDays = cal.component(.CalendarUnitDay , fromDate: NSDate())
        }
        
        //need to set up today logic like myLifeView
        for(var i=1;i<(numDays+1);i++){
            
            if let dateStart : NSDate = YMDLocalToNSDate(year, month, i) {
                stepsCount.append(stepsForDayStarting(dateStart))
            }
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
        
        //loop through and add steps for each day.
        //WARN: Only using simple calculator until user profile exists
        for (var i=0;i<stepsCount.count;i++){
            var stepCount = stepsCount[i]
            stepsTotal += stepCount
            days.append(String(i+1))
            stepsYVals.append(ChartDataEntry(value: Float(stepCount), xIndex:i))
            var miles = Calculator.calculate_distance(stepCount, height: Int(5.5*12))
            milesTotal += miles
            milesYVals.append(ChartDataEntry(value: Float(miles), xIndex:i))
            var calories = Calculator.simple_calculate_calories(int: stepCount)
            caloriesTotal += calories
            caloriesYVals.append(ChartDataEntry(value: Float(calories), xIndex:i))
        }
        
        
        stepsAvgC = Double(stepsTotal)/Double(stepsCount.count)
        caloriesAvgC = caloriesTotal/Double(stepsCount.count)
        milesAvgC = milesTotal/Double(stepsCount.count)
        
        xVals = days
        
        
        stepsDataSet = LineChartDataSet(yVals: stepsYVals, label: "Steps taken per day")
        stepsDataSet!.setColor(UIColor.redColor())
        milesDataSet = LineChartDataSet(yVals: milesYVals, label: "Miles traveled per day")
        milesDataSet!.setColor(UIColor.redColor())
        caloriesDataSet = LineChartDataSet(yVals: caloriesYVals, label: "Calories burned per day")
        caloriesDataSet!.setColor(UIColor.redColor())
        
        chartView.xAxis.labelHeight = 0
        
        dispatch_async(dispatch_get_main_queue(), {
            self.milesAvg.text = String(format: "%.1f", milesAvgC)
            self.stepsAvg.text = String(format: "%.0f", stepsAvgC)
            self.caloriesAvg.text = String(format: "%.0f", caloriesAvgC)
            
            self.caloriesCountLabel.text = String(format: "%.0f", caloriesTotal)
            self.milesCountLabel.text = String(format: "%.0f", milesTotal)
            self.stepsCountLabel.text = String(stepsTotal)
            
        })
        
        displayStepsChart()
        
        
        
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
        chartView.setNeedsDisplay()
        
        stepsButtonDisplay.setBackgroundImage(UIImage(named:"buttonRed"), forState: UIControlState.Normal)
        caloriesButtonDisplay.setBackgroundImage(UIImage(named:"instabutton"), forState: UIControlState.Normal)
        milesButtonDisplay.setBackgroundImage(UIImage(named:"instabutton"), forState: UIControlState.Normal)
        
    }

    
    func displayMilesChart() {
        chartView.clearValues()
        chartView.data = LineChartData(xVals: xVals, dataSet: milesDataSet)
        chartView.setNeedsDisplay()
        
        stepsButtonDisplay.setBackgroundImage(UIImage(named:"instabutton"), forState: UIControlState.Normal)
        caloriesButtonDisplay.setBackgroundImage(UIImage(named:"instabutton"), forState: UIControlState.Normal)
        milesButtonDisplay.setBackgroundImage(UIImage(named:"buttonRed"), forState: UIControlState.Normal)
        
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
        chartView.setNeedsDisplay()
        
        stepsButtonDisplay.setBackgroundImage(UIImage(named:"instabutton"), forState: UIControlState.Normal)
        caloriesButtonDisplay.setBackgroundImage(UIImage(named:"buttonRed"), forState: UIControlState.Normal)
        milesButtonDisplay.setBackgroundImage(UIImage(named:"instabutton"), forState: UIControlState.Normal)
        
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
                resetDate(-1)
    }
    
    @IBAction func forewardButtonPress(sender: AnyObject) {
                resetDate(1)
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
            self.loadDataForYM(self.todayYear, month: self.todayMonth)
        })
        
    }
    
    
    
}