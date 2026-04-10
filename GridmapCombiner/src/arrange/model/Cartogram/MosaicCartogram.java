package arrange.model.Cartogram;

import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import arrange.Utils.Utils;
import arrange.model.Network;
import arrange.model.graph.ConnectedComponents;
import arrange.model.graph.DijkstraShortestPath;
import arrange.model.graph.Graph;
import arrange.model.graph.GraphAlgorithms;
import arrange.model.subdivision.Map;
import arrange.model.util.ElementList;
import arrange.model.util.Identifier;
import arrange.model.util.IpeExporter;
import arrange.model.util.LinkedHashMultiset;
import arrange.model.util.Multiset;
import arrange.model.util.Pair;
import arrange.model.util.Vector2D;
import arrange.model.util.WeightedObject;
import integration.Stopwatch;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public abstract class MosaicCartogram {

    private final Map map;
    private final Network dual;
    private final LinkedHashMap<Coordinate, Cell> cells;
    private final ElementList<MosaicRegion> regions;
    private double cellWeight;
    private static final boolean EXPORT_REGION_OVERLAY = false;

    public MosaicCartogram() {
        throw new RuntimeException("Not implemented");
    }

    public MosaicCartogram(Map map, Network dual) {
        this.map = map;
        this.dual = dual;
        this.cells = new LinkedHashMap<>();
        this.regions = new ElementList<>(map.numberOfBoundedFaces());
        initialize();
    }

    public MosaicCartogram(MosaicCartogram other) {
        this.map = other.map;
        this.dual = other.dual;
        cells = new LinkedHashMap<>();
        for (Entry<Coordinate, Cell> entry : other.cells.entrySet()) {
            Coordinate c = entry.getKey();
            Network.Vertex v = entry.getValue().getVertex();
            this.cells.put(c, createCell(c, v));
        }
        this.regions = new ElementList<>(other.regions.size());
        for (MosaicRegion cr : other.regions) {
            this.regions.add(new MosaicRegion(cr));
        }
        this.cellWeight = other.cellWeight;
    }

    public final int numberOfCells() {
        return cells.size();
    }

    public final Map getMap() {
        return map;
    }

    public final Network getDual() {
        return dual;
    }

    public final void computeDesiredRegions(double cellWeight, final int sample, final boolean timing) {
        this.cellWeight = cellWeight;
        computeDesiredRegions(sample, timing);
    }

    public final double getCellWeight() {
        return cellWeight;
    }

    public abstract double getCellArea();

    public abstract double getCellSide();

    public abstract double getCellApothem();

    public abstract double getCellSamplingWidth();

    public abstract double getCellSamplingHeight();

    /*
     * Returns the boundary points of a cell with center on (0, 0).
     */
    public abstract Point2D[] getDefaultCellBoundaryPoints();

    public Network.Vertex setVertex(Coordinate c, Network.Vertex v) {
        if (c == null || v == null) {
            throw new NullPointerException();
        }
        regions.get(v).addHexagon(c);
        Cell cell = cells.get(c);
        if (cell == null) {
            cells.put(c, createCell(c, v));
            return null;
        } else {
            Network.Vertex old = cell.getVertex();
            regions.get(old).removeHexagon(c);
            cell.setVertex(v);
            return old;
        }
    }

    public Network.Vertex getVertex(Coordinate c) {
        Cell cell = cells.get(c);
        if (cell == null) {
            return null;
        }
        return cell.getVertex();
    }

    public Cell getCell(Coordinate c) {
        Cell cell = cells.get(c);
        if (cell == null) {
            return createCell(c, null);
        } else {
            return cell;
        }
    }

    public Network.Vertex removeCell(Coordinate c) {
        Cell cell = cells.remove(c);
        if (cell == null) {
            return null;
        } else {
            Network.Vertex old = cell.getVertex();
            if (old != null) {
                regions.get(old).removeHexagon(c);
            }
            return old;
        }
    }

    public abstract Coordinate getContainingCell(double px, double py);

    public Coordinate getContainingCell(Vector2D position) {
        return getContainingCell(position.getX(), position.getY());
    }

    public Coordinate getContainingCell(Point2D point) {
        return getContainingCell(point.getX(), point.getY());
    }

    public Set<? extends Coordinate> getCoordinateSet() {
        return cells.keySet();
    }

    public Coordinate[] getCoordinateArray() {
        Coordinate[] occupied = new Coordinate[cells.size()];
        int i = 0;
        for (Coordinate c : cells.keySet()) {
            occupied[i++] = c;
        }
        return occupied;
    }

    public Cell[] getCellArray() {
        Cell[] cellArray = new Cell[cells.size()];
        int pos = 0;
        for (Cell cell : cells.values()) {
            cellArray[pos++] = cell;
        }
        return cellArray;
    }

    public Iterable<? extends Coordinate> coordinates() {
        return cells.keySet();
    }

    public Iterable<? extends Cell> cells() {
        return cells.values();
    }

    public abstract Coordinate zeroVector();

    public abstract Coordinate[] unitVectors();

    public void translateRegions(Iterable<Integer> regionIndices, Coordinate t) {
        for (int index : regionIndices) {
            MosaicRegion region = regions.get(index);
            for (Coordinate c : region.occupiedCoordinates()) {
                cells.remove(c);
            }
        }
        for (int index : regionIndices) {
            MosaicRegion region = regions.get(index);
            Network.Vertex v = region.getVertex();
            region.translate(t);
            for (Coordinate c : region) {
                Cell cell = cells.get(c);
                if (cell == null) {
                    cells.put(c, createCell(c, v));
                } else {
                    Network.Vertex old = cell.getVertex();
                    regions.get(old).removeHexagon(c);
                    cell.setVertex(v);
                }
            }
        }
    }

    public void clear() {
        cells.clear();
        for (MosaicRegion region : regions) {
            region.clear();
        }
    }

    public Iterable<MosaicRegion> regions() {
        Iterable<MosaicRegion> iterable = new Iterable<MosaicRegion>() {
            @Override
            public Iterator<MosaicRegion> iterator() {
                return regions.iterator();
            }
        };
        return iterable;
    }

    public int numberOfRegions() {
        return regions.size();
    }

    public MosaicRegion getRegion(int id) {
        return regions.get(id);
    }

    public MosaicRegion getRegion(Coordinate c) {
        Network.Vertex v = getVertex(c);
        if (v != null) {
            return regions.get(v);
        }
        return null;
    }

    public boolean isValid() {
        for (MosaicRegion region : regions) {
            if (!region.isValid()) {
                return false;
            }
        }
        return true;
    }

    public boolean isConnected(MosaicRegion region) {
        return region.isConnected();
    }

    public boolean isConnected() {
        for (MosaicRegion region : regions) {
            if (!region.isConnected()) {
                return false;
            }
        }
        return true;
    }

    public boolean isValid(MosaicRegion region) {
        return region.isValid();
    }

    public double quality(boolean normalize) {
        double total = 0;
        if (normalize) {
            for (MosaicRegion region : regions) {
                total += Math.abs((double) region.getSymmetricDifference() / region.getGuidingShape().size());
            }
        } else {
            for (MosaicRegion region : regions) {
                total += Math.abs((double) region.getSymmetricDifference());
            }
        }
        return total;
    }

    public Pair<Double, Double> getGridQualityPair() {
        double v1 = this.quality(false);
        double v2 = this.quality(true);
        return new Pair<>(v1, v2);
    }

    public void overlayGrid(MosaicCartogram other) {
        clear();
        // Get candidates for the new grid
        LinkedList<Coordinate> candidates = new LinkedList<>();
        for (Cell cell : other.cells()) {
            Network.Vertex vertex = cell.getVertex();
            Point2D center = cell.getCenter();
            Coordinate c = getContainingCell(center);
            candidates.addFirst(c);
            this.setVertex(c, vertex);
        }
        while (!candidates.isEmpty()) {
            Coordinate c = candidates.pollLast();
            for (Coordinate d : c.neighbours()) {
                Cell cell = this.getCell(d);
                if (cell.getVertex() == null) {
                    Point2D center = cell.getCenter();
                    Coordinate otherCoordinate = other.getContainingCell(center);
                    Network.Vertex vertex = other.getVertex(otherCoordinate);
                    if (vertex != null) {
                        this.setVertex(d, vertex);
                        candidates.addFirst(d);
                    }
                }
            }
        }
    }

    public void exportCoordinates(String fileName) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(new File(fileName)));
            for (int i = 0; i < regions.size(); i++) {
                MosaicRegion region = regions.get(i);
                bw.write("ID " + i);
                bw.newLine();
                Coordinate t = region.totalTranslation.normalize();
                bw.write(exportString(t.getComponents()));
                bw.newLine();
                for (Coordinate c : region) {
                    bw.write(exportString(c.getComponents()));
                    bw.newLine();
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(IpeExporter.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                bw.close();
            } catch (IOException ex) {
                Logger.getLogger(IpeExporter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void importCoordinates(String fileName) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(new File(fileName)));
            String line = br.readLine();
            Network.Vertex v = null;
            while (line != null) {
                if (line.startsWith("ID")) {
                    line = line.substring(3);
                    int id = Integer.parseInt(line);
                    v = dual.getVertex(id);
                    line = br.readLine();
                    String[] splitLine = line.split("\\s+");
                    int[] components = new int[splitLine.length];
                    for (int i = 0; i < splitLine.length; i++) {
                        components[i] = Integer.parseInt(splitLine[i]);
                    }
                    Coordinate t = parseCoordinate(components);
                    regions.get(id).translateGuidingShape(t);
                } else {
                    String[] splitLine = line.split("\\s+");
                    int[] components = new int[splitLine.length];
                    for (int i = 0; i < splitLine.length; i++) {
                        components[i] = Integer.parseInt(splitLine[i]);
                    }
                    Coordinate c = parseCoordinate(components);
                    setVertex(c, v);
                }
                line = br.readLine();
            }
        } catch (IOException ex) {
            Logger.getLogger(IpeExporter.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(IpeExporter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public abstract MosaicCartogram duplicate();

    /**
     * Assumes that the coordinates are connected.
     */
    public static ArrayList<Set<Coordinate>> computeHoles(MosaicCartogram mosaic, Set<? extends Coordinate> coordinates) {
        // Find all white hexagons that have a neighbours in this grid
        Graph neighboursGraph = new Graph();
        LinkedHashMap<Coordinate, Graph.Vertex> coordinateToVertex = new LinkedHashMap<>();
        ElementList<Coordinate> vertexToCoordinate = new ElementList<>();
        Graph.Vertex leftmost = null;
        double leftmostColumn = Double.POSITIVE_INFINITY;
        for (Coordinate c : coordinates) {
            for (Coordinate d : c.connectedVicinity()) {
                if (!coordinates.contains(d) && !coordinateToVertex.containsKey(d)) {
                    Graph.Vertex v = neighboursGraph.addVertex();
                    coordinateToVertex.put(d, v);
                    vertexToCoordinate.add(d);
                    double column = mosaic.getCell(d).getCenter().getX();
                    if (column < leftmostColumn) {
                        leftmostColumn = column;
                        leftmost = v;
                    }
                }
            }
        }
        // Connect white neighbours in the adjecency graph
        for (Entry<Coordinate, Graph.Vertex> entry : coordinateToVertex.entrySet()) {
            Coordinate cu = entry.getKey();
            Graph.Vertex u = entry.getValue();
            for (Coordinate cv : cu.neighbours()) {
                Graph.Vertex v = coordinateToVertex.get(cv);
                if (v != null && v.getId() > u.getId()) {
                    neighboursGraph.addEdge(u, v);
                }
            }
        }
        ConnectedComponents<Graph.Vertex, Graph.Edge> cc = new ConnectedComponents<>(neighboursGraph);
        int outerBoundaryIndex = cc.getComponentIndex(leftmost);
        ArrayList<Set<Coordinate>> holes = new ArrayList<>();
        for (int i = 0; i < cc.numberOfComponents(); i++) {
            if (i != outerBoundaryIndex) {
                Set<Graph.Vertex> component = cc.getComponent(i);
                LinkedHashSet<Coordinate> hole = new LinkedHashSet<>();
                ArrayDeque<Coordinate> activeStack = new ArrayDeque<>();
                HashSet<Coordinate> done = new HashSet<>();
                for (Graph.Vertex v : component) {
                    Coordinate c = vertexToCoordinate.get(v);
                    activeStack.addLast(c);
                    done.add(c);
                }

                while (!activeStack.isEmpty()) {
                    Coordinate c = activeStack.pollLast();
                    if (hole.add(c)) {
                        for (Coordinate d : c.neighbours()) {
                            if (!done.contains(d) && !coordinates.contains(d)) {
                                activeStack.addLast(d);
                                done.add(d);
                            }
                        }
                    }
                }
                holes.add(hole);
            }
        }
        return holes;
    }

    /**
     * Assumes that the coordinates are connected.
     */
    public static ArrayList<Set<Coordinate>> computeHoleBoundaries(MosaicCartogram mosaic, Set<? extends Coordinate> coordinates) {
        // Find all white hexagons that have a neighbours in this grid
        Graph neighboursGraph = new Graph();
        LinkedHashMap<Coordinate, Graph.Vertex> coordinateToVertex = new LinkedHashMap<>();
        ElementList<Coordinate> vertexToCoordinate = new ElementList<>();
        Graph.Vertex leftmost = null;
        double leftmostColumn = Double.POSITIVE_INFINITY;
        for (Coordinate c : coordinates) {
            for (Coordinate d : c.connectedVicinity()) {
                if (!coordinates.contains(d) && !coordinateToVertex.containsKey(d)) {
                    Graph.Vertex v = neighboursGraph.addVertex();
                    coordinateToVertex.put(d, v);
                    vertexToCoordinate.add(d);
                    double column = mosaic.getCell(d).getCenter().getX();
                    if (column < leftmostColumn) {
                        leftmostColumn = column;
                        leftmost = v;
                    }
                }
            }
        }
        // Connect white neighbours in the adjecency graph
        for (Entry<Coordinate, Graph.Vertex> entry : coordinateToVertex.entrySet()) {
            Coordinate cu = entry.getKey();
            Graph.Vertex u = entry.getValue();
            for (Coordinate cv : cu.neighbours()) {
                Graph.Vertex v = coordinateToVertex.get(cv);
                if (v != null && v.getId() > u.getId()) {
                    neighboursGraph.addEdge(u, v);
                }
            }
        }
        ConnectedComponents<Graph.Vertex, Graph.Edge> cc = new ConnectedComponents<>(neighboursGraph);
        int outerBoundaryIndex = cc.getComponentIndex(leftmost);
        ArrayList<Set<Coordinate>> holes = new ArrayList<>();
        for (int i = 0; i < cc.numberOfComponents(); i++) {
            if (i != outerBoundaryIndex) {
                Set<Graph.Vertex> component = cc.getComponent(i);
                LinkedHashSet<Coordinate> hole = new LinkedHashSet<>();
                for (Graph.Vertex v : component) {
                    hole.add(vertexToCoordinate.get(v));
                }
                holes.add(hole);
            }
        }
        return holes;
    }

    protected abstract Cell createCell(Coordinate c, Network.Vertex v);

    protected abstract Coordinate parseCoordinate(int[] components);

    private String exportString(int[] components) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < components.length - 1; i++) {
            sb.append(components[i]);
            sb.append(" ");
        }
        sb.append(components[components.length - 1]);
        return sb.toString();
    }

    private void initialize() {
        for (int i = 0; i < map.numberOfBoundedFaces(); i++) {
            regions.add(new MosaicRegion(dual.getVertex(i)));
        }
    }

    private void computeDesiredRegions(final int SAMPLE, final boolean timing) {
        Stopwatch sw = timing ? Stopwatch.get("- guides").start() : null;

        final double HORIZONTAL_INCREMENT = getCellSamplingWidth() / SAMPLE;
        final double VERTICAL_INCREMENT = getCellSamplingHeight() / SAMPLE;
        double cellArea = getCellArea();
        for (Map.Face f : map.boundedFaces()) {
            CellRegion bestRegion = null;
            int numHexagons = Math.max(1, (int) Math.round(f.getWeight() / cellWeight));
            double regionArea = numHexagons * cellArea;
            double factor = Math.sqrt(regionArea / f.getArea());
            double bestQuality = Double.NEGATIVE_INFINITY;
            LinkedHashMap<Coordinate, Double> bestintersected = null;
            double bestTx = 0;
            double bestTy = 0;
            double tx = 0;
            for (int i = 0; i <= SAMPLE; i++) {
                double ty = 0;
                for (int j = 0; j <= SAMPLE; j++) {

                    LinkedHashMap<Coordinate, Double> intersected = new LinkedHashMap<>();
                    Pair<CellRegion, Double> regionPair = computeDesiredRegion(f, factor, tx, ty, intersected);
                    CellRegion region = regionPair.getFirst();
                    double quality = regionPair.getSecond();
                    if (quality > bestQuality) {
                        bestRegion = region;
                        bestQuality = quality;
                        bestTx = tx;
                        bestTy = ty;
                        bestintersected = intersected;
                    }
                    ty += VERTICAL_INCREMENT;
                }
                tx += HORIZONTAL_INCREMENT;
            }
            regions.get(f).setDesiredRegion(bestRegion, factor, bestTx, bestTy, bestintersected);
            ////////////////////////////////////////////////////////////////////
            if (EXPORT_REGION_OVERLAY) {
                IpeExporter exporter = new IpeExporter();
                Path2D facePath = f.toPath2D();
                AffineTransform at = new AffineTransform();
                at.setTransform(factor, 0, 0, factor, bestTx, bestTy);
                facePath.transform(at);
                exporter.appendCellRegion(bestRegion, "hexagons");
                exporter.setStrokeWidth(0.1);
                exporter.appendPath(facePath, "country");
                exporter.exportToFile(f.getLabel().getText() + "-overlay.ipe");
            }
            ////////////////////////////////////////////////////////////////////
        }

        if (timing) {
            sw.stop();
        }
    }

    private Pair<CellRegion, Double> computeDesiredRegion(Map.Face f, double factor, double tx, double ty, LinkedHashMap<Coordinate, Double> intersected) {
        final int numHexagons = Math.max(1, (int) Math.round(f.getWeight() / cellWeight));
        LinkedList<Coordinate> candidates = new LinkedList<>();
        Vector2D firstPos = f.getBoundaryVertices().get(0).getPosition();
        Coordinate firstHex = getContainingCell(factor * firstPos.getX() + tx, factor * firstPos.getY() + ty);
        candidates.addFirst(firstHex);
        for (Coordinate neighbour : firstHex.neighbours()) {
            candidates.addFirst(neighbour);
        }

        // Create area to test for intersections
        Path2D path = f.toPath2D();
        AffineTransform at = new AffineTransform();
        at.setTransform(factor, 0, 0, factor, tx, ty);
        path.transform(at);
        Area faceArea = new Area(path);

        // Create and setVertex the desired region
        CellRegion region = new CellRegion();

        // Identify all candidate cells to be in the desired region
        while (!candidates.isEmpty()) {
            Coordinate c = candidates.pollLast();
            if (!intersected.containsKey(c)) {
                Path2D hexPath = getCell(c).getBoundaryShape();
                Area hexArea = new Area(hexPath);
                hexArea.intersect(faceArea);

                // Compute the area of the intersection
                double intersectNumArea = 0;
                PathIterator it = hexArea.getPathIterator(null);
                ArrayList<Vector2D> points = new ArrayList<>();
                while (!it.isDone()) {
                    double[] values = new double[6];
                    int type = it.currentSegment(values);
                    switch (type) {
                        case PathIterator.SEG_CLOSE:
                            intersectNumArea += Utils.computePolygonArea(points);
                            break;
                        case PathIterator.SEG_LINETO:
                            points.add(new Vector2D(values[0], values[1]));
                            break;
                        case PathIterator.SEG_MOVETO:
                            points.clear();
                            points.add(new Vector2D(values[0], values[1]));
                            break;
                    }
                    it.next();
                }

                // Add if there is enough intersection
                if (!hexArea.isEmpty()) {
                    intersected.put(c, -intersectNumArea);
                    for (Coordinate neighbour : c.neighbours()) {
                        if (!intersected.containsKey(neighbour)) {
                            candidates.addFirst(neighbour);
                        }
                    }
                }
            }
        }

        // Sort the candidates according to the area of the intersection
        ArrayList<WeightedObject<Coordinate, Double>> sortedIntersected = new ArrayList<>(intersected.size());
        for (Entry<Coordinate, Double> entry : intersected.entrySet()) {
            sortedIntersected.add(new WeightedObject<>(entry.getKey(), entry.getValue()));
        }
        Collections.sort(sortedIntersected);

        // Adjacency graph of all intersected hexagons
        Graph gFull = new Graph();
        HashMap<Coordinate, Graph.Vertex> coordinateToGFull = new HashMap<>();
        for (int i = 0; i < sortedIntersected.size(); i++) {
            Graph.Vertex v = gFull.addVertex();
            Coordinate c = sortedIntersected.get(i).getObject();
            coordinateToGFull.put(c, v);
        }
        for (int i = 0; i < sortedIntersected.size(); i++) {
            Graph.Vertex u = gFull.getVertex(i);
            Coordinate c = sortedIntersected.get(i).getObject();
            for (Coordinate d : c.neighbours()) {
                Graph.Vertex v = coordinateToGFull.get(d);
                if (v != null) {
                    if (i < v.getId()) {
                        gFull.addEdge(u, v);
                    }
                }
            }
        }

        // Try to plus the best connected hexagons
        ArrayList<Coordinate> connectors = new ArrayList<>();
        do {
            // Adjacency graph restricted to the chosen hexagons
            Graph gBest = new Graph();
            // Add hexagons according to quality
            HashMap<Coordinate, Graph.Vertex> coordinateToGBest = new HashMap<>();
            for (int i = 0; i < numHexagons - connectors.size(); i++) {
                Graph.Vertex v = gBest.addVertex();
                Coordinate c = sortedIntersected.get(i).getObject();
                coordinateToGBest.put(c, v);
            }
            // Add extra connectors to make sure the graph is connected
            for (Coordinate c : connectors) {
                Graph.Vertex v = gBest.addVertex();
                coordinateToGBest.put(c, v);
            }
            // Compute edges
            for (int i = 0; i < numHexagons; i++) {
                Graph.Vertex u = gBest.getVertex(i);
                Coordinate c;
                if (i < numHexagons - connectors.size()) {
                    c = sortedIntersected.get(i).getObject();
                } else {
                    c = connectors.get(i - numHexagons + connectors.size());
                }
                for (Coordinate d : c.neighbours()) {
                    Graph.Vertex v = coordinateToGBest.get(d);
                    if (v != null) {
                        if (i < v.getId()) {
                            gBest.addEdge(u, v);
                        }
                    }
                }
            }
            // Compute connected components
            ConnectedComponents<Graph.Vertex, Graph.Edge> cc = new ConnectedComponents<>(gBest);
            if (cc.numberOfComponents() > 1) {
                // Graph is disconnected, must plus a new connector
                Set<Graph.Vertex> s1 = new HashSet<>();
                Set<Graph.Vertex> s2 = new HashSet<>();
                // Create sets with the equivalent vertices from gFull
                for (Graph.Vertex v : cc.getComponent(0)) {
                    Coordinate c;
                    if (numHexagons < connectors.size()) {
                        return new Pair<>(null, Double.NEGATIVE_INFINITY);
                    }

                    if (v.getId() < numHexagons - connectors.size()) {
                        c = sortedIntersected.get(v.getId()).getObject();
                    } else {
                        c = connectors.get(v.getId() - numHexagons + connectors.size());
                    }
                    s1.add(coordinateToGFull.get(c));
                }
                for (Graph.Vertex v : cc.getComponent(1)) {
                    Coordinate c;
                    if (v.getId() < numHexagons - connectors.size()) {
                        c = sortedIntersected.get(v.getId()).getObject();
                    } else {
                        c = connectors.get(v.getId() - numHexagons + connectors.size());
                    }
                    s2.add(coordinateToGFull.get(c));
                }
                // Run dijkstra on gFull to find connecting hexagons
                ArrayList<Graph.Vertex> p = DijkstraShortestPath.shortestPath(gFull, s1, s2);
                for (int i = 1; i < p.size() - 1; i++) {
                    Graph.Vertex v = p.get(i);
                    Coordinate c = sortedIntersected.get(v.getId()).getObject();
                    if (!connectors.contains(c)) {
                        boolean alreadyIn = false;
                        for (int j = 0; j < numHexagons - connectors.size() - 1; j++) {
                            WeightedObject wo = sortedIntersected.get(j);
                            if (wo.getObject().equals(c)) {
                                alreadyIn = true;
                                break;
                            }
                        }
                        if (!alreadyIn) {
                            connectors.add(c);
                        }
                    }
                }
            } else {
                break;
            }
        } while (true);

        // Finaly create the region!!
        double totalIntersection = 0;
        for (int i = 0; i < numHexagons - connectors.size(); i++) {
            Coordinate c = sortedIntersected.get(i).getObject();
            region.addHexagon(c);
            totalIntersection -= intersected.get(c);
        }
        for (int i = 0; i < connectors.size(); i++) {
            Coordinate c = connectors.get(i);
            region.addHexagon(c);
            totalIntersection -= intersected.get(c);
        }

        // Close holes (This function is infinite! It's eternal!)
        ArrayList<Set<Coordinate>> holes = computeHoles(this, region.coordinateSet());
        if (!holes.isEmpty()) {
            int extraTiles = 0;
            for (Set<Coordinate> hole : holes) {
                extraTiles += hole.size();
                for (Coordinate c : hole) {
                    region.addHexagon(c);
                    Double intersectionArea = intersected.get(c);
                    if (intersectionArea != null) {
                        totalIntersection -= intersectionArea;
                    }
                }
            }
            // Build connectivity graph to make sure we don't disconnect the region
            Graph gFinal = new Graph();
            LinkedHashMap<Coordinate, Graph.Vertex> coordinateToGFinal = new LinkedHashMap<>();
            TreeSet<WeightedObject<Coordinate, Double>> removeCandidates = new TreeSet<>();
            for (Coordinate c : region) {
                Graph.Vertex v = gFinal.addVertex();
                coordinateToGFinal.put(c, v);
                if (region.isEdge(c)) {
                    removeCandidates.add(new WeightedObject<>(c, -intersected.get(c)));
                }
            }
            for (Coordinate c : region) {
                Graph.Vertex vc = coordinateToGFinal.get(c);
                for (Coordinate d : c.neighbours()) {
                    if (region.contains(d)) {
                        Graph.Vertex vd = coordinateToGFinal.get(d);
                        if (vc.getId() < vd.getId()) {
                            gFinal.addEdge(vc, vd);
                        }
                    }
                }
            }
            // Remove corresponding number of tiles
            for (int i = 0; i < extraTiles; i++) {
                for (WeightedObject<Coordinate, Double> wo : removeCandidates) {
                    Coordinate c = wo.getObject();
                    Graph.Vertex vc = coordinateToGFinal.get(c);
                    gFinal.removeVertex(vc);
                    if (GraphAlgorithms.isConnected(gFinal)) {
                        double intersectArea = wo.getWeight();
                        removeCandidates.remove(wo);
                        coordinateToGFinal.remove(c);
                        totalIntersection += intersectArea;
                        for (Coordinate d : c.neighbours()) {
                            if (!region.isEdge(d)) {
                                Double newIntersectArea = intersected.get(d);
                                if (newIntersectArea == null) {
                                    newIntersectArea = 0.0;
                                }
                                removeCandidates.add(new WeightedObject<>(d, -newIntersectArea));
                            }
                        }
                        region.removeHexagon(c);
                        break;
                    } else {
                        vc = gFinal.addVertex();
                        coordinateToGFinal.put(c, vc);
                        for (Coordinate d : c.neighbours()) {
                            if (region.contains(d)) {
                                gFinal.addEdge(vc, coordinateToGFinal.get(d));
                            }
                        }
                    }
                }
            }
        }

        if (region.size() != numHexagons) {
            throw new RuntimeException("Guiding shape with wrong number of tiles");
        }

        return new Pair<>(region, totalIntersection);
    }

    public final class MosaicRegion extends CellRegion implements Identifier {

        private final Network.Vertex vertex;
        private final Graph connectivityGraph;
        private LinkedHashMap<Coordinate, Graph.Vertex> positionToVertex = new LinkedHashMap<>();
        private LinkedHashMultiset<Network.Vertex> neighbourDualVertices = new LinkedHashMultiset<>();
        private CellRegion guidingShape = null;
        public double factor = 1.0;
        public Coordinate totalTranslation = zeroVector();
        public Vector2D guidingShapeTranslation = new Vector2D(0, 0);
        public LinkedHashMap<Coordinate, Double> qualitymap = null; // NB on original guide coordinates
        private int hits = 0;
        private boolean connected = true;
        private boolean recomputeConnectivity = true;

        protected MosaicRegion(Network.Vertex vertex) {
            this.vertex = vertex;
            this.connectivityGraph = new Graph();
        }

        protected MosaicRegion(MosaicRegion other) {
            super(other);
            this.vertex = other.vertex;
            this.connectivityGraph = new Graph(other.connectivityGraph);
            for (Entry<Coordinate, Graph.Vertex> entry : other.positionToVertex.entrySet()) {
                this.positionToVertex.put(entry.getKey(), connectivityGraph.getVertex(entry.getValue().getId()));
            }
            this.neighbourDualVertices = new LinkedHashMultiset<>(other.neighbourDualVertices);
            this.guidingShape = (other.guidingShape == null ? null : new CellRegion(other.guidingShape));
            this.factor = other.factor;
            this.totalTranslation = other.totalTranslation;
            this.guidingShapeTranslation = new Vector2D(other.guidingShapeTranslation);
            this.hits = other.hits;
            this.connected = other.connected;
            this.recomputeConnectivity = other.recomputeConnectivity;
        }

        @Override
        public int getId() {
            return vertex.getId();
        }

        public Network.Vertex getVertex() {
            return vertex;
        }

        public Map.Face getMapFace() {
            return map.getFace(vertex.getId());
        }

        public CellRegion getGuidingShape() {
            return guidingShape;
        }

        public boolean isConnected() {
            if (recomputeConnectivity) {
                connected = GraphAlgorithms.isConnected(connectivityGraph);
                recomputeConnectivity = false;
            }
            return connected;
        }

        public LinkedHashMultiset<Network.Vertex> getNeighborsVertices() {
            return neighbourDualVertices;
        }

        public boolean isAdjacencyCorrect() {
            if (dual.getDegree(vertex) != neighbourDualVertices.size()) {
                return false;
            }

            for (Network.Vertex v : dual.neighbours(vertex)) {
                if (!neighbourDualVertices.contains(v)) {
                    return false;
                }
            }
            return true;
        }

        public boolean isValid() {
            return isConnected() && isAdjacencyCorrect();
        }

        public boolean isDesired(Coordinate c) {
            return guidingShape.contains(c);
        }

        public int getSymmetricDifference() {
            return this.size() + guidingShape.size() - 2 * hits;
        }

        public int getHexError() {
            return guidingShape.size() - size();
        }

        public Coordinate getGuidingShapeTranslation() {
            return totalTranslation;
        }

        public void computeBestOverlay() {
            Coordinate b1 = this.barycenter();
            Coordinate b2 = guidingShape.barycenter();
            Coordinate center = b2.minus(b1);
            Coordinate offset = center;
            hits = 0;
            for (Coordinate newOffset : center.disk(5)) {
                int newHits = computeOffsetQuality(newOffset);
                if (newHits > hits) {
                    offset = newOffset;
                    hits = newHits;
                }
            }
            offset = offset.times(-1);
            guidingShape.translate(offset);
            totalTranslation = totalTranslation.plus(offset);
            Vector2D move = offset.toVector2D();
            guidingShapeTranslation.add(move);
        }

        public void translateGuidingShape(Coordinate t) {
            //TODO: Update overlapping stuff
            guidingShape.translate(t);
            totalTranslation = totalTranslation.plus(t);
            Vector2D move = t.toVector2D();
            guidingShapeTranslation.add(move);
            hits = 0;
            for (Coordinate c : this) {
                if (guidingShape.contains(c)) {
                    hits++;
                }
            }
        }

        public Vector2D getCorrespondingMapPoint(Coordinate c) {
            Vector2D point = c.toVector2D();
            point.subtract(guidingShapeTranslation);
            point.multiply(1d / factor);
            return point;
        }

        @Override
        protected boolean addHexagon(Coordinate c) {
            boolean isNew = super.addHexagon(c);
            if (isDesired(c)) {
                hits++;
            }
            if (isNew) {
                if (!connected) {
                    recomputeConnectivity = true;
                } else {
                    boolean hasNeighbour = false;
                    for (Coordinate d : c.neighbours()) {
                        if (contains(d)) {
                            hasNeighbour = true;
                            break;
                        }
                    }
                    if (!hasNeighbour) {
                        recomputeConnectivity = true;
                    }
                }
                Graph.Vertex u = connectivityGraph.addVertex();
                positionToVertex.put(c, u);
                for (Coordinate neighbour : c.neighbours()) {
                    Network.Vertex v = MosaicCartogram.this.getVertex(neighbour);
                    if (v == vertex) {
                        connectivityGraph.addEdge(u, positionToVertex.get(neighbour));
                    } else {
                        if (v != null) {
                            neighbourDualVertices.add(v);
                            MosaicCartogram.this.getRegion(v.getId()).neighbourDualVertices.add(vertex);
                        }
                    }
                }
            } else {
                System.out.println("Warning: added existing hexagon to region");
                throw new RuntimeException();
            }
            return isNew;
        }

        @Override
        protected boolean removeHexagon(Coordinate c) {
            boolean hexagonExists = super.removeHexagon(c);
            if (hexagonExists) {
                if (isDesired(c)) {
                    hits--;
                }
                recomputeConnectivity = true;
                connectivityGraph.removeVertex(positionToVertex.get(c));
                positionToVertex.remove(c);
                for (Coordinate neighbour : c.neighbours()) {
                    Network.Vertex v = MosaicCartogram.this.getVertex(neighbour);
                    if (v != vertex) {
                        if (v != null) {
                            neighbourDualVertices.removeOne(v);
                            MosaicCartogram.this.getRegion(v.getId()).neighbourDualVertices.removeOne(vertex);
                        }
                    }
                }
            } else {
                System.out.println("Warning: removed non-existent hexagon from region");
            }
            return hexagonExists;
        }

        @Override
        public void translate(Coordinate t) {
            super.translate(t);
            guidingShape.translate(t);
            totalTranslation = totalTranslation.plus(t);
            Vector2D move = t.toVector2D();
            guidingShapeTranslation.add(move);
            LinkedHashMap<Coordinate, Graph.Vertex> translatedPositionToVertex = new LinkedHashMap<>();
            for (Entry<Coordinate, Graph.Vertex> entry : positionToVertex.entrySet()) {
                translatedPositionToVertex.put(entry.getKey().plus(t), entry.getValue());
            }
            positionToVertex = translatedPositionToVertex;
            for (Multiset.Entry<Network.Vertex> entry : neighbourDualVertices.entrySet()) {
                Network.Vertex v = entry.getElement();
                int m = entry.getMultiplicity();
                for (int i = 0; i < m; i++) {
                    MosaicCartogram.this.getRegion(v.getId()).neighbourDualVertices.removeOne(vertex);
                }
            }
            neighbourDualVertices.clear();
            for (Coordinate c : this) {
                for (Coordinate neighbour : c.neighbours()) {
                    Network.Vertex v = MosaicCartogram.this.getVertex(neighbour);
                    if (v != null && v != vertex) {
                        neighbourDualVertices.add(v);
                        MosaicCartogram.this.getRegion(v.getId()).neighbourDualVertices.add(vertex);
                    }
                }
            }
        }

        @Override
        protected void clear() {
            super.clear();
            positionToVertex.clear();
            neighbourDualVertices.clear();
            connectivityGraph.clear();
            hits = 0;
            connected = true;
            recomputeConnectivity = true;
        }

        private int computeOffsetQuality(Coordinate offset) {
            int matches = 0;
            for (Coordinate c : this) {
                Coordinate translated = c.plus(offset);
                if (guidingShape.contains(translated)) {
                    matches++;
                }
            }
            return matches;
        }

        private void setDesiredRegion(CellRegion desiredRegion, double factor, double tx, double ty, LinkedHashMap<Coordinate, Double> intersected) {
            this.guidingShape = desiredRegion;
            this.factor = factor;
            guidingShapeTranslation.setX(tx);
            guidingShapeTranslation.setY(ty);
            totalTranslation = zeroVector();
            this.qualitymap = intersected;
        }
    }

    public class CellRegion implements Iterable<Coordinate> {

        private LinkedHashSet<Coordinate> coordinates;
        private LinkedHashMultiset<Coordinate> neighbours;

        protected CellRegion() {
            coordinates = new LinkedHashSet<>();
            neighbours = new LinkedHashMultiset<>();
        }

        protected CellRegion(CellRegion other) {
            this.coordinates = new LinkedHashSet<>(other.coordinates);
            this.neighbours = new LinkedHashMultiset<>(other.neighbours);
        }

        public int size() {
            return coordinates.size();
        }

        public Set<Coordinate> neighbours() {
            return Collections.unmodifiableSet(neighbours);
        }

        public boolean contains(Coordinate c) {
            return coordinates.contains(c);
        }

        public boolean isEdge(Coordinate c) {
            boolean in = false;
            boolean out = false;
            for (Coordinate d : c.neighbours()) {
                if (this.contains(d)) {
                    in = true;
                } else {
                    out = true;
                }
                if (in && out) {
                    return true;
                }
            }
            return false;
        }

        public boolean intersects(CellRegion other) {
            for (Coordinate c : this) {
                if (other.contains(c)) {
                    return true;
                }
            }
            return false;
        }

        public int intersectionSize(CellRegion other) {
            int count = 0;
            for (Coordinate c : this) {
                if (other.contains(c)) {
                    count++;
                }
            }
            return count;
        }

        public boolean intersectsNeighbours(CellRegion other) {
            for (Coordinate c : this) {
                if (other.neighbours.contains(c)) {
                    return true;
                }
            }
            return false;
        }

        public boolean touches(CellRegion other) {
            for (Coordinate c : this.neighbours()) {
                if (other.contains(c)) {
                    return true;
                }
            }
            return false;
        }

        public Coordinate barycenter() {
            return getContainingCell(continuousBarycenter());
        }

        public Vector2D continuousBarycenter() {
            Vector2D result = new Vector2D(0, 0);
            for (Coordinate c : coordinates) {
                result.add(c.toVector2D());
            }
            result.multiply(1d / coordinates.size());
            return result;
        }

        public ArrayList<Point2D> computeOutlinePoints() {
            ArrayList<Point2D> outline = new ArrayList<>();
            if (!neighbours.iterator().hasNext()) {
                throw new RuntimeException("empty cartogram region");
            }
            Coordinate firstNeighbour = neighbours.iterator().next();
            Coordinate firstPosition = null;
            for (Coordinate c : firstNeighbour.neighbours()) {
                if (coordinates.contains(c)) {
                    firstPosition = c;
                    break;
                }
            }
            if (firstPosition == null) {
                throw new RuntimeException();
            }
            {
                int index = firstPosition.neighbourIndex(firstNeighbour);
                Cell cell = MosaicCartogram.this.getCell(firstPosition);
                Point2D firstPoint = cell.getBoundaryPoints()[index];
                outline.add(firstPoint);
            }
            Coordinate neighbour = firstNeighbour;
            Coordinate position = firstPosition;
            do {
                Coordinate nextNeighbour;
                {
                    Cell cell = MosaicCartogram.this.getCell(position);
                    Point2D[] points = cell.getBoundaryPoints();
                    int index = (position.neighbourIndex(neighbour) + 1) % points.length;
                    outline.add(points[index]);
                    Coordinate[] positionNeighbours = position.neighbours();
                    nextNeighbour = positionNeighbours[index];
                }
                if (!coordinates.contains(nextNeighbour)) {
                    neighbour = nextNeighbour;
                } else {
                    neighbour = nextNeighbour;
                    do {
                        Coordinate[] positionNeighbours = neighbour.neighbours();
                        int index = (neighbour.neighbourIndex(position) + 1) % positionNeighbours.length;
                        position = neighbour;
                        neighbour = positionNeighbours[index];
                    } while (coordinates.contains(neighbour));
                }
            } while (!neighbour.equals(firstNeighbour) || !position.equals(firstPosition));
            return outline;
        }

        public final Coordinate[] occupiedCoordinates() {
            Coordinate[] occupied = new Coordinate[coordinates.size()];
            int i = 0;
            for (Coordinate c : this) {
                occupied[i++] = c;
            }
            return occupied;
        }

        public final Set<Coordinate> coordinateSet() {
            return coordinates;
        }

        public void setCoordinates(Set<Coordinate> coordinates) {
            this.coordinates = new LinkedHashSet(coordinates);
        }

        public final MosaicCartogram containingCartogram() {
            return MosaicCartogram.this;
        }

        @Override
        public final Iterator<Coordinate> iterator() {
            return coordinates.iterator();
        }

        protected boolean addHexagon(Coordinate c) {
            boolean isNew = coordinates.add(c);
            if (isNew) {
                if (neighbours.contains(c)) {
                    neighbours.remove(c);
                }
                for (Coordinate neighbour : c.neighbours()) {
                    if (!contains(neighbour)) {
                        neighbours.add(neighbour);
                    }
                }
            }
            return isNew;
        }

        protected boolean removeHexagon(Coordinate c) {
            boolean exists = coordinates.remove(c);
            if (exists) {
                int thisCount = 0;
                for (Coordinate neighbour : c.neighbours()) {
                    int multiplicity = neighbours.getMultiplicity(neighbour);
                    if (multiplicity != 0) {
                        neighbours.removeOne(neighbour);
                    } else {
                        thisCount++;
                    }
                }
                if (thisCount > 0) {
                    neighbours.add(c, thisCount);
                }
            }
            return exists;
        }

        public void translate(Coordinate t) {
            LinkedHashSet<Coordinate> translatedHexagons = new LinkedHashSet<>();
            for (Coordinate c : coordinates) {
                translatedHexagons.add(c.plus(t));
            }
            coordinates = translatedHexagons;
            LinkedHashMultiset<Coordinate> translatedNeighbours = new LinkedHashMultiset<>();
            for (Multiset.Entry<Coordinate> entry : neighbours.entrySet()) {
                translatedNeighbours.add(entry.getElement().plus(t), entry.getMultiplicity());
            }
            neighbours = translatedNeighbours;
        }

        protected void clear() {
            coordinates.clear();
            neighbours.clear();
        }
    }

    public abstract class Cell {

        protected Network.Vertex v;

        protected Cell() {
        }

        public abstract Coordinate getCoordinate();

        public Network.Vertex getVertex() {
            return v;
        }

        /**
         * Returns an array containing the boundary points of this cell,
         * starting from the one that is common to both the first and the last
         * neighbors of this cell in the order returned by
         * this.getCoordinate().getNeighbours().
         */
        public abstract Point2D[] getBoundaryPoints();

        /**
         * Returns the boundary of this cell in a Path2D object in which the
         * points appear in the same order as in getBoundaryPoints().
         */
        public Path2D getBoundaryShape() {
            Path2D path = new Path2D.Double();
            Point2D[] points = getBoundaryPoints();
            path.moveTo(points[0].getX(), points[0].getY());
            for (int i = 1; i < points.length; i++) {
                path.lineTo(points[i].getX(), points[i].getY());
            }
            path.closePath();
            return path;
        }

        public Point2D getCenter() {
            return getCoordinate().toPoint2D();
        }

        public MosaicCartogram getContainingGrid() {
            return MosaicCartogram.this;
        }

        public void setVertex(Network.Vertex v) {
            this.v = v;
        }
    }

    public abstract static class Coordinate {

        protected Coordinate() {
        }

        public abstract int[] getComponents();

        public abstract Coordinate plus(Coordinate c);

        public abstract Coordinate minus(Coordinate c);

        public abstract Coordinate times(int k);

        public abstract Coordinate times(double k);

        public abstract Coordinate normalize();

        public abstract int norm();

        public abstract int dotProduct(Coordinate c);

        /**
         * Returns an array with the neighbors of this coordinate in
         * counterclockwise order starting from the rightmost one.
         */
        public abstract Coordinate[] neighbours();

        public abstract Coordinate[] connectedVicinity();

        public abstract Coordinate[] ring(int radius);

        public abstract Coordinate[] disk(int radius);

        public int neighbourIndex(Coordinate c) {
            Coordinate[] neighbours = neighbours();
            for (int i = 0; i < neighbours.length; i++) {
                if (c.equals(neighbours[i])) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * Returns the Euclidean 2D coordinate that represents the center of the
         * cell represented by this coordinate.
         */
        public abstract Vector2D toVector2D();

        /**
         * Returns the Euclidean 2D coordinate that represents the center of the
         * cell represented by this coordinate.
         */
        public abstract Point2D toPoint2D();

        @Override
        public abstract int hashCode();

        @Override
        public abstract boolean equals(Object obj);

        @Override
        public abstract String toString();
    }
}
