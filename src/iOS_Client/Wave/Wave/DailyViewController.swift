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
        
        
        
        if let date = currentDate {
            if let image = info[UIImagePickerControllerOriginalImage] as? UIImage {
                storeImage(image, date: date)
 
            }
        }
    }
    
    func getOrCreateDirectoryForImages() -> String {
        let documentsPath = NSSearchPathForDirectoriesInDomains(.DocumentDirectory, .UserDomainMask, true)[0] as! String
        
        let photoPath = documentsPath+"/Photos/"
        if (NSFileManager.defaultManager().createDirectoryAtPath(photoPath, withIntermediateDirectories: true, attributes: nil, error: nil)) {
            println("Photo dir created")
        }
        
        return photoPath
    }
    
    
    func removeOldImage(oldPhoto: PhotoStorage) {
        NSFileManager.defaultManager().removeItemAtPath(oldPhoto.photopath, error: nil)
    }
    
    func uploadImage(image: UIImage, date: NSDate){
        
        
        
        var tempImage:UIImage = image
        var tempData = UIImageJPEGRepresentation(tempImage, 1.0)
        //        UIImageJPEGRepresentatio
        let base64String = tempData.base64EncodedStringWithOptions(.allZeros)
        println(base64String.lengthOfBytesUsingEncoding(NSUTF16StringEncoding))
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
        
    }
    
    
    func storeImage(image: UIImage, date: NSDate) {
        
        //get system path to our image store
        var path = getOrCreateDirectoryForImages()
        
        //this is the store function so we will be saving the image to a GUID
        var uuid = NSUUID().UUIDString
        var data : NSData = UIImageJPEGRepresentation(image, 1.0)
        
        let imagename = uuid + ".jpg"
        let filepath = path + imagename
        
        var insertUpdateItem : PhotoStorage?
        
        //We want to store the image to the date 
        //to be timezone independent
        //this means that we use the GMT NSDate for storage that corresponds to
        //the beginning of the day
        var cal = NSCalendar.currentCalendar()
        var thisDate = cal.component(.CalendarUnitDay , fromDate: date)
        var thisMonth = cal.component(.CalendarUnitMonth , fromDate: date)
        var thisYear = cal.component(.CalendarUnitYear , fromDate: date)
        var storageDate = YMDGMTToNSDate(thisYear, thisMonth, thisDate)
        if let storeDate = storageDate {
            
            
            if let uid = UserData.getOrCreateUserData().getCurrentUID() {
                let predicate = NSPredicate(format:"%@ == user AND %@ == date", uid, storeDate)
                
                let fetchRequest = NSFetchRequest(entityName: "PhotoStorage")
                
                if let fetchResults = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [PhotoStorage] {
                    if(fetchResults.count > 0){
                        //then we must delete the old image and replace
                        if (fetchResults.count == 1) {
                            insertUpdateItem = fetchResults[0]
                            removeOldImage(fetchResults[0])
                            
                            
                        } else {
                            //error case
                            println("Warning: Excess Images")
                            insertUpdateItem = fetchResults[0]
                            removeOldImage(fetchResults[0])
                        }
                        
                        
                    } else {
                        
                        insertUpdateItem = NSEntityDescription.insertNewObjectForEntityForName("PhotoStorage", inManagedObjectContext: (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!) as? PhotoStorage
                    }
                    
                    
                    
                    if let newItem = insertUpdateItem {
                        newItem.photopath = filepath
                        newItem.date = storeDate
                        newItem.user = uid
                        
                        //lets go ahead and store the file now
                        data.writeToFile(filepath, atomically: true)
                        UserData.saveContext()
                        
                        //pass through the unadulterated date to uploadImage
                        //which will do the same conversion
                        uploadImage(image, date: date)
                    } else {
                        println("failed to update image")
                    }
                }
            }
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
                self.distanceLabel.text = String(format: "%.1f", miles) + " MILES"
            })
            
            
            /* Set the calories */

            let calories = caloriesForDayStarting(date)
            dispatch_async(dispatch_get_main_queue(), {
                self.calorieLabel.text = String(format: "%.1f", calories) + " CAL"
            })
            
        } else {
            /* failure case */
        }
        
        
    }
}



