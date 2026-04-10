package arrange.Utils;


import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import arrange.model.Network.Vertex;
import arrange.model.util.CircularListIterator;
import arrange.model.util.Pair;
import arrange.model.util.Position2D;
import arrange.model.util.Vector2D;
import arrange.model.util.Vector2D.Quadrant;

/**
 * Class with general utility functions.
 */
public class Utils {

    public static final double EPS = 1E-6;

    /**
     * Given two angles in the range [-pi, pi], returns the difference between
     * them in the range [0, pi].
     */
    public static double angleDifference(double a1, double a2) {
        double difference = Math.abs(a2 - a1);
        if (difference > Math.PI) {
            difference = 2 * Math.PI - difference;
        }
        return difference;
    }

    /**
     * Return a color that somewhat resembles all colors of the given vertices.
     *
     * @param vertices Vertices to calculate color of.
     * @return Average of colors of given vertices, or black if no vertices are
     * given ({@code vertices == null} or {@code vertices.size() == 0}).
     */
    public static Color averageVertexColor(ArrayList<Vertex> vertices) {
        if (vertices == null || vertices.isEmpty()) {
            return Color.BLACK;
        }

        float r = 0;
        float g = 0;
        float b = 0;
        float a = 0;

        for (Vertex v : vertices) {
            r += v.getColor().getRed() / 255f;
            g += v.getColor().getGreen() / 255f;
            b += v.getColor().getBlue() / 255f;
            a += v.getColor().getAlpha() / 255f;
        }

        return new Color(r / vertices.size(), g / vertices.size(),
                b / vertices.size(), a / vertices.size());
    }

    public static double signedTriangleArea(Vector2D v1, Vector2D v2, Vector2D v3) {
        return ((v2.getX() - v1.getX()) * (v3.getY() - v1.getY()) - (v2.getY() - v1.getY()) * (v3.getX() - v1.getX())) / 2;
    }

    public static int triangleOrientation(Vector2D v1, Vector2D v2, Vector2D v3) {
        double area = signedTriangleArea(v1, v2, v3);
        if (area > 0) {
            return 1;
        } else if (area < 0) {
            return -1;
        } else {
            return 0;
        }
    }

    public static int triangleOrientation(Position2D p1, Position2D p2, Position2D p3) {
        return triangleOrientation(p1.getPosition(), p2.getPosition(), p3.getPosition());
    }

//    public static int triangleOrientation(Position2D p1, Position2D p2, Position2D p3) {
//        return triangleOrientation(p1.getPosition(), p2.getPosition(), p3.getPosition());
//    }
    public static int counterclockwiseCompare(Vector2D center, Vector2D v1, Vector2D v2) {
        Quadrant q1 = Vector2D.difference(v1, center).quadrant();
        Quadrant q2 = Vector2D.difference(v2, center).quadrant();
        int comparison = q1.compareTo(q2);
        if (comparison == 0) {
            return triangleOrientation(v1, center, v2);
        } else {
            return comparison;
        }
    }

    public static Vector2D meanPosition(List<? extends Position2D> positions) {
        Vector2D result = new Vector2D(0, 0);
        if (positions.size() > 0) {
            for (Position2D p : positions) {
                result.add(p.getPosition());
            }
            result.multiply(1d / positions.size());
        }
        return result;
    }

    /**
     * Returns the intersection of the two lines that pass through p0p1 and
     * q0q1, or null if it does not exist.
     */
    public static Vector2D lineIntersection(Vector2D p0, Vector2D p1, Vector2D q0, Vector2D q1) {
        Vector2D p1p0 = Vector2D.difference(p1, p0);
        Vector2D q1q0 = Vector2D.difference(q1, q0);
        double det = p1p0.getY() * q1q0.getX() - p1p0.getX() * q1q0.getY();
        if (Math.abs(det) < EPS) {
            return null;
        } else {
            double invDet = 1 / det;
            Vector2D p0q0 = Vector2D.difference(p0, q0);
            double p = invDet * (q1q0.getY() * p0q0.getX() - q1q0.getX() * p0q0.getY());
            return p1p0.multiply(p).add(p0);
        }
    }

    /**
     * Returns the intersection of two line segments p0p1 and q0q1 or null if it
     * does not exist.
     */
    public static Vector2D lineSegmentIntersection(Vector2D p0, Vector2D p1, Vector2D q0, Vector2D q1) {
        Vector2D p1p0 = Vector2D.difference(p1, p0);
        Vector2D q1q0 = Vector2D.difference(q1, q0);
        double det = p1p0.getY() * q1q0.getX() - p1p0.getX() * q1q0.getY();
        if (Math.abs(det) < EPS) {
            return null;
        } else {
            double invDet = 1 / det;
            Vector2D p0q0 = Vector2D.difference(p0, q0);
            double p = invDet * (q1q0.getY() * p0q0.getX() - q1q0.getX() * p0q0.getY());
            if (p < 0 || p > 1) {
                return null;
            } else {
                double q = invDet * (p1p0.getY() * p0q0.getX() - p1p0.getX() * p0q0.getY());
                if (q < 0 || q > 1) {
                    return null;
                } else {
                    return p1p0.multiply(p).add(p0);
                }
            }
        }
    }

    /**
     * Returns the point closest to p on the line that passes through q0q1.
     */
    public static Vector2D closestPoint(Vector2D p, Vector2D q0, Vector2D q1) {
        Vector2D q1q0 = Vector2D.difference(q1, q0);
        Vector2D pq0 = Vector2D.difference(p, q0);
        double k = q1q0.dotProduct(pq0) / q1q0.dotProduct(q1q0);
        return q1q0.multiply(k).add(q0);
    }

    /**
     * Returns the absolute area of the polygon with the specified boundary
     * points.
     */
    public static double computePolygonArea(ArrayList<Vector2D> points) {
        double area = 0;
        for (int i = 0; i < points.size(); i++) {
            Vector2D p = points.get(i);
            Vector2D q = points.get((i + 1) % points.size());
            area += p.getX() * q.getY() - q.getX() * p.getY();
        }
        return Math.abs(area / 2);
    }

    /**
     * Returns the leftmost item or null if the list is empty.
     */
    public static <T extends Position2D> T leftmost(Iterable<T> positions) {
        Iterator<T> iterator = positions.iterator();
        T leftmost = null;
        if (iterator.hasNext()) {
            leftmost = iterator.next();
            while (iterator.hasNext()) {
                T current = iterator.next();
                if (current.getPosition().getX() < leftmost.getPosition().getX()) {
                    leftmost = current;
                }
            }
        }
        return leftmost;
    }

    /**
     * Returns the rightmost item or null if the list is empty.
     */
    public static <T extends Position2D> T rightmost(Iterable<T> positions) {
        Iterator<T> iterator = positions.iterator();
        T rightmost = null;
        if (iterator.hasNext()) {
            rightmost = iterator.next();
            while (iterator.hasNext()) {
                T current = iterator.next();
                if (current.getPosition().getX() > rightmost.getPosition().getX()) {
                    rightmost = current;
                }
            }
        }
        return rightmost;
    }

    /**
     * Returns the bottommost item or null if the list is empty.
     */
    public static <T extends Position2D> T bottommost(Iterable<T> positions) {
        Iterator<T> iterator = positions.iterator();
        T bottommost = null;
        if (iterator.hasNext()) {
            bottommost = iterator.next();
            while (iterator.hasNext()) {
                T current = iterator.next();
                if (current.getPosition().getY() < bottommost.getPosition().getY()) {
                    bottommost = current;
                }
            }
        }
        return bottommost;
    }

    /**
     * Returns the bottommost item or null if the list is empty.
     */
    public static <T extends Position2D> T topmost(Iterable<T> positions) {
        Iterator<T> iterator = positions.iterator();
        T topmost = null;
        if (iterator.hasNext()) {
            topmost = iterator.next();
            while (iterator.hasNext()) {
                T current = iterator.next();
                if (current.getPosition().getY() > topmost.getPosition().getY()) {
                    topmost = current;
                }
            }
        }
        return topmost;
    }

    public static Pair<Double, Double> lineSegmentIntersection(Pair<Double, Double> startP1, Pair<Double, Double> endP1, Pair<Double, Double> startP2, Pair<Double, Double> endP2) {
        Vector2D p0 = new Vector2D(startP1.getFirst(), startP2.getSecond());
        Vector2D p1 = new Vector2D(endP1.getFirst(), endP1.getSecond());
        Vector2D q0 = new Vector2D(startP2.getFirst(), startP2.getSecond());
        Vector2D q1 = new Vector2D(endP2.getFirst(), endP2.getSecond());
        Vector2D lineSegmentIntersection = lineSegmentIntersection(p0, p1, q0, q1);
        if (lineSegmentIntersection == null) {
            return null;
        }
        return new Pair<Double, Double>(lineSegmentIntersection.getX(), lineSegmentIntersection.getY());
    }
}
