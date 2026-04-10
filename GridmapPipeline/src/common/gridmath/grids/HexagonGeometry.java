package common.gridmath.grids;

import common.gridmath.GridGeometry;
import common.gridmath.GridMath;
import common.gridmath.GridMath.Coordinate;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Polygon;

/**
 *
 * @author Wouter Meulemans
 */
public class HexagonGeometry extends GridGeometry {

    private static final Polygon hex = new Polygon();

    static {
        for (int i = 0; i < 6; i++) {
            double x = Math.cos(i * Math.PI / 3 - Math.PI / 6);
            double y = Math.sin(i * Math.PI / 3 - Math.PI / 6);
            hex.addVertex(new Vector(x, y));
        }
    }

    public HexagonGeometry() {
        super("HEXAGON", Vector.right(TWO_APOTHEM), new Vector(-APOTHEM, THREE_HALF_SIDE), hex.clone());
    }

    @Override
    public int norm(GridMath.Coordinate a, GridMath.Coordinate b) {
        final int dx = b.x - a.x;
        final int dy = b.y - a.y;

        if (dx <= dy) {
            if (dy <= 0) {
                // x <= y <= 0
                return -dx;
            } else if (dx <= 0) {
                // x <= 0 < y
                return dy - dx;
            } else {
                // 0 < x <= y
                return dy;
            }
        } else if (0 <= dy) {
            // 0 <= y < x
            return dx;
        } else if (0 <= dx) {
            // y < 0 <= x
            return dx - dy;
        } else {
            // y < x < 0
            return -dy;
        }
    }

    private static final double SIDE = 1.0;
    private static final double HALF_SIDE = 0.5;
    private static final double THREE_HALF_SIDE = 1.5;
    private static final double APOTHEM = Math.sqrt(3) / 2;
    private static final double TWO_APOTHEM = Math.sqrt(3);
    private static final double TAN30 =1.0 /Math.sqrt(3) ;// Math.sqrt(3) / 3;

    @Override
    public Coordinate getContainingCell(Vector point, Coordinate origin) {
        Vector ip = inverseTransformed(point);

        double px = ip.getX();
        double py = ip.getY();

        int y = (int) (Math.floor((py + HALF_SIDE) / THREE_HALF_SIDE));
        double horizontalDecrease = y * APOTHEM;
        int x = (int) (Math.floor((px + horizontalDecrease + APOTHEM) / TWO_APOTHEM));
        double boxTopX = -APOTHEM * y + TWO_APOTHEM * x;
        double boxTopY = THREE_HALF_SIDE * y + SIDE;
        double pointBelowTop = boxTopY - py;
        double boundaryBelowTop = TAN30 * (px - boxTopX);
        if (px > boxTopX && pointBelowTop < boundaryBelowTop) {
            x++;
            y++;
        } else if (px < boxTopX && pointBelowTop < -boundaryBelowTop) {
            y++;
        }

        return origin.plus(x, y);
    }
    
    @Override
    public int getSelfRefinableFactor() {
        return 4;
    }
    
    @Override
    public Coordinate[] getRefinementFor(Coordinate c, Coordinate origin) {
        return new Coordinate[]{
            origin.plus(2 * c.x, 2 * c.y),
            origin.plus(2 * c.x, 2 * c.y + 1),
            origin.plus(2 * c.x + 1, 2 * c.y + 1),
            origin.plus(2 * c.x + 1, 2 * c.y + 2)
        };
    }
}
