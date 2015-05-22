//
//  StepEntry.swift
//  Wave
//
//  Created by Phil Gandy on 5/15/15.
//

import Foundation
import CoreData

class StepEntry: NSManagedObject {
    @NSManaged var count: Int16
    @NSManaged var endtime: NSDate
    @NSManaged var guid: String
    @NSManaged var ispushed: String
    @NSManaged var serialnumber: String
    @NSManaged var starttime: NSDate
    @NSManaged var syncid: String
    @NSManaged var user: String
    @NSManaged var workout_type: String
}
