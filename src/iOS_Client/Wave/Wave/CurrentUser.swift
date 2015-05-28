//
//  UserEntry.swift
//  Wave
//
//  Created by Phil Gandy on 5/22/15.
//

import Foundation

import CoreData


class CurrentUser: NSManagedObject {
    
    @NSManaged var id: Int16
    @NSManaged var user: UserEntry?
    
    
}
