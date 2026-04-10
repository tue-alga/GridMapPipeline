package combine.combinations;

import common.gridmath.GridMath.Coordinate;
import combine.Combination;
import common.Partition;
import common.Site;
import common.SiteMap;
import common.Stage;
import common.dual.Dual;
import common.gridmath.AdjacencyType;
import common.gridmath.util.CoordinateSet;
import common.gridmath.GridMath;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import nl.tue.geometrycore.geometry.Vector;

/**
 *
 * @author Wouter Meulemans
 */
public abstract class OffsetSearcher extends Combination {

    protected GridMath gridmath;

    public OffsetSearcher(String name) {
        super(name);
    }

    @Override
    public void run(SiteMap map) {

        gridmath = map.gridmath;

        if (map.duals == null) {
            map.determineDuals();
        }

        List<Dual> sortedComponents = new ArrayList(map.duals);
        sortedComponents.sort((c1, c2) -> Integer.compare(c2.getTotalWeight(), c1.getTotalWeight()));

        List<Dual> placedComponents = new ArrayList();
        CoordinateSet existingCells = null;

        for (Dual component : sortedComponents) {

            CoordinateSet newCells = new CoordinateSet(gridmath);
            for (Partition p : component.primalPartitions()) {
                newCells.addAll(p.cells);
            }

            if (existingCells == null) {
                existingCells = newCells;
                placedComponents.add(component);
            } else {

                //Determine where to place this component such that it doesn't overlap and is roughly in the right place.
                Coordinate offset = getBestOffset(component, placedComponents, existingCells, newCells);

                // NB: getBestOffset returns with the newCells translated accordingly
                existingCells.addAll(newCells);

                placedComponents.add(component);
                for (Partition p : component.primalPartitions()) {
                    p.cells.translate(offset);
                    if (p.guide != null) {
                        p.guide.translate(offset);
                    }

                    if (map.stage >= Stage.ASSIGNED) {
                        for (Site s : p.sites) {
                            s.setCell(s.getCell().plus(offset));
                        }
                    }
                }

            }
        }

        gridmath = null;
    }

    private boolean hasConflict(CoordinateSet usedCells, Coordinate offset, CoordinateSet newCells) {
        newCells.translate(offset);
        if (newCells.touches(usedCells, AdjacencyType.ROOKS)) {
            newCells.translate(offset.inverse());
            return true;
        }
        return false;
    }

    /**
     * Find the coordinate of the cell c1 in any component of placedComponents
     * that was closest to any cell c2 of this component and the angle between
     * c1 and b2. This gives an offset an a target angle.
     *
     * @param placedComponents
     * @param curComponent
     * @param placedCurClosest
     * @return angle
     */
    protected abstract Vector getClosestCoordinateAndAngle(List<Dual> placedComponents, Dual curComponent, Coordinate[] placedCurClosest);

    private Coordinate getBestOffset(Dual current, List<Dual> placedComponents, CoordinateSet existingCells, CoordinateSet newCells) {

        //Find the coordinate of the cell c1 in any component of placedComponents that was closest to any cell c2 of this component 
        //and the angle between c1 and b2. This gives an offset and a target angle
        Coordinate[] placedCurClosest = new Coordinate[2];
        Vector direction = getClosestCoordinateAndAngle(placedComponents, current, placedCurClosest);
        if (direction == null) {
            return gridmath.origin();
        }

        Coordinate base = placedCurClosest[0].minus(placedCurClosest[1]).localRoot();
        //stay in the halfplane of the angle while moving.

        //holds which coordinates have already been tried
        CoordinateSet found = new CoordinateSet(gridmath);
        PriorityQueue<Coordinate> toTry = new PriorityQueue<>((a, b) -> {
            // closest to origin?
            int c = Integer.compare(a.norm(), b.norm());
            if (c != 0) {
                return c;
            }
            // or best angle
            return Double.compare(Vector.dotProduct(direction, b.toVector()), Vector.dotProduct(direction, a.toVector()));
        });
        found.add(gridmath.origin());
        toTry.add(gridmath.origin());

        while (true) {
            Coordinate relative = toTry.poll();
            Coordinate absolute = base.plus(relative);
            if (!hasConflict(existingCells, absolute, newCells)) {
                return absolute;
            }

            for (Coordinate off : gridmath.unitVectors()) {
                Coordinate nbr = relative.plus(off);
                if (Vector.dotProduct(direction, nbr.toVector()) > 0 && !found.contains(nbr)) {
                    found.add(nbr);
                    toTry.add(nbr);
                }
            }
        }
    }
}
