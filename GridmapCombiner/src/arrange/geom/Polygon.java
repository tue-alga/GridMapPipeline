package arrange.geom;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class Polygon {

    private ArrayList<Point2D> points;
    private double area;
    private Point2D centroid;

    public Polygon(ArrayList<Point2D> points) {
        if (points.isEmpty()) {
            throw new IllegalArgumentException("list of points is empty");
        }
        this.points = new ArrayList<>(points);
        area = 0.0;
        double cx = 0.0;
        double cy = 0.0;
        for (int i = 0; i < points.size(); i++) {
            Point2D p = points.get(i);
            Point2D q = points.get((i + 1) % points.size());
            double increment = p.getX() * q.getY() - q.getX() * p.getY();
            area += increment;
            cx += increment * (p.getX() + q.getX());
            cy += increment * (p.getY() + q.getY());
        }
        area /= 2;
        cx /= 6 * area;
        cy /= 6 * area;
        centroid = new Point2D(cx, cy) {};
    }

    public int numberOfSides() {
        return points.size();
    }

    public double getArea() {
        return Math.abs(area);
    }

    public double getSignedArea() {
        return area;
    }

    public Point2D getCentroid() {
        return centroid;
    }

    public Area convertToArea() {
        Path2D path = convertToPath();
        return new Area(path);
    }

    public Path2D convertToPath() {
        Path2D path = new Path2D.Double();
        boolean first = true;
        for (Point2D p : points) {
            if (first) {
                path.moveTo(p.getX(), p.getY());
                first = false;
            } else {
                path.lineTo(p.getX(), p.getY());
            }
        }
        path.closePath();
        return path;
    }

    /*
     * The Area class assumes that regions are bounded
     */
    public static List<Polygon> areaToPolygon(Area a) {
        ArrayList<Polygon> polygons = new ArrayList<>();
        ArrayList<Point2D> points = new ArrayList<>();
        PathIterator pit = a.getPathIterator(null);
        while (!pit.isDone()) {
            double[] coords = new double[6];
            int type = pit.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_CLOSE:
                    polygons.add(new Polygon(points));
                    points.clear();
                    break;
                case PathIterator.SEG_LINETO:
                    points.add(new Point2D(coords[0], coords[1]));
                    break;
                case PathIterator.SEG_MOVETO:
                    points.add(new Point2D(coords[0], coords[1]));
                    break;
                default:
                    throw new IllegalArgumentException("area is not polygonal");
            }
            pit.next();
        }
        return polygons;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(points.get(0));
        for (int i = 1; i < points.size(); i++) {
            sb.append(", ");
            sb.append(points.get(i));
        }
        sb.append("}");
        return sb.toString();
    }
}
