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
        picker.dismissViewControllerAnimated(true, completion: nil)

        uploadImage(info)
    }
    
    func uploadImage(info: [NSObject: AnyObject]){
        if let date = currentDate{
            
            
            var tempImage:UIImage = info[UIImagePickerControllerOriginalImage] as! UIImage
            var tempData = UIImageJPEGRepresentation(tempImage, 1.0)
            //        UIImageJPEGRepresentatio
            let base64String = tempData.base64EncodedStringWithOptions(.allZeros)
//            println(base64String.lengthOfBytesUsingEncoding(NSUTF16StringEncoding))
            var cal = NSCalendar.currentCalendar()
            
            var todayDate = cal.component(.CalendarUnitDay , fromDate: date)
            var todayMonth = cal.component(.CalendarUnitMonth , fromDate: date)
            var todayYear = String(cal.component(.CalendarUnitYear , fromDate: date))
            var month = ""
            var day = ""
            if(todayMonth<10){
                month = "0" + (String(todayMonth))
            }else{
                month = String(todayMonth)
            }
            if(todayDate<10){
                day = "0" + (String(todayDate))
            }else{
                day = String(todayDate)
            }
            
            
            var fbUploadRef = UserData.getOrCreateUserData().getCurrentUserRef()
            fbUploadRef = fbUploadRef! + "/photos/"
            fbUploadRef = fbUploadRef! + todayYear + "/" + month + "/" + day
            var firebaseImage:Firebase = Firebase(url:fbUploadRef)
            var parts = ["0":"1","1":base64String]
            firebaseImage.setValue(parts)
            
            
            UserData.getOrCreateUserData().downloadPhotoFromFirebase(NSDate: date)
            
            
            
        }
        
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



