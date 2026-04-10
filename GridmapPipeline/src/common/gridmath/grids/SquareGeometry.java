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
public class SquareGeometry extends GridGeometry {

    private static final Polygon square = new Polygon(
            new Vector(0.5, -0.5),
            new Vector(0.5, 0.5),
            new Vector(-0.5, 0.5),
            new Vector(-0.5, -0.5)
    );

    public SquareGeometry() {
        super("SQUARE", Vector.right(), Vector.up(), square.clone());
    }

    @Override
    public int norm(GridMath.Coordinate a, GridMath.Coordinate b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    @Override
    public Coordinate getContainingCell(Vector point, Coordinate origin) {
        Vector ip = inverseTransformed(point);
        int x = (int) Math.round(ip.getX());
        int y = (int) Math.round(ip.getY());
        return origin.plus(x, y);
    }

    @Override
    public Coordinate[] getUnitVectors(Coordinate origin) {
        return new Coordinate[]{
            origin.plus(1, 0),
            origin.plus(0, 1),
            origin.plus(-1, 0),
            origin.plus(0, -1)
        };
    }

    @Override
    public int getSelfRefinableFactor() {
        return 4;
    }

    @Override
    public Coordinate[] getRefinementFor(Coordinate c, Coordinate origin) {
        return new Coordinate[]{
            origin.plus(2 * c.x, 2 * c.y),
            origin.plus(2 * c.x + 1, 2 * c.y),
            origin.plus(2 * c.x + 1, 2 * c.y + 1),
            origin.plus(2 * c.x, 2 * c.y + 1)
        };
    }
}
