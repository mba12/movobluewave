//
//  SyncStatusViewController.swift
//  Wave
//
//  Created by Rudy Yukich on 5/20/15.


import Foundation
import UIKit

class SyncStatusViewController : UIViewController {
    var uploadDataVC : UploadDataViewController?
    var syncCompleteVC : SyncCompleteViewController?
    @IBOutlet weak var syncStatusLabel: UILabel!
    @IBOutlet weak var syncStatusProgress: UIProgressView!
    
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        if (segue.identifier == "SyncComplete") {
            syncCompleteVC = segue.destinationViewController as? SyncCompleteViewController
            syncCompleteVC?.syncStatusVC = self
        }
    }
    
    @IBAction func cancelButtonPress(sender: AnyObject) {
        //canceling a sync in progress
        //should mean that we call our parents complete to reset the devices connected
        uploadDataVC?.complete(sender)
        self.navigationController?.popViewControllerAnimated(true)
    }
    
}