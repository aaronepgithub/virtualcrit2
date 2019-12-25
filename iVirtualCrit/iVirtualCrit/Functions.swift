//
//  Functions.swift
//  iVirtualCrit
//
//  Created by aaronep on 1/28/18.
//  Copyright © 2018 aaronep. All rights reserved.
//

import Foundation

extension Array where Element: Numeric {
    /// Returns the total sum of all elements in the array
    var total: Element { return reduce(0, +) }
}

extension Array where Element: BinaryInteger {
    /// Returns the average of all elements in the array
    var average: Double {
        return isEmpty ? 0 : Double(Int(total)) / Double(count)
    }
}

extension Array where Element: FloatingPoint {
    /// Returns the average of all elements in the array
    var average: Element {
        return isEmpty ? 0 : total / Element(count)
    }
}

func getFormattedTime(d: Date) -> String {
    let currentDateTime = d
    let formatter = DateFormatter()
    formatter.timeStyle = .medium
    formatter.dateStyle = .none
    return formatter.string(from: currentDateTime as Date)
}

func getFormattedTimeAndDate(d: Date) -> String {
    let currentDateTime = d
    let formatter = DateFormatter()
    formatter.timeStyle = .medium
    formatter.dateStyle = .medium
    return formatter.string(from: currentDateTime as Date)
}

func createTimeString(seconds: Int)->String //"00:00:00"
{
    let h:Int = seconds / 3600
    let m:Int = (seconds/60) % 60
    let s:Int = seconds % 60
    let a = String(format: "%u:%02u:%02u", h,m,s)
    return a
}

func getTimeIntervalSince(d1: Date, d2: Date) -> Double {
    return d2.timeIntervalSince(d1 as Date!)
}

func getScoreFromHR(x: Double) -> Double {
    if x == 0 {
        return 0
    } else {
        let y = Double(maxHRvalue)
        let z = Double(100)
        return (x / y) * z
    }
}

func stringer(dbl: Double, len: Int) -> String {
    if dbl.isNaN == true || dbl.isInfinite == true  {
        return "0"
    } else {
        return String(format:"%.\(len)f", dbl)
    }
}

func calcMinPerMile(mph: Double) -> String {
    let a = (60 / mph)
    if a.isFinite == false {
        return "00:00"
    }
    
    let b = (a - Double(Int(a)))
    let c = b * 60
    
    let d = Int(a)
    let e = Int(c)
    if (e < 10) {
        return "\(d):0\(e)"
    } else {
        return "\(d):\(e)"
    }
}
