package arrange.colouring.colourschemes;

import java.awt.Color;
import java.io.InvalidClassException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public abstract class ColourScheme implements Iterator<Color> {

    /**
     * List of all available colours.
     */
    private final List<Color> allColors;
    /**
     * Index of the current color.
     */
    private int currentColor;
    /**
     * Random number generator for {@link #getRandomColour()}.
     */
    private Random random;
    
    /**
     * Initialise colours using {@link #getColours()}.
     * 
     * @throws InvalidClassException If the number of colours is less than 2.
     */
    protected ColourScheme() throws InvalidClassException {
        allColors = new ArrayList<>();
        Iterator<Color> colours = getColours();
        while (colours.hasNext()) {
            allColors.add(colours.next());
        }
        if (allColors.size() < 2) {
            throw new InvalidClassException("ColourScheme should have at least "
                    + "2 colours. This one has " + allColors.size() + ".");
        }
        currentColor = -1;
        random = new Random();
    }
    
    protected abstract Iterator<Color> getColours();
    
    /**
     * @return A random colour from this colour scheme.
     */
    public final Color getRandomColour() {
        return allColors.get(random.nextInt(allColors.size()));
    }
    
    @Override
    public final boolean hasNext() {
        return true;
    }
    
    @Override
    public final Color next() {
        currentColor = (currentColor + 1) % allColors.size();
        return allColors.get(currentColor);
    }
    
    @Override
    public final void remove() {
        throw new UnsupportedOperationException();
    }
    
}
