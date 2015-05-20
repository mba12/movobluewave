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
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        if (segue.identifier == "SyncComplete") {
            syncCompleteVC = segue.destinationViewController as? SyncCompleteViewController
            syncCompleteVC?.syncStatusVC = self
        }
    }
}