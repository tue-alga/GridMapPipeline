/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assign;

/**
 *
 * @author msondag
 */
public class Coordinate {

    public double x, y;


    public Coordinate(double x, double y) {
        this.x = x;
        this.y = y;
    }


    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }

    double distanceL1(Coordinate c) {
        return Math.abs((c.x - x)) + Math.abs((c.y - y));
    }

    double distance(Coordinate c) {
        return Math.sqrt(Math.pow(c.x - x, 2) + Math.pow(c.y - y, 2));
    }

    public void minus(double x, double y) {
        this.x -= x;
        this.y -= y;
    }

    void scale(double scaleX, double scaleY) {
        x = x * scaleX;
        y = y * scaleY;
    }


}
