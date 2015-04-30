//
//  waveControlAndSync.swift
//  bluetoothTest
//
//  Created by Rudy Yukich on 4/6/15
//

import Foundation
import CoreBluetooth


/* Theory of operation
 * waveControlAndSync presents a class and delegate protocol for handling connection with a Movo Wave Device
 *   this class and protocol uses UUID identifiers of specific devices to identify connected devices with a UI program
 *   a series of callbacks allow communication with specific devices using the UUID
 *   a timeout is used to handle device command failure due to disconnect or missed packets
 *
 * A bluetooth device must be connected as a peripheral to send/receive messages.
 * Wave devices are identified by two specific features:
 *   - the presence of a give identifying name
 *   - Advertisement for the service 0x180A
 *   - Specific serial numbers
 *   - During any given instance, device identifier can also be used, but is not known to persist through power cycles
 *
 * When controlling the waveControlAndSync object, the caller is responsible for managing BlueTooth state.
 *  A the CoreBluetooth instance can be put into/out of scanning mode through requestConnection() and cancelScan().
 *
 *  Information about connection status will be communicated via callbacks:
 *      connectedWaveDevice() - on successful connection to a Wave device
 *      connectedWaveDeviceSerial() - on successful enumeration of Wave device serial number
 *      disconnectedWaveDevice() - on disconnection.  Note, this may be exogenic as well as endogenic.
 *      receivedMessage() - MAY CHANGE - complete message in response to request
 *      bluetoothManagerStateChange - convenience function for bluetooth power state changes
 *      requestComplete() - indication of completed request (NOT RESPONSE SUCCESSFULLY RECEIVED, see detailed notes)
 *
 *
 *    Once connected to a device (or devices), commands are sent through the public interface, getSerial, getChart, getSteps, getTime, etc.
 *      Typically, this will be called using a specific identifier, HOWEVER, that is not strictly necessary, when called with a nil parameter, the first
 *      connected device will be interrogated.
 */


/* Operational notes
 *  iOS doesn't clobber commands in transmission, however, the device will interrupt itself in return transmission
 *  in practice, what this means is that a new command during a multi-part message in progress will interrupt the multi-part message
 *   -> what this means is that we should maintain, per-device, a queue of message requests and not issue new messages until either the 
 *   -> message in progress has completed or has timed out
 *   -> After which, we can move onto the next message in the queue.
 *
 */

protocol waveControlAndSyncDelegate {
    //callback receiving the UUID of a successfully connected device
    func connectedWaveDevice(id: NSString)
    
    //callback receiving the serial number of a successfully connected device
    func connectedWaveDeviceSerial(id: NSString, serial: NSString)
    
    //callback confirming disconnection of connected devices
    func disconnectedWaveDevice(id: NSString)
    
    //callback for receiving a message from a device -- this should change to an interface that calls back with the unpacked message
    func receivedMessage(message: NSObject, id: NSString)
    
    //notification that the bluetooth manager changed state
    func bluetoothManagerStateChange(state: CBCentralManagerState)
    
    //notification of command complete for any individual command
    //note that this does not mean that the all messages have been 
    //received, just that the device has acknowledged the command
    func requestComplete(error: NSError!)
    
}

class waveOperation : NSOperation {

    //we will set the output data to the paramter passed in
    let outputData: NSMutableData
    let commandData: NSData
    let writePeripheral: CBPeripheral
    let writeCharacteristic: CBCharacteristic
    let timeout: Double
    var timer: NSTimer?
    init (writePeripheral: CBPeripheral, writeCharacteristic: CBCharacteristic, commandData: NSData, timeout: Double) {
        self.outputData = NSMutableData()
        self.commandData = commandData
        self.writePeripheral = writePeripheral
        self.writeCharacteristic = writeCharacteristic
        self.timeout = timeout
    }
    override func main() {
        //check for cancel at start up
        
        if self.cancelled {
            return
        }
        //send command
        writePeripheral.writeValue(commandData, forCharacteristic: writeCharacteristic, type: CBCharacteristicWriteType.WithResponse)

        timer = NSTimer.scheduledTimerWithTimeInterval( timeout/1000.0, target: self, selector: Selector("cancelTask"), userInfo: nil, repeats: false)
        NSRunLoop.currentRunLoop().addTimer(timer!, forMode: NSDefaultRunLoopMode)

        //we will need to interally check for timeout
        var lastlength = 0
        while (!complete()) {
            if (self.cancelled) {
                return
            }
            NSThread.sleepForTimeInterval(0.01)
            if (outputData.length > lastlength) {
                lastlength = outputData.length
                timer?.invalidate()
                timer = NSTimer.scheduledTimerWithTimeInterval( timeout/1000.0, target: self, selector: Selector("cancelTask"), userInfo: nil, repeats: false)
                NSRunLoop.currentRunLoop().addTimer(timer!, forMode: NSDefaultRunLoopMode)
            }
        }
        println("Completed Operation")
        timer?.invalidate()
        
    }
    
    func cancelTask() {
        println("Canceled Task")
        cancel()
    }
    
    func complete() -> Bool {
        
        //This is a very crude detector for completion for the moment
        if (outputData.length == 20) {
            var array = [UInt8](count: outputData.length, repeatedValue: 0)
            outputData.getBytes(&array, length: outputData.length)
            if (array[0] != 0x24) {
                return true
            }
            
        } else if (outputData.length == 200) {
            var array = [UInt8](count: outputData.length, repeatedValue: 0)
            outputData.getBytes(&array, length: outputData.length)
            if (array[0] == 0x24) {
                return true
            }
            
        }
        return false
    }
    
    func insertData(newData: NSData) {
        //this will insert data, check for completion and reset the timeout timer
        outputData.appendData(newData)
        
    }
    
    
}


class waveControlAndSync: NSObject, CBCentralManagerDelegate, CBPeripheralDelegate {
    
    var centralManager:CBCentralManager?
    let callbackDelegate:waveControlAndSyncDelegate!
    
    //timeout in ms for a command response segment to be received
    //(for multipart commands this is the time from one packet 
    //to the next packet in sequence)
    var commandTimeout:Double!
    
    //dictionary of peripherals either connecting or connected
    //peripherals are removed from this list at disconnection or 
    //at failure to connect (thus allowing automatic reconnect
    var connectingPeripherals:NSMutableDictionary!
    
    //dictionary of peripherals that are connected
    var wavePeripherals:NSMutableDictionary!
    
    //dictionary of write characteristics for connected peripherals
    var writeCharacteristics:NSMutableDictionary!
    
    //dictionary of notify characteristics for connected peripherals
    var notifyCharacteristics:NSMutableDictionary!
    
    //dictionary of serial numbers for connected peripherals
    var connectedSerials:NSMutableDictionary!
    
    //for simplicities sake, for now we will only allow a single command queue to exist
    var operationQueue:NSOperationQueue!
    
    var outputBuffer:NSMutableData!
    
    var scanning:Bool!

    init(delegate: waveControlAndSyncDelegate, timeout: Double = 3000) {
        callbackDelegate = delegate
        super.init()
        centralManager = CBCentralManager(delegate: self, queue: dispatch_get_main_queue())
        wavePeripherals = NSMutableDictionary()
        writeCharacteristics = NSMutableDictionary()
        notifyCharacteristics = NSMutableDictionary()
        connectingPeripherals = NSMutableDictionary()
        outputBuffer = NSMutableData()
        operationQueue = {
            var queue = NSOperationQueue()
            queue.name = "Command Queue"
            queue.maxConcurrentOperationCount = 1
            return queue
        }()

        scanning = false
        commandTimeout = timeout
    }
    
// This is the interface that the App should use to communicate with the bluetooth device
    
    //requests the waveControlAndSync Object to attempt to connect to a Wave Device
    //returns false if unable to scan
    func requestConnection() -> Bool {
        if (centralManager!.state == CBCentralManagerState.PoweredOn) {
            scanning = true
            let serviceUUIDs:[AnyObject] = [CBUUID(string: "180A")]
            centralManager!.scanForPeripheralsWithServices(serviceUUIDs, options: [ CBCentralManagerScanOptionAllowDuplicatesKey : true ])
            return true
        }
        return false
    }
    
    func cancelScan() {
        centralManager!.stopScan()
        scanning = false
    }
    
    //request that the system disconnect a currently connected Wave Device (if any)
    func disconnectWaveDevice(id: NSString) {
        var wavePeripheral:CBPeripheral?
        wavePeripheral = wavePeripherals.valueForKey(id as String) as? CBPeripheral
        if (wavePeripheral != nil) {
            println("Disconnecting Peripheral")
            centralManager!.cancelPeripheralConnection(wavePeripheral)

        }
        
    }
    
    //request to disconnect all devices
    func disconnectWaveDevices() {
        for (id, wavePeripheral) in wavePeripherals {
            centralManager!.cancelPeripheralConnection(wavePeripheral as? CBPeripheral )
        }
    }
    
    //request serial from a particular ID
    func getSerial(id: NSString?=nil) -> Bool {

        var rawArray:[UInt8] = [0x91, 0x00, 0x00];
        let command = NSData(bytes: &rawArray, length: rawArray.count)
        return sendCommand(id, command: command)
    }
    
    //request chart from a particular ID
    func getChart(id: NSString?=nil) -> Bool {
        var rawArray:[UInt8] = [0xC4, 0x03, 0x01, 0x06, 0x3, 0x04];
        let command = NSData(bytes: &rawArray, length: rawArray.count)
        return sendCommand(id, command: command)
    }
    
    //request steps from a particular ID
    func getSteps(id: NSString?=nil) -> Bool {
        var rawArray:[UInt8] = [0xC6, 0x01, 0x09, 0x09];
        let command = NSData(bytes: &rawArray, length: rawArray.count)
        return sendCommand(id, command: command)
    }
    
    //request time from a particular ID
    func getTime(id: NSString?=nil) -> Bool {
        var rawArray:[UInt8] = [0x89, 0x00, 0x00];
        let command = NSData(bytes: &rawArray, length: rawArray.count)
        return sendCommand(id, command: command)
    }
    
//End public interface
    
//Internal helper functions
    
    
    func sendCommand(id: NSString?, command: NSData) -> Bool {
        var writeCharacteristic:CBCharacteristic?
        var writePeripheral:CBPeripheral?
        var myId : NSString
        if (id == nil) {
            if (wavePeripherals.count>0) {
                myId = wavePeripherals.allKeys[0] as! NSString; //set to first key in block
            } else {
                return false
            }
        } else {
            myId = id!
        }
        writeCharacteristic = writeCharacteristics.valueForKey(myId as String) as! CBCharacteristic?
        writePeripheral = wavePeripherals.valueForKey(myId as String) as! CBPeripheral?
        if ( writeCharacteristic != nil && writePeripheral != nil) {
                //(writePeripheral as CBPeripheral!).writeValue(command, forCharacteristic: writeCharacteristic, type: CBCharacteristicWriteType.WithResponse)
                let command = waveOperation(writePeripheral: writePeripheral!, writeCharacteristic: writeCharacteristic!, commandData: command, timeout: commandTimeout)
                command.completionBlock = {
                    if (command.cancelled) {
                        return
                    }
                    self.callbackDelegate.receivedMessage(command.outputData, id: command.writePeripheral.identifier.UUIDString)
                }
                operationQueue.addOperation(command)
            return true
        } else {
            return false
        }
        
    }
    
//end internal functions
    
    
//Delegate Callbacks from CoreBluetooth

    //if user turns power on/off for bluetooth, the UI should know
    func centralManagerDidUpdateState(central: CBCentralManager!) {
        callbackDelegate.bluetoothManagerStateChange(central.state)
        
    }
    
    //for any discovered peripheral, we want to check if its name matches the required device name.
    //our sample devices are "808A", but that will be changing.
    //in principle this should get called for each peripheral that we discover
    func centralManager(central: CBCentralManager!, didDiscoverPeripheral peripheral: CBPeripheral!, advertisementData: [NSObject : AnyObject]!, RSSI: NSNumber!) {

        peripheral.delegate = self
        if ((peripheral.name != nil)) {println(peripheral.name)}
        
        if ((peripheral.name != nil) && ((peripheral.name == "808A") || (peripheral.name == "Wave")) && (connectingPeripherals.valueForKey(peripheral.identifier.UUIDString) == nil)) {
            //found a match, attempt to connect
            connectingPeripherals.setObject(peripheral, forKey: peripheral.identifier.UUIDString)
            centralManager!.connectPeripheral(peripheral, options: nil)
        }
        
    }
    
    //since we are only connecting to peripherals that match the correct name
    //anything that detects we need to immediately discover services
    //notify our callback delegate of the connection and ID
    func centralManager(centeral: CBCentralManager!, didConnectPeripheral peripheral: CBPeripheral!) {
        wavePeripherals.setObject(peripheral, forKey: peripheral.identifier.UUIDString)
        peripheral.discoverServices(nil)
        callbackDelegate.connectedWaveDevice(peripheral.identifier.UUIDString)
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
            writeCharacteristics.removeObjectForKey(peripheral.identifier.UUIDString)
            notifyCharacteristics.removeObjectForKey(peripheral.identifier.UUIDString)
            connectingPeripherals.removeObjectForKey(peripheral.identifier.UUIDString)
        }
    }
    
    //TODO: gracefully handle failed connections to peripherals
    func centralManager(central: CBCentralManager!, didFailToConnectPeripheral peripheral: CBPeripheral!, error: NSError!) {
        connectingPeripherals.removeObjectForKey(peripheral.identifier.UUIDString)
    }
    
    func peripheral(peripheral: CBPeripheral!, didDiscoverServices error: NSError!) {
        if let actualError = error{
            //may require error handling
        }
        else {
            for service in peripheral.services as! [CBService]!{
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
                for characteristic in service.characteristics as! [CBCharacteristic]{
                    switch characteristic.UUID.UUIDString{
                    case "FFE4":
                        //Set notification on notification characteristic
                        println("Found a general notify characteristic")
                        peripheral.setNotifyValue(true, forCharacteristic: characteristic)
                        notifyCharacteristics.setObject(characteristic, forKey: peripheral.identifier.UUIDString)
                    default:
                        println(characteristic.UUID.UUIDString)
                    }
                }
            } else if service.UUID == CBUUID(string: "FFE5") {
                for characteristic in service.characteristics as! [CBCharacteristic]{
                    switch characteristic.UUID.UUIDString{
                    case "FFE9":
                        // Save our write characteristic
                        println("Found a write characteristic")
                        writeCharacteristics.setObject(characteristic, forKey: peripheral.identifier.UUIDString)
                        //writeCharacteristic = characteristic
                        peripheral.discoverDescriptorsForCharacteristic(characteristic)
                    default:
                        println(characteristic.UUID.UUIDString)
                    }
                }
                
            } else if service.UUID == CBUUID(string: "180A") {
                println("found device information")
                for charateristic in service.characteristics as! [CBCharacteristic] {
                    println(charateristic.UUID)
                }
                
            } else {
                println("Unknown service")
                println(service.UUID)
            }
            
            
            if (notifyCharacteristics.valueForKey(peripheral.identifier.UUIDString) != nil && writeCharacteristics.valueForKey(peripheral.identifier.UUIDString) != nil) {
                //attempt to get serial number
                
                println("Would call get serial number")
                
            }
        }
        
    }
    
    //not used
    func peripheral(peripheral: CBPeripheral!, didDiscoverDescriptorsForCharacteristic characteristic: CBCharacteristic!, error: NSError!) {
        for descriptor in characteristic.descriptors as! [CBDescriptor] {
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
            #if os(iOS)
                var data : NSData! = characteristic.value
            #elseif os(OSX)
                var data : NSData! = characteristic.value()
            #endif
                if (data != nil) {
                    var count = data.length
                    var array = [UInt8](count: count, repeatedValue: 0)
                    data.getBytes(&array, length: count)
                    println(array.map{ String($0, radix: 16, uppercase: false)})
                    //callbackDelegate.receivedMessage(data, id: peripheral.identifier.UUIDString)
                    if (operationQueue.operationCount > 0) {
                        (operationQueue.operations[0] as! waveOperation).insertData(data)
                    }
                    //                    println(array)
                } else {
                    println( characteristic.value )
                }
                
            default:
                println(characteristic.UUID)
            #if os(iOS)
                var data : NSData! = characteristic.value
            #elseif os(OSX)
                var data : NSData! = characteristic.value()
            #endif
                
                println(NSString(data: data, encoding: NSUTF8StringEncoding));
                //                println(characteristic.value())
            }
        }
        
    }
    
    //confirmation that command completed
    func peripheral(peripheral: CBPeripheral!, didWriteValueForCharacteristic characteristic: CBCharacteristic!, error: NSError!) {
        println(characteristic.UUID)
        println(characteristic.value)
        println("Command complete")
        callbackDelegate.requestComplete(error)
        
    }
    
    //unlikely, not implemented
    func peripheral(peripheral: CBPeripheral!, didUpdateNotificationStateForCharacteristic characteristic: CBCharacteristic!, error: NSError!) {
        println("Received Update Notification")
        println(characteristic.UUID)
        #if os(iOS)
            var data : NSData! = characteristic.value
        #elseif os(OSX)
            var data : NSData! = characteristic.value()
        #endif
        
        if (data != nil) {
            var count = data.length
            var array = [UInt8](count: count, repeatedValue: 0)
            data.getBytes(&array, length: count)
            println(array.map{ String($0, radix: 16, uppercase: false)})
        } else {
        #if os(iOS)
            var data : NSData! = characteristic.value
        #elseif os(OSX)
            var data : NSData! = characteristic.value()
        #endif
            
            println(data)
        }
    }
    
//End Delegate Callbacks
    
}