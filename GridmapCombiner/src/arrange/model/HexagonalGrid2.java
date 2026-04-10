package arrange.model;

/**
 * Different implementation of the HexagonGrid class. Might be useful if the
 * hash function of the HashMap does not spread the entries well. I'll leave it
 * here and see what I'll do with it later.
 */

//package model;
//
//import java.awt.geom.Path2D;
//import java.awt.geom.Point2D;
//import java.util.ArrayList;
//import java.util.Iterator;
//import model.Network.Vertex;
//
///**
// * This class represents an infinite grid of hexagons.
// *
// * Barycentric coordinates -----------------------
// *
// * A HexagonGrid uses so-called <i>barycentric coordinates</i> for the hexagons.
// * A barycentric coordinate is of the form (x, y, z), where x, y and z are
// * integers. A step towards the right is represented by increasing x; a step to
// * the bottom-left is represented by increasing y; a step to the top-left is
// * represented by increasing z.
// *
// * Any hexagon is represented by an infinite number of barycentric coordinates.
// * For example, since first moving to the right, then to the top-left, then to
// * the bottom-left gets us back to the original hexagon, (0, 0, 0) represents
// * the same hexagon as (1, 1, 1). In general, (x, y, z) and (x + a, y + a, z +
// * a) represent the same hexagons.
// *
// * Barycentric coordinates of the form (x, y, 0) are called <i>normalized</i>.
// * We can normalize a barycentric coordinate (x, y, z) by transforming it to the
// * equivalent coordinate (x - z, y - z, 0).
// *
// * The advantage of barycentric coordinates is that we can easily move into any
// * direction from a given coordinate.
// *
// * Representation --------------
// *
// * Empty (non-existing) hexagons are indicated as
// * <code>null</code>. Therefore it is not possible to store white hexagons.
// */
//public final class HexagonGrid implements Iterable<HexagonGrid.Hexagon> {
//
//    private static final int INITIAL_GRID_CAPACITY = 128;
//    private static final double TAN30 = Math.sqrt(3) / 3;
//    private double side = 1.0;
//    private double halfSide = 0.5;
//    private double threeHalfSide = 1.5;
//    private double apothem = Math.sqrt(3) / 2;
//    private double twoApothem = Math.sqrt(3);
//    private int numHexagons = 0;
//    private Hexagon listHead = new Hexagon();
//    private Hexagon listTail = listHead;
//    private GridMatrix matrix = new GridMatrix(INITIAL_GRID_CAPACITY);
//    /**
//     * Standard hexagon shape to be used in other methods.
//     */
//    private static final Point2D[] standardBoundary = new Point2D.Double[6];
//
//    static {
//        for (int i = 0; i < 6; i++) {
//            double x = Math.cos(i * Math.PI / 3 + Math.PI / 6);
//            double y = Math.sin(i * Math.PI / 3 + Math.PI / 6);
//            standardBoundary[i] = new Point2D.Double(x, y);
//        }
//    }
//
//    public HexagonGrid() {
//    }
//
//    public HexagonGrid(double side) {
//        setSide(side);
//    }
//
//    public HexagonGrid(HexagonGrid other) {
//        this.side = other.side;
//        this.halfSide = other.halfSide;
//        this.threeHalfSide = other.threeHalfSide;
//        this.apothem = other.apothem;
//        this.twoApothem = other.twoApothem;
//        this.numHexagons = other.numHexagons;
//        this.listHead = new Hexagon();
//        this.listTail = listHead;
//        for (Hexagon h : other) {
//            BaryCoordinate c = BaryCoordinate.normalize(h.getPosition());
//            Hexagon newHexagon = new Hexagon(h);
//            this.matrix.set(c.getX(), c.getY(), newHexagon);
//            listTail.next = newHexagon;
//            newHexagon.previous = listTail;
//            listTail = newHexagon;
//        }
//        listTail.next = null;
//    }
//
//    public double getSide() {
//        return side;
//    }
//
//    public void setSide(double side) {
//        this.side = side;
//        this.halfSide = side / 2;
//        this.threeHalfSide = 3 * this.halfSide;
//        this.twoApothem = side * Math.sqrt(3);
//        this.apothem = twoApothem / 2;
//    }
//
//    public double getHexagonArea() {
//        return 6 * side * side * Math.sqrt(3) / 4;
//    }
//
//    public int numHexagons() {
//        return numHexagons;
//    }
//
//    public BaryCoordinate[] occupiedCoordinates() {
//        BaryCoordinate[] occupied = new BaryCoordinate[numHexagons];
//        int i = 0;
//        for (Hexagon h : this) {
//            occupied[i++] = h.getPosition();
//        }
//        return occupied;
//    }
//
//    /**
//     * Returns the normalized barycentric coordinates of the hexagon that
//     * contains the point given by the specified euclidian coordinates.
//     *
//     * Algorithm based on http://stackoverflow.com/questions/7705228/
//     */
//    public BaryCoordinate getContainingHexagon(double abscissa, double ordinate) {
//        int y = -(int) (Math.floor((ordinate + halfSide) / threeHalfSide));
//        double horizontalDecrease = y * apothem;
//        int x = (int) (Math.floor((abscissa + horizontalDecrease + apothem) / twoApothem));
//        double boxTopX = -apothem * y + twoApothem * x;
//        double boxTopY = -threeHalfSide * y + side;
//        double lhs = ordinate - boxTopY;
//        double rhs = TAN30 * (abscissa - boxTopX);
//        if (lhs > rhs) {
//            x--;
//            y--;
//        } else if (lhs > -rhs) {
//            y--;
//        }
//        return new BaryCoordinate(x, y, 0);
//    }
//
//    public void setVertex(BaryCoordinate c, Vertex v) {
//        BaryCoordinate normalized = BaryCoordinate.normalize(c);
//        int x = normalized.getX();
//        int y = normalized.getY();
//        Hexagon hexagon = matrix.get(x, y);
//        if (hexagon == null) {
//            Hexagon h = new Hexagon(c, v);
//            matrix.set(x, y, h);
//            numHexagons++;
//            h.previous = listTail;
//            h.next = null;
//            listTail.next = h;
//            listTail = h;
//        } else {
//            hexagon.setVertex(v);
//        }
//    }
//
//    public Vertex getVertex(BaryCoordinate c) {
//        BaryCoordinate normalized = BaryCoordinate.normalize(c);
//        Hexagon h = matrix.get(normalized.getX(), normalized.getY());
//        if (h == null) {
//            return null;
//        } else {
//            return h.getVertex();
//        }
//    }
//
//    public void removeHexagon(BaryCoordinate c) {
//        BaryCoordinate normalized = BaryCoordinate.normalize(c);
//        Hexagon h = matrix.get(normalized.getX(), normalized.getY());
//        if (h != null) {
//            matrix.set(normalized.getX(), normalized.getY(), null);
//            numHexagons--;
//            Hexagon previous = h.previous;
//            Hexagon next = h.next;
//            previous.next = next;
//            if (next != null) {
//                next.previous = previous;
//            } else {
//                listTail = previous;
//            }
//        }
//    }
//
//    public Hexagon getHexagon(BaryCoordinate c) {
//        BaryCoordinate normalized = BaryCoordinate.normalize(c);
//        return matrix.get(normalized.getX(), normalized.getY());
//    }
//
//    public Path2D getHexagonShape(BaryCoordinate c) {
//        Path2D path = new Path2D.Double();
//        Point2D[] points = getBoundaryPoints(c);
//        path.moveTo(points[0].getX(), points[0].getY());
//        for (int i = 1; i < 6; i++) {
//            path.lineTo(points[i].getX(), points[i].getY());
//        }
//        path.closePath();
//        return path;
//    }
//
//    public Point2D[] getBoundaryPoints(BaryCoordinate c) {
//        Point2D center = c.toPoint2D(side);
//        Point2D[] points = new Point2D.Double[6];
//        for (int i = 0; i < 6; i++) {
//            points[i] = new Point2D.Double();
//            double x = side * standardBoundary[i].getX() + center.getX();
//            double y = side * standardBoundary[i].getY() + center.getY();
//            points[i].setLocation(x, y);
//        }
//        return points;
//    }
//
//    /**
//     * Returns the next hexagon following the given direction. The parameter
//     * must be a unit vector. Returns null if no Hexagon exists in the given
//     * direction.
//     *
//     * TODO: make this less disgusting.
//     */
////    public Hexagon nextInDirection(Hexagon c, BaryCoordinate direction) {
////        BaryCoordinate current = new BaryCoordinate(c.getPosition());
////        for (int i = 0; i < 1000; i++) { // 1000 should be large enough, right?
////            current.add(direction);
////            Hexagon next = hexagons.get(current);
////            if (next != null) {
////                return next;
////            }
////        }
////        return null;
////    }
//    /**
//     * Resets all hexagons to white.
//     */
//    public void clear() {
//        matrix = new GridMatrix(INITIAL_GRID_CAPACITY);
//        numHexagons = 0;
//        listHead = new Hexagon();
//        listTail = listHead;
//    }
//
//    @Override
//    public Iterator<Hexagon> iterator() {
//        return new GridIterator(listHead);
//    }
//
//    /**
//     * Returns the boundary of a hexagon centered at (0, 0).
//     *
//     * @return The boundary, as an array of points.
//     */
//    public static Point2D[] getStandardBoundaryPoints() {
//        return standardBoundary;
//    }
//
//    /**
//     * A Hexagon represents a regular polygon with 6 corners in 2D.
//     *
//     * @author Thom Castermans
//     */
//    public final class Hexagon {
//
//        private Hexagon previous = null;
//        private Hexagon next = null;
//        private BaryCoordinate position; // Coordinate in the hexagon grid.
//        private Vertex v;                // Associated vertex
//
//        private Hexagon() {
//            this.previous = null;
//            this.next = null;
//            this.position = null;
//            this.v = null;
//        }
//
//        public Hexagon(BaryCoordinate position) {
//            this(position, null);
//        }
//
//        public Hexagon(BaryCoordinate position, Vertex v) {
//            this.position = position;
//            this.v = v;
//        }
//
//        public Hexagon(Hexagon other) {
//            this.position = other.position;
//            this.v = other.v;
//        }
//
//        public BaryCoordinate getPosition() {
//            return position;
//        }
//
//        public Vertex getVertex() {
//            return v;
//        }
//
//        public void setVertex(Vertex v) {
//            this.v = v;
//        }
//
//        public Point2D[] getBoundaryPoints() {
//            return HexagonGrid.this.getBoundaryPoints(position);
//        }
//
//        public Point2D getCenter() {
//            return position.toPoint2D(side);
//        }
//    }
//
//    private static class DoubleEndedArrayList<E> {
//
//        private ArrayList<E> negative;
//        private ArrayList<E> nonNegative;
//
//        public DoubleEndedArrayList() {
//            negative = new ArrayList<>();
//            negative.add(null);
//            nonNegative = new ArrayList<>();
//        }
//
//        public DoubleEndedArrayList(int initialCapacity) {
//            negative = new ArrayList<>(initialCapacity);
//            negative.add(null);
//            nonNegative = new ArrayList<>(initialCapacity);
//        }
//
//        public DoubleEndedArrayList(int initialCapacity, E e) {
//            if (initialCapacity < 1) {
//                negative = new ArrayList<>(initialCapacity);
//                negative.add(null);
//                nonNegative = new ArrayList<>(initialCapacity);
//            } else {
//                negative = new ArrayList<>(initialCapacity);
//                negative.add(null);
//                nonNegative = new ArrayList<>(initialCapacity);
//                nonNegative.add(e);
//                for (int i = 1; i < initialCapacity; i++) {
//                    negative.add(e);
//                    nonNegative.add(e);
//                }
//            }
//        }
//
//        public int highestIndex() {
//            return nonNegative.size() - 1;
//        }
//
//        public int lowestIndex() {
//            return 1 - negative.size();
//        }
//
//        public E get(int index) {
//            if (index >= 0) {
//                return nonNegative.get(index);
//            } else {
//                return negative.get(-index);
//            }
//        }
//
//        public E set(int index, E element) {
//            if (index >= 0) {
//                return nonNegative.set(index, element);
//            } else {
//                return negative.set(-index, element);
//            }
//        }
//
//        public boolean addLeft(E e) {
//            return negative.add(e);
//        }
//
//        public boolean addRight(E e) {
//            return nonNegative.add(e);
//        }
//    }
//
//    private static class GridMatrix {
//
//        private DoubleEndedArrayList<DoubleEndedArrayList<Hexagon>> matrix;
//
//        public GridMatrix(int initialCapacity) {
//            matrix = new DoubleEndedArrayList<>(initialCapacity, null);
//            for (int i = -initialCapacity + 1; i < initialCapacity; i++) {
//                matrix.set(i, new DoubleEndedArrayList<Hexagon>(initialCapacity, null));
//            }
//        }
//
//        public Hexagon get(int x, int y) {
//            if (x < matrix.lowestIndex() || x > matrix.highestIndex()) {
//                return null;
//            } else {
//                DoubleEndedArrayList<Hexagon> column = matrix.get(x);
//                if (y < column.lowestIndex() || y > column.highestIndex()) {
//                    return null;
//                } else {
//                    return column.get(y);
//                }
//            }
//        }
//
//        public Hexagon set(int x, int y, Hexagon h) {
//            if (x < matrix.lowestIndex()) {
//                do {
//                    matrix.addLeft(new DoubleEndedArrayList<Hexagon>());
//                } while (x < matrix.lowestIndex());
//            } else if (x > matrix.highestIndex()) {
//                do {
//                    matrix.addRight(new DoubleEndedArrayList<Hexagon>());
//                } while (x > matrix.highestIndex());
//            }
//            DoubleEndedArrayList<Hexagon> column = matrix.get(x);
//            if (y < column.lowestIndex()) {
//                do {
//                    column.addLeft(null);
//                } while (y < column.lowestIndex());
//            } else if (y > column.highestIndex()) {
//                do {
//                    column.addRight(null);
//                } while (y > column.highestIndex());
//            }
//            return column.set(y, h);
//        }
//    }
//
//    public static class GridIterator implements Iterator<Hexagon> {
//
//        private Hexagon current;
//
//        public GridIterator(Hexagon first) {
//            this.current = first;
//        }
//
//        @Override
//        public boolean hasNext() {
//            return current.next != null;
//        }
//
//        @Override
//        public Hexagon next() {
//            current = current.next;
//            return current;
//        }
//
//        @Override
//        public void remove() {
//            throw new UnsupportedOperationException("Not supported yet.");
//        }
//    }
//}
