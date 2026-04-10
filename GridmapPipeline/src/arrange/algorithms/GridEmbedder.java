package arrange.algorithms;

import arrange.algorithms.sub.SchnyderWood;
import common.gridmath.GridMath.Coordinate;
import arrange.model.MosaicCartogram;
import arrange.algorithms.graph.Network;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import arrange.util.Utils;
import arrange.algorithms.graph.PlanarSubdivision;
import arrange.util.ConvexDecomposition;
import arrange.util.ElementList;
import common.dual.Dual;
import common.dual.Vertex;
import common.util.Stopwatch;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Rectangle;
import nl.tue.geometrycore.util.Pair;

/**
 * @author Rafael Cano
 */
public class GridEmbedder {

    // Class variables
    private final Dual dual;
    private final Network strongDual;
    private final PlanarSubdivision subdivision;
    private ElementList<PlanarSubdivision.Vertex> parent;
    private final ElementList<Integer> labels;
    private final ElementList<Integer> heights;
    private final ElementList<Integer> subtreeWidth;
    private final ElementList<Integer> subtreeOffset;
    private final ElementList<PlanarSubdivision.Vertex> preordering;
    // Extra vertices to form an outer triangular face
    private final PlanarSubdivision.Vertex top;
    private final PlanarSubdivision.Vertex left;
    private final PlanarSubdivision.Vertex right;

    public GridEmbedder(Dual dual) {
        this.dual = dual;

        this.strongDual = new Network(dual);
        addOuterFace();

        triangulateFaces();
        this.subdivision = new PlanarSubdivision(this.strongDual);
        if (!this.subdivision.isTriangulation()) {
            throw new RuntimeException("cannot triangulate input");
        }
        this.top = subdivision.getVertex(dual.vertexCount());
        this.left = subdivision.getVertex(dual.vertexCount() + 1);
        this.right = subdivision.getVertex(dual.vertexCount() + 2);
        this.parent = new ElementList<>(subdivision.numberOfVertices(), null);
        this.labels = new ElementList<>(subdivision.numberOfVertices(), null);
        this.heights = new ElementList<>(subdivision.numberOfVertices(), null);
        this.subtreeWidth = new ElementList<>(subdivision.numberOfVertices(), null);
        this.subtreeOffset = new ElementList<>(subdivision.numberOfVertices(), 0);
        this.preordering = new ElementList<>(subdivision.numberOfVertices());
    }

    private void initializeCartogram(MosaicCartogram cartogram) {

        final Coordinate origin = cartogram.getGridMath().origin();

        HashSet<Coordinate> blankCoordinates = new HashSet<>();

        // Vertical boundaries
        for (int id = 0; id < subdivision.numberOfVertices(); id++) {
            PlanarSubdivision.Vertex vSub = subdivision.getVertex(id);
            if (vSub == top) {
                continue;
            }
            PlanarSubdivision.Vertex pSub = parent.get(vSub);
            int parentHeight = heights.get(pSub);
            int height = heights.get(vSub);
            int width = subtreeWidth.get(vSub);
            int offset = subtreeOffset.get(vSub);
            for (int i = parentHeight; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    Coordinate c = origin.patternShift(j + offset, -i);
                    if (id < dual.vertexCount()) {
                        Vertex vDual = dual.getVertices().get(id);
                        setInCartogram(c, vDual, cartogram);
                    } else {
                        blankCoordinates.add(c);
                    }
                }
            }
        }

        // Horizontal Boudaries
        Coordinate[] occupied = cartogram.getCoordinateArray();
        for (Coordinate c : occupied) {
            if (c.variant() != 0) {
                continue;
            }
            Vertex v = cartogram.getVertex(c);
            Coordinate leftmost = c;
            do {
                leftmost = leftmost.patternLeft();
            } while (leftmost.x > 0 && cartogram.getVertex(leftmost) == null
                    && !blankCoordinates.contains(leftmost));
            if (leftmost.x > 0 && !blankCoordinates.contains(leftmost)) {
                Coordinate current = c.patternLeft();
                while (!current.equals(leftmost)) {
                    setInCartogram(current, v, cartogram);
                    current = current.patternLeft();
                }
            }
            Coordinate rightmost = c;
            do {
                rightmost = rightmost.patternRight();
            } while (rightmost.x < subtreeWidth.get(top) && cartogram.getVertex(rightmost) == null
                    && !blankCoordinates.contains(rightmost));
            if (rightmost.x < subtreeWidth.get(top) && !blankCoordinates.contains(rightmost)) {
                Coordinate current = c.patternRight();
                while (!current.equals(rightmost)) {
                    setInCartogram(current, v, cartogram);
                    current = current.patternRight();
                }
            }
        }

        assert cartogram.isConnectedAndCorrectAdjacencies();
    }

    private void setInCartogram(Coordinate c, Vertex v, MosaicCartogram cartogram) {

        final int pc = cartogram.getGridMath().getPatternComplexity();
        cartogram.setVertex(c, v);
        for (int k = 1; k < pc; k++) {
            cartogram.setVertex(c.patternVariant(k), v);
        }
    }

    /**
     * Computes an OST from a Schnyder wood.
     */
    private void computeOrderlySpanningTreeSchnyder() {
        SchnyderWood sw = new SchnyderWood(strongDual, subdivision);
        parent = sw.getParents();

        // Compute the counterclockwise preordering
        computeLabelsAndWidth(top);
    }

    private void addOuterFace() {
        Rectangle box = Rectangle.byBoundingBox(dual.getVertices());
        double minX = box.getLeft();
        double maxX = box.getRight();
        double minY = box.getBottom();
        double maxY = box.getTop();

        double width = Math.max(maxX - minX, 10);
        double height = Math.max(maxY - minY, 10);

        Vector p2 = new Vector(minX - width, minY - height / 2);
        Vector p3 = new Vector(maxX + width, minY - height / 2);
        Vector p2prime = new Vector(minX - width / 10, maxY);
        Vector p3prime = new Vector(maxX + width / 10, maxY);

        Vector p1 = Utils.lineIntersection(p2, p2prime, p3, p3prime);
        Network.Vertex v1 = strongDual.addVertex(p1);
        Network.Vertex v2 = strongDual.addVertex(p2);
        Network.Vertex v3 = strongDual.addVertex(p3);
        strongDual.addEdge(v1, v2);
        strongDual.addEdge(v1, v3);
        strongDual.addEdge(v2, v3);
        Network.Vertex[] extra = new Network.Vertex[]{v1, v2, v3};
        for (Network.Vertex u : extra) {
            for (int i = 0; i < strongDual.numberOfVertices() - 3; i++) {
                Network.Vertex v = strongDual.getVertex(i);
                if (visible(u, v)) {
                    strongDual.addEdge(u, v);
                }
            }
        }
    }

    private void triangulateFaces() {
        PlanarSubdivision preliminary = new PlanarSubdivision(strongDual);
        ArrayList<Network.Vertex> faceVertices = new ArrayList<>();
        for (PlanarSubdivision.Face face : preliminary.boundedFaces()) {
            if (face.numberOfSides() > 3) {
                ConvexDecomposition cd = new ConvexDecomposition(face.getBoundaryVertices());
                for (ArrayList<PlanarSubdivision.Vertex> polygon : cd.polygons()) {
                    Vector center = Utils.meanPosition(polygon);
                    Network.Vertex newVertex = strongDual.addVertex(center);
                    faceVertices.add(newVertex);
                    for (PlanarSubdivision.Vertex psV : polygon) {
                        Network.Vertex nV = strongDual.getVertex(psV.getId());
                        strongDual.addEdge(newVertex, nV);
                    }
                }
                for (Pair<PlanarSubdivision.Vertex, PlanarSubdivision.Vertex> edge : cd.edges()) {
                    PlanarSubdivision.Vertex psV1 = edge.getFirst();
                    PlanarSubdivision.Vertex psV2 = edge.getSecond();
                    Vector center = Vector.interpolate(psV1.getPosition(), psV2.getPosition(), 0.5);
                    Network.Vertex u = strongDual.addVertex(center);
                    Network.Vertex nV1 = strongDual.getVertex(psV1.getId());
                    Network.Vertex nV2 = strongDual.getVertex(psV2.getId());
                    strongDual.addEdge(u, nV1);
                    strongDual.addEdge(u, nV2);
                    for (Network.Vertex v : faceVertices) {
                        if (visible(u, v)) {
                            strongDual.addEdge(u, v);
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns true if u and v are visible to each other, false otherwise. Not
     * the best implementation, but will do here.
     */
    private boolean visible(Network.Vertex u, Network.Vertex v) {
        for (Network.Edge e : strongDual.edges()) {
            Network.Vertex es = e.getSource();
            Network.Vertex et = e.getTarget();
            if (es != u && et != u && es != v && et != v) {
                if (Utils.lineSegmentIntersection(u.getPosition(), v.getPosition(), es.getPosition(), et.getPosition()) != null) {
                    return false;
                }
            }
        }
        return true;
    }

    private PlanarSubdivision.Vertex cw(PlanarSubdivision.Vertex u, PlanarSubdivision.Vertex v) {
        List<? extends PlanarSubdivision.Halfedge> halfedges = v.getOutgoingHalfedges();
        int size = halfedges.size();
        for (int i = 0; i < size; i++) {
            PlanarSubdivision.Halfedge h = halfedges.get(i);
            if (h.getTarget() == u) {
                return halfedges.get((i + 1) % size).getTarget();
            }
        }
        throw new RuntimeException();
        //return null;
    }

    private PlanarSubdivision.Vertex ccw(PlanarSubdivision.Vertex u, PlanarSubdivision.Vertex v) {
        List<? extends PlanarSubdivision.Halfedge> halfedges = v.getOutgoingHalfedges();
        int size = halfedges.size();
        for (int i = 0; i < size; i++) {
            PlanarSubdivision.Halfedge h = halfedges.get(i);
            if (h.getTarget() == u) {
                return halfedges.get((i + size - 1) % size).getTarget();
            }
        }
        throw new RuntimeException();
        //return null;
    }

    private PlanarSubdivision.Vertex l(PlanarSubdivision.Vertex v) {
        List<? extends PlanarSubdivision.Halfedge> outgoing = v.getOutgoingHalfedges();
        int size = outgoing.size();
        PlanarSubdivision.Vertex p = parent.get(v);
        if (p != null) {
            int start = -1;
            for (int i = 0; i < size; i++) {
                PlanarSubdivision.Halfedge h = outgoing.get(i);
                if (h.getTarget() == p) {
                    start = i;
                    break;
                }
            }
            if (start == -1) {
                throw new RuntimeException();
            }
            int j = start;
            do {
                j = (j - 1 + size) % size;
            } while (j != start && labels.get(outgoing.get(j).getTarget()) < labels.get(v));
            j = (j + 1) % size;
            if (j == start) {
                return null;
            }
            return outgoing.get(j).getTarget();
        }
        System.out.println(labels.get(v) + " " + v.getPosition());
        throw new RuntimeException();
    }

    private PlanarSubdivision.Vertex r(PlanarSubdivision.Vertex v) {
        List<? extends PlanarSubdivision.Halfedge> outgoing = v.getOutgoingHalfedges();
        int size = outgoing.size();
        PlanarSubdivision.Vertex p = parent.get(v);
        if (p != null) {
            int start = -1;
            for (int i = 0; i < size; i++) {
                PlanarSubdivision.Halfedge h = outgoing.get(i);
                if (h.getTarget() == p) {
                    start = i;
                    break;
                }
            }
            int j = start;
            do {
                j = (j + 1) % size;
            } while (j != start && labels.get(outgoing.get(j).getTarget()) > labels.get(v)
                    && parent.get(outgoing.get(j).getTarget()) != v);
            j = (j - 1 + size) % size;
            if (j == start) {
                return null;
            }
            return outgoing.get(j).getTarget();
        }
        throw new RuntimeException();
    }

    private int computeLabelsAndWidth(PlanarSubdivision.Vertex u) {
        labels.set(u, preordering.size());
        preordering.add(u);
        int width = 0;
        PlanarSubdivision.Vertex p = parent.get(u);
        List<? extends PlanarSubdivision.Halfedge> outgoing = u.getOutgoingHalfedges();
        int size = outgoing.size();
        if (p == null) {
            int start = -1;
            for (int i = 0; i < size; i++) {
                PlanarSubdivision.Halfedge h = outgoing.get(i);
                if (h.getTarget() == left) {
                    start = i;
                    break;
                }
            }
            int j = start;
            do {
                PlanarSubdivision.Vertex v = outgoing.get(j).getTarget();
                subtreeOffset.set(v, width);
                width += computeLabelsAndWidth(v);
                j = (j - 1 + size) % size;
            } while (j != start);
        } else {
            int start = -1;
            for (int i = 0; i < size; i++) {
                PlanarSubdivision.Halfedge h = outgoing.get(i);
                if (h.getTarget() == p) {
                    start = i;
                    break;
                }
            }
            int j = start;
            do {
                j = (j - 1 + size) % size;
            } while (j != start && parent.get(outgoing.get(j).getTarget()) != u);
            if (j != start) {
                // Not a leaf, call recursive procedure
                do {
                    PlanarSubdivision.Vertex v = outgoing.get(j).getTarget();
                    subtreeOffset.set(v, width + subtreeOffset.get(u));
                    width += computeLabelsAndWidth(v);
                    j = (j - 1 + size) % size;
                } while (parent.get(outgoing.get(j).getTarget()) == u);
            }
        }
        width = Math.max(width, 1);
        subtreeWidth.set(u, width);
        return width;
    }

    private void computeHeights() {
        new HeightRecurrence().compute();
    }

    public void run(MosaicCartogram cartogram) {
        
        Stopwatch sw = Stopwatch.get("- embedder").start();
        
        computeOrderlySpanningTreeSchnyder();
        computeHeights();
        initializeCartogram(cartogram);
        
        sw.stop();
    }

    private class HeightRecurrence {

        private ElementList<Integer> yTable;
        private ElementList<ElementList<Integer>> y2Table;

        public HeightRecurrence() {
            yTable = new ElementList<>(subdivision.numberOfVertices(), null);
            y2Table = new ElementList<>();
            for (int i = 0; i < subdivision.numberOfVertices(); i++) {
                y2Table.add(new ElementList<Integer>(subdivision.numberOfVertices(), null));
            }
        }

        public void compute() {
            for (PlanarSubdivision.Vertex v : preordering) {
                heights.set(v, y(v));
            }
        }

        public int y(PlanarSubdivision.Vertex v) {
            if (yTable.get(v) != null) {
                return yTable.get(v);
            } else {
                if (labels.get(v) == 0) {
                    yTable.set(v, 1);
                    return 1;
                } else {
                    PlanarSubdivision.Vertex lv = l(v);
                    PlanarSubdivision.Vertex rv = r(v);
                    int vl = -1;
                    int vr = -1;
                    if (lv != null) {
                        vl = y(lv, v);
                    }
                    if (rv != null) {
                        vr = y(v, rv);
                    }
                    int value = Math.max(vl, vr);
                    yTable.set(v, value);
                    return value;
                }
            }
        }

        public int y(PlanarSubdivision.Vertex vi, PlanarSubdivision.Vertex vj) {
            if (y2Table.get(vi).get(vj) != null) {
                return y2Table.get(vi).get(vj);
            } else {
                int value = 1 + Math.max(yl(vi, vj), yr(vi, vj));
                y2Table.get(vi).set(vj, value);
                y2Table.get(vj).set(vi, value);
                return value;
            }
        }

        public int yl(PlanarSubdivision.Vertex vi, PlanarSubdivision.Vertex vj) {
            PlanarSubdivision.Vertex vjprime = ccw(vj, vi);
            if (parent.get(vi) == vjprime) {
                return y(vjprime);
            } else {
                return y(vi, vjprime);
            }
        }

        public int yr(PlanarSubdivision.Vertex vi, PlanarSubdivision.Vertex vj) {
            PlanarSubdivision.Vertex viprime = cw(vi, vj);
            if (parent.get(vj) == viprime) {
                return y(viprime);
            } else {
                return y(viprime, vj);
            }
        }
    }

}
