package evaluate.sub;

import combine.Combination;
import combine.CombineAlgorithm;
import combine.combinations.AgglomerativeCombination;
import combine.combinations.AgglomerativePartitionCombination;
import combine.combinations.AgnosticCombination;
import combine.combinations.AssignedCombination;
import common.Partition;
import common.SiteMap;
import common.gridmath.GridGeometry;
import common.gridmath.GridMath;
import common.gridmath.grids.HexagonGeometry;
import common.gridmath.grids.SquareGeometry;
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
import nl.tue.geometrycore.geometry.linear.Rectangle;
import nl.tue.geometrycore.geometryrendering.styling.Dashing;
import nl.tue.geometrycore.geometryrendering.styling.Hashures;
import nl.tue.geometrycore.io.ipe.IPEWriter;

public class ComposeTrials extends Evaluation {

    public ComposeTrials() {
        super("ComposeTrials");
    }

    @Override
    public void addScripts(List<Script> scripts) {
        GridGeometry[] geoms = {
            new SquareGeometry(),
            new HexagonGeometry()
        };
        String[] inputs = {
            "uk",
            "nl"
        };
        Combination[] combines = {
            new AgnosticCombination(),
            new AssignedCombination(),
            new AgglomerativeCombination(),
            new AgglomerativePartitionCombination()
        };

        for (Combination comb : combines) {
            CombineAlgorithm combine = new CombineAlgorithm(comb);
            for (GridGeometry geom : geoms) {
                for (String input : inputs) {

                    String cs = input + "-" + geom + "-" + comb.toString();

                    GridmapScript s = new GridmapScript(baseroot + input + "-" + geom + ".wkt", outroot + cs);
                    s.setCombine(combine);
                    scripts.add(s);
                }
            }
        }
    }

    @Override
    protected void computeOutcomes(IPEWriter write) throws IOException {

        List<Result> sites = new ArrayList();
        for (File f : collectWktFiles()) {
            if (!f.getName().endsWith(".wkt")) {
                continue;
            }

            Result cr = new Result();
            sites.add(cr);

            cr.result = WKT.read(f);
            cr.map = f.getName().substring(0, 2);
            cr.hex = f.getName().contains("HEXAGON");
            cr.method = f.getName().substring(f.getName().lastIndexOf("-") + 1, f.getName().lastIndexOf("."));
        }

        writeComposeFigures(write, filter(sites, (Result cr) -> cr.map.equals("nl")));
        writeComposeFigures(write, filter(sites, (Result cr) -> cr.map.equals("uk")));

    }

    private class Result {

        SiteMap result;
        String map;
        boolean hex;
        String method;

    }

    private void writeComposeFigures(IPEWriter write, List<Result> results) {
        write.newPage("input", "old", "new", "aggl", "aggl2");

        double w = 96;
        double h = 112;
        double sep = 10;

        write.setLayer("input");
        {
            SiteMap map = results.get(0).result;

            Transform t = Transform.fitToBox(
                    Rectangle.byBoundingBox(map.outlines),
                    Rectangle.byCornerAndSize(new Vector(12, 400), w, h)
            );

            write.setStroke(null, 0, Dashing.SOLID);
            for (Partition p : map.partitions()) {
                write.setFill(p.color, Hashures.SOLID);
                write.draw(t.apply(p));
            }
        }

        write.setLayer("old");
        {
            SiteMap sqr = filter(results, (Result cr) -> !cr.hex && cr.method.contains("Agnostic")).get(0).result;
            write(write, sqr, 1, 0);
            SiteMap hex = filter(results, (Result cr) -> cr.hex && cr.method.contains("Agnostic")).get(0).result;
            write(write, hex, 1, 1);
        }

        write.setLayer("new");
        {
            SiteMap sqr = filter(results, (Result cr) -> !cr.hex && cr.method.contains("ClosestAssigned")).get(0).result;
            write(write, sqr, 2, 0);
            SiteMap hex = filter(results, (Result cr) -> cr.hex && cr.method.contains("ClosestAssigned")).get(0).result;
            write(write, hex, 2, 1);
        }

//        write.setLayer("aggl");
//        {
//            SiteMap sqr = filter(results, (ComposeResult cr) -> !cr.hex && cr.method.contains("AgglomerativeAssigned")).get(0).result;
//            write(write, sqr, 3, 0);
//            SiteMap hex = filter(results, (ComposeResult cr) -> cr.hex && cr.method.contains("AgglomerativeAssigned")).get(0).result;
//            write(write, hex, 3, 1);
//        }
        write.setLayer("aggl2");
        {
            SiteMap sqr = filter(results, (Result cr) -> !cr.hex && cr.method.contains("AgglomerativePartitionAssigned")).get(0).result;
            write(write, sqr, 3, 0);
            SiteMap hex = filter(results, (Result cr) -> cr.hex && cr.method.contains("AgglomerativePartitionAssigned")).get(0).result;
            write(write, hex, 3, 1);
        }
    }

    private void write(IPEWriter write, SiteMap map, int x, int y) {

        double w = 96;
        double h = 112;
        double sep = 10;

        Transform t = Transform.fitToBox(
                bbox(map.cells()),
                Rectangle.byCornerAndSize(new Vector(12 + x * (w + sep), 400 - y * (h + sep)), w, h)
        );

        write.setStroke(Color.white, 0.2, Dashing.SOLID);
        for (Partition p : map.partitions()) {
            write.setFill(p.color, Hashures.SOLID);
            for (GridMath.Coordinate c : p.cells) {
                write.draw(t.apply(c.getBoundary()));
            }
        }
    }
}
