package common.gridmath.grids;

import common.gridmath.GridGeometry;
import common.gridmath.GridMath.Coordinate;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Polygon;

/**
 *
 * @author Wouter Meulemans
 */
public class StaggeredSquareGeometry extends GridGeometry {
    private static final Polygon square = new Polygon(
            new Vector(0.5, -0.5),
            new Vector(0.5, 0.5),
            new Vector(-0.5, 0.5),
            new Vector(-0.5, -0.5)
    );
    
    public StaggeredSquareGeometry() {
        super("STAGSQUARE", Vector.right(), new Vector(-0.5,1), square.clone());
    }
    
    @Override
    public int norm(Coordinate a, Coordinate b) {
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
    
    
    @Override
    public Coordinate getContainingCell(Vector point, Coordinate origin) {
        Vector ip = inverseTransformed(point);
        int y = (int) Math.round(ip.getY());
        int x = (int) Math.round(ip.getX() + y * 0.5);
        return origin.plus(x, y);
    }
}
