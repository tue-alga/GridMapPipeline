package arrange.algorithms;

import arrange.MosaicConstants;
import arrange.algorithms.sub.QualityMapConstruction;
import arrange.model.GuidingShape;
import common.Partition;
import common.gridmath.GridMath;
import common.gridmath.GridMath.Coordinate;
import common.gridmath.util.CoordinateMap;
import common.gridmath.util.CoordinateSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.tue.geometrycore.datastructures.priorityqueue.BasicIndexable;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Polygon;

/**
 *
 * @author Wouter Meulemans
 */
public class GuideConstructor {

    private final GridMath gridmath;
    private final Partition partition;
    private final int numTiles;
    private final double factor;

    public GuideConstructor(GridMath gridmath, Partition partition, int numTiles) {
        this.gridmath = gridmath;
        this.partition = partition;
        this.numTiles = numTiles;

        factor = Math.sqrt(numTiles * gridmath.getAverageCellArea() / partition.areaUnsigned());
    }

    private CoordinateMap<Double> constructQualityMap(Vector off) {

        Polygon shape = partition.clone();
        shape.scale(factor);
        shape.translate(off);

        QualityMapConstruction qmc = new QualityMapConstruction(gridmath, shape);
        return qmc.construct();
    }

    private DSP findConnection(CoordinateSet shape, CoordinateMap<Double> qmap) {

        CoordinateMap<DSP> dspmap = new CoordinateMap();
        DSP first = new DSP(shape.arbitraryCoordinate()); // NB: we dont take a random coordinate here to ensure deterministic behavior icw parallellism
        first.distance = 0;
        first.totalquality = 0;
        PriorityQueue<DSP> queue = new PriorityQueue<>(20, (a, b) -> {
            int c = Integer.compare(a.distance, b.distance);
            if (c != 0) {
                return c;
            }
            return Double.compare(b.totalquality, a.totalquality);
        });
        queue.add(first);
        dspmap.put(first.coordinate, first);

        int toFind = shape.size() - 1;

        // we want to find a path between components of few cells, but given the options, one with high quality cells
        // we're running a hybrid of BFS/Dijkstras:
        // moving over contained cells does not increase distance and quality
        // moving over uncontained cells does increase distance by 1 and quality according to the map
        while (!queue.isEmpty() && toFind > 0) {
            DSP dsp = queue.poll();

            for (Coordinate c : dsp.coordinate.adjacent(MosaicConstants.CONN_WITHIN_REGION)) {

                DSP nbr = dspmap.get(c);
                if (nbr == null) {
                    // newly found coordinate
                    nbr = new DSP(c);
                    if (shape.contains(c)) {
                        if (dsp.distance > 0) {
                            return dsp;
                        }
                        // NB: we're still doing the component of the initial coordinate here
                        // we're not interested in prev pointers here
                        nbr.distance = 0;
                        nbr.totalquality = 0;
                        toFind--;
                        queue.add(nbr);
                        dspmap.put(c, nbr);
                    } else if (qmap.containsKey(c)) { // stick to intersected cells
                        nbr.prev = dsp;
                        nbr.distance = dsp.distance + 1;
                        nbr.totalquality = dsp.totalquality + qmap.get(c);
                        dspmap.put(c, nbr);
                        queue.add(nbr);
                    }
                } // otherwise: existing coordinate -- nothing to do really, we know we explore the nodes in the right order already
            }
        }

        assert toFind == 0; // otherwise, we should have just continued, unless the queue is empty. Would suggest that the set of intersected cells is not contiguous

        return null;
    }

    private static class DSP {

        final Coordinate coordinate;
        int distance;
        double totalquality;
        DSP prev = null;

        public DSP(Coordinate coordinate) {
            this.coordinate = coordinate;
        }

        public void addExternalPath(CoordinateSet shape) {
            DSP curr = this;
            while (curr.distance > 0) {
                shape.add(curr.coordinate);
                curr = curr.prev;
            }
        }

    }

    private static class CoordinateQuality extends BasicIndexable {

        final Coordinate coordinate;
        final double quality;

        public CoordinateQuality(Coordinate coordinate, double quality) {
            this.coordinate = coordinate;
            this.quality = quality;
        }
    }

    private void connect(GuidingShape shape, CoordinateMap<Double> qmap) {

        while (true) {
            // find a path
            DSP newpath = findConnection(shape, qmap);
            if (newpath == null) {
                // connected!
                break;
            }

            // add the path
            newpath.addExternalPath(shape);
        }
    }

    private void initializeShape(GuidingShape shape, List<CoordinateQuality> coordinatesByQuality) {

        if (numTiles > coordinatesByQuality.size()) {
            System.exit(1);
        }

        for (int i = 0; i < numTiles; i++) {
            CoordinateQuality cq = coordinatesByQuality.get(i);
            shape.add(cq.coordinate);
        }
    }

    private void fillHoles(CoordinateSet shape) {

        shape.fillHoles(MosaicConstants.CONN_EXTERIOR);
    }

    private void ensureComplexity(CoordinateSet shape, CoordinateMap<Double> qmap) {

        final int surplus = shape.size() - numTiles;

        assert surplus >= 0 : "too few tiles?"; // shouldnt happen because we started with sufficient candidates

        if (surplus == 0) {
            return;
        }

        // because we added all holes, the coordinate set is now simply connected
        // keep a list of candidates to be removed, sorted by increasing quality
        TreeSet<CoordinateQuality> removeCandidates = new TreeSet<>((a, b) -> Double.compare(a.quality, b.quality));
        for (Coordinate c : shape.coordinates()) {
            if (shape.contiguousNeighborhood(c, MosaicConstants.CONN_WITHIN_REGION)) {
                removeCandidates.add(new CoordinateQuality(c, qmap.get(c)));
            }
        }

        // Remove corresponding number of tiles
        for (int i = 0; i < surplus; i++) {

            CoordinateQuality cq = removeCandidates.pollFirst();
            while (!shape.contiguousNeighborhood(cq.coordinate, MosaicConstants.CONN_WITHIN_REGION)) {
                cq = removeCandidates.pollFirst();
            }

            shape.remove(cq.coordinate);

            assert shape.isConnected(MosaicConstants.CONN_WITHIN_REGION);

            for (Coordinate d : cq.coordinate.adjacent(MosaicConstants.CONN_WITHIN_REGION)) {
                if (shape.contains(d) && shape.contiguousNeighborhood(d, MosaicConstants.CONN_WITHIN_REGION)) {
                    Double dqual = qmap.get(d);
                    if (dqual == null) {
                        dqual = 0.0;
                    }
                    removeCandidates.add(new CoordinateQuality(d, dqual));
                }
            }

        }

        assert shape.size() != numTiles : "Guiding shape with wrong number of tiles";
    }

    public GuidingShape compute() {
        final int ns = MosaicConstants.GUIDING_SHAPE_SAMPLES;
        Vector[] span = gridmath.getSamplingSpan();
        span[0].scale(1.0 / ns);
        span[1].scale(1.0 / ns);

        GuidingShape best = null;
        
        if (MosaicConstants.GUIDING_SHAPE_THREADS <= 1) {

            for (int i = 0; i < ns; i++) {
                for (int j = 0; j < ns; j++) {

                    TestOffset to = new TestOffset(Vector.add(Vector.multiply(i, span[0]), Vector.multiply(j, span[1])));
                    GuidingShape g = to.compute();
                    if (best == null || best.getQuality() < g.getQuality()) {
                        best = g;
                    }
                }
            }

        } else {
            ExecutorService exec = Executors.newFixedThreadPool(MosaicConstants.GUIDING_SHAPE_THREADS);

            List<Future<GuidingShape>> guides = new ArrayList();

            for (int i = 0; i < ns; i++) {
                for (int j = 0; j < ns; j++) {

                    Future<GuidingShape> guide = exec.submit(new TestOffset(Vector.add(Vector.multiply(i, span[0]), Vector.multiply(j, span[1]))));
                    guides.add(guide);
                }
            }

            for (Future<GuidingShape> guide : guides) {
                try {
                    GuidingShape g = guide.get();
                    if (best == null || best.getQuality() < g.getQuality()) {
                        best = g;
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(GuideConstructor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            exec.shutdown();
        }

        return best;
    }

    private class TestOffset implements Callable<GuidingShape> {

        final Vector offset;

        public TestOffset(Vector offset) {
            this.offset = offset;            
        }

        public GuidingShape compute() {

            CoordinateMap<Double> qmap = constructQualityMap(offset);

            // Sort the candidates according to the area of the intersection
            ArrayList<CoordinateQuality> coordinatesByQuality = new ArrayList<>(qmap.size());
            for (CoordinateMap<Double>.Entry entry : qmap.entrySet()) {
                coordinatesByQuality.add(new CoordinateQuality(entry.getCoordinate(), entry.getValue()));
            }
            // highest quality first
            Collections.sort(coordinatesByQuality, (a, b) -> Double.compare(b.quality, a.quality));

            GuidingShape shape = new GuidingShape(gridmath, qmap, factor, offset);
            initializeShape(shape, coordinatesByQuality);
            connect(shape, qmap);

            assert shape.isConnected(MosaicConstants.CONN_WITHIN_REGION);

            fillHoles(shape);

            assert shape.isConnected(MosaicConstants.CONN_WITHIN_REGION);

            ensureComplexity(shape, qmap);

            assert shape.isConnected(MosaicConstants.CONN_WITHIN_REGION);

            shape.constructionDone();

            return shape;
        }

        @Override
        public GuidingShape call() throws Exception {
            return compute();
        }

    }
}
