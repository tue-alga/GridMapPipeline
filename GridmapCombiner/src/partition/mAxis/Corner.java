/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package partition.mAxis;

import partition.Cut;
import partition.PartitionPolygon;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.LineSegment;

/**
 *
 * @author msondag
 */
public class Corner {

    //The vertex on the corner
    public Vector cornerPoint;


    //all points that are equidistant over the medial axis
    public ArrayList<Vector> projectionPoints;

    /**
     * Automatically removes cornerPoint from projectionPoints
     *
     * @param cornerPoint
     * @param centerPoint
     * @param projectionPoints
     * @param convexCorner
     */
    public Corner(Vector cornerPoint, List<Vector> projectionPoints) {
        this.cornerPoint = cornerPoint;
        this.projectionPoints = new ArrayList();
        this.projectionPoints.addAll(projectionPoints);

        //remove the cornerpoint so we don't get empty cuts
        this.projectionPoints.remove(cornerPoint);

    }

    public Set<Cut> getCuts() {
        Set<Cut> cuts = new HashSet();
        for (Vector v : projectionPoints) {
            Cut c = new Cut(cornerPoint, v);
            cuts.add(c);
        }
        return cuts;
    }
}
