//
//  SyncCompleteViewController.swift
//  Wave
//
//  Created by Rudy Yukich on 5/20/15.
//
//

import Foundation
import UIKit

class SyncCompleteViewController : UIViewController {
    
    @IBOutlet weak var ackButton: UIButton!
    var syncStatusVC : SyncStatusViewController?
    
    @IBAction func ackButtonPress(sender: AnyObject) {
        syncStatusVC?.uploadDataVC?.complete(sender)
        performSegueWithIdentifier("UploadComplete", sender: self)
    }
    
}