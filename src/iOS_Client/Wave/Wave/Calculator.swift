//
//  Calculator.swift
//  Wave
//
//  Created by Michael Ahern on 5/1/15.
//

import Foundation

public class Calculator {
    
    static let MALE_BMR_WEIGHT:Double = 13.75;
    static let MALE_BMR_AGE:Double = 6.76;
    static let MALE_BMR_HEIGHT:Double = 5.00;
    static let MALE_BMR_CONST:Double = 66.00;
    static let FEMALE_BMR_WEIGHT:Double = 9.56;
    static let FEMALE_BMR_HEIGHT:Double = 1.85;
    static let FEMALE_BMR_AGE:Double = 4.68;
    static let FEMALE_BMR_CONST:Double = 655;
    static let LB_TO_KG:Double = 0.4536;
    static let IN_TO_CM:Double = 2.54;
    static let ROUND_FACTOR:Double = 0.5;
    static let MET_HEIGHT_DIVISOR:Double = 60
    static let MET_MILE_CONSTANT:Double = 3.92;
    static let MALE = "Male";
    //this is a lookup-table in order to get the MET values calculated by Michael.
    static let MET_TABLE = [2.0, 2.5, 3.0, 3.3, 3.8, 5.0, 6.3, 8.0];
    
    /*
    * steps integer
    * height in inches
    */
    public static func calculate_distance(steps:Int, height:Int) -> Double {
        
        // below is the explanation for the math done to calculate the MET
        // milesper10k = (height / 60) * 3.92
        // miles = (steps / 10000) * milesper10k
        
        var milesPer10k = (Double(height) / 60.0) * 3.92;
        var miles = ( Double(steps) / 10000.0) * milesPer10k;
        return miles;
    }
    
    public static func simple_calculate_calories(int steps:Int) -> Double {
        return Double(steps) * 0.045;
    }
    
    public static func calculate_calories(steps:Int, height:Int, weight:Int, gender:String,
                                     birthYear:Int, minutes:Int) -> Double {
        var calories:Double = 0;
        var cm:Double = convert_inches_to_cm(Double(height));
        var kg:Double = convert_lbs_to_kgs(double: Double(weight));
        var distance:Double = calculate_distance(steps, height: height);

        // These flags are deprecated - xcode suggestion does not work
        let flags: NSCalendarUnit = .DayCalendarUnit | .MonthCalendarUnit | .YearCalendarUnit;
        let date = NSDate();
        let components = NSCalendar.currentCalendar().components(flags, fromDate: date)
    
        let year = components.year
    
        var age:Int = Int(year) - birthYear;
    
        // round to nearest 0.5 = math.round(met / 0.5) * 0.5
        // get index between 0-7 = rounded * 2 - 3, clamp to 0 and 7 (rounded values expected are between 1.5 and 5)
        var met:Double = calculate_met(Double(distance), minutes: Double(minutes), steps: Double(steps), cm: Double(cm));
        var index:Int = Int(met);
        met = MET_TABLE[index]; // this line performs the lookup in the table above to get the MET value
    
    
        var bmr:Double = 0.0;
        if (gender == MALE) {
            // this formula is used if the person is MALE
            var part1:Double = Double(weight) * LB_TO_KG * MALE_BMR_WEIGHT;
            var part2 = MALE_BMR_HEIGHT * IN_TO_CM;
            bmr =  part1 + Double(height) * part2 - Double(age) * MALE_BMR_AGE + MALE_BMR_CONST;
        } else {
            // this formula is used if the person is FEMALE or has not identified a gender
            var fpart1:Double = Double(weight) * LB_TO_KG * FEMALE_BMR_WEIGHT;
            bmr = fpart1 + Double(height) * FEMALE_BMR_HEIGHT * IN_TO_CM - Double(age) * FEMALE_BMR_AGE + FEMALE_BMR_CONST;
        }
    
        bmr = calculate_bmr(kg, height_cm: cm, age: Double(age), gender: gender);
        var t:Double = Double(minutes) / 60.0;   // NOTE: this should be 1 for hourly data
    
        // final calculation following the formula: BMR / 24 * T (in this case 1 hour) * MET
        calories = bmr / 24.0 * met * t;
    
        return calories;
    }
    
    public static func convert_inches_to_cm(inches:Double) -> Double {
        return inches * 2.54;
    }
    
    public static func convert_lbs_to_kgs(double pounds:Double) -> Double {
        return (pounds / 2.2046);
    }
    
    public static func calculate_bmr(weight_kg:Double, height_cm:Double, age:Double, gender:String) -> Double {
    
        var bmr:Double = 0.00;
        // NOTE: watch out for case sensitivity in this compare
        if (gender == MALE) {
            bmr = (13.75 * weight_kg) + (5 * height_cm) - (6.76 * age) + 66.0;
        } else { // Default is female if not explicitly male in our database.
            bmr = (9.56 * weight_kg) + (1.85 * height_cm) - (4.68 * age) + 655.0;
        }
        return bmr;
    }
    
    public static func calculate_met(distance:Double, minutes:Double, steps:Double, cm:Double) -> Double {
    
        // Would be preferable to have hour by hour data on steps to better know walking intensity.
        var avg_speed:Double = distance / minutes / 60.0;
        
        var value:Double = Double(round(  (  (steps / 10000.00) * (cm / MET_HEIGHT_DIVISOR) * MET_MILE_CONSTANT) / 0.50  )) * ROUND_FACTOR * 2.0 - 3.0;
    
        var met:Double = clamp(value, min: 0.0, max: 7);
        return met;
    }
    
    static internal func clamp(val:Double, min:Double, max:Double) -> Double {
        if (val < min) { return min; }
        else if(val > max) { return max; }
        else { return  val; }
    }

}
