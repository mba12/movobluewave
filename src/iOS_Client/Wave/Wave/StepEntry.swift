//
//  StepEntry.swift
//  Wave
//
//  Created by Phil Gandy on 5/15/15.
//

import Foundation
import CoreData

@objc(StepEntry)
class StepEntry: NSManagedObject {

    @NSManaged var syncid: String
    @NSManaged var starttime: NSDate
    @NSManaged var endtime: NSDate
    @NSManaged var user: String
    @NSManaged var count: Int16
    @NSManaged var ispushed: String
    @NSManaged var serialnumber: String
    @NSManaged var workout_type: String
    @NSManaged var guid: String

}
