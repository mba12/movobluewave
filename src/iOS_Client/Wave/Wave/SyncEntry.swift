//
//  SyncEntry.swift
//  Wave
//
//  Created by Phil Gandy on 5/15/15.
//

import Foundation
import CoreData

class SyncEntry: NSManagedObject {
    @NSManaged var endtime: NSDate
    @NSManaged var guid: String
    @NSManaged var starttime: NSDate
    @NSManaged var status: Bool
    @NSManaged var syncid: String
    @NSManaged var user: String
}
