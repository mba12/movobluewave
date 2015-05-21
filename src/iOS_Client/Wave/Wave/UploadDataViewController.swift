//
//  UploadDataViewController.swift
//  Wave
//
//  Created by Phil Gandy on 5/11/15.
//

import Foundation
import UIKit
import CoreData

class UploadDataViewController: UIViewController, waveSyncManagerDelegate, UITableViewDelegate, UITableViewDataSource {
    var waveSync : waveSyncManager!
    var syncUid:String = "Error"
    var syncStartTime:NSDate = NSDate()
    @IBOutlet weak var scanButton: UIButton!
    @IBOutlet weak var syncButton: UIButton!
    @IBOutlet weak var waveDeviceTableView: UITableView!
    
    var syncStatusVC : SyncStatusViewController?
    let appDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
    
    @IBAction func cancel(sender: AnyObject?){
        dismissViewControllerAnimated(true, completion: nil)
        waveSync.scan(false)
        waveSync.waveController?.disconnectWaveDevices()
        
    }
    
    func complete(sender: AnyObject?) {
        waveSync.scan(false)
        waveSync.waveController?.disconnectWaveDevices()
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        //default value, not initialized.
//        syncUid = "Error"
        
        // Do any additional setup after loading the view, typically from a nib.
        waveDeviceTableView.tableFooterView = UIView(frame: CGRect.zeroRect)
        
    }
    
    override func viewDidAppear(animated: Bool) {
        //lets reset the waveSync to make sure we are in a sane state
        waveSync.waveController?.disconnectWaveDevices()
        waveSync.scan(true)
    }
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }

    required init(coder aDecoder:NSCoder) {
        super.init(coder: aDecoder)
        waveSync = waveSyncManager(delegate: self)
    }
    
    //returns status updates for state changes in the sync process
    func syncStatusUpdate(status: WaveSyncStatus, deviceId: NSString?,  completeRatio: Float) {
        println("Sync status update " + String(status.rawValue))
        dispatch_sync(dispatch_get_main_queue(), {
//            self.statusLabel.text = "Sync status update " + String(status.rawValue) + "," + String(Int(completeRatio*100)) + "%"
            
            if (status == WaveSyncStatus.Idle || status == WaveSyncStatus.Start) {
                self.syncStatusVC?.syncStatusLabel.text = "Connecting..."
                self.syncStatusVC?.syncStatusProgress.setProgress(0.0, animated: true)
            } else if (status == WaveSyncStatus.VerifingDate || status == WaveSyncStatus.SettingDate) {
                self.syncStatusVC?.syncStatusLabel.text = "Verifying Device..."
                self.syncStatusVC?.syncStatusProgress.setProgress(0.1, animated: true)
            } else if (status == WaveSyncStatus.DownloadingData) {
                self.syncStatusVC?.syncStatusLabel.text = "Downloading Data, "+String(Int(completeRatio*100))+"%"
                self.syncStatusVC?.syncStatusProgress.setProgress( (completeRatio+0.1)/1.1, animated: true)
            } else if (status == WaveSyncStatus.Fail) {
                self.syncStatusVC?.syncStatusLabel.text = "Error..."
            } else if (status == WaveSyncStatus.Finished) {
                self.syncStatusVC?.syncStatusLabel.text = "Complete!"
                self.syncStatusVC?.syncStatusProgress.setProgress(1.0, animated: true)
            }
            
        })
    }
    
    func duplicateCheck(waveStep: WaveStep, serial:String )->Bool{
        
        
        var isDuplicate = false
        
        let predicate = NSPredicate(format:"%@ == starttime AND %@ == endtime AND %@ == serialnumber", waveStep.start, waveStep.end, serial)
        let fetchRequestDupeCheck = NSFetchRequest(entityName: "StepEntry")
        fetchRequestDupeCheck.predicate = predicate
        
        let appDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
        let managedContext = appDelegate.managedObjectContext
        
        if let fetchResults = appDelegate.managedObjectContext!.executeFetchRequest(fetchRequestDupeCheck, error: nil) as? [StepEntry] {
            if(fetchResults.count > 0){
                NSLog("Duplicate found")
                
                return true;
                
            }
        }
        return false;
    }
    
    
    //returns an array of WaveSteps with all of the data from a sync operation
    func syncComplete(deviceId: NSString?, data: [WaveStep]) {
        println("Completed Sync")
        var count = 0
        var uuid = NSUUID().UUIDString
        syncUid = uuid

        
        let managedContext = appDelegate.managedObjectContext
        if let serial = waveSync?.connectedWaveDeviceSerialString(deviceId!) as? String{
            
            
            
            
            ///HANDLE DATA HERE
            for step in data {
                count += step.steps
                if(!duplicateCheck(step,  serial: serial)){
                    var newItem = NSEntityDescription.insertNewObjectForEntityForName("StepEntry", inManagedObjectContext: appDelegate.managedObjectContext!) as! StepEntry
                    newItem.count = Int16(step.steps)
                    newItem.user = UserData.getOrCreateUserData().getCurrentUID()
                    newItem.syncid = syncUid
                    newItem.starttime = step.start
                    newItem.endtime = step.end
                    newItem.serialnumber = String(serial)
                }
                
                println("step: "+String(step.steps))
            }
            
            
            var syncItem = NSEntityDescription.insertNewObjectForEntityForName("SyncEntry", inManagedObjectContext: appDelegate.managedObjectContext!) as! SyncEntry
            syncItem.guid = syncUid
            syncItem.starttime = syncStartTime
            syncItem.endtime = NSDate()
            syncItem.user = UserData.getOrCreateUserData().getCurrentUID()
            syncItem.status = false
            
            
            
            
            appDelegate.managedObjectContext!.save(nil)
            uploadSyncResultsToFirebase()
            
            
            ///END HANDLE DATA
            
            ///success
            dispatch_sync(dispatch_get_main_queue(), {
                //            self.statusLabel.text = "Sync success, counted " + String(count) + " total steps"
                //self.dismissViewControllerAnimated(false, completion: nil)
                self.syncStatusVC?.performSegueWithIdentifier("SyncComplete", sender: self)
            })
        } else {
            //failure
            dispatch_sync(dispatch_get_main_queue(), {
                //            self.statusLabel.text = "Sync success, counted " + String(count) + " total steps"
                //self.dismissViewControllerAnimated(false, completion: nil)
                self.syncStatusVC?.performSegueWithIdentifier("SyncFailure", sender: self)
            })
        }
    }
    
    func dateFormatFBRootNode( dateIn:NSDate)->String{
        var dateFormatter = NSDateFormatter()
        dateFormatter.dateFormat = "yyyy/MM/dd"
        dateFormatter.timeZone = NSTimeZone(forSecondsFromGMT: 0)
        dateFormatter.calendar = NSCalendar(calendarIdentifier: NSCalendarIdentifierISO8601)!
        dateFormatter.locale = NSLocale(localeIdentifier: "en_US_POSIX")
        var dateStringOut = dateFormatter.stringFromDate(dateIn)
        return dateStringOut
    }
    
    func dateFormatFBTimeNode( dateIn:NSDate)->String{
        var dateFormatter = NSDateFormatter()
        dateFormatter.dateFormat = "'T'HH:mm:ss'Z"
        dateFormatter.timeZone = NSTimeZone(forSecondsFromGMT: 0)
        dateFormatter.calendar = NSCalendar(calendarIdentifier: NSCalendarIdentifierISO8601)!
        dateFormatter.locale = NSLocale(localeIdentifier: "en_US_POSIX")
        var dateStringOut = dateFormatter.stringFromDate(dateIn)
        return dateStringOut
    }
    
    func dateToStringFormat( dateIn:NSDate)->String{
        var dateFormatter = NSDateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z"
        dateFormatter.timeZone = NSTimeZone(forSecondsFromGMT: 0)
        dateFormatter.calendar = NSCalendar(calendarIdentifier: NSCalendarIdentifierISO8601)!
        dateFormatter.locale = NSLocale(localeIdentifier: "en_US_POSIX")
        var dateStringOut = dateFormatter.stringFromDate(dateIn)
        return dateStringOut
    }
    
    func uploadSyncResultsToFirebase(){
        
        var firebaseURL = UserData.getOrCreateUserData().getFirebase()
        firebaseURL = firebaseURL + "users/"
        firebaseURL = firebaseURL + UserData.getOrCreateUserData().getCurrentUID()
        var firebaseStepsURL = firebaseURL + "/steps/"
        var refSteps = Firebase(url:firebaseStepsURL)
        var firebaseSyncURL = firebaseURL + "/sync/"
        firebaseSyncURL = firebaseSyncURL + syncUid
        firebaseSyncURL = firebaseSyncURL + "/"
        var refSync = Firebase(url:firebaseSyncURL)
        
        
        var syncStart = ["starttime":dateToStringFormat(syncStartTime)]
        var syncStop = ["endtime":dateToStringFormat(NSDate())]
        
        refSync.updateChildValues(syncStart)
        refSync.updateChildValues(syncStop)
            
        
        
        let predicate = NSPredicate(format:"%@ == syncid", syncUid)
        let fetchRequestSteps = NSFetchRequest(entityName: "StepEntry")
        fetchRequestSteps.predicate = predicate
        if let fetchResults = appDelegate.managedObjectContext!.executeFetchRequest(fetchRequestSteps, error: nil) as? [StepEntry] {
            
            
            if(fetchResults.count > 0){
                println("Count %i",fetchResults.count)
                var resultsCount = fetchResults.count
                for(var i=0;i<(resultsCount);i++){
                    var dateStringFB = dateFormatFBRootNode(fetchResults[i].starttime)
                    var dateStringTimeOnly = dateFormatFBTimeNode(fetchResults[i].starttime)
                    
                    
                    var appendString = dateStringFB
                    appendString = appendString + "/"
                    appendString = appendString + fetchResults[i].syncid
                    appendString = appendString + "/"
                    appendString = appendString + dateStringTimeOnly
                    appendString = appendString + "/"

                    var syncAppend = "steps/"
                    syncAppend = syncAppend + appendString
//                    syncAppend = syncAppend + dateStringFB
//                    syncAppend = syncAppend + "/"
//                    syncAppend =
//                    syncAppend = syncAppend + dateStringTimeOnly
                    
                    var daySyncRef = refSync.childByAppendingPath(syncAppend)
                    var dayRef = refSteps.childByAppendingPath(appendString)
                    
                    var count = ["count": String(fetchResults[i].count)]
                    var deviceid = ["deviceid": String(fetchResults[i].serialnumber)]
                    var starttime = ["endtime": dateFormatFBTimeNode(fetchResults[i].endtime)]
                    var endtime = ["starttime": dateFormatFBTimeNode(fetchResults[i].starttime)]
                    
                    dayRef.updateChildValues(count)
                    dayRef.updateChildValues(deviceid)
                    dayRef.updateChildValues(starttime)
                    dayRef.updateChildValues(endtime)
                    
                    daySyncRef.updateChildValues(count)
                    daySyncRef.updateChildValues(deviceid)
                    daySyncRef.updateChildValues(starttime)
                    daySyncRef.updateChildValues(endtime)

                    
                    
                    
                    
                    NSLog("%@",fetchResults[i].starttime)
                    
                    
                    
//                    totalStepsForToday = totalStepsForToday + Int(fetchResults[i].count)
                }
                
            }else{
                //no new steps, nothing to upload
            }
        } else {
            //error grabbing steps from coredata
        }
        
        
        //this will pull steps for a day in april and display them.
        //        var urlString = "https://ss-movo-wave-v2.firebaseio.com/users/simplelogin:7/steps/2015/4/"
        //        urlString = urlString + String(cellDateNumber)
        //        var todayFirebaseRef = Firebase(url:urlString)
        //        // Attach a closure to read the data at our posts reference
        //        todayFirebaseRef.observeEventType(.Value, withBlock: { snapshot in
        //
        //            let children = snapshot.hasChildren()
        //            if(children){
        //            var itr = snapshot.children
        //            while let rest = itr.nextObject() as? FDataSnapshot {
        //                //                    println(rest.value)
        //                var itr2 = rest.children
        //                while let rest2 = itr2.nextObject() as? FDataSnapshot{
        //                    //                    println(rest2.value)
        //
        //
        //                    var stepsChild:FDataSnapshot = rest2.childSnapshotForPath("count")
        //                    println(stepsChild.value)
        //
        //                    var todaysSteps = (rest2.childSnapshotForPath("count").valueInExportFormat() as? String)!
        //                    cell.textLabel2?.text = todaysSteps
        //
        //                }
        //            }
        //            }else{
        //                cell.textLabel2?.text = "0"
        //            }
        
        
        //                var valueStr:String  = String(stringInterpolationSegment: snapshot.value)
        //                cell.textLabel2?.text = valueStr
        //                println(valueStr)
        //            }, withCancelBlock: { error in
        //                println(error.description)
        //        })


        
    }
    
    //final failure call from a sync attempt.  Sync should not be considered
    //to be in a fatal failure until this message is returned
    func syncFailure(deviceId: NSString?) {
        println("Failed Sync")
        //failure
        dispatch_sync(dispatch_get_main_queue(), {
            //            self.statusLabel.text = "Sync success, counted " + String(count) + " total steps"
            //self.dismissViewControllerAnimated(false, completion: nil)
            self.syncStatusVC?.performSegueWithIdentifier("SyncFailure", sender: self)
        })
    }
    
    //returns updates indicating the listed device is ready - or not ready (i.e. disconnected)
    func deviceReady(id: NSString, serial: Array<UInt8>?, ready: Bool) {
        if (ready) {
            println("Device Ready " + (id as String))
            dispatch_sync(dispatch_get_main_queue(), {
//                self.statusLabel.text = "Device Ready: "+(id as String)
            })
        }
        dispatch_async(dispatch_get_main_queue(), {
            self.waveDeviceTableView.reloadData()
        })

    }

    
    @IBAction func scanButtonPress(sender: AnyObject) {
        waveSync.scan(true)
    }
    @IBAction func syncButtonPress(sender: AnyObject) {
                syncStartTime = NSDate()
        waveSync.attemptSync(deviceId: nil)
    }

    //UITableViewDelegateMethods
    func tableView(tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        if (section == 0) {
            return "Recognized Devices"
        } else {
            return "Unknown Devices"
        }
        
    }
    
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 2
    }
    
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if (section==0) {
            return waveSync.waveController!.connectedSerials.count
        } else {
            return 0
        }
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCellWithIdentifier("WaveTableCell") as! WaveTableCell
        let row = indexPath.row
        if (row < waveSync.waveController!.connectedSerials.count) {
            var Name = "Unknown"
            if let data  = waveSync.waveController!.connectedSerials.allValues[row] as? NSData {
                var count = data.length
                var array = [UInt8](count: count, repeatedValue: 0)
                data.getBytes(&array, length: count)
                Name = "".join(array.map{ String($0, radix: 16, uppercase: true)})
            }
            cell.NameLabel.text = Name
        }
        cell.backgroundColor = UIColor.clearColor()
        return cell
    }
    
    
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        tableView.deselectRowAtIndexPath(indexPath, animated: true)
        
        let row = indexPath.row
        if (row < waveSync.waveController!.connectedSerials.count) {
            if let id = waveSync.waveController!.connectedSerials.allKeys[row] as? String {
                waveSync.attemptSync(deviceId: id)
                performSegueWithIdentifier("SyncStatus", sender: self)
            }
        }
    }
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        if (segue.identifier == "SyncStatus") {
            syncStatusVC = segue.destinationViewController as? SyncStatusViewController
            syncStatusVC?.uploadDataVC = self
        }
    }
}


class WaveTableCell : UITableViewCell {
    
    @IBOutlet weak var NameLabel: UILabel!
    
    
}


