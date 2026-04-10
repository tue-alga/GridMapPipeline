package arrange.algorithms.sub;

import common.gridmath.AdjacencyType;
import common.gridmath.GridMath;
import common.gridmath.GridMath.Coordinate;
import common.gridmath.util.CoordinateMap;
import common.gridmath.util.CoordinateSet;
import java.util.ArrayList;
import java.util.List;
import nl.tue.geometrycore.geometry.BaseGeometry;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Line;
import nl.tue.geometrycore.geometry.linear.LineSegment;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.util.DoubleUtil;

/**
 *
 * @author Wouter Meulemans
 */
public class QualityMapConstruction {

    private final GridMath gridmath;
    private final Polygon shape;

    public QualityMapConstruction(GridMath gridmath, Polygon shape) {
        this.gridmath = gridmath;
        this.shape = shape;
    }

    private class Intersection {

        final Vector loc;
        final int coord_edge;
        final double coord_sqrDist;
        final int shape_edge;
        Interval interval;

        Intersection(Vector loc, int shape_edge, Polygon coord_shape) {
            this.loc = loc;
            this.shape_edge = shape.index(shape_edge);
            int ce = -1;
            double cd = Double.NaN;
            for (int i = 0; i < coord_shape.edgeCount(); i++) {
                if (coord_shape.edge(i).onBoundary(loc)) {
                    ce = i;
                    cd = loc.squaredDistanceTo(coord_shape.vertex(i));
                    break;
                }
            }
            this.coord_edge = ce;
            this.coord_sqrDist = cd;
        }

        boolean isFrom() {
            return this == interval.from;
        }

        boolean isTo() {
            return this == interval.to;
        }

        Intersection other() {
            return this == interval.from ? interval.to : interval.from;
        }
    }

    private class Interval {

        final Intersection from;
        final Intersection to;
        Interval parent = null;
        List<Interval> children = new ArrayList();

        public Interval(Intersection from, Intersection to) {
            this.from = from;
            this.to = to;
        }

        boolean forward() {
            return from.coord_edge < to.coord_edge || (from.coord_edge == to.coord_edge && from.coord_sqrDist < to.coord_sqrDist);
        }

    }

    private class CoordData {

        final Coordinate coord;
        final Polygon coord_shape;
        final List<Intersection> endpoints = new ArrayList();
        final List<Interval> intervals = new ArrayList();

        public CoordData(Coordinate coord, Polygon coord_shape) {
            this.coord = coord;
            this.coord_shape = coord_shape;
        }

    }

    private class Walking {

        Vector pos;
        int shape_edge;
    }

    private boolean accept(Coordinate c, Vector loc, Vector towards) {
        Polygon p = c.getBoundary();

        // check angles at vertex
        for (int i = 0; i < p.vertexCount(); i++) {
            Vector v = p.vertex(i);
            if (v.isApproximately(loc)) {
                Vector u = p.vertex(i - 1);
                Vector w = p.vertex(i + 1);

                Vector vu = Vector.subtract(u, v);
                Vector vw = Vector.subtract(w, v);
                Vector vloc = Vector.subtract(loc, v);

                return vu.computeClockwiseAngleTo(vw) >= vu.computeClockwiseAngleTo(vloc);
            }
        }

        // not at a vertex:
        // check locally towards the inside (assuming CCW polygon)
        for (LineSegment e : p.edges()) {
            if (e.onBoundary(loc)) {
                return Line.spannedBy(e).isLeftOf(towards);
            }
        }

        // not on boundary at all
        return false;
    }

    private CoordData getCoordinate(Vector loc, Vector towards, CoordinateMap<CoordData> map) {
        // TODO: make this more logical / leverage grid specifics

        Coordinate c = gridmath.getContainingCell(loc);

        // make sure its not strictly inside (only useful for initial call really)
        if (!c.getBoundary().contains(loc, -DoubleUtil.EPS) && !accept(c, loc, towards)) {
            // it lies on the boundary and goes outwards, find the neighbor that has it inside
            for (Coordinate nbr : c.adjacent(AdjacencyType.QUEENS)) {
                boolean acc = accept(nbr, loc, towards);
                if (acc) {
                    c = nbr;
                    break;
                }
            }
        }

        CoordData cd = map.get(c);
        if (cd == null) {
            cd = new CoordData(c, c.getBoundary());
            map.put(c, cd);
        }
        return cd;
    }

    private boolean findFirst(Walking walk, CoordData coord) {
        if (coord.coord_shape.onBoundary(walk.pos)) {
            return true;
        }

        final int first_edge_again = -1 * shape.edgeCount();
        while (walk.shape_edge >= first_edge_again) {

            final Vector target = shape.vertex(walk.shape_edge); // purposefully before the decrement, s.t. it is the target of the edge after decrement

            walk.shape_edge--;
            List<BaseGeometry> is = coord.coord_shape.intersect(shape.edge(walk.shape_edge));
            Vector closest = null;
            double sqrDist = Double.POSITIVE_INFINITY; // distance to target of edge
            for (BaseGeometry bg : is) {
                switch (bg.getGeometryType()) {
                    case VECTOR: {
                        Vector v = (Vector) bg;
                        double sd = v.squaredDistanceTo(target);
                        if (sd < sqrDist) {
                            closest = v;
                            sqrDist = sd;
                        }
                        break;
                    }
                    case LINESEGMENT: {
                        LineSegment ls = (LineSegment) bg;
                        Vector v = ls.getStart();
                        double sd = v.squaredDistanceTo(target);
                        if (sd < sqrDist) {
                            closest = v;
                            sqrDist = sd;
                        }
                        v = ls.getEnd();
                        sd = v.squaredDistanceTo(target);
                        if (sd < sqrDist) {
                            closest = v;
                            sqrDist = sd;
                        }
                        break;
                    }
                }
            }
            if (closest != null) {
                walk.pos = closest;
                return true;
            }
        }

        // no intersection found at all
        return false;
    }

    private void findNext(Walking walk, CoordData coord) {
        while (true) {
            Vector tar = shape.vertex(walk.shape_edge + 1);
            LineSegment ray = new LineSegment(walk.pos, tar);
            List<BaseGeometry> is = coord.coord_shape.intersect(ray);

            Vector vec = null;
            for (BaseGeometry bg : is) {
                switch (bg.getGeometryType()) {
                    case VECTOR: {
                        Vector v = (Vector) bg;
                        if (!v.isApproximately(walk.pos)
                                && (vec == null || vec.squaredDistanceTo(walk.pos) > v.squaredDistanceTo(walk.pos))) {
                            vec = v;
                        }
                        break;
                    }
                    case LINESEGMENT: {
                        LineSegment ls = (LineSegment) bg;
                        Vector v = ls.getStart();
                        if (!v.isApproximately(walk.pos)
                                && (vec == null || vec.squaredDistanceTo(walk.pos) > v.squaredDistanceTo(walk.pos))) {
                            vec = v;
                        }
                        v = ls.getEnd();
                        if (!v.isApproximately(walk.pos)
                                && (vec == null || vec.squaredDistanceTo(walk.pos) > v.squaredDistanceTo(walk.pos))) {
                            vec = v;
                        }
                        break;
                    }
                }
            }

            if (vec == null) {
                walk.pos = tar;
                walk.shape_edge++;
            } else {
                walk.pos = vec;
                if (vec.isApproximately(tar)) {
                    walk.shape_edge++;
                }
                return;
            }
        }
    }

    private CoordinateMap<CoordData> constructIntersections() {

        CoordinateMap<CoordData> map = new CoordinateMap<>();
        CoordData coord = getCoordinate(shape.vertex(0), shape.vertex(1), map);

        Walking walk = new Walking();
        walk.pos = shape.vertex(0);
        walk.shape_edge = 0;

        // find a first boundary intersection
        if (!findFirst(walk, coord)) {
            // fully contained, signalling with null
            map.put(coord.coord, null);
            return map;
        }

        Intersection start = new Intersection(walk.pos, walk.shape_edge, coord.coord_shape);
        coord.endpoints.add(start);
        // free walk to the start again
        Vector first_pos = walk.pos;
        if (walk.shape_edge < 0) {
            walk.pos = shape.vertex(0);
            walk.shape_edge = 0;
        }

        while (true) {

            findNext(walk, coord);

            Intersection end = new Intersection(walk.pos, walk.shape_edge, coord.coord_shape);
            coord.endpoints.add(end);

            Interval iv = new Interval(start, end);
            coord.intervals.add(iv);
            start.interval = iv;
            end.interval = iv;

            if (walk.pos.isApproximately(first_pos)) {
                break;
            } else if (walk.shape_edge > 2 * shape.edgeCount()) {
                throw new RuntimeException("Construction is running around in circles: probably the grid geometry is not precise enough?");
            }

            coord = getCoordinate(walk.pos, shape.vertex(walk.shape_edge + 1), map);

            start = new Intersection(walk.pos, walk.shape_edge, coord.coord_shape);
            coord.endpoints.add(start);
        }

        return map;
    }

    private double qualityOf(CoordData cdata) {

        if (cdata == null) {
            // special case: shape fully in a cell
            return shape.areaUnsigned();
        }

        boolean[] degenerate = {false};
        cdata.endpoints.sort((a, b) -> {
            // earlier edge?
            int c = Integer.compare(a.coord_edge, b.coord_edge);
            if (c != 0) {
                return c;
            }

            // earlier along common edge?
            c = Double.compare(a.coord_sqrDist, b.coord_sqrDist);
            if (c != 0) {
                return c;
            }

            // compare with other endpoints, in reverse!
            Intersection other_a = a.other();
            Intersection other_b = b.other();

            c = Integer.compare(other_b.coord_edge, other_a.coord_edge);
            if (c != 0) {
                return c;
            }

            c = Double.compare(other_b.coord_sqrDist, other_a.coord_sqrDist);
            if (c != 0) {
                return c;
            }

            // degenerate: both start and end are equal, this should only happen if the shape is fully within the cell, and touches it at one or two endpoints...
            degenerate[0] = true;
            return 0;
        });

        if (degenerate[0]) {
            return shape.areaUnsigned();
        }

        // determine nesting of polylines in coordinate
        Interval parent = null;
        List<Interval> root_children = new ArrayList();
        for (Intersection is : cdata.endpoints) {
            Interval iv = is.interval;

            if (is.isFrom() == iv.forward()) {
                // start the interval
                iv.parent = parent;
                if (parent != null) {
                    parent.children.add(iv);
                } else if (iv.forward()) {
                    root_children.add(iv);
                }
                parent = iv;
            } else {
                // end of the interval
                parent = iv.parent;
            }
        }

        // compute area of each enclosed (== backwards edge)
        double area = determineArea(cdata, null, root_children);
        for (Intersection is : cdata.endpoints) {
            Interval iv = is.interval;
            if (is.isFrom() && !iv.forward()) { // just do this once per interval...
                area += determineArea(cdata, iv, iv.children);
            }
        }

        return area;
    }

    private double determineArea(CoordData cdata, Interval bwd, List<Interval> fwds) {
        Polygon p = new Polygon();

        if (fwds.isEmpty() && bwd == null) {
            // special case: we did this for the root, but it has no children, so its outside...
            return 0;
        }

        // start at end of backinterval
        int walk;
        if (bwd == null) {
            p.addVertex(cdata.coord_shape.vertex(0));
            walk = 0;
        } else {
            p.addVertex(bwd.to.loc);
            walk = bwd.to.coord_edge;
        }

        int fwd = 0;
        while (fwd < fwds.size()) {
            Interval iv = fwds.get(fwd);

            // walk to start of fwd interval, along coordinate boundary
            int target = iv.from.coord_edge;
            while (walk < target) {
                walk++;
                p.addVertex(cdata.coord_shape.vertex(walk));
            }

            // walk along fwd interval, along shape
            p.addVertex(iv.from.loc);
            walk = iv.from.shape_edge;
            target = iv.to.shape_edge;
            while (walk < target) {
                walk++;
                p.addVertex(shape.vertex(walk));
            }
            walk = iv.to.coord_edge;
            p.addVertex(iv.to.loc);

            fwd++;
        }

        // walk to start of bwd interval, along coordinate boundary
        int target = bwd == null ? cdata.coord_shape.edgeCount() : bwd.from.coord_edge;
        while (walk < target) {
            walk++;
            p.addVertex(cdata.coord_shape.vertex(walk));
        }

        if (bwd != null) {
            // walk along bwd interval, along shape
            p.addVertex(bwd.from.loc);
            walk = bwd.from.shape_edge;
            target = bwd.to.shape_edge;
            if (target < walk) {
                target += shape.edgeCount();
            }
            while (walk < target) {
                walk++;
                p.addVertex(shape.vertex(walk));
            }
        }

        return p.areaUnsigned();
    }

    private CoordinateMap<Double> convertToQualityMap(CoordinateMap<CoordData> map) {

        CoordinateMap<Double> qmap = new CoordinateMap<>();
        CoordinateSet stabbed = new CoordinateSet(gridmath);

        // first, all cells stabbed by the boundary
        for (CoordinateMap<CoordData>.Entry e : map.entrySet()) {
            qmap.put(e.getCoordinate(), qualityOf(e.getValue()));
            stabbed.add(e.getCoordinate());
        }

        // post process: fill the holes that are contained in the shape
        List<CoordinateSet> holes = stabbed.detectHoles(AdjacencyType.ROOKS); // NB: this should be rooks iso MosaicConstants

        for (CoordinateSet hole : holes) {
            // either all in or all out
            if (shape.contains(hole.iterator().next().toVector())) {
                // all in!
                for (Coordinate c : hole) {
                    qmap.put(c, c.getBoundary().areaUnsigned());
                }
            }
        }

        return qmap;
    }

    public CoordinateMap<Double> construct() {

        CoordinateMap<CoordData> map = constructIntersections();
        CoordinateMap<Double> qmap = convertToQualityMap(map);
        return qmap;

    }
}
