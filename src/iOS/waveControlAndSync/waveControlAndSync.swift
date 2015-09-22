//
//  waveControlAndSync.swift
//  bluetoothTest
//
//  Created by Rudy Yukich on 4/6/15
//

import Foundation
import CoreBluetooth


/* Theory of operation

 *
 *    waveSyncManager provides a automated wrapper for waveControlAndSync so that a
 *    UI application need not directly interact with most of the BT primatives
 *    or state model.

 *    With waveSyncManager, one can get the list of connected devices (for the purpose of building
 *    a table through the underlying waveControlAndSync object.
 *
 *    waveSyncManager has a very limited required delegate class:
 *
 *          func syncStatusUpdate(status: WaveSyncStatus, deviceId: NSString?,  completeRatio: Float) - returns status updates for state changes in the sync process
 *
 *          func syncComplete(deviceId: NSString?, data: [WaveStep]) - returns an array of WaveSteps with all of the data from a sync operation

 *          func syncFailure(deviceId: NSString?) - final failure call from a sync attempt.  Sync should not be considered to be in a fatal failure until this message is returned
 *
 *          func deviceReady(id: NSString, serial: Array<UInt8>?, ready: Bool) - returns updates indicating the listed device is ready - or not ready (i.e. disconnected)
 *

 *    And should be manipulated though:
 *              - attampts to run a sync on the requested device
 *              - NOTE: failures will come back through syncStatusUpdate
 *          func attemptSync(deviceId: NSString? = nil)

 *
 *          func scan(shouldScan: Bool)

 *  Scanning status can be observed through:
 *          var shouldScan
 *          var scanning



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



 *
 */


/* Operational notes
 *  iOS doesn't clobber commands in transmission, however, the device will interrupt itself in return transmission
 *  in practice, what this means is that a new command during a multi-part message in progress will interrupt the multi-part message
 *   -> what this means is that we should maintain, per-device, a queue of message requests and not issue new messages until either the 
 *   -> message in progress has completed or has timed out
 *   -> After which, we can move onto the next message in the queue.
 *
 */



/* Protocol declarations */


protocol waveSyncManagerDelegate {
    //returns status updates for state changes in the sync process
    func syncStatusUpdate(status: WaveSyncStatus, deviceId: NSString?,  completeRatio: Float)
    
    //returns an array of WaveSteps with all of the data from a sync operation
    func syncComplete(deviceId: NSString?, data: [WaveStep])
    
    //final failure call from a sync attempt.  Sync should not be considered 
    //to be in a fatal failure until this message is returned
    func syncFailure(deviceId: NSString?)
    
    //returns updates indicating the listed device is ready - or not ready (i.e. disconnected)
    func deviceReady(id: NSString, serial: Array<UInt8>?, ready: Bool)
}


protocol waveControlAndSyncDelegate {
    //callback receiving the UUID of a successfully connected device
    func connectedWaveDevice(id: NSString)
    
    //callback receiving the serial number of a successfully connected device
    func connectedWaveDeviceSerial(id: NSString, serial: Array<UInt8>)
    
    //callback confirming disconnection of connected devices
    func disconnectedWaveDevice(id: NSString)
    
    //callback for receiving a message from a device -- this should change to an interface that calls back with the unpacked message
    func receivedMessage(message: WaveMessageResponse, id: NSString)
    
    //notification that the bluetooth manager changed state
    func bluetoothManagerStateChange(state: CBCentralManagerState)
    
    //notification of command complete for any individual command
    //note that this does not mean that the all messages have been 
    //received, just that the device has acknowledged the command
    func requestComplete(error: NSError!)
    
}





/* Major class objects */



//waveSyncManager provides a backend class that
//is be used to simplify the process of performing device
//registration and syncing

//this is the primary class that a simplied UX app should use to interact with
//the wave device using a fixed sync behavior
// i.e. check date -> set date -> collect data -> end

class waveSyncManager : NSObject, waveControlAndSyncDelegate {
    var waveController:waveControlAndSync?
    var deviceId:NSString!
    var callbackDelegate:waveSyncManagerDelegate!
    var operationQueue:NSOperationQueue
    var shouldScan:Bool
    var scanning:Bool
    required init(delegate: waveSyncManagerDelegate) {
        callbackDelegate = delegate
        operationQueue = {
            let queue = NSOperationQueue()
            queue.name = "Sync Queue"
            queue.maxConcurrentOperationCount = 1
            return queue
            }()
        shouldScan = true
        scanning = false
        super.init()
        waveController = waveControlAndSync(delegate: self)
        
        if (waveController!.centralManager!.state == CBCentralManagerState.PoweredOn) {
            waveController!.requestConnection()
            scanning = true
        }
        
    }
    
    func scan(shouldScan: Bool) {
        self.shouldScan = shouldScan
        if (shouldScan) {
            if (waveController!.centralManager!.state == CBCentralManagerState.PoweredOn) {
                waveController!.requestConnection()
                scanning = true
            }
        } else {
            waveController!.cancelScan()
            scanning = false
        }
    }
    
    func attemptSync(deviceId: NSString? = nil) {
        //creates a new waveSyncOperation and adds it to the sync operation queue
        
        let waveSync = waveSyncOperation(syncManager: self, deviceId: deviceId, timeout: 5000.0)
        waveSync.completionBlock = {
            if (waveSync.cancelled) {
                //Our wave sync operation
                //most likely timed out
                //or the device disconnected mid-sync
                //we will probably need to prompt the
                //user (we CAN retry, but for now
                //lets leave that up to the user
                //and see how frequently that
                //comes up in practice
                
                //but we will check for finished anyhow, e.g. if somehow
                //we timed out and finished both
                if (waveSync.status == WaveSyncStatus.Finished) {
                    self.callbackDelegate.syncComplete(waveSync.deviceId, data: waveSync.stepData)
                    
                } else {
                    self.callbackDelegate.syncFailure(waveSync.deviceId)
                }
                return
            }
            
            //handle non-cancled completion of sync
            //most likely this is a successfully completed
            //sync operation, and everything is in good shape
            //at this point
            if (waveSync.status == WaveSyncStatus.Finished) {
                self.callbackDelegate.syncComplete(waveSync.deviceId, data: waveSync.stepData)
                
            } else {
                self.callbackDelegate.syncFailure(waveSync.deviceId)
                
            }
        }
        operationQueue.addOperation(waveSync)
    }

    func connectedWaveDeviceSerialString(id: NSString) -> NSString? {
        if let data : NSData = waveController!.connectedSerials.valueForKey(id as String) as? NSData {
            let count = data.length
            var array = [UInt8](count: count, repeatedValue: 0)
            data.getBytes(&array, length: count)
            return array.map{ String($0, radix: 16, uppercase: true)}.joinWithSeparator("")
        }
        return nil
    }
    
/* Delegate Methods */
    
    
    
    func connectedWaveDevice(id: NSString) {
        //A device isn't "ready" until we get its serial number
        //then we can prompt a user for display
        //so connectedWaveDevice does not result in any immediate action
    }
    
    func connectedWaveDeviceSerial(id: NSString, serial: Array<UInt8>) {
        callbackDelegate.deviceReady(id, serial: serial, ready: true)
        
        
    }

    func disconnectedWaveDevice(id: NSString) {
        //A device is immediately no longer "ready" when it disconnects
        //so deviceReady should be called with "nil"
        
        
        callbackDelegate.deviceReady(id, serial: nil, ready: false )
    }
    
    func requestComplete(error: NSError!) {
        //this just confirms that the message was sent.
        //this is a 'do nothing' function at this time
    }
    
    
    func receivedMessage(message: WaveMessageResponse, id: NSString) {
        //It wouldn't take too long to make this capable of carrying out multiple syncs
        //at once, but for the sake of sanity, lets pass through all messages to the
        //active sync operation.
        
        //In the case that we receive serial number
        //receivedMessage calls, we just need to make sure that
        //the waveSyncOperation doesn't cancel on a bad ID (it should just drop the message)
        if (operationQueue.operationCount > 0) {
            //only send along the message if the id from received
            if ((operationQueue.operations[0] as! waveSyncOperation).deviceId == id) {
                (operationQueue.operations[0] as! waveSyncOperation).receivedMessage(message, id: id)
            }
        }
        
        
        
    }
    
    
    func bluetoothManagerStateChange(state: CBCentralManagerState) {
        print(state)
        if (state == CBCentralManagerState.PoweredOn && shouldScan) {
            waveController!.requestConnection()
            scanning = true
        } else if (state == CBCentralManagerState.PoweredOff) {
            scanning = false
        }
    }
    
    /* Internal Methods */
    func reportedTimeDelta(delta: NSTimeInterval) {
        
        print("Time Delta: " + delta.description)
        
    }
    
}


//waveSyncOperation is our async operation to attempt to sync to a particular wave device
class waveSyncOperation : NSOperation {
    let timeout: Double
    var deviceId: NSString?
    var timeoutDate : NSDate!
    var status : WaveSyncStatus!
    var syncManager: waveSyncManager
    var syncDay : Int
    var complete : Bool
    var stepData : [WaveStep]
    var startSync : NSDate
    init (syncManager: waveSyncManager, deviceId: NSString?, timeout: Double) {
        self.status = .Idle
        self.timeout = timeout
        //if nil deviceId is passed it, it will get set on our first call to
        //send a message
        self.deviceId = deviceId
        self.syncManager = syncManager
        self.syncDay = 0
        self.complete = false
        self.stepData = [WaveStep]()
        self.startSync = NSDate()
        self.timeoutDate = NSDate().dateByAddingTimeInterval(timeout/1000.0)
    }
    
    
    override func main() {
        var lastlength = 0
        self.timeoutDate = NSDate().dateByAddingTimeInterval(timeout/1000.0)
        
        
        if (self.status == .Idle) {
            //if status is Idle we need to request system time
            //and kick off the process
            deviceId = syncManager.waveController!.getTime(deviceId)
            self.status = .Start
            syncManager.callbackDelegate.syncStatusUpdate(self.status, deviceId: self.deviceId, completeRatio: 1.0)
        }
        
        
        while (!isComplete()) {
            if (self.cancelled) {
                return
            }
            //we will need to interally check for timeout
            //because NSTimer doesn't play well with NSOperations
            NSThread.sleepForTimeInterval(0.01)
            if (NSDate().compare(timeoutDate) == NSComparisonResult.OrderedDescending) {
                print("SYNC TIMEOUT")
                cancel()
            }
        }
        print("Completed Operation")
    }
    
    func isComplete() -> Bool {
        
        return self.complete
        
    }
    
    
    
    func receivedMessage(message: WaveMessageResponse, id: NSString) {
        //Called when a message comes in
        //this is the core of the state machine operation
        //as this can simply operate in a reactor pattern
        //until/unless the operation is canceled
        //and removed from the queue.
        
        //this is a 7 state state-machine as follows:
        
        //Fail -- entered only when the operation is giving up
        //Idle -- the state before the operation is activated on the operation queue
        //Start -- the state entered when the operation is activated on the operation queue
        //VerifingDate -- state entered once a date has been retrieved from a movo device
        //SettingDate -- state entered when the sync manager attempts to update the date on the movo device
        //DownloadingData -- state entered when the sync manager starts requesting chart data from the movo device
        //Finished -- state entered once all 7 days of history have been received
        
        
        //Right now there is no attempt to retry / reconnect within this state machine -- 
        //all errors other than unexpected data are fatal
        
        //if no data is received for greater than the timeout, then the 
        //operation is canceled.
        
        //there is a small chance that repeated non-error data could come in not causing a state transition but not allowing a timeout. 
        //this is unlikely to occur and is -for the time being- going to be considered expected behavior.  
        //we should revisit if this becomes a problem in practice
        
        //see that this message belongs to this operation
        //if it does not, ignore it
        if (id != self.deviceId) {
            return
        }
        
        //otherwise this is a valid message from our
        //related device.
        //update our timeout time
        timeoutDate = NSDate().dateByAddingTimeInterval(timeout/1000.0)
        switch self.status! {
            case .Start:
                //we received a message while in the start state, if it was get time success, we 
                if (message.code == WaveCommandResponseCodes.GetTimeSuccess) {
                    if let timestamp = message.data![0] as? WaveYMDHMSDOW {
                        //this will let us compare the date the device has with our current date
                        var timestampDate = waveYMDHMSDOWGMTToNSDate(timestamp)
                        print("WAVE DEVICE TIME: "+timestampDate.description)
                        var delta = NSDate().timeIntervalSinceDate(timestampDate)
                        self.status = WaveSyncStatus.VerifingDate
                        syncManager.callbackDelegate.syncStatusUpdate(self.status, deviceId: deviceId, completeRatio: 1.0)
                        syncManager.reportedTimeDelta(delta)
                        
                        print("Time delta: " + delta.description)
                        if (abs(delta) > 30*60) {
                            //if delta is greater than 30 minutes
                            self.status = WaveSyncStatus.SettingDate
                            syncManager.callbackDelegate.syncStatusUpdate(self.status, deviceId: deviceId, completeRatio: 1.0)
                            
                            var currentTime : WaveYMDHMSDOW = nSDateToWaveYMDHMSDOWGMT(NSDate())
                            syncManager.waveController?.setTime(deviceId, Year: currentTime.Year, Month: currentTime.Month, Day: currentTime.Day, Hours: currentTime.Hours, Minutes: currentTime.Minutes, Seconds: currentTime.Seconds, DOW: currentTime.DOW)
                        } else {
                            self.status = WaveSyncStatus.DownloadingData
                            syncManager.callbackDelegate.syncStatusUpdate(self.status, deviceId: deviceId, completeRatio: 0.0)
                            syncDay = 0; //start 7 days back and roll to present
                            requestDataForDay(syncDay)
                        }

                    } else {
                        self.status = WaveSyncStatus.Fail
                        syncManager.callbackDelegate.syncStatusUpdate(self.status, deviceId: deviceId, completeRatio: 1.0)
                        self.cancel()
                        
                    }
                } else {
                    if (message.code == WaveCommandResponseCodes.GetTimeFailure || message.code == WaveCommandResponseCodes.Timeout || message.code == WaveCommandResponseCodes.UnknownResponse) {
                        //unrecoverable error
                        self.status = WaveSyncStatus.Fail
                        syncManager.callbackDelegate.syncStatusUpdate(self.status, deviceId: deviceId, completeRatio: 1.0)
                        self.cancel()
                    }
                    //otherwise fall through for next message
                }
            case .SettingDate:
                
                if (message.code == WaveCommandResponseCodes.SetTimeSuccess) {
                    self.status = WaveSyncStatus.DownloadingData
                    syncManager.callbackDelegate.syncStatusUpdate(self.status, deviceId: deviceId, completeRatio: 0.0)
                    syncDay = 0; //start 7 days back and roll to present
                    requestDataForDay(syncDay)
                } else {
                    if (message.code == WaveCommandResponseCodes.SetTimeFailure || message.code == WaveCommandResponseCodes.Timeout || message.code == WaveCommandResponseCodes.UnknownResponse) {
                        //unrecoverable error
                        self.status = WaveSyncStatus.Fail
                        syncManager.callbackDelegate.syncStatusUpdate(self.status, deviceId: deviceId, completeRatio: 1.0)
                        self.cancel()
                    }
                }

            case .DownloadingData:
                //downloadingData has 7 steps for the current device
                
                if (message.code == WaveCommandResponseCodes.GetChartSuccess) {
                    var dateFormatter = NSDateFormatter()
                    dateFormatter.dateFormat = "yyyy-MM-dd HH:mm"
                    var stepcount = 0
                    if var newdata : [WaveStep] = message.data! as? [WaveStep] {
                        stepData += newdata
                    
                        if (newdata.count > 0) {
                            for x in newdata {
                                stepcount += x.steps
                                print(String(x.steps) + " steps at: " + x.start.description + " - " + x.end.description)
                            }
                            var firstStep = (message.data! as! [WaveStep])[0].start
                            var finalStep = (message.data! as! [WaveStep]).last?.end
                            print("Counted "+String(stepcount)+" steps from: "+dateFormatter.stringFromDate(firstStep)+" to "+dateFormatter.stringFromDate(finalStep!))
                            print(""+firstStep.description+" to "+finalStep!.description)
                        
                        } else {
                            //still success, just nothing to print
                            print("Received Chart with no steps")
                        }
                    }
                    
                    syncDay = syncDay+1
                    if (syncDay > 7) {
                        //then we are complete
                        self.complete = true
                        self.status = WaveSyncStatus.Finished
                        syncManager.callbackDelegate.syncStatusUpdate(self.status, deviceId: deviceId, completeRatio: 1.0)
                    } else {
                        syncManager.callbackDelegate.syncStatusUpdate(self.status, deviceId: deviceId, completeRatio: Float(syncDay)/7.0)
                        requestDataForDay(syncDay)
                        
                    }
                    
                } else {
                    if (message.code == WaveCommandResponseCodes.GetChartFailure || message.code == WaveCommandResponseCodes.Timeout || message.code == WaveCommandResponseCodes.UnknownResponse) {
                        //unrecoverable error
                        self.status = WaveSyncStatus.Fail
                        syncManager.callbackDelegate.syncStatusUpdate(self.status, deviceId: deviceId, completeRatio: 1.0)
                        self.cancel()
                    }
                    
                    
                }
            
            default:
                if (message.code == WaveCommandResponseCodes.Timeout || message.code == WaveCommandResponseCodes.UnknownResponse) {
                    //unrecoverable error
                    self.status = WaveSyncStatus.Fail
                    syncManager.callbackDelegate.syncStatusUpdate(self.status, deviceId: deviceId, completeRatio: 1.0)
                    self.cancel()
                }
        }

        print("WaveSyncOperation Received Message")
        
        
    }
    
    //startSync was locked when the sync started to avoid syncs at midnight GMT messing things
    //up
    //then we use parameter Int as starting from 7 days ago until day == 7 (now)
    func requestDataForDay(day: Int) {
        
        let waveDate : WaveYMDHMSDOW = nSDateToWaveYMDHMSDOWGMT(NSDate(timeInterval: NSTimeInterval(-60*60*24*(7-day)), sinceDate: startSync))
        
        syncManager.waveController!.getChart(deviceId, Year: waveDate.Year, Month: waveDate.Month, Day: waveDate.Day)
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

    init(delegate: waveControlAndSyncDelegate, timeout: Double = 5000) {
        callbackDelegate = delegate
        super.init()
        centralManager = CBCentralManager(delegate: self, queue: dispatch_get_main_queue())
        wavePeripherals = NSMutableDictionary()
        writeCharacteristics = NSMutableDictionary()
        notifyCharacteristics = NSMutableDictionary()
        connectingPeripherals = NSMutableDictionary()
        connectedSerials = NSMutableDictionary()
        outputBuffer = NSMutableData()
        operationQueue = {
            let queue = NSOperationQueue()
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
            let serviceUUIDs:[CBUUID]? = [CBUUID(string: "180A")]
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
            print("Disconnecting Peripheral")
            centralManager!.cancelPeripheralConnection(wavePeripheral!)

        }
        
    }
    
    //request to disconnect all devices
    func disconnectWaveDevices() {
        for (id, wavePeripheral) in wavePeripherals {
            centralManager!.cancelPeripheralConnection((wavePeripheral as? CBPeripheral)! )
        }
    }
    
    //request serial from a particular ID
    func getSerial(id: NSString?=nil) -> NSString? {

        var rawArray:[UInt8] = [0x91, 0x00, 0x00];
        let command = NSData(bytes: &rawArray, length: rawArray.count)
        return sendCommand(id, command: command)
    }
    
    //request chart from a particular ID
    func getChart(id: NSString?=nil, Year: Int, Month: Int, Day: Int) -> NSString? {
        
        if (!checkYMDHMSDOW(Year, Month: Month, Day: Day)) {
            return nil
        }
        
        var xor : UInt8 = UInt8(Year) ^ UInt8(Month) ^ UInt8(Day)
        var rawArray:[UInt8] = [0xC4, 0x03, UInt8(Year), UInt8(Month), UInt8(Day), xor];
        let command = NSData(bytes: &rawArray, length: rawArray.count)
        return sendCommand(id, command: command)
    }
    
    //request steps from a particular ID
    //this is the number of steps in the current 30-minute window
    func getSteps(id: NSString?=nil) -> NSString? {
        var rawArray:[UInt8] = [0xC6, 0x01, 0x09, 0x09];
        let command = NSData(bytes: &rawArray, length: rawArray.count)
        return sendCommand(id, command: command)
    }
    
    //request time from a particular ID
    func getTime(id: NSString?=nil) -> NSString? {
        var rawArray:[UInt8] = [0x89, 0x00, 0x00];
        let command = NSData(bytes: &rawArray, length: rawArray.count)
        return sendCommand(id, command: command)
    }
    
    //set the time for a device
    //including day of week
    func setTime(id: NSString?=nil, Year: Int, Month: Int, Day: Int, Hours: Int, Minutes: Int, Seconds: Int, DOW: Int) -> NSString? {
        
        if (!checkYMDHMSDOW(Year, Month: Month, Day: Day, Hours: Hours, Minutes: Minutes, Seconds: Seconds, DOW: DOW)) {
            return nil
        }
        var xor : UInt8 = UInt8(Year) ^ UInt8(Month) ^ UInt8(Day) ^ UInt8(Hours) ^ UInt8(Minutes) ^ UInt8(Seconds) ^ UInt8(DOW)
        var rawArray:[UInt8] = [0xC2, 0x07, UInt8(Year), UInt8(Month), UInt8(Day), UInt8(Hours), UInt8(Minutes), UInt8(Seconds), UInt8(DOW), xor]
        let command = NSData(bytes: &rawArray, length: rawArray.count)
        return sendCommand(id, command: command)
        
    }
    
//End public interface
    
//Internal helper functions
    
    func sendCommand(id: NSString?, command: NSData) -> NSString? {
        var writeCharacteristic:CBCharacteristic?
        var writePeripheral:CBPeripheral?
        var myId : NSString
        if (id == nil) {
            if (wavePeripherals.count>0) {
                myId = wavePeripherals.allKeys[0] as! NSString; //set to first key in block
            } else {
                return nil
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
                        //temporary machanism for failed call (might actually be long term appropriate)
                        //do we want to let the caller know what
                        //command timed out?  Since we are driving this synchronously 
                        //maybe we don't care
                        self.callbackDelegate.receivedMessage(WaveMessageResponse(code: WaveCommandResponseCodes.Timeout, data: nil, mode: nil), id: command.writePeripheral.identifier.UUIDString)
                        return
                    }
                    
                    //we should parse the commands here rather than requiring an intermediary
                    let count = command.outputData.length
                    var array = [UInt8](count: count, repeatedValue: 0)
                    command.outputData.getBytes(&array, length: count)
                    if let responseCode = WaveCommandResponseCodes(rawValue: Int(array[0])) {
                        switch responseCode {
                            case .GetChartSuccess:
                                self.parseChartResponse(array, id: command.writePeripheral.identifier.UUIDString)
                            case .GetDataSuccess:
                                self.parseDeviceDataResponse(array, id: command.writePeripheral.identifier.UUIDString)
                            case .GetTimeSuccess:
                                self.parseTimeResponse(array, id: command.writePeripheral.identifier.UUIDString)
                            case .GetVersionSuccess:
                                self.parseVersionResponse(array, id: command.writePeripheral.identifier.UUIDString)
                            case .GetSerialSuccess:
                                self.parseSerialResponse(array, id: command.writePeripheral.identifier.UUIDString)
                            /* All of these cases just want to pass the code back to the next tier up
                            case .SetTimeSuccess:
                                //No meaningful return information
                                println("SetTimeSuccess")
                            case .SetPasswordSuccess:
                                //No meaningful return information
                                println("SetPasswordSuccess")
                            case .SetPasswordFailure:
                                println("SetPasswordFailure")
                            case .GetChartFailure:
                                println("GetChartFailure")
                            case .GetDataFailure:
                                println("GetDataFailure")
                            case .SetTimeFailure:
                                println("SetTimeFailure")
                            case .GetTimeFailure:
                                println("GetTimeFailure")
                            case .GetVersionFailure:
                                println("GetVersionFailure")
                            case .GetSerialFailure:
                                println("GetSerialFailure")
                            */
                            default:
                                self.callbackDelegate.receivedMessage(WaveMessageResponse(code: responseCode, data: nil, mode: nil), id: command.writePeripheral.identifier.UUIDString)
                        }
                    } else {
                        self.callbackDelegate.receivedMessage(WaveMessageResponse(code: WaveCommandResponseCodes.UnknownResponse, data: nil, mode: nil), id: command.writePeripheral.identifier.UUIDString)
                    }
                }
                operationQueue.addOperation(command)
            return myId
        } else {
            return nil
        }
        
    }
    

    func checksum(array: [UInt8]) -> Bool {
        let datalen = Int(array[1])
        if (array.count < (datalen + 3)) {
            return false
        }
        let checksum = array[datalen+2]
        
        var xor = array[2]
        for (var i = 1; i<datalen; i++) {
            xor = xor ^ array[2+i]
        }
        if (xor == checksum) {
            return true
        }
        
        return false
        
    }
    
    func parseChartResponse(array: [UInt8], id: NSString) {
        var success = false
        var xor : UInt8
        if (array.count >= 3) {
            //we already know that array[0] == the right code
            //length data is valid
            if (checksum(array)) {
                //checksum valid
                var chartData = Array(array[2..<(Int(array[1]+2))])
                var chartArray = [WaveStep]()
                if (chartData.count < 3) {
                    //then we actually go a date out of range response
                    self.callbackDelegate.receivedMessage(WaveMessageResponse(code: WaveCommandResponseCodes.GetChartSuccess, data: chartArray, mode: nil) , id: id)
                    return
                    
                }
                /* Extract YMD */
                var Year : Int = Int(chartData[0])
                var Month : Int = Int(chartData[1])
                var Day : Int = Int(chartData[2])
                
                if (checkYMDHMSDOW(Year, Month: Month, Day: Day, Hours: nil, Minutes: nil, Seconds: nil, DOW: nil)) {
                    /* Valid time stamp */
                    
                    /* This is the layer where we will translate from Wave Date/Time stamps to system date time
                    * stamps */

                    var startTime : NSDate = waveYMDHMSDOWGMTToNSDate(WaveYMDHMSDOW(Year: Year, Month: Month, Day: Day, Hours: 0, Minutes: 0, Seconds: 0, DOW: 0))
                        // Valid NSDate for handling 
                        // Right now we are at fixed 30 minute time stamps
                    for (var i = 3; i < (chartData.count-1); i += 2) {
                        var endTime = startTime.dateByAddingTimeInterval(NSTimeInterval(60*30))
                        //NOTE: this excludes 0xFF in either LSB or MSB
                        //it is unclear how the firmware operates: we know 0xFF in MSB
                        //indicates no data for that byte, but we don't know if 0xFF in 
                        //LSB also indicates no data
                        //var low = (chartData[i] != 0xFF) ? Int(chartData[i]):0
                        var low = Int(chartData[i])
                        var high = (chartData[i+1] != 0xFF) ? Int(chartData[i+1]):0
                        var count = low | (high<<8)
                        //protect for "reserved data case"
                        if (chartData[i+1] == 0xFF) {
                            count = 0
                        }
                        if (count > 0) {
                            chartArray.append(WaveStep(start: startTime, end: endTime, steps: count))
                        }
                        startTime = endTime
                        if (NSDate().compare(startTime) == NSComparisonResult.OrderedAscending) {
                            //then startTime is in the future
                            //we can ignore the rest of steps in the time 
                            //set
                            break;
                            
                        }
                    }
                    self.callbackDelegate.receivedMessage(WaveMessageResponse(code: WaveCommandResponseCodes.GetChartSuccess, data: chartArray, mode: nil) , id: id)
                    success = true
             
                }

            }
            
        }
        if (success == false) {
            self.callbackDelegate.receivedMessage(WaveMessageResponse(code: WaveCommandResponseCodes.ParseError, data: nil, mode: nil), id: id)
        }
        
        
    }
    
    func parseDeviceDataResponse(array: [UInt8], id: NSString) {
        var success = false
        var xor : UInt8
        if (array.count >= 3) {
            //we already know that array[0] is correct
            //length data is valid
            if (checksum(array)) {
                //checksum valid
                var data = Array(array[2..<(Int(array[1]+2))])
                self.callbackDelegate.receivedMessage(WaveMessageResponse(code: WaveCommandResponseCodes.GetDataSuccess, data: [NSData(bytes: &data, length: data.count)], mode: nil) , id: id)
                success = true
            }
            
        }
        if (success == false) {
            self.callbackDelegate.receivedMessage(WaveMessageResponse(code: WaveCommandResponseCodes.ParseError, data: nil, mode: nil), id: id)
        }
        
        
    }
    
    func parseTimeResponse(array: [UInt8], id: NSString) {
        var success = false
        var xor : UInt8
        if (array.count >= 3) {
            //we already know that array[0] is correct
            //length data is valid
            if (checksum(array)) {
                //checksum valid
                //therefore parse datetime - for now we'll use our custom type
                var timeRaw = Array(array[2..<(Int(array[1]+2))])
                let Year = Int(timeRaw[0])
                let Month = Int(timeRaw[1])
                let Day = Int(timeRaw[2])
                let Hours = Int(timeRaw[3])
                let Minutes = Int(timeRaw[4])
                let Seconds = Int(timeRaw[5])
                let DOW = Int(timeRaw[6])
                if (checkYMDHMSDOW(Year, Month: Month, Day: Day, Hours: Hours, Minutes: Minutes, Seconds: Seconds, DOW: DOW)) {
                    self.callbackDelegate.receivedMessage(WaveMessageResponse(code: WaveCommandResponseCodes.GetTimeSuccess, data: [WaveYMDHMSDOW(Year: Year, Month: Month, Day: Day, Hours: Hours, Minutes: Minutes, Seconds: Seconds, DOW: DOW)], mode: nil) , id: id)
                }
                success = true
            }
            
        }
        if (success == false) {
            self.callbackDelegate.receivedMessage(WaveMessageResponse(code: WaveCommandResponseCodes.ParseError, data: nil, mode: nil), id: id)
        }
    
    }
    
    func parseVersionResponse(array: [UInt8], id: NSString) {
        var success = false
        var xor : UInt8
        if (array.count >= 3) {
            //we already know that array[0] is correct
            //length data is valid
            if (checksum(array)) {
                //checksum valid
                //therefore version number is
                var version = Array(array[2..<(Int(array[1]+2))])
                self.callbackDelegate.receivedMessage(WaveMessageResponse(code: WaveCommandResponseCodes.GetVersionSuccess, data: [NSData(bytes: &version, length: version.count)], mode: nil) , id: id)
                success = true
            }
            
        }
        if (success == false) {
            self.callbackDelegate.receivedMessage(WaveMessageResponse(code: WaveCommandResponseCodes.ParseError, data: nil, mode: nil), id: id)
        }
        
    }
    
    func parseSerialResponse(array: [UInt8], id: NSString) {
        var success = false
        var xor : UInt8
        if (array.count >= 3) {
            //we already know that array[0] is correct
                //length data is valid
            if (checksum(array)) {
                //checksum valid
                //therefore serial is
                var serial = Array(array[2..<(Int(array[1]+2))])
                self.connectedSerials.setObject(NSData(bytes: serial, length: serial.count*sizeof(UInt8)), forKey: id)
                self.callbackDelegate.connectedWaveDeviceSerial(id, serial: serial)
                self.callbackDelegate.receivedMessage(WaveMessageResponse(code: WaveCommandResponseCodes.GetSerialSuccess, data: [NSData(bytes: &serial, length: serial.count)], mode: nil) , id: id)
                success = true
            }
            
        }
        if (success == false) {
            self.callbackDelegate.receivedMessage(WaveMessageResponse(code: WaveCommandResponseCodes.ParseError, data: nil, mode: nil), id: id)
        }
    }
    

//end utility functions
    
    
//Delegate Callbacks from CoreBluetooth

    //if user turns power on/off for bluetooth, the UI should know
    func centralManagerDidUpdateState(central: CBCentralManager) {
        callbackDelegate.bluetoothManagerStateChange(central.state)
        
    }
    
    //for any discovered peripheral, we want to check if its name matches the required device name.
    //our sample devices are "808A", but that will be changing.
    //in principle this should get called for each peripheral that we discover
    func centralManager(central: CBCentralManager, didDiscoverPeripheral peripheral: CBPeripheral, advertisementData: [String : AnyObject], RSSI: NSNumber) {

        peripheral.delegate = self
        if ((peripheral.name != nil)) {print(peripheral.name)}
        
        if ((peripheral.name != nil) &&  ( /* (peripheral.name == "808A") || */ (peripheral.name == "Wave")) && (connectingPeripherals.valueForKey(peripheral.identifier.UUIDString) == nil)) {
            //found a match, attempt to connect
            connectingPeripherals.setObject(peripheral, forKey: peripheral.identifier.UUIDString)
            centralManager!.connectPeripheral(peripheral, options: nil)
        }
        
    }
    
    //since we are only connecting to peripherals that match the correct name
    //anything that detects we need to immediately discover services
    //notify our callback delegate of the connection and ID
    func centralManager(centeral: CBCentralManager, didConnectPeripheral peripheral: CBPeripheral) {
        wavePeripherals.setObject(peripheral, forKey: peripheral.identifier.UUIDString)
        peripheral.discoverServices(nil)
        callbackDelegate.connectedWaveDevice(peripheral.identifier.UUIDString)
    }
    
    
    //this should let us handle disconnections properly
    //right now we just want to remove the device from our list of wavePeripherals if it is a known device
    //and if it is a known device, we let the UI know.
    func centralManager(central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: NSError?) {
        var wavePeripheral : CBPeripheral?
        wavePeripheral = wavePeripherals.valueForKey(peripheral.identifier.UUIDString) as? CBPeripheral
        if (peripheral == wavePeripheral) {
            wavePeripherals.removeObjectForKey(peripheral.identifier.UUIDString)
            writeCharacteristics.removeObjectForKey(peripheral.identifier.UUIDString)
            notifyCharacteristics.removeObjectForKey(peripheral.identifier.UUIDString)
            connectingPeripherals.removeObjectForKey(peripheral.identifier.UUIDString)
            connectedSerials.removeObjectForKey(peripheral.identifier.UUIDString)
            callbackDelegate.disconnectedWaveDevice(peripheral.identifier.UUIDString)

        }
    }
    
    //TODO: gracefully handle failed connections to peripherals
    func centralManager(central: CBCentralManager, didFailToConnectPeripheral peripheral: CBPeripheral, error: NSError?) {
        connectingPeripherals.removeObjectForKey(peripheral.identifier.UUIDString)
    }
    
    func peripheral(peripheral: CBPeripheral, didDiscoverServices error: NSError?) {
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
    func peripheral(peripheral: CBPeripheral, didDiscoverCharacteristicsForService service: CBService, error: NSError?) {
        if let actualError = error{
        }
        else {
            if service.UUID == CBUUID(string: "FFE0"){
                for characteristic in service.characteristics as [CBCharacteristic]! {
                    switch characteristic.UUID.UUIDString{
                    case "FFE4":
                        //Set notification on notification characteristic
                        print("Found a general notify characteristic")
                        peripheral.setNotifyValue(true, forCharacteristic: characteristic)
                        notifyCharacteristics.setObject(characteristic, forKey: peripheral.identifier.UUIDString)
                    default:
                        print(characteristic.UUID.UUIDString)
                    }
                }
            } else if service.UUID == CBUUID(string: "FFE5") {
                for characteristic in service.characteristics as [CBCharacteristic]! {
                    switch characteristic.UUID.UUIDString{
                    case "FFE9":
                        // Save our write characteristic
                        print("Found a write characteristic")
                        writeCharacteristics.setObject(characteristic, forKey: peripheral.identifier.UUIDString)
                        //writeCharacteristic = characteristic
                        peripheral.discoverDescriptorsForCharacteristic(characteristic)
                    default:
                        print(characteristic.UUID.UUIDString)
                    }
                }
                
            } else if service.UUID == CBUUID(string: "180A") {
                print("found device information")
                for charateristic in service.characteristics as [CBCharacteristic]! {
                    print(charateristic.UUID)
                }
                
            } else {
                print("Unknown service")
                print(service.UUID)
            }
            
            
            if (notifyCharacteristics.valueForKey(peripheral.identifier.UUIDString) != nil && writeCharacteristics.valueForKey(peripheral.identifier.UUIDString) != nil) {

                
            }
        }
        
    }
    
    //not used
    func peripheral(peripheral: CBPeripheral, didDiscoverDescriptorsForCharacteristic characteristic: CBCharacteristic, error: NSError?) {
        for descriptor in characteristic.descriptors as [CBDescriptor]! {
            print("found descriptor", terminator: "")
            print(descriptor.UUID)
        }
    }
    
    //receive update notifications
    //retrieve data from the read characteristic
    func peripheral(peripheral: CBPeripheral, didUpdateValueForCharacteristic characteristic: CBCharacteristic, error: NSError?) {
        if let actualError = error{
        }else {
            switch characteristic.UUID.UUIDString{
            case "FFE4":
                print(characteristic.UUID)
            #if os(iOS)
                let data : NSData! = characteristic.value
            #elseif os(OSX)
                var data : NSData! = characteristic.value()
            #endif
                if (data != nil) {
                    let count = data.length
                    var array = [UInt8](count: count, repeatedValue: 0)
                    data.getBytes(&array, length: count)
                    print(array.map{ String($0, radix: 16, uppercase: false)})
                    //callbackDelegate.receivedMessage(data, id: peripheral.identifier.UUIDString)
                    if (operationQueue.operationCount > 0) {
                        (operationQueue.operations[0] as! waveOperation).insertData(data)
                    }
                    //                    println(array)
                } else {
                    print( characteristic.value )
                }
                
            default:
                print(characteristic.UUID)
            #if os(iOS)
                let data : NSData! = characteristic.value
            #elseif os(OSX)
                var data : NSData! = characteristic.value()
            #endif
                
                print(NSString(data: data, encoding: NSUTF8StringEncoding));
                //                println(characteristic.value())
            }
        }
        
    }
    
    //confirmation that command completed
    func peripheral(peripheral: CBPeripheral, didWriteValueForCharacteristic characteristic: CBCharacteristic, error: NSError?) {
        print(characteristic.UUID)
        print(characteristic.value)
        print("Command complete")
        callbackDelegate.requestComplete(error)
        
    }
    
    //unlikely, not implemented
    func peripheral(peripheral: CBPeripheral, didUpdateNotificationStateForCharacteristic characteristic: CBCharacteristic, error: NSError?) {
        print("Received Update Notification")
        print(characteristic.UUID.UUIDString)
        #if os(iOS)
            let data : NSData! = characteristic.value
        #elseif os(OSX)
            var data : NSData! = characteristic.value()
        #endif
        
        if (data != nil) {
            let count = data.length
            var array = [UInt8](count: count, repeatedValue: 0)
            data.getBytes(&array, length: count)
            print(array.map{ String($0, radix: 16, uppercase: false)})
        } else {
        #if os(iOS)
            let data : NSData? = characteristic.value
        #elseif os(OSX)
            var data : NSData? = characteristic.value()
        #endif
            
            print(data)
        }
        
        if (characteristic.UUID.UUIDString == "FFE4") {
            //attempt to get serial number
            print("Requesting Serial number")
            getSerial(peripheral.identifier.UUIDString)
            
        }
    }
    
//End Delegate Callbacks
    
}


////waveOperation is a utility NSOperation class used by waveControlAndSync
class waveOperation : NSOperation {
    
    //we will set the output data to the paramter passed in
    let outputData: NSMutableData
    let commandData: NSData
    let writePeripheral: CBPeripheral
    let writeCharacteristic: CBCharacteristic
    let timeout: Double
    var commandType: UInt8
    var commandDataLength: Int
    var timeoutDate : NSDate!
    init (writePeripheral: CBPeripheral, writeCharacteristic: CBCharacteristic, commandData: NSData, timeout: Double) {
        self.commandType = 0
        self.commandDataLength = -1
        self.outputData = NSMutableData()
        self.commandData = commandData
        self.writePeripheral = writePeripheral
        self.writeCharacteristic = writeCharacteristic
        self.timeout = timeout
        self.timeoutDate = NSDate().dateByAddingTimeInterval(timeout/1000.0)
    }
    
    
    override func main() {
        var lastlength = 0
        self.timeoutDate = NSDate().dateByAddingTimeInterval(timeout/1000.0)
        
        //send command
        writePeripheral.writeValue(commandData, forCharacteristic: writeCharacteristic, type: CBCharacteristicWriteType.WithResponse)
        
        while (!complete()) {
            if (self.cancelled) {
                return
            }
            //we will need to interally check for timeout
            //because NSTimer doesn't play well with NSOperations
            NSThread.sleepForTimeInterval(0.01)
            if (outputData.length > lastlength) {
                lastlength = outputData.length
                timeoutDate = NSDate().dateByAddingTimeInterval(timeout/1000.0)
                print("restarted timer")
            } else if (NSDate().compare(timeoutDate) == NSComparisonResult.OrderedDescending) {
                print("TIMEOUT")
                cancel()
            }
        }
        print("Completed Operation")
        
    }
    
    func cancelTask() {
        print("Canceled Task")
        cancel()
    }
    
    func complete() -> Bool {
        
        //This is a reasonable detector for completion for the moment / this API level
        if ( (self.commandDataLength >= 0) && outputData.length >= (self.commandDataLength+3)) {
            //then we have a completed packet
            return true
        }
        return false
    }
    
    func insertData(newData: NSData) {
        var firstMessage = false
        if (outputData.length == 0) {
            //then we are looking at the first message
            firstMessage = true
            
            
        }
        //this will insert data, check for completion and reset the timeout timer
        outputData.appendData(newData)
        if (firstMessage) {
            var array = [UInt8](count: outputData.length, repeatedValue: 0)
            outputData.getBytes(&array, length: outputData.length)
            
            //we need to decode the first byte to get command / response, and the second to figure out the length of data expected
            
            self.commandType = array[0]
            let testCommand : WaveCommandResponseCodes? = WaveCommandResponseCodes(rawValue: Int(self.commandType))
            if (testCommand != nil) {
                switch testCommand! {
                case .SetPasswordSuccess:
                    break
                case .GetChartSuccess:
                    break
                case .GetDataSuccess:
                    break
                case .SetTimeSuccess:
                    break
                case .GetTimeSuccess:
                    break
                case .GetVersionSuccess:
                    break
                case .GetSerialSuccess:
                    break
                case .SetPasswordFailure:
                    break
                case .GetChartFailure:
                    break
                case .GetDataFailure:
                    break
                case .SetTimeFailure:
                    break
                case .GetVersionFailure:
                    break
                case .GetSerialFailure:
                    break
                default:
                    //anything that isn't in the above is a failure, and we should abort the
                    //attempt to receive this data
                    cancel()
                }
            } else {
                //if it can't be converted to a command type it is also a failure
                cancel()
            }
            
            
            //note that command data length is always total length - 3 (2byte header, 1byte footer)
            self.commandDataLength = Int(array[1])
            
        }
        
        
    }
    
    
}







/* Enums and Internal structures */

enum WaveSyncStatus : Int {
    case Fail
    case Idle
    case Start
    case VerifingDate
    case SettingDate
    case DownloadingData
    case Finished
}

enum WaveCommandResponseCodes: Int {
    case SetPasswordSuccess = 0x25
    case GetChartSuccess = 0x24
    case GetDataSuccess = 0x26
    case SetTimeSuccess = 0x22
    case GetTimeSuccess = 0x29
    case GetVersionSuccess = 0x30
    case GetSerialSuccess = 0x31
    case SetPasswordFailure = 0x05
    case GetChartFailure = 0x04
    case GetDataFailure = 0x06
    case SetTimeFailure = 0x02
    case GetTimeFailure = 0x09
    case GetVersionFailure = 0x0A
    case GetSerialFailure = 0x0B
    case UnknownResponse = -1
    case Timeout = -2
    case ParseError = -3
}

enum WaveDeviceDataMode : Int {
    case MondaySteps = 0x01
    case TuesdaySteps = 0x02
    case WednesdaySteps = 0x03
    case ThursdaySteps = 0x04
    case FridaySteps = 0x05
    case SaturdaySteps = 0x06
    case SundaySteps = 0x07
    case CurrentTotalSteps = 0x09
    case RemainingBattery = 0x08
}

class WaveMessageResponse : NSObject {
    let code : WaveCommandResponseCodes
    let mode : WaveDeviceDataMode?
    let data : [NSObject]?
    init(code: WaveCommandResponseCodes, data: [NSObject]?, mode: WaveDeviceDataMode?) {
        self.code = code
        self.data = data
        self.mode = mode
    }
    
}

class WaveYMDHMSDOW : NSObject {
    let Year : Int
    let Month : Int
    let Day : Int
    let Hours : Int
    let Minutes : Int
    let Seconds : Int
    let DOW : Int
    init(Year: Int, Month: Int, Day: Int, Hours: Int, Minutes: Int, Seconds: Int, DOW: Int) {
        self.Year = Year
        self.Month = Month
        self.Day = Day
        self.Hours = Hours
        self.Minutes = Minutes
        self.Seconds = Seconds
        self.DOW = DOW
        
    }
}


//Unpacked timestamp along with steps
class WaveStep : NSObject {
    let start : NSDate
    let end : NSDate
    let steps : Int
    init (start: NSDate, end: NSDate, steps: Int) {
        self.start = start
        self.end = end
        self.steps = steps
    }
}



/* Utility and helper functions */



func waveYMDHMSDOWGMTToNSDate(dateIn: WaveYMDHMSDOW) -> NSDate {
    let startTimeComponents = NSDateComponents()
    let calendar = NSCalendar.currentCalendar()
    calendar.timeZone = NSTimeZone(forSecondsFromGMT: 0)
    startTimeComponents.setValue(dateIn.Day, forComponent: NSCalendarUnit.Day)
    startTimeComponents.setValue(2000+dateIn.Year, forComponent: NSCalendarUnit.Year)
    startTimeComponents.setValue(dateIn.Month, forComponent: NSCalendarUnit.Month)
    startTimeComponents.setValue(dateIn.Hours, forComponent: NSCalendarUnit.Hour)
    startTimeComponents.setValue(dateIn.Minutes, forComponent: NSCalendarUnit.Minute)
    startTimeComponents.setValue(dateIn.Seconds, forComponent: NSCalendarUnit.Second)
    let startTime : NSDate = calendar.dateFromComponents(startTimeComponents)!
    return startTime
}

func nSDateToWaveYMDHMSDOWGMT(dateIn: NSDate) -> WaveYMDHMSDOW {
    
    let calendar = NSCalendar.currentCalendar()
    calendar.timeZone = NSTimeZone(forSecondsFromGMT: 0)
    let day = calendar.component(NSCalendarUnit.Day, fromDate: dateIn)
    let month = calendar.component(NSCalendarUnit.Month, fromDate: dateIn)
    let year = calendar.component(NSCalendarUnit.Year, fromDate: dateIn)-2000
    var dow = calendar.component(NSCalendarUnit.Weekday, fromDate: dateIn)
    //adjust to Monday = 1 rather than Sunday = 1
    dow = dow - 1
    if (dow == 0) {
        dow = 7
    }
    let hours = calendar.component(NSCalendarUnit.Hour, fromDate: dateIn)
    let minutes = calendar.component(NSCalendarUnit.Minute, fromDate: dateIn)
    let seconds = calendar.component(NSCalendarUnit.Second, fromDate: dateIn)
    
    let time = WaveYMDHMSDOW(Year: year, Month: month, Day: day, Hours: hours, Minutes: minutes, Seconds: seconds, DOW: dow)
    return time
}

func checkYMDHMSDOW(Year: Int, Month: Int, Day: Int, Hours: Int? = nil, Minutes: Int? = nil, Seconds: Int? = nil, DOW: Int? = nil) -> Bool {
    if (Year < 0 || Year > 99 ||  Month < 1 || Month > 12 || Day < 1) {
        return false
    }
    
    //if any of HMSDOW are set all most be
    if (Hours != nil || Minutes != nil || Seconds != nil || DOW != nil) {
        if (Hours == nil || Minutes == nil || Seconds == nil || DOW == nil) {
            return false
        }
        if (Hours < 0 || Hours > 23 || Minutes < 0 || Minutes > 59 || Seconds < 0 || Seconds > 59 || DOW < 1 || DOW > 7) {
            return false
        }
        
    }
    
    //check for valid day in month
    if (Month == 1 || Month == 3 || Month == 5 || Month == 7 || Month == 8 || Month == 10 || Month == 12) {
        if (Day > 31) {
            return false;
        }
    } else if (Month == 2) {
        if ( (Year % 4) == 0 ) {
            //leap year, we are not going to worry about the 100's rule because that won't come up until after I am most likely dead.
            if (Day > 29) {
                return false
            }
        } else {
            if (Day > 28) {
                return false
            }
        }
    } else {
        if (Day > 30) {
            return false
        }
    }
    
    //if all conditions are satisfied, then this is a value YMDHMSDOW
    return true
    
}



