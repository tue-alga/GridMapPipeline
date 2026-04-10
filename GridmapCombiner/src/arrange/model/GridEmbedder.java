package arrange.model;

import arrange.model.Cartogram.MosaicCartogram;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import arrange.Utils.Utils;
import arrange.model.HexagonalMap.BarycentricCoordinate;
import arrange.model.Cartogram.MosaicCartogram.Coordinate;
import arrange.model.SquareMap.EuclideanCoordinate;
import arrange.model.graph.CrossingFinder;
import arrange.model.subdivision.Map;
import arrange.model.subdivision.PlanarSubdivision;
import arrange.model.subdivision.PlanarSubdivisionAlgorithms;
import arrange.model.util.CircularListIterator;
import arrange.model.util.ConvexDecomposition;
import arrange.model.util.ElementList;
import arrange.model.util.IpeExporter;
import arrange.model.util.Pair;
import arrange.model.util.Vector2D;

/**
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class GridEmbedder {

    // Class variables
    private final Network originalGraph;
    private final Network graph;
    private final Map map;
    private final PlanarSubdivision subdivision;
    private final ElementList<Boolean> deleted;
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
    // Variables to visualize the OST algorithm
    private static final boolean DRAW_STEPS = false;
    private ElementList<Boolean> deletedEdge;
    private int stepCounter;

    public GridEmbedder(Map map, Network originalGraph) {
//        Map.Face greece = map.getFace("GRC");
//        if (greece != null) {
//            System.out.println("GRC id = " + greece.getId());
//            for (Map.Halfedge h : greece.getBoundaryHalfedges()) {
//                Map.Face neighbour = h.getTwin().getFace();
//                if (neighbour.getLabel() != null) {
//                    System.out.println(neighbour);
//                }
//            }
//        }
        this.originalGraph = originalGraph;

//        debug only
//        IpeExporter.exportGraph(originalGraph, "dual.ipe");
        this.graph = new Network(originalGraph);
        this.map = map;
        addOuterFace();

        triangulateFaces();
        this.subdivision = new PlanarSubdivision(this.graph);
        //        debug only
//        IpeExporter.exportGraph(this.graph, "graph.ipe");
        if (!PlanarSubdivisionAlgorithms.isTriangulation(this.subdivision)) {
            //        debug only
//            IpeExporter.exportGraph(this.graph, "graph.ipe");
            throw new RuntimeException("cannot triangulate input");
        }
        this.top = subdivision.getVertex(originalGraph.numberOfVertices());
        this.left = subdivision.getVertex(originalGraph.numberOfVertices() + 1);
        this.right = subdivision.getVertex(originalGraph.numberOfVertices() + 2);
        this.parent = new ElementList<>(subdivision.numberOfVertices(), null);
        this.deleted = new ElementList<>(subdivision.numberOfVertices(), false);
        this.labels = new ElementList<>(subdivision.numberOfVertices(), null);
        this.heights = new ElementList<>(subdivision.numberOfVertices(), null);
        this.subtreeWidth = new ElementList<>(subdivision.numberOfVertices(), null);
        this.subtreeOffset = new ElementList<>(subdivision.numberOfVertices(), 0);
        this.preordering = new ElementList<>(subdivision.numberOfVertices());
//        computeOrderlySpanningTreeSchnyder();
//        for (int i = 1; i < preordering.numberOfCells(); i++) {
//            PlanarSubdivision.Vertex v = preordering.getVertex(i);
//            PlanarSubdivision.Vertex p = parent.getVertex(v);
//            String nameV;
//            String nameP;
//            if (v.getId() < map.numberOfBoundedFaces()) {
//                nameV = map.getFace(v.getId()).getLabel().getText();
//            } else {
//                nameV = v.getPosition().toString();
//            }
//            if (p.getId() < map.numberOfBoundedFaces()) {
//                nameP = map.getFace(parent.getVertex(v).getId()).getLabel().getText();
//            } else {
//                nameP = p.getPosition().toString();
//            }
//            System.out.println(i + ": " + nameV + ", parent = " + nameP);
//        }
//        if (true) {
//            return;
//        }
    }

    public void initializeCartogram(MosaicCartogram cartogram) {
        if (cartogram instanceof HexagonalMap) {
            initializeHexagonalCartogram((HexagonalMap) cartogram);
        } else if (cartogram instanceof SquareMap) {
            initializeSquareCartogram((SquareMap) cartogram);
        } else {
            throw new RuntimeException("Embedder not implemented for this type of grid");
        }
    }

    private void initializeHexagonalCartogram(HexagonalMap cartogram) {
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
                    BarycentricCoordinate c = new BarycentricCoordinate(j + offset, 0, -i);
                    if (id < originalGraph.numberOfVertices()) {
                        Network.Vertex vDual = originalGraph.getVertex(id);
                        cartogram.setVertex(c, vDual);
                    } else {
                        blankCoordinates.add(c);
                    }
                }
            }
        }

        // Horizontal Boudaries
        BarycentricCoordinate[] occupied = cartogram.getCoordinateArray();
        for (BarycentricCoordinate c : occupied) {
            Network.Vertex v = cartogram.getVertex(c);
            BarycentricCoordinate unitMove = new BarycentricCoordinate(1, 0, 0);
            BarycentricCoordinate leftmost = c;
            do {
                leftmost = leftmost.minus(unitMove);
            } while (leftmost.getX() > 0 && cartogram.getVertex(leftmost) == null
                    && !blankCoordinates.contains(leftmost));
            if (leftmost.getX() > 0 && !blankCoordinates.contains(leftmost)) {
                Coordinate current = c.minus(unitMove);
                while (!current.equals(leftmost)) {
                    cartogram.setVertex(current, v);
                    current = current.minus(unitMove);
                }
            }
            BarycentricCoordinate rightmost = c;
            do {
                rightmost = rightmost.plus(unitMove);
            } while (rightmost.getX() < subtreeWidth.get(top) && cartogram.getVertex(rightmost) == null
                    && !blankCoordinates.contains(rightmost));
            if (rightmost.getX() < subtreeWidth.get(top) && !blankCoordinates.contains(rightmost)) {
                Coordinate current = c.plus(unitMove);
                while (!current.equals(rightmost)) {
                    cartogram.setVertex(current, v);
                    current = current.plus(unitMove);
                }
            }
        }

        if (cartogram.isValid()) {
            //System.out.println("Hurrah!");
        } else {
            IpeExporter.exportCells(cartogram.cells(), "bad-cartogram.ipe");
            throw new RuntimeException("Initialization of cartogram failed");
        }
    }

    private void initializeSquareCartogram(SquareMap cartogram) {
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
                    EuclideanCoordinate c = new EuclideanCoordinate(j + offset, -i);
                    if (id < originalGraph.numberOfVertices()) {
                        Network.Vertex vDual = originalGraph.getVertex(id);
                        cartogram.setVertex(c, vDual);
                    } else {
                        blankCoordinates.add(c);
                    }
                }
            }
        }

        // Horizontal Boudaries
        EuclideanCoordinate[] occupied = cartogram.getCoordinateArray();
        for (EuclideanCoordinate c : occupied) {
            Network.Vertex v = cartogram.getVertex(c);
            EuclideanCoordinate unitMove = new EuclideanCoordinate(1, 0);
            EuclideanCoordinate leftmost = c;
            do {
                leftmost = leftmost.minus(unitMove);
            } while (leftmost.getX() > 0 && cartogram.getVertex(leftmost) == null
                    && !blankCoordinates.contains(leftmost));
            if (leftmost.getX() > 0 && !blankCoordinates.contains(leftmost)) {
                Coordinate current = c.minus(unitMove);
                while (!current.equals(leftmost)) {
                    cartogram.setVertex(current, v);
                    current = current.minus(unitMove);
                }
            }
            EuclideanCoordinate rightmost = c;
            do {
                rightmost = rightmost.plus(unitMove);
            } while (rightmost.getX() < subtreeWidth.get(top) && cartogram.getVertex(rightmost) == null
                    && !blankCoordinates.contains(rightmost));
            if (rightmost.getX() < subtreeWidth.get(top) && !blankCoordinates.contains(rightmost)) {
                Coordinate current = c.plus(unitMove);
                while (!current.equals(rightmost)) {
                    cartogram.setVertex(current, v);
                    current = current.plus(unitMove);
                }
            }
        }

        assert cartogram.isValid();
//        if (cartogram.isValid()) {
//            System.out.println("Hurrah!");
//        } else {
//            IpeExporter.exportCells(cartogram.cells(), "bad-cartogram.ipe");
//            throw new RuntimeException("Initialization of cartogram failed");
//        }
    }

    public void computeHeights() {
        pComputeHeights();
//        for (int i = 0; i < preordering.numberOfCells(); i++) {
//            PlanarSubdivision.Vertex v = preordering.getVertex(i);
//            System.out.println(i + ": " + nodeName(v) + " -> height = " + heights.getVertex(i));
//        }
    }

    public Network getModifiedGraph() {
        return graph;
    }

    /**
     * Implementation of the algorithm described in "Orderly Spanning Trees with
     * Applications", Chiang et al (2005). Do not refer to the short version of
     * the paper, the algorithm described there is wrong!
     */
    public void computeOrderlySpanningTreeChiang() {
        /////////////////////////// REMOVE //////////////////////////////////
        for (PlanarSubdivision.Vertex v : subdivision.vertices()) {
            v.name = nodeName(v);
        }
        /////////////////////////////////////////////////////////////////////
        // Initialize data
        LinkedList<PlanarSubdivision.Vertex> boundary = new LinkedList<>();
        boundary.add(top);
        boundary.add(left);
        boundary.add(right);

        if (DRAW_STEPS) {
            deletedEdge = new ElementList<>(graph.numberOfEdges(), false);
            stepCounter = 0;
            exportState();
        }

        // Execute magical function that I don't quite understand
        block(top, left, boundary);

        // Compute the counterclockwise preordering
        computeLabelsAndWidth(top);
//        //debuf only
//        exportOrderlySpanningTree();
        if (!ostIsValid()) {
            throw new RuntimeException("Invalid OST!!!");
        }
//        for (int i = 0; i < preordering.numberOfCells(); i++) {
//            PlanarSubdivision.Vertex v = preordering.getVertex(i);
//            PlanarSubdivision.Vertex p = parent.getVertex(v);
//            System.out.println(i + ": " + nodeName(v) + ", parent = " + nodeName(p));
//        }
    }

    /**
     * Computes an OST from a Schnyder wood.
     */
    public void computeOrderlySpanningTreeSchnyder() {
//        computeOrderlySpanningTreeChiang();
//        if (true) {
//            return;
//        }
        SchnyderWood sw = new SchnyderWood(graph, subdivision);
        parent = sw.getParents();

        // Compute the counterclockwise preordering
        computeLabelsAndWidth(top);
//        //debug only
//        exportOrderlySpanningTree();
        if (!ostIsValid()) {
            throw new RuntimeException("Invalid OST!!!");
        }
//        for (int i = 0; i < preordering.numberOfCells(); i++) {
//            PlanarSubdivision.Vertex v = preordering.getVertex(i);
//            PlanarSubdivision.Vertex p = parent.getVertex(v);
//            System.out.println(i + ": " + nodeName(v) + ", parent = " + nodeName(p));
//        }
    }

    private void exportOrderlySpanningTree() {
        Network tree = new Network(subdivision.numberOfVertices());
        for (int i = 0; i < subdivision.numberOfVertices(); i++) {
            tree.getVertex(i).setPosition(new Vector2D(subdivision.getVertex(i).getPosition()));
            if (i < originalGraph.numberOfVertices()) {
                tree.getVertex(i).setColor(map.getFace(i).getColor());
            }
            PlanarSubdivision.Vertex p = parent.get(i);
            if (p != null) {
                tree.addEdge(i, p.getId());
            }
        }
        IpeExporter.exportGraph(tree, "ost.ipe");
    }

    private void addOuterFace() {
        Pair<Vector2D, Vector2D> box = boundingBox();
        double minX = box.getFirst().getX();
        double maxX = box.getSecond().getX();
        double minY = box.getFirst().getY();
        double maxY = box.getSecond().getY();
        double width = Math.max(maxX - minX, 10);
        double height = Math.max(maxY - minY, 10);
        Vector2D p2 = new Vector2D(minX - width, minY - height / 2);
        Vector2D p3 = new Vector2D(maxX + width, minY - height / 2);
        Vector2D p2prime = new Vector2D(minX - width / 10, maxY);
        Vector2D p3prime = new Vector2D(maxX + width / 10, maxY);
        Vector2D p1 = Utils.lineIntersection(p2, p2prime, p3, p3prime);
        Network.Vertex v1 = graph.addVertex(p1);
        Network.Vertex v2 = graph.addVertex(p2);
        Network.Vertex v3 = graph.addVertex(p3);
        graph.addEdge(v1, v2);
        graph.addEdge(v1, v3);
        graph.addEdge(v2, v3);
        Network.Vertex[] extra = new Network.Vertex[]{v1, v2, v3};
        for (Network.Vertex u : extra) {
            for (int i = 0; i < graph.numberOfVertices() - 3; i++) {
                Network.Vertex v = graph.getVertex(i);
                if (visible(u, v)) {
                    graph.addEdge(u, v);
                }
            }
        }
    }

    private void triangulateFaces() {
        PlanarSubdivision preliminary = new PlanarSubdivision(graph);
        ArrayList<Network.Vertex> faceVertices = new ArrayList<>();
        for (PlanarSubdivision.Face face : preliminary.boundedFaces()) {
            if (face.numberOfSides() > 3) {
                ConvexDecomposition<PlanarSubdivision.Vertex> cd = new ConvexDecomposition<>(face.getBoundaryVertices());
                for (ArrayList<PlanarSubdivision.Vertex> polygon : cd.polygons()) {
                    Vector2D center = Utils.meanPosition(polygon);
                    Network.Vertex newVertex = graph.addVertex(center);
                    faceVertices.add(newVertex);
                    for (PlanarSubdivision.Vertex psV : polygon) {
                        Network.Vertex nV = graph.getVertex(psV.getId());
                        graph.addEdge(newVertex, nV);
                    }
                }
                for (Pair<PlanarSubdivision.Vertex, PlanarSubdivision.Vertex> edge : cd.edges()) {
                    PlanarSubdivision.Vertex psV1 = edge.getFirst();
                    PlanarSubdivision.Vertex psV2 = edge.getSecond();
                    Vector2D center = Vector2D.sum(psV1.getPosition(), psV2.getPosition());
                    center.multiply(0.5);
                    Network.Vertex u = graph.addVertex(center);
                    Network.Vertex nV1 = graph.getVertex(psV1.getId());
                    Network.Vertex nV2 = graph.getVertex(psV2.getId());
                    graph.addEdge(u, nV1);
                    graph.addEdge(u, nV2);
                    for (Network.Vertex v : faceVertices) {
                        if (visible(u, v)) {
                            graph.addEdge(u, v);
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
        for (Network.Edge e : graph.edges()) {
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

    private Pair<Vector2D, Vector2D> boundingBox() {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (Network.Vertex v : graph.vertices()) {
            double x = v.getPosition().getX();
            double y = v.getPosition().getY();
            if (x < minX) {
                minX = x;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (y > maxY) {
                maxY = y;
            }
        }
        return new Pair<>(new Vector2D(minX, minY), new Vector2D(maxX, maxY));
    }

    /**
     * Boundary should be the list of external vertices in counterclockwise
     * order starting from r.
     */
    private void block(PlanarSubdivision.Vertex r, PlanarSubdivision.Vertex v,
            LinkedList<PlanarSubdivision.Vertex> boundary) {
        // Find parent of v, next(G, v) and prev(G, v)
        PlanarSubdivision.Vertex p = r;
        PlanarSubdivision.Vertex prevV;
        PlanarSubdivision.Vertex nextV;
        {
            CircularListIterator<PlanarSubdivision.Vertex> cit = new CircularListIterator<>(boundary);
            while (p != v && subdivision.getHalfedge(v, p) == null) {
                p = cit.previous();
            }
            if (p == v) {
                throw new RuntimeException();
            }
            while (cit.previous() != v);
            prevV = cit.previous();
            cit.next();
            cit.next();
            nextV = cit.next();
        }
        parent.set(v, p);
        deleted.set(v, true);

        if (DRAW_STEPS) {
            Network.Vertex nv = graph.getVertex(v.getId());
            PlanarSubdivision.Vertex current = nextV;
            while (current != prevV) {
                Network.Vertex nu = graph.getVertex(current.getId());
                Network.Edge e = graph.getEdge(nv, nu);
                deletedEdge.set(e, true);
                current = ccw(current, v);
            }
            Network.Vertex nu = graph.getVertex(current.getId());
            Network.Edge e = graph.getEdge(nv, nu);
            deletedEdge.set(e, true);
            exportState();
        }

        // Find the vertices on the same side as prev(G,v) that will be part of
        // the boundary when v is deleted
        LinkedList<PlanarSubdivision.Vertex> leftBoundary = new LinkedList<>();
        ElementList<Integer> leftCount = new ElementList<>(subdivision.numberOfVertices(), 0);
        {
            if (p != prevV) {
                PlanarSubdivision.Vertex current = cw(v, p, deleted);
                PlanarSubdivision.Vertex previous = p;
                while (current != prevV) {
                    leftBoundary.addFirst(current);
                    leftCount.set(current, leftCount.get(current) + 1);
                    PlanarSubdivision.Vertex temp = current;
                    current = cw(previous, current, deleted);
                    previous = temp;
                }
            }
        }
        // Find the vertices on the same side as next(G,v) that will be part of
        // the boundary when v is deleted
        LinkedList<PlanarSubdivision.Vertex> rightBoundary = new LinkedList<>();
        ElementList<Integer> rightCount = new ElementList<>(subdivision.numberOfVertices(), 0);
        {
            if (p != nextV) {
                PlanarSubdivision.Vertex current = ccw(v, p, deleted);
                PlanarSubdivision.Vertex previous = p;
                while (current != nextV) {
                    rightBoundary.add(current);
                    rightCount.set(current, rightCount.get(current) + 1);
                    PlanarSubdivision.Vertex temp = current;
                    current = ccw(previous, current, deleted);
                    previous = temp;
                }
            }
        }
        // Add the remaining vertices to the left and right boundaries
        {
            CircularListIterator<PlanarSubdivision.Vertex> boundaryIterator = new CircularListIterator<>(boundary);
            ListIterator<PlanarSubdivision.Vertex> leftBoundaryIterator = leftBoundary.listIterator();
            PlanarSubdivision.Vertex current = boundaryIterator.next();
            do {
                leftBoundaryIterator.add(current);
                leftCount.set(current, leftCount.get(current) + 1);
                current = boundaryIterator.next();
            } while (current != v);

            current = boundaryIterator.next();
            while (current != p) {
                rightBoundary.add(current);
                rightCount.set(current, rightCount.get(current) + 1);
                current = boundaryIterator.next();
            }
            rightBoundary.addFirst(p);
            rightCount.set(p, rightCount.get(p) + 1);

            while (current != r) {
                leftBoundary.add(current);
                leftCount.set(current, leftCount.get(current) + 1);
                current = boundaryIterator.next();
            }
        }
        // Arrays to quickly tell if a vertex is in ccw(G, r, v) or cw(G, r, v)
        ElementList<Boolean> inCCW = new ElementList<>(subdivision.numberOfVertices(), false);
        ElementList<Boolean> inCW = new ElementList<>(subdivision.numberOfVertices(), false);
        {
            CircularListIterator<PlanarSubdivision.Vertex> cit = new CircularListIterator<>(boundary);
            PlanarSubdivision.Vertex current;
            do {
                current = cit.next();
                inCCW.set(current, true);
            } while (current != v);

            inCW.set(v, true);
            do {
                current = cit.next();
                inCW.set(current, true);
            } while (current != r);
        }
        // Create and recursively solve the 2-connected components
        if (leftBoundary.size() > 1) {
            ArrayList<LinkedList<PlanarSubdivision.Vertex>> components;
            components = computeBiconnectedComponents(leftBoundary, leftCount);
            for (LinkedList<PlanarSubdivision.Vertex> component : components) {
                CircularListIterator<PlanarSubdivision.Vertex> cit = new CircularListIterator<>(component);
                PlanarSubdivision.Vertex rc = cit.next();
                PlanarSubdivision.Vertex nextRc = cit.next();
                PlanarSubdivision.Vertex vc = null;
                PlanarSubdivision.Vertex current;
                cit = new CircularListIterator<>(component);
                do {
                    current = cit.previous();
                    if (inCCW.get(current)) {
                        vc = current;
                        break;
                    }
                } while (current != nextRc);
                if (vc == null) {
                    vc = nextRc;
                }
                block(rc, vc, component);
            }
        }
        if (rightBoundary.size() > 1) {
            ArrayList<LinkedList<PlanarSubdivision.Vertex>> components;
            components = computeBiconnectedComponents(rightBoundary, rightCount);
            for (LinkedList<PlanarSubdivision.Vertex> component : components) {
                CircularListIterator<PlanarSubdivision.Vertex> cit = new CircularListIterator<>(component);
                cit.previous();
                PlanarSubdivision.Vertex prevRc = cit.next();
                PlanarSubdivision.Vertex rc = cit.next();
                PlanarSubdivision.Vertex vc = null;
                PlanarSubdivision.Vertex current;
                do {
                    current = cit.next();
                    if (inCW.get(current)) {
                        vc = current;
                        break;
                    }
                } while (current != prevRc);
                if (vc == null) {
                    vc = prevRc;
                }
                block(rc, vc, component);
            }
        }
    }

    private ArrayList<LinkedList<PlanarSubdivision.Vertex>> computeBiconnectedComponents(
            LinkedList<PlanarSubdivision.Vertex> boundary, ElementList<Integer> count) {

        ArrayList<LinkedList<PlanarSubdivision.Vertex>> components = new ArrayList<>();
        ArrayDeque<LinkedList<PlanarSubdivision.Vertex>> componentStack = new ArrayDeque<>();
        LinkedList<PlanarSubdivision.Vertex> activeBoundary = new LinkedList<>();
        componentStack.push(activeBoundary);
        components.add(activeBoundary);
        CircularListIterator<PlanarSubdivision.Vertex> cit = new CircularListIterator<>(boundary);
        PlanarSubdivision.Vertex current;
        do {
            current = cit.next();
            if (current == activeBoundary.peekFirst()) {
                // This component is finished
                componentStack.pop();
                int currentCount = count.get(current);
                if (currentCount >= 2) {
                    // New component connected to the same cut vertex
                    count.set(current, currentCount - 1);
                    activeBoundary = new LinkedList<>();
                    activeBoundary.add(current);
                    componentStack.push(activeBoundary);
                    components.add(activeBoundary);
                } else {
                    activeBoundary = componentStack.peek();
                }
            } else {
                activeBoundary.add(current);
                int currentCount = count.get(current);
                if (currentCount >= 2) {
                    // Found a cut vertex
                    count.set(current, currentCount - 1);
                    activeBoundary = new LinkedList<>();
                    activeBoundary.add(current);
                    componentStack.push(activeBoundary);
                    components.add(activeBoundary);
                }
            }
        } while (!componentStack.isEmpty());
        return components;
    }

    private PlanarSubdivision.Vertex cw(PlanarSubdivision.Vertex u, PlanarSubdivision.Vertex v,
            ElementList<Boolean> deleted) {
        List<? extends PlanarSubdivision.Halfedge> halfedges = v.getOutgoingHalfedges();
        int size = halfedges.size();
        for (int i = 0; i < size; i++) {
            PlanarSubdivision.Halfedge h = halfedges.get(i);
            if (h.getTarget() == u) {
                int j = (i + 1) % size;
                PlanarSubdivision.Halfedge next = halfedges.get(j);
                while (deleted.get(next.getTarget())) {
                    j = (j + 1) % size;
                    next = halfedges.get(j);
                }
                return next.getTarget();
            }
        }
        throw new RuntimeException();
        //return null;
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

    private PlanarSubdivision.Vertex ccw(PlanarSubdivision.Vertex u, PlanarSubdivision.Vertex v,
            ElementList<Boolean> deleted) {
        List<? extends PlanarSubdivision.Halfedge> halfedges = v.getOutgoingHalfedges();
        int size = halfedges.size();
        for (int i = 0; i < size; i++) {
            PlanarSubdivision.Halfedge h = halfedges.get(i);
            if (h.getTarget() == u) {
                int j = (i + size - 1) % size;
                PlanarSubdivision.Halfedge next = halfedges.get(j);
                while (deleted.get(next.getTarget())) {
                    j = (j + size - 1) % size;
                    next = halfedges.get(j);
                }
                return next.getTarget();
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

    private void pComputeHeights() {
        new HeightRecurrence().compute();
    }

    private void exportState() {
        IpeExporter exporter = new IpeExporter();
        exporter.setStrokeWidth(3.0);
        for (Network.Edge e : graph.edges()) {
            PlanarSubdivision.Vertex u = subdivision.getVertex(e.getSource().getId());
            PlanarSubdivision.Vertex v = subdivision.getVertex(e.getTarget().getId());
            if (parent.get(u) == v || parent.get(v) == u) {
                exporter.setStrokeColor(Color.RED);
                exporter.appendEdge(e, null);
            } else if (!deletedEdge.get(e)) {
                exporter.setStrokeColor(Color.BLACK);
                exporter.appendEdge(e, null);
            }
        }
        exporter.setFillColor(Color.WHITE);
        exporter.setStrokeColor(Color.BLACK);
        for (Network.Vertex v : graph.vertices()) {
            exporter.appendVertex(v, null);
        }
        exporter.exportToFile(String.format("ost-states/ost-state-%03d.ipe", stepCounter++));

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

    private String nodeName(PlanarSubdivision.Vertex v) {
        if (v == null) {
            return "#NULL#";
        } else if (v.getId() < map.numberOfBoundedFaces()) {
            try {
                return map.getFace(v.getId()).getLabel().getText();
            } catch (NullPointerException e) {
                return null;
            }
        } else if (v == top) {
            return "TOP";
        } else if (v == left) {
            return "LEFT";
        } else if (v == right) {
            return "RIGHT";
        } else {
            return v.getPosition().toString();
        }
    }

    private boolean ostIsValid() {
        TreeChecker tc = new TreeChecker();
        return tc.isOrderly();

    }

    private class TreeChecker {

        public boolean isOrderly() {
            for (PlanarSubdivision.Vertex u : preordering) {
                if (labels.get(u) == 0) {
                    for (PlanarSubdivision.Halfedge h : u.getOutgoingHalfedges()) {
                        PlanarSubdivision.Vertex v = h.getTarget();
                        if (parent.get(v) != u) {
                            return false;
                        }
                    }
                } else {
                    List<? extends PlanarSubdivision.Halfedge> outgoing = u.getOutgoingHalfedges();
                    CircularListIterator<? extends PlanarSubdivision.Halfedge> cit = new CircularListIterator<>(outgoing);
                    PlanarSubdivision.Vertex first = cit.previous().getTarget();
                    PlanarSubdivision.Vertex current = first;
                    PlanarSubdivision.Vertex p = parent.get(u);
                    do {
                        if (current == p) {
                            break;
                        }
                        current = cit.previous().getTarget();
                    } while (current != first);
                    if (current != p) {
                        return false;
                    }

                    // state = 0: parent of u
                    // state = 1: unrelated nodes v with label[v] < label[u]
                    // state = 2: children of u
                    // state = 3: unrelated nodes v with label[v] > label[u]
                    int state = 0;
                    PlanarSubdivision.Vertex v = cit.previous().getTarget();
                    while (v != p) {
                        if (parent.get(v) == u) {
                            if (state > 2) {
                                return false;
                            } else {
                                state = 2;
                            }
                        } else if (unrelated(u, v)) {
                            if (labels.get(v) < labels.get(u)) {
                                if (state < 2) {
                                    state = 1;
                                } else {
                                    return false;
                                }
                            } else {
                                state = 3;
                            }
                        } else {
                            return false;
                        }
                        v = cit.previous().getTarget();
                    }
                }
            }
            return true;
        }

        private boolean unrelated(PlanarSubdivision.Vertex u, PlanarSubdivision.Vertex v) {
            return (!isAncestor(u, v) && !isAncestor(v, u));
        }

        private boolean isAncestor(PlanarSubdivision.Vertex u, PlanarSubdivision.Vertex v) {
            PlanarSubdivision.Vertex x = v;
            do {
                x = parent.get(x);
                if (x == u) {
                    return true;
                }
            } while (x != null);
            return false;
        }
    }
}
