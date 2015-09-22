//
//  MenuViewController.swift
//  Wave
//
//  Created by Phil Gandy on 5/8/15.
//

import Foundation
import CoreData
import UIKit


class MenuViewController: UIViewController, UITableViewDelegate, UITableViewDataSource {
    
    @IBOutlet weak var tableView: UITableView!
    
    //var calendar = [NSCalendar .currentCalendar()]
    
    
    let textCellIdentifier = "TextCell"
    
    let menu = ["My Profile","Upload Data","Users","FAQ","Contact","Logout"]
    let managedObjectContext = (UIApplication.sharedApplication().delegate as! AppDelegate).managedObjectContext

    override func viewDidLoad() {
        super.viewDidLoad()
        
        tableView.delegate = self
        tableView.dataSource = self
        tableView.tableFooterView = UIView(frame: CGRect.zero)
        tableView.backgroundColor = UIColor.clearColor()
        
        
        if (UserData.getOrCreateUserData().getCurrentEmail() == nil) {
            //no current user
            
            NSLog("No usable CurrentUser!")
            performSegueWithIdentifier("Logout", sender: self)
            
            
        } else {

            
            
        }
   
        
    }
    
    override func viewDidAppear(animated: Bool) {
        
        if (UserData.getOrCreateUserData().getCurrentEmail() == nil) {
            //no current user
            
            NSLog("No usable CurrentUser!")
            performSegueWithIdentifier("Logout", sender: self)
            
            
        }
    }
    
    // MARK:  UITextFieldDelegate Methods
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }
    
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return menu.count
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCellWithIdentifier(textCellIdentifier, forIndexPath: indexPath) 
        
        let row = indexPath.row
        cell.textLabel?.text = menu[row]
        cell.backgroundColor = UIColor.clearColor()
        
        
        
        return cell
    }
    
    // MARK:  UITableViewDelegate Methods
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        tableView.deselectRowAtIndexPath(indexPath, animated: true)
        
        let row = indexPath.row
        
        if (menu[row] != "FAQ" && menu[row] != "Contact") {
            performSegueWithIdentifier(menu[row], sender:self)
        } else if (menu[row] == "FAQ") {
            UIApplication.sharedApplication().openURL(NSURL(string: "http://www.getmovo.com/appfaqbt")!)
            
        } else if (menu[row] == "Contact") {
            var username = ""
            if let curUsername : String = UserData.getOrCreateUserData().getCurrentUserName() {
                username = curUsername
            }
            var urlstring = "subject=Contact from "+username
            urlstring = "mailto:info@getmovo.com?"+urlstring.stringByAddingPercentEncodingWithAllowedCharacters(.URLPathAllowedCharacterSet())!
            UIApplication.sharedApplication().openURL(NSURL(string: urlstring)!)
        }
        //        dismissViewControllerAnimated(true, completion: nil)
        
        print(menu[row])
    }
    
    let segueIdentifier = "users"
    
    // MARK: - Navigation
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        if segue.identifier == segueIdentifier {
            if let destination = segue.destinationViewController as? MenuViewController {
                if let blogIndex = tableView.indexPathForSelectedRow?.row {
                    //destination.blogName = menu[blogIndex]
                }
            }
        }
    }
    
    
    
}
