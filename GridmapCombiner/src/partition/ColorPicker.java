/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package partition;

import java.awt.Color;

/**
 *
 * @author msondag
 */
public class ColorPicker {

    public static int colorId = 0;

    /**
     * Returns a new color each time it is called. Attempts to make all colors different.
     * @return 
     */
    public static Color getNewColor() {
        Color c = getCartographicColor(colorId);
        colorId++;
        return c;
    }

    private static Color getColor(double r, double g, double b) {
        int red = (int) Math.round(r * 255.0);
        int green = (int) Math.round(g * 255.0);
        int blue = (int) Math.round(b * 255.0);
        return new Color(red, green, blue);
    }

    private static Color getCartographicColor(int color) {

        //only get color values between 0 and 12
        if (color > 12) {
            color = color % 13;
        }

        switch (color) {
            case 0:
                return getColor(0.145, 0.737, 0.612);
            case 1:
                return getColor(0.533, 0.78, 0.396);
            case 2:
                return getColor(0.561, 0.737, 0.757);
            case 3:
                return getColor(0.604, 0.839, 0.741);
            case 4:
                return getColor(0.706, 0.592, 0.506);
            case 5:
                return getColor(0.733, 0.718, 0.349);
            case 6:
                return getColor(0.831, 0.878, 0.353);
            case 7:
                return getColor(0.835, 0.725, 0.541);
            case 8:
                return getColor(0.867, 0.529, 0.475);
            case 9:
                return getColor(0.996, 0.965, 0.608);
            case 10:
                return getColor(0.996, 0.859, 0.706);
            case 11:
                return getColor(0.980, 0.714, 0.58);
            case 12:
                return getColor(1.000, 0.8, 0.302);
            default:
                return null;
        }

    }

}
