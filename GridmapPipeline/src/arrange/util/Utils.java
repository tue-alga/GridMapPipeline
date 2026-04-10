package arrange.util;

import java.util.List;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.util.DoubleUtil;

/**
 * Class with general utility functions.
 * 
 * @author Rafeal Cano, Wouter Meulemans
 */
public class Utils {

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

    public static int triangleOrientation(Vector v1, Vector v2, Vector v3) {
        double cross = Vector.crossProduct(
                Vector.subtract(v2, v1),
                Vector.subtract(v3, v1)
        );
        if (cross > 0) {
            return 1;
        } else if (cross < 0) {
            return -1;
        } else {
            return 0;
        }
    }

    public static int triangleOrientation(Position2D p1, Position2D p2, Position2D p3) {
        return triangleOrientation(p1.getPosition(), p2.getPosition(), p3.getPosition());
    }

    public static int counterclockwiseCompare(Vector center, Vector v1, Vector v2) {
        Quadrant q1 = Quadrant.of(Vector.subtract(v1, center));
        Quadrant q2 = Quadrant.of(Vector.subtract(v2, center));
        int comparison = q1.compareTo(q2);
        if (comparison == 0) {
            return triangleOrientation(v1, center, v2);
        } else {
            return comparison;
        }
    }

    public static Vector meanPosition(List<? extends Position2D> positions) {
        Vector result = new Vector(0, 0);
        if (positions.size() > 0) {
            for (Position2D p : positions) {
                result.translate(p.getPosition());
            }
            result.scale(1.0 / positions.size());
        }
        return result;
    }

    /**
     * Returns the intersection of the two lines that pass through p0p1 and
     * q0q1, or null if it does not exist.
     */
    public static Vector lineIntersection(Vector p0, Vector p1, Vector q0, Vector q1) {
        Vector p1p0 = Vector.subtract(p1, p0);
        Vector q1q0 = Vector.subtract(q1, q0);
        double det = p1p0.getY() * q1q0.getX() - p1p0.getX() * q1q0.getY();
        if (Math.abs(det) < DoubleUtil.EPS) {
            return null;
        } else {
            double invDet = 1 / det;
            Vector p0q0 = Vector.subtract(p0, q0);
            double p = invDet * (q1q0.getY() * p0q0.getX() - q1q0.getX() * p0q0.getY());

            p1p0.scale(p);
            p1p0.translate(p0);
            return p1p0;
        }
    }

    /**
     * Returns the intersection of two line segments p0p1 and q0q1 or null if it
     * does not exist.
     */
    public static Vector lineSegmentIntersection(Vector p0, Vector p1, Vector q0, Vector q1) {
        Vector p1p0 = Vector.subtract(p1, p0);
        Vector q1q0 = Vector.subtract(q1, q0);
        double det = p1p0.getY() * q1q0.getX() - p1p0.getX() * q1q0.getY();
        if (Math.abs(det) < DoubleUtil.EPS) {
            return null;
        } else {
            double invDet = 1 / det;
            Vector p0q0 = Vector.subtract(p0, q0);
            double p = invDet * (q1q0.getY() * p0q0.getX() - q1q0.getX() * p0q0.getY());
            if (p < 0 || p > 1) {
                return null;
            } else {
                double q = invDet * (p1p0.getY() * p0q0.getX() - p1p0.getX() * p0q0.getY());
                if (q < 0 || q > 1) {
                    return null;
                } else {

                    p1p0.scale(p);
                    p1p0.translate(p0);
                    return p1p0;
                }
            }
        }
    }

    public static void zeroSafeNormalize(Vector v) {
        double len = v.length();
        if (len != 0) {
            v.scale(1.0 / len);
        }
    }

}
