package arrange.model.util.KML;

import java.awt.Point;

/**
 *
 * @author Max Sondag
 */
class RasterLineSegment {

    public double x1, x2, y1, y2, w, h;
    //RasterSquare 1 is the left/top square, rasterSquare 2 is the right/bottom square
    RasterSquare r1, r2;

    public RasterLineSegment(double x1, double y1, double w, double h, RasterSquare r1, RasterSquare r2) {

        this.x1 = x1;
        this.x2 = x1 + w;
        this.y1 = y1;
        this.y2 = y1 + h;
        this.w = w;
        this.h = h;
        this.r1 = r1;
        this.r2 = r2;
        
        if(x1 != x2 && y1 != y2)
        {
            System.err.println("This linesegment is neither horizontal or vertical");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!RasterLineSegment.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        final RasterLineSegment other = (RasterLineSegment) obj;

        if (this.x1 == other.x1 && this.x2 == other.x2 && this.y1 == other.y1 && this.y2 == other.y2) {
            return true;
        }
        //inverse is still the same
        return false;

    }

    public boolean hasSameEndpoint(RasterLineSegment sPrime) {
        Point.Double s1 = new Point.Double(x1, y1);
        Point.Double s2 = new Point.Double(x2, y2);

        Point.Double sPrime1 = new Point.Double(sPrime.x1, sPrime.y1);
        Point.Double sPrime2 = new Point.Double(sPrime.x2, sPrime.y2);

        return s1.equals(sPrime1) || s1.equals(sPrime2) || s2.equals(sPrime1) || s2.equals(sPrime2);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = (int) (53 * hash + (x1 * x2));
        hash = (int) (53 * hash + (y1 * y2));
        return hash;
    }

    boolean isHorizontal() {
        return y1 == y2;
    }

}
