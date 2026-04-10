package partition;

import common.Outline;
import java.util.Comparator;
import java.util.Iterator;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Polygon;

/**
 *
 * @author Wouter Meulemans
 */
class Attachment extends Vector {

    static final Comparator<Attachment> comparator = (Attachment o1, Attachment o2) -> {
        int c = Integer.compare(o1.edge, o2.edge);
        if (c == 0) {
            // same vertex/edge
            return Double.compare(o1.dist, o2.dist);
        }
        return c;
    };

    static final Comparator<Attachment> cut_comparator = (Attachment o1, Attachment o2) -> {
        int c = comparator.compare(o1, o2);

        if (c == 0) {
            // use the other endpoint
            if (o1.isStartAttach() && !o2.isStartAttach()) {
                // o2 comes before o1
                return 1;
            } else if (!o1.isStartAttach() && o2.isStartAttach()) {
                // o1 comes before o2
                return -1;
            } else {
                // both are startAttach or both are endAttach
                return comparator.compare(o2.other(), o1.other());
                // NB: this is inverted, to ensure proper nesting
            }
        }
        return c;
    };

    Cut cut;
    int edge;
    double dist;

    public Attachment(Vector loc, Cut cut, int vertex) {
        super(loc);
        this.cut = cut;
        this.edge = vertex;
        this.dist = 0;
    }

    public Attachment(Vector loc, Cut cut, int edge, double dist) {
        super(loc);
        this.cut = cut;
        this.edge = edge;
        this.dist = dist;
    }

    public boolean isStartAttach() {
        return cut.start_attach == this;
    }

    public Attachment other() {
        return isStartAttach() ? cut.end_attach : cut.start_attach;
    }

    public double perimeterTo(Outline partition, Attachment other) {

        Vector prev = this;
        double len = 0;
        for (Vector iv : verticesTo(partition, other)) {
            len += prev.distanceTo(iv);
            prev = iv;
        }
        len += prev.distanceTo(other);
        return len;
    }

    /**
     * Iterates over all vertices from this attachment to the other (both
     * exclusive), along the partition boundary.
     */
    public Iterable<Vector> verticesTo(Polygon partition, Attachment other) {
        return new Iterable<Vector>() {
            @Override
            public Iterator<Vector> iterator() {
                return new Iterator<Vector>() {

                    int v = edge + 1;

                    @Override
                    public boolean hasNext() {
                        return v < other.edge || (v == other.edge && other.dist > 0);
                    }

                    @Override
                    public Vector next() {
                        Vector result = partition.vertex(v);
                        v++;
                        return result;
                    }
                };
            }

        };
    }

    @Override
    public String toString() {
        return "(" + edge + ", " + dist + ")=>"+getX()+","+getY();
    }

}
