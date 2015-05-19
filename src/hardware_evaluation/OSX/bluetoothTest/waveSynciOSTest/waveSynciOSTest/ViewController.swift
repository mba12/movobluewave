//
//  ViewController.swift
//  waveSynciOSTest
//
//  Created by Rudy Yukich on 5/18/15.
//  Copyright (c) 2015 Sensorstar - Movo. All rights reserved.
//

import UIKit

class waveSynciOSTestVC: UIViewController, waveSyncManagerDelegate {

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    var waveSync : waveSyncManager!
    
    required init(coder aDecoder:NSCoder) {
        super.init(coder: aDecoder)
        waveSync = waveSyncManager(delegate: self)
    }
    
    @IBOutlet weak var syncStatus: UITextField!
    
    //returns status updates for state changes in the sync process
    func syncStatusUpdate(status: WaveSyncStatus, deviceId: NSString?,  completeRatio: Float) {
        println("Sync status update " + String(status.rawValue))
        dispatch_sync(dispatch_get_main_queue(), {
            self.syncStatus.text = "Sync status update " + String(status.rawValue) + "," + String(Int(completeRatio*100)) + "%"
        })
    }
    

    //returns an array of WaveSteps with all of the data from a sync operation
    func syncComplete(deviceId: NSString?, data: [WaveStep]) {
        println("Completed Sync")
        var count = 0
        for step in data {
           count += step.steps
        }
        dispatch_sync(dispatch_get_main_queue(), {
           self.syncStatus.text = "Sync success, counted " + String(count) + " total steps"
        })
        
    }
    
    //final failure call from a sync attempt.  Sync should not be considered
    //to be in a fatal failure until this message is returned
    func syncFailure(deviceId: NSString?) {
        println("Failed Sync")
        dispatch_sync(dispatch_get_main_queue(), {
            self.syncStatus.text = "Sync attempt failed"
        })
    }
    
    @IBOutlet weak var deviceStatus: UITextField!
    //returns updates indicating the listed device is ready - or not ready (i.e. disconnected)
    func deviceReady(id: NSString, serial: Array<UInt8>?, ready: Bool) {
        if (ready) {
            println("Device Ready " + (id as String))
            dispatch_sync(dispatch_get_main_queue(), {
                self.deviceStatus.text = "Device Ready: "+(id as String)
            })
        }
    }
    
    @IBOutlet weak var scanButton: UIButton!
    @IBOutlet weak var syncButton: UIButton!

    @IBAction func scanButtonPress(sender: AnyObject) {
        waveSync.scan(true)
    }
    @IBAction func syncButtonPress(sender: AnyObject) {
        waveSync.attemptSync(deviceId: nil)
    }
}

