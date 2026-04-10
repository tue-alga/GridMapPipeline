package arrange.algorithms.Moves;

import arrange.model.Cartogram.MosaicCartogram;
import arrange.model.Network;

public final class SwapMove extends Move {

    private MosaicCartogram.Coordinate c1;
    private MosaicCartogram.Coordinate c2;
    private Network.Vertex v1;
    private Network.Vertex v2;

    public SwapMove(MosaicCartogram currentGrid,MosaicCartogram.Coordinate c1, MosaicCartogram.Coordinate c2) {
        this.currentGrid = currentGrid;
        this.c1 = c1;
        this.c2 = c2;
        this.v1 = currentGrid.getVertex(c1);
        this.v2 = currentGrid.getVertex(c2);
    }

    @Override
    public double evaluate() {
        currentGrid.setVertex(c1, v2);
        currentGrid.setVertex(c2, v1);
        valid = gridIsValid();
        currentGrid.setVertex(c1, v1);
        currentGrid.setVertex(c2, v2);
        return 0;
    }

    @Override
    public double execute() {
        currentGrid.setVertex(c1, v2);
        currentGrid.setVertex(c2, v1);
        return 0;
    }
}
