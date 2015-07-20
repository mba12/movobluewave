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
    var syncStartTime:NSDate = NSDate()
    @IBOutlet weak var scanButton: UIButton!
    @IBOutlet weak var syncButton: UIButton!
    @IBOutlet weak var waveDeviceTableView: UITableView!
    
    var syncStatusVC : SyncStatusViewController?
    let appDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
    var dataFilePath: String?
    
    var knownDevices : [String] = [String]()
    var unknownDevices : [String] = [String]()
    
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
        
        let longpress = UILongPressGestureRecognizer(target: self, action: "longPressGestureRecognized:")

        waveDeviceTableView.addGestureRecognizer(longpress)



        
        
    }
    
    override func viewDidAppear(animated: Bool) {
        //lets reset the waveSync to make sure we are in a sane state
        waveSync.waveController?.disconnectWaveDevices()
        waveSync.scan(true)
        setupNotificationSet()
        updateDevicesList()
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
    

    
    
    //returns an array of WaveSteps with all of the data from a sync operation
    func syncComplete(deviceId: NSString?, data: [WaveStep]) {
        println("Completed Sync")
        var count = 0



        
        let managedContext = appDelegate.managedObjectContext
        if let serial = waveSync?.connectedWaveDeviceSerialString(deviceId!) as? String{
            
            //add device to known devices list
            addToKnownDevices(serial)
            
            ///HANDLE DATA HERE
            if let syncUid = insertSyncDataInDB(serial, data, syncStartTime) {

                uploadSyncResultsToFirebase(syncUid, syncStartTime)
            }
            
            ///END HANDLE DATA
            
            ///success
            dispatch_sync(dispatch_get_main_queue(), {
                //            self.statusLabel.text = "Sync success, counted " + String(count) + " total steps"
                //self.dismissViewControllerAnimated(false, completion: nil)
                self.syncStatusVC?.performSegueWithIdentifier("SyncComplete", sender: self)
                let movonames = NSUserDefaults.standardUserDefaults()
                if let namePrompt = (movonames.stringForKey(UserData.getOrCreateUserData().getCurrentUID()! + serial + "renamePrompt")){
                    //This user has already been prompted to name this device.
                }else{
                    if let userId = UserData.getOrCreateUserData().getCurrentUID(){
                        self.promptUserForRename(serial)
                        movonames.setObject("true", forKey: userId + (serial as String) + "renamePrompt")
        
                    }

                }

               

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

    func promptUserForRename(serial: NSString?){
        if let serialString = serial{
            
        
        let movonames = NSUserDefaults.standardUserDefaults()
        
            var alert = UIAlertController(title: "Rename Wave?", message: "Would you like to rename your wave?", preferredStyle: UIAlertControllerStyle.Alert)
            
            let action = UIAlertAction(title: "Yes", style: UIAlertActionStyle.Default,
                handler: {[weak self]
                    (paramAction:UIAlertAction!) in
                    if let textFields = alert.textFields{
                        //                        if let deviceSerialIn = self?.deviceSerial! {
                        let theTextFields = textFields as! [UITextField]
                        let enteredText = theTextFields[0].text
                        //self!.displayLabel.text = enteredText
                        
                        let defaults = NSUserDefaults.standardUserDefaults()
                        if let userId = UserData.getOrCreateUserData().getCurrentUID(){
                            defaults.setObject(enteredText, forKey: userId + (serialString as String))
                            NSLog("Saving new device name %@ %@ %@", enteredText, UserData.getOrCreateUserData().getCurrentUID()!, serialString)
                            dispatch_async(dispatch_get_main_queue(), {
                                //use of a bang here, wil
                                self!.waveDeviceTableView.reloadData()
                            })
                            
                        }
                       
                    }

                })
            alert.addAction(action)
            
            alert.addAction(UIAlertAction(title: "Cancel", style: UIAlertActionStyle.Default, handler: nil))
            alert.addTextFieldWithConfigurationHandler({(textField: UITextField!) in
                textField.placeholder = "Enter text:"
            })
                            self.presentViewController(alert, animated: true, completion: nil)
            
            }
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
    
    func updateDevicesList() {
        //update our list of devices
        waveDeviceTableView.layer.removeAllAnimations()
        knownDevices = [String]()
        unknownDevices = [String]()
        if let uid = UserData.getOrCreateUserData().getCurrentUID() {
            for dev in waveSync.waveController!.connectedSerials {
                //for each device in the connected serials list
                //we need to see if it is in the known devices list
                
                if let data = dev.value as? NSData {
                    var count = data.length
                    var array = [UInt8](count: count, repeatedValue: 0)
                    data.getBytes(&array, length: count)
                    var serial = "".join(array.map{ String($0, radix: 16, uppercase: true)})
                    if (isKnownDevice(uid, serial)) {
                        knownDevices.append(serial)
                        
                    } else {
                        unknownDevices.append(serial)
                    }
                }
                
            }
        }
        
        
    }
    
    //returns updates indicating the listed device is ready - or not ready (i.e. disconnected)
    func deviceReady(id: NSString, serial: Array<UInt8>?, ready: Bool) {
        
        
        updateDevicesList()
        
        if (ready) {
            println("Device Ready " + (id as String))
            dispatch_sync(dispatch_get_main_queue(), {
//                self.statusLabel.text = "Device Ready: "+(id as String)
            })
        } else {
            println("Device NOT ready " + (id as String))
            
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
            return knownDevices.count
        } else {
            return unknownDevices.count
        }
    }
    


    
    func longPressGestureRecognized(gestureRecognizer: UIGestureRecognizer) {
        let longPress = gestureRecognizer as! UILongPressGestureRecognizer
        
        let state = longPress.state
        
        var locationInView = longPress.locationInView(waveDeviceTableView)
        
        var indexPath = waveDeviceTableView.indexPathForRowAtPoint(locationInView)

        //get device identifier
        if let section = indexPath?.section{
            if let row = indexPath?.row{
                if(section==0){
                    if(row < knownDevices.count){
                        //this is in the known devices list
                        let deviceSerial = knownDevices[row];
                        promptUserForRename(deviceSerial)
                        
                        
                        
                    }
                }
            }
        }
        
        
        
    }

    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCellWithIdentifier("WaveTableCell") as! WaveTableCell
        
        let section = indexPath.section
        let row = indexPath.row
        if (section == 0) {
            if (row < knownDevices.count) {
                let movonames = NSUserDefaults.standardUserDefaults()
                if let uid = UserData.getOrCreateUserData().getCurrentUID(){
                    
                    
                    if let name = movonames.stringForKey(uid + knownDevices[row]){
                        if(!(name == "")){
                            cell.NameLabel.text = name
                        }else{
                            cell.NameLabel.text = knownDevices[row]
                            
                        }
                        
                    }else{
                        cell.NameLabel.text = knownDevices[row]
                        
                    }
                }else{
                    cell.NameLabel.text = knownDevices[row]
                    
                }
                cell.contentView.layer.opacity = 0.1
                UIView.animateWithDuration(1.0,
                    delay:0,
                    options: .Repeat | .Autoreverse | .AllowUserInteraction,
                    animations: {
                        cell.contentView.layer.opacity = 1
                    }, completion: nil)
            }
        } else {
            if (row < unknownDevices.count) {
                cell.NameLabel.text = unknownDevices[row]
                if(!(knownDevices.count>0)){
                    cell.contentView.layer.opacity = 0.1
                    UIView.animateWithDuration(1.0,
                        delay:0,
                        options: .Repeat | .Autoreverse | .AllowUserInteraction,
                        animations: {
                            cell.contentView.layer.opacity = 1
                        }, completion: nil)
                }
            }
            
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


