//
//  UserData.swift
//  Wave
//
//  Created by Phil Gandy on 5/17/15.
//

import Foundation
import CoreData
import UIKit

private var _UserData:UserData? = nil


extension String {
    func toDouble() -> Double? {
        return NSNumberFormatter().numberFromString(self)?.doubleValue
    }
}

protocol UserMetaDataDelegate {
    func refreshedMetadata()
}

class UserData {
    //vars
    private var currentUserEntry : UserEntry?
    
    
    //static let currentFireBaseRef:String = "https://ss-movo-wave-v2.firebaseio.com/"
    static let currentFireBaseRef:String = "https://movowave.firebaseio.com/"
    static var delegate : UserMetaDataDelegate? = nil
    
    private init(){
        //init vars
        
        if (loadDefaultUser()) {
            NSLog("Success loading default user")

            
        } else {
            NSLog("Failed to load default user")
        }

        
    }
    
    static func getOrCreateUserData() -> UserData{
        if (_UserData==nil){
            _UserData = UserData()
            checkAuth()
        }
        return _UserData!
    }
    
    static func disposeUserData(){
        _UserData = nil
        
    }
    
    static func getFirebase()->String{
        return currentFireBaseRef
    }
    
    //WARNING: height should be in a single unit!! - RY
    //it is a bad idea to be tracking height across two units (i.e. use feet (double) or meters (double) or inches (int/double) or cm (int or double) but do not split, it will just cause headaches
    //it would be much better, additionally, if we used SI units under the hood... (i.e. meters, kilograms), but imperial units if we must.
    
    
    func loadDefaultUser() -> Bool {
        
        if let currentUser = UserData.getOrCreateCurrentUser() {
            return loadUser(currentUser)
        }
        
        return false
        
    }
    
    func loadUser(user: CurrentUser) -> Bool {
        //unwrap the userentry from the current user pointer!
        if let userentry = user.user {
            return loadUser(userentry)
        }
        
        return false
    }
    
    
    func loadUser(user: UserEntry) -> Bool {
        
        currentUserEntry = user
        if let DBCurrentUser : CurrentUser = UserData.getOrCreateCurrentUser() {
            DBCurrentUser.user = user
            UserData.saveContext()
            
            //anytime we login, download the metadata for changes
            downloadMetaData()
            return true
        }
        
        return false
    }
    
    static func getOrCreateCurrentUser() -> CurrentUser? {
        var createNewCurrentUser = false
        let fetchRequest = NSFetchRequest(entityName: "CurrentUser")
        if let fetchResults = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [CurrentUser] {
            if (fetchResults.count == 1) {
                return fetchResults[0]
            } else if (fetchResults.count > 1) {
                var toRtn = fetchResults[0]
                clearExcessItems(fetchResults)
                return toRtn
            } else {
                createNewCurrentUser = true
            }
            
        } else {
            //then
            createNewCurrentUser = true
        }
        
        if (createNewCurrentUser) {
            if let newCurrentUser : CurrentUser = NSEntityDescription.insertNewObjectForEntityForName("CurrentUser", inManagedObjectContext: (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!) as? CurrentUser {
                
                UserData.saveContext()
                return newCurrentUser
            }
            
        }
        
        //safety catchall, should not be reached unless there is something wrong with the managed context
        return nil
    }
    
    static func saveContext() {
        (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.save(nil)
    }
    
    func createUser(email:String, pw:String, uid:String?, birth:NSDate?, heightfeet:Int?, heightinches:Int?, weightlbs:Int?, gender:String?, fullName:String?, user:String?, ref:String) -> UserEntry {
        
        
        let appDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
        let managedContext = appDelegate.managedObjectContext
        var newItem = NSEntityDescription.insertNewObjectForEntityForName("UserEntry", inManagedObjectContext: appDelegate.managedObjectContext!) as! UserEntry
        
        newItem.id = uid
        newItem.email = email
        newItem.pw = pw
        newItem.birthdate = birth
        if let hf = heightfeet {
            newItem.heightfeet = Int16(hf)
        }
        if let hi = heightinches {
            newItem.heightinches = Int16(hi)
        }
        if let w = weightlbs {
            newItem.weight = Int16(w)
        }
        newItem.gender = gender
        newItem.fullname = fullName
        newItem.username = user
        newItem.reference = ref
        
        appDelegate.managedObjectContext!.save(nil)
        return newItem
        
    }

    
    func downloadMetaData(){
        //NSLog("Downloading new metadata")
        var metaRef = getCurrentUserRef()
        metaRef = metaRef! + "/metadata"
        
        var fbMeta = Firebase(url:metaRef)
        fbMeta.observeSingleEventOfType(.Value, withBlock: { snapshot in
//            var metaObjects = snapshot.children
            if let username = (snapshot.childSnapshotForPath("currentUsername").valueInExportFormat() as? String) {
                self.setCurrentUsername(username)
            }
            
            if let email = snapshot.childSnapshotForPath("currentEmail").valueInExportFormat() as? String {
                self.setCurrentEmail(email)
            }
            
            if let fullname = (snapshot.childSnapshotForPath("currentFullName").valueInExportFormat() as? String) {
                self.setCurrentFullName(fullname)
            }
            
            if let weight = (snapshot.childSnapshotForPath("currentWeight").valueInExportFormat() as? String) {
                if((weight) != "Error"){
                    var weightIn = (snapshot.childSnapshotForPath("currentWeight").valueInExportFormat() as? String)!
                    var weightInt = weightIn.toInt()
                    self.setCurrentWeight(weightInt!)
                }
            }
            if let heightft = snapshot.childSnapshotForPath("currentHeight1").valueInExportFormat() as? String {
                if(heightft != "Error"){
                    var height1In = (snapshot.childSnapshotForPath("currentHeight1").valueInExportFormat() as? String)!
                    var height1Int = height1In.toInt()
                    self.setCurrentHeightFeet(height1Int!)
                }
            }
            if let heightin = snapshot.childSnapshotForPath("currentHeight2").valueInExportFormat() as? String {
                if(heightin != "Error"){
                    var height2In = (snapshot.childSnapshotForPath("currentHeight2").valueInExportFormat() as? String)!
                    var height2Int = height2In.toInt()
                    self.setCurrentHeightInches(height2Int!)
                }
                
            }
            
            if let birthdatems = snapshot.childSnapshotForPath("currentBirthdate").valueInExportFormat() as? String {
                if (birthdatems != "Error") {
                    if let birthdatemsD = birthdatems.toDouble()
                    {
                        self.setCurrentBirthdate(NSDate(timeIntervalSince1970: birthdatemsD/1000.0))
                    }
                }
            }
          
            if let gender = (snapshot.childSnapshotForPath("currentGender").valueInExportFormat() as? String) {
                self.setCurrentGender(gender)
            }
            //birthday
            
            if let delegate = UserData.delegate {
                
                delegate.refreshedMetadata()
            }
            
            }, withCancelBlock: { error in
                println(error.description)
                
                
        })
        
    }
    
    
    func saveMetaDataToFirebase(){
        NSLog("Saving metadata to firebase")
        var stringRef = getCurrentUserRef()
        stringRef = stringRef! + "/metadata"
        var fbMetaRef:Firebase = Firebase(url: stringRef)
        if (getCurrentFullName() != nil) {
            fbMetaRef.childByAppendingPath("currentFullName").setValue(getCurrentFullName())
        } else {
            fbMetaRef.childByAppendingPath("currentFullName").setValue("Error")
        }
        //        fbMetaRef.childByAppendingPath("currentBirthdate").setValue(String(getCurrentBirthdate()))
        if (getCurrentEmail() != nil) {
            fbMetaRef.childByAppendingPath("currentEmail").setValue(getCurrentEmail())
        } else {
            fbMetaRef.childByAppendingPath("currentEmail").setValue("Error")
        }
        if (getCurrentGender() != nil) {
            fbMetaRef.childByAppendingPath("currentGender").setValue(getCurrentGender())
        }else {
            fbMetaRef.childByAppendingPath("currentGender").setValue("Error")
        }
        if (getCurrentHeightFeet() != nil) {
            fbMetaRef.childByAppendingPath("currentHeight1").setValue(getCurrentHeightFeet()?.description)
        } else {
            fbMetaRef.childByAppendingPath("currentHeight1").setValue("Error")
        }
        if (getCurrentHeightInches() != nil) {
            fbMetaRef.childByAppendingPath("currentHeight2").setValue(getCurrentHeightInches()?.description)
        } else {
            fbMetaRef.childByAppendingPath("currentHeight2").setValue("Error")
        }
        
        if let birthdate = getCurrentBirthdate() {
            
            //
                var date : Double = birthdate.timeIntervalSince1970 as Double
                date = date * 1000.0
                let formatter = NSNumberFormatter()
                formatter.maximumFractionDigits = 0
                var bdString = formatter.stringFromNumber(date)
                fbMetaRef.childByAppendingPath("currentBirthdate").setValue(bdString)
        } else {
            fbMetaRef.childByAppendingPath("currentBirthdate").setValue("Error")
        }
        
        fbMetaRef.childByAppendingPath("currentUsername").setValue(getCurrentUserName())
        
        if (getCurrentWeight() != nil) {
            fbMetaRef.childByAppendingPath("currentWeight").setValue(getCurrentWeight()?.description)
        } else {
            fbMetaRef.childByAppendingPath("currentWeight").setValue("Error")
        }
        
        
        fbMetaRef.childByAppendingPath("currentUID").setValue(getCurrentUID())
        
    }
    

    
    
    func getCurrentUID() -> String? {
        if let UID = currentUserEntry?.id {
            
            return UID
        }
        return nil
    }
    
    func setCurrentUID(uid:String) {
        if let cue : UserEntry = currentUserEntry {
            cue.id = uid
            UserData.saveContext()
        }
    }
    
    
    
    
    
    
    
    
    
    
    func getCurrentEmail() -> String? {
        if let email = currentUserEntry?.email {
            return email
        }
        return nil
    }
    func setCurrentEmail(email:String) {
        if let cue : UserEntry = currentUserEntry {
            cue.email = email
            UserData.saveContext()
        }
    }
    func getCurrentPW() -> String? {
        if let pw = currentUserEntry?.pw {
            return pw
        }
        return nil
        
    }
    func setCurrentPW(password:String){
        if let cue : UserEntry = currentUserEntry {
            cue.pw = password
            UserData.saveContext()
        }
    }
    //date object
    func getCurrentBirthdate() -> NSDate? {
        if let bd = currentUserEntry?.birthdate {
            return bd
        }
        return nil
        
    }
    func setCurrentBirthdate(birthDate:NSDate){
        if let cue : UserEntry = currentUserEntry {
            cue.birthdate = birthDate
            UserData.saveContext()
        }
    }
    
    func getCurrentHeightFeet() -> Int? {
        if let hf = currentUserEntry?.heightfeet {
            return Int(hf)
        }
        return nil
        
    }
    func setCurrentHeightFeet(heightfeet:Int) {
        if let cue : UserEntry = currentUserEntry {
            cue.heightfeet = Int16(heightfeet)
            UserData.saveContext()
        }
    }
    func getCurrentHeightInches() -> Int? {
        if let hi = currentUserEntry?.heightinches {
            return Int(hi)
        }
        return nil
        
    }
    func setCurrentHeightInches(heightinches:Int){
        if let cue : UserEntry = currentUserEntry {
            cue.heightinches = Int16(heightinches)
            UserData.saveContext()
        }
    }
    func getCurrentWeight() -> Int? {
        if let wlbs = currentUserEntry?.weight {
            return Int(wlbs)
        }
        return nil
        
    }
    func setCurrentWeight(weightlbs:Int){
        if let cue : UserEntry = currentUserEntry {
            cue.weight = Int16(weightlbs)
            UserData.saveContext()
        }
    }
    func getCurrentGender() -> String? {
        if let g = currentUserEntry?.gender {
            return g
        }
        return nil
        
    }
    func setCurrentGender(gender:String){
        if let cue : UserEntry = currentUserEntry {
            cue.gender = gender
            UserData.saveContext()
        }
    }
    func getCurrentFullName() -> String? {
        if let fn = currentUserEntry?.fullname {
            return fn
        }
        return nil
        
    }
    func setCurrentFullName(name:String){
        if let cue : UserEntry = currentUserEntry {
            cue.fullname = name
            UserData.saveContext()
        }
    }
    
    func getCurrentUserName() -> String? {
        if let un = currentUserEntry?.username {
            return un
        }
        return nil
        
    }
    func setCurrentUsername(username:String){
        if let cue : UserEntry = currentUserEntry {
            cue.username = username
            UserData.saveContext()
        }
    }
    func getCurrentUserRef() -> String? {
        if let ref = currentUserEntry?.reference {
            return ref
        }
        return nil
    }
    func setCurrentUserRef(ref:String){
        if let cue : UserEntry = currentUserEntry {
            cue.reference = ref
            UserData.saveContext()
        }
    }
    
    func getCurrentUserPhoto() -> UIImage? {
        //stub function
        return nil
    }
    
    
    //passing in a nil date results in getting/setting the profile picture
    static func getImageForDate(date: NSDate?, callbackDelegate: ImageUpdateDelegate, thumbnail: Bool) {
        loadImageFromFile(date, callbackDelegate: callbackDelegate, thumbNail: thumbnail)
        shouldDownloadNewImage(date, callbackDelegate: callbackDelegate, thumbNail: thumbnail)
    }
    
    

    
    static func buildFBPictureDownloadFromDate(date: NSDate?) -> (fbpath:String, storageDate:NSDate?) {
        
        var fbDownloadRef = UserData.getOrCreateUserData().getCurrentUserRef()
        fbDownloadRef = fbDownloadRef! + "/photos/"
        
        var storageDate : NSDate!
        if let downloadDate = date {
            var cal = NSCalendar.currentCalendar()
            
            var todayDate = cal.component(.CalendarUnitDay , fromDate: downloadDate)
            var todayMonth = cal.component(.CalendarUnitMonth , fromDate: downloadDate)
            var todayYear = cal.component(.CalendarUnitYear , fromDate: downloadDate)
            
            var month = ""
            var day = ""
            if(todayMonth<10){
                month = "0" + (String(todayMonth))
            }else{
                month = String(todayMonth)
            }
            if(todayDate<10){
                day = "0" + (String(todayDate))
            }else{
                day = String(todayDate)
            }
            fbDownloadRef = fbDownloadRef! + String(todayYear) + "/" + month + "/" + day
            storageDate = YMDGMTToNSDate(todayYear, todayMonth, todayDate)
        } else {
            fbDownloadRef = fbDownloadRef! + "profilepic"
            storageDate = YMDGMTToNSDate(1970, 1, 1)
        }
        
        
        return (fbDownloadRef!, storageDate)
    }
    
    static func uploadPhotoToFirebase(base64StringIn:String, date:NSDate?){
        var base64String = base64StringIn
        
        var md5Sum = base64String.md5()
        
        
        
        var cal = NSCalendar.currentCalendar()
        
        
        var (fbUploadRef, storageDate) = buildFBPictureDownloadFromDate(date)
        
        
        
        var firebaseImage:Firebase = Firebase(url:fbUploadRef)
        
        firebaseImage.setValue(nil)
        var size = (base64String as NSString).length
        var totalChunks = (size / photoMaximumSizeChunk) + ( (size%photoMaximumSizeChunk != 0) ? 1:0)
        firebaseImage.updateChildValues(["0":String(totalChunks)])
        
        
        var part = 2
        while (!base64String.isEmpty) {
            var size = (base64String as NSString).length
            var index = advance(base64String.startIndex, ( ( size > photoMaximumSizeChunk) ? photoMaximumSizeChunk:size ))
            var result:String = base64String.substringToIndex(index)
            let range = base64String.startIndex..<index
            base64String.removeRange(range)
            
            firebaseImage.updateChildValues([String(part):result])
            part += 1
        }
        firebaseImage.updateChildValues(["1":md5Sum])

        println("Upload complete")
        
    }
    

    //pass in a nil date for profile picutre
    static func shouldDownloadNewImage(date:NSDate?, callbackDelegate: ImageUpdateDelegate?, thumbNail : Bool){
        let suffix = (thumbNail) ? ".thumb.jpg":""
        
        var (fbDownloadRef, storageDate) = buildFBPictureDownloadFromDate(date)
        fbDownloadRef = fbDownloadRef + "/1"
        var firebaseImage:Firebase = Firebase(url:fbDownloadRef)

        if let storeDate = storageDate {
            
            firebaseImage.observeSingleEventOfType(.Value, withBlock: { snapshot in
                //check MD5 and act on it
                //NSLog("returned")
                if let downloadedMD5 = snapshot.value as? String{
                    
                    if let uid = UserData.getOrCreateUserData().getCurrentUID() {
                        let predicate = NSPredicate(format:"%@ == user AND %@ == date", uid, storeDate)
                        
                        let fetchRequest = NSFetchRequest(entityName: "PhotoStorage")
                        fetchRequest.predicate = predicate
                        if let fetchResults = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [PhotoStorage] {
                            if(fetchResults.count > 0){
                                //then we must delete the old image and replace
                                if (fetchResults.count == 1) {
                                    
                                    if let md5 = fetchResults[0].md5 {
                                        if (md5 == downloadedMD5) {
                                            //don't download new
                                            /* should have been already loaded
                                            if let delegate = callbackDelegate{                                                            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0), {
                                                    delegate.updatedImage(date, newImage: UIImage(contentsOfFile: UserData.getDocumentsPath()+fetchResults[0].photopath+suffix))
                                                })
                                                return
                                            }
                                            */
                                            return
                                        } else {
                                            //download replacement images
                                            UserData.downloadPhotoFromFirebase(date, callbackDelegate: callbackDelegate)
                                        }
                                        
                                    } else{
                                        //download newer image
                                        UserData.downloadPhotoFromFirebase(date, callbackDelegate: callbackDelegate)
                                        return
                                    }
                                    
                                } else {
                                    //error case
                                    println("Warning: Excess Images")
                                    if let md5 = fetchResults[0].md5 {
                                        if (md5 == downloadedMD5) {
                                            //don't download new
                                            /* should have already been loaded
                                            if let delegate = callbackDelegate{
                                                
                                                dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0), {
                                                    delegate.updatedImage(date, newImage: UIImage(contentsOfFile: UserData.getDocumentsPath()+fetchResults[0].photopath+suffix))
                                                })
                                                return
                                                
                                            } */
                                            return
                                        } else {
                                            //download replacement images
                                            UserData.downloadPhotoFromFirebase(date, callbackDelegate: callbackDelegate)
                                        }
                                        
                                        
                                    }else{
                                        //download newer image
                                        UserData.downloadPhotoFromFirebase(date, callbackDelegate: callbackDelegate)
                                        return
                                    }
                                    
                                    
                                }
                            } else{
                                UserData.downloadPhotoFromFirebase(date, callbackDelegate: callbackDelegate)
                                return
                            }
                        }
                    }
                    
                }else{
                    
                    if let uid = UserData.getOrCreateUserData().getCurrentUID() {
                        let predicate = NSPredicate(format:"%@ == user AND %@ == date", uid, storeDate)
                        
                        let fetchRequest = NSFetchRequest(entityName: "PhotoStorage")
                        fetchRequest.predicate = predicate
                        if let fetchResults = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [PhotoStorage] {
                            if(fetchResults.count > 0){
                                //then we must delete the old image and replace
                                if (fetchResults.count == 1) {
                                    /*
                                    if let delegate = callbackDelegate{
                                        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0), {
                                            delegate.updatedImage(date, newImage: UIImage(contentsOfFile: UserData.getDocumentsPath()+fetchResults[0].photopath+suffix))
                                            })
                                        
                                        return
                                    }
                                    
                                    */
                                    return
                                }else{
                                    //warn
                                    println("Warning: Excess Images")
                                    /*
                                    if let delegate = callbackDelegate{
                                        
                                        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0), {
                                            delegate.updatedImage(date, newImage: UIImage(contentsOfFile: UserData.getDocumentsPath()+fetchResults[0].photopath+suffix))
                                            })
                                        return
                                    }
                                    */
                                    return
                                }
                                
                                
                            }
                        }
                    }
                    
                }
                if let delegate = callbackDelegate{
                    //delegate.updatedImage(date, newImage: nil)
                }
                
                }, withCancelBlock: { error in
                    
                    println(error.description)
                    //continue loading image from file
                    if let uid = UserData.getOrCreateUserData().getCurrentUID() {
                        let predicate = NSPredicate(format:"%@ == user AND %@ == date", uid, storeDate)
                        
                        let fetchRequest = NSFetchRequest(entityName: "PhotoStorage")
                        fetchRequest.predicate = predicate
                        
                        
                        if let fetchResults = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [PhotoStorage] {
                            if(fetchResults.count > 0){
                                //then we must delete the old image and replace
                                if (fetchResults.count == 1) {
                                    /*
                                    if let delegate = callbackDelegate{
                                        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0), {
                                            delegate.updatedImage(date, newImage: UIImage(contentsOfFile: UserData.getDocumentsPath()+fetchResults[0].photopath))
                                        })
                                        return
                                    }
                                    
                                    */
                                    
                                } else {
                                    //continue loading image from file
                                    println("Warning: Excess Images")
                                    
                                    //don't download new
                                    /*
                                    if let delegate = callbackDelegate{
                                        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0), {
                                            delegate.updatedImage(date, newImage: UIImage(contentsOfFile: UserData.getDocumentsPath()+fetchResults[0].photopath))
                                        })
                                        return
                                        
                                        
                                        
                                    }
                                    */
                                }
                            }
                            
                        }
                        
                    }
                    
                    if let delegate = callbackDelegate{
                        //delegate.updatedImage(date, newImage: nil)
                    }
                    
            })
            
        }
        
        
    }

    

    static func downloadPhotoFromFirebase(date:NSDate?, callbackDelegate: ImageUpdateDelegate?){

        var (fbDownloadRef, storageDate) = buildFBPictureDownloadFromDate(date)
        var firebaseImage:Firebase = Firebase(url:fbDownloadRef)
        firebaseImage.observeSingleEventOfType(.Value, withBlock: { snapshot in

            var encodedData : String?
            if let numberOfImageBlobs = (snapshot.childSnapshotForPath("0").valueInExportFormat() as? String) {
                if(numberOfImageBlobs=="1"){
                    if let rawData = snapshot.childSnapshotForPath("2").valueInExportFormat() as? String{
                        encodedData = rawData
                        //decodedData = NSData(base64EncodedString: rawData, options: nil)
                        
                        
                    }
                  
      
                }else{
                    var count = numberOfImageBlobs.toInt()
                    var rawData = ""
                    //i = 2 to skip the first and second nodes, as they're metadata.
                    for(var i = 0; i < count; i++){
                        if let curData = snapshot.childSnapshotForPath(String(i+2)).valueInExportFormat() as? String {
                            rawData = rawData + curData
                        }

                    }
                    encodedData = rawData
                    //decodedData = NSData(base64EncodedString: rawData, options: nil)

                    
                }
                
            }

            if let data : String = encodedData {
                if (data.lengthOfBytesUsingEncoding(NSUTF8StringEncoding) > 0) {
                    UserData.storeImage(nil, rawData: data, date: date, pushToFirebase: false, callbackDelegate: callbackDelegate)
                    return
                }
                
            }

            if let delegate = callbackDelegate {
                //delegate.updatedImage(date, newImage: nil)
            }
            
            
            
            
        })
        

        
    }
    
    static func getDocumentsPath() -> String {
        let documentsPath = NSSearchPathForDirectoriesInDomains(.DocumentDirectory, .UserDomainMask, true)[0] as! String
        return documentsPath
    }
    
    static func getOrCreateDirectoryForImages() -> (full: String, relative: String) {

        let fullPhotoPath = UserData.getDocumentsPath()+"/Photos/"
        let relativePath = "/Photos/"
        if (NSFileManager.defaultManager().createDirectoryAtPath(fullPhotoPath, withIntermediateDirectories: true, attributes: nil, error: nil)) {
            println("Photo dir created")
        }
        
        return (fullPhotoPath, relativePath)
    }
    
    
    static func removeOldImage(oldPhoto: PhotoStorage) {
        NSFileManager.defaultManager().removeItemAtPath(oldPhoto.photopath, error: nil)
    }
    
    
    
    static func storeImage(image: UIImage?, rawData: String?, date: NSDate?,  pushToFirebase : Bool, callbackDelegate: ImageUpdateDelegate?) {
        
        
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0),  {
        //get system path to our image store
        var (fullPath, relativePath) = self.getOrCreateDirectoryForImages()
        
        //this is the store function so we will be saving the image to a GUID
        var uuid = NSUUID().UUIDString
        
        var dataN: NSData?
        var dataThumb: NSData?
        var base64StringN : String?
        
        
        if let localImage = image {
            dataN = UIImageJPEGRepresentation(localImage, 1.0)
            dataThumb = UIImageJPEGRepresentation(localImage, 0.5)
            if let imagedata = dataN {
                base64StringN = imagedata.base64EncodedStringWithOptions(.allZeros)
            }
        } else if let unwrappedB64 = rawData {
            base64StringN = unwrappedB64
            dataN = NSData(base64EncodedString: unwrappedB64, options: .allZeros)
            if let extractData = dataN {
                if let tempImage = UIImage(data: extractData) {
                    dataThumb = UIImageJPEGRepresentation(tempImage, 0.5)
                }
            }
        }
        
        let imagename = uuid + ".jpg"
        let thumbname = imagename + ".thumb.jpg"
        let filepath = fullPath + imagename
        let thumbfilepath = fullPath + thumbname
        
        let shortfilepath = relativePath + imagename
        var insertUpdateItem : PhotoStorage?
        
        //We want to store the image to the date
        //to be timezone independent
        //this means that we use the GMT NSDate for storage that corresponds to
        //the beginning of the day
        var storageDate : NSDate?
        if let unwrapDate = date {
            var cal = NSCalendar.currentCalendar()
            var thisDate = cal.component(.CalendarUnitDay , fromDate: unwrapDate)
            var thisMonth = cal.component(.CalendarUnitMonth , fromDate: unwrapDate)
            var thisYear = cal.component(.CalendarUnitYear , fromDate: unwrapDate)
            storageDate = YMDGMTToNSDate(thisYear, thisMonth, thisDate)
        } else {
            storageDate = YMDGMTToNSDate(1970, 1, 1)
        }
        if let data = dataN {
            if let base64String = base64StringN {
                
                if let storeDate = storageDate {
                    
                    
                    if let uid = UserData.getOrCreateUserData().getCurrentUID() {
                        let predicate = NSPredicate(format:"%@ == user AND %@ == date", uid, storeDate)
                        
                        let fetchRequest = NSFetchRequest(entityName: "PhotoStorage")
                        fetchRequest.predicate = predicate
                        
                        if let fetchResults = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [PhotoStorage] {
                            if(fetchResults.count > 0){
                                //then we must delete the old image and replace
                                if (fetchResults.count == 1) {
                                    insertUpdateItem = fetchResults[0]
                                    self.removeOldImage(fetchResults[0])
                                    
                                    
                                } else {
                                    //error case
                                    println("Warning: Excess Images")
                                    insertUpdateItem = fetchResults[0]
                                    self.removeOldImage(fetchResults[0])
                                }
                                
                                
                            } else {
                                
                                insertUpdateItem = NSEntityDescription.insertNewObjectForEntityForName("PhotoStorage", inManagedObjectContext: (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!) as? PhotoStorage
                            }
                            
                            
                            
                            if let newItem = insertUpdateItem {
                                
                                
                                newItem.photopath = shortfilepath
                                newItem.date = storeDate
                                newItem.user = uid
                                //lets go ahead and store the file now
                                //and update the local display
                                data.writeToFile(filepath, atomically: false)
                                dataThumb?.writeToFile(thumbfilepath, atomically: false)
                                
                                if let delegate = callbackDelegate {
                                    if let realImage = image {
                                        delegate.updatedImage(date, newImage: realImage)
                                    } else if let realImage = UIImage(data: data) {
                                        delegate.updatedImage(date, newImage: realImage)
                                    }
                                }
                                let base64String = data.base64EncodedStringWithOptions(.allZeros)
                                dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0),  {
                                    newItem.md5 = base64String.md5()
                                    UserData.saveContext()
                                    
                                    if (pushToFirebase) {
                                        //pass through the unadulterated date to uploadImage
                                        //which will do the same conversion
                                        
                                        let base64String = data.base64EncodedStringWithOptions(.allZeros)
                                        UserData.uploadPhotoToFirebase(base64String, date: date)
                                    }
                                })
                                /*
                                if let delegate = callbackDelegate {
                                delegate.updatedImage(date, newImage: image)
                                return
                                }*/
                                return
                                
                                
                                
                            } else {
                                println("failed to update image")
                            }
                        }
                    }
                }
            }
        }
        
        if let delegate = callbackDelegate {
            //delegate.updatedImage(date, newImage: nil)
        }
            
        })
    }
    
    static func loadImageFromFile(date: NSDate?, callbackDelegate: ImageUpdateDelegate?, thumbNail: Bool) {
        //We want to store the image to the date
        //to be timezone independent
        //this means that we use the GMT NSDate for storage that corresponds to
        //the beginning of the day
        let suffix = (thumbNail) ? ".thumb.jpg":""
        var storageDate : NSDate?
        if let unwrapDate = date {
            var cal = NSCalendar.currentCalendar()
            var thisDate = cal.component(.CalendarUnitDay , fromDate: unwrapDate)
            var thisMonth = cal.component(.CalendarUnitMonth , fromDate: unwrapDate)
            var thisYear = cal.component(.CalendarUnitYear , fromDate: unwrapDate)
            storageDate = YMDGMTToNSDate(thisYear, thisMonth, thisDate)
        } else {
            storageDate = YMDGMTToNSDate(1970, 1, 1)
        }
        if let storeDate : NSDate = storageDate {
            if let uid = UserData.getOrCreateUserData().getCurrentUID() {
                let predicate = NSPredicate(format:"%@ == user AND %@ == date", uid, storeDate)
                
                let fetchRequest = NSFetchRequest(entityName: "PhotoStorage")
                fetchRequest.predicate = predicate
                
                
                if let fetchResults = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [PhotoStorage] {
                    if(fetchResults.count > 0){
                        //then we must delete the old image and replace
                        if (fetchResults.count == 1) {
                            if let delegate = callbackDelegate{                                            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0), {
                                    delegate.updatedImage(date, newImage: UIImage(contentsOfFile: UserData.getDocumentsPath()+fetchResults[0].photopath+suffix))
                                })
                                return
                            }
                            
                        } else {
                            //continue loading image from file
                            println("Warning: Excess Images")
                            
                            //don't download new
                            if let delegate = callbackDelegate{                                            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0), {
                                    delegate.updatedImage(date, newImage: UIImage(contentsOfFile: UserData.getDocumentsPath()+fetchResults[0].photopath+suffix))
                                })
                                return
                            }
                            
                        }
                        
                    }
                }
                
            }
        }
        
        if let delegate = callbackDelegate{
            //delegate.updatedImage(date, newImage: nil)
        }
        
        
    }
    
}


extension String {
    func md5() -> String! {
        let str = self.cStringUsingEncoding(NSUTF8StringEncoding)
        let strLen = CUnsignedInt(self.lengthOfBytesUsingEncoding(NSUTF8StringEncoding))
        let digestLen = Int(CC_MD5_DIGEST_LENGTH)
        let result = UnsafeMutablePointer<CUnsignedChar>.alloc(digestLen)
        
        CC_MD5(str!, strLen, result)
        
        var hash = NSMutableString()
        for i in 0..<digestLen {
            hash.appendFormat("%02x", result[i])
        }
        
        result.destroy()
        
        return String(format: hash as String)
    }
}




protocol ImageUpdateDelegate {
    
    func updatedImage(date: NSDate?, newImage: UIImage?)
    
}
