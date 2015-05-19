//
//  KnownWaves.swift
//  Wave
//
//  Created by Phil Gandy on 5/15/15.
//

import Foundation
import CoreData

class KnownWaves: NSManagedObject {

    @NSManaged var mac: String
    @NSManaged var queried: NSNumber
    @NSManaged var serial: String
    @NSManaged var user: String

}
