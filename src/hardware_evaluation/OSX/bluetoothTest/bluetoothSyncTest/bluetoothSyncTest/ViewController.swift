//
//  ViewController.swift
//  bluetoothSyncTest
//
//  Created by Rudy Yukich on 5/11/15.
//  Copyright (c) 2015 Sensorstar - Movo. All rights reserved.
//

import Cocoa

class ViewController: NSViewController, waveSyncManagerDelegate {

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
    }

    override var representedObject: AnyObject? {
        didSet {
        // Update the view, if already loaded.
        }
    }

    //         func attemptSync(deviceId: NSString? = nil)
    
    
    //func scan(shouldScan: Bool)
    
    var waveSync : waveSyncManager!
    
    required init?(coder aDecoder:NSCoder) {
        super.init(coder: aDecoder)
        waveSync = waveSyncManager(delegate: self)
    }
    
    
    
    //returns status updates for state changes in the sync process
    func syncStatusUpdate(status: WaveSyncStatus, deviceId: NSString?,  completeRatio: Float) {
        println("Sync status update" + String(status.rawValue))
        
        
    }
    
    //returns an array of WaveSteps with all of the data from a sync operation
    func syncComplete(deviceId: NSString?, data: [WaveStep]) {
        println("Completed Sync")
        
    }
    
    //final failure call from a sync attempt.  Sync should not be considered
    //to be in a fatal failure until this message is returned
    func syncFailure(deviceId: NSString?) {
        println("Failed Sync")
        
    }
    
    //returns updates indicating the listed device is ready - or not ready (i.e. disconnected)
    func deviceReady(id: NSString, serial: Array<UInt8>?, ready: Bool) {
        println("Device Ready" + (id as String))
    }
    
    @IBOutlet weak var scanStatusLabel: NSTextField!
    @IBOutlet weak var syncLabel: NSTextField!
    
    @IBAction func scanButtonPress(sender: AnyObject) {
        waveSync.scan(true)
    }
    
    @IBAction func syncButtonPress(sender: AnyObject) {
        //for now just try with the default device
        waveSync.attemptSync(deviceId: nil)
    }
    
}

