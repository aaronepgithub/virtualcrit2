//
//  Results_VC.swift
//  iVirtualCrit
//
//  Created by aaronep on 1/31/18.
//  Copyright © 2018 aaronep. All rights reserved.
//

import UIKit

class Results_VC: UITableViewController {

    @IBOutlet var outResults: UITableView!
    @IBOutlet weak var outFooter: UILabel!
    @IBOutlet weak var outHeader: UILabel!
    
    @objc func tapFooterFunction(sender:UITapGestureRecognizer) {
        //print("tapFooter working")
        disMe()
    }
    
    var tapHeaderCount = 5
    @objc func tapHeaderFunction(sender:UITapGestureRecognizer) {
        //print("tapHeader working, change to resultsRoundSpeed")
        
        switch tapHeaderCount {
        case 1:
            if arrResultsDetailsRoundSpeed.isEmpty {return}
            outHeader.text = "LEADERS (CRIT, BY SPEED)"
            arrResults = arrResultsRoundSpeed
            arrResultsDetails = arrResultsDetailsRoundSpeed
            outResults.reloadData()
            tapHeaderCount = 2
        case 2:
            if arrResultsRoundScore.isEmpty {return}
            outHeader.text = "LEADERS (CRIT, BY SCORE)"
            arrResults = arrResultsRoundScore
            arrResultsDetails = arrResultsDetailsRoundScore
            outResults.reloadData()
            tapHeaderCount = 3
        case 3:
            if arrResultsTotalScore.isEmpty {return}
            outHeader.text = "LEADERS (TOTALS, BY SCORE)"
            arrResults = arrResultsTotalScore
            arrResultsDetails = arrResultsDetailsTotalScore
            outResults.reloadData()
            tapHeaderCount = 4
        case 4:
            if arrResultsTotalSpeed.isEmpty {return}
            outHeader.text = "LEADERS (TOTALS, BY SPEED)"
            arrResults = arrResultsTotalSpeed
            arrResultsDetails = arrResultsDetailsTotalSpeed
            outResults.reloadData()
            tapHeaderCount = 5
        case 5:
        if arrResultsMyRoundSpeed.isEmpty {return}
        outHeader.text = "MY RESULTS (TAP TO CHANGE)"
        arrResults = arrResultsMyRoundSpeed
        arrResultsDetails = arrResultsDetailsMyRoundSpeed
        outResults.reloadData()
        tapHeaderCount = 1
            
        default:
            print("Default, do nothing")
        }
//        arrResults = arrResultsRoundSpeed
//        arrResultsDetails = arrResultsDetailsRoundSpeed
//        outResults.reloadData()
        
//        arrResults = arrResultsRoundScore
//        arrResultsDetails = arrResultsDetailsRoundScore
//        outResults.reloadData()

    }
    
    func disMe() {
        self.dismiss(animated: true, completion: nil)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        let tap = UITapGestureRecognizer(target: self, action: #selector(tapFooterFunction))
        outFooter.isUserInteractionEnabled = true
        outFooter.addGestureRecognizer(tap)
        
        let tapHeader = UITapGestureRecognizer(target: self, action: #selector(tapHeaderFunction))
        outHeader.isUserInteractionEnabled = true
        outHeader.addGestureRecognizer(tapHeader)
        
        if arrResultsMyRoundSpeed.count > 0 {
            outHeader.text = "MY RESULTS"
            arrResults = arrResultsMyRoundSpeed
            arrResultsDetails = arrResultsDetailsMyRoundSpeed
        }
        
        
        if arrResults.count == 0 {
            let when = DispatchTime.now() + 5
            outHeader.text = "NO DATA, CHECK BACK"
            DispatchQueue.main.asyncAfter(deadline: when){
                self.disMe()
            }
        }

    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }

    override func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return arrResults.count
    }
    
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        let cell = tableView.dequeueReusableCell(withIdentifier: "ID1", for: indexPath)
        
        cell.textLabel?.text = "\(indexPath.row + 1).  \(arrResults[indexPath.row])"
        cell.detailTextLabel?.text = "\(arrResultsDetails[indexPath.row])"
        return cell
        
        
    }

    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        
        //DISMISS AFTER SELECTING ANY ROW
        self.dismiss(animated: true, completion: nil)
        tableView.deselectRow(at: indexPath as IndexPath, animated: true)
    }
    

}
