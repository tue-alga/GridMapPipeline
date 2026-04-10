package arrange.algorithms.moves;

import arrange.MosaicConstants;
import common.gridmath.GridMath.Coordinate;
import arrange.model.MosaicCartogram;
import arrange.model.MosaicCartogram.MosaicRegion;
import common.dual.Vertex;
import common.gridmath.AdjacencyType;

/**
 *
 * @author Max Sondag
 */
public final class ReleaseMove {

    private final MosaicCartogram grid;
    private final Coordinate position;

    public ReleaseMove(MosaicCartogram grid, Coordinate c) {
        this.grid = grid;
        this.position = c;
    }

    /**
     * Test whether release move will keep the region connected and with the
     * correct adjacencies. The latter test is omitted when connectedSuffices ==
     * true. 
     */
    public boolean canExecute(boolean connectedSuffices) {
        MosaicRegion region = grid.getRegion(position);
        
        if (!region.isSimplyConnectedWithout(position)) {
            return false;
        }
        
        if (!connectedSuffices && !region.isAdjacencyCorrectWithout(position, null)) {
            return false;
        }
        
        return true;
    }
    
    public boolean createsHole() {
                        
         // adapted from CoordinateSet::contiguousNeighborhood
        Coordinate[] nbrs = position.adjacent(AdjacencyType.QUEENS);
        boolean[] raq = MosaicConstants.CONN_EXTERIOR == AdjacencyType.ROOKS ? position.rookAmongQueensSignature() : null; 

        boolean prevExterior = grid.getVertex(nbrs[nbrs.length - 1]) == null;
        int exteriorRuns = 0;
        boolean runToConsider = prevExterior && (raq == null || raq[nbrs.length - 1]);

        for (int i = 0; i < nbrs.length; i++) {
            Coordinate nbr = nbrs[i];

            boolean ext = grid.getVertex(nbr) == null;
            if (ext == prevExterior) {
                // keep going

                // NB: raq != null if runToConsider is to be false
                runToConsider = runToConsider || (ext && !runToConsider && raq[i]);
            } else {
                if (runToConsider) {
                    exteriorRuns++;
                }
                prevExterior = ext;

                runToConsider = ext && (raq == null || raq[i]);
            }
        }
        // taking this cell will create a hole if there is more than one run of adjacent exterior cells
        return exteriorRuns != 1;
    }

    /**
     * Performs the move.
     */
    public void execute() {
        Vertex v = grid.getVertex(position);
        grid.removeCell(position);
    }

}
