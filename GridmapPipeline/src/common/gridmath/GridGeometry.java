package common.gridmath;

import common.gridmath.GridMath.Coordinate;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Polygon;

/**
 *
 * @author Wouter Meulemans
 */
public class GridGeometry {

    private final Polygon[] boundaries, transformed;
    private final Vector right, up, transformed_right, transformed_up;
    private AffineTransform transform = null;
    private double[] inverse = null;
    private String name;
    private final GridGeometry patternlevel;

    public GridGeometry(String name, Vector right, Vector up, Polygon boundary) {
        this.name = name;
        this.boundaries = new Polygon[]{boundary};
        this.right = right;
        this.up = up;
        this.transformed = new Polygon[boundaries.length];
        this.transformed_right = right.clone();
        this.transformed_up = up.clone();
        int i = 0;
        for (Polygon p : boundaries) {
            if (p.areaSigned() < 0) {
                p.reverse();
            }
            transformed[i++] = p.clone();
        }
        this.patternlevel = null;
    }

    public GridGeometry(String name, Vector right, Vector up, GridGeometry patternlevel, Polygon... boundaries) {
        this.name = name;
        this.boundaries = boundaries;
        this.right = right;
        this.up = up;
        this.transformed = new Polygon[boundaries.length];
        this.transformed_right = right.clone();
        this.transformed_up = up.clone();
        int i = 0;
        for (Polygon p : boundaries) {
            if (p.areaSigned() < 0) {
                p.reverse();
            }
            transformed[i++] = p.clone();
        }
        if (patternlevel.boundaries.length != 1 && patternlevel.boundaries.length != boundaries.length) {
            System.err.println("Warning: pattern level has higher complexity that does not match this grid?");
            System.err.println("   " + patternlevel.name + ": " + patternlevel.boundaries.length);
            System.err.println(" < " + name + ": " + boundaries.length);
        }
        this.patternlevel = patternlevel;
    }

    public GridGeometry getPatternlevel() {
        return patternlevel;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void rotate(double ccwAngleInDegrees) {
        double ccwAngle = Math.toRadians(ccwAngleInDegrees);
        applyTransform(AffineTransform.getRotateInstance(ccwAngle));
        name += "!r!" + ccwAngleInDegrees;
    }

    public void scale(double sx, double sy) {
        applyTransform(AffineTransform.getScaleInstance(sx, sy));
        name += "!s!" + sx + "!" + sy;
    }

    public void shear(double sx, double sy) {
        applyTransform(AffineTransform.getShearInstance(sx, sy));
        name += "!h!" + sx + "!" + sy;
    }

    private void applyTransform(AffineTransform at) {

        if (patternlevel != null) {
            patternlevel.applyTransform(new AffineTransform(at));
        }

        double[] fwd = new double[6];
        at.getMatrix(fwd);

        apply(transformed_right, fwd);
        apply(transformed_up, fwd);
        for (Polygon p : transformed) {
            for (Vector v : p.vertices()) {
                apply(v, fwd);
            }
        }

        if (transform == null) {
            transform = at;
        } else {
            at.concatenate(transform);
            transform = at;
        }

        inverse = new double[6];
        try {
            transform.createInverse().getMatrix(inverse);
        } catch (NoninvertibleTransformException ex) {
            Logger.getLogger(GridGeometry.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public int getPatternComplexity() {
        return boundaries.length;
    }

    public Polygon[] getOriginalBoundaries() {
        return boundaries;
    }

    public Polygon getOriginalBoundary(int i) {
        return boundaries[i];
    }

    public Polygon[] getTransformedBoundaries() {
        return transformed;
    }

    public Polygon getTransformedBoundary(int i) {
        return transformed[i];
    }

    public int norm(Coordinate a, Coordinate b) {
        return (int) Math.round(a.toVector().distanceTo(b.toVector()));
    }

    private static void apply(Vector v, double[] matrix) {
        if (matrix == null) {
            return;
        }
        double vx = v.getX();
        double vy = v.getY();
        double tvx = matrix[0] * vx + matrix[2] * vy + matrix[4];
        double tvy = matrix[1] * vx + matrix[3] * vy + matrix[5];
        v.set(tvx, tvy);
    }

    public Vector inverseTransformed(Vector v) {
        v = v.clone();
        apply(v, inverse);
        return v;
    }

    public Vector originalUp() {
        return up;
    }

    public Vector originalRight() {
        return right;
    }

    public Vector transformedUp() {
        return transformed_up;
    }

    public Vector transformedRight() {
        return transformed_right;
    }

    public Coordinate getContainingCell(Vector point, Coordinate origin) {
        return null;
    }

    public Coordinate[][][] getNeighborhoods(Coordinate origin) {
        return null;
    }

    public Coordinate[] getUnitVectors(Coordinate origin) {
        if (patternlevel == null) {
            return null;
        } else {
            Coordinate[] uvs = patternlevel.getUnitVectors(origin);
            if (uvs == null) {
                return null;
            } else {
                int pc = getPatternComplexity();
                for (int i = 0; i < uvs.length; i++) {
                    uvs[i] = origin.plus(uvs[i].x * pc, uvs[i].y);
                }
                return uvs;
            }
        }
    }

    public int getSelfRefinableFactor() {
        return Integer.MAX_VALUE;
    }

    public Coordinate[] getRefinementFor(Coordinate c, Coordinate origin) {
        return null;
    }
}
