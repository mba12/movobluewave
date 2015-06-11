//
//  UserMenuBarViewController.swift
//  Wave
//
//  Created by Rudy Yukich on 5/21/15.
//
//

import Foundation
import UIKit

class UserMenuBarViewController : UIViewController, UITabBarControllerDelegate, ImageUpdateDelegate, UserMetaDataDelegate {
    
    @IBOutlet weak var statsButton: UIButton!
    @IBOutlet weak var profilePictureButton: UIButton!
    @IBOutlet weak var userNameLabel: UILabel!

    //hack to handle launch condition
    static var runOnce : Bool = false
    
    override func viewDidLoad() {
        if let username = UserData.getOrCreateUserData().getCurrentUserName() {
            dispatch_async(dispatch_get_main_queue(),  {
                self.userNameLabel.text = username
            })
        }
        UserData.delegate = self
        UserData.getImageForDate(nil, callbackDelegate: self)
        
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
                if (UserMenuBarViewController.runOnce) {
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
                } else {
                    UserMenuBarViewController.runOnce = true
                }
            }
        }
        
        if let username = UserData.getOrCreateUserData().getCurrentUserName() {
            dispatch_async(dispatch_get_main_queue(),  {
                self.userNameLabel.text = username
            })
        }
        
        UserData.getImageForDate(nil, callbackDelegate: self)
        
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
    
    
    func updatedImage(date: NSDate?, newImage: UIImage?) {
        var setImage = false
        if (date == nil) {
            if let image = newImage {
                //then we have a new profile image
                dispatch_async(dispatch_get_main_queue(),  {
                    self.profilePictureButton.setImage(image, forState: UIControlState.Normal)
                    self.profilePictureButton.layer.cornerRadius = self.profilePictureButton.frame.size.width / 2;
                    self.profilePictureButton.clipsToBounds = true;
                    self.profilePictureButton.imageView?.contentMode = UIViewContentMode.ScaleAspectFill
                })
                
            } else {
                //if image is nil
                dispatch_async(dispatch_get_main_queue(),  {
                    self.profilePictureButton.setImage(UIImage(named: "user_icon_cir"), forState: UIControlState.Normal)
                })
            }
            
        }
        
    }
    
    func refreshedMetadata() {
        if let username = UserData.getOrCreateUserData().getCurrentUserName() {
            dispatch_async(dispatch_get_main_queue(),  {
                self.userNameLabel.text = username
            })
        }
    }
    
}