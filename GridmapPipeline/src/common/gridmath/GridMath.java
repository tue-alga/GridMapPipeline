package common.gridmath;

import common.gridmath.util.CoordinateSet;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.tue.geometrycore.geometry.BaseGeometry;
import nl.tue.geometrycore.geometry.GeometryType;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.LineSegment;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.geometryrendering.styling.Dashing;
import nl.tue.geometrycore.geometryrendering.styling.TextAnchor;
import nl.tue.geometrycore.io.ipe.IPEWriter;
import nl.tue.geometrycore.util.DoubleUtil;

/**
 * This class in principle can derive the necessary topology and information,
 * purely from the set of polygons and the up/right translations. It does assume
 * that the given pattern of polygons appropriately tiles the Euclidean plane
 * via integer multiples of the given translations. The pattern as given should
 * be centered around the origin.
 * 
 * @author Wouter Meulemans
 */
public class GridMath {

    private final double DERIVE_EPS = 0.001;
    private final GridGeometry geom;
    private final int pc;
    private final Polygon[] boundaries;
    private final Vector[] centroids;
    private final Coordinate origin = new Coordinate(0, 0);
    private final Vector up, right;
    private final Coordinate[] unitVectors;
    private final Coordinate[][] rooks_neighbors;
    private final Coordinate[][] bishops_neighbors;
    private final Coordinate[][] queens_neighbors;
    private final boolean[][] rooks_among_queens;
    private final double min_nonneighbor_distance;

    public GridMath(GridGeometry geom) {
        this.geom = geom;
        pc = geom.getPatternComplexity();
        boundaries = geom.getTransformedBoundaries();

        centroids = new Vector[pc];
        for (int var = 0; var < pc; var++) {
            centroids[var] = boundaries[var].centroid();
        }

        up = geom.transformedUp();
        right = geom.transformedRight();

        Coordinate[][][] nbrhoods = geom.getNeighborhoods(origin);

        if (nbrhoods == null) {
            rooks_neighbors = new Coordinate[pc][];
            bishops_neighbors = new Coordinate[pc][];
            queens_neighbors = new Coordinate[pc][];
            for (int var = 0; var < pc; var++) {
                initNeighborhood(var);
            }
        } else {
            rooks_neighbors = nbrhoods[0];
            bishops_neighbors = nbrhoods[1];
            queens_neighbors = nbrhoods[2];
        }
        rooks_among_queens = new boolean[pc][];
        for (int var = 0; var < pc; var++) {
            boolean[] raq = rooks_among_queens[var] = new boolean[queens_neighbors[var].length];
            for (int i = 0; i < raq.length; i++) {
                raq[i] = false;
                for (Coordinate c : rooks_neighbors[var]) {
                    if (c.equals(queens_neighbors[var][i])) {
                        raq[i] = true;
                        break;
                    }
                }
            }
        }

        Coordinate[] uvs = geom.getUnitVectors(origin);
        if (uvs == null) {
            unitVectors = initUnitVectors();
        } else {
            unitVectors = uvs;
        }

        min_nonneighbor_distance = initMinNNDistance();
    }

    public void printResult() {

        System.out.println("");
        System.out.println("" + geom.toString());

        System.out.println("Variants: " + pc);
        for (int var = 0; var < pc; var++) {
            System.out.println("  Variant " + var);
            System.out.println("     Nr rooks: " + rooks_neighbors[var].length);
            System.out.println("     Nr bshps: " + bishops_neighbors[var].length);
            System.out.println("     Nr queen: " + queens_neighbors[var].length);
        }

        System.out.println("Unit vectors:");
        for (Coordinate c : unitVectors) {
            System.out.println("  " + c);
        }

        System.out.println("NN Distance:");
        System.out.println("  " + min_nonneighbor_distance);

        try (IPEWriter write = IPEWriter.fileWriter(new File("gridmath_" + geom.toString() + ".ipe"))) {
            write.configureTextHandling(true, 10, true);
            write.setTextSerifs(true);
            write.initialize();

            write.setTextStyle(TextAnchor.CENTER, 1);
            for (int var = 0; var < pc; var++) {
                write.newPage("c", "r", "b", "u");

                write.setLayer("c");
                write.setStroke(Color.black, 0.04, Dashing.SOLID);
                Coordinate ctr = new Coordinate(var, 0);
                write.draw(ctr.getBoundary());
                write.draw(ctr.toVector(), ctr.toString());

                write.setLayer("r");
                write.setStroke(Color.blue, 0.04, Dashing.SOLID);
                for (Coordinate c : rooks_neighbors[var]) {
                    write.draw(c.getBoundary());
                    write.draw(c.toVector(), c.toString());
                }

                write.setLayer("b");
                write.setStroke(Color.yellow, 0.04, Dashing.SOLID);
                for (Coordinate c : bishops_neighbors[var]) {
                    write.draw(c.getBoundary());
                    write.draw(c.toVector(), c.toString());
                }

                if (var == 0) {
                    write.setLayer("u");
                    write.setStroke(Color.gray, 0.04, Dashing.SOLID);
                    for (Coordinate c : unitVectors) {
                        write.draw(c.getBoundary());
                    }
                    write.draw(
                            LineSegment.byStartAndOffset(Vector.origin(), right),
                            LineSegment.byStartAndOffset(Vector.origin(), up)
                    );
                }
            }

        } catch (IOException ex) {
            Logger.getLogger(GridMath.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public GridGeometry geometry() {
        return geom;
    }

    private double initMinNNDistance() {
        double min = Double.POSITIVE_INFINITY;
        for (int var = 0; var < pc; var++) {
            double nnd = nonneighborDistance(var);
            if (nnd < min) {
                min = nnd;
            }
        }
        return min;
    }

    private double nonneighborDistance(int var) {
        CoordinateSet set = new CoordinateSet(this, AdjacencyType.QUEENS);
        Coordinate c = new Coordinate(var, 0);
        Polygon p = c.getBoundary();
        set.add(c);
        for (Coordinate nbr : c.adjacent(AdjacencyType.QUEENS)) {
            set.add(nbr);
        }
        double nnd = Double.POSITIVE_INFINITY;
        for (Coordinate nnbr : set.neighbors(AdjacencyType.QUEENS)) {
            // NB: the minimal distance must occur between a vertex of one polygon and a vertex or edge of the other
            Polygon nnp = nnbr.getBoundary();
            for (Vector v : p.vertices()) {
                double d = nnp.distanceTo(v);
                if (d < nnd) {
                    nnd = d;
                }
            }
            for (Vector v : nnp.vertices()) {
                double d = p.distanceTo(v);
                if (d < nnd) {
                    nnd = d;
                }
            }
        }
        return nnd;
    }

    private Coordinate[] initUnitVectors() {
        CoordinateSet units = new CoordinateSet(this, null);
        for (int var = 0; var < pc; var++) {
            for (Coordinate nbr : queens_neighbors[var]) {
                Coordinate lr = nbr.localRoot();
                if (!lr.equals(origin)) {
                    units.add(lr);
                }
            }
        }

        Coordinate[] uvs = new Coordinate[units.size()];
        int i = 0;
        for (Coordinate uv : units) {
            uvs[i++] = uv;
        }
        return uvs;
    }

    public Coordinate closestEquivalance(Vector vector) {
        double[] xy = Vector.solveVectorAddition(right, up, vector);

        int px = (int) Math.round(xy[0]);
        int py = (int) Math.round(xy[1]);

        return new Coordinate(px * pc, py);
    }

    private class NbrInfo {

        Coordinate coord;
        int from;
        double edgedist;
        Vector nbr_vec;
        boolean rooks;
    }

    private boolean properOverlap(LineSegment a, LineSegment b) {
        List<BaseGeometry> is = a.intersect(b, DERIVE_EPS);
        if (is.isEmpty()) {
            return false;
        } else {
            return is.get(0).getGeometryType() != GeometryType.VECTOR;
        }
    }

    private NbrInfo determine(Polygon p, Polygon tp) {

        // we assume a single contiguous intersection, if anything.
        // we can have three types of incidences
        //  -- vertex-vertex
        //  -- vertex-edge
        //  -- edge-vertex
        for (int i = 0; i < p.vertexCount(); i++) {
            Vector v = p.vertex(i);

            // see if v lies on the boundary of tp
            for (int j = 0; j < tp.vertexCount(); j++) {
                Vector tv = tp.vertex(j);
                if (v.isApproximately(tv, DERIVE_EPS)) {
                    // vertex-vertex incidence
                    NbrInfo nbr = new NbrInfo();
                    if (properOverlap(p.edge(i - 1), tp.edge(j))) {
                        // incoming edge overlap
                        // happens only when i = 0...
                        nbr.from = p.index(i - 1);
                        nbr.edgedist = 0;
                        nbr.rooks = true;
                    } else if (properOverlap(p.edge(i), tp.edge(j - 1))) {
                        // outgoing edge overlap
                        nbr.from = i;
                        nbr.edgedist = 0;
                        nbr.rooks = true;
                    } else {
                        // only point incidence
                        nbr.from = i;
                        nbr.edgedist = 0;
                        nbr.rooks = false;
                        Vector backwards = Vector.subtract(tp.vertex(j - 1), tv);
                        Vector forwards = Vector.subtract(tp.vertex(j + 1), tv);
                        nbr.nbr_vec = Vector.add(backwards, forwards);
                        if (Vector.crossProduct(forwards, backwards) < 0) {
                            // reflex vertex
                            nbr.nbr_vec.invert();
                        }
                    }
                    return nbr;
                }
            }
        }

        // no vertex-vertex incidences exist
        // we go over edge-vertex pairs, but must be careful not to prematurely decide on a bishops-adjacency
        // so, we keep searching if we found a witness of bishops adjacency, but can readily stop if we found a rooks.
        NbrInfo nbr = null;
        // test vertex-edge
        for (int i = 0; i < p.vertexCount(); i++) {
            Vector v = p.vertex(i);
            for (int j = 0; j < tp.edgeCount(); j++) {
                LineSegment te = tp.edge(j);
                if (te.onBoundary(v, DERIVE_EPS)) {
                    if (Vector.collinear(v, p.vertex(i - 1), te.getStart(), DERIVE_EPS)) {
                        // incoming edge overlap
                        // happens only when i = 0...
                        nbr = new NbrInfo();
                        nbr.from = p.index(i - 1);
                        nbr.edgedist = 0;
                        nbr.rooks = true;
                        return nbr;
                    } else if (Vector.collinear(v, p.vertex(i + 1), te.getStart(), DERIVE_EPS)) {
                        // outgoing edge overlap
                        nbr = new NbrInfo();
                        nbr.from = i;
                        nbr.edgedist = 0;
                        nbr.rooks = true;
                        return nbr;
                    } else if (nbr == null) {
                        // only point incidence
                        nbr = new NbrInfo();
                        nbr.from = i;
                        nbr.edgedist = 0;
                        nbr.rooks = false;
                        nbr.nbr_vec = Vector.subtract(te.getEnd(), te.getStart());
                        nbr.nbr_vec.rotate90DegreesCounterclockwise();
                    }
                }
            }
        }

        // test edge-vertex
        for (int i = 0; i < p.edgeCount(); i++) {
            LineSegment e = p.edge(i);

            for (int j = 0; j < tp.vertexCount(); j++) {
                Vector tv = tp.vertex(j);
                if (e.onBoundary(tv, DERIVE_EPS)) {
                    // edge-vertex incidence
                    if (Vector.collinear(e.getStart(), e.getEnd(), tp.vertex(j + 1), DERIVE_EPS)) {
                        nbr = new NbrInfo();
                        nbr.from = i;
                        nbr.edgedist = tp.vertex(j + 1).distanceTo(e.getStart());
                        nbr.rooks = true;
                        return nbr;
                    } else if (Vector.collinear(e.getStart(), e.getEnd(), tp.vertex(j - 1), DERIVE_EPS)) {
                        nbr = new NbrInfo();
                        nbr.from = i;
                        nbr.edgedist = tv.distanceTo(e.getStart());
                        nbr.rooks = true;
                        return nbr;
                    } else if (nbr == null) {
                        nbr = new NbrInfo();
                        // only point incidence
                        nbr.from = i;
                        nbr.edgedist = tv.distanceTo(e.getStart());
                        nbr.rooks = false;
                        Vector backwards = Vector.subtract(tp.vertex(j - 1), tv);
                        Vector forwards = Vector.subtract(tp.vertex(j + 1), tv);
                        nbr.nbr_vec = Vector.add(backwards, forwards);
                        if (Vector.crossProduct(forwards, backwards) < 0) {
                            // reflex vertex
                            nbr.nbr_vec.invert();
                        }
                    }
                }
            }
        }

        return nbr;
    }

    private void initNeighborhood(int var) {

        // this isn't the most efficient code, but we are doing this only once per cartogram computation.
        // And we assume that a pattern consists of relatively few, low complexity shapes
        // NB: we perform this code on the original geometry iso the transformed one, to control for precision issues that the transformation might incur
        final Coordinate c = new Coordinate(var, 0);
        final Polygon p = geom.getOriginalBoundary(var);

        int numRooks = 0;
        List<NbrInfo> nbrs = new ArrayList();

        List<Coordinate> toTest = new ArrayList();
        for (int v = 0; v < pc; v++) {
            Coordinate tc = new Coordinate(v, 0);
            if (v != var) {
                toTest.add(tc);
            }
            // just test the full 8 pattern neighbors...
            toTest.add(tc.plus(pc, 0));
            toTest.add(tc.plus(-pc, 0));
            toTest.add(tc.plus(pc, 1));
            toTest.add(tc.plus(-pc, 1));
            toTest.add(tc.plus(pc, -1));
            toTest.add(tc.plus(-pc, -1));
            toTest.add(tc.plus(0, 1));
            toTest.add(tc.plus(0, -1));
        }

        for (Coordinate t : toTest) {
            Polygon tp = geom.getOriginalBoundary(t.variant()).clone();
            int dx = t.patternX();
            if (dx != 0) {
                tp.translate(Vector.multiply(dx, geom.originalRight()));
            }
            int dy = t.y;
            if (dy != 0) {
                tp.translate(Vector.multiply(dy, geom.originalUp()));
            }

            NbrInfo nbr = determine(p, tp);

            if (nbr != null) {
                nbr.coord = t;
                nbrs.add(nbr);
                if (nbr.rooks) {
                    numRooks++;
                }
            }
        }

        nbrs.sort((a, b) -> {
            if (a == b) {
                return 0;
            }

            int cmp = Integer.compare(a.from, b.from);
            if (cmp != 0) {
                return cmp;
            }

            if (!DoubleUtil.close(a.edgedist, b.edgedist)) {
                return Double.compare(a.edgedist, b.edgedist);
            }

            // NB: only one can be rooks adjacent...
            if (a.rooks) {
                return 1;
            } else if (b.rooks) {
                return -1;
            } else if (Vector.crossProduct(a.nbr_vec, b.nbr_vec) > 0) {
                return -1;
            } else {
                return 1;
            }
        });

        rooks_neighbors[var] = new Coordinate[numRooks];
        bishops_neighbors[var] = new Coordinate[nbrs.size() - numRooks];
        queens_neighbors[var] = new Coordinate[nbrs.size()];

        int ri = 0, bi = 0, qi = 0;
        for (NbrInfo nbr : nbrs) {
            if (nbr.rooks) {
                rooks_neighbors[var][ri++] = nbr.coord;
            } else {
                bishops_neighbors[var][bi++] = nbr.coord;
            }
            queens_neighbors[var][qi++] = nbr.coord;
        }
    }

    public double getAverageCellArea() {
        return Math.abs(Vector.crossProduct(right, up)) / pc;
    }

    public Vector[] getSamplingSpan() {
        return new Vector[]{up.clone(), right.clone()};
    }

    public int getPatternComplexity() {
        return pc;
    }

    public Coordinate origin() {
        return origin;
    }

    public Coordinate[] unitVectors() {
        return unitVectors;
    }

    public Coordinate getContainingCell(Vector point) {
        Coordinate c = geom.getContainingCell(point, origin);
        // the geometry may know a smarter way of mapping
        if (c != null) {
            return c;
        }

        // fallback
        double[] xy = Vector.solveVectorAddition(right, up, point);

        int px = (int) Math.round(xy[0]);
        int py = (int) Math.round(xy[1]);

        Vector shift = Vector.add(Vector.multiply(px, right), Vector.multiply(py, up));
        Vector relpoint = Vector.subtract(point, shift);

        for (int var = 0; var < pc; var++) {

            c = new Coordinate(pc * px + var, py);
            if (boundaries[var].contains(relpoint)) {
                return c;
            }

            for (Coordinate uv : unitVectors) {
                Vector uvrelpoint = Vector.subtract(relpoint, uv.patternCentroid());
                if (boundaries[var].contains(uvrelpoint)) {
                    return c.plus(uv);
                }
            }
        }

        for (int var = 0; var < pc; var++) {
            c = new Coordinate(pc * px + var, py);
            for (Coordinate uv_1 : unitVectors) {
                for (Coordinate uv_2 : unitVectors) {
                    Coordinate combo = uv_1.plus(uv_2);
                    Vector uvrelpoint = Vector.subtract(relpoint, combo.patternCentroid());
                    if (boundaries[var].contains(uvrelpoint)) {
                        return c.plus(combo);
                    }
                }
            }
        }

        System.err.println("Warning: couldn't find coordinate for " + point + " -- geometry to warped? Are there gaps?");
        return null;
    }

    public Coordinate[] adjacent(Coordinate c, AdjacencyType at) {
        switch (at) {
            case ROOKS:
                return rooks_neighbors[c.variant()];
            case BISHOPS:
                return bishops_neighbors[c.variant()];
            case QUEENS:
                return queens_neighbors[c.variant()];
            default:
                System.err.println("Warning: unexpected adjacency type " + at);
                return null;
        }
    }

    public double getNonNeighborDistance() {
        return min_nonneighbor_distance;
    }

    public class Coordinate {

        public final int x, y;

        public Coordinate(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Coordinate plus(int px, int py) {
            return new Coordinate(x + px, y + py);
        }

        public Coordinate plus(Coordinate c) {
            return new Coordinate(x + c.x, y + c.y);
        }

        public Coordinate minus(int px, int py) {
            return new Coordinate(x - px, y - py);
        }

        public Coordinate minus(Coordinate c) {

            return new Coordinate(x - c.x, y - c.y);
        }

        public Coordinate times(int k) {

            return new Coordinate(x * k, y * k);
        }

        public Coordinate times(double k) {
            return new Coordinate((int) Math.round(x * k), (int) Math.round(y * k));
        }

        public int norm() {
            return geom.norm(origin, this);
        }

        public int norm(Coordinate c) {
            return geom.norm(this, c);
        }

        public Coordinate[] adjacent(AdjacencyType at) {
            Coordinate[] relative;
            switch (at) {
                case ROOKS:
                    relative = rooks_neighbors[variant()];
                    break;
                case BISHOPS:
                    relative = bishops_neighbors[variant()];
                    break;
                case QUEENS:
                    relative = queens_neighbors[variant()];
                    break;
                default:
                    System.err.println("Warning: unexpected adjacency type " + at);
                    return null;
            }
            Coordinate[] absolute = new Coordinate[relative.length];
            Coordinate lr = localRoot();
            for (int i = 0; i < relative.length; i++) {
                absolute[i] = lr.plus(relative[i]);
            }
            return absolute;
        }

        public boolean[] rookAmongQueensSignature() {
            return rooks_among_queens[variant()];
        }

        public Vector toVector() {
            Vector v = centroids[variant()].clone();
            v.translate(patternCentroid());
            return v;
        }

        @Override
        public int hashCode() {
            //throw new UnsupportedOperationException();
            int hash = 3;
            hash = 53 * hash + this.x;
            hash = 53 * hash + this.y;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Coordinate other = (Coordinate) obj;
            if (x != other.x) {
                return false;
            }
            if (y != other.y) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }

        public Polygon getBoundary() {
            Polygon p = boundaries[variant()].clone();
            p.translate(patternCentroid());
            return p;
        }

        public Coordinate inverse() {
            return new Coordinate(-x, -y);
        }

        public Coordinate patternShift(int right, int up) {
            return new Coordinate(x + right * pc, y + up);
        }

        public Coordinate patternLeft() {
            return new Coordinate(x - pc, y);
        }

        public Coordinate patternRight() {
            return new Coordinate(x + pc, y);
        }

        public Coordinate patternUp() {
            return new Coordinate(x, y + 1);
        }

        public Coordinate patternDown() {
            return new Coordinate(x, y - 1);
        }

        public Coordinate patternVariant(int k) {
            return new Coordinate(x - variant() + k, y);
        }

        public Coordinate localRoot() {
            return new Coordinate(x - variant(), y);
        }

        public int variant() {
            return Math.floorMod(x, pc);
        }

        public int patternX() {
            return Math.floorDiv(x, pc);
        }

        public Vector patternCentroid() {
            Vector pd = Vector.multiply(y, up);
            pd.translate(Vector.multiply(patternX(), right));
            return pd;
        }

        public boolean isEquivalanceOffset() {
            return variant() == 0;
        }
    }
}
