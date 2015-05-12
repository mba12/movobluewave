//
//  MyLifeViewController.swift
//  Wave
//
//  Created by Phil Gandy on 5/11/15.
//  Copyright (c) 2015 Phil Gandy. All rights reserved.
//

import Foundation
import UIKit

class MyLifeViewController: UIViewController, UICollectionViewDelegateFlowLayout, UICollectionViewDataSource {

    @IBOutlet weak var collectionViewHost: UIView!

    @IBOutlet weak var collectionView: UICollectionView!
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        /*
        let layout: UICollectionViewFlowLayout = UICollectionViewFlowLayout()
        layout.sectionInset = UIEdgeInsets(top: 20, left: 10, bottom: 10, right: 10)
        
        collectionView = UICollectionView(frame: collectionViewHost.frame, collectionViewLayout: layout)

        collectionView!.dataSource = self
        collectionView!.delegate = self

        


        self.view.addSubview(collectionView!)
*/
        collectionView!.registerClass(CollectionViewCell.self, forCellWithReuseIdentifier: "CollectionViewCell")
        collectionView!.backgroundColor = UIColor.clearColor()
        collectionViewHost.backgroundColor = UIColor.clearColor()
    }
    
    func numberOfSectionsInCollectionView(collectionView: UICollectionView) -> Int {
        return 1
    }
    
    func collectionView(collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return 31
    }
    
    func collectionView(collectionView: UICollectionView, cellForItemAtIndexPath indexPath: NSIndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCellWithReuseIdentifier("CollectionViewCell", forIndexPath: indexPath) as! CollectionViewCell
        cell.backgroundColor = UIColor.blackColor()
        //cell.textLabel?.text = "\(indexPath.section):\(indexPath.row)"
        cell.textLabel?.text = "\(indexPath.row)"
        cell.imageView?.image = UIImage(named: "datebgwide")
        return cell
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }


}