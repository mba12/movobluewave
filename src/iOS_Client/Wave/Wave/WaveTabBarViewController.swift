//
//  WaveTabBarViewController.swift
//  Wave
//
//  Created by Rudy Yukich on 5/21/15.
//
//

import Foundation
import UIKit

class WaveTabBarViewController : UITabBarController {
    override func viewDidLoad() {
        super.viewDidLoad()
        (UIApplication.sharedApplication().delegate as! AppDelegate).tabBarController = self
        
        if (UserData.getOrCreateUserData().getCurrentEmail() == nil) {
            //no current user
            
            NSLog("No usable CurrentUser!")
            performSegueWithIdentifier("Logout", sender: self)
            
            
        } else {
            

            self.selectedIndex = 1

        }
        
        
    }
    
    override func viewWillAppear(animated: Bool) {
        super.viewWillAppear(animated)
        (UIApplication.sharedApplication().delegate as! AppDelegate).tabBarController = self        
    }
    
}
