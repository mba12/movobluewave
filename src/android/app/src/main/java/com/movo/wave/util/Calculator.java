package com.movo.wave.util;

import java.util.Calendar;

/**
 * Created by Michael Ahern on 4/6/15.
 */
public class Calculator {

    private static final double MALE_BMR_WEIGHT = 13.75;
    private static final double MALE_BMR_AGE = 6.76;
    private static final double MALE_BMR_HEIGHT = 5.00;
    private static final double MALE_BMR_CONST = 66.00;
    private static final double FEMALE_BMR_WEIGHT = 9.56;
    private static final double FEMALE_BMR_HEIGHT = 1.85;
    private static final double FEMALE_BMR_AGE = 4.68;
    private static final double FEMALE_BMR_CONST = 655;
    private static final double LB_TO_KG = 0.4536;
    private static final double IN_TO_CM = 2.54;
    private static final double ROUND_FACTOR = 0.5;
    private static final double MET_HEIGHT_DIVISOR = 60;
    private static final double MET_MILE_CONSTANT = 3.92;


    //this is a lookup-table in order to get the MET values calculated by Michael.
    private double[] MET_TABLE = {2.0, 2.5, 3.0, 3.3, 3.8, 5.0, 6.3, 8.0};

    private static final double distance = 0;
    private static final String MALE = "Male";
    private static Calendar calendar = Calendar.getInstance();

    /*
    * steps integer
    * height in inches
    */
    public double calculate_distance(int steps, int height) {

        // below is the explanation for the math done to calculate the MET
        // milesper10k = (height / 60) * 3.92
        // miles = (steps / 10000) * milesper10k

        double milesPer10k = ((double) height / 60.0) * 3.92;
        double miles = ((double) steps / 10000.0) * milesPer10k;
        return miles;
    }


    public double simple_calculate_calories(int steps) {

        return (double) steps * 0.045;
    }

    public double calculate_calories(int steps, int height, int weight, String gender, int birthYear, int minutes) {
        double calories = 0;
        double cm = convert_inches_to_cm(height);
        double kg = convert_lbs_to_kgs(weight);
        double distance = calculate_distance(steps, height);
        double age = calendar.get(Calendar.YEAR) - birthYear;

        // round to nearest 0.5 = math.round(met / 0.5) * 0.5
        // get index between 0-7 = rounded * 2 - 3, clamp to 0 and 7 (rounded values expected are between 1.5 and 5)
        double met = calculate_met((double) distance, (double) minutes, steps, cm);
        int index = (int) met;
        met = MET_TABLE[index]; // this line performs the lookup in the table above to get the MET value


        double bmr = 0.0;
        if (gender.equals(MALE)) {
            // this formula is used if the person is MALE
            bmr = weight * LB_TO_KG * MALE_BMR_WEIGHT + height * MALE_BMR_HEIGHT * IN_TO_CM -
                    age * MALE_BMR_AGE + MALE_BMR_CONST;
        } else {
            // this formula is used if the person is FEMALE or has not identified a gender
            bmr = weight * LB_TO_KG * FEMALE_BMR_WEIGHT + height * FEMALE_BMR_HEIGHT * IN_TO_CM
                    - age * FEMALE_BMR_AGE + FEMALE_BMR_CONST;
        }

        bmr = calculate_bmr(kg, cm, age, gender);
        double t = minutes / 60.0;   // NOTE: this should be 1 for hourly data

        // final calculation following the formula: BMR / 24 * T (in this case 1 hour) * MET
        calories = bmr / 24.0 * met * t;

        return calories;

    }

    private double convert_inches_to_cm(double inches) {
        return inches * 2.54;
    }

    private double convert_lbs_to_kgs(double pounds) {
        return (pounds / 2.2046);
    }

    private double calculate_bmr(double weight_kg, double height_cm, double age, String gender) {

        double bmr = 0.00;
        // NOTE: watch out for case sensitivity in this compare
        if (gender.equals(MALE)) {
            bmr = (13.75 * weight_kg) + (5 * height_cm) - (6.76 * age) + 66.0;
        } else { // Default is female if not explicitly male in our database.
            bmr = (9.56 * weight_kg) + (1.85 * height_cm) - (4.68 * age) + 655.0;
        }
        return bmr;
    }


    private double calculate_met(double distance, double minutes, double steps, double cm) {

        // Would be preferable to have hour by hour data on steps to better know walking intensity.
        double avg_speed = distance / minutes / 60.0;

        // TODO: CHECK THIS
        double met = clamp((int) java.lang.Math.round(((steps / 10000.00) *
                    (cm / MET_HEIGHT_DIVISOR) * MET_MILE_CONSTANT) / 0.50) *
                    0.50 * 2.0 - 3.0, 0, 7);
        return met;
    }

    private double clamp(double val, double min, double max) {
        if (val < min) { return min; }
        else if(val > max) { return max; }
        else { return  val; }
    }

    public static void main(String[] args)
            throws Exception {

        Calculator calc = new Calculator();
        double dist = calc.calculate_distance(13233, 72);
        System.out.println("End process: " + dist);
    }

}
