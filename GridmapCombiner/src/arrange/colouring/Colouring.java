package arrange.colouring;

import arrange.model.subdivision.Map;

/**
 * A colouring of a map.
 */
public abstract class Colouring {
    
    /**
     * Colours the map.
     * @param map The map to colour.
     */
    public abstract void assignColours(Map map);
}
