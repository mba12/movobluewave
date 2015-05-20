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
        tableView.tableFooterView = UIView(frame: CGRect.zeroRect)
        
        if let var fbUserRef:String = UserData.getOrCreateUserData().getCurrentUserRef() as String?{
            
            if(fbUserRef=="Error"){
                NSLog("No user logged in, logging in as stored user")
                //                let predicate = NSPredicate(format:"%@ >= starttime AND %@ <= endtime AND %@ == user", dateStop, dateStart,UserData.getOrCreateUserData().getCurrentUID())
                
                let fetchRequest = NSFetchRequest(entityName: "UserEntry")
                //                fetchRequest.predicate = predicate
                if let fetchResults = self.managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [UserEntry] {
                    
                    if(fetchResults.count > 0){
                        UserData.getOrCreateUserData().createUser(
                            String: fetchResults[0].id,
                            String: fetchResults[0].email,
                            String: fetchResults[0].pw,
                            NSDate: fetchResults[0].birthdate,
                            Int: Int(fetchResults[0].heightfeet),
                            Int: Int(fetchResults[0].heightinches),
                            Int: Int(fetchResults[0].weight),
                            String: fetchResults[0].gender,
                            String: fetchResults[0].fullname,
                            String: fetchResults[0].username,
                            String: fetchResults[0].reference)
                        //                        UserData.getOrCreateUserData().createUser(String: fetchResults[0].id, String: fetchResults[0].email, String: fetchResults[0].pw, NSDate:fetchResults[0].birthdate, Int: fetchResults[0].heightinches, Int: 0, Int: 0, String: "Male", String: "Phil Gandy", String: "pgandy", String: "")
                        
                        //                        CreateUser(String uid:String, String email:String, String pw:String, NSDate birth:NSDate, Int height1:Int, Int height2:Int, Int weight:Int, String gender:String, String fullName:String, String user:String, String ref:String){
                        
                    }else{
                        let ref = Firebase(url: "https://ss-movo-wave-v2.firebaseio.com")
                        ref.authUser("9@9.com", password: "9",
                            withCompletionBlock: { error, authData in
                                
                                if error != nil {
                                    // There was an error logging in to this account
                                    NSLog("Login failed")
                                } else {
                                    // We are now logged in
                                    NSLog("We logged in as 9: %@",authData.uid)
                                    //                                    self.userID = authData.uid
                                    var ref = "https://ss-movo-wave-v2.firebaseio.com"
                                    ref = ref + "/users/"
                                    ref = ref + authData.uid
                                    UserData.getOrCreateUserData().createUser(String: authData.uid, String: "9@9.com", String: "9", NSDate: NSDate(), Int: 0, Int: 0, Int: 0, String: "Male", String: "Phil Gandy", String: "pgandy", String: ref)
                                    
                                    //                            self.retrieveData()
                                    
                                    
                                }
                        })
                        
                    }
                } else {
                    
                }
                
                
                
                
                
                
            } else {
                //                retrieveData()
                
            }
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
        let cell = tableView.dequeueReusableCellWithIdentifier(textCellIdentifier, forIndexPath: indexPath) as! UITableViewCell
        
        let row = indexPath.row
        cell.textLabel?.text = menu[row]
        cell.backgroundColor = UIColor.clearColor()
        
        
        
        return cell
    }
    
    // MARK:  UITableViewDelegate Methods
    func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        tableView.deselectRowAtIndexPath(indexPath, animated: true)
        
        let row = indexPath.row
        
        if (menu[row] != "FAQ" && menu[row] != "Contact" && menu[row] != "Users") {
            performSegueWithIdentifier(menu[row], sender:self)
        }
        
        
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
