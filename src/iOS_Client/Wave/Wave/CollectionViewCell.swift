//
//  CollectionViewCell.swift
//  Wave
//
//  Created by Phil Gandy on 5/11/15.
//  Copyright (c) 2015 Phil Gandy. All rights reserved.
//


import UIKit

class CollectionViewCell: UICollectionViewCell {
    
    required init(coder aDecoder: NSCoder) {
    super.init(coder: aDecoder)
    }
    
    var textLabel: UILabel!
    var textLabel2: UILabel!
    var imageView: UIImageView!
    
    override init(frame: CGRect) {
        super.init(frame: frame)
    
        imageView = UIImageView(frame: CGRect(x: 0, y: 16, width: frame.size.width, height: frame.size.height*2/3))
        imageView.contentMode = UIViewContentMode.ScaleAspectFit
        contentView.addSubview(imageView)
    
        let textFrame = CGRect(x: 0, y: 32, width: frame.size.width, height: frame.size.height/3)
        textLabel = UILabel(frame: textFrame)
        textLabel.font = UIFont.systemFontOfSize(UIFont.smallSystemFontSize())
        textLabel.textAlignment = .Center
        contentView.addSubview(textLabel)
        
        
        let textFrame2 = CGRect(x: 0, y: 60, width: frame.size.width, height: frame.size.height/3)
        textLabel2 = UILabel(frame: textFrame2)
        textLabel2.font = UIFont.systemFontOfSize(UIFont.smallSystemFontSize())
        textLabel2.textAlignment = .Center
        contentView.addSubview(textLabel2)
    }
}
