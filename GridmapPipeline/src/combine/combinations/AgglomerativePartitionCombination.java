package combine.combinations;

import combine.Combination;
import common.Partition;
import common.Site;
import common.SiteMap;
import common.Stage;
import common.dual.Dual;
import common.gridmath.AdjacencyType;
import common.gridmath.GridMath;
import common.gridmath.GridMath.Coordinate;
import common.gridmath.util.CoordinateSet;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Polygon;

/**
 *
 * @author Wouter Meulemans
 */
public class AgglomerativePartitionCombination extends Combination {

    private GridMath gridmath;

    public AgglomerativePartitionCombination() {
        super("AgglomerativePartitionAssigned");
    }

    @Override
    public boolean requiresAssigned() {
        return true;
    }

    private class Component {

        List<Dual> duals;
        CoordinateSet cells;
    }

    @Override
    public void run(SiteMap map) {

        gridmath = map.gridmath;

        if (map.duals == null) {
            map.determineDuals();
        }

        List<Component> components = new ArrayList();
        for (Dual d : map.duals) {
            Component comp = new Component();
            components.add(comp);
            comp.duals = new ArrayList();
            comp.duals.add(d);

            comp.cells = new CoordinateSet(gridmath);
            for (Partition p : d.primalPartitions()) {
                comp.cells.addAll(p.cells);
            }
        }

        while (components.size() > 1) {

            int n = components.size();

            double outline_dist = Double.POSITIVE_INFINITY;
            Component[] stat_move_comp = new Component[2];
            Partition[] stat_move_part = new Partition[2];

            for (int i = 0; i < n; i++) {
                Component a = components.get(i);
                for (int j = i + 1; j < n; j++) {
                    Component b = components.get(j);
                    outline_dist = partitionDistance(a, b, stat_move_comp, stat_move_part, outline_dist);
                }
            }

            Component stationary = stat_move_comp[0];
            Component moving = stat_move_comp[1];
            Coordinate[] stat_move = new Coordinate[2];
            Vector vec = getClosestCoordinateAndAngle(stat_move_part[0], stat_move_part[1], stat_move);

            Coordinate offset = getBestOffset(stationary, moving, stat_move, vec);

            stationary.duals.addAll(moving.duals);
            stationary.cells.addAll(moving.cells); // NB: already translated during offset finding

            // shift the cartogram/assignment (if present)
            for (Dual out : moving.duals) {
                for (Partition p : out.primalPartitions()) {
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

            components.remove(moving);
        }

        gridmath = null;
    }

    private boolean hasConflict(CoordinateSet stationary, Coordinate offset, CoordinateSet moving) {
        moving.translate(offset);
        if (moving.touches(stationary, AdjacencyType.ROOKS)) {
            moving.translate(offset.inverse());
            return true;
        }
        return false;
    }

    private double partitionDistance(Component a, Component b, Component[] a_b, Partition[] part_a_b, double tobeat) {
        double dist = tobeat;
        for (Dual a_dual : a.duals) {
            for (Partition a_part : a_dual.primalPartitions()) {

                for (Dual b_dual : b.duals) {
                    for (Partition b_part : b_dual.primalPartitions()) {

                        double d = distance(a_part, b_part);
                        if (d < dist) {
                            dist = d;
                            a_b[0] = a;
                            a_b[1] = b;
                            part_a_b[0] = a_part;
                            part_a_b[1] = b_part;
                        }
                    }
                }
            }
        }
        return dist;
    }

    private double distance(Polygon a, Polygon b) {
        double dist = Double.POSITIVE_INFINITY;
        for (Vector v : a.vertices()) {
            double d = b.distanceTo(v);
            if (d < dist) {
                dist = d;
            }
        }
        for (Vector v : b.vertices()) {
            double d = a.distanceTo(v);
            if (d < dist) {
                dist = d;
            }
        }
        return dist;
    }

    private Vector getClosestCoordinateAndAngle(Partition a_part, Partition b_part, Coordinate[] a_b) {

        Vector bestAngle = null;
        double bestLength = Double.MAX_VALUE;

        for (Site a_site : a_part.sites) {

            for (Site b_site : b_part.sites) {

                double squaredLength = a_site.squaredDistanceTo(b_site);
                if (squaredLength < bestLength) {
                    bestAngle = Vector.subtract(b_site, a_site);
                    bestLength = squaredLength;

                    a_b[0] = a_site.getCell();
                    a_b[1] = b_site.getCell();
                }
            }
        }

        return bestAngle;
    }

    private Coordinate getBestOffset(Component stationary, Component moving, Coordinate[] stat_move, Vector direction) {

        Coordinate base = stat_move[0].minus(stat_move[1]).localRoot();
        //stay in the halfplane of the angle while moving.

        //holds which coordinates have already been tried
        CoordinateSet found = new CoordinateSet(gridmath);
        PriorityQueue<Coordinate> toTry = new PriorityQueue<>((c1, c2) -> {
            // closest to origin?
            int c = Integer.compare(c1.norm(), c2.norm());
            if (c != 0) {
                return c;
            }
            // or best angle
            return Double.compare(Vector.dotProduct(direction, c2.toVector()), Vector.dotProduct(direction, c1.toVector()));
        });
        found.add(gridmath.origin());
        toTry.add(gridmath.origin());

        while (true) {
            Coordinate relative = toTry.poll();
            Coordinate absolute = base.plus(relative);
            if (!hasConflict(stationary.cells, absolute, moving.cells)) {
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
