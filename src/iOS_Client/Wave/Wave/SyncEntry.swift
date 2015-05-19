//
//  SyncEntry.swift
//  Wave
//
//  Created by Phil Gandy on 5/15/15.
//  Copyright (c) 2015 Phil Gandy. All rights reserved.
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
