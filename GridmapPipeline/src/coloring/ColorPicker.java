package coloring;

import java.awt.Color;
import nl.tue.geometrycore.geometryrendering.styling.ExtendedColors;

/**
 *
 * @author Max Sondag, Wouter Meulemans
 */
public class ColorPicker {

    private static int colorId = 0;

    /**
     * Returns a new color each time it is called. Attempts to make all colors different.
     * @return 
     */
    public static Color getNewColor() {
        Color c = ExtendedColors.cartographic[colorId];
        colorId = (colorId+1)%ExtendedColors.cartographic.length;
        return c;
    }
    
    public static void reset() {
        colorId = 0;
    }
}
