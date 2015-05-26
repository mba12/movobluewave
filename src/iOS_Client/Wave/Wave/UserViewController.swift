//
//  UserViewController.swift
//  Wave
//
//  Created by Phil Gandy on 5/11/15.
//

import Foundation
import UIKit
import CoreData

class UserViewController: UIViewController, UITableViewDelegate, UITableViewDataSource {
    
    
    @IBOutlet weak var userSelectionTable: UITableView!
    @IBAction func cancelButtonPress(sender: AnyObject) {
        dismissViewControllerAnimated(true, completion: nil)
    }
    
    var userList : [UserEntry]?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        userSelectionTable.tableFooterView = UIView(frame: CGRect.zeroRect)
        userSelectionTable.backgroundColor = UIColor.clearColor()
    }
    
    override func viewWillAppear(animated: Bool) {
        super.viewWillAppear(animated)
        userList = fetchUserList()

        dispatch_async(dispatch_get_main_queue(), {
            self.userSelectionTable.reloadData()
        })
        
    }
    
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        if let email = (userSelectionTable.cellForRowAtIndexPath(indexPath) as! UserTableCell?)!.userNameLabel.text {
            println("Selected User: "+email)
            
            if (email == "Add Account") {
                //then we need to handle this as an account add...
                //this is simply a segue to the register user VC
                
                performSegueWithIdentifier("AddAccount", sender: self)
                
            } else {
                //anything else, we would want to log in, name is the email address
                if (login(email)) {
                    println("Switching to user: "+email)
                    dismissViewControllerAnimated(true, completion: nil)
                }
                
            }
        }
        
        
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        if let cell = userSelectionTable.dequeueReusableCellWithIdentifier("UserTableCell") as? UserTableCell
        {
            if let users = userList {
                
                if (indexPath.row < users.count) {
                    cell.userNameLabel.text = users[indexPath.row].email
                } else {
                    cell.userNameLabel.text = "Add Account"
                }
            } else {
                cell.userNameLabel.text = "Add Account"
            }
            cell.backgroundColor = UIColor.clearColor()
            return cell
        }
        return UITableViewCell()
        
    }
    
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        
        if let count = userList?.count {
            //all items in userList + Add Account
            return count+1
        }
        return 1
    }
    
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }
    
    
    func tableView(tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        return nil
    }
    
    

    
}

class UserTableCell : UITableViewCell {
    
    @IBOutlet weak var userNameLabel: UILabel!
    
}