//
//  KnownWaves.swift
//  Wave
//
//  Created by Phil Gandy on 5/15/15.
//

import Foundation
import CoreData

class KnownWaves: NSManagedObject {

    @NSManaged var deviceid: String
    @NSManaged var queried: NSNumber
    @NSManaged var serialnumber: String
    @NSManaged var user: String

}
