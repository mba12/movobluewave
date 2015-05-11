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
    
    func connectedWaveDeviceSerial(id: NSString, serial: [UInt8]) {
        //do nothing yet
        println("Got Serial!")
        outputData.stringValue = " ".join(serial.map{ String($0, radix: 16, uppercase: false)})
        
    }
    
    func disconnectedWaveDevice(id: NSString) {
        connectedLabel.stringValue = ""
        waveController.requestConnection()
    }
    
    func requestComplete(error: NSError!) {
        //do nothing yet
    }
    func receivedMessage(message: WaveMessageResponse, id: NSString) {
        /*
        var data:NSData! = message as! NSData
        var count = data.length
        var array = [UInt8](count: count, repeatedValue: 0)
        data.getBytes(&array, length: count)
        outputData.stringValue = " ".join(array.map{ String($0, radix: 16, uppercase: false)})
        */
        if (message.code == WaveCommandResponseCodes.GetTimeSuccess) {
            if let timestamp = message.data![0] as? WaveYMDHMSDOW {
                outputData.stringValue = String(waveYMDHMSDOWGMTToNSDate(timestamp).description)
                                    
            }
            
            
        } else if (message.code == WaveCommandResponseCodes.GetChartSuccess) {
            var dateFormatter = NSDateFormatter()
            dateFormatter.dateFormat = "yyyy-MM-dd HH:mm"
            var stepcount = 0
            if (message.data!.count > 0) {
            for x in message.data! as! [WaveStep] {
                stepcount += x.steps
            }
            var firstStep = (message.data! as! [WaveStep])[0].start
            var finalStep = (message.data! as! [WaveStep]).last?.end
            outputData.stringValue = "Counted "+String(stepcount)+" steps from: "+dateFormatter.stringFromDate(firstStep)+" to "+dateFormatter.stringFromDate(finalStep!)
                
            } else {
                outputData.stringValue = "Successful chart, but no step data for requested period"
            }
            
            
        }
        println("Received Message")

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
        waveController.getChart(id: deviceId, Year: 15, Month: 5, Day: 7)
    }
    
    @IBOutlet weak var sendGetSerial: NSButton!
    @IBAction func getSerialClick(sender: AnyObject) {
        waveController.getSerial(id: deviceId)
    }
    @IBOutlet weak var connectedLabel: NSTextField!
    
    @IBOutlet weak var writeCharacteristicLabel: NSTextField!
    
    @IBOutlet weak var outputData: NSTextField!
}


