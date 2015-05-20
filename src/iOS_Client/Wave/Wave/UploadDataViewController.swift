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
    @IBOutlet weak var scanButton: UIButton!
    @IBOutlet weak var syncButton: UIButton!
    @IBOutlet weak var waveDeviceTableView: UITableView!
    
    var syncStatusVC : SyncStatusViewController?
    
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
        })
    }
    
    
    //returns an array of WaveSteps with all of the data from a sync operation
    func syncComplete(deviceId: NSString?, data: [WaveStep]) {
        println("Completed Sync")
        var count = 0
        
        let appDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
        let managedContext = appDelegate.managedObjectContext

        
        
        ///HANDLE DATA HERE
        for step in data {
            count += step.steps
            
            var newItem = NSEntityDescription.insertNewObjectForEntityForName("StepEntry", inManagedObjectContext: appDelegate.managedObjectContext!) as! StepEntry
            newItem.count = Int16(step.steps)
            newItem.user = UserData.getOrCreateUserData().getCurrentUID()
            newItem.syncid = syncUid
            newItem.starttime = step.start
            newItem.endtime = step.end
            newItem.serialnumber = String(deviceId!)

            
            println("step: "+String(step.steps))
        }
        appDelegate.managedObjectContext!.save(nil)

//        class WaveStep : NSObject {
//            let start : NSDate
//            let end : NSDate
//            let steps : Int
//            init (start: NSDate, end: NSDate, steps: Int) {
//                self.start = start
//                self.end = end
//                self.steps = steps
//            }
//        }
       
        
        ///END HANDLE DATA
        
        dispatch_sync(dispatch_get_main_queue(), {
//            self.statusLabel.text = "Sync success, counted " + String(count) + " total steps"
            //self.dismissViewControllerAnimated(false, completion: nil)
            self.syncStatusVC?.performSegueWithIdentifier("SyncComplete", sender: self)
        })
        
    }
    
    //final failure call from a sync attempt.  Sync should not be considered
    //to be in a fatal failure until this message is returned
    func syncFailure(deviceId: NSString?) {
        println("Failed Sync")
        dispatch_sync(dispatch_get_main_queue(), {
//            self.statusLabel.text = "Sync attempt failed"
            //self.dismissViewControllerAnimated(false, completion: nil)
            self.syncStatusVC?.performSegueWithIdentifier("SyncComplete", sender: self)
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
        var uuid = NSUUID().UUIDString
        syncUid = uuid
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


