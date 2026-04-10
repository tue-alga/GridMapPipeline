package arrange.model;

import arrange.model.Cartogram.MosaicCartogram;
import java.awt.geom.Point2D;
import arrange.model.Network.Vertex;
import arrange.model.subdivision.Map;
import arrange.model.util.Vector2D;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public final class SquareMap extends MosaicCartogram {

    private static final double SIDE = 1.0;
    private static final double APOTHEM = 0.5;

    public SquareMap() {
    }

    public SquareMap(Map map, Network dual) {
        super(map, dual);
    }

    public SquareMap(SquareMap other) {
        super(other);
    }

    @Override
    public double getCellArea() {
        return SIDE * SIDE;
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
        return SIDE;
    }

    @Override
    public double getCellSamplingHeight() {
        return SIDE;
    }

    @Override
    public Point2D[] getDefaultCellBoundaryPoints() {
        Point2D[] points = new Point2D[4];
        points[0] = new Point2D.Double(APOTHEM, -APOTHEM);
        points[1] = new Point2D.Double(APOTHEM, APOTHEM);
        points[2] = new Point2D.Double(-APOTHEM, APOTHEM);
        points[3] = new Point2D.Double(-APOTHEM, -APOTHEM);
        return points;
    }

    @Override
    public EuclideanCoordinate getContainingCell(double px, double py) {
        int x = (int) Math.round(px);
        int y = (int) Math.round(py);
        return new EuclideanCoordinate(x, y);
    }

    @Override
    public EuclideanCoordinate[] getCoordinateArray() {
        EuclideanCoordinate[] occupied = new EuclideanCoordinate[numberOfCells()];
        int i = 0;
        for (Coordinate c : coordinates()) {
            occupied[i++] = (EuclideanCoordinate) c;
        }
        return occupied;
    }

    @Override
    public EuclideanCoordinate zeroVector() {
        return new EuclideanCoordinate(0, 0);
    }

    @Override
    public EuclideanCoordinate[] unitVectors() {
        EuclideanCoordinate[] vectors = new EuclideanCoordinate[4];
        vectors[0] = new EuclideanCoordinate(1, 0);
        vectors[1] = new EuclideanCoordinate(0, 1);
        vectors[2] = new EuclideanCoordinate(-1, 0);
        vectors[3] = new EuclideanCoordinate(0, -1);
        return vectors;
    }

    @Override
    public EuclideanCoordinate parseCoordinate(int[] values) {
        if (values.length == 2) {
            return new EuclideanCoordinate(values[0], values[1]);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public SquareMap duplicate() {
        return new SquareMap(this);
    }

    @Override
    protected Square createCell(Coordinate c, Vertex v) {
        if (c instanceof EuclideanCoordinate) {
            return new Square((EuclideanCoordinate) c, v);
        } else {
            throw new IllegalArgumentException();








        }
    }

    public final class Square extends Cell {

        private final EuclideanCoordinate coordinate;

        private Square(EuclideanCoordinate coordinate, Vertex v) {
            this.coordinate = coordinate;
            this.v = v;
        }

        private Square(Square other) {
            this(other.coordinate, other.v);
        }

        @Override
        public EuclideanCoordinate getCoordinate() {
            return coordinate;
        }

        @Override
        public Point2D[] getBoundaryPoints() {
            Point2D[] points = new Point2D[4];
            int x = coordinate.getX();
            int y = coordinate.getY();
            points[0] = new Point2D.Double(x + APOTHEM, y - APOTHEM);
            points[1] = new Point2D.Double(x + APOTHEM, y + APOTHEM);
            points[2] = new Point2D.Double(x - APOTHEM, y + APOTHEM);
            points[3] = new Point2D.Double(x - APOTHEM, y - APOTHEM);
            return points;
        }
    }

    public static final class EuclideanCoordinate extends Coordinate {

        private final int x;
        private final int y;

        public EuclideanCoordinate(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        @Override
        public int[] getComponents() {
            return new int[]{x, y};
        }

        @Override
        public EuclideanCoordinate plus(Coordinate c) {
            if (c instanceof EuclideanCoordinate) {
                EuclideanCoordinate ec = (EuclideanCoordinate) c;
                return new EuclideanCoordinate(x + ec.x, y + ec.y);
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public EuclideanCoordinate minus(Coordinate c) {
            if (c instanceof EuclideanCoordinate) {
                EuclideanCoordinate ec = (EuclideanCoordinate) c;
                return new EuclideanCoordinate(x - ec.x, y - ec.y);
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public EuclideanCoordinate times(int k) {
            return new EuclideanCoordinate(k * x, k * y);
        }

        @Override
        public EuclideanCoordinate times(double k) {
            return new EuclideanCoordinate((int) Math.round(x * k), (int) Math.round(y * k));
        }

        @Override
        public EuclideanCoordinate normalize() {
            return this;
        }

        @Override
        public int norm() {
            return Math.abs(x) + Math.abs(y);
        }

        @Override
        public int dotProduct(Coordinate c) {
            if (c instanceof EuclideanCoordinate) {
                EuclideanCoordinate ec = (EuclideanCoordinate) c;
                return x * ec.x + y * ec.y;
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public EuclideanCoordinate[] neighbours() {
            EuclideanCoordinate[] neighbours = new EuclideanCoordinate[4];
            neighbours[0] = new EuclideanCoordinate(x + 1, y);
            neighbours[1] = new EuclideanCoordinate(x, y + 1);
            neighbours[2] = new EuclideanCoordinate(x - 1, y);
            neighbours[3] = new EuclideanCoordinate(x, y - 1);
            return neighbours;
        }

        @Override
        public EuclideanCoordinate[] connectedVicinity() {
            EuclideanCoordinate[] vicinity = new EuclideanCoordinate[8];
            vicinity[0] = new EuclideanCoordinate(x + 1, y);
            vicinity[1] = new EuclideanCoordinate(x + 1, y + 1);
            vicinity[2] = new EuclideanCoordinate(x, y + 1);
            vicinity[3] = new EuclideanCoordinate(x - 1, y + 1);
            vicinity[4] = new EuclideanCoordinate(x - 1, y);
            vicinity[5] = new EuclideanCoordinate(x - 1, y - 1);
            vicinity[6] = new EuclideanCoordinate(x, y - 1);
            vicinity[7] = new EuclideanCoordinate(x + 1, y - 1);
            return vicinity;
        }

        @Override
        public EuclideanCoordinate[] ring(int radius) {
            EuclideanCoordinate[] ring = new EuclideanCoordinate[4 * radius];
            int lastX = x + radius;
            int lastY = y;
            int pos = 0;
            for (int i = 0; i < radius; i++) {
                ring[pos++] = new EuclideanCoordinate(lastX--, lastY++);
            }
            for (int i = 0; i < radius; i++) {
                ring[pos++] = new EuclideanCoordinate(lastX--, lastY--);
            }
            for (int i = 0; i < radius; i++) {
                ring[pos++] = new EuclideanCoordinate(lastX++, lastY--);
            }
            for (int i = 0; i < radius; i++) {
                ring[pos++] = new EuclideanCoordinate(lastX++, lastY++);
            }
            return ring;
        }

        @Override
        public EuclideanCoordinate[] disk(int radius) {
            EuclideanCoordinate[] disk = new EuclideanCoordinate[1 + 2 * radius * (radius + 1)];
            disk[0] = this;
            int pos = 1;
            for (int i = 1; i <= radius; i++) {
                EuclideanCoordinate[] ring = ring(i);
                for (EuclideanCoordinate c : ring) {
                    disk[pos++] = c;
                }
            }
            return disk;
        }

        @Override
        public Vector2D toVector2D() {
            return new Vector2D(SIDE * x, SIDE * y);
        }

        @Override
        public Point2D toPoint2D() {
            return new Point2D.Double(SIDE * x, SIDE * y);
        }

        @Override
        public int hashCode() {
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
            EuclideanCoordinate other = (EuclideanCoordinate) obj;
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
            return "(" + x + ", " + y + ")";
        }
    }
}
