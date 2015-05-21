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

