package arrange.algorithms.Moves;

import java.util.ArrayList;
import java.util.Set;
import arrange.model.Cartogram.MosaicCartogram;
import arrange.model.Cartogram.MosaicCartogram.Coordinate;
import arrange.model.Cartogram.MosaicCartogram.MosaicRegion;
import arrange.model.Network;

/**
 *
 * @author Max Sondag
 */
public final class TakeMove extends Move {

    public final Coordinate position;
    public final Network.Vertex newVertex;
    public final Network.Vertex oldVertex;
    private boolean improves = false;
    public boolean createsAlley = false;
    public boolean createsHole = false;
    private Network weakDual;

    public TakeMove(Network weakDual, MosaicCartogram currentGrid, Coordinate position, Network.Vertex vertex) {
        this.weakDual = weakDual;
        this.currentGrid = currentGrid;
        this.position = position;
        this.newVertex = vertex;
        this.oldVertex = currentGrid.getVertex(position);
        MosaicRegion newRegion = currentGrid.getRegion(newVertex.getId());
        if (!newRegion.isDesired(position)) {
            improves = false;
        } else {
            if (oldVertex == null) {
                //not occupied by another region
                improves = true;
            } else {
                MosaicRegion oldRegion = currentGrid.getRegion(oldVertex.getId());
                if (!oldRegion.isDesired(position)) {
                    //other region does not want this position
                    improves = true;
                } else {
                    double newSD = newRegion.getSymmetricDifference();
                    double oldSD = oldRegion.getSymmetricDifference();
                    double newSize = newRegion.getGuidingShape().size();
                    double oldSize = oldRegion.getGuidingShape().size();
                    double currentError = Math.max(newSD / newSize, oldSD / oldSize);
                    double newError = Math.max((newSD - 1) / newSize, (oldSD + 1) / oldSize);
                    if (newError < currentError) {
                        improves = true;
                        //net improve when taking this region
                    } else {
                        improves = false;
                    }
                }
            }
        }
    }

    @Override
    public double evaluate() {
        ArrayList<Boolean> alleys = new ArrayList<>();
        for (Coordinate c : position.neighbours()) {
            if (isAlley(c)) {
                alleys.add(true);
            } else {
                alleys.add(false);
            }
        }
        currentGrid.setVertex(position, newVertex);
        for (int i = 0; i < position.neighbours().length; i++) {
            Coordinate c = position.neighbours()[i];
            if (isAlley(c) && !alleys.get(i)) {
                createsAlley = true;
            }
        }
        if (takeIsValid()) {
            valid = true;
            quality = evaluateGridQuality();
            necessity = Math.max(currentGrid.getRegion(newVertex.getId()).getSymmetricDifference(), 1);
        } else {
            valid = false;
            quality = Integer.MAX_VALUE;
        }
        if (oldVertex != null) {
            currentGrid.setVertex(position, oldVertex);
        } else {
            currentGrid.removeCell(position);
        }
        return quality;
    }

    public double evaluateWithHole(ArrayList<Set<Coordinate>> oldHoles) {
        ArrayList<Boolean> alleys = new ArrayList<>();
        for (Coordinate c : position.neighbours()) {
            if (isAlley(c)) {
                alleys.add(true);
            } else {
                alleys.add(false);
            }
        }
        currentGrid.setVertex(position, newVertex);
        ArrayList<Set<Coordinate>> holes = MosaicCartogram.computeHoleBoundaries(currentGrid, currentGrid.getCoordinateSet());
        if (!holes.isEmpty()) {
            int oldTotal = 0;
            int newTotal = 0;
            for (Set<Coordinate> hole : oldHoles) {
                oldTotal += hole.size();
            }
            for (Set<Coordinate> hole : holes) {
                newTotal += hole.size();
            }
            if (newTotal > oldTotal) {
                createsHole = true;
            }
        }
        for (int i = 0; i < position.neighbours().length; i++) {
            Coordinate c = position.neighbours()[i];
            if (isAlley(c) && !alleys.get(i)) {
                createsAlley = true;
            }
        }
        if (takeIsValid()) {
            valid = true;
            quality = evaluateGridQuality();
            necessity = Math.max(currentGrid.getRegion(newVertex.getId()).getSymmetricDifference(), 1);
        } else {
            valid = false;
            quality = Integer.MAX_VALUE;
        }

        //For polishing, we do not always care about adjacency preservation
        connected = takeIsConnected();

        if (oldVertex != null) {
            currentGrid.setVertex(position, oldVertex);
        } else {
            currentGrid.removeCell(position);
        }
        return quality;
    }

    @Override
    public double execute() {
        currentGrid.setVertex(position, newVertex);
        return quality;
    }

    public boolean improves() {
        return improves;
    }

    public boolean isConnected() {
        return connected;
    }

    private boolean takeIsConnected() {
        if (oldVertex == null) {
            //old vertex was an unoccupied vertex. Needs to be a neighbor for the result to be connected
            for (Coordinate d : position.neighbours()) {
                Network.Vertex vd = currentGrid.getVertex(d);
                if (vd == newVertex) {
                    return true;//connected
                }
            }
            return false;//not connected
        } else {
            //If both the old region and the new region are still valid, then 
            //all regions are still valid
            MosaicRegion oldRegion = currentGrid.getRegion(oldVertex.getId());
            MosaicRegion newRegion = currentGrid.getRegion(newVertex.getId());
            return (gridIsConnected(oldRegion) && gridIsConnected(newRegion));
        }
    }

    private boolean takeIsValid() {
        if (oldVertex == null) {
            //old vertex was an unoccupied vertex. Needs to be a neighbor for the result to be connected
            boolean connected = false;
            for (Coordinate d : position.neighbours()) {
                Network.Vertex vd = currentGrid.getVertex(d);
                if (vd != null && vd != newVertex) {
                    if (!weakDual.hasEdge(newVertex, vd)) {
                        return false;
                    }
                } else if (vd == newVertex) {
                    connected = true;
                }
            }
            return connected;
        } else {
            //If both the old region and the new region are still valid, then 
            //all regions are still valid
            MosaicRegion oldRegion = currentGrid.getRegion(oldVertex.getId());
            MosaicRegion newRegion = currentGrid.getRegion(newVertex.getId());
            return (gridIsValid(oldRegion) && gridIsValid(newRegion));
        }
    }
}
