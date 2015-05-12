//
//  MyProfileViewController.swift
//  Wave
//
//  Created by Phil Gandy on 5/11/15.
//  Copyright (c) 2015 Phil Gandy. All rights reserved.
//

import Foundation
import UIKit

class MyProfileViewController:  UIViewController{
    @IBOutlet weak var cancel: UIButton!
    
    override func viewDidLoad() {
  	  super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
    }
    
    
    
    
    
    @IBAction func cancel(sender: UIButton){
        dismissViewControllerAnimated(true, completion: nil)

    }
    
 
    
    
}

