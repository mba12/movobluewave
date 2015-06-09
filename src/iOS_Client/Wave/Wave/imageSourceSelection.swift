//
//  imageSourceSelection.swift
//  Wave
//
//  Created by Rudy Yukich on 6/9/15.
//
//

import Foundation
import UIKit
import CoreData


protocol ImageSourceSelectionDelegate {
    func didSelectSource(useCamera: Bool)
}


class ImageSourceSelection {
    static func pickImageSource(parent: UIViewController, delegate: ImageSourceSelectionDelegate, location : CGRect?) {
        var didAddAction = false
        let alert : UIAlertController = UIAlertController(title: nil, message: nil, preferredStyle: UIAlertControllerStyle.ActionSheet)
        
        let takePicture = UIAlertAction(title: "Take photo", style: UIAlertActionStyle.Default, handler: {action in delegate.didSelectSource(true) })
        
        let pickPhoto = UIAlertAction(title: "Choose from library", style: UIAlertActionStyle.Default, handler: {
            action in delegate.didSelectSource(false) }
        )
        if (UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.Camera)) {
            alert.addAction(takePicture)
            didAddAction = true
        }
        if (UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.SavedPhotosAlbum)) {
            alert.addAction(pickPhoto)
            didAddAction = true
        }
        
        alert.modalInPopover = false
        
        if (UIDevice.currentDevice().userInterfaceIdiom == .Phone) {
            alert.addAction(UIAlertAction(title: "Cancel", style: UIAlertActionStyle.Cancel, handler: nil))
            
        }
        
        if let presenter = alert.popoverPresentationController {
            presenter.sourceView = parent.view
            if let loc = location {
                presenter.sourceRect = loc
            } else {
                presenter.sourceRect = CGRectMake(parent.view.bounds.size.width-20, 80.0, 1.0, 1.0)
            }
            
            presenter.permittedArrowDirections = UIPopoverArrowDirection.Up
            
        }
        if (didAddAction) {
            parent.presentViewController(alert, animated: true, completion: nil)
        }
        
    }
}