package arrange.model;

import arrange.model.Cartogram.MosaicCartogram;
import java.awt.geom.Point2D;
import arrange.model.Network.Vertex;
import arrange.model.subdivision.Map;
import arrange.model.util.Vector2D;

/**
 * This class represents an infinite grid of hexagons.
 *
 * Barycentric getCoordinateArray -----------------------
 *
 * A HexagonalMap uses so-called <i>barycentric getCoordinateArray</i> for the
 * hexagons. A barycentric coordinate is of the form (x, y, z), where x, y and z
 * are integers. A step towards the right is represented by increasing x; a step
 * to the bottom-left is represented by increasing y; a step to the top-left is
 * represented by increasing z.
 *
 * Any hexagon is represented by an infinite number of barycentric
 * getCoordinateArray. For example, since first moving to the right, then to the
 * top-left, then to the bottom-left gets us back to the original hexagon, (0,
 * 0, 0) represents the same hexagon as (1, 1, 1). In general, (x, y, z) and (x
 * + a, y + a, z + a) represent the same hexagons.
 *
 * Barycentric getCoordinateArray of the form (x, y, 0) are called
 * <i>normalized</i>. We can normalize a barycentric coordinate (x, y, z) by
 * transforming it to the equivalent coordinate (x - z, y - z, 0).
 *
 * The advantage of barycentric getCoordinateArray is that we can easily move
 * into any direction from a given coordinate.
 *
 * Representation --------------
 *
 * Empty (non-existing) hexagons are indicated as
 * <code>null</code>. Therefore it is not possible to store white hexagons.
 */
public final class HexagonalMap extends MosaicCartogram {

    private static final double SIDE = 1.0;
    private static final double HALF_SIDE = 0.5;
    private static final double THREE_HALF_SIDE = 1.5;
    private static final double APOTHEM = Math.sqrt(3) / 2;
    private static final double TWO_APOTHEM = Math.sqrt(3);
    private static final double TAN30 = Math.sqrt(3) / 3;
    /**
     * Standard hexagon shape to be used in other methods.
     */
    public static final Point2D[] standardBoundary = new Point2D.Double[6];

    static {
        // Warning: do not change the order in which the points are created!
        for (int i = 0; i < 6; i++) {
            double x = Math.cos(i * Math.PI / 3 - Math.PI / 6);
            double y = Math.sin(i * Math.PI / 3 - Math.PI / 6);
            standardBoundary[i] = new Point2D.Double(x, y);
        }
    }

    public HexagonalMap() {
    }

    public HexagonalMap(Map map, Network dual) {
        super(map, dual);
    }

    public HexagonalMap(HexagonalMap other) {
        super(other);
    }

    @Override
    public double getCellArea() {
        return 6 * SIDE * SIDE * Math.sqrt(3) / 4;
    }

    @Override
    public double getCellSide() {
        return SIDE;
    }

    @Override
    public double getCellApothem() {
        return APOTHEM;
    }

    @Override
    public double getCellSamplingWidth() {
        return TWO_APOTHEM;
    }

    @Override
    public double getCellSamplingHeight() {
        return THREE_HALF_SIDE;
    }

    @Override
    public Point2D[] getDefaultCellBoundaryPoints() {
        Point2D[] points = new Point2D.Double[6];
        for (int i = 0; i < 6; i++) {
            points[i] = new Point2D.Double();
            double x = SIDE * standardBoundary[i].getX();
            double y = SIDE * standardBoundary[i].getY();
            points[i].setLocation(x, y);
        }
        return points;
    }

    /**
     * Returns the normalized barycentric getCoordinateArray of the hexagon that
     * contains the point given by the specified euclidian getCoordinateArray.
     *
     * Algorithm based on http://stackoverflow.com/questions/7705228/
     */
    @Override
    public BarycentricCoordinate getContainingCell(double px, double py) {
        int y = -(int) (Math.floor((py + HALF_SIDE) / THREE_HALF_SIDE));
        double horizontalDecrease = y * APOTHEM;
        int x = (int) (Math.floor((px + horizontalDecrease + APOTHEM) / TWO_APOTHEM));
        double boxTopX = -APOTHEM * y + TWO_APOTHEM * x;
        double boxTopY = -THREE_HALF_SIDE * y + SIDE;
        double lhs = py - boxTopY;
        double rhs = TAN30 * (px - boxTopX);
        if (lhs > rhs) {
            x--;
            y--;
        } else if (lhs > -rhs) {
            y--;
        }
        return new BarycentricCoordinate(x, y, 0);
    }

    @Override
    public BarycentricCoordinate[] getCoordinateArray() {
        BarycentricCoordinate[] occupied = new BarycentricCoordinate[numberOfCells()];
        int i = 0;
        for (Coordinate c : coordinates()) {
            occupied[i++] = (BarycentricCoordinate) c;
        }
        return occupied;
    }

    @Override
    public BarycentricCoordinate zeroVector() {
        return new BarycentricCoordinate(0, 0, 0);
    }

    @Override
    public BarycentricCoordinate[] unitVectors() {
        BarycentricCoordinate[] vector = new BarycentricCoordinate[6];
        vector[0] = new BarycentricCoordinate(1, 0, 0);
        vector[1] = new BarycentricCoordinate(0, -1, 0);
        vector[2] = new BarycentricCoordinate(0, 0, 1);
        vector[3] = new BarycentricCoordinate(-1, 0, 0);
        vector[4] = new BarycentricCoordinate(0, 1, 0);
        vector[5] = new BarycentricCoordinate(0, 0, -1);
        return vector;
    }

    @Override
    public BarycentricCoordinate parseCoordinate(int[] values) {
        if (values.length == 3) {
            return new BarycentricCoordinate(values[0], values[1], values[2]);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public HexagonalMap duplicate() {
        return new HexagonalMap(this);
    }

    @Override
    protected Hexagon createCell(Coordinate c, Vertex v) {
        if (c instanceof BarycentricCoordinate) {
            return new Hexagon((BarycentricCoordinate) c, v);
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * A Hexagon represents a regular polygon with 6 corners in 2D.
     *
     * @author Thom Castermans
     */
    public final class Hexagon extends Cell {

        private final BarycentricCoordinate coordinate;

        private Hexagon(BarycentricCoordinate coordinate, Vertex v) {
            this.coordinate = coordinate;
            this.v = v;
        }

        private Hexagon(Hexagon other) {
            this(other.coordinate, other.v);
        }

        @Override
        public BarycentricCoordinate getCoordinate() {
            return coordinate;
        }

        @Override
        public Point2D[] getBoundaryPoints() {
            Point2D center = coordinate.toPoint2D();
            Point2D[] points = new Point2D.Double[6];
            for (int i = 0; i < 6; i++) {
                points[i] = new Point2D.Double();
                double x = SIDE * standardBoundary[i].getX() + center.getX();
                double y = SIDE * standardBoundary[i].getY() + center.getY();
                points[i].setLocation(x, y);
            }
            return points;
        }
    }

    /**
     * A barycentric coordinate implemented as an immutable object.
     *
     * @see HexagonalMap
     */
    public static final class BarycentricCoordinate extends Coordinate {

        private final int x;
        private final int y;
        private final int z;

        public BarycentricCoordinate(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        @Override
        public int[] getComponents() {
            return new int[]{x, y, z};
        }

        @Override
        public BarycentricCoordinate plus(Coordinate c) {
            if (c instanceof BarycentricCoordinate) {
                BarycentricCoordinate bc = (BarycentricCoordinate) c;
                return new BarycentricCoordinate(x + bc.x, y + bc.y, z + bc.z);
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public BarycentricCoordinate minus(Coordinate c) {
            if (c instanceof BarycentricCoordinate) {
                BarycentricCoordinate bc = (BarycentricCoordinate) c;
                return new BarycentricCoordinate(x - bc.x, y - bc.y, z - bc.z);
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public BarycentricCoordinate times(int k) {
            return new BarycentricCoordinate(x * k, y * k, z * k);
        }

        @Override
        public BarycentricCoordinate times(double k) {
            return new BarycentricCoordinate((int) Math.round(x * k), (int) Math.round(y * k), (int) Math.round(z * k));
        }

        @Override
        public BarycentricCoordinate normalize() {
            return new BarycentricCoordinate(x - z, y - z, 0);
        }

        public BarycentricCoordinate minimize() {
            if (x <= y) {
                if (y <= z) {
                    // x <= y <= z
                    return new BarycentricCoordinate(x - y, 0, z - y);
                } else {
                    if (x <= z) {
                        // x <= z < y
                        return new BarycentricCoordinate(x - z, y - z, 0);
                    } else {
                        // z < x <= y
                        return new BarycentricCoordinate(0, y - x, z - x);
                    }
                }
            } else {
                if (z <= y) {
                    // z <= y < x
                    return new BarycentricCoordinate(x - y, 0, z - y);
                } else {
                    if (z <= x) {
                        // y < z <= x
                        return new BarycentricCoordinate(x - z, y - z, 0);
                    } else {
                        // y < x < z
                        return new BarycentricCoordinate(0, y - x, z - x);
                    }
                }
            }
        }

        @Override
        public int norm() {
            if (x <= y) {
                if (y <= z) {
                    // x <= y <= z
                    return z - x;
                } else {
                    if (x <= z) {
                        // x <= z < y
                        return y - x;
                    } else {
                        // z < x <= y
                        return y - z;
                    }
                }
            } else {
                if (z <= y) {
                    // z <= y < x
                    return x - z;
                } else {
                    if (z <= x) {
                        // y < z <= x
                        return x - y;
                    } else {
                        // y < x < z
                        return z - y;
                    }
                }
            }
        }

        @Override
        public int dotProduct(Coordinate c) {
            if (c instanceof BarycentricCoordinate) {
                BarycentricCoordinate bc = (BarycentricCoordinate) c;
                return x * bc.x + y * bc.y + z * bc.z;
            } else {
                throw new IllegalArgumentException();
            }

        }

        @Override
        public BarycentricCoordinate[] neighbours() {
            BarycentricCoordinate[] neighbours = new BarycentricCoordinate[6];
            neighbours[0] = new BarycentricCoordinate(x + 1, y, z);
            neighbours[1] = new BarycentricCoordinate(x, y - 1, z);
            neighbours[2] = new BarycentricCoordinate(x, y, z + 1);
            neighbours[3] = new BarycentricCoordinate(x - 1, y, z);
            neighbours[4] = new BarycentricCoordinate(x, y + 1, z);
            neighbours[5] = new BarycentricCoordinate(x, y, z - 1);
            return neighbours;
        }

        @Override
        public BarycentricCoordinate[] connectedVicinity() {
            return neighbours();
        }

        @Override
        public BarycentricCoordinate[] ring(int radius) {
            BarycentricCoordinate[] ring = new BarycentricCoordinate[6 * radius];
            int lastX = x + radius;
            int lastY = y;
            int lastZ = z;
            int pos = 0;
            for (int i = 0; i < radius; i++) {
                ring[pos++] = new BarycentricCoordinate(lastX--, lastY--, lastZ);
            }
            for (int i = 0; i < radius; i++) {
                ring[pos++] = new BarycentricCoordinate(lastX, lastY++, lastZ++);
            }
            for (int i = 0; i < radius; i++) {
                ring[pos++] = new BarycentricCoordinate(lastX--, lastY, lastZ--);
            }
            for (int i = 0; i < radius; i++) {
                ring[pos++] = new BarycentricCoordinate(lastX++, lastY++, lastZ);
            }
            for (int i = 0; i < radius; i++) {
                ring[pos++] = new BarycentricCoordinate(lastX, lastY--, lastZ--);
            }
            for (int i = 0; i < radius; i++) {
                ring[pos++] = new BarycentricCoordinate(lastX++, lastY, lastZ++);
            }
            return ring;
        }

        @Override
        public BarycentricCoordinate[] disk(int radius) {
            BarycentricCoordinate[] disk = new BarycentricCoordinate[1 + 3 * radius * (radius + 1)];
            disk[0] = this;
            int pos = 1;
            for (int i = 1; i <= radius; i++) {
                BarycentricCoordinate[] ring = ring(i);
                for (BarycentricCoordinate c : ring) {
                    disk[pos++] = c;
                }
            }
            return disk;
        }

        @Override
        public Vector2D toVector2D() {
            return new Vector2D(
                    SIDE * (x - (y + z) / 2.0) * Math.sqrt(3),
                    SIDE * 1.5 * (z - y));
        }

        @Override
        public Point2D.Double toPoint2D() {
            return new Point2D.Double(
                    SIDE * (x - (y + z) / 2.0) * Math.sqrt(3),
                    SIDE * 1.5 * (z - y));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + x - z;
            result = prime * result + y - z;
            return result;
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
            BarycentricCoordinate other = (BarycentricCoordinate) obj;
            if (x - z != other.x - other.z) {
                return false;
            }
            if (y - z != other.y - other.z) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ", " + z + ")";
        }
    }
}
