//
//  CollectionViewCell.swift
//  Wave
//
//  Created by Phil Gandy on 5/11/15.
//


import UIKit

class CollectionViewCell: UICollectionViewCell, ImageUpdateDelegate {
    
    required init?(coder aDecoder: NSCoder) {
    super.init(coder: aDecoder)
    }
    
    var textLabel: UILabel!
    var textLabel2: UILabel!
    var imageView: UIImageView!
    var gradImageView: UIImageView!
    var bgImageView: UIImageView!
    var currentDate:NSDate?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
    
        
        bgImageView = UIImageView(frame: CGRect(x: 0, y: 0, width: frame.size.width, height: frame.size.height))
        bgImageView.contentMode = UIViewContentMode.ScaleAspectFill
        bgImageView.clipsToBounds = true
        
        bgImageView.image = UIImage(named:"calendarbg")
        contentView.addSubview(bgImageView)

        gradImageView = UIImageView(frame: CGRect(x: 0, y: 0, width: frame.size.width, height: frame.size.height))
        gradImageView.contentMode = UIViewContentMode.ScaleAspectFill
        gradImageView.clipsToBounds = true
        gradImageView.image = UIImage(named:"calendargradient")
        contentView.addSubview(gradImageView)
        
        
//        cell.backgroundView = UIColor(patternImage: UIImage(named:"calendarbg")!)

        
        
    
        imageView = UIImageView(frame: CGRect(x: 0, y: frame.height/2-(frame.height*0.45/2), width: frame.size.width, height: frame.size.height*0.45))
        imageView.contentMode = UIViewContentMode.ScaleAspectFit
        imageView.clipsToBounds = true
        
        contentView.addSubview(imageView)
    
        let textFrame = CGRect(x: 0, y: (frame.size.width/2)-10, width: frame.size.width, height: 24)
        textLabel = UILabel(frame: textFrame)
        textLabel.font = UIFont(name: "Gotham", size: 18) //.systemFontOfSize(18)
        textLabel.textColor = UIColor.blackColor()
        textLabel.textAlignment = .Center
        contentView.addSubview(textLabel)
        
        
        let textFrame2 = CGRect(x: 0, y: (frame.size.width-24-1), width: frame.size.width, height: 24)
        textLabel2 = UILabel(frame: textFrame2)
        textLabel2.font = UIFont(name: "Gotham", size: 14) //.systemFontOfSize(18)
        textLabel2.textColor = UIColor.whiteColor()
        textLabel2.textAlignment = .Center
        contentView.addSubview(textLabel2)
        
        
        
    }
    
    
    func updatedImage(date: NSDate?, newImage: UIImage?) {
        var setImage = false
        if let unwrappedDate = date {
            if (unwrappedDate == currentDate) {
                if let image = newImage {
                    dispatch_async(dispatch_get_main_queue(), {
                        self.bgImageView.image = newImage
                    })
                    setImage = true
                }
            }
            /*
            if ( (unwrappedDate == currentDate) && !setImage) {
                var newImage = UIImage(named: "calendarbg")
                dispatch_async(dispatch_get_main_queue(),  {
                    self.bgImageView.image = newImage
                })
            }
            */
        }
    
    }
    
    override func prepareForReuse() {
            let image = UIImage(named: "calendarbg")
            self.bgImageView.image = image

            textLabel.text = ""
            textLabel2.text = ""

    }
    
}
