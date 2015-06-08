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

class DailyViewController : UIViewController, UIImagePickerControllerDelegate, UINavigationControllerDelegate, ImageUpdateDelegate {
    
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
        imagePicker.sourceType = UIImagePickerControllerSourceType.Camera
        imagePicker.delegate = self
        self.presentViewController(imagePicker, animated: true, completion: nil)
    }
    
    func imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [NSObject : AnyObject]) {
        
        var dateForImage = currentDate
        picker.dismissViewControllerAnimated(true, completion: {
            
            /*
            println("showing spinner")
            var spinner = showSpinner("Uploading Image", "Please wait...")
        
            self.presentViewController(spinner, animated: true, completion: nil)
            */
            
            if let date = dateForImage {
                if let image = info[UIImagePickerControllerOriginalImage] as? UIImage {
                    UserData.storeImage(image, rawData: nil, date: date, pushToFirebase: true, callbackDelegate: self)
                    
                }
            }
            /*
            spinner.dismissViewControllerAnimated(true, completion: nil)
            */
        })
        
    }
         
    
    func swipeLeft(recognizer : UISwipeGestureRecognizer) {
        if let date = currentDate {
            /* test for current date */
            if (!isToday(date)) {
                currentDate = currentDate?.dateByAddingTimeInterval(60*60*24);
            }
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
                if let stepsstring = floatCommaNumberFormatter(0).stringFromNumber(steps) {
                    self.stepsLabel.text = stepsstring
                } else {
                    self.stepsLabel.text = "0"
                }
            })
            
            
            /* Set the miles */
            //default height of person in feet, roughly 5.5'
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
            
            
            let miles = Calculator.calculate_distance(steps, height: Int(height*12.0))
            
            dispatch_async(dispatch_get_main_queue(), {
                if let milestring = floatCommaNumberFormatter(1).stringFromNumber(miles) {
                    self.distanceLabel.text = milestring
                } else {
                    self.distanceLabel.text = "0.0"
                }
            })
            
            
            /* Set the calories */

            let calories = caloriesForDayStarting(date)
            dispatch_async(dispatch_get_main_queue(), {
                if let caloriestring = floatCommaNumberFormatter(1).stringFromNumber(calories) {
                    self.calorieLabel.text =  caloriestring
                } else {
                    self.calorieLabel.text =  "0.0"
                }
            })
            
            UserData.getImageForDate(date, callbackDelegate: self)
            
        } else {
            /* failure case */
        }
        
        
    }
    
    func updatedImage(date: NSDate?, newImage: UIImage?) {
        var setImage = false
        if let unwrappedDate = date {
            if (unwrappedDate == currentDate) {
                if let image = newImage {
                    dispatch_async(dispatch_get_main_queue(), {
                        self.backgroundImage.image = newImage
                    })
                    setImage = true
                }
            }
            if ( (unwrappedDate == currentDate) && !setImage) {
                dispatch_async(dispatch_get_main_queue(),  {
                    self.backgroundImage.image = UIImage(named: "splash")
                })
            }
        }
    }
    
}



