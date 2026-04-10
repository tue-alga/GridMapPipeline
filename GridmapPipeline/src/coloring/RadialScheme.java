package coloring;

import java.awt.Color;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Rectangle;

/**
 * Colors radially around a given center point. Hue depends on angle (w.r.t a
 * zero angle), brightness on distance from the center point. Used for e.g. the
 * NL map, with: middle = (315.0 / 573.0, 451.0 / 666.0) -- point in IJsselmeer;
 * zeroangle = 90 (opening at the top).
 *
 * @author Max Sondag, Wouter Meulemans
 */
public class RadialScheme extends ColorScheme {

    private Vector middle;
    private double zeroangle;
    private double maxDistance;

    public RadialScheme(Vector middle, double zeroangle) {
        this.middle = middle;
        this.zeroangle = zeroangle;

        maxDistance = 0;
        for (Vector c : Rectangle.byCornerAndSize(Vector.origin(), 1, 1).corners()) {
            maxDistance = Math.max(maxDistance, c.distanceTo(middle));
        }
    }

    @Override
    public Color assign(double xfrac, double yfrac) {
        Vector pt = new Vector(xfrac, yfrac);
        double distance = pt.distanceTo(middle);
        double angle = Math.toDegrees(Vector.subtract(pt, middle).computeClockwiseAngleTo(Vector.right(), true, false));

        float anglePercentage = (float) ((angle + zeroangle) % 360.0) / 360f;
        float distancePercentage = (float) (distance / maxDistance);

        float h = anglePercentage;
        float s = 0.7f;
        float b = 1f - distancePercentage * 0.8f;

        return Color.getHSBColor(h, s, b);
    }

}
