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
    }
    
    override func viewWillAppear(animated: Bool) {
        super.viewWillAppear(animated)
        (UIApplication.sharedApplication().delegate as! AppDelegate).tabBarController = self        
    }
    
}
