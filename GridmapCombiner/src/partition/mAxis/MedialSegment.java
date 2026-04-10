/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package partition.mAxis;

import partition.PartitionPolygon;
import partition.PartitionSegment;
import static partition.Precision.eq;
import static partition.Precision.le;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.LineSegment;

/**
 *
 * @author msondag
 */
public class MedialSegment extends LineSegment {



    /**
     * Holds whether this segment starts/ends on the polygon
     */
    protected boolean onPolygon = false;

    //Holds which two points this line is defined by
    protected Vector definingPoint1 = null;
    protected Vector definingPoint2 = null;
    //holds which two lines this point is defined by.
    protected LineSegment definingLine1 = null;
    protected LineSegment definingLine2 = null;

    /**
     * Only for exterior segments. Holds the point that is the furthest inside
     * the pocket.
     */
    protected Vector furthestPoint;

    /**
     * Line segment
     *
     * @param p1
     * @param p2
     */
    public MedialSegment(Vector p1, Vector p2) {
        super(p1, p2);
    }

    /**
     * Returns the oppositePoint point of p on this segment or an empty list if
     * it is not part of any. Returns a single point point, unless it is a
     * parabola.
     *
     * @pre p must lie on a definingGeometry of this medialSegment
     * @return
     */
    public List<Vector> getOppositePoints(Vector p) {
        assert (onDefiningGeometry(p));
        if (definingPoint1 != null) {
            //defined by two points
            if (p.isApproximately(definingPoint1)) {
                return Arrays.asList(definingPoint2);
            } else {
                return Arrays.asList(definingPoint1);
            }
        } else {
            //defined by two lines
            LineSegment pSegment;
            LineSegment otherSegment;
            if (definingLine1.onBoundary(p)) {
                pSegment = definingLine1;
                otherSegment = definingLine2;
            } else {
                pSegment = definingLine2;
                otherSegment = definingLine1;
            }

            if (otherSegment == null) {
                System.err.println("OtherSegment cannot be null. Might be due to precision or degeneracies");
                return null;
            }

            //gets this distance and direction of the seg
            Vector v = pSegment.getDirection();
            //get the correct distance
            double radius = distanceTo(p);
            v.scale(radius);
            //rotate v while it is still at the origin to a 90 degree angle towards the medial segment
            //need to rotate it such that the v points  towards the mediala xis from p.
            boolean rotateClockwise = v.computeSignedAngleTo(getDirection()) > 0;
            if (rotateClockwise) {
                v.rotate90DegreesClockwise();
            } else {
                v.rotate90DegreesCounterclockwise();

            }
            //translate it to p.
            v.translate(p);
            //v is now the center of the circle on the medial axis that hits p, find the oppositePoint
            return Arrays.asList(otherSegment.closestPoint(v));

        }
    }


    /**
     * Sets the defininglines/points for this segment, as well as exterioir
     * corner point and whether it lies on the boundary.
     *
     * @param polygon
     */
    public void addDefiningGeometries(PartitionPolygon polygon) {
        List<Vector> startProjectionPoints = getProjectionPoints(polygon, getStart());
        List<Vector> endProjectionPoints = getProjectionPoints(polygon, getEnd());

        if (startProjectionPoints.size() == 1) {
            //line formed by 2 points
            definingPoint1 = startProjectionPoints.get(0);
            definingPoint2 = endProjectionPoints.get(0);
            return;
        }

        //find the shared points
        ArrayList<Vector> sharedProjectionPoints = new ArrayList();
        for (Vector v1 : startProjectionPoints) {
            for (Vector v2 : endProjectionPoints) {
                if (v1.isApproximately(v2)) {
                    sharedProjectionPoints.add(v1);
                    break;
                }
            }
        }

        //if there are exactly two shared points.
        if (sharedProjectionPoints.size() == 2) {
            //line formed by 2 points
            definingPoint1 = sharedProjectionPoints.get(0);
            definingPoint2 = sharedProjectionPoints.get(1);
            return;
        }

        if (sharedProjectionPoints.size() == 1) {
            //line hits a vertex on the boundary, can treat it as a normal line segment
        }

        //lines formed by 2 line segments.
        for (PartitionSegment ps : polygon.getSegments()) {
            //go through the polygon, and find a segment where a point from both start and end are projected on.
            //There are exactly two such segments.
            for (Vector startVector : startProjectionPoints) {
                if (!ps.onBoundary(startVector)) {
                    continue;
                }
                //startvector on the segment, find the endbecor
                for (Vector endVector : endProjectionPoints) {
                    if (!(ps.onBoundary(endVector) && !endVector.isApproximately(startVector))) {
                        continue;
                    }

                    //both start and end on the same segment. Second check is there to handle precision errors.
                    //There is a node from both start and end on this vertex, thus ps is one side that defines this line segment
                    if (definingLine1 == null) {
                        definingLine1 = (new LineSegment(startVector, endVector));
                    } else {
                        //there are at most two lines for which this holds on the polygon
                        definingLine2 = (new LineSegment(startVector, endVector));
                    }
                }
            }
        }
    }

    public void setBoundary(PartitionPolygon polygon) {
        //set whether this segment lies on the polygon
        for (PartitionSegment ps : polygon.getSegments()) {
            if (ps.onBoundary(getStart()) || ps.onBoundary(getEnd())) {
                onPolygon = true;
                //set the furthest points for alpha convecity.
                if (ps.onBoundary(getStart())) {
                    furthestPoint = getStart();
                } else {
                    furthestPoint = getEnd();
                }
                break;
            }
        }
    }

    public boolean onDefiningGeometry(Vector p) {
        if (definingPoint1 != null && definingPoint1.isApproximately(p)) {
            return true;
        }
        if (definingPoint2 != null && definingPoint2.isApproximately(p)) {
            return true;
        }
        if (definingLine1 != null && (definingLine1.onBoundary(p) || definingLine1.getStart().isApproximately(p) || definingLine1.getEnd().isApproximately(p))) {
            return true;
        }
        if (definingLine2 != null && (definingLine2.onBoundary(p) || definingLine2.getStart().isApproximately(p) || definingLine2.getEnd().isApproximately(p))) {
            return true;
        }
        //not on any of the definingGeometries
        return false;
    }

    private List<Vector> removeDuplicates(List<Vector> closestPoints) {
        List<Vector> filteredList = new ArrayList();
        for (Vector v1 : closestPoints) {
            boolean alreadyPresent = false;
            for (Vector v2 : filteredList) {
                if (v1.isApproximately(v2, 0.001)) {
                    alreadyPresent = true;
                    break;
                }
            }
            if (!alreadyPresent) {
                filteredList.add(v1);
            }
        }
        return filteredList;
    }

    /**
     * Gets all projected points of base. Base should be on the medial axis.
     * Does not assume the definingGeometries are presetn
     *
     * @param polygon
     * @param base
     * @return
     */
    protected List<Vector> getProjectionPoints(PartitionPolygon polygon, Vector base) {
        double minDistance = Double.MAX_VALUE;
        List<Vector> closestPoints = new ArrayList();

        for (LineSegment ls : polygon.getSegments()) {
            Vector closestPoint = ls.closestPoint(base);

            double distance = base.distanceTo(closestPoint);
            //Be carefull with double precision here
            if (le(distance, minDistance)) {
                minDistance = distance;
                closestPoints.clear();
                closestPoints.add(closestPoint);
            } else if (eq(distance, minDistance)) {
                closestPoints.add(closestPoint);
            }
        }
        closestPoints = removeDuplicates(closestPoints);
        return closestPoints;
    }

}
