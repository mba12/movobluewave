//
//  UserMenuBarViewController.swift
//  Wave
//
//  Created by Rudy Yukich on 5/21/15.
//
//

import Foundation
import UIKit

class UserMenuBarViewController : UIViewController, UITabBarControllerDelegate {
    
    @IBOutlet weak var statsButton: UIButton!
    @IBOutlet weak var profilePictureButton: UIButton!
    @IBOutlet weak var userNameLabel: UILabel!

    
    override func viewDidLoad() {
    }
    
    override func viewWillAppear(animated: Bool) {
        super.viewWillAppear(animated)
        if let application = (UIApplication.sharedApplication().delegate as? AppDelegate) {
            if let tabbarVC = application.tabBarController {
                tabbarVC.delegate = self
            }
        }

        if let application = (UIApplication.sharedApplication().delegate as? AppDelegate) {
            if let tabBarController = application.tabBarController {
                if (tabBarController.selectedIndex == 1) {
                    dispatch_async(dispatch_get_main_queue(),  {
                        self.statsButton.setImage(UIImage(named: "statsbutton"), forState: UIControlState.Normal)
                        self.statsButton.setNeedsDisplay()
                    })
                    
                } else if (tabBarController.selectedIndex == 2) {
                    dispatch_async(dispatch_get_main_queue(),  {
                        self.statsButton.setImage(UIImage(named: "statsicon_selected"), forState: UIControlState.Normal)
                        self.statsButton.setNeedsDisplay()
                    })
                }
            }
        }
        
        if let username = UserData.getOrCreateUserData().getCurrentUserName() {
            dispatch_async(dispatch_get_main_queue(),  {
                self.userNameLabel.text = username
            })
        }
        
        if let profilePicture = UserData.getOrCreateUserData().getCurrentUserPhoto() {
            dispatch_async(dispatch_get_main_queue(),  {
                self.profilePictureButton.setImage(profilePicture, forState: UIControlState.Normal)
            })
            
        }
        
    }
    
    func tabBarController(tabBarController: UITabBarController, didSelectViewController viewController: UIViewController) {

    }
    
    @IBAction func statsButtonPress(sender: AnyObject) {
        //when the statsbutton is pressed
        if let application = (UIApplication.sharedApplication().delegate as? AppDelegate) {
            if let tabbarVC = application.tabBarController {
                println("Tab bar controller in " + String(tabbarVC.selectedIndex))
                if (tabbarVC.selectedIndex == 2) {
                    dispatch_async(dispatch_get_main_queue(),  {
                        tabbarVC.selectedIndex = 1
                    })
                } else if (tabbarVC.selectedIndex == 1) {
                    dispatch_async(dispatch_get_main_queue(),  {
                        tabbarVC.selectedIndex = 2
                    })
                }
                
            }
        }
    }
    
    
    
}