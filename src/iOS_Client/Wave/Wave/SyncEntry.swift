//
//  SyncEntry.swift
//  Wave
//
//  Created by Phil Gandy on 5/15/15.
//

import Foundation
import CoreData

class SyncEntry: NSManagedObject {

    @NSManaged var syncid: String
    @NSManaged var starttime: NSDate
    @NSManaged var endtime: NSDate
    @NSManaged var user: String
    @NSManaged var status: String
    @NSManaged var guid: String

}
