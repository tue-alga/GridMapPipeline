/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package partition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.util.Pair;

/**
 *
 * @author msondag
 */
class GraphStructure {

    List<Face> faces = new ArrayList();
    List<EdgeCut> edgeCuts = new ArrayList();

    HashMap<Cut, EdgeCut> cutMapping = new HashMap();

    public GraphStructure(List<PartitionPolygon> polygons, List<Cut> cuts, List<Site> sites) {

        addFaces(polygons);
        addEdges(cuts);
        addSiteCount(sites);

        addRelations();
    }

    public boolean isProductive(Cut newCut, Set<Cut> usedCuts, int productivityThreshold) {
        Set<EdgeCut> cutsProcessed = new HashSet();
        for (Cut uc : usedCuts) {
            cutsProcessed.add(cutMapping.get(uc));
        }
        cutsProcessed.add(cutMapping.get(newCut));

        EdgeCut ec = cutMapping.get(newCut);
        Face f1 = ec.face1;
        Face f2 = ec.face2;
        //count first side of the cut

        int countf1 = countProductiveSites(f1, cutsProcessed, productivityThreshold);
        if (countf1 < productivityThreshold) {
            return false;
        }
        //count second side of the cut
        int countf2 = countProductiveSites(f2, cutsProcessed, productivityThreshold);
        if (countf2 < productivityThreshold) {
            return false;
        }
        return true;
    }

    private void addFaces(List<PartitionPolygon> polygons) {
        for (PartitionPolygon p : polygons) {
            Face f = new Face(p);
            faces.add(f);
        }
    }

    private void addEdges(List<Cut> cuts) {
        for (Cut c : cuts) {
            EdgeCut e = new EdgeCut(c);
            edgeCuts.add(e);
            cutMapping.put(c, e);
        }
    }

    private void addSiteCount(List<Site> sites) {
        //count the amount of sites in each polygon
        for (Site s : sites) {
            for (Face f : faces) {
                if (f.vertices.size() < 3) {
                    //one of the cuts does not make sense.
                    continue;
                }
                if (f.containsPoint(s.point)) {
                    f.siteCount++;
                    break;//site can only be in one
                }
            }
        }
    }

    /**
     * Add the relations between the faces and the edgeCuts
     */
    private void addRelations() {
        for (Face f : faces) {
            for (EdgeCut ec : edgeCuts) {
                if (f.cutPartOfBoundary(ec)) {
                    f.addEdgeCut(ec);
                    ec.addFace(f);
                }
            }
        }
    }

    private int countProductiveSites(Face f, Set<EdgeCut> edgeCutsProcessed, int productivityThreshold) {
        int count = f.siteCount;
        if (count >= productivityThreshold) {
            return count;
        }

        for (EdgeCut ec : f.adjacentCuts) {
            if (!edgeCutsProcessed.contains(ec)) {
                //go over the cut and sum those
                Face otherFace = ec.getOtherFace(f);
                edgeCutsProcessed.add(ec);//make sure we don't go back over the cut

                count += countProductiveSites(otherFace, edgeCutsProcessed, productivityThreshold);
            }
        }

        return count;
    }

    private class Face {

        int siteCount = 0;
        Set<EdgeCut> adjacentCuts = new HashSet();
        List<PartitionSegment> segments;
        List<Vector> vertices;

        private Face(PartitionPolygon p) {
            this.segments = new ArrayList(p.segments);
            this.vertices = new ArrayList(p.vertices);
        }

        private boolean cutPartOfBoundary(EdgeCut ec) {
            for (PartitionSegment ps : segments) {
                if (ec.segment.hasSameEndpoints(ps)) {
                    return true;
                }
            }
            return false;
        }

        private void addEdgeCut(EdgeCut ec) {
            adjacentCuts.add(ec);
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
    }

    private class EdgeCut {

        Face face1;
        Face face2;
        PartitionSegment segment;

        private EdgeCut(Cut c) {
            this.segment = c.segment;
        }

        private void addFace(Face f) {
            if (face1 == null) {
                face1 = f;
            } else if (face2 == null) {
                face2 = f;
            } else {
                System.err.println("Face1 and face2 are both non-null, but an extra face needs to be added in EdgeCut.");
            }
        }

        private Face getOtherFace(Face f) {
            if (face1 == f) {
                return face2;
            } else {
                return face1;
            }
        }

    }

}
