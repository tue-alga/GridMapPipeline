package arrange.colouring.colourschemes;

import java.awt.Color;
import java.io.InvalidClassException;
import java.util.Arrays;
import java.util.Iterator;

public class OxygenColourScheme extends ColourScheme {

    protected OxygenColourScheme() throws InvalidClassException {
        super();
    }

    @Override
    protected Iterator<Color> getColours() {
        Color[] colors = new Color[] {
                new Color(255, 128, 128), // red
                new Color(119, 183,  83), // green
                new Color(128, 179, 255), // blue
                new Color(255, 235,  85), // yellow
                new Color(193, 115, 176), // purple
                new Color(179, 146,  93), // brown
        };
        return Arrays.asList(colors).iterator();
    }

}
