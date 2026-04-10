package common.gridmath.util;

import common.gridmath.GridMath.Coordinate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import nl.tue.geometrycore.datastructures.priorityqueue.BasicIndexable;
import nl.tue.geometrycore.util.ListUtil;

/**
 *
 * @author Wouter Meulemans
 */
public class CoordinateMap<T> {

    private final List<Entry> entries = new ArrayList();
    private final List<List<Entry>> pos_pos = new ArrayList();
    private final List<List<Entry>> pos_neg = new ArrayList();
    private final List<List<Entry>> neg_neg = new ArrayList();
    private final List<List<Entry>> neg_pos = new ArrayList();
    private Coordinate origin = null;

    public CoordinateMap() {
    }

    public CoordinateMap(CoordinateMap<T> other) {
        this.origin = other.origin; // NB: we use that coordinates are immutable

        // clone all entries
        for (Entry other_e : other.entries) {
            Entry e = new Entry();
            e.setIndex(other_e.getIndex());
            e.local = other_e.local;
            e.value = other_e.value;
            entries.add(e);
        }

        Function<Entry, Entry> map = (Entry other_e) -> {
            if (other_e == null) {
                return null;
            } else {
                return entries.get(other_e.getIndex());
            }
        };

        // clone the four quadrants
        for (List<Entry> other_list : other.pos_pos) {
            List<Entry> list = new ArrayList();
            pos_pos.add(list);
            for (Entry other_occ : other_list) {
                list.add(map.apply(other_occ));
            }
        }
        for (List<Entry> other_list : other.pos_neg) {
            List<Entry> list = new ArrayList();
            pos_neg.add(list);
            for (Entry other_occ : other_list) {
                list.add(map.apply(other_occ));
            }
        }
        for (List<Entry> other_list : other.neg_neg) {
            List<Entry> list = new ArrayList();
            neg_neg.add(list);
            for (Entry other_occ : other_list) {
                list.add(map.apply(other_occ));
            }
        }
        for (List<Entry> other_list : other.neg_pos) {
            List<Entry> list = new ArrayList();
            neg_pos.add(list);
            for (Entry other_occ : other_list) {
                list.add(map.apply(other_occ));
            }
        }
    }

    public void translate(Coordinate offset) {
        if (origin != null) {
            origin = origin.plus(offset);
        }
    }

    public boolean put(Coordinate global, T element) {
        if (origin == null) {
            origin = global;
        }
        return putLocal(global.minus(origin), element);
    }

    private boolean putLocal(Coordinate local, T element) {

        Entry e = getLocal(local);

        if (e == null) {
            // not a neighbor, nor a coordinate
            e = new Entry();

            e.local = local;
            e.value = element;

            setLocal(local, e);
            ListUtil.insert(e, entries);

        } else {
            e.value = element;
        }

        return true;
    }

    public T get(Coordinate global) {
        if (origin == null) {
            return null;
        }
        Entry e = getLocal(global.minus(origin));
        if (e == null) {
            return null;
        }
        return e.value;
    }

    public T getOrDefault(Coordinate global, T defaultvalue) {
        if (origin == null) {
            return defaultvalue;
        }
        Entry e = getLocal(global.minus(origin));
        if (e == null) {
            return defaultvalue;
        }
        return e.value;
    }

    public T remove(Coordinate global) {
        if (origin == null) {
            return null;
        }
        return removeLocal(global.minus(origin));
    }

    private T removeLocal(Coordinate local) {

        Entry e = getLocal(local);
        if (e == null) {
            return null;
        }

        ListUtil.swapRemove(e, entries);
        setLocal(e.local, null);

        if (entries.isEmpty()) {
            // NB: we still do the above, to clear the grid appropriately
            origin = null;
        }

        return e.value;
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return origin == null;
    }

    public boolean containsKey(Coordinate global) {
        if (origin == null) {
            return false;
        }
        return containsKeyLocal(global.minus(origin));
    }

    private boolean containsKeyLocal(Coordinate local) {
        return getLocal(local) != null;
    }

    private Entry getLocal(Coordinate local) {

        // determine
        int x, y;
        List<List<Entry>> quad;
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

    private void setLocal(Coordinate local, Entry entry) {

        // determine
        int x, y;
        List<List<Entry>> quad;
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
        quad.get(x).set(y, entry);
    }

    public Iterable<Entry> entrySet() {
        return entries;
    }

    public Coordinate[] keyArray() {
        final int n = entries.size();
        Coordinate[] coords = new Coordinate[n];
        for (int i = 0; i < n; i++) {
            coords[i] = entries.get(i).local.plus(origin);
        }
        return coords;
    }

    public void clear() {
        origin = null;
        entries.clear();
        pos_pos.clear();
        pos_neg.clear();
        neg_neg.clear();
        neg_pos.clear();
    }

    public void addKeysToSet(CoordinateSet set) {
        for (Entry e : entries) {
            set.add(e.local.plus(origin));
        }
    }

    public class Entry extends BasicIndexable {

        private Coordinate local;
        private T value;

        public Coordinate getCoordinate() {
            return local.plus(origin);
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }
    }
}
