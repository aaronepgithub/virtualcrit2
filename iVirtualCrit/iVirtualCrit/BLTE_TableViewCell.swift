//
//  BLTE_TableViewCell.swift
//  iVirtualCrit
//
//  Created by aaronep on 1/29/18.
//  Copyright © 2018 aaronep. All rights reserved.
//

import UIKit

class BLTE_TableViewCell: UITableViewCell {
    
    @IBOutlet weak var BLTE_CellSubTitle: UILabel!
    @IBOutlet weak var BLTE_CellTitle: UILabel!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

}
