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
        tableView.backgroundColor = UIColor.clearColor()
        
        if let var fbUserRef:String = UserData.getOrCreateUserData().getCurrentUserRef() as String?{
            
            if(fbUserRef=="Error"){
                NSLog("No user logged in, logging in as stored user")
                //                let predicate = NSPredicate(format:"%@ >= starttime AND %@ <= endtime AND %@ == user", dateStop, dateStart,UserData.getOrCreateUserData().getCurrentUID())
                
                let fetchRequest = NSFetchRequest(entityName: "UserEntry")
                //                fetchRequest.predicate = predicate
                if let fetchResults = self.managedObjectContext!.executeFetchRequest(fetchRequest, error: nil) as? [UserEntry] {
                    
                    if(fetchResults.count > 0){
                        UserData.getOrCreateUserData().loadUser(fetchResults[0])
                    }
                    
                    let FB = Firebase(url: UserData.getFirebase())
                    FB.authUser(UserData.getOrCreateUserData().getCurrentEmail(), password: UserData.getOrCreateUserData().getCurrentPW(),
                        withCompletionBlock: { error, authData in
                            
                            if error != nil {
//WARNING: Send user back to signin/create user screen!
                                self.performSegueWithIdentifier("Logout", sender: self)
                                // There was an error logging in to this account
                                NSLog("Login failed")
                            } else {
                                // We are now logged in
                                NSLog("We logged in as %@: %@",UserData.getOrCreateUserData().getCurrentEmail(), authData.uid)
                                
                            }
                    })
                    
                    
                }else{
                    //then redirect the user to the signin / create page
                    
                    performSegueWithIdentifier("Logout", sender: self)
                    /*
                    let email = "rudy@sensorstar.com"
                    let FB = Firebase(url: UserData.getFirebase())
                    FB.authUser(email, password: "pub386",
                        withCompletionBlock: { error, authData in
                            
                            if error != nil {
                                // There was an error logging in to this account
                                NSLog("Login failed")
//WARNING: Send user back to signin screen!
                            } else {
                                // We are now logged in
                                NSLog("We logged in as %@: %@",email, authData.uid)
                                //                                    self.userID = authData.uid
                                var ref = "https://ss-movo-wave-v2.firebaseio.com"
                                ref = ref + "/users/"
                                ref = ref + authData.uid
                                UserData.getOrCreateUserData().createUser(authData.uid, email: "rudy@sensorstar.com", pw: "pub386", birth: NSDate(), heightfeet: 0, heightinches: 0, weightlbs: 0, gender: "Male", fullName: "Rudy Yukich", user: "ryukich", ref: ref)
                                
                                //                            self.retrieveData()
                                
                                
                            }
                    })

                    */
                    
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
        
        if (menu[row] != "FAQ" && menu[row] != "Contact") {
            performSegueWithIdentifier(menu[row], sender:self)
        } else if (menu[row] == "FAQ") {
            UIApplication.sharedApplication().openURL(NSURL(string: "http://www.getmovo.com/appfaq")!)
            
        } else if (menu[row] == "Contact") {
            var username = ""
            if let curUsername : String = UserData.getOrCreateUserData().currentUsername {
                username = curUsername
            }
            var urlstring = "subject=Contact from "+username
            urlstring = "mailto:info@getmovo.com?"+urlstring.stringByAddingPercentEncodingWithAllowedCharacters(.URLPathAllowedCharacterSet())!
            UIApplication.sharedApplication().openURL(NSURL(string: urlstring)!)
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
