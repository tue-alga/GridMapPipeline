package arrange.model.util.KML;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author Max Sondag
 */
public class RasterSquare {

    public double x, y, width, height;
    //index in the rasterSquare
    public int i, j;
    /**
     * Holds the id of the region this square belongs to. Null = no region
     */
    public String id;

    public RasterSquare(double x, double y, double width, double height, int i, int j) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.id = null;
        this.i = i;
        this.j = j;
    }

    /**
     * If the area (partially) overlaps this area, then the id of this square
     * will be set to {@code id}.
     *
     * @param area
     * @param id
     */
    public void assignArea(Area area, String id) {
        Area rectangle = new Area(new Rectangle2D.Double(x, y, width, height));
        rectangle.intersect(area);
        if (!rectangle.isEmpty()) {
            //partially overlaps
            this.id = id;
        }
    }

}
