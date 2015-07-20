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
    
    var syncStatusVC : SyncStatusViewController?
    
    @IBAction func ackButtonPress(sender: AnyObject) {
        syncStatusVC?.uploadDataVC?.complete(sender)
        performSegueWithIdentifier("UploadComplete", sender: self)
        if let application = (UIApplication.sharedApplication().delegate as? AppDelegate) {
            if let tabbarVC = application.tabBarController {
                println("setting selected index")
                tabbarVC.selectedIndex = 1
            }
        }
    }
    
    @IBAction func retryButtonPress(sender: AnyObject) {
        syncStatusVC?.uploadDataVC?.complete(sender)
        performSegueWithIdentifier("Upload Data", sender: self)
    }
    
   }