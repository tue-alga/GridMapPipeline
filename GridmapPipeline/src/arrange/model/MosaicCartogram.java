package arrange.model;

import arrange.MosaicConstants;
import common.gridmath.GridMath;
import arrange.algorithms.GuideConstructor;
import common.gridmath.GridMath.Coordinate;
import arrange.util.Identifier;
import common.Partition;
import common.dual.Dual;
import common.dual.Vertex;
import common.gridmath.util.CoordinateMap;
import common.gridmath.util.CoordinateSet;
import common.util.Stopwatch;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometryrendering.styling.Dashing;
import nl.tue.geometrycore.geometryrendering.styling.Hashures;
import nl.tue.geometrycore.gui.debug.DebugPage;

/**
 *
 * @author Rafael Cano, Wouter Meulemans
 */
public class MosaicCartogram {

    private GridMath gridmath;
    private final CoordinateMap<Vertex> cells;
    private final MosaicRegion[] regions;
    private final AdjacencyTracker adjacencies;

    public MosaicCartogram(GridMath gridmath, Dual dual) {
        this.gridmath = gridmath;
        this.cells = new CoordinateMap<>();
        this.regions = new MosaicRegion[dual.vertexCount()];

        for (Vertex v : dual.getVertices()) {
            regions[v.getGraphIndex()] = new MosaicRegion(v);
        }

        adjacencies = new AdjacencyTracker(regions);
    }

    public MosaicCartogram(MosaicCartogram other) {
        this.gridmath = other.gridmath;
        cells = new CoordinateMap<>(other.cells);
        this.regions = new MosaicRegion[other.regions.length];
        for (int i = 0; i < regions.length; i++) {
            this.regions[i] = new MosaicRegion(other.regions[i]);
        }
        adjacencies = new AdjacencyTracker(other.adjacencies);
    }

    public GridMath getGridMath() {
        return gridmath;
    }

    public final int numberOfCells() {
        return cells.size();
    }

    public Vertex setVertex(Coordinate c, Vertex v) {
        if (c == null || v == null) {
            throw new NullPointerException();
        }
        regions[v.getGraphIndex()].addCoordinate(c);
        Vertex old = cells.get(c);
        if (old == null) {
            cells.put(c, v);
            return null;
        } else {
            regions[old.getGraphIndex()].removeCoordinate(c);
            cells.put(c, v);
            return old;
        }
    }

    public Vertex getVertex(Coordinate c) {
        return cells.get(c);
    }

    public Vertex removeCell(Coordinate c) {
        Vertex old = cells.remove(c);
        if (old != null) {
            regions[old.getGraphIndex()].removeCoordinate(c);
        }
        return old;
    }

    public Coordinate[] getCoordinateArray() {
        return cells.keyArray();
    }

    public void translateRegions(Iterable<Vertex> vertices, Coordinate t) {

        // we can probably do this more cleverly:
        // - updating cells only in the difference between the old and the new position
        // - not recomputing all adjacencies (currently in region.translate)
        // but, this is only used in sliding and not the main time sink...
        for (Vertex v : vertices) {
            MosaicRegion region = regions[v.getGraphIndex()];
            for (Coordinate c : region.allocated.coordinates()) {
                cells.remove(c);
            }
        }

        for (Vertex v : vertices) {
            MosaicRegion region = regions[v.getGraphIndex()];
            region.translate(t);

            for (Coordinate c : region.allocated.coordinates()) {
                Vertex old = cells.get(c);
                if (old != null) {
                    regions[old.getGraphIndex()].removeCoordinate(c);
                }
                cells.put(c, v);
            }
        }
    }

    public MosaicRegion[] regions() {
        return regions;
    }

    public int numberOfRegions() {
        return regions.length;
    }

    public MosaicRegion getRegion(Vertex v) {
        return regions[v.getGraphIndex()];
    }

    public MosaicRegion getRegion(Coordinate c) {
        Vertex v = getVertex(c);
        if (v != null) {
            return regions[v.getGraphIndex()];
        }
        return null;
    }

    public boolean isConnectedAndCorrectAdjacencies() {
        for (MosaicRegion region : regions) {
            if (!region.isConnected() || !region.isAdjacencyCorrect()) {
                return false;
            }
        }
        return true;
    }

    public boolean isConnected() {
        for (MosaicRegion region : regions) {
            if (!region.isConnected()) {
                return false;
            }
        }
        return true;
    }

    public int totalTileError() {
        int totalError = 0;
        for (MosaicRegion region : regions) {
            totalError += Math.abs(region.getTileError());
        }
        return totalError;
    }

    public int totalSymmetricDifference() {
        int total = 0;
        for (MosaicRegion region : regions) {
            if (region.getSymmetricDifference() < 0) {
                System.err.println("?!?!?!?");
            }
            total += region.getSymmetricDifference();
        }
        return total;
    }

    public double totalRelativeSymmetricDifference() {
        double total = 0;
        for (MosaicRegion region : regions) {
            total += region.getSymmetricDifference() / region.getGuidingShape().size();
        }
        return total;
    }

    /**
     * Assumes that the coordinates are connected.
     */
    public Coordinate[] computeHoleBoundaries() {

        CoordinateSet set = new CoordinateSet(gridmath);
        cells.addKeysToSet(set);
        return set.detectHoleBoundaries(MosaicConstants.CONN_EXTERIOR);

    }

    /**
     *
     * @param cellWeight this indicates how much weight is to represent a single
     * cell. So, #cells = faceWeight / cellWeight. This is rounded and maxed
     * with 1.
     */
    public final void initializeGuidingShapes(double cellWeight) {

        Stopwatch sw = Stopwatch.get("- guides").start();

        for (MosaicRegion region : regions) {
            Partition partition = region.getPartition();
            int numTiles = Math.max(1, (int) Math.round(partition.getWeight() / cellWeight));
            GuideConstructor guide = new GuideConstructor(gridmath, partition, numTiles);
            region.guide = guide.compute();

            // position
            if (!region.allocated.isEmpty()) {
                Vector baryRegion = region.allocated.continuousBarycenter();
                Vector baryGuide = region.guide.continuousBarycenter();
                Vector diff = Vector.subtract(baryRegion, baryGuide);
                region.translateGuidingShape(gridmath.closestEquivalance(diff));
            }
        }

        sw.stop();
    }

    public final void repositionGuidingShapes() {

        for (MosaicRegion region : regions) {
            if (!region.allocated.isEmpty()) {
                Vector baryRegion = region.allocated.continuousBarycenter();
                Vector baryGuide = region.guide.continuousBarycenter();
                Vector diff = Vector.subtract(baryRegion, baryGuide);
                region.translateGuidingShape(gridmath.closestEquivalance(diff));
            }
        }
    }

    public void refineGridMath() {
        // either, current gridmath has pc 1, or matches the new gridmath in terms of complexity        

        assert gridmath.geometry().getSelfRefinableFactor() < Integer.MAX_VALUE;

        Coordinate origin = gridmath.origin();

        // supplant everything
        CoordinateMap<Vertex> cellmap = new CoordinateMap<>(cells);
        cells.clear();
        for (CoordinateMap<Vertex>.Entry e : cellmap.entrySet()) {
            final Coordinate c = e.getCoordinate();
            final Vertex v = e.getValue();

            for (Coordinate rc : gridmath.geometry().getRefinementFor(c, origin)) {
                cells.put(rc, v);
            }
        }

        for (MosaicRegion region : regions) {
            CoordinateSet alloc = new CoordinateSet(gridmath);
            for (Coordinate c : region.allocated) {
                for (Coordinate rc : gridmath.geometry().getRefinementFor(c, origin)) {
                    alloc.add(rc);
                }
            }
            region.allocated = alloc;
            region.guide = null; // for now, save the effort, we set the guiding shapes first thing in any case
        }

        // we can probably do this more cleverly, but it's not the main time sink in any case
        for (MosaicRegion region : regions) {
            region.resetAdjacencies();
        }
    }

    public void refineGridMath(GridMath newgridmath) {
        // either, current gridmath has pc 1, or matches the new gridmath in terms of complexity        
        assert newgridmath.getPatternComplexity() / gridmath.getPatternComplexity() == 0 : "no proper refinement?";

        int dpc = newgridmath.getPatternComplexity() / gridmath.getPatternComplexity();

        Coordinate origin = newgridmath.origin();

        // supplant everything
        CoordinateMap<Vertex> cellmap = new CoordinateMap<>(cells);
        cells.clear();
        for (CoordinateMap<Vertex>.Entry e : cellmap.entrySet()) {
            final Coordinate c = e.getCoordinate();
            final Vertex v = e.getValue();
            for (int i = 0; i < dpc; i++) {
                cells.put(origin.plus(dpc * c.x + i, c.y), v);
            }
        }

        for (MosaicRegion region : regions) {
            CoordinateSet alloc = new CoordinateSet(newgridmath);
            for (Coordinate c : region.allocated) {
                for (int i = 0; i < dpc; i++) {
                    alloc.add(origin.plus(dpc * c.x + i, c.y));
                }
            }
            region.allocated = alloc;
            region.guide = null; // for now, save the effort, we set the guiding shapes first thing in any case
        }

        gridmath = newgridmath;

        // we can probably do this more cleverly, but it's not the main time sink in any case
        for (MosaicRegion region : regions) {
            region.resetAdjacencies();
        }
    }

    public boolean isAlley(Coordinate c) {
        Vertex neighbor = null;
        boolean singleRegion = true;
        int count = 0;
        Coordinate[] neighbors = c.adjacent(MosaicConstants.CONN_EXTERIOR);
        for (Coordinate d : neighbors) {
            Vertex vertex = cells.get(d);
            if (vertex != null) {
                count++;
                if (neighbor == null) {
                    neighbor = vertex;
                } else if (neighbor != vertex) {
                    singleRegion = false;
                }
            }
        }
        return count == neighbors.length - 1 && !singleRegion;
    }

    public final class MosaicRegion implements Identifier {

        private CoordinateSet allocated;
        private GuidingShape guide;

        private final Vertex vertex;

        // statistics about region
        private int hits;
        private boolean connected;
        private boolean recomputeConnectivity; // TODO: we no longer need to track this after improving slider: connectivityqueries only used in asserts...

        protected MosaicRegion(Vertex vertex) {
            this.vertex = vertex;
            this.allocated = new CoordinateSet(gridmath);
            this.guide = new GuidingShape(gridmath, new CoordinateMap<>(), 1, Vector.origin()); // dummy

            this.hits = 0;
            this.connected = true;
            this.recomputeConnectivity = false;
        }

        protected MosaicRegion(MosaicRegion other) {
            this.vertex = other.vertex;
            this.allocated = new CoordinateSet(other.allocated);
            this.guide = new GuidingShape(other.guide);

            this.hits = other.hits;
            this.connected = other.connected;
            this.recomputeConnectivity = other.recomputeConnectivity;
        }

        @Override
        public int getId() {
            return vertex.getGraphIndex();
        }

        public Vertex getVertex() {
            return vertex;
        }

        public Partition getPartition() {
            return vertex.getPartition();
        }

        public CoordinateSet getAllocated() {
            return allocated;
        }

        public GuidingShape getGuidingShape() {
            return guide;
        }

        public boolean isConnected() {
            if (recomputeConnectivity) {
                connected = allocated.isConnected(MosaicConstants.CONN_WITHIN_REGION);
                recomputeConnectivity = false;
            }
            return connected;
        }

        public boolean isAdjacencyCorrect() {
            for (MosaicRegion r : regions) {
                boolean adjacent = adjacencies.get(this, r) > 0;
                boolean shouldbe = vertex.isNeighborOf(r.vertex);
                if (adjacent != shouldbe) {
                    return false;
                }
            }
            return true;
        }

        public boolean isDesired(Coordinate c) {
            return guide.contains(c);
        }

        public int getSymmetricDifference() {
            return allocated.size() + guide.size() - 2 * hits;
        }

        public int getTileError() {
            return guide.size() - allocated.size();
        }

        public void translateGuidingShape(Coordinate t) {
            assert t.isEquivalanceOffset();

            guide.translate(t);
            hits = allocated.intersectionSize(guide);
        }

        public List<Coordinate> takeCandidates() {
            List<Coordinate> tcs = new ArrayList();

            for (Coordinate c : allocated.neighbors(MosaicConstants.CONN_WITHIN_REGION)) {
                if (guide.contains(c)) {
                    tcs.add(c);
                }
            }

            tcs.sort((a, b) -> Double.compare(guide.desirability(b), guide.desirability(a))); // highest first!            
            return tcs;
        }

        public List<Coordinate> releaseCandidates() {
            List<Coordinate> tcs = new ArrayList();

            for (Coordinate c : allocated) {
                if (!guide.contains(c)) {
                    tcs.add(c);
                }
            }

            tcs.sort((a, b) -> Double.compare(guide.desirability(a), guide.desirability(b))); // lowest first!            
            return tcs;
        }

        public boolean addCoordinate(Coordinate c) {
            boolean isNew = allocated.add(c);

            if (!isNew) {
                throw new RuntimeException("Added existing tile to region");
            }

            if (isDesired(c)) {
                hits++;
            }

            if (recomputeConnectivity) {
                // skip, will be recomputing in any case
            } else if (connected) {
                // currently connected, addition must simply contain a neighbor, if the set was not empty
                connected = allocated.size() == 1 || allocated.containsSome(c.adjacent(MosaicConstants.CONN_WITHIN_REGION));
            } else {
                // not connected, see if adding this coordinate may have connected it all
                recomputeConnectivity = !allocated.contiguousNeighborhood(c, MosaicConstants.CONN_WITHIN_REGION);
            }

            for (Coordinate neighbor : c.adjacent(MosaicConstants.CONN_BETWEEN_REGIONS)) {
                MosaicRegion r = MosaicCartogram.this.getRegion(neighbor);
                if (r != this && r != null) {
                    adjacencies.increase(this, r);
                }
            }

            return true;
        }

        public boolean removeCoordinate(Coordinate c) {
            boolean tileExists = allocated.remove(c);
            if (!tileExists) {
                throw new RuntimeException("Removed non-existing tile from region");
            }
            if (isDesired(c)) {
                hits--;
            }

            if (recomputeConnectivity) {
                // skip, will recompute in any case
            } else if (connected) {
                // connected, removal may disrupt connectivity
                recomputeConnectivity = !allocated.contiguousNeighborhood(c, MosaicConstants.CONN_WITHIN_REGION);
            } else {
                // not connected, removal may cause connectivity, if none of its neighbors was in the set
                recomputeConnectivity = allocated.containsNone(c.adjacent(MosaicConstants.CONN_WITHIN_REGION));
            }

            for (Coordinate neighbor : c.adjacent(MosaicConstants.CONN_BETWEEN_REGIONS)) {
                MosaicRegion r = MosaicCartogram.this.getRegion(neighbor);
                if (r != this && r != null) {
                    adjacencies.decrease(this, r);
                }
            }

            return true;
        }

        public void translate(Coordinate t) {
            assert t.isEquivalanceOffset();

            allocated.translate(t);
            guide.translate(t);

            resetAdjacencies();
        }

        public void resetAdjacencies() {

            for (MosaicRegion r : regions) {
                adjacencies.set(this, r, 0);
            }

            // need to determine the number of rooks-adjacent pairs
            // we iterate over the rook's neighbors of the allocated set, 
            // and then consider whether each of its rook's neighbors is in the allocated set
            // (this will be more efficient for large compact sets, as the boundary is smaller than the interior)
            for (Coordinate nbr : allocated.neighbors(MosaicConstants.CONN_BETWEEN_REGIONS)) {
                for (Coordinate c : nbr.adjacent(MosaicConstants.CONN_BETWEEN_REGIONS)) {
                    if (allocated.contains(c)) {
                        MosaicRegion r = MosaicCartogram.this.getRegion(nbr);
                        if (r != this && r != null) {
                            adjacencies.increase(this, r);
                        }
                    }
                }
            }
        }

        public boolean isSimplyConnectedWithout(Coordinate c) {
            // assume it's simply connected with c
            assert allocated.contains(c);
            assert allocated.isConnected(MosaicConstants.CONN_WITHIN_REGION);
            assert allocated.detectHoleBoundaries(MosaicConstants.CONN_WITHIN_REGION).length == 0;

            return allocated.contiguousNeighborhood(c, MosaicConstants.CONN_WITHIN_REGION);
        }

        public boolean isSimplyConnectedWith(Coordinate c) {
            // assume it's simply connected without c
            assert !allocated.contains(c);
            assert allocated.isConnected(MosaicConstants.CONN_WITHIN_REGION);
            assert allocated.detectHoleBoundaries(MosaicConstants.CONN_WITHIN_REGION).length == 0;

            return allocated.contiguousNeighborhood(c, MosaicConstants.CONN_WITHIN_REGION);
        }

        public boolean isAdjacencyCorrectWithout(Coordinate c, MosaicRegion ignore) {
            assert allocated.contains(c);
            assert isAdjacencyCorrect();

            // NB: removing a coordinate cannot create new connections
            // just need to make sure that we dont lose an existing one
            Map<MosaicRegion, Integer> reduc = new HashMap<>();

            for (Coordinate neighbor : c.adjacent(MosaicConstants.CONN_NONADJACENT_AVOIDS)) {
                MosaicRegion r = MosaicCartogram.this.getRegion(neighbor);
                if (r != this && r != null && r != ignore) {
                    int red = reduc.getOrDefault(r, 0) + 1;
                    reduc.put(r, red);
                }
            }

            for (Entry<MosaicRegion, Integer> e : reduc.entrySet()) {
                if (adjacencies.get(e.getKey(), this) <= e.getValue()) {
                    // goes to zero, disconnects                  
                    return false;
                }
            }

            return true;
        }

        public boolean isAdjacencyCorrectWith(Coordinate c) {
            assert !allocated.contains(c);
            assert isAdjacencyCorrect();

            // NB: adding a coordinate cannot remove existing connections
            // just need to make sure that we dont add nonexisting one
            for (Coordinate neighbor : c.adjacent(MosaicConstants.CONN_NONADJACENT_AVOIDS)) {
                MosaicRegion r = MosaicCartogram.this.getRegion(neighbor);
                // assuming initial correct adjacencies
                if (r != this && r != null && !adjacencies.areAdjacent(this, r)) {
                    return false;
                }
            }

            return true;
        }
    }

    public void export(String tag, int level) {

        if (MosaicConstants.DRAW_INTERMEDIATES == null || level > MosaicConstants.DRAW_INTERMEDIATES_DETAIL) {
            return;
        }

        DebugPage page = MosaicConstants.DRAW_INTERMEDIATES.addPage(tag);
        page.newView("regions");
        page.newView("regions", "guides");
        page.newView("guides");

        for (MosaicRegion region : regions) {
            page.setStroke(Color.BLACK, 0.04, Dashing.SOLID);
            page.setFill(region.vertex.getPartition().color, Hashures.SOLID);

            page.setLayer("regions");
            page.pushGroup();
            for (Coordinate c : region.allocated) {
                page.draw(c.getBoundary());
            }
            page.popGroup();

            page.setLayer("guides");
            page.pushGroup();
            for (Coordinate c : region.guide) {
                page.draw(c.getBoundary());
            }
            page.popGroup();
        }

        MosaicConstants.DRAW_INTERMEDIATES.notifyPageDone();
    }
}
