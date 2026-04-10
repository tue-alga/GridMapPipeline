package arrange.util;

import nl.tue.geometrycore.geometry.Vector;

/**
 *
 * @author Wouter Meulemans
 */
public enum Quadrant {
    ORIGIN, POSITIVE_X_AXIS, FIRST, POSITIVE_Y_AXIS, SECOND, NEGATIVE_X_AXIS, THIRD, NEGATIVE_Y_AXIS, FOURTH;

    public static Quadrant of(Vector v) {
        final double x = v.getX();
        final double y = v.getY();
        if (x > 0) {
            if (y > 0) {
                return Quadrant.FIRST;
            } else if (y < 0) {
                return Quadrant.FOURTH;
            } else {
                return Quadrant.POSITIVE_X_AXIS;
            }
        } else if (x < 0) {
            if (y > 0) {
                return Quadrant.SECOND;
            } else if (y < 0) {
                return Quadrant.THIRD;
            } else {
                return Quadrant.NEGATIVE_X_AXIS;
            }
        } else {
            if (y > 0) {
                return Quadrant.POSITIVE_Y_AXIS;
            } else if (y < 0) {
                return Quadrant.NEGATIVE_Y_AXIS;
            } else {
                return Quadrant.ORIGIN;
            }
        }
    }

}
