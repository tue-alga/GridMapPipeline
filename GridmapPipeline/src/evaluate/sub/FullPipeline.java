package evaluate.sub;

import analyze.Analyzer;
import analyze.QualityMap;
import analyze.QualityMap.Summary;
import analyze.analyzers.Deformation;
import analyze.analyzers.Displacement;
import analyze.analyzers.OrthogonalOrder;
import analyze.analyzers.SiteGuideFit;
import arrange.DeformAlgorithm;
import assign.AssignAlgorithm;
import assign.alignments.UniformAgnosticAlignment;
import combine.CombineAlgorithm;
import combine.combinations.AgglomerativePartitionCombination;
import combine.combinations.AssignedCombination;
import common.Outline;
import common.Partition;
import common.Site;
import common.SiteMap;
import common.gridmath.GridGeometry;
import common.gridmath.grids.HexagonGeometry;
import common.gridmath.grids.SquareGeometry;
import common.util.Transform;
import evaluate.Script;
import evaluate.Evaluation;
import static evaluate.Evaluation.bbox;
import evaluate.GridmapScript;
import io.WKT;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.curved.Circle;
import nl.tue.geometrycore.geometry.linear.Rectangle;
import nl.tue.geometrycore.geometryrendering.styling.Dashing;
import nl.tue.geometrycore.geometryrendering.styling.ExtendedColors;
import nl.tue.geometrycore.geometryrendering.styling.Hashures;
import nl.tue.geometrycore.geometryrendering.styling.TextAnchor;
import nl.tue.geometrycore.io.ipe.IPEWriter;
import nl.tue.geometrycore.util.DoubleUtil;
import partition.CutType;
import partition.PartitionAlgorithm;

/**
 *
 * @author Wouter Meulemans
 */
public class FullPipeline extends Evaluation {

    private final int reps = 3;

    public FullPipeline() {
        super("FullPipeline");
    }

    @Override
    public void addScripts(List<Script> scripts) {

        GridGeometry[] geoms = {
            new SquareGeometry(),
            new HexagonGeometry()
        };

        AssignAlgorithm assign = new AssignAlgorithm(new UniformAgnosticAlignment(), null);

        { // NL
            for (GridGeometry geom : geoms) {
                DeformAlgorithm deform = new DeformAlgorithm(geom);

                GridmapScript s = new GridmapScript(dataroot + "NL_municipalities2017.wkt", outroot + "nl-" + geom);
                s.setPartition(new PartitionAlgorithm(CutType.COMBINED, 3, 10));
                s.setRepetitions(reps);
                s.setDeform(deform);
                s.setAssign(assign);
                s.setCombine(new CombineAlgorithm(new AgglomerativePartitionCombination()));
                scripts.add(s);
            }
        }

        { // UK
            for (GridGeometry geom : geoms) {
                DeformAlgorithm deform = new DeformAlgorithm(geom);

                GridmapScript s = new GridmapScript(dataroot + "UK_constituencies.wkt", outroot + "uk-" + geom);
                s.setPartition(new PartitionAlgorithm(CutType.COMBINED, 3, 15));
                s.setRepetitions(reps);
                s.setDeform(deform);
                s.setAssign(assign);
                s.setCombine(new CombineAlgorithm(new AssignedCombination()));
                scripts.add(s);
            }
        }
    }

    private String analysisName(String codename) {
        return codename
                //.replace("GuidingShapeFit", "Guide")
                .replace("SiteGuideFit-miss", "Guide-Miss")
                .replace("SiteGuideFit-deviation", "Guide-Dev")
                .replace("OrthogonalOrder", "OrthoOrder")
                .replace("ALL", "$\\infty$")
                .replace("Deformation", "Deform")
                .replace("Displacement-Guide", "Displaced");
    }

    @Override
    protected void computeOutcomes(IPEWriter write) throws IOException {
        drawQualityMaps(write);
        getTotalRunningTimes();
    }

    private void getTotalRunningTimes() throws IOException {

        String[] maps = {"uk", "nl"};
        String[] geoms = {"SQUARE", "HEXAGON"};

        System.out.println("\\toprule");
        System.out.println("\\textbf{map} & \\textbf{grid} & \\textbf{MSS (s)} & \\textbf{new (s)} \\\\");

        for (String map : maps) {
            System.out.println("\\midrule");
            for (String geom : geoms) {

                int old_part, old_deform, old_assign, old_compose;
                {
                    List<String> lines = Files.readAllLines((new File(outroot + map + "-old-" + geom + ".txt")).toPath());
                    old_part = readTotalTiming(lines.get(lines.size() - 16));
                    old_deform = readTotalTiming(lines.get(lines.size() - 11));
                    old_assign = readTotalTiming(lines.get(lines.size() - 3));
                    old_compose = readTotalTiming(lines.get(lines.size() - 4));
                }

                int new_part, new_deform, new_assign, new_compose;
                {
                    List<String> lines = Files.readAllLines((new File(outroot + map + "-" + geom + ".txt")).toPath());
                    new_part = readTotalTiming(lines.get(lines.size() - 16));
                    new_deform = readTotalTiming(lines.get(lines.size() - 11));
                    new_assign = readTotalTiming(lines.get(lines.size() - 4));
                    new_compose = readTotalTiming(lines.get(lines.size() - 2));
                }

                int old_total = old_part + old_deform + old_assign + old_compose;
                int new_total = new_part + new_deform + new_assign + new_compose;

                if (geom == geoms[0]) {
                    System.out.print(map.toUpperCase());
                }
                System.out.println(" & " + geomFormat(geom) + " & " + format(old_total) + " & " + format(new_total) + " \\\\");

            }
        }

        System.out.println("\\bottomrule");

        System.out.println("");

        System.out.println("\\toprule");
        System.out.println("\\textbf{map} & \\textbf{grid} & \\textbf{step} & \\textbf{MSS (s)} & \\textbf{new (s)} \\\\");

        for (String map : maps) {
            System.out.println("\\midrule");
            for (String geom : geoms) {

                int old_part, old_deform, old_assign, old_compose;
                {
                    List<String> lines = Files.readAllLines((new File(outroot + map + "-old-" + geom + ".txt")).toPath());
                    old_part = readTotalTiming(lines.get(lines.size() - 16));
                    old_deform = readTotalTiming(lines.get(lines.size() - 11));
                    old_assign = readTotalTiming(lines.get(lines.size() - 3));
                    old_compose = readTotalTiming(lines.get(lines.size() - 4));
                }

                int new_part, new_deform, new_assign, new_compose;
                {
                    List<String> lines = Files.readAllLines((new File(outroot + map + "-" + geom + ".txt")).toPath());
                    new_part = readTotalTiming(lines.get(lines.size() - 16));
                    new_deform = readTotalTiming(lines.get(lines.size() - 11));
                    new_assign = readTotalTiming(lines.get(lines.size() - 4));
                    new_compose = readTotalTiming(lines.get(lines.size() - 2));
                }

                int old_total = old_part + old_deform + old_assign + old_compose;
                int new_total = new_part + new_deform + new_assign + new_compose;

                if (geom == geoms[0]) {
                    System.out.print(map.toUpperCase());
                }
                System.out.println(" & " + geomFormat(geom) + " & \\emph{all} & " + format(old_total) + " & " + format(new_total) + " \\\\");
                System.out.println(" & & partition & " + format(old_part) + " & " + format(new_part) + " \\\\");
                System.out.println(" & & arrange & " + format(old_deform) + " & " + format(new_deform) + " \\\\");
                System.out.println(" & & assign & " + format(old_assign) + " & " + format(new_assign) + " \\\\");
                System.out.println(" & & compose & " + format(old_compose) + " & " + format(new_compose) + " \\\\");

            }
        }

        System.out.println("\\bottomrule");
    }

    private String geomFormat(String geom) {
        return geom.charAt(0) + geom.substring(1).toLowerCase();
    }

    private String format(int totalms) {
        double sec = totalms / (1000.0 * reps);
        return df3digits.format(sec);
    }

    private Color oldmaxcol = Color.black;
    private Color newmaxcol = new Color(140, 45, 4);
    private Color mincol = new Color(254, 237, 222);

    private Color interpolate(double value, double min, double newmax, double oldmax) {
        if (value < min) {
            return mincol;
        } else if (value > oldmax) {
            System.err.println("Warning: value outside range, clamping color " + value + " > oldmax " + oldmax);
            return oldmaxcol;
        } else {
            double f;
            Color low, high;
            if (value <= newmax) {
                low = mincol;
                high = newmaxcol;
                f = (value - min) / (newmax - min);
            } else {
                low = newmaxcol;
                high = oldmaxcol;
                f = (value - newmax) / (oldmax - newmax);
            }

            double re = DoubleUtil.interpolate(low.getRed(), high.getRed(), f) / 255.0;
            double gr = DoubleUtil.interpolate(low.getGreen(), high.getGreen(), f) / 255.0;
            double bl = DoubleUtil.interpolate(low.getBlue(), high.getBlue(), f) / 255.0;

            return ExtendedColors.fromUnitRGB(re, gr, bl);
        }
    }

    private void drawQualityMaps(IPEWriter write) throws IOException {
        String[] maps = {
            "uk-SQUARE",
            "uk-old-SQUARE",
            "uk-HEXAGON",
            "uk-old-HEXAGON",
            "nl-SQUARE",
            "nl-old-SQUARE",
            "nl-HEXAGON",
            "nl-old-HEXAGON"
        };

        Analyzer[] analyzers = {
            new SiteGuideFit(SiteGuideFit.Variant.miss),
            new SiteGuideFit(SiteGuideFit.Variant.deviation),
            new Displacement(Displacement.AlignMode.Guide),
            new OrthogonalOrder(5),
            new OrthogonalOrder(15),
            new OrthogonalOrder(50),
            new OrthogonalOrder(100),
            new OrthogonalOrder(0),
            new Deformation(5),
            new Deformation(15),
            new Deformation(50),
            new Deformation(100),
            new Deformation(0)
        };

        QualityMap[][] qmaps = new QualityMap[maps.length][analyzers.length];

        double[] prevmaxs = new double[analyzers.length];

        int mapindex = -1;
        for (String m : maps) {

            mapindex++;
            System.out.println("Map " + m);
            SiteMap map = WKT.read(new File(outroot + m + ".wkt"));
            boolean oldmap = m.contains("-old-");

            double w = 96;
            double h = 155;
            double sw = 4;
            double sh = 20;
            double off = 6;

            Vector o = new Vector(16, 650);

            int cols = 5;

            int r = 0;
            int c = 0;

            String[] layers = new String[analyzers.length + 4];
            layers[0] = "partition";
            layers[1] = "sites";
            layers[2] = "result";
            layers[3] = "labels";
            for (int i = 0; i < analyzers.length; i++) {
                layers[i + 4] = analyzers[i].toString();
            }

            write.newPage(layers);

            {
                Rectangle rect = Rectangle.byCornerAndSize(Vector.addSeq(o, Vector.right(c * (w + sw)), Vector.down(r * (h + sh))), w, h);

                write.setLayer(layers[0]);
                partition(write, map, rect, false);

                write.setLayer(layers[1]);
                sites(write, map, false, rect);

                write.setLayer(layers[3]);
                write.setTextStyle(TextAnchor.BASELINE_CENTER, 9);
                write.setStroke(Color.black, 0.2, Dashing.SOLID);
                write.draw(Vector.add(rect.leftBottom(), new Vector(w / 2, -off)), "input");
            }
            c++;

            {
                Rectangle rect = Rectangle.byCornerAndSize(Vector.addSeq(o, Vector.right(c * (w + sw)), Vector.down(r * (h + sh))), w, h);
                write.setLayer(layers[2]);
                assignment(write, map, rect, false, false, null);

                write.setLayer(layers[3]);
                write.setTextStyle(TextAnchor.BASELINE_CENTER, 9);
                write.setStroke(Color.black, 0.2, Dashing.SOLID);
                write.draw(Vector.add(rect.leftBottom(), new Vector(w / 2, -off)), "result");
            }
            c++;

            double[] maxs = new double[analyzers.length];

            for (int i = 0; i < analyzers.length; i++) {

                QualityMap qmap = analyzers[i].run(map);
                qmaps[mapindex][i] = qmap;

                maxs[i] = qmap.getActualMaximum();

                Rectangle rect = Rectangle.byCornerAndSize(Vector.addSeq(o, Vector.right(c * (w + sw)), Vector.down(r * (h + sh))), w, h);
                write.setLayer(layers[i + 4]);

                double min = 0;
                double redmax = oldmap ? prevmaxs[i] : maxs[i];
                double blackmax = maxs[i];

                qualityMap(write, qmap, map, rect, min, redmax, blackmax);

                write.setLayer("labels");
                write.setTextStyle(TextAnchor.BASELINE_CENTER, 9);
                write.setStroke(Color.black, 0.2, Dashing.SOLID);
                write.draw(Vector.add(rect.leftBottom(), new Vector(w / 2, -off)), analysisName(qmap.toString()));

                double rad = 2.5;
                double space = 0.5;
                double inset = 7;
                Vector ctr = m.startsWith("nl-") ? Vector.add(rect.leftTop(), new Vector(rad + inset, -rad)) : Vector.add(rect.rightTop(), new Vector(-rad - inset, -rad));
                Vector lbloff = m.startsWith("nl-") ? Vector.right(rad + space) : Vector.left(rad + space);
                write.setStroke(null, 0.2, null);
                write.setTextStyle(m.startsWith("nl-") ? TextAnchor.LEFT : TextAnchor.RIGHT, 6);

                if (i == 0) {

                    write.setFill(newmaxcol, Hashures.SOLID);
                    write.draw(new Circle(ctr, rad));
                    write.draw(Vector.add(ctr, lbloff), "miss");
                    ctr.translate(0, -rad * 2 - space);

                    write.setFill(mincol, Hashures.SOLID);
                    write.draw(new Circle(ctr, rad));
                    write.draw(Vector.add(ctr, lbloff), "hit");
                } else {

                    DecimalFormat df2 = analyzers[i] instanceof OrthogonalOrder
                            ? new DecimalFormat("#0", DecimalFormatSymbols.getInstance(Locale.US))
                            : new DecimalFormat("#0.0", DecimalFormatSymbols.getInstance(Locale.US));

                    if (blackmax > redmax) {
                        write.setFill(oldmaxcol, Hashures.SOLID);
                        write.draw(new Circle(ctr, rad));
                        write.draw(Vector.add(ctr, lbloff), df2.format(blackmax));
                        ctr.translate(0, -rad * 2 - space);
                    }

                    write.setFill(newmaxcol, Hashures.SOLID);
                    write.draw(new Circle(ctr, rad));
                    write.draw(Vector.add(ctr, lbloff), df2.format(redmax));
                    ctr.translate(0, -rad * 2 - space);

                    write.setFill(interpolate(0.5, 0, 1, 1), Hashures.SOLID);
                    write.draw(new Circle(ctr, rad));
                    write.draw(Vector.add(ctr, lbloff), df2.format(redmax / 2.0));
                    ctr.translate(0, -rad * 2 - space);

                    write.setFill(mincol, Hashures.SOLID);
                    write.draw(new Circle(ctr, rad));
                    write.draw(Vector.add(ctr, lbloff), df2.format(0));
                    ctr.translate(0, -rad * 2 - space);
                }

                c++;
                if (c >= cols) {
                    c = 0;
                    r++;
                }
            }

            prevmaxs = maxs;

            System.out.println("");
        }

        System.out.println("");

        makeTables(maps, analyzers, 0, 2, qmaps);
        System.out.println("");
        makeTables(maps, analyzers, 3, 7, qmaps);
        System.out.println("");
        makeTables(maps, analyzers, 8, 12, qmaps);

        System.out.println("");

    }

    private void makeTables(String[] maps, Analyzer[] analyzers, int first, int last, QualityMap[][] qmaps) {

        double betterfrac = 0.95;
        double reallybetterfrac = 0.8;

        System.out.print("\\begin{tabular}{@{\\hspace{1pt}}");
        System.out.print("l@{\\hspace{\\tabcolsep}}");
        System.out.print("l");
        for (int i = first; i <= last; i++) {
            System.out.print("@{\\hspace{\\tabcolsep}}r");
            System.out.print("@{\\hspace{\\tabcolsep}}r");
        }
        System.out.println("@{\\hspace{1pt}}}");

        System.out.println("\\toprule");

        System.out.print("\\textbf{map} & \\textbf{pipeline}");
        for (int i = first; i <= last; i++) {
            String aname = analysisName(analyzers[i].toString());
            if (aname.startsWith("Ortho") || aname.startsWith("Deform")) {
                String k = aname.split("-")[1].replaceAll("$", "");
                System.out.print(" & \\multicolumn{2}{c}{\\textbf{$k = " + k + "$}}");
            } else {
                System.out.print(" & \\multicolumn{2}{c}{\\textbf{" + aname + "}}");
            }
        }
        System.out.println(" \\\\");
        System.out.print(" & ");
        for (int i = first; i <= last; i++) {
            System.out.print(" & \\emph{avg} & \\emph{max}");
        }
        System.out.println(" \\\\");
        System.out.println("\\midrule");
        double[] newavgs = new double[2 * (last - first + 1)];
        double[] oldavgs = new double[2 * (last - first + 1)];
        Arrays.fill(newavgs, 0.0);
        Arrays.fill(oldavgs, 0.0);
        for (int mapindex = 0; mapindex < maps.length; mapindex++) {

            String m = maps[mapindex];
            String map = m.startsWith("nl") ? "NL" : "UK";
            String grid = m.contains("SQUARE") ? "Square" : "Hexagon";
            boolean old = m.contains("old");
            String pipeline = m.contains("old") ? "MSS" : "new";
            System.out.print((old ? "" : map + " " + grid) + " & " + pipeline);

            int coli = 0;
            for (int i = first; i <= last; i++) {
                String aname = analysisName(analyzers[i].toString());
                QualityMap qmap = qmaps[mapindex][i];
                Summary summary = qmap.summarize();

                double fac = aname.startsWith("Guide-") ? 100 : 1;
                if (old) {
                    oldavgs[coli++] += fac * summary.avg;
                    oldavgs[coli++] += fac * summary.max;
                } else {
                    newavgs[coli++] += fac * summary.avg;
                    newavgs[coli++] += fac * summary.max;
                }

                Summary othersummary = qmaps[old ? (mapindex - 1) : (mapindex + 1)][i].summarize();
                String avgbetterstring;
                if (summary.avg < othersummary.avg * reallybetterfrac) {
                    avgbetterstring = "\\textcolor{reallybettercolor}";
                } else if (summary.avg < othersummary.avg * betterfrac) {
                    avgbetterstring = "\\textcolor{bettercolor}";
                } else {
                    avgbetterstring = "";
                }
                String maxbetterstring;
                if (summary.max < othersummary.max * reallybetterfrac) {
                    maxbetterstring = "\\textcolor{reallybettercolor}";
                } else if (summary.max < othersummary.max * betterfrac) {
                    maxbetterstring = "\\textcolor{bettercolor}";
                } else {
                    maxbetterstring = "";
                }

                if (aname.startsWith("Guide-")) {
                    System.out.print(" & " + avgbetterstring + "{$" + df2digits.format(fac * summary.avg) + "\\%$} & " + maxbetterstring + "{$" + df2digits.format(fac * summary.max) + "\\%$}");
                } else if (aname.startsWith("Ortho")) {
                    System.out.print(" & " + avgbetterstring + "{$" + df2digits.format(summary.avg) + "$} & " + maxbetterstring + "{$" + df0digits.format(summary.max) + "$}");
                } else {
                    System.out.print(" & " + avgbetterstring + "{$" + df2digits.format(summary.avg) + "$} & " + maxbetterstring + "{$" + df2digits.format(summary.max) + "$}");
                }
            }
            System.out.println(" \\\\");

            if (old) {
                System.out.println("\\midrule");
            }
        }

        System.out.print(" \\emph{average} & new ");
        for (int i = 0; i < newavgs.length; i++) {
            String post = "";
            if (analysisName(analyzers[first + i / 2].toString()).startsWith("Guide-")) {
                post = "\\%";
            }
            String betterstring;
            if (newavgs[i] < oldavgs[i] * reallybetterfrac) {
                betterstring = "\\textcolor{reallybettercolor}";
            } else if (newavgs[i] < oldavgs[i] * betterfrac) {
                betterstring = "\\textcolor{bettercolor}";
            } else {
                betterstring = "";
            }
            System.out.print(" & " + betterstring + "{$" + df2digits.format(newavgs[i] / 4.0) + post + "$}");
        }
        System.out.println(" \\\\");
        System.out.print("  & MSS ");
        for (int i = 0; i < oldavgs.length; i++) {
            String post = "";
            if (analysisName(analyzers[first + i / 2].toString()).startsWith("Guide-")) {
                post = "\\%";
            }
            String betterstring;
            if (oldavgs[i] < newavgs[i] * reallybetterfrac) {
                betterstring = "\\textcolor{reallybettercolor}";
            } else if (oldavgs[i] < newavgs[i] * betterfrac) {
                betterstring = "\\textcolor{bettercolor}";
            } else {
                betterstring = "";
            }
            System.out.print(" & " + betterstring + "{$" + df2digits.format(oldavgs[i] / 4.0) + post + "$}");
        }
        System.out.println(" \\\\");

        System.out.println("\\bottomrule");
        System.out.println("\\end{tabular}");
    }

    public void qualityMap(IPEWriter write, QualityMap qmap, SiteMap site, Rectangle target, double min, double newmax, double oldmax) {

        Rectangle box = bbox(site.cells());
        Transform t = Transform.fitToBox(box, target);

        write.setStroke(Color.white, 0.2, Dashing.SOLID);
        for (Outline o : site.outlines) {
            for (Partition p : o.partitions) {

                write.pushGroup();
                for (Site s : p.sites) {

                    Color c;
                    double val = qmap.getQuality(s);
                    if (Double.isNaN(val)) {
                        System.err.println("Warning: NaN value?");
                        c = ExtendedColors.gray;
                    } else {
                        c = interpolate(val, min, newmax, oldmax);
                    }
                    write.setFill(c, Hashures.SOLID);

                    write.draw(t.apply(s.getCell().getBoundary()));
                }
                write.popGroup();
            }
        }

    }
}
