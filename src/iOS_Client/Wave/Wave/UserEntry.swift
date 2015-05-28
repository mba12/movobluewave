//
//  UserEntry.swift
//  Wave
//
//  Created by Phil Gandy on 5/18/15.
//

import Foundation

import CoreData

class UserEntry: NSManagedObject {
    @NSManaged var birthdate: NSDate?
    @NSManaged var email: String
    @NSManaged var fullname: String?
    @NSManaged var gender: String?
    @NSManaged var heightfeet: Int16
    @NSManaged var heightinches: Int16
    @NSManaged var id: String?
    @NSManaged var pw: String
    @NSManaged var reference: String?
    @NSManaged var username: String?
    @NSManaged var weight: Int16
    @NSManaged var isCurrent : CurrentUser?
}
