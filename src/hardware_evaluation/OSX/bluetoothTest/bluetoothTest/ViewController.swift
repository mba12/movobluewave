//
//  ViewController.swift
//  bluetoothTest
//
//  Created by Rudy Yukich on 3/24/15.
//

import Cocoa
import CoreBluetooth

class ViewController: NSViewController,  waveControlAndSyncDelegate {

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
    }

    override var representedObject: AnyObject? {
        didSet {
        // Update the view, if already loaded.
        }

    }


    override func viewDidDisappear() {
        //close peripheral and exit
        waveController.disconnectWaveDevices()
        exit(0)
    }
    
    
    var waveController:waveControlAndSync!
    var deviceId:NSString!
    
    required init?(coder aDecoder:NSCoder) {
        super.init(coder: aDecoder)
        waveController = waveControlAndSync(delegate: self)
        waveController.requestConnection()
    }
    
    func connectedWaveDevice(id: NSString) {
        connectedLabel.stringValue = id as String
        waveController.cancelScan()
        deviceId = id
    }
    
    func connectedWaveDeviceSerial(id: NSString, serial: NSString) {
        //do nothing yet

        
    }
    
    func disconnectedWaveDevice(id: NSString) {
        connectedLabel.stringValue = ""
    }
    
    func requestComplete(error: NSError!) {
        //do nothing yet
    }
    func receivedMessage(message: NSObject, id: NSString) {
        
        var data:NSData! = message as! NSData
        var count = data.length
        var array = [UInt8](count: count, repeatedValue: 0)
        data.getBytes(&array, length: count)
        outputData.stringValue = " ".join(array.map{ String($0, radix: 16, uppercase: false)})

    }
    
    func bluetoothManagerStateChange(state: CBCentralManagerState) {
        println(state)
        if (state == CBCentralManagerState.PoweredOn) {
            waveController.requestConnection()
        }
    }
  
    @IBOutlet weak var sendWriteButton: NSButton!
    @IBAction func handleClick(sender: AnyObject) {
        waveController.getTime(id: deviceId)
        
    }
    @IBOutlet weak var sendGetSteps: NSButton!
    
    @IBAction func getStepsClick(sender: AnyObject) {
        waveController.getSteps(id: deviceId)
    }
    
    @IBOutlet weak var sendGetChart: NSButton!
    @IBAction func getChartClick(sender: AnyObject) {
        waveController.getChart(id: deviceId)
    }
    
    @IBOutlet weak var sendGetSerial: NSButton!
    @IBAction func getSerialClick(sender: AnyObject) {
        waveController.getSerial(id: deviceId)
    }
    @IBOutlet weak var connectedLabel: NSTextField!
    
    @IBOutlet weak var writeCharacteristicLabel: NSTextField!
    
    @IBOutlet weak var outputData: NSTextField!
}


