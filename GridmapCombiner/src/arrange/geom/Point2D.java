package arrange.geom;

import java.util.Comparator;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class Point2D {

    private double x;
    private double y;
    /**
     * Comparator to sort points in ascending x order (ascending y order to
     * break ties).
     */
    public static final Comparator<Point2D> ascendingX = new Comparator<Point2D>() {
        @Override
        public int compare(Point2D p1, Point2D p2) {
            int c = Double.compare(p1.x, p2.x);
            if (c == 0) {
                return Double.compare(p1.y, p2.y);
            } else {
                return c;
            }
        }
    };
    /**
     * Comparator to sort points in ascending y order (ascending x order to
     * break ties).
     */
    public static final Comparator<Point2D> ascendingY = new Comparator<Point2D>() {
        @Override
        public int compare(Point2D p1, Point2D p2) {
            int c = Double.compare(p1.y, p2.y);
            if (c == 0) {
                return Double.compare(p1.x, p2.x);
            } else {
                return c;
            }
        }
    };

    public Point2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    @Override
    public String toString() {
        return String.format("(%.2f, %.2f)", x, y);
    }
}
