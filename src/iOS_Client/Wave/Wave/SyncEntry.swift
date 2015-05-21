//
//  SyncEntry.swift
//  Wave
//
//  Created by Phil Gandy on 5/15/15.
//

import Foundation
import CoreData

@objc(SyncEntry)
class SyncEntry: NSManagedObject {

    @NSManaged var syncid: String
    @NSManaged var starttime: NSDate
    @NSManaged var endtime: NSDate
    @NSManaged var user: String
    @NSManaged var status: Bool
    @NSManaged var guid: String

}
