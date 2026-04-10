package common;

import arrange.model.GuidingShape;
import common.gridmath.util.CoordinateSet;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.LineSegment;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.util.DoubleUtil;


/**
 *
 * @author Wouter Meulemans
 */
public class Partition extends Polygon {

    public String label = null;
    public Vector labelPoint = null;
    public Color color = null;
    public Outline outline;
    public List<Site> sites = new ArrayList();
    public CoordinateSet cells = null;
    public GuidingShape guide = null;
    // for actual cartograms only
    public int customWeight = -1;
    // cache
    private Vector centroid = null;
    private double signedArea = Double.NaN;

    public Partition() {
        super();
    }

    public Partition(Vector... vertices) {
        super(vertices);
    }

    public Partition(List<Vector> vertices) {
        super(vertices);
    }

    @Override
    public Vector centroid() {
        if (centroid == null) {
            centroid = super.centroid();
        }
        return centroid;
    }

    @Override
    public double areaSigned() {
        if (Double.isNaN(signedArea)) {
            signedArea = super.areaSigned();
        }
        return signedArea;
    }

    public int getWeight() {
        // abstracting this to weight, mostly such that it would be slightly easier to replace for running cartograms independently
        return customWeight > 0 ? customWeight : sites.size();
    }

    public Vector getLabelPoint() {
        if (labelPoint == null) {
            Vector c = centroid();
            if (contains(c, -0.01)) {
                // properly contained
                labelPoint = c;
            } else {
                // near boundary, let's find another point
                int leftmost = 0;
                for (int i = 1; i < vertexCount(); i++) {
                    if (vertex(leftmost).getX() > vertex(i).getX()) {
                        leftmost = i;
                    }
                }

                Vector r = vertex(leftmost);

                // leftmost is certainly a convex vertex
                Polygon triangle = new Polygon(vertex(leftmost - 1), r, vertex(leftmost + 1));
                Vector closest = null;
                for (int i = 0; i < vertexCount(); i++) {
                    Vector v = vertex(i);
                    if (triangle.contains(v, -DoubleUtil.EPS)) {
                        if (closest == null || v.distanceTo(r) < closest.distanceTo(r)) {
                            closest = v;
                        }
                    }
                }
                if (closest == null) {
                    labelPoint = triangle.centroid();
                } else {
                    labelPoint = (new LineSegment(r, closest)).getPointAt(0.5);
                }
            }
        }
        return labelPoint;
    }
}
