package arrange.algorithms.Moves;

import arrange.model.Cartogram.MosaicCartogram;
import arrange.model.Cartogram.MosaicCartogram.Coordinate;
import arrange.model.Network;

public abstract class Move implements Comparable<Move> {

    public double quality = Integer.MAX_VALUE;
    public double necessity = Integer.MIN_VALUE;
    public boolean valid = false;
    public boolean connected = false;
    public MosaicCartogram currentGrid;

    public abstract double evaluate();

    public abstract double execute();

    protected boolean isAlley(Coordinate c) {
        //Duplicate from mosaicHeuristic
        Network.Vertex neighbour = null;
        boolean singleRegion = true;
        int count = 0;
        Coordinate[] neighbours = c.neighbours();
        for (Coordinate d : neighbours) {
            Network.Vertex vertex = currentGrid.getVertex(d);
            if (vertex != null) {
                count++;
                if (neighbour == null) {
                    neighbour = vertex;
                } else if (neighbour != vertex) {
                    singleRegion = false;
                }
            }
        }
        if (count == neighbours.length - 1 && !singleRegion) {
            return true;
        }
        return false;
    }

    /**
     * The direction must be a unit vector, 'last' should be reachable by adding
     * 'direction' to 'first' a finite number of times.
     */
    protected void push(MosaicCartogram.Coordinate first, MosaicCartogram.Coordinate last, MosaicCartogram.Coordinate direction) {
        MosaicCartogram.Coordinate current = last;
        MosaicCartogram.Coordinate next = current.plus(direction);
        Network.Vertex u = currentGrid.getVertex(next);
        if (u != null) {
            currentGrid.removeCell(next);
        }
        while (!next.equals(first)) {
            Network.Vertex v = currentGrid.getVertex(current);
            if (v != null) {
                currentGrid.removeCell(current);
                currentGrid.setVertex(next, v);
            }
            current = current.minus(direction);
            next = next.minus(direction);
        }
    }

    /**
     * A region associated with a vertex v: (1) must be connected; (2) must be
     * adjacent to all neighbors of v; and (3) must not be connected to any
     * non-neighbor of v.
     */
    protected boolean gridIsValid() {
        return currentGrid.isValid();
    }

    /**
     * A region associated with a vertex v: (1) must be connected; (2) must be
     * adjacent to all neighbors of v; and (3) must not be connected to any
     * non-neighbor of v. The only region that can be invalid is {@code region}
     *
     * @param region the only region that can be invalid
     * @return
     */
    protected boolean gridIsValid(MosaicCartogram.MosaicRegion region) {
        return currentGrid.isValid(region);
    }

    /**
     * Returns whether the indicated region forms a connected component.
     *
     * @param region the only region that can be invalid
     * @return
     */
    protected boolean gridIsConnected(MosaicCartogram.MosaicRegion region) {
        return currentGrid.isConnected(region);
    }

    protected double evaluateGridQuality() {
        return currentGrid.quality(true);
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isConnected() {
        return connected;
    }

    public double getQuality() {
        return quality;
    }

    public double getNecessity() {
        return necessity;
    }

    @Override
    public int compareTo(Move move) {
        int compare = Double.compare(quality, move.quality);
        if (compare == 0) {
            return Double.compare(move.necessity, necessity);
        } else {
            return compare;
        }
    }
}
