//
//  DataModel.swift
//  iVirtualCrit
//
//  Created by aaronep on 1/28/18.
//  Copyright © 2018 aaronep. All rights reserved.
//

import Foundation


struct current {
    static var currentHR: Int = 0
    static var currentScore: Double = 0
    static var currentSpeed: Double = 0
    static var currentCadence: Double = 0
    
    static var totalDistance: Double = 0
    static var segmentDistance: Double = 0
    static var totalMovingTime: Double = 0
    static var totalAverageSpeed: Double = 0
}

struct geo {
    static var status: String = "ON"
    static var elapsedTime: Double = 0
    static var distance: Double = 0
    static var speed: Double = 0
    static var pace: String = "0"
    static var direction: String = "X"
    static var avgSpeed: Double = 0
    static var avgPace: String = "0"
}


struct system {
    static var status: String = "STOPPED"
    static var startTime: Date?
    static var stopTime: Date?
    static var actualElapsedTime: Double = 0
}



struct rounds {
    //static var geoDistancesPerRound = [Double]()
    //static var btDistancesPerRound = [Double]()

    static var speeds = [Double]()
    static var geoSpeeds = [Double]()
    static var btSpeeds = [Double]()
    static var heartrates = [Double]()
    static var cadences = [Double]()
    static var scores = [Double]()
}
