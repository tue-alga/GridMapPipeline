package arrange.colouring;

import java.awt.*;

import arrange.model.subdivision.Map;
import arrange.model.subdivision.Map.Face;

/**
 * A map colouring that uses a colour scale that is dependent on the value of
 * the face to be coloured. The colour of a face is determined by averaging two
 * colours (the low and the high colour) in such a way that low values receive
 * the low colour, and high values receive the high colour. To be exact, the
 * colour of a face `f` is computed as follows:
 * 
 * ````
 * relWeight(f) = (weight(f) - min) / (max - min);
 * 
 *               / low                                    if relWeight(f) <= 0;
 *              |  high                                   if relWeight(f) >= 1;
 * colour(f) = <
 *              |  (1 - relWeight(f)) * low
 *               \              + relWeight(f) * high     otherwise.
 * ````
 */
public class ValueColouring extends Colouring {
    
    protected Color low;
    protected double min;
    
    protected Color high;
    protected double max;
    
    /**
     * Creates a new value colouring with the given base colour.
     * 
     * @param low The colour for the low end of the colour scale.
     * @param min The value for which the low colour is displayed.
     * @param high The colour for the high end of the colour scale.
     * @param max The value for which the high colour is displayed.
     */
    public ValueColouring(Color low, double min, Color high, double max) {
        this.low = low;
        this.min = min;
        this.high = high;
        this.max = max;
    }
    
    /**
     * Creates a new value colouring with the extent and colour range of
     * the given map (that is, its faces).
     * 
     * @param map Map to analyse.
     */
    public ValueColouring(Map map) {
        Face loFace = null;
        Face hiFace = null;
        this.min = Double.POSITIVE_INFINITY;
        this.max = Double.NEGATIVE_INFINITY;
        for (Face face : map.boundedFaces()) {
            double fw = face.getWeight();
            if (fw < this.min) {
                this.min = fw;
                loFace = face;
            }
            if (fw > this.max) {
                this.max = fw;
                hiFace = face;
            }
        }
        if (loFace != null) {
            this.low = loFace.getColor();
        } else {
            this.low = Color.WHITE;
        }
        if (hiFace != null) {
            this.high = hiFace.getColor();
        } else {
            this.high = Color.BLACK;
        }
    }

    @Override
    public void assignColours(Map map) {
        for (Face f : map.faces()) {
            double weight = f.getWeight();
            f.setColor(getColorForWeight(weight));
        }
    }
    
    /**
     * @return The color for high values.
     */
    public Color getHighColor() {
        return high;
    }
    
    /**
     * @return The color for low values.
     */
    public Color getLowColor() {
        return low;
    }
    
    /**
     * @return The value associated with the high colour.
     */
    public double getMax() {
        return max;
    }
    
    /**
     * @return The value associated with the low colour.
     */
    public double getMin() {
        return min;
    }
    
    /**
     * Returns the colour for the given relative value (0 is low, 1 is high).
     */
    protected Color getColourForRel(float rel) {
        if (rel <= 0) {
            return low;
        } else if (rel >= 1) {
            return high;
        } else {
            float[] lowColor = new float[3];
            low.getRGBColorComponents(lowColor);
            
            float[] highColor = new float[3];
            high.getRGBColorComponents(highColor);
            
            return new Color(
                          (1 - rel) * lowColor[0] + rel * highColor[0],
                          (1 - rel) * lowColor[1] + rel * highColor[1],
                          (1 - rel) * lowColor[2] + rel * highColor[2]
                       );
        }
    }
    
    /**
     * Returns the colour for a given weight.
     */
    protected Color getColorForWeight(double weight) {
        return getColourForRel((float) ((weight - min) / (max - min)));
    }
}

