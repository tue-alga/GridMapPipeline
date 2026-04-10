package arrange.algorithms;

import arrange.algorithms.sub.ForceDirectedLayout;
import arrange.algorithms.moves.TakeMove;
import arrange.algorithms.moves.ReleaseMove;
import common.gridmath.GridMath.Coordinate;
import arrange.model.MosaicCartogram;
import arrange.model.MosaicCartogram.MosaicRegion;
import common.dual.Dual;
import common.dual.Vertex;
import common.util.Stopwatch;

/**
 *
 * @author Rafael Cano, Wouter Meulemans
 */
public class MoveAndReshape {

    private final Dual dual;
    private MosaicCartogram grid = null;

    public MoveAndReshape(Dual dual) {
        this.dual = dual;
    }

    public MosaicCartogram run(MosaicCartogram grid, int max_no_improve_iterations) {

        this.grid = grid;

        assert grid.isConnectedAndCorrectAdjacencies();

        //Initializing done. Use force directed layout for rest
        ForceDirectedLayout forceDirectedLayout = new ForceDirectedLayout(grid, dual);

        //We improve it until we can not improve it within maxNoImproveIterations iterations
        int currentBadIterations = 0;
        //stores the quality of the previous iteration
        int symdiff = grid.totalSymmetricDifference();
        double relsymdiff = grid.totalRelativeSymmetricDifference();
        MosaicCartogram bestGrid = new MosaicCartogram(grid);

        Stopwatch sw_force = Stopwatch.get("- force");
        Stopwatch sw_reshape = Stopwatch.get("- reshape");

        int it = 0;
        while (currentBadIterations < max_no_improve_iterations) {
            //move the guiding shapes

            sw_force.start();
            forceDirectedLayout.run();
            sw_force.stop();

           // grid.export("Reshaping: guides moved " + it, 3);

            //update the regions with releasing and taking.
            sw_reshape.start();
            takeAndRelease();
            sw_reshape.stop();

           // grid.export("Reshaping: reshaped " + it, 3);

            //check if it improved
            int newsymdiff = grid.totalSymmetricDifference();
            if (newsymdiff > symdiff) {
                currentBadIterations++;
            } else {
                double newrelsymdiff = grid.totalRelativeSymmetricDifference();
                if (newsymdiff < symdiff || (newsymdiff == symdiff && newrelsymdiff < relsymdiff)) {
                    symdiff = newsymdiff;
                    relsymdiff = newrelsymdiff;
                    currentBadIterations = 0;
                    bestGrid = new MosaicCartogram(grid);
                    // grid.export("Reshaping: new best grid " + it, 2);
                } else {
                    currentBadIterations++;
                }
            }

            it++;
        }

        grid = bestGrid;

        assert grid.isConnectedAndCorrectAdjacencies();

        return grid;
    }

    private boolean takeAndRelease() {

        boolean somethingchanged = false;
        for (MosaicRegion region : grid.regions()) {
            // Try taking something from the neighbors of a region
            Vertex vertex = region.getVertex();

            //holds whether something changed. If so, then the neighbors need
            //to be update again
            boolean changed = true;
            while (changed) {
                changed = false;

                for (Coordinate position : region.takeCandidates()) {
                    // NB: we're changing the set as we go. But, we consider each rook's neighbor only once 
                    // and adding a rook's neighbor turn another rook's neighbor's status (i.e. to occupied, bishop's or no neighbor)
                    // only, there may be new rook's neighbors to consider (hence the while loop)

                    //try taking it
                    TakeMove tm = new TakeMove(grid, position, vertex);
                    if (tm.improves() && tm.canExecute(false)) {
                        tm.execute();
                        //neighbors changed, thus need to update the neighbors again
                        changed = true;
                        somethingchanged = true;
                    }
                }
            }
        }

        // Try releasing something
        for (MosaicRegion region : grid.regions()) {

            for (Coordinate c : region.releaseCandidates()) {
                ReleaseMove rm = new ReleaseMove(grid, c);
                if (rm.canExecute(false)) {
                    rm.execute();
                    somethingchanged = true;
                }
            }
        }

        return somethingchanged;
    }

}
