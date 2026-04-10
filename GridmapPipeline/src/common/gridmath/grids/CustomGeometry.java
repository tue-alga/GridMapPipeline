package common.gridmath.grids;

import common.gridmath.GridGeometry;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.geometry.linear.Rectangle;

/**
 *
 * @author Wouter Meulemans
 */
public class CustomGeometry {

    public static GridGeometry escherGeese() {
        Polygon lower = new Polygon(
                new Vector(300, 272),
                new Vector(282, 268),
                new Vector(254, 266),
                new Vector(216, 254),
                new Vector(200, 222),
                new Vector(178, 218),
                new Vector(152, 224),
                new Vector(112, 208),
                new Vector(126, 230),
                new Vector(144, 248),
                new Vector(160, 256),
                new Vector(134, 274),
                new Vector(112, 296),
                new Vector(142, 292),
                new Vector(158, 296),
                new Vector(144, 318),
                new Vector(112, 336),
                new Vector(130, 332),
                new Vector(158, 330),
                new Vector(196, 318),
                new Vector(212, 286),
                new Vector(234, 282),
                new Vector(260, 288)
        );

        Polygon upper = new Polygon(
                new Vector(300, 272),
                new Vector(260, 288),
                new Vector(234, 282),
                new Vector(212, 286),
                new Vector(196, 318),
                new Vector(158, 330),
                new Vector(130, 332),
                new Vector(112, 336),
                new Vector(152, 352),
                new Vector(178, 346),
                new Vector(200, 350),
                new Vector(216, 382),
                new Vector(254, 394),
                new Vector(282, 396),
                new Vector(300, 400),
                new Vector(284, 392),
                new Vector(266, 374),
                new Vector(252, 352),
                new Vector(284, 334),
                new Vector(298, 312),
                new Vector(282, 308),
                new Vector(252, 312),
                new Vector(274, 290)
        );

        Rectangle box = Rectangle.byBoundingBox(lower, upper);
        lower.untranslate(box.center());
        upper.untranslate(box.center());

        Vector up = Vector.up(128);
        Vector right = new Vector(140, 16);

        return new GridGeometry("GEESE", right, up, new StaggeredSquareGeometry(), lower, upper);
    }

    public static GridGeometry squaresAndTriangles() {

        Vector or = Vector.origin();

        Vector arm = Vector.up(10);

        Vector a = arm.clone();

        Vector b = arm.clone();
        b.rotate(Math.toRadians(60));
        Vector h = b.clone();
        h.scale(-1, 1);

        Vector d = arm.clone();
        d.rotate(Math.toRadians(60 + 90));
        Vector f = d.clone();
        f.scale(-1, 1);

        Vector c = Vector.add(b, d);
        Vector g = c.clone();
        g.scale(-1, 1);

        Vector e = Vector.add(d, f);

        Polygon t_left = new Polygon(
                or, a, b
        );
        Polygon s_left = new Polygon(
                or, b, c, d
        );
        Polygon q_down = new Polygon(
                or, d, e, f
        );
        Polygon s_right = new Polygon(
                or, f, g, h
        );
        Polygon t_right = new Polygon(
                or, h, a
        );

        Vector right = Vector.subtract(h, d);
        Vector up = Vector.subtract(b, f);

        return new GridGeometry("MIXED", right, up, new SquareGeometry(), t_left, s_left, q_down, s_right, t_right);
    }
}
