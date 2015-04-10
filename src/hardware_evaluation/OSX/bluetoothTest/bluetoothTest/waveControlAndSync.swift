//
//  waveControlAndSync.swift
//  bluetoothTest
//
//  Created by Rudy Yukich on 4/6/15
//

import Foundation
import CoreBluetooth


protocol waveControlAndSyncDelegate {
    func connectedWaveDevice(id: NSString)
    func disconnectedWaveDevice(id: NSString)
    func receivedMessage(message: NSObject)
    func bluetoothManagerStateChange(state: CBCentralManagerState)
    
}


class waveControlAndSync: NSObject, CBCentralManagerDelegate, CBPeripheralDelegate {
    
    let centralManager:CBCentralManager!
    let callbackDelegate:waveControlAndSyncDelegate!
    var connectingPeripheral:CBPeripheral!
    var writeCharacteristic:CBCharacteristic!
    var wavePeripherals:NSMutableDictionary!
    var connectedSerial:NSString!
    var scanning:Bool!
    
    init(delegate: waveControlAndSyncDelegate) {
        super.init()
        wavePeripherals = NSMutableDictionary()
        centralManager = CBCentralManager(delegate: self, queue: dispatch_get_main_queue())
        callbackDelegate = delegate
        scanning = false
    }
    
// This is the interface that the App should use to communicate with the bluetooth device
    
    //requests the waveControlAndSync Object to attempt to connect to a Wave Device
    //returns false if unable to scan
    func requestConnection() -> Bool {
        if (centralManager.state == CBCentralManagerState.PoweredOn) {
            scanning = true
            let serviceUUIDs:[AnyObject] = [CBUUID(string: "180A")]
            centralManager.scanForPeripheralsWithServices(serviceUUIDs, options: [ CBCentralManagerScanOptionAllowDuplicatesKey : true ])
            return true
        }
        return false
    }
    
    func cancelConnectionAttempt() {
        centralManager.stopScan()
        scanning = false
    }
    
    //request that the system disconnect a currently connected Wave Device (if any)
    func disconnectWaveDevice(id: NSString) {
        var wavePeripheral:CBPeripheral?
        wavePeripheral = wavePeripherals.valueForKey(id) as? CBPeripheral
        if (wavePeripheral != nil) {
            println("Disconnecting Peripheral")
            centralManager.cancelPeripheralConnection(wavePeripheral)
        }
        
    }
    
    
    
    
//Delegate Callbacks

    //if user turns power on/off for bluetooth, the UI should know
    func centralManagerDidUpdateState(central: CBCentralManager!) {
        callbackDelegate.bluetoothManagerStateChange(central.state)
        
    }
    
    //for any discovered peripheral, we want to check if its name matches the required device name.
    //our sample devices are "808A", but that will be changing.
    //in principle this should get called for each peripheral that we discover
    func centralManager(central: CBCentralManager!, didDiscoverPeripheral peripheral: CBPeripheral!, advertisementData: [NSObject : AnyObject]!, RSSI: NSNumber!) {
        connectingPeripheral = peripheral
        connectingPeripheral.delegate = self
        if ((connectingPeripheral.name != nil)) {println(connectingPeripheral.name)}
        
        if ((connectingPeripheral.name != nil) && (connectingPeripheral.name == "808A")) {
            //found a match, attempt to connect
            centralManager.connectPeripheral(connectingPeripheral, options: nil)
        }
        
    }
    
    //since we are only connecting to peripherals that match the correct name
    //anything that detects we need to immediately discover services
    func centralManager(centeral: CBCentralManager!, didConnectPeripheral peripheral: CBPeripheral!) {
        if (connectingPeripheral == peripheral) {
            wavePeripherals.insertValue(peripheral, inPropertyWithKey: peripheral.identifier.UUIDString)
            peripheral.discoverServices(nil)
        }
    
    }
    
    
    //this should let us handle disconnections properly
    //right now we just want to remove the device from our list of wavePeripherals if it is a known device
    //and if it is a known device, we let the UI know.
    func centralManager(central: CBCentralManager!, didDisconnectPeripheral peripheral: CBPeripheral!, error: NSError!) {
        var wavePeripheral : CBPeripheral?
        wavePeripheral = wavePeripherals.valueForKey(peripheral.identifier.UUIDString) as? CBPeripheral
        if (peripheral == wavePeripheral) {
            callbackDelegate.disconnectedWaveDevice(peripheral.identifier.UUIDString)
            wavePeripherals.removeObjectForKey(peripheral.identifier.UUIDString)
        }
    }
    
    //TODO: gracefully handle failed connections to peripherals
    func centralManager(central: CBCentralManager!, didFailToConnectPeripheral peripheral: CBPeripheral!, error: NSError!) {
        
    }
    
    func peripheral(peripheral: CBPeripheral!, didDiscoverServices error: NSError!) {
        if let actualError = error{
            //may require error handling
        }
        else {
            for service in peripheral.services as [CBService]!{
                peripheral.discoverCharacteristics(nil, forService: service)
            }
        }
        
    }
    
    //capture the write characteristic for the Wave Device
    //and attach a notify request to the read characteristic
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
                        println("Found a write characteristic")
                        writeCharacteristic = characteristic
                        peripheral.discoverDescriptorsForCharacteristic(characteristic)
                    default:
                        println(characteristic.UUID.UUIDString)
                    }
                }
                
            } else if service.UUID == CBUUID(string: "180A") {
                println("found device information")
                for charateristic in service.characteristics as [CBCharacteristic] {
                    println(charateristic.UUID)
                }
                
            } else {
                println("Unknown service")
                println(service.UUID)
            }
        }
        
    }
    
    //not used
    func peripheral(peripheral: CBPeripheral!, didDiscoverDescriptorsForCharacteristic characteristic: CBCharacteristic!, error: NSError!) {
        for descriptor in characteristic.descriptors as [CBDescriptor] {
            print("found descriptor")
            println(descriptor.UUID)
        }
    }
    
    //receive update notifications
    //retrieve data from the read characteristic
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
    
    //confirmation that command completed
    func peripheral(peripheral: CBPeripheral!, didWriteValueForCharacteristic characteristic: CBCharacteristic!, error: NSError!) {
        println(characteristic.UUID)
        println(characteristic.value())
        
    }
    
    //unlikely
    func peripheral(peripheral: CBPeripheral!, didUpdateNotificationStateForCharacteristic characteristic: CBCharacteristic!, error: NSError!) {
        println("Received Update Notification")
        println(characteristic.UUID)
        var data = characteristic.value()
        if (data != nil) {
            var count = data.length
            var array = [UInt8](count: count, repeatedValue: 0)
            data.getBytes(&array, length: count)
            println(array.map{ String($0, radix: 16, uppercase: false)})
        } else {
            println( characteristic.value() )
        }
    }
    
//End Delegate Callbacks
    
}