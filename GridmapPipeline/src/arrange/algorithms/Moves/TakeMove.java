package arrange.algorithms.moves;

import arrange.MosaicConstants;
import arrange.model.MosaicCartogram;
import common.gridmath.GridMath.Coordinate;
import arrange.model.MosaicCartogram.MosaicRegion;
import common.dual.Vertex;
import common.gridmath.AdjacencyType;

/**
 *
 * @author Max Sondag
 */
public final class TakeMove {

    private final MosaicCartogram grid;
    private final Coordinate position;
    private final Vertex newVertex;
    private final Vertex oldVertex;

    public TakeMove(MosaicCartogram grid, Coordinate position, Vertex vertex) {
        this.grid = grid;
        this.position = position;
        this.newVertex = vertex;
        this.oldVertex = grid.getVertex(position);
    }

    /**
     * Test whether take move will keep both regions connected and with the
     * correct adjacencies. The latter test is omitted when connectedSuffices ==
     * true.
     */
    public boolean canExecute(boolean connectedSuffices) {

        MosaicRegion newRegion = grid.getRegion(newVertex);
        if (!newRegion.isSimplyConnectedWith(position)) {
            return false;
        }
        if (!connectedSuffices && !newRegion.isAdjacencyCorrectWith(position)) {
            return false;
        }

        if (oldVertex != null) {
            // taking an occupied cell: check the old region

            MosaicRegion oldRegion = grid.getRegion(oldVertex);
            if (!oldRegion.isSimplyConnectedWithout(position)) {
                return false;
            }
            // NB: dont check connectivity between oldVertex and newVertex:
            // by assumption of initial connectivity, they remain adjacent
            if (!connectedSuffices && !oldRegion.isAdjacencyCorrectWithout(position, newRegion)) {
                return false;
            }
        }
        // else: taking an unoccupied cell, no need for further checks

        return true;
    }

    public void execute() {
        grid.setVertex(position, newVertex);
    }

    public boolean improves() {

        MosaicRegion newRegion = grid.getRegion(newVertex);
        if (!newRegion.isDesired(position)) {
            return false;
        }

        if (oldVertex == null) {
            //not occupied by another region
            return true;
        }

        MosaicRegion oldRegion = grid.getRegion(oldVertex);
        if (!oldRegion.isDesired(position)) {
            //other region does not want this position
            return true;
        }

        double newSD = newRegion.getSymmetricDifference();
        double oldSD = oldRegion.getSymmetricDifference();
        double newSize = newRegion.getGuidingShape().size();
        double oldSize = oldRegion.getGuidingShape().size();

        double currentError = Math.max(newSD / newSize, oldSD / oldSize);
        double newError = Math.max((newSD - 1) / newSize, (oldSD + 1) / oldSize);

        return newError < currentError;
    }

    public boolean createsHole() {
        if (oldVertex != null) {
            // cannot create a hole unless taking from the sea
            return false;
        }

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
}
