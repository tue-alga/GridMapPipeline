/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package arrange.algorithms;

import arrange.algorithms.sub.Polisher;
import arrange.MosaicConstants;
import arrange.algorithms.moves.TakeMove;
import arrange.model.MosaicCartogram;
import arrange.model.MosaicCartogram.MosaicRegion;
import common.dual.Dual;
import common.dual.Vertex;
import common.gridmath.AdjacencyType;
import common.gridmath.GridMath.Coordinate;
import common.gridmath.util.CoordinateSet;
import common.util.Stopwatch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Rafael Cano, Wouter Meulemans
 */
public class MosaicFinalizer {

    private final Dual dual;
    private final MosaicCartogram grid;
    private final int[] neighborMultiplicity; // just a temporary array 

    public MosaicFinalizer(Dual dual, MosaicCartogram grid) {
        this.dual = dual;
        this.grid = grid;
        this.neighborMultiplicity = new int[grid.regions().length];
    }

    private boolean checkExact() {
        if (!MosaicConstants.EXACT_TILES) {
            return true;
        }

        boolean ok = true;
        for (Vertex v : dual.getVertices()) {
            MosaicCartogram.MosaicRegion region = grid.getRegion(v);
            int size = region.getAllocated().size();
            int desiredSize = region.getGuidingShape().size();
            if (size != desiredSize) {
                System.out.println("  Incorrect size for " + v.getPartition().label);
                ok = false;
            }
        }
        return ok;
    }

    public void run() {

        Stopwatch sw = Stopwatch.get("- finalizer").start();

        fillHoles();
        fillAlleys();

        // grid.export("Finalizing: holes and alleys filled", 1);
        Polisher p = new Polisher(dual, grid);
        p.polish();

        assert checkExact();

        //If the exact amount of tiles is required, constraints are reduced. Holes and topology violations are then allowed.    
        assert MosaicConstants.EXACT_TILES || grid.isConnectedAndCorrectAdjacencies();
        assert !MosaicConstants.EXACT_TILES || grid.isConnected();

        sw.stop();

    }

    private class Candidate {

        Vertex vertex;
        int multiplicity;

        public Candidate(Vertex vertex, int multiplicity) {
            this.vertex = vertex;
            this.multiplicity = multiplicity;
        }

    }

    private List<Candidate> determineSortedCandidates(Coordinate c) {
        Arrays.fill(neighborMultiplicity, 0);

        for (Coordinate d : c.adjacent(AdjacencyType.ROOKS)) {
            Vertex vertex = grid.getVertex(d);
            if (vertex != null) {
                neighborMultiplicity[vertex.getGraphIndex()]++;
            }
        }

        List<Candidate> candidates = new ArrayList<>();
        for (MosaicRegion r : grid.regions()) {
            if (neighborMultiplicity[r.getId()] > 0) {
                candidates.add(new Candidate(r.getVertex(), neighborMultiplicity[r.getId()]));
            }
        }
        Collections.sort(candidates, (a, b) -> Integer.compare(b.multiplicity, a.multiplicity)); // NB: highest multiplicity first

        return candidates;
    }

    private void fillHoles() {

        Coordinate[] holes = grid.computeHoleBoundaries();;
        int rnd = 0;

        boolean tookSomething = true;
        while (tookSomething) {

            //grid.export("Finalizing: holes " + rnd, 2);

            rnd++;

            tookSomething = false;
            for (Coordinate c : holes) {
                List<Candidate> candidates = determineSortedCandidates(c);

                for (Candidate cand : candidates) {
                    TakeMove tm = new TakeMove(grid, c, cand.vertex);
                    if (tm.canExecute(false)) {
                        tm.execute();
                        tookSomething = true;
                        break;
                    }
                }
            }
            if (tookSomething) {
                holes = grid.computeHoleBoundaries();
            }
        }

        if (holes.length > 0) {
            System.err.println("Warning: holes remaining");
        }

        //grid.export("Finalizing: holes " + rnd, 2);
    }

    private void fillAlleys() {
        CoordinateSet alleys;
        CoordinateSet ignoreList = new CoordinateSet(grid.getGridMath());

        boolean somethingChanged = true;
        while (somethingChanged) {
            somethingChanged = false;
            alleys = computeAlleys();
            for (Coordinate c : alleys) {
                if (ignoreList.contains(c)) {
                    continue;
                }

                List<Candidate> candidates = determineSortedCandidates(c);

                boolean filled = false;
                for (Candidate cand : candidates) {
                    TakeMove tm = new TakeMove(grid, c, cand.vertex);
                    if (tm.canExecute(false)) {
                        tm.execute();
                        filled = true;
                        break;
                    }
                }
                if (!filled) {
                    ignoreList.add(c);
                } else {
                    somethingChanged = true;
                }
            }
        }
    }

    private CoordinateSet computeAlleys() {
        CoordinateSet alleys = new CoordinateSet(grid.getGridMath());
        for (MosaicCartogram.MosaicRegion region : grid.regions()) {
            for (Coordinate c : region.getAllocated().neighbors(MosaicConstants.CONN_BETWEEN_REGIONS)) {
                if (grid.getVertex(c) == null && grid.isAlley(c)) {
                    alleys.add(c);
                }
            }
        }
        return alleys;
    }
}
