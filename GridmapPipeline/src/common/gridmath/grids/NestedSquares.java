package common.gridmath.grids;

import common.gridmath.GridGeometry;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Polygon;

/**
 *
 * @author Wouter Meulemans
 */
public class NestedSquares extends GridGeometry {

    public NestedSquares(int k) {
        super("NS" + k, Vector.right(), Vector.up(), new SquareGeometry(), construct(k));
    }

    private static Polygon[] construct(int k) {
        Polygon[] ps = new Polygon[k * k];
        double d = 1.0 / k;
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < k; j++) {
                Polygon p = ps[i + k * j] = new Polygon();
                p.addVertex(new Vector(-0.5 + i * d + d, -0.5 + j * d));
                p.addVertex(new Vector(-0.5 + i * d + d, -0.5 + j * d + d));
                p.addVertex(new Vector(-0.5 + i * d, -0.5 + j * d + d));
                p.addVertex(new Vector(-0.5 + i * d, -0.5 + j * d));
            }
        }
        return ps;
    }

}
