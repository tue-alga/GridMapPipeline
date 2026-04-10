/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package colorgenerator;

import java.awt.Color;

/**
 *
 * @author msondag
 */
public class Site {

    double x, y;
    String label;
    Color color;

    public Site(double x, double y, String label) {
        this.x = x;
        this.y = y;
        this.label = label;
    }

    public String print() {
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();
        return label + "\t" + x + "\t" + y + "\t" + red + "\t" + green + "\t" + blue;
    }

}
