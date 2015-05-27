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
            //            if let iOSString:NSString = NSString(UTF8String: base64String){
            //                if(iOSString.length>1000000){
            //                    var result:[String] = []
            //                    var index = 0
            //                    while(index < iOSString.length){
            //                        var endPart = iOSString.endIndex
            //
            //                        var subPart = iOSString.substringWithRange(Range<String.Index>(start: advance(iOSString.startIndex,index), end: advance(iOSString.endIndex, -1))
            //)
            //                    }
            //
            //                }
            //            }
            
            
            
            if let array = base64String.cStringUsingEncoding(NSUTF8StringEncoding) {
                var result:[String] = []
                if(array.count>1000000){
                    //break photo into chunks
                    var totalChunks = 0
                    var i = 0
                    var subSection = ""
                    while (i<array.count){
                        var j = 0
                        if(i<array.count && ((i+1000000)<array.count)){
                            j = 0
                            while(j<1000000){
                                subSection = subSection + String(array[j+i])
                                j++
                            }
                            result.append(subSection)
                            totalChunks++
                            subSection = ""
                            
                            i = i + 1000000
                            
                        }else if(i<array.count && ((i+1000000)>array.count)){
                            j = 0
                            while((j+i)<(array.count-i)){
                                subSection = subSection + String(array[j+i])
                                j++
                            }
                            result.append(subSection)
                            subSection = ""
                            
                        }
                        
                    }
                    var parts = ["0":String(totalChunks)]
                    for(var cd = 0;cd<totalChunks;cd++){
//                     ,"1":base64String
                        parts.updateValue(result[cd], forKey: String(cd))
                        
                    }
                    
                    firebaseImage.setValue(parts)

                    
                    
                }else{
                    //size is ok for 1 chunk
                    var parts = ["0":"1","1":base64String]
                    
                    firebaseImage.setValue(parts)
                    
                    
                }
            }
            
            
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



