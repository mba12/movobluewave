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
    @NSManaged var starttime: String
    @NSManaged var endtime: String
    @NSManaged var user: String
    @NSManaged var status: String
    @NSManaged var guid: String

}
