/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package partition;

import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.LineSegment;

/**
 *
 * @author msondag
 */
public class PartitionSegment extends LineSegment {

    //Which polygon it is part of
    private PartitionPolygon p;

    /**
     * Initializes the partition segment without a partitionpolygon
     *
     * @param start
     * @param end
     */
    public PartitionSegment(Vector start, Vector end) {
        super(start, end);
        p = null;
    }

    /**
     * Initializes the partition segment without a partitionpolygon
     *
     * @param ls
     */
    public PartitionSegment(LineSegment ls) {
        super(ls.getStart(), ls.getEnd());
        p = null;
    }

    /**
     *
     * @param start
     * @param end
     */
    public PartitionSegment(Vector start, Vector end, PartitionPolygon p) {
        super(start, end);
        this.p = p;
    }

    /**
     * Splits the segment into two at the specified position
     *
     * @param position
     */
    public void splitSegment(Vector position) {
        Vector start = getStart();
        Vector end = getEnd();

        //split this segment
        PartitionSegment l1 = new PartitionSegment(start, position, p);
        PartitionSegment l2 = new PartitionSegment(position, end, p);

        //replace the segment with the two subsegments
        p.replaceSegment(this, l1, l2);
    }

    public void setPartitionPolygon(PartitionPolygon p) {
        this.p = p;
    }

    /**
     * Returns true if this segments contains p as an endpoint
     *
     * @param p
     * @return
     */
    boolean hasEndpoint(Vector p) {
        return (getStart().isApproximately(p) || getEnd().isApproximately(p));
    }

    /**
     * Returns true if this segment contains p1 or p2 as an endpoint
     *
     * @param p1
     * @param p2
     * @return
     */
    boolean isEndpoint(Vector p1, Vector p2) {
        return (getStart().isEqual(p1) || getEnd().isEqual(p1)
                || getStart().isEqual(p2) || getEnd().isEqual(p2));
    }

    /**
     * Returns the shared endpoint of the two segments, or null if it doesn't
     * exist
     *
     * @param segment
     * @return
     */
    public Vector getSharedEndpoint(PartitionSegment segment) {
        Vector start1 = getStart();
        Vector end1 = getEnd();

        Vector start2 = segment.getStart();
        Vector end2 = segment.getEnd();

        if (start1.isEqual(start2) || start1.isEqual(end2)) {
            return start1;
        }
        if (end1.isEqual(start2) || end1.isEqual(end2)) {
            return end1;
        }
        return null;

    }

    /**
     * The segment is allowed to be inverted to be inverted.
     *
     * @param ps
     * @return
     */
    public boolean hasSameEndpoints(PartitionSegment ps) {
        if (getStart().isApproximately(ps.getStart()) && getEnd().isApproximately(ps.getEnd())) {
            return true;
        }
        if (getEnd().isApproximately(ps.getStart()) && getStart().isApproximately(ps.getEnd())) {
            return true;
        }
        return false;
    }

    public boolean isApproxEndpoint(Vector v) {
        return (getStart().isApproximately(v) || getEnd().isApproximately(v));
    }

    public PartitionSegment copy() {
        return new PartitionSegment(new Vector(getStart()), new Vector(getEnd()));
    }

}
