//
//  MenuViewController.swift
//  Wave
//
//  Created by Phil Gandy on 5/8/15.
//  Copyright (c) 2015 Phil Gandy. All rights reserved.
//

import Foundation

import UIKit


class MenuViewController: UIViewController, UITableViewDelegate, UITableViewDataSource {
    
    @IBOutlet weak var tableView: UITableView!
    
    //var calendar = [NSCalendar .currentCalendar()]
    
    
    let textCellIdentifier = "TextCell"
    
    let menu = ["My Profile","Upload Data","Users","FAQ","Contact","Logout"]
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        tableView.delegate = self
        tableView.dataSource = self
    }
    
    // MARK:  UITextFieldDelegate Methods
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }
    
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return menu.count
    }
    
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCellWithIdentifier(textCellIdentifier, forIndexPath: indexPath) as! UITableViewCell
        
        let row = indexPath.row
        cell.textLabel?.text = menu[row]
        
        
        
        
        return cell
    }
    
    // MARK:  UITableViewDelegate Methods
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        tableView.deselectRowAtIndexPath(indexPath, animated: true)
        
        let row = indexPath.row
        
        performSegueWithIdentifier(menu[row], sender:self)
        
        
        //        dismissViewControllerAnimated(true, completion: nil)
        
        println(menu[row])
    }
    
    let segueIdentifier = "users"
    
    // MARK: - Navigation
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        if segue.identifier == segueIdentifier {
            if let destination = segue.destinationViewController as? MenuViewController {
                if let blogIndex = tableView.indexPathForSelectedRow()?.row {
                    //destination.blogName = menu[blogIndex]
                }
            }
        }
    }
    
    
    
}
