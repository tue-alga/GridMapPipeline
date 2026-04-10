package partition;

import common.Site;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import nl.tue.geometrycore.geometry.GeometryConvertable;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.LineSegment;
import nl.tue.geometrycore.geometry.linear.Polygon;

/**
 *
 * @author Wouter Meulemans
 */
public class Cut implements GeometryConvertable<LineSegment> {

    static final Comparator<Cut> disjoint_comparator = (Cut o1, Cut o2) -> {
        return Attachment.comparator.compare(o1.start_attach, o2.start_attach);
    };

    static final Comparator<Cut> attach_comparator = (Cut o1, Cut o2) -> {
        int c = Attachment.comparator.compare(o1.start_attach, o2.start_attach);
        if (c == 0) {
            c = Attachment.comparator.compare(o1.end_attach, o2.end_attach);
        }
        return c;
    };

    Attachment start_attach, end_attach;

    // tree of all cuts (first phases) or applied cuts (during cut application)
    Cut parent = null;
    List<Cut> children = new ArrayList();

    // statistics of the subtree
    double subtree_length = 0;
    int subtree_size = 0;

    // data for area between this cuut and its children (in the full tree)
    List<Site> sites_justbelow = new ArrayList();
    Polygon polygon_justbelow = null;

    public double length() {
        return start_attach.distanceTo(end_attach);
    }

    boolean contains(Cut cut) {
        return Attachment.comparator.compare(start_attach, cut.start_attach) <= 0
                && Attachment.comparator.compare(end_attach, cut.end_attach) >= 0;
    }

    boolean containsStictly(Attachment attach) {
        return Attachment.comparator.compare(start_attach, attach) < 0 && Attachment.comparator.compare(attach, end_attach) < 0;
    }

    boolean sameCut(Cut cut) {
        return Attachment.comparator.compare(start_attach, cut.start_attach) == 0
                && Attachment.comparator.compare(end_attach, cut.end_attach) == 0;
    }

    Vector midPoint() {
        return Vector.interpolate(start_attach, end_attach, 0.5);
    }

    @Override
    public String toString() {
        return "Cut[" + start_attach + " -> " + end_attach + "]";
    }

    @Override
    public LineSegment toGeometry() {
        return new LineSegment(start_attach, end_attach);
    }
}
