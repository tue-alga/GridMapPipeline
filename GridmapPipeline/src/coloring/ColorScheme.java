package coloring;

import common.Site;
import common.SiteMap;
import java.awt.Color;
import nl.tue.geometrycore.geometry.linear.Rectangle;

/**
 *
 * @author Wouter Meulemans
 */
public abstract class ColorScheme {

    public abstract Color assign(double xfrac, double yfrac);

    public void apply(SiteMap map) {
        //get bounding box
        Rectangle box = new Rectangle();
        for (Site s : map.sites()) {
            box.include(s);
        }        

        double minX = box.getLeft();
        double minY = box.getBottom();

        //get color ranges
        //start at hsl color and change luminance value
        double xRange = box.width();
        double yRange = box.height();

        //for each site, get the color. Different color ranges for different maps.
        for (Site s : map.sites()) {
            double xFrac = (s.getX() - minX) / xRange;
            double yFrac = (s.getY() - minY) / yRange;
            s.setColor(assign(xFrac, yFrac));
        }
    }
}
