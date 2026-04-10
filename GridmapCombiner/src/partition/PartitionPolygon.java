/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package partition;

import integration.Outline;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import nl.tue.geometrycore.geometry.BaseGeometry;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.LineSegment;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.util.Pair;

/**
 *
 * @author msondag
 */
public class PartitionPolygon {

    protected Outline outline;
    protected List<Site> sites;
    
    /**
     * Segments are in order
     */
    protected List<PartitionSegment> segments;

    /**
     * Vertices are in order. Can be either clockwise or counterclockwise.
     */
    protected List<Vector> vertices = new ArrayList();

    /**
     * A cut that is used to generate this polygon.
     */
    protected Cut cut1 = null;
    /**
     * The second cut that is used to generate this polygon.
     */
    protected Cut cut2 = null;

    /**
     * Makes the partition polygon from the segment. Assigns the segments to
     * this polygon. Segments have to be in order
     *
     * @param segments
     */
    public PartitionPolygon(List<PartitionSegment> segments) {
        super();
        initialize(segments);
    }

    public boolean isClockwise() {
        //find the vertex that has the lowest y and is the furthest on the right.
        //must be on the convex hull
        Vector lowestRight = vertices.get(0);
        for (Vector v : vertices) {
            if (v.getY() == lowestRight.getY()) {
                if (v.getX() > lowestRight.getX()) {
                    lowestRight = v;
                }
            }
            if (v.getY() < lowestRight.getY()) {
                lowestRight = v;
            }
        }
        int i = vertices.indexOf(lowestRight);
        //get the next and previous vertex
        Vector v1 = vertices.get((i - 1 + vertices.size()) % vertices.size());
        Vector v2 = lowestRight;
        Vector v3 = vertices.get((i + 1) % vertices.size());

        Vector v2v1 = Vector.subtract(v1, v2);
        Vector v2v3 = Vector.subtract(v3, v2);

        double crossProduct = Vector.crossProduct(v2v1, v2v3);
        return crossProduct > 0;
    }

    private void initialize(List<PartitionSegment> segments) {
        this.segments = segments;
        for (PartitionSegment ps : segments) {
            ps.setPartitionPolygon(this);
        }

        for (PartitionSegment ps : segments) {
            vertices.add(ps.getEnd());
        }

        if (vertices.size() < 3) {
            System.out.println("Not a proper polygon");
        }
    }

    /**
     * Will return two subpolygons of {@code p}, split along the line cut. Adds
     * extra vertices if the cut lies on an edge instead of a vertex
     *
     * @param c The cut over which we will split. Needs to be contained within
     *          the polygon.
     * @return
     */
    public Pair<PartitionPolygon, PartitionPolygon> splitPolygon(Cut c) {

        //add an extra vertex on the segments of the polygon if needed.
        splitOnCut(c);

        //start building the two polygons.
        List<PartitionSegment> p1Segments = new ArrayList();//segments of polygon 1
        List<PartitionSegment> p2Segments = new ArrayList();//segments of polygon 2

        //Whether we are currently adding to polygon 1
        boolean addingTo1 = true;

        for (PartitionSegment segment : segments) {
            Vector end = segment.getEnd();
            //add the segment to the list we are currently adding to
            if (addingTo1) {
                p1Segments.add(segment);
            } else {
                p2Segments.add(segment);
            }
            //if this segments ends at the cut, add the cut to both p1 and p2. Continue with the other one
            if (end.isApproximately(c.start) || end.isApproximately(c.end)) {
                if (end.isApproximately(c.end)) {
                    //make sure it is alligned in the correct direction
                    c.invertSegment();
                }

                if (addingTo1) {
                    p1Segments.add(c.segment);
                } else {
                    p2Segments.add(c.segment);
                }
                //continue with the other segment.
                addingTo1 = !addingTo1;
            }
        }

        //p1Segments and p2Segments now contains the segment for the polygons.
        PartitionPolygon p1 = new PartitionPolygon(p1Segments);
        PartitionPolygon p2 = new PartitionPolygon(p2Segments);
        return new Pair(p1, p2);
    }

    /**
     * polygon will have extra vertices at the start and end of the cut,
     * and the same for polygons adjacent to these edges to prevent precision
     * errors.
     *
     * @param c
     */
    private void splitOnCut(Cut c) {

        Pair<PartitionSegment, PartitionSegment> segmentEndPoints = c.getSegmentEndpoints(segments);
        PartitionSegment startLs = segmentEndPoints.getFirst();
        PartitionSegment endLs = segmentEndPoints.getSecond();

        //startLs and endLs now hold the closest linesegments. 
        //Add the vertices for the cut if they are not already on a vertex.
        if (!startLs.hasEndpoint(c.start)) {
            startLs.splitSegment(c.start);
        }
        if (!endLs.hasEndpoint(c.end)) {
            endLs.splitSegment(c.end);
        }
    }

    /**
     * Replaces a segment in the polygon with two segments.Called from
     * splitSegment
     *
     * @param original
     * @param l1
     * @param l2
     */
    public void replaceSegment(PartitionSegment original, PartitionSegment l1, PartitionSegment l2) {
        int originalIndex = segments.indexOf(original);
        if (originalIndex == -1) {
            return;
        }

        segments.remove(original);
        segments.add(originalIndex, l1);
        segments.add(originalIndex + 1, l2);

        //update the vertices
        //need to go manually through the vertices since equals method is not overridden for vector.
        int startIndex = -1;
        for (int i = 0; i < vertices.size(); i++) {
            Vector v = vertices.get(i);
            if (v.isApproximately(original.getStart())) {
                startIndex = i;
                break;
            }
        }

        vertices.add(startIndex + 1, l1.getEnd());//add it just after start

    }

    /**
     * Returns whether the cut c is inside this partition polygon
     *
     * @param c
     * @return
     */
    public boolean containsCut(Cut c) {
        //go halfway through the segment
        //and see whether that point lies inside the polygon enclosed by this polygon.
        PartitionSegment segment = c.segment;
        return containsPoint(segment.getPointAt(0.5));
    }

    /**
     * The list should not be changed
     *
     * @return
     */
    public List<Vector> getVertices() {
        return vertices;
    }

    /**
     * The list should not be changed
     *
     * @return
     */
    public List<PartitionSegment> getSegments() {
        return segments;
    }

    public boolean containsPoint(Vector point) {
        if (point.getX() == Double.POSITIVE_INFINITY || point.getX() == Double.NEGATIVE_INFINITY) {
            return false;
        }
        if (point.getY() == Double.POSITIVE_INFINITY || point.getY() == Double.NEGATIVE_INFINITY) {
            return false;
        }

        //can use direct implementation instead of object, but performance not instantiation not that much of an issue.
        Polygon p = new Polygon(vertices);
        return p.contains(point);
    }

    public boolean intersectedBySegment(LineSegment ls) {
        //TODO: Optimize by translating the contain method here.
        Polygon p = new Polygon(vertices);
        List<BaseGeometry> intersect = p.intersect(ls);

        //intersect must be at endpoint of linesegments
        for (BaseGeometry bg : intersect) {
            Vector intersectionPoint = (Vector) bg;
            if (intersectionPoint.isApproximately(ls.getStart()) || intersectionPoint.isApproximately(ls.getEnd())) {
                continue;
            }
            //intersection point is not at start or end, thus it is a proper intersection
            return true;
        }
        //no proper intersection found
        return false;
    }

    public String toIpe() {
        String ipeString = "";

        Color c = ColorPicker.getNewColor();

        Double red = ((double) c.getRed()) / 255.0;
        Double green = ((double) c.getGreen()) / 255.0;
        Double blue = ((double) c.getBlue()) / 255.0;

        String fill = "fill=\"" + red + " " + green + " " + blue + "\"";

        ipeString += "<path stroke=\"black\" " + fill + " pen=\"1.2\" cap=\"1\" join=\"1\">\n";

        Vector start = segments.get(0).getStart();
        ipeString += start.getX() + " " + start.getY() + " m\n";
        for (int i = 0; i < segments.size() - 1; i++) {
            //skip the last one as it ends in the start again.
            Vector end = segments.get(i).getEnd();
            double x = end.getX();
            double y = end.getY();
            ipeString += x + " " + y + " l\n";
        }
        ipeString += "h\n";
        ipeString += "</path>\n";
        return ipeString;
    }

    public boolean isInteriorSegment(LineSegment ls) {
        return containsPoint(ls.getPointAt(0.01));
    }

    /**
     * Returns the distance between the two vertices that are the furthest
     * apart.
     *
     * @return
     */
    public double getDiameter() {
        double diameter = 0;
        for (Vector v1 : getVertices()) {
            for (Vector v2 : getVertices()) {
                diameter = Math.max(diameter, v1.distanceTo(v2));
            }
        }
        return diameter;
    }

    public void removeDegeneracies() {

        List<Vector> degenerateVertices = new ArrayList();

        for (int i = 0; i < vertices.size(); i++) {
            //get three vertices on row, middle vertex should not be on the line from v1 to v3.
            Vector v1 = vertices.get(i);
            Vector v2 = vertices.get((i + 1) % vertices.size());
            Vector v3 = vertices.get((i + 2) % vertices.size());

            LineSegment v1v3 = new LineSegment(v1, v3);

            if (v1v3.onBoundary(v2)) {
                degenerateVertices.add(v2);
            }
        }

        vertices.removeAll(degenerateVertices);
        segments.clear();
        //redo the segment
        for (int i = 0; i < vertices.size(); i++) {
            Vector v1 = vertices.get(i);
            Vector v2 = vertices.get((i + 1) % vertices.size());
            PartitionSegment ps = new PartitionSegment(v1, v2, this);
            segments.add(ps);
        }
    }

    /**
     * Rounds down all coordinates such that we only use d-digit numbers
     *
     * @param i
     */
    public void fixedPrecision(int d) {
        for (PartitionSegment ps : segments) {
            Vector start = ps.getStart();
            Vector fixedStart = fixedPrecision(start, d);
            Vector end = ps.getEnd();
            Vector fixedEnd = fixedPrecision(end, d);
            ps.setStart(fixedStart);
            ps.setEnd(fixedEnd);
        }
        for (Vector v : vertices) {
            v.set(fixedPrecision(v, d));
        }
    }

    private Vector fixedPrecision(Vector v, int digits) {
        double factor = Math.pow(10, digits);
        double x = Math.round(factor * v.getX()) / factor;
        double y = Math.round(factor * v.getY()) / factor;
        return new Vector(x, y);
    }

    public double getMinY() {
        double minY = Double.MAX_VALUE;
        for (Vector v : vertices) {
            minY = Math.min(v.getY(), minY);
        }
        return minY;
    }

    public PartitionPolygon copy() {
        List<PartitionSegment> segmentsCopy = new ArrayList();
        for (PartitionSegment ps : segments) {
            segmentsCopy.add(ps.copy());
        }
        PartitionPolygon copy = new PartitionPolygon(segmentsCopy);
        for (PartitionSegment ps : segmentsCopy) {
            ps.setPartitionPolygon(copy);
        }
        return copy;
    }
}
