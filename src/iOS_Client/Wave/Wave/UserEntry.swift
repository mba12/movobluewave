//
//  UserEntry.swift
//  Wave
//
//  Created by Phil Gandy on 5/18/15.
//  Copyright (c) 2015 Phil Gandy. All rights reserved.
//

import Foundation
import Foundation
import CoreData

@objc(UserEntry)
class UserEntry: NSManagedObject {
    
    @NSManaged var id: String
    @NSManaged var email: String
    @NSManaged var fullname: String
    @NSManaged var gender: String
    @NSManaged var pw: String
    @NSManaged var username: String
    @NSManaged var birthdate: NSDate
    @NSManaged var height1: Int
    @NSManaged var height2: Int
    @NSManaged var weight: Int
    
}
