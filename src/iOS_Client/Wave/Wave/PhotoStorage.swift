//
//  PhotoStorage.swift
//  Wave
//
//  Created by Phil Gandy on 5/15/15.
//

import Foundation
import CoreData

class PhotoStorage: NSManagedObject {

    @NSManaged var date: NSNumber
    @NSManaged var user: String
    @NSManaged var photoblob: NSData

}
