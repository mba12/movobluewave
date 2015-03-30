//
//  ViewController.swift
//  bluetoothTest
//
//  Created by Rudy Yukich on 3/24/15.
//

import Cocoa
import CoreBluetooth

class ViewController: NSViewController,  CBCentralManagerDelegate, CBPeripheralDelegate {

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
        if (testPeripheral != nil) {
            println("Disconnecting Peripheral")
            centralManager.cancelPeripheralConnection(testPeripheral)
        }
        exit(0)
    }
    
    let centralManager:CBCentralManager!
    var connectingPeripheral:CBPeripheral!
    var testWriteCharacteristic:CBCharacteristic!
    var testPeripheral:CBPeripheral!
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        centralManager = CBCentralManager(delegate: self, queue: dispatch_get_main_queue())
    }
    
    func centralManagerDidUpdateState(central: CBCentralManager!){
        switch central.state{
        case .PoweredOn:
            println("poweredOn")
            let serviceUUIDs:[AnyObject] = [CBUUID(string: "180A")]//, CBUUID(string: "FFE0")]
            let lastPeripherals = centralManager.retrieveConnectedPeripheralsWithServices(serviceUUIDs)
            //let lastPeripherals = centralManager.retrievePeripheralsWithIdentifiers(nil);
            if lastPeripherals.count > 0{
                let device = lastPeripherals.last as CBPeripheral;
                connectingPeripheral = device;
                centralManager.connectPeripheral(connectingPeripheral, options: nil)
            }
            else {
                //[CBUUID(string: "FFE5"), CBUUID(string: "FFE0")]
                centralManager.scanForPeripheralsWithServices(nil, options: [ CBCentralManagerScanOptionAllowDuplicatesKey : true ])
            }
        default:
            println(central.state)
        }
    }
    func centralManager(central: CBCentralManager!, didDiscoverPeripheral peripheral: CBPeripheral!, advertisementData: [NSObject : AnyObject]!, RSSI: NSNumber!) {
        connectingPeripheral = peripheral
        connectingPeripheral.delegate = self
        if ((connectingPeripheral.name != nil)) {println(connectingPeripheral.name)}
        
        if ((connectingPeripheral.name != nil) && (connectingPeripheral.name == "808A")) {
            println("connecting")
            centralManager.connectPeripheral(connectingPeripheral, options: nil)
        }
    }
    func centralManager(central: CBCentralManager!, didConnectPeripheral peripheral: CBPeripheral!) {
        testPeripheral = peripheral
        connectedLabel.stringValue = "Connected"
        peripheral.discoverServices(nil)
    }
    func peripheral(peripheral: CBPeripheral!, didDiscoverServices error: NSError!) {
        if let actualError = error{
        }
        else {
            for service in peripheral.services as [CBService]!{
                peripheral.discoverCharacteristics(nil, forService: service)
            }
        }
    }
    func peripheral(peripheral: CBPeripheral!, didDiscoverCharacteristicsForService service: CBService!, error: NSError!) {
        if let actualError = error{
        }
        else {
            if service.UUID == CBUUID(string: "FFE0"){
                for characteristic in service.characteristics as [CBCharacteristic]{
                    switch characteristic.UUID.UUIDString{
                    case "FFE4":
                        //Set notification on notification characteristic
                        println("Found a general notify characteristic")
                        peripheral.setNotifyValue(true, forCharacteristic: characteristic)
                    default:
                        println(characteristic.UUID.UUIDString)
                    }
                }
            } else if service.UUID == CBUUID(string: "FFE5") {
                for characteristic in service.characteristics as [CBCharacteristic]{
                    switch characteristic.UUID.UUIDString{
                    case "FFE9":
                        // Save our write characteristic
                        println("Found a write characteristic -- writing a read current step")
                        testWriteCharacteristic = characteristic
                        writeCharacteristicLabel.stringValue = "Found Write Characteristic"
                        peripheral.discoverDescriptorsForCharacteristic(characteristic)
                    default:
                        println(characteristic.UUID.UUIDString)
                    }
                }
            
            } else if service.UUID == CBUUID(string: "180A") {
                println("found device information")
                for charateristic in service.characteristics as [CBCharacteristic] {
                    // This is the device information characteristic list
                    print("reading... ")
                    println(charateristic.UUID)
                    //peripheral.readValueForCharacteristic(charateristic)
                    
                }
                
            } else {
                println("Unknown service")
                println(service.UUID)
            }
        }
    }
    
    func peripheral(peripheral: CBPeripheral!, didDiscoverDescriptorsForCharacteristic characteristic: CBCharacteristic!, error: NSError!) {
        for descriptor in characteristic.descriptors as [CBDescriptor] {
            print("found descriptor")
            println(descriptor.UUID)
        }
        
        
    }
    

    func peripheral(peripheral: CBPeripheral!, didUpdateValueForCharacteristic characteristic: CBCharacteristic!, error: NSError!) {
        if let actualError = error{
        }else {
            switch characteristic.UUID.UUIDString{
            case "FFE4":
                println(characteristic.UUID)
                var data = characteristic.value()
                if (data != nil) {
                    var count = data.length
                    var array = [UInt8](count: count, repeatedValue: 0)
                    data.getBytes(&array, length: count)
                    println(array.map{ String($0, radix: 16, uppercase: false)})
                    outputData.stringValue = " ".join(array.map{ String($0, radix: 16, uppercase: false)})
//                    println(array)
                } else {
                    
                    println( characteristic.value() )
                }
                
            default:
                println(characteristic.UUID)
                
                println(NSString(data: characteristic.value(), encoding: NSUTF8StringEncoding));
//                println(characteristic.value())
            }
        }
    }
    func peripheral(peripheral: CBPeripheral!, didWriteValueForCharacteristic characteristic: CBCharacteristic!, error: NSError!) {
        
            println(characteristic.UUID)
            println(characteristic.value())
            //peripheral.readValueForCharacteristic(characteristic);
    }
    
    func peripheral(peripheral: CBPeripheral!, didUpdateValueForDescriptor descriptor: CBDescriptor!, error: NSError!) {
        println("found descriptor")
        
        
    }
    
    func peripheral(peripheral: CBPeripheral!, didUpdateNotificationStateForCharacteristic characteristic: CBCharacteristic!, error: NSError!) {
        println("Received Update Notification")
        println(characteristic.UUID)
        var data = characteristic.value()
        if (data != nil) {
            var count = data.length
            var array = [UInt8](count: count, repeatedValue: 0)
            data.getBytes(&array, length: count)
            println(array.map{ String($0, radix: 16, uppercase: false)})
            outputData.stringValue = " ".join(array.map{ String($0, radix: 16, uppercase: false)})
        } else {
        
            println( characteristic.value() )
        }
    }
    
    /* Should be refactored into single CB that looks at the sender */
    
    @IBOutlet weak var sendWriteButton: NSButton!
    @IBAction func handleClick(sender: AnyObject) {
        if (testWriteCharacteristic != nil && testPeripheral != nil) {
            var rawArray:[UInt8] = [0x89, 0x00, 0x00];
            let getTime = NSData(bytes: &rawArray, length: rawArray.count)
            
            testPeripheral.writeValue(getTime, forCharacteristic: testWriteCharacteristic, type: CBCharacteristicWriteType.WithoutResponse)
            
        }
        
    }
    @IBOutlet weak var sendGetSteps: NSButton!
    
    @IBAction func getStepsClick(sender: AnyObject) {
        if (testWriteCharacteristic != nil && testPeripheral != nil) {
            var rawArray:[UInt8] = [0xC6, 0x01, 0x09, 0x09];
            let getTime = NSData(bytes: &rawArray, length: rawArray.count)
            
            testPeripheral.writeValue(getTime, forCharacteristic: testWriteCharacteristic, type: CBCharacteristicWriteType.WithoutResponse)
            
        }
    }
    
    @IBOutlet weak var sendGetChart: NSButton!
    @IBAction func getChartClick(sender: AnyObject) {
        if (testWriteCharacteristic != nil && testPeripheral != nil) {
            var rawArray:[UInt8] = [0xC4, 0x03, 0x01, 0x06, 0x3, 0x04];
            let getTime = NSData(bytes: &rawArray, length: rawArray.count)
            
            testPeripheral.writeValue(getTime, forCharacteristic: testWriteCharacteristic, type: CBCharacteristicWriteType.WithoutResponse)

            
        }
    }
    
    @IBOutlet weak var connectedLabel: NSTextField!
    
    @IBOutlet weak var writeCharacteristicLabel: NSTextField!
    
    @IBOutlet weak var outputData: NSTextField!
}


