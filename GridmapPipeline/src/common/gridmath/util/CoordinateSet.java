package common.gridmath.util;

import arrange.util.Random;
import common.gridmath.AdjacencyType;
import common.gridmath.GridMath;
import common.gridmath.GridMath.Coordinate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;
import nl.tue.geometrycore.datastructures.priorityqueue.BasicIndexable;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.util.ListUtil;

/**
 *
 * @author Wouter Meulemans
 */
public class CoordinateSet implements Iterable<Coordinate> {

    private final GridMath gridmath;
    private final AdjacencyType track;
    private final List<Occupied> occupied = new ArrayList();
    private final List<Occupied> rooks = new ArrayList();
    private final List<Occupied> bishops = new ArrayList();
    private final List<List<Occupied>> pos_pos = new ArrayList();
    private final List<List<Occupied>> pos_neg = new ArrayList();
    private final List<List<Occupied>> neg_neg = new ArrayList();
    private final List<List<Occupied>> neg_pos = new ArrayList();
    private Coordinate origin = null;
    private Vector sum = Vector.origin();
    private int insertion = 0;

    public CoordinateSet(GridMath gridmath) {
        this(gridmath, AdjacencyType.QUEENS);
    }

    public CoordinateSet(GridMath gridmath, AdjacencyType track) {
        this.gridmath = gridmath;
        this.track = track;
    }

    public CoordinateSet(CoordinateSet other) {
        this.gridmath = other.gridmath;
        this.origin = other.origin; // NB: we use that coordinates are immutable
        this.insertion = other.insertion;
        this.track = other.track;
        this.sum = other.sum.clone();

        // clone all occupied
        for (Occupied other_occ : other.occupied) {
            Occupied occ = new Occupied();
            occ.setIndex(other_occ.getIndex());
            occ.local = other_occ.local;
            occ.insertion = other_occ.insertion;
            occ.state = State.OCCUPIED;
            occ.rook_count = other_occ.rook_count;
            occ.bishop_count = other_occ.bishop_count;
            occupied.add(occ);
        }

        // clone all neighbors
        for (Occupied other_occ : other.rooks) {
            Occupied occ = new Occupied();
            occ.setIndex(other_occ.getIndex());
            occ.local = other_occ.local;
            occ.insertion = other_occ.insertion;
            occ.state = State.ROOKS;
            occ.rook_count = other_occ.rook_count;
            occ.bishop_count = other_occ.bishop_count;
            rooks.add(occ);
        }

        for (Occupied other_occ : other.bishops) {
            Occupied occ = new Occupied();
            occ.setIndex(other_occ.getIndex());
            occ.local = other_occ.local;
            occ.insertion = other_occ.insertion;
            occ.state = State.BISHOPS;
            occ.rook_count = other_occ.rook_count;
            occ.bishop_count = other_occ.bishop_count;
            bishops.add(occ);
        }

        Function<Occupied, Occupied> map = (Occupied other_occ) -> {
            if (other_occ == null) {
                return null;
            } else {
                switch (other_occ.state) {
                    case OCCUPIED:
                        return occupied.get(other_occ.getIndex());
                    case ROOKS:
                        return rooks.get(other_occ.getIndex());
                    case BISHOPS:
                        return bishops.get(other_occ.getIndex());
                    default:
                        System.err.println("Warning: unexpected state " + other_occ.state);
                        return null;
                }
            }
        };

        // clone the four quadrants
        for (List<Occupied> other_list : other.pos_pos) {
            List<Occupied> list = new ArrayList();
            pos_pos.add(list);
            for (Occupied other_occ : other_list) {
                list.add(map.apply(other_occ));
            }
        }
        for (List<Occupied> other_list : other.pos_neg) {
            List<Occupied> list = new ArrayList();
            pos_neg.add(list);
            for (Occupied other_occ : other_list) {
                list.add(map.apply(other_occ));
            }
        }
        for (List<Occupied> other_list : other.neg_neg) {
            List<Occupied> list = new ArrayList();
            neg_neg.add(list);
            for (Occupied other_occ : other_list) {
                list.add(map.apply(other_occ));
            }
        }
        for (List<Occupied> other_list : other.neg_pos) {
            List<Occupied> list = new ArrayList();
            neg_pos.add(list);
            for (Occupied other_occ : other_list) {
                list.add(map.apply(other_occ));
            }
        }
    }

    public void translate(Coordinate offset) {
        if (!offset.isEquivalanceOffset()) {
            throw new UnsupportedOperationException();
        }

        if (origin != null) {
            origin = origin.plus(offset);
        }
    }

    public boolean add(Coordinate global) {
        if (origin == null) {
            origin = global.localRoot();
        }
        return addLocal(global.minus(origin));
    }

    private boolean addLocal(Coordinate local) {

        { // for the coordinate itself
            Occupied occ = getLocal(local);

            if (occ == null) {
                // not a neighbor, nor a coordinate
                occ = new Occupied();

                occ.local = local;
                occ.rook_count = 0;
                occ.bishop_count = 0;

                setLocal(local, occ);

            } else {
                switch (occ.state) {
                    case OCCUPIED:
                        // already in the set   
                        return false;
                    case ROOKS:
                        ListUtil.swapRemove(occ, rooks);
                        break;
                    case BISHOPS:
                        ListUtil.swapRemove(occ, bishops);
                        break;
                    default:
                        System.err.println("Warning: unexpected state " + occ.state);
                        break;
                }
            }

            occ.state = State.OCCUPIED;
            occ.insertion = insertion++;
            ListUtil.insert(occ, occupied);

            sum.translate(occ.local.toVector());
        }

        // neighbor stuff
        if (available(AdjacencyType.ROOKS)) {
            for (Coordinate nbr : local.adjacent(AdjacencyType.ROOKS)) {
                Occupied nbrocc = getLocal(nbr);
                if (nbrocc == null) {
                    nbrocc = new Occupied();

                    nbrocc.local = nbr;
                    nbrocc.rook_count = 1;
                    nbrocc.bishop_count = 0;

                    nbrocc.state = State.ROOKS;
                    nbrocc.insertion = insertion++;
                    ListUtil.insert(nbrocc, rooks);
                    setLocal(nbr, nbrocc);
                } else {
                    nbrocc.rook_count++;
                    if (nbrocc.state == State.BISHOPS) {
                        ListUtil.swapRemove(nbrocc, bishops);

                        nbrocc.state = State.ROOKS;
                        nbrocc.insertion = insertion++;
                        ListUtil.insert(nbrocc, rooks);
                    }
                }
            }
        }

        if (available(AdjacencyType.BISHOPS)) {
            for (Coordinate nbr : local.adjacent(AdjacencyType.BISHOPS)) {
                Occupied nbrocc = getLocal(nbr);
                if (nbrocc == null) {
                    nbrocc = new Occupied();

                    nbrocc.local = nbr;
                    nbrocc.rook_count = 0;
                    nbrocc.bishop_count = 1;

                    nbrocc.state = State.BISHOPS;
                    nbrocc.insertion = insertion++;
                    ListUtil.insert(nbrocc, bishops);
                    setLocal(nbr, nbrocc);
                } else {
                    nbrocc.bishop_count++;
                }
            }
        }

        return true;
    }

    public boolean remove(Coordinate global) {
        if (origin == null) {
            return false;
        }
        return removeLocal(global.minus(origin));
    }

    private boolean removeLocal(Coordinate local) {

        { // for the coordinate itself
            Occupied occ = getLocal(local);
            if (occ == null || occ.state != State.OCCUPIED) {
                return false;
            }

            sum.untranslate(occ.local.toVector());

            ListUtil.swapRemove(occ, occupied);
            if (occ.rook_count > 0) {
                // this is still a neighbor of something
                occ.state = State.ROOKS;
                occ.insertion++;
                ListUtil.insert(occ, rooks);
            } else if (occ.bishop_count > 0) {
                // this is still a neighbor of something
                occ.state = State.BISHOPS;
                occ.insertion++;
                ListUtil.insert(occ, bishops);
            } else {
                setLocal(occ.local, null);
            }
        }

        // neighbor stuff
        if (available(AdjacencyType.ROOKS)) {
            for (Coordinate nbr : local.adjacent(AdjacencyType.ROOKS)) {
                Occupied nbrocc = getLocal(nbr);
                nbrocc.rook_count--;
                if (nbrocc.rook_count == 0 && nbrocc.state == State.ROOKS) {
                    ListUtil.swapRemove(nbrocc, rooks);
                    if (nbrocc.bishop_count > 0) {
                        nbrocc.state = State.BISHOPS;
                        nbrocc.insertion++;
                        ListUtil.insert(nbrocc, bishops);
                    } else {
                        setLocal(nbr, null);
                    }
                }
            }
        }

        if (available(AdjacencyType.BISHOPS)) {
            for (Coordinate nbr : local.adjacent(AdjacencyType.BISHOPS)) {
                Occupied nbrocc = getLocal(nbr);
                nbrocc.bishop_count--;
                if (nbrocc.bishop_count == 0 && nbrocc.state == State.BISHOPS) {
                    ListUtil.swapRemove(nbrocc, bishops);
                    setLocal(nbr, null);
                }
            }
        }

        if (occupied.isEmpty()) {
            // NB: we still do the above, to clear the grid appropriately
            origin = null;
            sum.set(0, 0);
            insertion = 0;
        }

        return true;
    }

    @Override
    public Iterator<Coordinate> iterator() {
        return new Iterator<Coordinate>() {
            int index = 0;
            final int oc = occupied.size();

            @Override
            public boolean hasNext() {
                return index < oc;
            }

            @Override
            public Coordinate next() {
                return occupied.get(index++).local.plus(origin);
            }
        };
    }

    public Iterable<Coordinate> coordinates() {
        return this;
    }

    public int size() {
        return occupied.size();
    }

    public boolean isEmpty() {
        return origin == null;
    }

    public boolean contains(Coordinate global) {
        if (origin == null) {
            return false;
        }
        return containsLocal(global.minus(origin));
    }

    private boolean containsLocal(Coordinate local) {

        Occupied occ = getLocal(local);
        return occ != null && occ.state == State.OCCUPIED;
    }

    public boolean containsNeighbor(Coordinate global, AdjacencyType at) {
        assert available(at);

        if (origin == null) {
            return false;
        }
        return containsNeighborLocal(global.minus(origin), at);
    }

    private boolean available(AdjacencyType at) {
        if (track == null) {
            return false;
        } else if (track == AdjacencyType.QUEENS) {
            return true;
        } else {
            return track == at;
        }
    }

    private boolean containsNeighborLocal(Coordinate local, AdjacencyType at) {

        Occupied occ = getLocal(local);
        if (occ == null) {
            return false;
        }

        switch (occ.state) {
            case OCCUPIED:
                return false;
            case ROOKS:
                return at == AdjacencyType.ROOKS || at == AdjacencyType.QUEENS;
            case BISHOPS:
                return at == AdjacencyType.BISHOPS || at == AdjacencyType.QUEENS;
            default:
                System.err.println("Warning: unexpected adjacency type " + at);
                return false;
        }
    }

    private Occupied getLocal(Coordinate local) {

        // determine
        int x, y;
        List<List<Occupied>> quad;
        if (local.x >= 0) {
            x = local.x;
            if (local.y >= 0) {
                y = local.y;
                quad = pos_pos;
            } else {
                y = -local.y - 1;
                quad = pos_neg;
            }
        } else {
            x = -local.x - 1;
            if (local.y >= 0) {
                y = local.y;
                quad = neg_pos;
            } else {
                y = -local.y - 1;
                quad = neg_neg;
            }
        }

        if (quad.size() > x && quad.get(x).size() > y) {
            return quad.get(x).get(y);
        } else {
            return null;
        }
    }

    private void setLocal(Coordinate local, Occupied elt) {

        // determine
        int x, y;
        List<List<Occupied>> quad;
        if (local.x >= 0) {
            x = local.x;
            if (local.y >= 0) {
                y = local.y;
                quad = pos_pos;
            } else {
                y = -local.y - 1;
                quad = pos_neg;
            }
        } else {
            x = -local.x - 1;
            if (local.y >= 0) {
                y = local.y;
                quad = neg_pos;
            } else {
                y = -local.y - 1;
                quad = neg_neg;
            }
        }

        // ensure the slot exists
        while (quad.size() <= x) {
            quad.add(new ArrayList());
        }
        while (quad.get(x).size() <= y) {
            quad.get(x).add(null);
        }

        // set it
        quad.get(x).set(y, elt);
    }

    public boolean containsSome(Coordinate... globals) {
        for (Coordinate c : globals) {
            if (contains(c)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAll(Coordinate... globals) {
        for (Coordinate c : globals) {
            if (!contains(c)) {
                return false;
            }
        }
        return true;
    }

    public boolean containsNone(Coordinate... globals) {
        for (Coordinate c : globals) {
            if (contains(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests whether the coordinate has both neighbors inside and outside the
     * set. That is, is it a coordinate along the edges of the shape. Note that,
     * if the coordinate is in the set, but none of its neighbors is, then it is
     * considered to be nonedge coordinate!
     *
     * @param global
     * @return
     */
    public boolean isEdge(Coordinate global) {
        if (origin == null) {
            return false;
        }

        Coordinate local = global.minus(origin);

        boolean in = false;
        boolean out = false;
        for (Coordinate d : local.adjacent(AdjacencyType.ROOKS)) {
            if (containsLocal(d)) {
                in = true;
                if (out) {
                    return true;
                }
            } else {
                out = true;
                if (in) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tests whether the set of coordinates is connected, in the given adjacency
     * type. Note that it returns true for an empty set.
     *
     * @param at
     * @return
     */
    public boolean isConnected(AdjacencyType at) {
        if (occupied.size() <= 1) {
            return true;
        }

        List<Occupied> visited = new ArrayList(occupied.size());

        Occupied first = occupied.get(0);
        visited.add(first);
        first.flag = true;

        int next = 0;
        while (next < visited.size()) {
            Occupied occ = visited.get(next++);
            for (Coordinate nbr : occ.local.adjacent(at)) {
                Occupied nbrocc = getLocal(nbr);
                if (nbrocc != null && nbrocc.state == State.OCCUPIED && !nbrocc.flag) {
                    visited.add(nbrocc);
                    nbrocc.flag = true;
                }
            }
        }

        boolean connected = visited.size() == occupied.size();
        for (Occupied occ : visited) {
            occ.flag = false;
        }
        return connected;
    }

    /**
     * Tests whether the set of coordinates would still be connected after
     * removing the given coordinate, in the given adjacency type. Note that it
     * returns true when this hypothetical set is empty.
     *
     * @param ignore
     * @param at
     * @return
     */
    public boolean isConnectedWithout(Coordinate ignore, AdjacencyType at) {
        if (occupied.size() <= 1) {
            return true;
        }

        Occupied ignore_occ = getLocal(ignore.minus(origin));
        if (ignore_occ == null || ignore_occ.state != State.OCCUPIED) {
            return isConnected(at);
        }

        List<Occupied> visited = new ArrayList(occupied.size());

        visited.add(ignore_occ);
        ignore_occ.flag = true;

        Occupied first = ignore_occ.getIndex() == 0 ? occupied.get(1) : occupied.get(0);
        visited.add(first);
        first.flag = true;

        int next = 1; // NB: 1 to skip over the ignore one
        while (next < visited.size()) {
            Occupied occ = visited.get(next++);
            for (Coordinate nbr : occ.local.adjacent(at)) {
                Occupied nbrocc = getLocal(nbr);
                if (nbrocc != null && nbrocc.state == State.OCCUPIED && !nbrocc.flag) {
                    visited.add(nbrocc);
                    nbrocc.flag = true;
                }
            }
        }

        boolean connected = visited.size() == occupied.size();
        for (Occupied occ : visited) {
            occ.flag = false;
        }
        return connected;
    }

    /**
     * Tests whether, locally, the neighborhood of the given coordinate is
     * connected. That is, if we consider the cycle of all Queen's neighbors,
     * are those of the specified adjacency type, in the same component of this
     * cycle?
     *
     * @param global
     * @param at
     * @return
     */
    public boolean contiguousNeighborhood(Coordinate global, AdjacencyType at) {
        
        // for queens:
        // we just need to check whether its neighborhood consists of exactly two runs: one inside and one outside the set
        // for rooks:
        // we do the same as above, but include queens as well in counting switches, TODO: excepting runs of ONLY queens...
        // for bishops:
        // unsupported        
        if (origin == null) {
            return false;
        }

        Coordinate local = global.minus(origin);

        Coordinate[] nbrs = local.adjacent(AdjacencyType.QUEENS);
        boolean[] raq = at == AdjacencyType.ROOKS ? local.rookAmongQueensSignature() : null;

        boolean prevIn = containsLocal(nbrs[nbrs.length - 1]);
        int containedRuns = 0;
        boolean runToConsider = prevIn && (raq == null || raq[nbrs.length - 1]);

        for (int i = 0; i < nbrs.length; i++) {
            Coordinate nbr = nbrs[i];

            boolean in = containsLocal(nbr);
            if (in == prevIn) {
                // keep going
                
                // NB: raq != null if runToConsider is to be false
                runToConsider = runToConsider || (in && !runToConsider && raq[i]); 
            } else {
                if (runToConsider) {
                    containedRuns++;
                }
                prevIn = in;
                
                runToConsider = in && (raq == null || raq[i]);
            }
        }
        return containedRuns == 1;
    }
       
    public Coordinate arbitraryCoordinate() {
        // just return the first coordinate in the set
        return origin.plus(occupied.get(0).local);
    }

    public Coordinate randomCoordinate() {
        int r = Random.nextInt(occupied.size());
        return origin.plus(occupied.get(r).local);
    }

    public Coordinate barycenter() {
        return gridmath.getContainingCell(continuousBarycenter());
    }

    private Coordinate barycenterLocal() {
        return gridmath.getContainingCell(continuousBarycenterLocal());
    }

    public GridMath getGridmath() {
        return gridmath;
    }

    public Vector continuousBarycenter() {
        Vector result = continuousBarycenterLocal();
        result.translate(origin.toVector());
        return result;
    }

    private Vector continuousBarycenterLocal() {
        return Vector.multiply(1.0 / occupied.size(), sum);
    }

    public Iterable<Coordinate> neighbors(AdjacencyType at) {
        assert available(at);

        switch (at) {
            case ROOKS:
                return () -> {
                    return new Iterator<>() {
                        int index = 0;
                        final int rc = rooks.size();

                        @Override
                        public boolean hasNext() {
                            return index < rc;
                        }

                        @Override
                        public Coordinate next() {
                            return rooks.get(index++).local.plus(origin);
                        }
                    };
                };
            case BISHOPS:
                return () -> {
                    return new Iterator<>() {
                        int index = 0;
                        final int bc = bishops.size();

                        @Override
                        public boolean hasNext() {
                            return index < bc;
                        }

                        @Override
                        public Coordinate next() {
                            return bishops.get(index++).local.plus(origin);
                        }
                    };
                };
            case QUEENS:
                return () -> {
                    return new Iterator<>() {
                        int index = 0;
                        final int rc = rooks.size();
                        final int total = rc + bishops.size();

                        @Override
                        public boolean hasNext() {
                            return index < total;
                        }

                        @Override
                        public Coordinate next() {
                            if (index < rc) {
                                return rooks.get(index++).local.plus(origin);
                            } else {
                                return bishops.get(index++ - rc).local.plus(origin);
                            }
                        }
                    };
                };
            default:
                System.err.println("Warning: unexpected adjacency type " + at);
                return null;
        }
    }

    /**
     * Do the sets have a common coordinate?
     *
     * @param other
     * @return
     */
    public boolean intersects(CoordinateSet other) {

        if (occupied.size() > other.occupied.size()) {
            return other.intersects(this);
        }
        // this is the smaller set

        if (origin == null) {
            return false;
        }

        // local + origin = other.local + other.origin
        // other.local = local + origin - other.origin
        Coordinate off = origin.minus(other.origin);
        for (Occupied occ : occupied) {
            if (other.containsLocal(occ.local.plus(off))) {
                return true;
            }
        }
        return false;
    }

    public void addAll(CoordinateSet other) {
        if (origin == null) {
            origin = other.origin;
            for (Occupied other_occ : other.occupied) {
                addLocal(other_occ.local);
            }
        } else {
            // local + origin = other.local + other.origin
            // local = other.local + other.origin - origin
            Coordinate off = other.origin.minus(origin);
            for (Occupied other_occ : other.occupied) {
                addLocal(other_occ.local.plus(off));
            }
        }
    }

    /**
     * Does this set contain a coordinate, or a coordinate that is adjacent to,
     * that is also in the other set?
     *
     * @param other
     * @param at
     * @return
     */
    public boolean touches(CoordinateSet other, AdjacencyType at) {
        assert available(at);

        if (origin == null || other.origin == null) {
            return false;
        }
        // local + origin = other.local + other.origin
        // other.local = local + origin - other.origin
        Coordinate off = origin.minus(other.origin);
        for (Occupied occ : occupied) {
            if (other.containsLocal(occ.local.plus(off))) {
                return true;
            }
        }
        if (at == AdjacencyType.ROOKS || at == AdjacencyType.QUEENS) {
            for (Occupied occ : rooks) {
                if (other.containsLocal(occ.local.plus(off))) {
                    return true;
                }
            }
        }
        if (at == AdjacencyType.BISHOPS || at == AdjacencyType.QUEENS) {
            for (Occupied occ : bishops) {
                if (other.containsLocal(occ.local.plus(off))) {
                    return true;
                }
            }
        }
        return false;
    }

    public int intersectionSize(CoordinateSet other) {

        if (occupied.size() > other.occupied.size()) {
            return other.intersectionSize(this);
        }
        // this is the smaller set
        if (origin == null) {
            return 0;
        }
        // local + origin = other.local + other.origin
        // other.local = local + origin - other.origin
        Coordinate off = origin.minus(other.origin);
        int size = 0;
        for (Occupied occ : occupied) {
            if (other.containsLocal(occ.local.plus(off))) {
                size++;
            }
        }
        return size;
    }

    public boolean containsAtLeast(int k, Coordinate... globals) {
        for (Coordinate c : globals) {
            if (contains(c)) {
                k--;
                if (k <= 0) {
                    return true;
                }
            }
        }
        return k <= 0;
    }

    public List<Coordinate> intersection(CoordinateSet other) {

        if (occupied.size() > other.occupied.size()) {
            return other.intersection(this);
        }
        // this is the smaller set

        List<Coordinate> is = new ArrayList();
        if (origin == null) {
            return is;
        }

        // local + origin = other.local + other.origin
        // other.local = local + origin - other.origin
        Coordinate off = origin.minus(other.origin);
        for (Occupied occ : occupied) {
            if (other.containsLocal(occ.local.plus(off))) {
                is.add(occ.local.plus(origin));
            }
        }
        return is;
    }

    public Coordinate[] extract() {
        Coordinate[] coords = new Coordinate[occupied.size()];
        for (Occupied occ : occupied) {
            coords[occ.getIndex()] = occ.local.plus(origin);
        }
        return coords;
    }

    public void fillHoles(AdjacencyType ex_at) {
        // add all holes to this set
        // NB: this runs on Queen's adjacency, regardless of the given parameter, 
        // since we need to be able to go around convex corners via Queen's adjacent things, for Rook's adjacency also

        assert available(AdjacencyType.QUEENS);

        { // first, mark the outer boundary
            Occupied lm = null;
            double lm_x = Double.POSITIVE_INFINITY;

            for (Occupied nbr : rooks) {
                double nbr_x = Double.POSITIVE_INFINITY;
                for (Vector v : nbr.local.getBoundary().vertices()) {
                    if (v.getX() < nbr_x) {
                        nbr_x = v.getX();
                    }
                }
                if (nbr_x < lm_x) {
                    lm = nbr;
                    lm_x = nbr_x;
                }
            }

            Stack<Occupied> stack = new Stack<>();
            lm.flag = true;
            stack.add(lm);

            while (!stack.isEmpty()) {
                Occupied next = stack.pop();
                for (Coordinate nbr : next.local.adjacent(ex_at)) {
                    Occupied occ_nbr = getLocal(nbr);
                    if (occ_nbr != null && !occ_nbr.flag && occ_nbr.state != State.OCCUPIED) {
                        occ_nbr.flag = true;
                        stack.add(occ_nbr);
                    }
                }
            }
        }

        {// explore all unmarked neighbors}
            int index = 0;
            while (index < rooks.size()) {
                Occupied occ = rooks.get(index);
                if (occ.flag) {
                    // outerface
                    index++;
                    continue;
                }

                Stack<Coordinate> stack = new Stack<>();
                stack.push(occ.local);
                addLocal(occ.local); // NB: this switches out occ from the rooks list, so we dont increase index!

                while (!stack.isEmpty()) {
                    Coordinate next = stack.pop();

                    for (Coordinate nbr : next.adjacent(AdjacencyType.ROOKS)) {
                        Occupied occ_nbr = getLocal(nbr);
                        if (occ_nbr != null && occ_nbr.state == State.ROOKS) {
                            stack.push(nbr);
                            addLocal(nbr);
                        }
                    }
                }
            }
        }

        { // clear flags
            for (Occupied occ : rooks) {
                occ.flag = false;
            }
            for (Occupied occ : bishops) {
                occ.flag = false;
            }
        }
    }

    public List<CoordinateSet> detectHoles(AdjacencyType ex_at) {
        // add all holes to this set
        // NB: this runs on Queen's adjacency, regardless of the given parameter, 
        // since we need to be able to go around convex corners via Queen's adjacent things, for Rook's adjacency also

        assert available(AdjacencyType.QUEENS);

        { // first, mark the outer boundary
            Occupied lm = null;
            double lm_x = Double.POSITIVE_INFINITY;

            for (Occupied nbr : rooks) {
                double nbr_x = Double.POSITIVE_INFINITY;
                for (Vector v : nbr.local.getBoundary().vertices()) {
                    if (v.getX() < nbr_x) {
                        nbr_x = v.getX();
                    }
                }
                if (nbr_x < lm_x) {
                    lm = nbr;
                    lm_x = nbr_x;
                }
            }

            Stack<Occupied> stack = new Stack<>();
            lm.flag = true;
            stack.add(lm);

            while (!stack.isEmpty()) {
                Occupied next = stack.pop();
                for (Coordinate nbr : next.local.adjacent(ex_at)) {
                    Occupied occ_nbr = getLocal(nbr);
                    if (occ_nbr != null && !occ_nbr.flag && occ_nbr.state != State.OCCUPIED) {
                        occ_nbr.flag = true;
                        stack.add(occ_nbr);
                    }
                }
            }
        }

        List<CoordinateSet> holes = new ArrayList();

        {// explore all unmarked neighbors}
            int index = 0;
            while (index < rooks.size()) {
                Occupied occ = rooks.get(index);
                if (occ.flag) {
                    // outerface or already detected
                    index++;
                    continue;
                }

                CoordinateSet hole = new CoordinateSet(gridmath);
                holes.add(hole);

                Stack<Coordinate> stack = new Stack<>();
                stack.push(occ.local);
                occ.flag = true;
                hole.add(occ.local); // NB: we build in local coordinates, so we translate at the end

                // hole contains the currently discovered part of the hole
                // stack contains the coordinates that have not explored their neighbors yet
                while (!stack.isEmpty()) {
                    Coordinate next = stack.pop();

                    for (Coordinate nbr : next.adjacent(AdjacencyType.ROOKS)) {
                        if (hole.contains(nbr)) {
                            // already discovered
                            continue;
                        }
                            
                        Occupied occ_nbr = getLocal(nbr);
                        if (occ_nbr == null) {
                            // part of hole, but not the boundary
                            stack.push(nbr);
                            hole.add(nbr);
                        } else if (occ_nbr.state != State.OCCUPIED) {
                            // part of the hole
                            stack.push(nbr);
                            hole.add(nbr);
                            occ_nbr.flag = true; // to not start another hole from this down the line
                        } // else, it is part of the set and thus not part of the hole
                    }
                }

                index++;
            }
        }

        { // clear flags
            for (Occupied occ : rooks) {
                occ.flag = false;
            }
            for (Occupied occ : bishops) {
                occ.flag = false;
            }
        }

        for (CoordinateSet h : holes) {
            h.translate(origin);
        }

        return holes;
    }

    public Coordinate[] detectHoleBoundaries(AdjacencyType ex_at) {
        // add all holes to this set
        // NB: this runs on Queen's adjacency, regardless of the given parameter, 
        // since we need to be able to go around convex corners via Queen's adjacent things, for Rook's adjacency also

        assert available(AdjacencyType.QUEENS);

        int outer = 0;

        { // first, mark the outer boundary
            Occupied lm = null;
            double lm_x = Double.POSITIVE_INFINITY;

            for (Occupied nbr : rooks) {
                double nbr_x = Double.POSITIVE_INFINITY;
                for (Vector v : nbr.local.getBoundary().vertices()) {
                    if (v.getX() < nbr_x) {
                        nbr_x = v.getX();
                    }
                }
                if (nbr_x < lm_x) {
                    lm = nbr;
                    lm_x = nbr_x;
                }
            }

            Stack<Occupied> stack = new Stack<>();
            lm.flag = true;
            outer++;
            stack.add(lm);

            while (!stack.isEmpty()) {
                Occupied next = stack.pop();
                for (Coordinate nbr : next.local.adjacent(ex_at)) {
                    Occupied occ_nbr = getLocal(nbr);
                    if (occ_nbr != null && !occ_nbr.flag && occ_nbr.state != State.OCCUPIED) {
                        occ_nbr.flag = true;
                        stack.add(occ_nbr);

                        if (occ_nbr.state == State.ROOKS) {
                            outer++;
                        }
                    }
                }
            }
        }

        Coordinate[] boundary = new Coordinate[rooks.size() - outer];

        {// explore all unmarked neighbors}
            int i = 0;
            for (Occupied occ : rooks) {
                if (!occ.flag) {
                    boundary[i++] = occ.local.plus(origin);
                }
            }
        }

        { // clear flags
            for (Occupied occ : rooks) {
                occ.flag = false;
            }
            for (Occupied occ : bishops) {
                occ.flag = false;
            }
        }

        return boundary;
    }

    private enum State {
        OCCUPIED, ROOKS, BISHOPS;
    }

    private class Occupied extends BasicIndexable {

        private Coordinate local;
        private State state;
        private int rook_count, bishop_count;
        private boolean flag = false; // should always be false, only used during local algorithms
        private int insertion;

        @Override
        public String toString() {
            return "[" + getIndex() + ", " + local + ", " + state + ", rc " + rook_count + ", bc " + bishop_count + ", " + flag + ", " + insertion + ", " + super.toString() + ']';
        }

    }
}
