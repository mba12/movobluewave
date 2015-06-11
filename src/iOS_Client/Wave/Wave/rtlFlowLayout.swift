//
//  rtlFlowLayout.swift
//  Wave
//
//  Created by Rudy Yukich on 6/11/15.
//
//

import Foundation
import UIKit


class rtlFlowLayout : UICollectionViewFlowLayout {
    
    required init(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        self.itemSize = CGSize(width: 95, height: 95)
        self.minimumInteritemSpacing = 7.5
        self.minimumLineSpacing = 5
        
    }
    override func layoutAttributesForElementsInRect(rect: CGRect) -> [AnyObject]? {
        var supersAttributes = super.layoutAttributesForElementsInRect(rect)
        
        for attributes in supersAttributes as! [UICollectionViewLayoutAttributes] {
            var frame = attributes.frame
            frame.origin.x = rect.size.width - attributes.frame.size.width - attributes.frame.origin.x
            attributes.frame = frame
            
        }

        return supersAttributes
    }
    
    
}