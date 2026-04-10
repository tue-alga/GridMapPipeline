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
public class TriangleGeometry extends GridGeometry {

    private static final Polygon lower = new Polygon(
            new Vector(-0.5, -0.5),
            new Vector(0.5, -0.5),
            new Vector(0.5, 0.5)
    );
    private static final Polygon upper = new Polygon(
            new Vector(-0.5, -0.5),
            new Vector(0.5, 0.5),
            new Vector(-0.5, 0.5)
    );

    private TriangleGeometry() {
        super("TRIANGLE", Vector.right(), Vector.up(), new SquareGeometry(), lower.clone(), upper.clone());
    }
    
    public static TriangleGeometry rightAngled() {
        return new TriangleGeometry();
    }

    public static TriangleGeometry equilateral() {
        TriangleGeometry gg = new TriangleGeometry();
        gg.shear(-0.5, 0);
        gg.scale(1, Math.sqrt(3) / 2);
        gg.setName("EQUILATERAL");
        return gg;
    }

    @Override
    public int norm(GridMath.Coordinate a, GridMath.Coordinate b) {
        final int px = (b.x - a.x) / 2;
        final int py = (b.y - a.y);

        int pnorm;
        if (px <= py) {
            if (py <= 0) {
                // x <= y <= 0
                pnorm = -px;
            } else if (px <= 0) {
                // x <= 0 < y
                pnorm = py - px;
            } else {
                // 0 < x <= y
                pnorm = py;
            }
        } else if (0 <= py) {
            // 0 <= y < x
            pnorm = px;
        } else if (0 <= px) {
            // y < 0 <= x
            pnorm = px - py;
        } else {
            // y < x < 0
            pnorm = -py;
        }

        int vnorm; // TODO: this isnt quite right yet
        if (a.variant() == b.variant()) {
            vnorm = 0;
        } else if (a.variant() == 0) {
            vnorm = 1;
        } else {
            vnorm = 1;
        }

        return 2 * pnorm + vnorm;
    }

    @Override
    public Coordinate getContainingCell(Vector point, Coordinate origin) {
        Vector ip = inverseTransformed(point);
        int x = (int) Math.round(ip.getX());
        int y = (int) Math.round(ip.getY());
        int var = (ip.getX() - x >= ip.getY() - y) ? 0 : 1;
        return origin.plus(2 * x + var, y);
    }

}
