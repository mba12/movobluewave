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
        //handle iPhone6 and iPhone6P//
        var isPhone = false
        var isIPhone6 = false
        var isIPhone6P = false
        if (UIDevice.currentDevice().userInterfaceIdiom == UIUserInterfaceIdiom.Phone) {
            isPhone = true
            if (UIScreen.mainScreen().bounds.size.height == 667.0) {
                isIPhone6 = true
            }
            if (UIScreen.mainScreen().scale > 2.9) {
                isIPhone6 = true
                isIPhone6P = true
            }
            if (UIScreen.mainScreen().nativeScale > 2.9) {
                isIPhone6 = true
                isIPhone6P = true
            }
            if (UIScreen.mainScreen().bounds.size.height == 736.0) {
                isIPhone6 = true
                isIPhone6P = true
            }
                
        }
        if (!isIPhone6) {
            self.itemSize = CGSize(width: 95, height: 95)
        } else {
            if (!isIPhone6P) {
                self.itemSize = CGSize(width: 110, height: 110)
            } else {
                self.itemSize = CGSize(width: 120, height: 120)
            }
        }
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