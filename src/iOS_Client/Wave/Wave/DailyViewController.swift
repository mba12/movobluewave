//
//  DailyViewController.swift
//  Wave
//
//  Created by Rudy Yukich on 5/20/15.
//
//


import Foundation
import UIKit
import CoreData

class DailyViewController : UIViewController, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
    
    @IBOutlet weak var navigationBar: UINavigationBar!
    @IBOutlet weak var distanceLabel: UILabel!
    @IBOutlet weak var calorieLabel: UILabel!
    @IBOutlet weak var stepsLabel: UILabel!
    @IBOutlet weak var backgroundImage: UIImageView!
    var currentDate : NSDate?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        var recognizer: UISwipeGestureRecognizer = UISwipeGestureRecognizer(target: self, action: "swipeLeft:")
        recognizer.direction = .Left
        self.view.addGestureRecognizer(recognizer)
        
        recognizer = UISwipeGestureRecognizer(target: self, action: "swipeRight:")
        recognizer.direction = .Right
        self.view.addGestureRecognizer(recognizer)
        
    }
    
    override func viewWillAppear(animated: Bool) {
        updateDisplay()
    }
    

    @IBAction func doneButtonPressed(sender: AnyObject) {
        self.dismissViewControllerAnimated(true, completion: nil)
    }
    
    
    @IBAction func photoButtonPressed(sender: AnyObject) {
        var imagePicker = UIImagePickerController()
        imagePicker.delegate = self
        self.presentViewController(imagePicker, animated: true, completion: nil)
    }
    
    func imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [NSObject : AnyObject]) {
            //do nothing
    }
    
    func swipeLeft(recognizer : UISwipeGestureRecognizer) {
        if (currentDate != nil) {
            currentDate = currentDate?.dateByAddingTimeInterval(60*60*24);
        }
        updateDisplay()
    }
    
    func swipeRight(recognizer: UISwipeGestureRecognizer) {
        if (currentDate != nil) {
            currentDate = currentDate?.dateByAddingTimeInterval(-60*60*24);
        }
        updateDisplay()
    }
    
    func updateDisplay() {
        
        if let date = currentDate {
            
            /* Set the date string */
            var calendar = NSCalendar.currentCalendar()
            var formatter = NSDateFormatter()
            formatter.dateStyle = NSDateFormatterStyle.MediumStyle
            formatter.timeZone = NSTimeZone.localTimeZone()
            if let item = navigationBar.topItem {
                dispatch_async(dispatch_get_main_queue(), {
                    item.title = formatter.stringFromDate(date)
                })
            }
            
            
            /* Set the steps */
            var steps = stepsForDayStarting(date)
            dispatch_async(dispatch_get_main_queue(), {
                self.stepsLabel.text = String(steps)
            })
            
            
            /* Set the miles */
//FIXME: not checking user data and using that if available
            let miles = Calculator.calculate_distance(steps, height: Int(5.5*12))
            dispatch_async(dispatch_get_main_queue(), {
                self.distanceLabel.text = String(format: "%.1f", miles) + " MILES"
            })
            
            
            /* Set the calories */
//FIXME: not checking user data and using that if available
            let calories = Calculator.simple_calculate_calories(int: steps)
            dispatch_async(dispatch_get_main_queue(), {
                self.calorieLabel.text = String(format: "%.1f", calories) + " CAL"
            })
            
        } else {
            /* failure case */
        }
        
    
    }
}


// Should be moved to be a general utility function
//MARK: should be moved to be a general utility function
func stepsForDayStarting(dateStart: NSDate) -> Int {
    var totalStepsForToday : Int = 0
    var dateStop = dateStart.dateByAddingTimeInterval(60*60*24); //24hrs
    let predicate = NSPredicate(format:"%@ <= starttime AND %@ >= endtime AND %@ == user", dateStart, dateStop, UserData.getOrCreateUserData().getCurrentUID())
    
    let fetchRequest = NSFetchRequest(entityName: "StepEntry")
    fetchRequest.predicate = predicate
    if let fetchResults = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [StepEntry] {
        if(fetchResults.count > 0){
            println("Count %i",fetchResults.count)
            var resultsCount = fetchResults.count
            for(var i=0;i<(resultsCount);i++){
                //                    println("Adding steps up for %i %i",cellDateNumber, Int(fetchResults[i].count))
                totalStepsForToday = totalStepsForToday + Int(fetchResults[i].count)
            }
        }
    }
    
    return totalStepsForToday
}
