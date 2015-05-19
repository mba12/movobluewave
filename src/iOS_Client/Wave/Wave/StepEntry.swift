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
    @NSManaged var starttime: String
    @NSManaged var endtime: String
    @NSManaged var user: String
    @NSManaged var count: String
    @NSManaged var ispushed: String
    @NSManaged var deviceid: String
    @NSManaged var workout_type: String
    @NSManaged var guid: String

}
