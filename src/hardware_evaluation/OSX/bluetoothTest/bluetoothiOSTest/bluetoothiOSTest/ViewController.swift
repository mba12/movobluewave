//
//  ViewController.swift
//  bluetoothiOSTest
//
//  Created by Rudy Yukich on 4/20/15.
//  Copyright (c) 2015 Sensorstar - Movo. All rights reserved.
//

import UIKit
import CoreBluetooth

class ViewController: UIViewController, waveControlAndSyncDelegate {
    
    required init(coder aDecoder:NSCoder) {
        super.init(coder: aDecoder)
        waveController = waveControlAndSync(delegate: self)
        
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        
        
        waveController.requestConnection()
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    let waveController:waveControlAndSync!
    var deviceId:NSString!
    
    
    
    func connectedWaveDevice(id: NSString) {
        peripheralText.text = id
        waveController.cancelScan()
        deviceId = id
    }
    
    func connectedWaveDeviceSerial(id: NSString, serial: NSString) {
        //do nothing yet
        
        
    }
    
    func disconnectedWaveDevice(id: NSString) {
        peripheralText.text = ""
    }
    
    func requestComplete(error: NSError!) {
        //do nothing yet
    }
    func receivedMessage(message: NSObject, id: NSString) {
        
        var data:NSData! = message as NSData
        var count = data.length
        var array = [UInt8](count: count, repeatedValue: 0)
        data.getBytes(&array, length: count)
        dataText.text = " ".join(array.map{ String($0, radix: 16, uppercase: false)})
        
    }
    
    func bluetoothManagerStateChange(state: CBCentralManagerState) {
        println(state)
        if (state == CBCentralManagerState.PoweredOn) {
            waveController.requestConnection()
        }
    }
    

    @IBOutlet weak var getTimeButton: UIButton!
    @IBOutlet weak var getStepsButton: UIButton!
    @IBOutlet weak var getChartButton: UIButton!
    @IBOutlet weak var getSerialButton: UIButton!
    @IBOutlet weak var dataText: UITextField!
    @IBOutlet weak var characteristicText: UITextField!
    @IBOutlet weak var peripheralText: UITextField!
    @IBAction func getTimeClick(sender: AnyObject) {
        waveController.getTime(id: deviceId)
    }
    @IBAction func getStepsClick(sender: AnyObject) {
        waveController.getSteps(id: deviceId)
    }
    @IBAction func getChartClick(sender: AnyObject) {
        waveController.getChart(id: deviceId)
    }
    @IBAction func getSerialClick(sender: AnyObject) {
        waveController.getSerial(id: deviceId)
    }

}

