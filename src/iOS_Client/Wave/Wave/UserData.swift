//
//  UserData.swift
//  Wave
//
//  Created by Phil Gandy on 5/17/15.
//

import Foundation

private let _UserData = UserData()

class UserData {
    
    static let sharedInstance = UserData()
    
    
    
}


func getAValue() -> Int{
    return 1
}