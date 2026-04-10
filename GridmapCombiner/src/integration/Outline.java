/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package integration;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.LineSegment;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.util.DoubleUtil;

/**
 *
 * @author wmeulema
 */
public class Outline extends Polygon {

    public String label = null;
    public Vector labelPoint = null;
    public Color color = null;
    public List<Site> sites = new ArrayList();
    public List<Partition> partitions = new ArrayList();
    // for actual cartograms only
    public int customWeight = -1;

    public Outline() {
        super();
    }

    public Outline(Vector... vertices) {
        super(vertices);
    }

    public Outline(List<Vector> vertices) {
        super(vertices);
    }
    
    public void trivialPartition() {
        Partition p = new Partition(vertices());
        p.color = color;
        p.label = label;
        p.labelPoint = labelPoint;
        p.sites = sites;   
        p.outline = this;
        p.customWeight = customWeight;
        partitions.add(p);
    }
    
    public Partition addPartition() {
        Partition p = new Partition();
        partitions.add(p);
        return p;
    }
    
    public void ensureCCW() {
        if (areaSigned() < 0) {
            reverse();
        }
    }

    public void removeDegeneracies() {

        int i = 0;
        while (i < vertexCount()) {
            Vector u = vertex(i - 1);
            Vector v = vertex(i);
            Vector w = vertex(i + 1);

            LineSegment uw = new LineSegment(u, w);

            if (uw.onBoundary(v)) {
                removeVertex(i);
            } else {
                i++;
            }
        }
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
                Polygon triangle = new Polygon(vertex(leftmost-1),r, vertex(leftmost+1));
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
                    labelPoint = (new LineSegment(r,closest)).getPointAt(0.5);
                }
            }
        }
        return labelPoint;
    }
}
