/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package partition.mAxis;

import partition.Cut;
import partition.PartitionPolygon;
import partition.Utility;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.LineSegment;

/**
 * Not a true medial axis class, just stores the minimal information required
 *
 * @author msondag
 */
public class MedialAxis {

    Set<Vector> vertices = new HashSet();
    List<MedialSegment> medialSegments = new ArrayList();
    PartitionPolygon polygon;

    public MedialAxis(PartitionPolygon polygon) {
        this.polygon = polygon;

        getMedialAxis();
        //System.out.println("Assigning opposties");
        assignOpposites();
        for (MedialSegment ms : medialSegments) {
            ms.setBoundary(polygon);
        }
    }

    private void getMedialAxis() {
        List<String> lines = executeCgal();
        //cgal returns interior and exterior medial axis. We only need the interior.
        //and discard the rest

        for (String line : lines) {
            //format for parabola: p x1 y1 x2 y2 cx cy dx dy o
            //format for line segment: l x1 y1 x2 y2
            //where p indicats a parabola, s a line segment
            //x1,y1 is the coordinate of the first point
            //x2,y2 is the coordinate of the second point
            //cx,cy is coordinate of point defining parabola
            //dx,dy,o define the infinite line of the parabola
            //format for infinite ray: r x1 y1 x2 y2
            //where r indicates a parabola
            //x1,y1 indicate the start of the ray
            //x2,y2 indicate the direction of the ray 
            MedialSegment ms;
            if (line.startsWith("p ")) {
                ms = parseParabola(line);
            } else if (line.startsWith("s ")) {
                ms = parseSegment(line);
            } else if (line.startsWith("r ")) {
                //ray segment. Must be exterior.
                ms = null;
            } else {
                //if is is not starting with any of these, it is not part of the medial axis output.
                System.err.println("Unexpected input for medial axis lines: " + line);
                continue;
            }

            if (ms == null) {
                //segment invalid or exterior. Sip it
                continue;
            }
            if (ms.getStart().isEqual(ms.getEnd())) {
                //don't do point segments
                //System.out.println("Point segment");
                continue;
            }

            medialSegments.add(ms);
            vertices.add(ms.getStart());
            vertices.add(ms.getEnd());
        }
    }

    private List<String> executeCgal() {
        //convert to the right format for cgal
        String polygonLocation = polygonToCin();

        //execute process and get data
        String fileLocation = "../precompiled/sdg-voronoi-edges.exe";

        List<String> command = Arrays.asList(fileLocation, polygonLocation);
       // System.out.println("starting cgal");
        String medialAxisString = Utility.executeCommandLine(command);
       // System.out.println("cgal done");

        //remove the temporary file
        File f = new File(polygonLocation);
        f.delete();

        List<String> returnString = Arrays.asList(medialAxisString.split("\n"));

        return returnString;
    }

    private String polygonToCin() {
        List<String> outputLines = new ArrayList();
        List<Vector> pVertices = polygon.getVertices();
        for (int i = 0; i < (pVertices.size() - 1); i++) {
            Vector v1 = pVertices.get(i);
            Vector v2 = pVertices.get(i + 1);
            outputLines.add(edgeToCin(v1, v2));
        }
        outputLines.add(edgeToCin(pVertices.get(pVertices.size() - 1), pVertices.get(0)));

        String tempFilePath = "temp.cls";
        try {
            Files.write(Paths.get(tempFilePath), outputLines);

        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(MedialAxis.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return tempFilePath;
    }

    private String edgeToCin(Vector v1, Vector v2) {
        return "s " + v1.getX() + " " + v1.getY() + " " + v2.getX() + " " + v2.getY();
    }

    private MedialSegment parseSegment(String line) {
        //LineSegment
        String[] split = line.split(" ");
        Vector p1 = new Vector(Double.parseDouble(split[1]), Double.parseDouble(split[2]));
        Vector p2 = new Vector(Double.parseDouble(split[3]), Double.parseDouble(split[4]));

        MedialSegment ms = new MedialSegment(p1, p2);
//        if (polygon.intersectedBySegment(ls)) {
//            System.err.println("Segment " + ls + " is intersecting the polygon. returning null.");
//            return null;
//        }

        //don't need the exterior medial axis. Quite slow to do it this way.
//        boolean interior = polygon.containsPoint(ms.getPointAlongPerimeter(0.01));
//        if (!interior) {
//            return null;
//        }
        return ms;
    }

    private MedialSegment parseParabola(String line) {
        String[] split = line.split(" ");
        Vector p1 = new Vector(Double.parseDouble(split[1]), Double.parseDouble(split[2]));
        Vector p2 = new Vector(Double.parseDouble(split[3]), Double.parseDouble(split[4]));
        Vector p3 = new Vector(Double.parseDouble(split[5]), Double.parseDouble(split[6]));

        //Not technically correct, but works in practice. Need to interpolate over the arc.
        LineSegment ls = new LineSegment(p1, p2);
//        boolean interior = polygon.containsPoint(ls.getPointAlongPerimeter(0.01));

        //don't need the exterior medial axis. Quite slow to do it this way.
//        if (!interior) {
//            return null;
//        }
        return new ParabolaMedialSegment(p1, p2, p3, split[7], split[8], split[9]);
    }

    public void assignOpposites() {
        for (MedialSegment ms : medialSegments) {
            ms.addDefiningGeometries(polygon);
        }
    }

    public void toIpe(String fileLocation) {
        MedialAxisToIpe maIpe = new MedialAxisToIpe(this);
        maIpe.toIpe(fileLocation);
    }

    public void toIpe(String fileLocation, List<Cut> rawCuts) {
        MedialAxisToIpe maIpe = new MedialAxisToIpe(this);
        maIpe.toIpe(fileLocation, rawCuts);
    }

    /**
     * Remove all segments from {@code medialSegments} that have a ending in a
     * leaf
     *
     * @param segments
     */
    public void trimLeafs() {
        Set<MedialSegment> toRemove = new HashSet();
        for (MedialSegment ms : medialSegments) {
            Vector v1 = ms.getStart();
            Vector v2 = ms.getEnd();
            for (Vector pv : polygon.getVertices()) {
                if (v1.isEqual(pv) || v2.isEqual(pv)) {
                    toRemove.add(ms);
                }
            }
        }
        medialSegments.removeAll(toRemove);
    }

    /**
     * Can be optimized immensely. Need to let medial axis store which edges of
     * the polygon it belong to. Assigns endspoints to medial segments
     *
     * @return
     */
    public List<Corner> getCorners() {

        List<Corner> corners = new ArrayList();
        List<Vector> cornerPoints = new ArrayList();

        boolean clockwise = polygon.isClockwise();

        //get all concave vertices
        List<Vector> vertices = polygon.getVertices();
        for (int i = 0; i < vertices.size(); i++) {
            //+vertices due to negative
            Vector v1 = vertices.get((i - 1 + vertices.size()) % vertices.size());
            Vector v2 = vertices.get(i);
            Vector v3 = vertices.get((i + 1) % vertices.size());

            if (!clockwise) {
                //go the other way around. Swap v1 and v3 around
                Vector v4 = v1;
                v1 = v3;
                v3 = v4;
            }

            Vector v1v2 = Vector.subtract(v2, v1);
            Vector v2v3 = Vector.subtract(v2, v3);

            double angle = v1v2.computeClockwiseAngleTo(v2v3);
            if (angle < (Math.PI)) {
                cornerPoints.add(v2);
            }
        }

        //remove duplicates
        cornerPoints = removeDuplicates(cornerPoints);

        //go over the cornerpoints, and for each interior medial segment, that has the vertex in the corner
        //as on of it's defening points, add the other point. These are potential cuts.
        for (Vector cp : cornerPoints) {
            for (MedialSegment ms : medialSegments) {
                if (ms.onDefiningGeometry(cp)) {
                    List<Vector> oppositePoints = ms.getOppositePoints(cp);

                    if (oppositePoints == null) {
                        //System.err.println("OppositePoints are only null with degenerate segments. Program will continue to run");
                        continue;
                    }

                    Corner c = new Corner(cp, oppositePoints);
                    corners.add(c);
                }
            }
        }
        return corners;
    }

    private List<Vector> removeDuplicates(List<Vector> points) {
        List<Vector> filteredList = new ArrayList();
        for (Vector v1 : points) {
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

}
