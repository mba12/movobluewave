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
        var calendar = NSCalendar.currentCalendar()
        calendar.timeZone = NSTimeZone.localTimeZone()
        var days:[String] = []
        //        var steps:[Int] = []
        var yVals:[ChartDataEntry] = []
        
        //loop through and add steps for each day.
        for(var i=0;i<todayDate;i++){
            
            var startTimeComponents = NSDateComponents()
            
            startTimeComponents.setValue(i, forComponent: NSCalendarUnit.CalendarUnitDay)
            startTimeComponents.setValue(todayYear, forComponent: NSCalendarUnit.CalendarUnitYear)
            startTimeComponents.setValue(todayMonth, forComponent: NSCalendarUnit.CalendarUnitMonth)
            startTimeComponents.setValue(0, forComponent: NSCalendarUnit.CalendarUnitHour)
            startTimeComponents.setValue(0, forComponent: NSCalendarUnit.CalendarUnitMinute)
            startTimeComponents.setValue(0, forComponent: NSCalendarUnit.CalendarUnitSecond)
            
            var dateStart : NSDate = calendar.dateFromComponents(startTimeComponents)!
            
            var dateStop = dateStart.dateByAddingTimeInterval(60*60*24); //24hrs
            
            var stepCount = getStepsForTimeInterval(NSDate: dateStart, NSDate: dateStop)
            
            days.append(String(i+1))
            yVals.append(ChartDataEntry(value: Float(stepCount), xIndex:i))
            
        }
        
        
        
        let xVals = days
        
        
        let set1 = LineChartDataSet(yVals: yVals, label: "Steps")
        let data = LineChartData(xVals: xVals, dataSet: set1)
        chartView.xAxis.labelHeight = 0
        chartView.data = data
        self.view.reloadInputViews()
        
        
        
        
    }
    
    func getStepsForTimeInterval(NSDate dateStart:NSDate, NSDate dateStop:NSDate) -> Int{
        
        
        
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
                
                return totalStepsForToday
            }else{
                return 0
            }
        } else {
            return 0
        }
    }
    
    
    
}