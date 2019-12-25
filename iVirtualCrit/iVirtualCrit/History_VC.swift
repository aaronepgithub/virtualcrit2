//
//  History_VC.swift
//  iVirtualCrit
//
//  Created by aaronep on 1/31/18.
//  Copyright © 2018 aaronep. All rights reserved.
//

import UIKit

class History_VC: UIViewController {
    
    
    @IBAction func btn_Dismiss(_ sender: UIButton) {
        self.dismiss(animated: true, completion: nil)
    }
    
    @IBOutlet weak var textViewTimeline: UITextView!
    
    
    
    override func viewDidLoad() {
        super.viewDidLoad()

        
        textViewTimeline.text = ""
        var z = ""
        let x = udArray.count
        if x == 0 {return}
        var y = 0
        while y < x {
            z += udArray[y]
            z += "\n  RECORD \(y + 1) OF \(x)  \n\n"
            y += 1
        }
        textViewTimeline.text = z
        
        // Do any additional setup after loading the view.
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destinationViewController.
        // Pass the selected object to the new view controller.
    }
    */

}
