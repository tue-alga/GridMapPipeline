package arrange.colouring.colourschemes;

import java.io.InvalidClassException;
import java.util.HashMap;

/**
 * Factory for {@link ColourScheme}.
 */
public class ColourSchemes {
    
    private static HashMap<Integer, ColourScheme> cache;

    private static Integer OXYGEN = 1;
    
    private static void initCache() {
        if (cache == null) {
            cache = new HashMap<>();
        }
    }
    
    public static ColourScheme getOxygenColourScheme() {
        initCache();
        if (cache.containsKey(OXYGEN)) {
            return cache.get(OXYGEN);
        } else {
            try {
                OxygenColourScheme oxygen = new OxygenColourScheme();
                cache.put(OXYGEN, oxygen);
                return oxygen;
            } catch (InvalidClassException e) {
                // should not occur
                return null;
            }
        }
    }
    
}
