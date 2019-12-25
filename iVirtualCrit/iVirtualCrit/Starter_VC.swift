//
//  Starter_VC.swift
//  iVirtualCrit
//
//  Created by aaronep on 1/28/18.
//  Copyright © 2018 aaronep. All rights reserved.
//

import UIKit
import CoreLocation
import CoreBluetooth
import AudioToolbox
import Firebase
import SystemConfiguration

extension String {
    var toDouble: Double {
        return Double(self) ?? 0.0
    }
}

var secondsInCurrentRound: Int = 0
var distanceAtStartOfRoundBT = Double(0)
var crankRotationsDuringRound: Double = 0

var firstTimer: Bool = true

//USED FOR VIEWER_VC
var arr = [String]()
var arrSend = [String]()

//USED FOR RESULTS_VC (UDARRAY IS USED FOR HISTORY)
var arrResults = [String]()
var arrResultsDetails = [String]()

var arrResultsMyRoundSpeed = [String]()
var arrResultsDetailsMyRoundSpeed = [String]()

var arrResultsRoundSpeed = [String]()
var arrResultsDetailsRoundSpeed = [String]()

var arrResultsRoundScore = [String]()
var arrResultsDetailsRoundScore = [String]()

var arrResultsTotalSpeed = [String]()
var arrResultsDetailsTotalSpeed = [String]()

var arrResultsTotalScore = [String]()
var arrResultsDetailsTotalScore = [String]()



var roundCadence: Double = 0

var la: Double = 0
var lo: Double = 0

class Starter_VC: UITableViewController {
    
    var secondsSinceStart = Double(0)
    var secondsInCurrentMile = Double(0)
    //MAKE ZERO AT ROUND END/MILE END

    
    var distanceAtStartOfRoundGEO = Double(0)
    var distanceAtStartOfMile = Double(0)
    var lastLocationTimeStamp: Date!
    
    var timer = Timer()
    var timerIntervalValue: Double = 1
    
    var inRoundGeoDistance: Double = 0
    var inRoundBtDistance: Double = 0
    var inRoundGeoSpeed: Double = 0
    var inRoundBtSpeed: Double = 0
    var roundSpeed: Double = 0 //best between bt and geo

    var inRoundHR = [Int]()
//    var inRoundCadence = [Int]()
    var roundHR: Double = 0
    var roundScore: Double = 0
//    var roundCadence: Double = 0
    
    
    var secondsPerRound: Int = 1800
    var roundGeoSpeed: Double = 0
    
    var roundsCompleted: Double = 0
    var currentRound: Int = 0
    var currentMile: Double = 1.0
    
    var bestRoundSpeed: Double = 0
    var bestRoundHR: Double = 0
    var bestRoundCadence: Double = 0
    var bestRoundScore: Double = 0
    var bestRoundPace: String = ""
    
    var longestDistance: Double = 0
    
    
    var speedForLastMile: Double = 0
    var paceForLastMile: Double = 0
    var fastestMile: Double = 0 //MPH
    var arrMileSpeeds = [Double]()
    
    
    @IBOutlet weak var statusValue: UILabel!
    @IBOutlet weak var totalTime: UILabel!
    @IBOutlet weak var gpsMovingTime: UILabel!
    @IBOutlet weak var gpsMovingPace: UILabel!
    @IBOutlet weak var gpsMovingSpeed: UILabel!
    @IBOutlet weak var gpsAverageSpeed: UILabel!
    @IBOutlet weak var gpsAvergagePace: UILabel!
    @IBOutlet weak var gpsDistance: UILabel!
    @IBOutlet weak var gpsDirection: UILabel!
    @IBOutlet weak var gpsRoundSpeed: UILabel!
    
    @IBOutlet weak var btHR: UILabel!
    @IBOutlet weak var btMovingSpeed: UILabel!
    @IBOutlet weak var btMovingCadence: UILabel!
    @IBOutlet weak var btScore: UILabel!
    @IBOutlet weak var btMovingTime: UILabel!
    @IBOutlet weak var btDistance: UILabel!
    @IBOutlet weak var btMovAvg: UILabel!
    @IBOutlet weak var gpsStatus: UILabel!
    @IBOutlet weak var tDisplayVal: UILabel!
    
    @IBOutlet weak var btSpdRnd: UILabel!
    @IBOutlet weak var btCadRnd: UILabel!
    @IBOutlet weak var btHrRnd: UILabel!
    @IBOutlet weak var btScoreRnd: UILabel!
    
    @IBOutlet weak var lbl_timeInRound: UILabel!
    @IBOutlet weak var fb_SpeedLeader: UILabel!
    
    //NEW LABELS FOR STARTER/STATUS
    @IBOutlet weak var st_timeInCurrentMile: UILabel!
    @IBOutlet weak var st_speedRoundBT: UILabel!
    @IBOutlet weak var st_speedRoundGPS: UILabel!
    
    
//    func processUD(st: String) {
//        udString = "\(secondsSinceStart):  \(st) \n"
//        if currentRound == 1 {
//         udArray = []
//        }
//        udArray.append(udString)
//        let defaults = UserDefaults.standard
//        defaults.set(udArray, forKey: "SavedStringArray")
//    }
    
    
    //EACH SECOND
    @objc func timerInterval() {
        system.actualElapsedTime = getTimeIntervalSince(d1: system.startTime!, d2: Date())
        secondsInCurrentMile += 1
        st_timeInCurrentMile.text = "\(createTimeString(seconds: Int(round(secondsInCurrentMile)))) IN MILE \(Int(currentMile))"
        secondsSinceStart = round(system.actualElapsedTime)
        let s1 = Int(secondsSinceStart)
        let s2 = currentRound * secondsPerRound
        secondsInCurrentRound = s1 - s2
        
//        print("secondsInCurrentRound:  \(secondsInCurrentRound)")
//        print("secondsSinceStart:  \(secondsSinceStart)")
        
        if secondsSinceStart == 295 {
            if firstTimer == true {
                self.tabBarController?.selectedIndex = 3
            }
        }
        
        //NEW ROUND IDENTIFIER
        //TODO:  TURN THIS OFF TO TEST WITHOUT ANY NEW ROUNDS...
        if secondsInCurrentRound >= secondsPerRound {
            print("\n STARTING ROUND \(currentRound + 1), ROUND \(currentRound) COMPLETE")
            roundsCompleted += 1
            currentRound += 1
            distanceAtStartOfRoundBT = current.totalDistance
            distanceAtStartOfRoundGEO = geo.distance
            crankRotationsDuringRound = 0
            inRoundHR = []
            usingBTforCadence = false
            usingBTforSpeed = false
            usingBTforHeartrate = false
            updateRound()
            return
        }
        
        
        totalTime.text = "\(createTimeString(seconds: Int(round(system.actualElapsedTime)))) TOTAL TIME"
        
        lbl_timeInRound.text = "(\(secondsInCurrentRound)):  RND# \(currentRound) MILE# \(stringer(dbl: currentMile, len: 0))"
        
//        print("each second:  \(createTimeString(seconds: Int(round(system.actualElapsedTime))))")
//        print("each second:  \(system.actualElapsedTime)")
//        print("current.currentSpeed: \(current.currentSpeed)")
//        print("current.totalDistance: \(current.totalDistance)")
//        print("current.totalMovingTime: \(current.totalMovingTime)")
//        print("current.totalAverageSpeed: \(current.totalAverageSpeed)")
//        print("current.currentCadence: \(current.currentCadence)")
//        let str: String = "\(current.currentHR),\(current.currentScore)"
//        print("HR/Score:  \(str)")
//        print("geo.speed \(geo.speed)")
//        print("geo.elapsedTime \(geo.elapsedTime)")
//        print("geo.distance \(geo.distance)")
//        print("geo.pace \(geo.pace)")
//        print("geo.avgSpeed \(geo.avgSpeed)")
//        print("\n")
        
        btHR.text = "\(current.currentHR) BPM (HR)"
        btMovingSpeed.text = "\(stringer(dbl: current.currentSpeed, len: 1)) MPH(BT)"
        btMovingCadence.text = "\(stringer(dbl: current.currentCadence, len: 0)) RPM(BT)"
        btScore.text = "\(stringer(dbl: current.currentScore, len: 1)) %MAX"
        btDistance.text = "\(stringer(dbl: current.totalDistance, len: 2)) MI(BT)"
        btMovingTime.text = "\(createTimeString(seconds: Int(current.totalMovingTime))) MOVING(BT)"
        btMovAvg.text = "\(stringer(dbl: current.totalAverageSpeed, len: 1)) AVG SPD(BT)"
        
//        print("seconds since start:  \(secondsSinceStart)")
//        print("secondsInRound:  \(secondsInCurrentRound)")
//        print("currentRound:  \(currentRound)")
        
        //CALC ROUND SPEEDS
        if current.totalDistance > 0 {
            inRoundBtDistance = current.totalDistance - distanceAtStartOfRoundBT
            if inRoundBtDistance > 0.01 && secondsInCurrentRound > 5 {
                inRoundBtSpeed = rndSpdBT
                btSpdRnd.text = "\(stringer(dbl: inRoundBtSpeed, len: 1)) RND SPD"
                st_speedRoundBT.text = "\(stringer(dbl: inRoundBtSpeed, len: 1)) MPH(BT) ROUND \(currentRound)"
            } else {inRoundBtSpeed = 0}
        }
        
        if geo.distance > 0 {
            inRoundGeoDistance = geo.distance - distanceAtStartOfRoundGEO
            if inRoundGeoDistance > 0.1 && Double(secondsInCurrentRound) > 10 {
                inRoundGeoSpeed = inRoundGeoDistance / (Double(secondsInCurrentRound) / 60.0 / 60.0)
                gpsRoundSpeed.text = "\(stringer(dbl: inRoundGeoSpeed, len: 1)) RND SPD(G)"
                st_speedRoundGPS.text = "\(stringer(dbl: inRoundGeoSpeed, len: 1)) MPH(GPS) ROUND \(currentRound)"
            } else {inRoundGeoSpeed = 0}
        }

        //CALC ROUND HR/SCORE BEFORE ROUND ENDS
        if current.currentHR > 0 {
            inRoundHR.append(Int(current.currentHR))
            if inRoundHR.count > 2 {
                roundHR = inRoundHR.average
                roundScore = getScoreFromHR(x: roundHR)
                btHrRnd.text = "\(stringer(dbl: roundHR, len: 1)) RND HR"
                btScoreRnd.text = "\(stringer(dbl: roundScore, len: 1)) % RND SCORE"
            }
        }
        
        //CALC ROUND CAD BEFORE ROUND ENDS
        roundCadence = rndCadBT
        if roundCadence > 0 && roundCadence.isNaN == false {
            btCadRnd.text = "\(stringer(dbl: roundCadence, len: 1)) RND CAD"
        } else {
            roundCadence = 0
        }

        //TEST FOR NEW MILE
        if current.totalDistance > currentMile || geo.distance > currentMile {
            print("NEW MILE, MILE \(currentMile) COMPLETED")
            currentMile += 1
            updateMile()
        }
        if current.totalDistance > geo.distance {
            longestDistance = current.totalDistance
        } else {
            longestDistance = geo.distance
        }
        
        //CREATE DATA FOR VIEWER_VC
        createViewerArray()
    }
    //END SECOND TIMER
    

    //MARK:  UPDATEROUND at ROUND COMPLETE
    func updateRound() {
        //print("UPDATE ROUND")
        rounds.btSpeeds.append(inRoundBtSpeed)
        rounds.geoSpeeds.append(inRoundGeoSpeed)
        rounds.heartrates.append(roundHR)
        rounds.scores.append(getScoreFromHR(x: roundHR))
        rounds.cadences.append(roundCadence)
        
        roundSpeed = rounds.btSpeeds.last!
        if rounds.geoSpeeds.last! > roundSpeed {roundSpeed = rounds.geoSpeeds.last!}
        let roundPace = calcMinPerMile(mph: roundSpeed)
        rounds.speeds.append(roundSpeed)
        
        let arrRoundSpeedsSorted = rounds.speeds.sorted { $0 > $1 }
        let ix = arrRoundSpeedsSorted.index(of: roundSpeed)
        var indexOfLastRoundSpeed = 0
        if ix != nil {
            indexOfLastRoundSpeed = (ix ?? 10000) + 1
            print("indexOfLastRoundSpeed  \(indexOfLastRoundSpeed)")
            print("total Rounds:  \(rounds.speeds.count)")
            if indexOfLastRoundSpeed < 10000 && rounds.speeds.last! > 1.0 {
                roundIndexSentance = "  Your last round's speed ranked number \(indexOfLastRoundSpeed) out of \(rounds.speeds.count).  "
                roundIndexString1 = "SPEED RANK: \(indexOfLastRoundSpeed) OF \(rounds.speeds.count)"
            }
        }
        
        let arrRoundHeartratesSorted = rounds.heartrates.sorted { $0 > $1 }
        let hix = arrRoundHeartratesSorted.index(of: roundHR)
        var indexOfLastRoundHeartrate = 0
        if hix != nil {
            indexOfLastRoundHeartrate = (hix ?? 10000) + 1
            print("indexOfLastRoundHeartrate  \(indexOfLastRoundHeartrate)")
            print("total Rounds:  \(rounds.heartrates.count)")
            if indexOfLastRoundHeartrate < 10000 && rounds.heartrates.last! > 1.0 {
                roundIndexSentance += "  Your last round's score ranked number \(indexOfLastRoundHeartrate) out of \(rounds.heartrates.count).  "
                roundIndexString2 = "SCORE RANK: \(indexOfLastRoundHeartrate) OF \(rounds.heartrates.count)"
            }
        }

        
        
        
        
        //RESULTS DATA
        let a = "ROUND \(stringer(dbl: roundsCompleted, len: 0)) "
        let b = "\(stringer(dbl: rounds.heartrates.last!, len: 1)) HR"
        let c = "\(stringer(dbl: rounds.scores.last!, len: 1)) % MAX"
        let d = "\(stringer(dbl: rounds.speeds.last!, len: 2))  MPH"
//        let d = "\(stringer(dbl: rounds.btSpeeds.last!, len: 2))  MPH"
        let e = "\(stringer(dbl: rounds.cadences.last!, len: 1)) RPM"
        //let f = "\(stringer(dbl: rounds.geoSpeeds.last!, len: 2))  GEO"

//        arrResults.append("\(a) \(b), \(c)")
//        arrResultsDetails.append("\(d), \(e), \(f)")

        arrResultsMyRoundSpeed.append("  \(b),   \(c)")
        arrResultsDetailsMyRoundSpeed.append("  \(d)    \(e)")
        

        //ROUNDCOMPLETE POINT
        newRoundPoint(mileString: "\(a) COMPLETE\n\n\(d)\n\(b)\n\(roundPace) PACE\n\(e)\n\n\(roundIndexString1)\n\(roundIndexString2)")
        calcBestRoundMetrics()
        
//        print("\n")
//        print("End of Round for HR, Spd, Cad, GeoSpd")
//        dump(rounds.speeds)
//        dump(rounds.heartrates)
//        dump(rounds.scores)
//        dump(rounds.cadences)
//        dump(rounds.btSpeeds)
//        dump(rounds.geoSpeeds)
//        print("\n")
        
        geo.speed = 0.0
        geo.pace = "00:00"
        
        print("calling fbPushII")
        fbPushII()
    }
    
    var roundIndexSentance: String = ""
    var roundIndexString1: String = ""
    var roundIndexString2: String = ""
    
    // CALC BESTROUND METRICS
    func calcBestRoundMetrics() {
        print("CALC BEST ROUND METRICS")
        if roundSpeed > bestRoundSpeed {
            bestRoundSpeed = roundSpeed
            bestRoundPace = calcMinPerMile(mph: roundSpeed)
            if bestRoundSpeed == 0 {bestRoundPace = "00:00"}
            
            if audioStatus == "ON" {
                
                if roundHR > bestRoundHR {
                    print("fastest round and highest score")
                        Utils.shared.say(sentence: "That was your fastest round and your highest score. \(stringer(dbl: roundSpeed, len: 1)) MPH.  Your pace was \(calcMinPerMile(mph: roundSpeed)) PER MILE")
                } else {
                    print("fastest round")
                        Utils.shared.say(sentence: "That was your fastest round. \(stringer(dbl: roundSpeed, len: 1)) MPH.  Your pace was \(calcMinPerMile(mph: roundSpeed)) PER MILE")
                }
            }
        } else {
            print(roundIndexSentance)
            
                if audioStatus == "ON" {Utils.shared.say(sentence: "Round Complete. \(stringer(dbl: roundSpeed, len: 1)) MPH.  Your pace was \(calcMinPerMile(mph: roundSpeed)) PER MILE.  \(roundIndexSentance)")}
            roundIndexSentance = ""
        }
        if roundCadence > bestRoundCadence {bestRoundCadence = roundCadence}
        if roundHR > bestRoundHR {
            bestRoundHR = roundHR
            bestRoundScore = getScoreFromHR(x: bestRoundHR)
        }
        
        
        //MY BEST ROUNDS POINT
        let when = DispatchTime.now() + 240
        DispatchQueue.main.asyncAfter(deadline: when){
            self.newBestRoundPoint(mileString: "MY BEST ROUNDS\n\n\(stringer(dbl: self.bestRoundSpeed, len: 2)) SPEED\n\(stringer(dbl: self.bestRoundCadence, len: 1)) CADENCE\n\(stringer(dbl: self.bestRoundHR, len: 1)) HR\n\(stringer(dbl: self.bestRoundScore, len: 1)) SCORE\n\(self.bestRoundPace) PACE.\n\n LEADER IS \(self.currentSpeedLeaderName).\n\(stringer(dbl: self.currentSpeedLeaderSpeed, len: 2))")
            
            //print("\nMY BEST ROUNDS\n\(stringer(dbl: self.bestRoundSpeed, len: 1)) SPEED\n\(stringer(dbl: self.bestRoundCadence, len: 1)) CADENCE\n\(stringer(dbl: self.bestRoundHR, len: 1)) HR\n\(stringer(dbl: self.bestRoundScore, len: 1)) SCORE\n\(self.bestRoundPace) PACE\n")
            
            udArray.append("\(getFormattedTimeAndDate(d: Date()))\nMY BEST ROUNDS\n\(stringer(dbl: self.bestRoundSpeed, len: 2)) SPEED\n\(stringer(dbl: self.bestRoundCadence, len: 1)) CADENCE\n\(stringer(dbl: self.bestRoundHR, len: 1)) HR\n\(stringer(dbl: self.bestRoundScore, len: 1)) SCORE\n\(self.bestRoundPace) PACE\n")
        }
    }
    
    //MARK:  UPDATEMILE
    func updateMile() {
        speedForLastMile = 1.0 / (secondsInCurrentMile / 60.0 / 60.0)
        secondsInCurrentMile = 0
        
        //print("SPD FOR LAST MILE: \(speedForLastMile)")
        //print("PACE FOR LAST MILE:  \(calcMinPerMile(mph: speedForLastMile))")
        
        arrMileSpeeds.append(speedForLastMile)
        //arrMileSpeeds, fastest to slowest
        arrMileSpeeds = arrMileSpeeds.sorted { $0 > $1 }
        let ix = arrMileSpeeds.index(of: speedForLastMile)
        var indexOfLastMileSpeed = 0
        if ix != nil {
            indexOfLastMileSpeed = (ix ?? 100) + 1
        }
        print("indexOfLastMileSpeed  \(indexOfLastMileSpeed)")
        
        if speedForLastMile > fastestMile {
            fastestMile = speedForLastMile
            if audioStatus == "ON" {
                Utils.shared.say(sentence: "Fastest Mile is now  \(stringer(dbl: fastestMile, len: 1)) Miles Per Hour.  A Pace of \(calcMinPerMile(mph: fastestMile)) PER MILE")
                print("Fastest Mile is now  \(stringer(dbl: fastestMile, len: 1)) Miles Per Hour.  A Pace of \(calcMinPerMile(mph: fastestMile)) PER MILE")
            }
        } else {
            if audioStatus == "ON" {
//                Utils.shared.say(sentence: "Sorry, not your best mile.  The fastest is still \(stringer(dbl: fastestMile, len: 1)) Miles Per Hour.  A Pace of \(calcMinPerMile(mph: fastestMile)) PER MILE.  Your last mile ranked number \(indexOfLastMileSpeed) out of \(arrMileSpeeds.count).")

                Utils.shared.say(sentence: "Sorry, not your best mile. Your last mile ranked number \(indexOfLastMileSpeed) out of \(arrMileSpeeds.count).")
                
                
                print("Sorry, not your best mile.  The fastest is still \(stringer(dbl: fastestMile, len: 1)) Miles Per Hour.  A Pace of \(calcMinPerMile(mph: fastestMile)) PER MILE.  Your last mile ranked number \(indexOfLastMileSpeed) out of \(arrMileSpeeds.count).")
            }
        }

        newMilePoint(mileString: "\(stringer(dbl: (currentMile - 1), len: 0)) MILES COMPLETE\n\(stringer(dbl: speedForLastMile, len: 2)) MPH\n\(calcMinPerMile(mph: speedForLastMile)) PACE\nRANKING \(indexOfLastMileSpeed) OF \(arrMileSpeeds.count)\n\n\(stringer(dbl: fastestMile, len: 2)) FASTEST MILE\n\(calcMinPerMile(mph: fastestMile)) FASTEST PACE")
        
        print("\(stringer(dbl: (currentMile - 1), len: 0)) MILES COMPLETE\n\(stringer(dbl: speedForLastMile, len: 2)) MPH\n\(calcMinPerMile(mph: speedForLastMile)) PACE\nRANKING \(indexOfLastMileSpeed) OF \(arrMileSpeeds.count)\n\n\(stringer(dbl: fastestMile, len: 2)) FASTEST MILE\n\(calcMinPerMile(mph: fastestMile)) FASTEST PACE")
        
        udArray.append("\(getFormattedTimeAndDate(d: Date()))\n\(stringer(dbl: (currentMile - 1), len: 0)) MILES COMPLETE\n\(stringer(dbl: speedForLastMile, len: 2)) MPH\n\(calcMinPerMile(mph: speedForLastMile)) PACE\nRANKING \(indexOfLastMileSpeed) OF \(arrMileSpeeds.count)\n\n\(stringer(dbl: fastestMile, len: 2)) FASTEST MILE\n\(calcMinPerMile(mph: fastestMile)) FASTEST PACE")
        
        distanceAtStartOfMile = 0
    }
    //END UPDATEMILE
    
    func newMilePoint(mileString: String) {
        NotificationCenter.default.post(name: NSNotification.Name("tlUpdate"), object: nil, userInfo: ["title": "\(mileString)", "color": "green"])
    }
    
    func newRoundPoint(mileString: String) {
        NotificationCenter.default.post(name: NSNotification.Name("tlUpdate"), object: nil, userInfo: ["title": "\(mileString)", "color": "yellow"])
    }
    
    func newBestRoundPoint(mileString: String) {
        NotificationCenter.default.post(name: NSNotification.Name("tlUpdate"), object: nil, userInfo: ["title": "\(mileString)", "color": "blue"])
    }
    
    func newBestSpeedsPoint(mileString: String) {
        NotificationCenter.default.post(name: NSNotification.Name("tlUpdate"), object: nil, userInfo: ["title": "\(mileString)", "color": "blue"])
    }
    
    func stopAndSave() {
        //save
        //REMOVE OLD HISTORY
        //udArray = []
        //udArray.append(udString)
//        let defaults = UserDefaults.standard
//        defaults.set(udArray, forKey: "SavedStringArray")
    }
    
    func presentHistory() {
        dump(udArray)
    }
    
    func createViewerArray() {
        //HDR
        if activityType == "RUN" || activityType == "ROW" || current.totalMovingTime == 0 || usingBTforSpeed == false {
            arr.append("\(createTimeString(seconds: Int(geo.elapsedTime)))  \(stringer(dbl: geo.avgSpeed, len: 1)) AVG")
        } else {
            arr.append("\(createTimeString(seconds: Int(current.totalMovingTime)))  \(stringer(dbl: current.totalAverageSpeed, len: 1)) AVG")
        }
        
        //3 VALS
        arr.append("\(stringer(dbl: Double(current.currentHR), len: 0))")
        tabBarController?.tabBar.items?[0].badgeValue = "\(stringer(dbl: Double(current.currentHR), len: 0))"
        
        if activityType == "RUN" || activityType == "ROW" || current.totalMovingTime == 0 || usingBTforSpeed == false{
            arr.append("\(stringer(dbl: geo.speed, len: 1))")
            tabBarController?.tabBar.items?[1].badgeValue = "\(stringer(dbl: geo.speed, len: 1))"
        } else {
            arr.append("\(stringer(dbl: current.currentSpeed, len: 1))")
            tabBarController?.tabBar.items?[1].badgeValue = "\(stringer(dbl: current.currentSpeed, len: 1))"
        }
        
        if activityType == "RUN" || activityType == "ROW" || usingBTforCadence == false {
            arr.append("\(geo.pace)")
        } else {
            arr.append("\(stringer(dbl: current.currentCadence, len: 0))")
            tabBarController?.tabBar.items?[3].badgeValue = "\(stringer(dbl: current.currentCadence, len: 0))"
        }
        
        //3 LBLS
        arr.append("\(stringer(dbl: current.currentScore, len: 0))%\nHR")
        
        arr.append("SPD\n\(stringer(dbl: Double(secondsInCurrentRound), len: 0))")
        
        if activityType == "RUN" || activityType == "ROW" || usingBTforCadence == false {
            arr.append("PACE")
        } else {
            arr.append("CAD")
        }
        
        //FOOTER
        if geo.distance > current.totalDistance {
            arr.append("\(createTimeString(seconds: Int(system.actualElapsedTime)))  \(stringer(dbl: geo.distance, len: 2)) MILES")
            tabBarController?.tabBar.items?[2].badgeValue = "\(stringer(dbl: longestDistance, len: 1)) MI"
        } else {
            arr.append("\(createTimeString(seconds: Int(system.actualElapsedTime)))  \(stringer(dbl: current.totalDistance, len: 2)) MILES")
            tabBarController?.tabBar.items?[2].badgeValue = "\(stringer(dbl: longestDistance, len: 1)) MI"
        }
        
        arrSend = arr
        arr = []
    }
    
    
    @IBOutlet weak var lblRiderName: UILabel!
    
    func showInputDialog() {
        
        let alertController = UIAlertController(title: "Rider Name", message: "", preferredStyle: .alert)
        
        let confirmAction = UIAlertAction(title: "Enter", style: .default) { (_) in
            
            let name = alertController.textFields?[0].text
            self.lblRiderName.text = name!.uppercased()
            self.riderName = name!.uppercased()
            print("riderName:  \(self.riderName)")
        }
        
        let cancelAction = UIAlertAction(title: "Cancel", style: .cancel) { (_) in }
        
        alertController.addTextField { (textField) in
            textField.placeholder = self.riderName
        }

        //adding the action to dialogbox
        alertController.addAction(confirmAction)
        alertController.addAction(cancelAction)
        
        //finally presenting the dialog box
        self.present(alertController, animated: true, completion: nil)
    }
    
    
    
    //get random
    var riderName: String = "TIM"
    var activityType: String = "BIKE"
    var audioStatus: String = "ON"
    @IBOutlet weak var lblTireSize: UILabel!
    @IBOutlet weak var lblMaxHeartrateValue: UILabel!
    
    @IBOutlet weak var lblSecPerRound: UILabel!
    @IBOutlet weak var lblActivityType: UILabel!
    
    @IBOutlet weak var lblAudioStatus: UILabel!
    
    func startTimer() {
        timer = Timer.scheduledTimer(timeInterval: timerIntervalValue,target: self,selector: #selector(timerInterval),userInfo: nil,repeats: true)
        print("Started")
        print(getFormattedTime(d: system.startTime!))
        print(getFormattedTimeAndDate(d: system.startTime!));print("\n");
        


    }
    
    func startAtLaunch() {
        print("startAtLaunch")
        system.status = "STARTED";statusValue.text = "STARTED";
        system.actualElapsedTime = getTimeIntervalSince(d1: system.startTime!, d2: Date())
        
        startTimer()
        startLocationUpdates()
    }
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        print(indexPath)
        
        switch indexPath.row {
        case 1:
            print("Start/Stop/Reset")
            
            if ConnectionCheck.isConnectedToNetwork() {
                print("Connected to Internet")
            }
            else{
                print("disConnected")
            }
            
            system.status = "STOPPED";statusValue.text = "COMPLETE";
            system.stopTime = Date()
            print("Stopped")
            print(getFormattedTime(d: system.stopTime!))
            print(getFormattedTimeAndDate(d: system.stopTime!));print("\n");
            print(getTimeIntervalSince(d1: system.startTime!, d2: system.stopTime!));print("\n");
            timer.invalidate()
            stopLocationUpdates()
            stopAndSave()
            
            
            
//            if system.status == "STOPPED" {
//                system.status = "STARTED";statusValue.text = "STARTED";
//                system.actualElapsedTime = getTimeIntervalSince(d1: system.startTime!, d2: Date())
//
//                if geo.status == "ON" {
//                    startLocationUpdates()
//                }
//
//                startTimer()
//                self.tabBarController?.selectedIndex = 3
//            } else {
//                system.status = "STOPPED";statusValue.text = "STOPPED";
//                system.stopTime = Date()
//                print("Stopped")
//                print(getFormattedTime(d: system.stopTime!))
//                print(getFormattedTimeAndDate(d: system.stopTime!));print("\n");
//                print(getTimeIntervalSince(d1: system.startTime!, d2: system.stopTime!));print("\n");
//                timer.invalidate()
//                stopLocationUpdates()
//                stopAndSave()
//            }
        case 2:
            let gst = geo.status
            if gst == "ON" {geo.status = "OFF";gpsStatus.text = "OFF";stopLocationUpdates();}
            if gst == "OFF" {geo.status = "ON";gpsStatus.text = "ON";startLocationUpdates();}
        case 3:
            //ble
            self.tabBarController?.selectedIndex = 1
        case 5:
            showInputDialog()
        case 6:
            let at = activityType
            if secondsSinceStart < 245 {
                if at == "BIKE" {activityType = "RUN";lblActivityType.text = "RUN";secondsPerRound = 300;lblSecPerRound.text = "300";}
                if at == "RUN" {activityType = "ROW";lblActivityType.text = "ROW";secondsPerRound = 300;lblSecPerRound.text = "300";}
                if at == "ROW" {activityType = "BIKE";lblActivityType.text = "BIKE";secondsPerRound = 1800;lblSecPerRound.text = "1800";}
            }

        case 7:
            if lblAudioStatus.text == "OFF" {audioStatus = "ON";lblAudioStatus.text = "ON";print("audioStatus is ON");} else {audioStatus = "OFF";lblAudioStatus.text = "OFF"}
        case 8:
            let hrz = maxHRvalue
            if hrz == 185 {maxHRvalue = 190;lblMaxHeartrateValue.text = "190";}
            if hrz == 190 {maxHRvalue = 195;lblMaxHeartrateValue.text = "195";}
            if hrz == 195 {maxHRvalue = 200;lblMaxHeartrateValue.text = "200";}
            if hrz == 200 {maxHRvalue = 185;lblMaxHeartrateValue.text = "185";}
            //print("Max HR:  \(maxHRvalue)")
            
        case 9:
            let tsz = wheelCircumference
            if tsz == 2105 {lblTireSize.text = "700X28";wheelCircumference = 2136;}
            if tsz == 2136 {lblTireSize.text = "700X32";wheelCircumference = 2155;}
            if tsz == 2155 {lblTireSize.text = "700X25";wheelCircumference = 2105;}
            
            //print("WheelCir:  \(wheelCircumference)")
        case 10:
            print("SHOW HISTORY PAGE")
        case 11:
            print("SHOW RESULTS PAGE")
        case 12:
            let spr = secondsPerRound
            if system.status == "STOPPED" || secondsSinceStart < 245 {
                if spr == 300 {secondsPerRound = 1800;lblSecPerRound.text = "1800"}
                if spr == 1800 {secondsPerRound = 300;lblSecPerRound.text = "300"}
            } else {
                //print("already started")
            }
        case 13:
            //print("13")
            
            UIApplication.shared.openURL(URL(string: "https://www.virtualcrit.com/tandc.html")!)
            //if audioStatus == "ON" {Utils.shared.say(sentence: "OK Kazumi, Let's Go")}

            //CLEAR HISTORY
//            udArray = []
//            udArray.append("CLEARED")
//            let defaults = UserDefaults.standard
//            defaults.set(udArray, forKey: "SavedStringArray")
            
            //CLEAR DB
//            let refDB  = FIRDatabase.database().reference(fromURL: "https://virtualcrit-47b94.firebaseio.com/")
//            refDB.removeValue()
            

        default:
            print("DO NOTHING")
        }
        
        
        tableView.deselectRow(at: indexPath as IndexPath, animated: true)
    }
    override func viewDidLoad() {
        super.viewDidLoad()
        print("viewDidLoad")
        
        system.startTime = Date()
        system.actualElapsedTime = getTimeIntervalSince(d1: system.startTime!, d2: Date())
        
        print("system.startTime:  \(String(describing: system.startTime))")
        
        let rn = Int(arc4random_uniform(1000))
        riderName = "TIM" + String(rn)
        lblRiderName.text = riderName
        
        startAtLaunch()

        
//        let defaults = UserDefaults.standard
//        udArray = defaults.stringArray(forKey: "SavedStringArray") ?? [String]()
//        udString = "NEW ACTIVITY, \(getFormattedTimeAndDate(d: Date()))\n"
    }
    

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    
    //FB PUSH, AT ROUND COMPLETE
    func fbPushII() {
        //send round data to fb
        //print("Start fbPush")
        let date = Date();let formatter = DateFormatter();formatter.dateFormat = "yyyyMMdd";let result = formatter.string(from: date)
        
        if roundsCompleted > 0 {
            // FIREBASE PUSH  - START
            let round_post = [
                "a_calcDurationPost" : roundsCompleted * 5,
                "a_scoreRoundLast" : (rounds.scores.last!*1000).rounded()/1000,
                "a_speedRoundLast" : (rounds.speeds.last!*1000).rounded()/1000,
                "fb_CAD" : (rounds.cadences.last!*1000).rounded()/1000,
                "fb_Date" : result,
                "fb_DateNow" : result,
                "fb_HR" : (rounds.heartrates.last!*1000).rounded()/1000,
                "fb_RND" : (rounds.scores.last!*1000).rounded()/1000,
                "fb_SPD" : (rounds.speeds.last!*1000).rounded()/1000,
                "fb_maxHRTotal" : maxHRvalue,
                "fb_scoreHRRound" : (rounds.scores.last!*1000).rounded()/1000,
                "fb_scoreHRRoundLast" : (rounds.scores.last!*1000).rounded()/1000,
                "fb_scoreHRTotal" : (rounds.scores.last!*1000).rounded()/1000,
                "fb_timAvgCADtotal" : rounds.cadences.last!,
                "fb_timAvgHRtotal" : (rounds.scores.last!*1000).rounded()/1000,
                "fb_timAvgSPDtotal" : (rounds.speeds.last!*1000).rounded()/1000,
                "fb_timDistanceTraveled" : longestDistance,
                "fb_timGroup" : activityType,
                "fb_timName" : riderName,
                "fb_timTeam" : "Square Pizza"
                ] as [String : Any]

            
            if activityType == "RUN" {
                let refDBRun  = FIRDatabase.database().reference(fromURL: "https://virtualcrit-47b94.firebaseio.com/rounds/run/\(result)")
                refDBRun.childByAutoId().setValue(round_post)
            }
            if activityType == "ROW" {
                let refDBRun  = FIRDatabase.database().reference(fromURL: "https://virtualcrit-47b94.firebaseio.com/rounds/row/\(result)")
                refDBRun.childByAutoId().setValue(round_post)
            }
            
            let refDB  = FIRDatabase.database().reference(fromURL: "https://virtualcrit-47b94.firebaseio.com/rounds/\(result)")
            refDB.childByAutoId().setValue(round_post)
            
            //print("Complete pushFBRound")
        }
        
        let when = DispatchTime.now() + 5
        DispatchQueue.main.asyncAfter(deadline: when){
            //print("calling fbPost for Totals")
            self.fbPost()
        }
        
        

    }
    
    //FB POST, AT ROUND COMPLETE
    func fbPost() {
        var tSpeed = geo.avgSpeed
        if current.totalAverageSpeed > geo.avgSpeed {
            tSpeed = current.totalAverageSpeed
        }
//        tSpeed = tSpeed * 100
//        tSpeed = round(tSpeed)
//        tSpeed = tSpeed / 100
        tSpeed = (tSpeed*1000).rounded()/1000
        //(tSpeed*1000).rounded()/1000

        var tScore = getScoreFromHR(x: rounds.heartrates.average)
        tScore = (tScore*1000).rounded()/1000
        
        var tCadence = rounds.cadences.average
        tCadence = (tCadence*1000).rounded()/1000
        
        //print("Start fbPost for Totals")
        let date = Date();let formatter = DateFormatter();formatter.dateFormat = "yyyyMMdd";let result = formatter.string(from: date)
        
        if roundsCompleted > 0 {
            // FIREBASE TOTALS POST
            let totals_post = [
                "a_calcDurationPost" : roundsCompleted * 5,
                "a_scoreHRRoundLast" : tScore,
                "a_scoreHRTotal" : tScore,
                "a_speedLast" : tSpeed,
                "a_speedTotal" : tSpeed,
                "fb_CAD" : tCadence,
                "fb_Date" : result,
                "fb_DateNow" : result,
                "fb_maxHRTotal" : maxHRvalue,
                "fb_scoreHRRound" : tScore,
                "fb_scoreHRRoundLast" : tScore,
                "fb_scoreHRTotal" : tScore,
                "fb_timAvgCADtotal" : tCadence,
                "fb_timAvgHRtotal" : tScore,
                "fb_timAvgSPDtotal" : tSpeed,
                "fb_timDistanceTraveled" : longestDistance,
                "fb_timGroup" : activityType,
                "fb_timName" : riderName,
                "fb_timTeam" : "Square Pizza"
                ] as [String : Any]
            
            if activityType == "RUN" {
                let refDBRun  = FIRDatabase.database().reference(fromURL: "https://virtualcrit-47b94.firebaseio.com/totals/run/\(result)/\(riderName)")
                refDBRun.setValue(totals_post)
            }
            if activityType == "ROW" {
                let refDBRun  = FIRDatabase.database().reference(fromURL: "https://virtualcrit-47b94.firebaseio.com/totals/row/\(result)/\(riderName)")
                refDBRun.setValue(totals_post)
            }
            
            let refDB  = FIRDatabase.database().reference(fromURL: "https://virtualcrit-47b94.firebaseio.com/totals/\(result)/\(riderName)")
            refDB.setValue(totals_post)
            //print("Complete postFBTotals")
        }
        
        let when = DispatchTime.now() + 5
        DispatchQueue.main.asyncAfter(deadline: when){
            
            //TODO:  IF NOT CONNECTED, DON'T TRY TO DO THIS, FLOODS THE TIMELINE.
            
            if ConnectionCheck.isConnectedToNetwork() {
                //print("Connected to Internet")
                //print("calling fb1")
                self.fb1()
            }
            else{
                //print("disConnected")
            }
        }
    }
    
    var currentScoreLeaderName = ""
    var currentScoreLeaderScore: Double = 0
    var newScoreLeader: Bool = false
    var freshFB = false
    var arrLeaderNamesByScore: String = ""
    //FB GETS (1,2,3,4)
    func fb1() {
        //print("start fb1")
        arrLeaderNamesByScore = ""
        arrResultsRoundScore = []
        arrResultsDetailsRoundScore = []
        let date = Date();let formatter = DateFormatter();formatter.dateFormat = "yyyyMMdd";let result = formatter.string(from: date)
        //let result = "20170527"
        var refDB  = FIRDatabase.database().reference(fromURL: "https://virtualcrit-47b94.firebaseio.com/rounds")
        if activityType == "RUN" {
            refDB  = FIRDatabase.database().reference(fromURL: "https://virtualcrit-47b94.firebaseio.com/rounds/run")
        }
        if activityType == "ROW" {
            refDB  = FIRDatabase.database().reference(fromURL: "https://virtualcrit-47b94.firebaseio.com/rounds/row")
        }
        let ref = refDB.child(result)
        _ = ref.queryLimited(toLast: 15).queryOrdered(byChild: "fb_RND").observeSingleEvent(of: .value, with: { snapshot in
            if ( snapshot.value is NSNull ) {
                print("not found")
            } else {
                //print("fb1, have a snapshot")
                for child in (snapshot.children) {
                    //print("fb1, have a child")
                    var sRND: String = "0"
                    let snap = child as! FIRDataSnapshot //each child is a snapshot
                    let dict = snap.value as! NSDictionary // the value is a dict
                    let fbRND = dict["fb_RND"]!
                    let fbNAME = dict["fb_timName"]!

                    if let dRND = fbRND as? Double {
                        sRND = stringer(dbl: dRND, len: 1)
                        if dRND > self.currentScoreLeaderScore {
                            self.currentScoreLeaderName = "\(fbNAME)"
                            self.currentScoreLeaderScore = dRND
                            self.newScoreLeader = true
                        }
                    } else {
                        sRND = "0"
                    }

                    self.arrLeaderNamesByScore = "\(sRND) %  \(fbNAME)\n" + self.arrLeaderNamesByScore
                    
                    if let xn = fbNAME as? String {
                        if xn == self.riderName {
                            arrResultsRoundScore.append("<ME>")
                        } else {
                            arrResultsRoundScore.append("\(fbNAME)")
                        }
                    } else {
                        arrResultsRoundScore.append("\(fbNAME)")
                    }
                    arrResultsDetailsRoundScore.append("\(sRND) % MAX")
                    

                    
                }
                
                arrResultsRoundScore.reverse()
                arrResultsDetailsRoundScore.reverse()
                
                
                //print("Completed:  (Round) Get 15 leaders, ordered by score")
                //print(self.arrLeaderNamesByScore)
                udArray.append("\(getFormattedTimeAndDate(d: Date()))\nROUND LEADERS (SCORE)\n\(self.arrLeaderNamesByScore)")
                //print("\n")
            }
        })
        { (error) in
            print(error.localizedDescription);print("error fb1");}
        //print("fb1 complete")
        let when = DispatchTime.now() + 10
        DispatchQueue.main.asyncAfter(deadline: when){
            //print("calling fb2")
            self.fb2()
        }
    }
    
    
    var currentSpeedLeaderName = ""
    var currentSpeedLeaderSpeed: Double = 0
    var newSpeedLeader: Bool = false
    var arrLeaderNamesBySpeed: String = ""
    func fb2() {
        //print("start fb2")
        arrLeaderNamesBySpeed = ""
        arrResultsRoundSpeed = []
        arrResultsDetailsRoundSpeed = []
        var n1: String = ""
        let date = Date();let formatter = DateFormatter();formatter.dateFormat = "yyyyMMdd";let result = formatter.string(from: date);
        var refDB  = FIRDatabase.database().reference(fromURL: "https://virtualcrit-47b94.firebaseio.com/rounds")
        if activityType == "RUN" {
            refDB  = FIRDatabase.database().reference(fromURL: "https://virtualcrit-47b94.firebaseio.com/rounds/run")
        }
        if activityType == "ROW" {
            refDB  = FIRDatabase.database().reference(fromURL: "https://virtualcrit-47b94.firebaseio.com/rounds/row")
        }
        let ref = refDB.child(result)
        _ = ref.queryLimited(toLast: 15).queryOrdered(byChild: "fb_SPD").observeSingleEvent(of: .value, with: { snapshot in
            if ( snapshot.value is NSNull ) {
                print("not found")
            } else {
                for child in (snapshot.children) {
                    //print("fb2, have a child")
                    var sSPD = "0"
                    let snap = child as! FIRDataSnapshot //each child is a snapshot
                    let dict = snap.value as! NSDictionary // the value is a dict
                    let fbNAME = dict["fb_timName"]!
                    let fbSPD = dict["fb_SPD"]!
                    
                    if let n2 = fbNAME as? String {
                        n1 = n2
                    } else {
                        print("Can't get the name as a string")
                    }
                    
                    if let dSPD = fbSPD as? Double {
                        if dSPD > self.currentSpeedLeaderSpeed {
                            self.currentSpeedLeaderSpeed = dSPD
                            self.currentSpeedLeaderName = n1
                            self.newSpeedLeader = true
                            //print("New Speed Leader:  \(self.currentSpeedLeaderName)")
                            //print("New Fastest Speed:  \(self.currentSpeedLeaderSpeed)")
                            self.self.fb_SpeedLeader.text = "\(self.currentSpeedLeaderName):  \(stringer(dbl: self.currentSpeedLeaderSpeed, len: 2))"
                        }
                        sSPD = stringer(dbl: dSPD, len: 2)
                    } else {
                        sSPD = "0"
                    }
//                    self.arrLeaderNamesBySpeed = "\(sSPD) MPH  \(fbNAME)\n" + self.arrLeaderNamesBySpeed
                    
                    if let xn = fbNAME as? String {
                        if xn == self.riderName {
                            arrResultsRoundSpeed.append("<ME>")
                            self.arrLeaderNamesBySpeed = "\(sSPD) MPH  <ME>\n" + self.arrLeaderNamesBySpeed
                        } else {
                            arrResultsRoundSpeed.append("\(fbNAME)")
                            self.arrLeaderNamesBySpeed = "\(sSPD) MPH  \(fbNAME)\n" + self.arrLeaderNamesBySpeed
                        }
                    } else {
                        arrResultsRoundSpeed.append("\(fbNAME)")
                        self.arrLeaderNamesBySpeed = "\(sSPD) MPH  \(fbNAME)\n" + self.arrLeaderNamesBySpeed
                    }
                    
                    
//                    arrResultsRoundSpeed.append("\(fbNAME)")
                    arrResultsDetailsRoundSpeed.append("\(sSPD) MPH")
                }
                
                arrResultsRoundSpeed.reverse()
                arrResultsDetailsRoundSpeed.reverse()
                
                //print("Completed:  (Round) Get 15 leaders, ordered by speed")
                //print(self.arrLeaderNamesBySpeed)
                udArray.append("\(getFormattedTimeAndDate(d: Date()))\nROUND LEADERS (SPEED)\n\(self.arrLeaderNamesBySpeed)")
                //print("\n")
                
                let when2 = DispatchTime.now() + 120
                DispatchQueue.main.asyncAfter(deadline: when2){
                    self.newBestSpeedsPoint(mileString: "TOP SPEEDS\n\n\(self.arrLeaderNamesBySpeed)")

                }
                
            }
        })
        { (error) in
            print(error.localizedDescription);print("error fb2");}
        print("fb2 complete")
        
//        if self.newSpeedLeader == true && newScoreLeader == false {
//            self.newSpeedLeader = false
//            if self.audioStatus == "ON" {
//                Utils.shared.say(sentence: "The Speed Leader is now \(self.currentSpeedLeaderName)")
//            }
//        }
//
//        if self.newSpeedLeader == false && newScoreLeader == true {
//            self.newScoreLeader = false
//            if self.audioStatus == "ON" {
//                Utils.shared.say(sentence: "The Score Leader is now \(self.currentScoreLeaderName)")
//            }
//        }
//
//        if self.newSpeedLeader == true && newScoreLeader == true {
//            self.newScoreLeader = false
//            self.newSpeedLeader = false
//            if self.audioStatus == "ON" {
//                Utils.shared.say(sentence: "The Speed Leader is now \(self.currentSpeedLeaderName).  And The Score Leader is now \(self.currentScoreLeaderName)")
//            }
//        }
        
        
        let when = DispatchTime.now() + 10
        DispatchQueue.main.asyncAfter(deadline: when){
            //print("calling fb3")
            self.fb3()
        }
    }
    
    var leaderNamesByScoreTotals: String = ""
    func fb3() { //get Totals from fb, ordered by score
        //print("start fb3")
        leaderNamesByScoreTotals = ""
        arrResultsTotalScore = []
        arrResultsDetailsTotalScore = []
        let date = Date();let formatter = DateFormatter();formatter.dateFormat = "yyyyMMdd";let result = formatter.string(from: date)
        var sSCORE: String = "0"
        var ref = FIRDatabase.database().reference(fromURL: "https://virtualcrit-47b94.firebaseio.com/totals/\(result)")
        if activityType == "RUN" {
            ref = FIRDatabase.database().reference(fromURL: "https://virtualcrit-47b94.firebaseio.com/totals/run/\(result)")
        }
        if activityType == "ROW" {
            ref = FIRDatabase.database().reference(fromURL: "https://virtualcrit-47b94.firebaseio.com/totals/row/\(result)")
        }
        ref.queryLimited(toLast: 15).queryOrdered(byChild: "a_scoreHRTotal").observeSingleEvent(of: .value, with: { snapshot in
            if ( snapshot.value is NSNull ) {
                print("not found")
            } else {
                for child in (snapshot.children) {
                    let snap = child as! FIRDataSnapshot //each child is a snapshot
                    let dict = snap.value as! NSDictionary // the value is a dict
                    let fbNAME = dict["fb_timName"]!
                    let fbSCORE = dict["a_scoreHRTotal"]!
                    
                    
                    if let dSCORE = fbSCORE as? Double {
                        sSCORE = stringer(dbl: dSCORE, len: 2)
                    } else {
                        sSCORE = "0"
                    }
                    self.leaderNamesByScoreTotals = "\(sSCORE)%  \(fbNAME)\n" + self.leaderNamesByScoreTotals
                    
                    if let xn = fbNAME as? String {
                        if xn == self.riderName {
                            arrResultsTotalScore.append("<ME>")
                        } else {
                            arrResultsTotalScore.append("\(fbNAME)")
                        }
                    } else {
                        arrResultsTotalScore.append("\(fbNAME)")
                    }
                    
                    
                    
//                    arrResultsTotalScore.append("\(fbNAME)")
                    arrResultsDetailsTotalScore.append("\(sSCORE) % MAX")
                
                }
                arrResultsTotalScore.reverse()
                arrResultsDetailsTotalScore.reverse()
                
                
                //print("leaderNamesByScoreTotals\n\(self.leaderNamesByScoreTotals) \n")
                udArray.append("\(getFormattedTimeAndDate(d: Date()))\nSCORE LEADERS (TOTAL)\n\(self.leaderNamesByScoreTotals)")
                
                //print("Complete fb3")
            }
        })
        { (error) in
            print(error.localizedDescription);print("error fb3");}
        //print("fb3 complete")
        let when = DispatchTime.now() + 5
        DispatchQueue.main.asyncAfter(deadline: when){
            //print("calling fb4")
            self.fb4()
        }
        
        

        
        
    }  //fb3 complete


    
    var leaderNamesBySpeedTotals: String = ""
    func fb4() { //get Totals from fb, ordered by speed
        //print("start fb4")
        leaderNamesBySpeedTotals = ""
        arrResultsTotalSpeed = []
        arrResultsDetailsTotalSpeed = []
        let date = Date();let formatter = DateFormatter();formatter.dateFormat = "yyyyMMdd";let result = formatter.string(from: date)
        
        var ref = FIRDatabase.database().reference(fromURL: "https://virtualcrit-47b94.firebaseio.com/totals/\(result)")
        if activityType == "RUN" {
            ref = FIRDatabase.database().reference(fromURL: "https://virtualcrit-47b94.firebaseio.com/totals/run/\(result)")
        }
        if activityType == "ROW" {
            ref = FIRDatabase.database().reference(fromURL: "https://virtualcrit-47b94.firebaseio.com/totals/row/\(result)")
        }
        ref.queryLimited(toLast: 15).queryOrdered(byChild: "a_speedTotal").observeSingleEvent(of: .value, with: { snapshot in
                if ( snapshot.value is NSNull ) {
                print("not found")
            } else {
                for child in (snapshot.children) {
                    var sSPD = "0"
                    let snap = child as! FIRDataSnapshot //each child is a snapshot
                    let dict = snap.value as! NSDictionary // the value is a dict
                    let fbNAME = dict["fb_timName"]!
                    let fbSPEED = dict["a_speedTotal"]!
                    
                    if let dSPD = fbSPEED as? Double {
                        sSPD = stringer(dbl: dSPD, len: 2)
                    } else {
                        sSPD = "0"
                    }
                    self.leaderNamesBySpeedTotals = "\(sSPD) MPH  \(fbNAME)\n" + self.leaderNamesBySpeedTotals
                    
                    if let xn = fbNAME as? String {
                        if xn == self.riderName {
                            arrResultsTotalSpeed.append("<ME>")
                        } else {
                            arrResultsTotalSpeed.append("\(fbNAME)")
                        }
                    } else {
                        arrResultsTotalSpeed.append("\(fbNAME)")
                    }
                    
                    
                    
//                    arrResultsTotalSpeed.append("\(fbNAME)")
                    arrResultsDetailsTotalSpeed.append("\(sSPD) MPH")
                    
                }
                    
                    arrResultsTotalSpeed.reverse()
                    arrResultsDetailsTotalSpeed.reverse()
                    
                //print("Complete fb4")
                //print("leaderNamesBySpeedTotals\n\(self.leaderNamesBySpeedTotals)\n")
                udArray.append("\(getFormattedTimeAndDate(d: Date()))\nSPEED LEADERS (TOTAL)\n\(self.leaderNamesBySpeedTotals)")
                self.freshFB = true
                    
                    if self.newSpeedLeader == true && self.newScoreLeader == false {
                    self.newSpeedLeader = false
                    if self.audioStatus == "ON" {
                        Utils.shared.say(sentence: "The Speed Leader is now, \(self.currentSpeedLeaderName)")
                    }
                }
                
                    if self.newSpeedLeader == false && self.newScoreLeader == true {
                    self.newScoreLeader = false
                    if self.audioStatus == "ON" {
                        Utils.shared.say(sentence: "The Score Leader is now, \(self.currentScoreLeaderName)")
                    }
                }
                
                    if self.newSpeedLeader == true && self.newScoreLeader == true {
                    self.newScoreLeader = false
                    self.newSpeedLeader = false
                    if self.audioStatus == "ON" {
                        Utils.shared.say(sentence: "The Speed Leader is now, \(self.currentSpeedLeaderName).  And The Score Leader is now, \(self.currentScoreLeaderName)")
                    }
                }
                    
                    
            }
        })
        { (error) in
            print(error.localizedDescription);print("error fb4");}
        //print("fb4 complete")
    }  //fb4 complete
   
    
    private func startLocationUpdates() {
        print("startLocationUpdates")
        locationManager.delegate = self
        locationManager.activityType = .fitness
        locationManager.distanceFilter = 10
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.pausesLocationUpdatesAutomatically = false
        
        locationManager.startUpdatingLocation()
        
    }
    
    func stopLocationUpdates() {
        locationManager.stopUpdatingLocation()
        print("stopLocationUpdates")
    }
    private let locationManager = LocationManager.shared
    private var locations: [CLLocation] = []
}

extension Starter_VC: CLLocationManagerDelegate {
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("Loc did fail:  \(error)")
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        for location in locations {
            let howRecent = location.timestamp.timeIntervalSinceNow
            guard location.horizontalAccuracy < 20 && abs(howRecent) < 10 else { continue }
            
            if self.locations.count > 1 {
                let ts = Double((location.timestamp.timeIntervalSince(self.locations.last!.timestamp)))
                if ts < 20 {
                    geo.elapsedTime += ts
                    gpsMovingTime.text = "\(createTimeString(seconds: Int((geo.elapsedTime)))) MOVING(G)"
                }
            }
                if self.locations.count > 2 {
                    if location.distance(from: self.locations.last!) < 161 {  // 1/10th of a mile
                        la = (self.locations.last?.coordinate.latitude)!
                        lo = (self.locations.last?.coordinate.longitude)!
                        geo.distance += location.distance(from: self.locations.last!) *  0.000621371 //Miles
                        
                        lastLocationTimeStamp = location.timestamp
                        var coords = [CLLocationCoordinate2D]()
                        coords.append(self.locations.last!.coordinate)
                        coords.append(location.coordinate)
                        
                        if location.speed > 0 {
                            geo.speed = location.speed * 2.23694
                            gpsMovingSpeed.text = "\(stringer(dbl: geo.speed,len: 1)) MPH(G)"
                            geo.pace = calcMinPerMile(mph: geo.speed)
                            gpsMovingPace.text = "\(String(describing: geo.pace)) PACE(G)"
                            gpsDistance.text = "\(stringer(dbl: geo.distance, len: 2)) MI(G)"
                            
                            geo.avgSpeed = Double(Double(geo.distance) / Double(geo.elapsedTime / 60 / 60))
                            gpsAverageSpeed.text = "\(stringer(dbl: geo.avgSpeed, len: 1)) AVG(G)"
                            gpsAvergagePace.text = "\(calcMinPerMile(mph: geo.avgSpeed)) AVG(G)"
                            
                            if activityType == "RUN" || activityType == "ROW" {
                            tabBarController?.tabBar.items?[3].badgeValue = "\(String(describing: geo.pace))"
                            tabBarController?.tabBar.items?[2].badgeValue = "\(stringer(dbl: geo.distance, len: 2)) MI"
                            }
                        }

                        if location.course > 315 || location.course <= 45 {
                            gpsDirection.text = "\(location.course)  [N]"
                            geo.direction = "\(location.course)  [N]"
                        }
                        if location.course > 45 && location.course <= 135 {
                            gpsDirection.text = "\(location.course)  [E]"
                            geo.direction = "\(location.course)  [E]"
                        }
                        if location.course > 135 && location.course <= 225 {
                            gpsDirection.text = "\(location.course)  [S]"
                            geo.direction = "\(location.course)  [S]"
                        }
                        if location.course > 225 && location.course <= 315 {
                            gpsDirection.text = "\(location.course)  [W]"
                            geo.direction = "\(location.course)  [W]"
                        }
                    }
                } else {
                    print("Waiting for the 3rd update  \(self.locations.count)")
                    if (self.locations.count == 2) {
                    //print("Last One")
                    }
                }
                self.locations.append(location)
        }
    }
    
    
}
