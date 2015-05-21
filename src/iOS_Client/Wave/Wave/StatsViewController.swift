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
    var date = NSDate()
    @IBOutlet weak var chartView: LineChartView!
    @IBOutlet weak var containerView: UIView!
    
    
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
        
        displayStepsChart()
        
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }
    
    override func viewDidAppear(animated: Bool) {
        super.viewDidAppear(animated)
        
        
        
        
    }
    
    
    func displayStepsChart(){
        var days:[String] = []
        //        var steps:[Int] = []
        var yVals:[ChartDataEntry] = []
        
        //loop through and add steps for each day.
        for(var i=0;i<todayDate;i++){
            
            if let dateStart : NSDate = YMDLocalToNSDate(todayYear, todayMonth, i) {            
                var stepCount = stepsForDayStarting(dateStart)
                days.append(String(i+1))
                yVals.append(ChartDataEntry(value: Float(stepCount), xIndex:i))
            }
            
        }
        
        
        
        let xVals = days
        
        
        let set1 = LineChartDataSet(yVals: yVals, label: "Steps")
        let data = LineChartData(xVals: xVals, dataSet: set1)
        chartView.xAxis.labelHeight = 0
        chartView.data = data
        self.view.reloadInputViews()
        
        
        
        
    }
    

    
    
}