package arrange.model;

import arrange.MosaicConstants;
import common.gridmath.GridMath;
import common.gridmath.GridMath.Coordinate;
import common.gridmath.util.CoordinateMap;
import common.gridmath.util.CoordinateSet;
import common.util.Transform;
import java.util.ArrayList;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;

/**
 *
 * @author Wouter Meulemans
 */
public class GuidingShape extends CoordinateSet {

    // the quality of each cell
    private final CoordinateMap<Double> cellquality;
    // for mapping back to original space
    private final double factor;
    private final Vector translation;
    private double quality;
    private int minx, maxx, miny, maxy;

    public GuidingShape(GridMath gridmath, CoordinateMap<Double> cellquality, double factor, Vector translation) {
        super(gridmath);
        this.cellquality = cellquality;
        this.factor = factor;
        this.translation = translation;
        this.quality = Double.NaN;
    }

    public GuidingShape(GuidingShape other) {
        super(other);
        cellquality = new CoordinateMap<>(other.cellquality);
        factor = other.factor;
        translation = other.translation.clone();
        quality = other.quality;
        minx = other.minx;
        maxx = other.maxx;
        miny = other.miny;
        maxy = other.maxy;
    }

    public void constructionDone() {
        quality = 0;
        for (Coordinate c : this) {
            quality += cellquality.getOrDefault(c, 0.0);
        }
        minx = Integer.MAX_VALUE;
        maxx = Integer.MIN_VALUE;
        miny = Integer.MAX_VALUE;
        maxy = Integer.MIN_VALUE;

        for (Coordinate c : this) {
            minx = Math.min(minx, c.x);
            maxx = Math.max(maxx, c.x);
            miny = Math.min(miny, c.y);
            maxy = Math.max(maxy, c.y);
        }
    }

    public double getQuality() {
        return quality;
    }

    public double desirability(Coordinate c) {
        return cellquality.getOrDefault(c, 0.0);
    }

    @Override
    public void translate(Coordinate t) {
        super.translate(t);

        minx += t.x;
        maxx += t.x;
        miny += t.y;
        maxy += t.y;
        cellquality.translate(t);
        translation.translate(t.patternCentroid());
    }

    public Vector getCorrespondingMapPoint(Coordinate c) {
        Vector point = c.toVector();
        point.untranslate(translation);
        point.scale(1.0 / factor);
        return point;
    }
    
    public Transform mapToGridTransform() {
        return new Transform(translation, factor);
    }

    public CoordinateMap<Double> getCellquality() {
        return cellquality;
    }

    public double getFactor() {
        return factor;
    }

    public Vector getTranslation() {
        return translation;
    }

    public boolean intersectsGuide(GuidingShape other) {
        int left_is = Math.max(minx, other.minx);
        int right_is = Math.min(maxx, other.maxx);
        if (right_is < left_is) {
            return false;
        }

        int bottom_is = Math.max(miny, other.miny);
        int top_is = Math.min(maxy, other.maxy);
        if (top_is < bottom_is) {
            return false;
        }

        int is_overlap = (top_is - bottom_is + 1) * (right_is - left_is + 1);
        int occ_size = Math.min(size(), other.size());
        if (is_overlap < 2 * occ_size) {
            Coordinate origin = getGridmath().origin();
            for (int x = left_is; x <= right_is; x++) {
                for (int y = bottom_is; y <= top_is; y++) {
                    Coordinate c = origin.plus(x, y);
                    if (contains(c) && other.contains(c)) {
                        return true;
                    }
                }
            }
            return false;
        } else {
            return intersects(other);
        }
    }

    public List<Coordinate> intersectionWithGuide(GuidingShape other) {
        int left_is = Math.max(minx, other.minx);
        int right_is = Math.min(maxx, other.maxx);
        if (right_is < left_is) {
            return new ArrayList();
        }

        int bottom_is = Math.max(miny, other.miny);
        int top_is = Math.min(maxy, other.maxy);
        if (top_is < bottom_is) {
            return new ArrayList();
        }

        int is_overlap = (top_is - bottom_is + 1) * (right_is - left_is + 1);
        int occ_size = Math.min(size(), other.size());
        if (is_overlap < 2 * occ_size) {
            Coordinate origin = getGridmath().origin();
            List<Coordinate> result = new ArrayList();
            for (int x = left_is; x <= right_is; x++) {
                for (int y = bottom_is; y <= top_is; y++) {
                    Coordinate c = origin.plus(x, y);
                    if (contains(c) && other.contains(c)) {
                        result.add(c);
                    }
                }
            }
            return result;
        } else {
            return intersection(other);
        }
    }

    public int distanceTo(GuidingShape other) {
        // NB: only used between adjacent regions
        
        // distance 0: do they intersect?
        if (intersectsGuide(other)) {
            return 0;
        }

        // distance 1: do they touch, i.e., is there a neighbor from one the other set
        for (Coordinate nbr : this.neighbors(MosaicConstants.CONN_BETWEEN_REGIONS)) {
            if (other.contains(nbr)) {
                return 1;
            }
        }

        // distance 2+: must be realized between two neighbors of the set
        int min = Integer.MAX_VALUE;
        for (Coordinate nbr : this.neighbors(MosaicConstants.CONN_BETWEEN_REGIONS)) {
            for (Coordinate nbr2 : other.neighbors(MosaicConstants.CONN_BETWEEN_REGIONS)) {
                int d = nbr.norm(nbr2) + 2;
                if (d == 2) {
                    return 2;
                } else if (min > d) {
                    min = d;
                }
            }
        }
        return min;
    }
}
