package evaluate;

import common.Outline;
import common.Partition;
import common.Site;
import common.SiteMap;
import common.gridmath.AdjacencyType;
import common.gridmath.GridMath.Coordinate;
import common.util.Transform;
import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.geometry.linear.Rectangle;
import nl.tue.geometrycore.geometryrendering.styling.Dashing;
import nl.tue.geometrycore.geometryrendering.styling.ExtendedColors;
import nl.tue.geometrycore.geometryrendering.styling.Hashures;
import nl.tue.geometrycore.geometryrendering.styling.TextAnchor;
import nl.tue.geometrycore.io.ipe.IPECommands;
import nl.tue.geometrycore.io.ipe.IPEWriter;

/**
 *
 * @author Wouter Meulemans
 */
public abstract class Evaluation {

    protected static final DecimalFormat df0digits = new DecimalFormat("#0", DecimalFormatSymbols.getInstance(Locale.US));
    protected static final DecimalFormat df1digits = new DecimalFormat("#0.0", DecimalFormatSymbols.getInstance(Locale.US));
    protected static final DecimalFormat df2digits = new DecimalFormat("#0.00", DecimalFormatSymbols.getInstance(Locale.US));
    protected static final DecimalFormat df3digits = new DecimalFormat("#0.000", DecimalFormatSymbols.getInstance(Locale.US));
    protected static final double fontsize = 9;
    protected static final double labelsize = 7.5;
    protected static final IPECommands cmd = IPECommands.create("C:/Program Files/ipe-7.2.24/bin/");
    // shared setup
    protected final String dataroot;
    protected final String outroot;
    protected final String baseroot;
    protected final String evalroot;
    // specifics
    private final String name;
    private final boolean produceLog, produceIpe;

    public Evaluation(String name) {
        this(name, true, true);
    }

    public Evaluation(String name, boolean produceLog, boolean produceIpe) {
        this.name = name;
        this.produceLog = produceLog;
        this.produceIpe = produceIpe;
        dataroot = "../Data/";
        outroot = "../Output/" + name + "/";
        baseroot = "../Output/Bases/";
        evalroot = "../Evaluation/";
    }

    @Override
    public String toString() {
        return name;
    }

    public abstract void addScripts(List<Script> scripts);

    public void computeOutcomes() {

        if (!produceLog && !produceIpe) {
            return;
        }

        (new File(evalroot)).mkdirs();

        System.out.println("Evaluation: " + name);
        PrintStream oldps = System.out;
        if (produceLog) {
            try {
                File psf = new File(evalroot + name + ".txt");
                System.setOut(new PrintStream(psf));
            } catch (FileNotFoundException ex) {
                System.err.println("" + ex.getMessage());
                System.setOut(oldps);
            }
        }

        try {

            if (produceIpe) {
                File f = new File(evalroot + name + ".ipe");

                IPEWriter write = IPEWriter.fileWriter(f);
                write.initialize();
                write.configureTextHandling(false, 10, false);
                write.setTextSerifs(true);

                computeOutcomes(write);

                write.close();

                cmd.convertIPEtoPDF(f);
            } else {
                computeOutcomes(null);
            }

        } catch (IOException ex) {
            Logger.getLogger(Evaluation.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (produceLog) {
            System.setOut(oldps);
        }
        System.out.println("  Done");
    }

    protected abstract void computeOutcomes(IPEWriter write) throws IOException;

    protected List<File> collectWktFiles() {
        return collectFiles((File ff) -> ff.getName().endsWith(".wkt"));
    }

    protected List<File> collectLogFiles() {
        return collectFiles((File ff) -> ff.getName().endsWith(".txt"));
    }

    protected List<File> collectFiles(Function<File, Boolean> accept) {
        File outdir = new File(outroot);
        List<File> files = new ArrayList();
        for (File f : outdir.listFiles()) {
            if (accept.apply(f)) {
                files.add(f);
            }
        }
        return files;
    }

    protected String last(String str, String sep) {
        return str.substring(str.lastIndexOf(" ") + 1);
    }

    protected String firstTrim(String str, String sep) {
        while (str.startsWith(sep)) {
            str = str.substring(1);
        }
        return str.substring(0, str.indexOf(" "));
    }

    public static Rectangle bbox(Iterable<Coordinate> cells) {
        Rectangle r = new Rectangle();
        for (Coordinate c : cells) {
            r.includeGeometry(c.getBoundary());
        }
        return r;
    }

    public static Coordinate topLeft(Outline out) {
        Coordinate topleft = null;
        for (Partition p : out.partitions) {
            for (Coordinate c : p.cells) {
                if (topleft == null || c.y > topleft.y || (c.y == topleft.y && c.x < topleft.x)) {
                    topleft = c;
                }
            }
        }
        return topleft;
    }

    public static Coordinate topRight(Outline out) {
        Coordinate topright = null;
        for (Partition p : out.partitions) {
            for (Coordinate c : p.cells) {
                if (topright == null || c.y > topright.y || (c.y == topright.y && c.x > topright.x)) {
                    topright = c;
                }
            }
        }
        return topright;
    }

    public static void outlines(IPEWriter write, SiteMap site, Rectangle target) {

        Rectangle box = Rectangle.byBoundingBox(site.outlines);
        Transform t = Transform.fitToBox(box, target);

        write.setStroke(null, 0.2, Dashing.SOLID);
        write.setFill(ExtendedColors.fromUnitGray(0.7), Hashures.SOLID);
        for (Outline o : site.outlines) {
            write.draw(t.apply(o));
        }

    }

    public static void sites(IPEWriter write, SiteMap site, boolean labels, Rectangle target) {

        Rectangle box = Rectangle.byBoundingBox(site.outlines);
        Transform t = Transform.fitToBox(box, target);

        write.setFill(null, Hashures.SOLID);
        if (labels) {
            write.setTextStyle(TextAnchor.LEFT, 7);
        }

        for (Site s : site.sites()) {
            Vector v = t.apply(s);

            write.setStroke(s.getColor(), 0.2, Dashing.SOLID);

            write.drawSymbol(v, 2, "mark/disk(sx)");
            if (labels) {
                write.draw(Vector.add(v, Vector.right(2)), s.getLabel());
            }
        }

    }

    public static void partition(IPEWriter write, SiteMap site, Rectangle target, boolean color) {

        Rectangle box = Rectangle.byBoundingBox(site.outlines);
        Transform t = Transform.fitToBox(box, target);

        if (color) {
            write.setStroke(null, 0.2, Dashing.SOLID);
        } else {
            write.setStroke(ExtendedColors.white, 0.2, Dashing.SOLID);
            write.setFill(ExtendedColors.fromUnitGray(0.7), Hashures.SOLID);
        }

        for (Partition p : site.partitions()) {
            if (color) {
                write.setFill(p.color, Hashures.SOLID);
            }
            write.draw(t.apply(p));
        }
    }

    public static void cartogram(IPEWriter write, SiteMap site, Rectangle target, boolean sort, boolean dark, String boundaries) {

        Rectangle box = bbox(site.cells());
        Transform t = Transform.fitToBox(box, target);

        List<Outline> outs = new ArrayList(site.outlines);
        List<Vector> offsets = new ArrayList();
        if (sort) {
            outs.sort((a, b) -> -Integer.compare(a.sites.size(), b.sites.size()));

            int space = 2;
            int first = 4;
            int last = 2;

            Coordinate next = topRight(outs.get(0));
            next = next.plus(space + first, 0);

            for (int i = outs.size() - 1; i >= 1; i--) {
                if (i == 1) {
                    next = next.plus(last, 0);
                }
                Outline o = outs.get(i);
                Coordinate topleft = topLeft(o);
                offsets.add(Vector.subtract(next.toVector(), topleft.toVector()));
                Coordinate topright = topRight(o);

                next = next.plus(space + topright.x - topleft.x, 0);
            }
            offsets.add(Vector.origin());

            Collections.reverse(offsets);

        } else {
            for (Outline o : outs) {
                offsets.add(Vector.origin());
            }
        }

        write.setStroke(dark ? ExtendedColors.black : ExtendedColors.white, 0.2, Dashing.SOLID);
        for (int i = 0; i < outs.size(); i++) {
            Outline o = outs.get(i);
            for (Partition p : o.partitions) {
                write.setFill(p.color, Hashures.SOLID);
                write.pushGroup();
                for (Coordinate c : p.cells) {
                    Polygon poly = c.getBoundary();
                    poly.translate(offsets.get(i));
                    write.draw(t.apply(poly));
                }
                write.popGroup();
            }
        }

        if (boundaries != null) {
            write.setLayer(boundaries);
            write.setFill(null, Hashures.SOLID);
            write.setStroke(dark ? ExtendedColors.black : ExtendedColors.white, 0.4, Dashing.SOLID);
            for (int i = 0; i < outs.size(); i++) {
                Outline o = outs.get(i);
                for (Partition p : o.partitions) {
                    write.setFill(p.color, Hashures.SOLID);

                    write.pushGroup();
                    for (Coordinate c : p.cells) {
                        // TODO: this only works for square/triangular/hexagon, where separating boundaries are a single edge and neighbors are in order of the boundary 
                        Coordinate[] nbrs = c.adjacent(AdjacencyType.ROOKS);
                        for (int j = 0; j < nbrs.length; j++) {
                            if (!p.cells.contains(nbrs[j])) {
                                write.draw(t.apply(c.getBoundary().edge(j)));
                            }
                        }
                    }
                    write.popGroup();
                }
            }
        }
    }

    public static void assignment(IPEWriter write, SiteMap site, Rectangle target, boolean sort, boolean labels, String boundaries) {

        Rectangle box = bbox(site.cells());
        Transform t = Transform.fitToBox(box, target);

        List<Outline> outs = new ArrayList(site.outlines);
        List<Vector> offsets = new ArrayList();
        if (sort) {
            outs.sort((a, b) -> -Integer.compare(a.sites.size(), b.sites.size()));

            int space = 2;
            int first = 4;
            int last = 2;

            Coordinate next = topRight(outs.get(0));
            next = next.plus(space + first, 0);

            for (int i = outs.size() - 1; i >= 1; i--) {
                if (i == 1) {
                    next = next.plus(last, 0);
                }
                Outline o = outs.get(i);
                Coordinate topleft = topLeft(o);
                offsets.add(Vector.subtract(next.toVector(), topleft.toVector()));
                Coordinate topright = topRight(o);

                next = next.plus(space + topright.x - topleft.x, 0);
            }
            offsets.add(Vector.origin());

            Collections.reverse(offsets);

        } else {
            for (Outline o : outs) {
                offsets.add(Vector.origin());
            }
        }

        write.setStroke(Color.white, 0.2, Dashing.SOLID);
        if (labels) {
            write.setTextStyle(TextAnchor.CENTER, 7);
        }
        for (int i = 0; i < outs.size(); i++) {
            Outline o = outs.get(i);
            for (Partition p : o.partitions) {
                write.pushGroup();
                for (Site s : p.sites) {
                    write.setFill(s.getColor(), Hashures.SOLID);
                    Polygon poly = s.getCell().getBoundary();
                    poly.translate(offsets.get(i));
                    write.draw(t.apply(poly));
                    if (labels) {
                        write.draw(t.apply(poly.centroid()), s.getLabel());
                    }
                }
                write.popGroup();
            }
        }

        if (boundaries != null) {
            write.setLayer(boundaries);
            write.setFill(null, Hashures.SOLID);
            write.setStroke(ExtendedColors.white, 0.4, Dashing.SOLID);
            for (int i = 0; i < outs.size(); i++) {
                Outline o = outs.get(i);
                for (Partition p : o.partitions) {
                    write.setFill(p.color, Hashures.SOLID);

                    write.pushGroup();
                    for (Coordinate c : p.cells) {
                        // TODO: this only works for square/triangular/hexagon, where separating boundaries are a single edge and neighbors are in order of the boundary 
                        Coordinate[] nbrs = c.adjacent(AdjacencyType.ROOKS);
                        for (int j = 0; j < nbrs.length; j++) {
                            if (!p.cells.contains(nbrs[j])) {
                                write.draw(t.apply(c.getBoundary().edge(j)));
                            }
                        }
                    }
                    write.popGroup();
                }
            }
        }
    }

    protected int readTotalTiming(String line) {
        int len;
        do {
            len = line.length();
            line = line.replace("  ", " ");
        } while (len != line.length());
        String[] spl = line.split(" ");
        return Integer.parseInt(spl[spl.length - 3]);
    }

    protected int totalMillisToAvgSec(int total, int reps) {
        return total / reps / 1000;
    }

    protected int avgSecToTotalMillis(int sec, int reps) {
        return sec * 1000 * reps;
    }

    protected <T> double average(List<T> list, Function<T, Double> val) {
        double tot = 0;
        for (T e : list) {
            tot += val.apply(e);
        }
        return tot / list.size();
    }

    protected double[] reverse(double[] arr) {
        double[] res = new double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            res[arr.length - 1 - i] = arr[i];
        }
        return res;
    }

    protected void writeBar(IPEWriter write, double left, double bottom, double barheight, double chartwidth, int max, int total, int[] values, Color[] colors) {

        write.setStroke(null, 0, Dashing.SOLID);

        double t = left;
        for (int i = 0; i < values.length; i++) {
            write.setFill(colors[i], Hashures.SOLID);
            double w = chartwidth * values[i] / (double) max;
            write.draw(Rectangle.byCornerAndSize(new Vector(t, bottom), w, barheight));
            t += w;
        }

        write.setFill(ExtendedColors.darkGray, Hashures.SOLID);
        double w = left + chartwidth * total / (double) max;
        write.draw(Rectangle.byCornerAndSize(new Vector(t, bottom), w - t, barheight));
    }

    protected <T> double max(List<T> list, Function<T, Double> val) {
        double m = Double.NEGATIVE_INFINITY;
        for (T e : list) {
            m = Math.max(m, val.apply(e));
        }
        return m;
    }

    protected <T> int maxint(List<T> list, Function<T, Integer> val) {
        int m = Integer.MIN_VALUE;
        for (T e : list) {
            m = Math.max(m, val.apply(e));
        }
        return m;
    }

    protected <T> List<T> filter(List<T> pss, Function<T, Boolean> accept) {
        List<T> res = new ArrayList();
        for (T ps : pss) {
            if (accept.apply(ps)) {
                res.add(ps);
            }
        }
        return res;
    }
}
