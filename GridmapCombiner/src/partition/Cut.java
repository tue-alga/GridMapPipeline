/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package partition;

import static partition.Precision.le;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.util.Pair;

/**
 *
 * @author msondag
 */
public class Cut {

    /**
     * Endpoints of the cut
     */
    public Vector start, end;
    /**
     * The partitionsegment representing the cut
     */
    public PartitionSegment segment;

    public double dilation = 0;

    public Cut(Vector start, Vector end) {
        this.start = start;
        this.end = end;
        segment = new PartitionSegment(start, end);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Cut other = (Cut) obj;

        double eps = 0.001; // = DoubleUtil.EPS;
        
        //cut must go over the same vertices, but inversion is allowed.
        boolean normalMatch = start.isApproximately(other.start, eps) && end.isApproximately(other.end, eps);
        boolean inverseMatch = end.isApproximately(other.start, eps) && start.isApproximately(other.end, eps);

        return normalMatch || inverseMatch;
    }

    /**
     * Invert the start and end segment of the cut.
     */
    public void invertSegment() {
        Vector temp = new Vector(start);
        start = new Vector(end);
        end = new Vector(temp);
        segment = new PartitionSegment(start, end);

    }

    /**
     * Return the length of the cut
     *
     * @return
     */
    public double getLength() {
        return start.distanceTo(end);
    }

    /**
     * Return and ipe-object representing the cut.
     *
     * @param layer
     * @return
     */
    public String getIpeString(String layer) {
        return "<path layer=\"" + layer + "\" stroke=\"blue\" cap=\"1\" join=\"1\">\n"
               + start.getX() + " " + start.getY() + " m\n"
               + end.getX() + " " + end.getY() + " l\n"
               + "</path>";
    }

    /**
     * Returns which segment the start/end-point of this cut lies on. Returns
     * null for the pair value if it does not start on any
     * segment.
     *
     * @param segments
     * @return
     */
    public Pair<PartitionSegment, PartitionSegment> getSegmentEndpoints(List<PartitionSegment> segments) {

        //will hold the closest linesegment to the startvector of c
        PartitionSegment startLs = null;
        double startDistance = Double.MAX_VALUE;//will hold the closest linesegment to the endVector of c

        PartitionSegment endLs = null;
        double endDistance = Double.MAX_VALUE;//will hold the closest linesegment to the endVector of c

        for (PartitionSegment ls : segments) {
            double newStartDistance = ls.distanceTo(start);
            double newEndDistance = ls.distanceTo(end);
            if (le(newStartDistance, startDistance)) {
                startDistance = newStartDistance;
                startLs = ls;
            }
            if (le(newEndDistance, endDistance)) {
                endDistance = newEndDistance;
                endLs = ls;
            }
        }
        return new Pair(startLs, endLs);
    }

    /**
     * Computes the dilation of this cut
     * @param p 
     */
    public void computeDilation(PartitionPolygon p) {
        //Inefficent calculation. Possible to speed up using data structure.
        
        
        double cutLength = getLength();

        //take the shortest distance between the two length
        double length1 = 0;
        double length2 = 0;
        boolean addingTo1 = true;
        for (PartitionSegment ps : p.getSegments()) {

            Vector onSegment = null;// holds the endpoint that is on this segment if any
            if (ps.onBoundary(start)) {
                onSegment = start;
            }
            if (ps.onBoundary(end)) {
                onSegment = end;
            }
            if (onSegment != null) {
                //if there is an endpoint on this segment, we only add part of the length, and start adding to the other
                if (!ps.getEnd().isApproximately(onSegment)) {
                    //we only do this if it is either in the middle of the segment, OR at the start.
                    //this it to prevent changing which distance to add on twice
                    double dis = ps.getStart().distanceTo(onSegment);
                    double remainingDis = ps.getEnd().distanceTo(onSegment);
                    if (addingTo1) {
                        length1 += dis;
                        length2 += remainingDis;
                    } else {
                        length2 += dis;
                        length1 += remainingDis;
                    }
                    //start adding to the other length.
                    addingTo1 = !addingTo1;
                }
            }

            if (addingTo1) {
                length1 += ps.length();
            } else {
                length2 += ps.length();
            }
        }

        double boundaryLength = Math.min(length1, length2);
        this.dilation = cutLength / boundaryLength;
    }

}
