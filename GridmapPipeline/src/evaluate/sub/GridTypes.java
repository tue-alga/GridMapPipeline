package evaluate.sub;

import arrange.DeformAlgorithm;
import common.SiteMap;
import common.gridmath.GridGeometry;
import common.gridmath.GridMath;
import common.gridmath.GridMath.Coordinate;
import common.gridmath.grids.CustomGeometry;
import common.gridmath.grids.SquareGeometry;
import common.gridmath.grids.StaggeredSquareGeometry;
import common.gridmath.grids.TriangleGeometry;
import common.util.Transform;
import evaluate.Script;
import evaluate.Evaluation;
import evaluate.GridmapScript;
import io.WKT;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.geometry.linear.Rectangle;
import nl.tue.geometrycore.geometryrendering.styling.Dashing;
import nl.tue.geometrycore.geometryrendering.styling.ExtendedColors;
import nl.tue.geometrycore.geometryrendering.styling.Hashures;
import nl.tue.geometrycore.geometryrendering.styling.TextAnchor;
import nl.tue.geometrycore.io.ipe.IPEWriter;

/**
 *
 * @author Wouter Meulemans
 */
public class GridTypes extends Evaluation {

    private GridGeometry[] grids;

    public GridTypes() {
        super("GridTypes");
        GridGeometry rects = new SquareGeometry();
        rects.scale(1.5, 1);

        GridGeometry sheared = new SquareGeometry();
        sheared.shear(0.5, 0);
        sheared.rotate(15);
        grids = new GridGeometry[]{
            rects,
            sheared,
            new StaggeredSquareGeometry(),
            TriangleGeometry.rightAngled(),
            TriangleGeometry.equilateral(),
            CustomGeometry.squaresAndTriangles(),
            CustomGeometry.escherGeese()
        };
    }

    @Override
    public void addScripts(List<Script> scripts) {

        for (GridGeometry grid : grids) {
            GridmapScript s = new GridmapScript(baseroot + "gb.wkt", outroot + "gb-" + grid);
            s.setDeform(new DeformAlgorithm(grid));
            scripts.add(s);
        }
    }

    @Override
    protected void computeOutcomes(IPEWriter write) throws IOException {

        List<Result> results = new ArrayList();
        for (File f : collectWktFiles()) {

            Result res = new Result();
            results.add(res);

            res.result = WKT.read(f);
            res.map = f.getName().substring(0, 2);
            res.grid = f.getName().substring(f.getName().lastIndexOf("-") + 1, f.getName().lastIndexOf("."));
        }

        write.newPage("default");
        write.setLayer("default");

        int index = 0;
        for (GridGeometry grid : grids) {

            String gname = grid.toString();
            Result gt = filter(results, (Result a) -> a.grid.equals(gname)).get(0);

            double w = 65;
            double h = 110;
            double sw = 4;
            double sh = 20;
            double off = 6;

            Vector o = new Vector(16, 650);

            Rectangle rect = Rectangle.byCornerAndSize(Vector.add(o, Vector.right(index * (w + sw))), w, h);

            cartogram(write, gt.result, rect, false, false, null);

            double previewsize = 25;
            Rectangle gtrect = Rectangle.byCornerAndSize(rect.rightTop(), -previewsize, -previewsize);
            drawPreview(write, gt.result.gridmath, gtrect);

            boolean labels = false;
            if (labels) {
                write.setTextStyle(TextAnchor.BASELINE_CENTER, 9);
                write.setStroke(Color.black, 0.2, Dashing.SOLID);
                write.draw(Vector.add(rect.leftBottom(), new Vector(w / 2, -off)), gt.grid);
            }

            index++;
        }
    }

    private void drawPreview(IPEWriter write, GridMath gridmath, Rectangle target) {

        Rectangle source = new Rectangle();
        for (int i = 0; i < gridmath.getPatternComplexity(); i++) {
            for (Coordinate uv : gridmath.unitVectors()) {
                source.includeGeometry(uv.plus(i, 0).getBoundary());
            }
        }

        Transform t = Transform.fitToBox(source, target);

        int mod = gridmath.unitVectors().length == 6 ? 3 : 2;

        write.setStroke(Color.white, 0.4, Dashing.SOLID);
        for (int i = 0; i < gridmath.getPatternComplexity(); i++) {
            write.setFill(ExtendedColors.fromUnitGray(0.3), Hashures.SOLID);
            Polygon B = gridmath.origin().plus(i, 0).getBoundary();
            write.draw(t.apply(B));

            int uvi = 0;
            for (Coordinate uv : gridmath.unitVectors()) {
                if (mod == 3) {
                    boolean geese = gridmath.geometry().getName().contains("GEESE");
                    switch (uvi % mod) {
                        case 0:
                            write.setFill(geese ? ExtendedColors.lightOrange : ExtendedColors.lightBlue, Hashures.SOLID);
                            break;
                        case 1:
                            write.setFill(geese ? ExtendedColors.lightBlue : ExtendedColors.lightPurple, Hashures.SOLID);
                            break;
                        case 2:
                            write.setFill(geese ? ExtendedColors.lightPurple : ExtendedColors.lightOrange, Hashures.SOLID);
                            break;
                    }
                } else {
                    switch (uvi % mod) {
                        case 0:
                            write.setFill(ExtendedColors.lightBlue, Hashures.SOLID);
                            break;
                        case 1:
                            write.setFill(ExtendedColors.lightOrange, Hashures.SOLID);
                            break;
                    }
                }

                write.draw(t.apply(uv.plus(i, 0).getBoundary()));
                uvi++;
            }
        }
    }

    private class Result {

        SiteMap result;
        String map;
        String grid;
    }

}
