/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package partition;

/**
 *
 * @author msondag
 */
public class Precision {

    //if two number are less than accuracy apart we consider them to be equal
    public static final double ACCURACY = 1e-6;

    //Check if d1 equals d2 taking accuracy into account
    public static boolean eq(double d1, double d2) {
        return Math.abs(d1 - d2) < ACCURACY;
    }

    //check if d1 >= d2 taking accuracy into account
    public static boolean geq(double d1, double d2) {
        return Math.abs(d1 - d2) < ACCURACY || d1 > d2;
    }

    //check if d1 > d2 taking accuracy into account
    public static boolean ge(double d1, double d2) {
        return (d1 - d2) > ACCURACY;
    }

    //check if d1 <= d2 taking accuracy into account
    public static boolean leq(double d1, double d2) {
        if (Math.abs(d1 - d2) < ACCURACY || d2 > d1) {
            return true;
        }
        return false;
    }

    //check if d1 < d2 taking accuracy into account
    public static boolean le(double d1, double d2) {
        return ((d2 - d1) > ACCURACY);

    }

    public static int comparePrecision(double d1, double d2) {
        if (le(d1, d2)) {
            return -1;
        } else if (eq(d1, d2)) {
            return 0;
        } else {
            return 1;
        }
    }
}
