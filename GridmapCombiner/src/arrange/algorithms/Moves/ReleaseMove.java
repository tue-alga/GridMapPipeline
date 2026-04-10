package arrange.algorithms.Moves;

import arrange.model.Cartogram.MosaicCartogram;
import arrange.model.Cartogram.MosaicCartogram.MosaicRegion;
import arrange.model.Network;

/**
 *
 * @author Max Sondag
 */
public final class ReleaseMove extends Move {

    private final MosaicCartogram.Coordinate c;
    private boolean improves = false;

    public ReleaseMove(MosaicCartogram currentGrid, MosaicCartogram.Coordinate c) {
        this.currentGrid = currentGrid;
        this.c = c;
    }

    @Override
    public double evaluate() {
        Network.Vertex v = currentGrid.getVertex(c);
        int oldDiff = currentGrid.getRegion(v.getId()).getSymmetricDifference();

        MosaicRegion oldRegion = currentGrid.getRegion(v.getId());
        currentGrid.removeCell(c);

        //whether the mosaicregion of v stays connected after removing this node
        connected = gridIsConnected(oldRegion);

        if (gridIsValid(oldRegion)) {
            quality = evaluateGridQuality();
            valid = true;
            int newDiff = currentGrid.getRegion(v.getId()).getSymmetricDifference();
            if (newDiff < oldDiff) {
                improves = true;
            } else {
                improves = false;
            }
        } else {
            valid = false;
        }
        currentGrid.setVertex(c, v);
        return quality;
    }

    @Override
    public double execute() {
        currentGrid.removeCell(c);
        return quality;
    }

    public boolean improves() {
        return improves;
    }

    public boolean createsHole() {
        for (MosaicCartogram.Coordinate d : c.neighbours()) {
            if (currentGrid.getVertex(d) == null) {
                return false;
            }
        }
        return true;
    }

    public boolean isConnected() {
        return connected;
    }

}
