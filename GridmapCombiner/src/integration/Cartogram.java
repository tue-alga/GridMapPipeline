/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package integration;

import arrange.gui.MainGUI.HeuristicRunner;
import arrange.main.Hexastuff;
import arrange.model.Cartogram.MosaicCartogram.Coordinate;
import arrange.model.HexagonalMap;
import arrange.model.util.Random;
import arrange.model.util.Vector2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.geometry.linear.Rectangle;
import nl.tue.geometrycore.geometryrendering.styling.Dashing;
import nl.tue.geometrycore.geometryrendering.styling.ExtendedColors;
import nl.tue.geometrycore.geometryrendering.styling.Hashures;
import nl.tue.geometrycore.io.ipe.IPECommands;
import nl.tue.geometrycore.io.ipe.IPEWriter;

/**
 *
 * @author wmeulema
 */
public class Cartogram {

    public static void run(String[] args) {

        for (String s : args) {
            if (s.startsWith("-")) {
                if (s != args[0]) {
                    System.out.println("");
                }
                System.out.print("   ");
            }
            System.out.print(" " + s);
        }
        System.out.println("");

        PrintStream oldps = System.out;
        try {
            

            String input = CmdLine.findStringArgument("-input", args);
            String output = CmdLine.findStringArgument("-output", args);
            if (CmdLine.hasSwitch("-original", args)) {
                HeuristicRunner.configureMosaicCartograms();
            } else {                
                HeuristicRunner.SCALING_THRESHOLD = 20;
            }
            boolean exact = CmdLine.hasSwitch("-exact", args);
            boolean hex = CmdLine.findStringArgument("-grid", args, "SQUARE").equals("HEXAGON");

            if (CmdLine.hasSwitch("-log", args)) {
                try {
                    File f = new File(CmdLine.findStringArgument("-log", args));
                    f.getParentFile().mkdirs();
                    System.setOut(new PrintStream(f));
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(Cartogram.class.getName()).log(Level.SEVERE, null, ex);
                    System.setOut(oldps);
                }
            }

            // we just need the labelled outlines
            SiteMap map = input.endsWith(".wkt")
                    ? WKT.read(new File(input))
                    : IPE.read(new File(input));

            String weights = CmdLine.findStringArgument("-weights", args);
            if (weights.equals("area")) {
                int min = CmdLine.findIntegerArgument("-weights", 2, args);
                int max = CmdLine.findIntegerArgument("-weights", 3, args);

                // scale every weight such that the smallest outline has size min and the largest size max
                double minarea = Double.POSITIVE_INFINITY;
                double maxarea = Double.NEGATIVE_INFINITY;
                for (Outline o : map.outlines) {
                    double a = o.areaUnsigned();
                    minarea = Math.min(minarea, a);
                    maxarea = Math.max(maxarea, a);
                }

                if (min < 1) {
                    minarea = 0;
                    min = 1;
                }

                for (Outline o : map.outlines) {
                    double a = o.areaUnsigned();
                    double f = (a - minarea) / (maxarea - minarea);

                    o.customWeight = min + (int) Math.round(f * (max - min));
                }

            } else if (weights.equals("random")) {
                int min = CmdLine.findIntegerArgument("-weights", 2, args);
                int max = CmdLine.findIntegerArgument("-weights", 3, args);

                // give every outline a weight, randomly in the given interval                
                for (Outline o : map.outlines) {
                    o.customWeight = min + Random.nextInt(max - min + 1);
                }

            } else if (weights.endsWith(".tsv")) {

                double fac = CmdLine.findDoubleArgument("-weights", 2, args, 1.0);

                Map<String, Double> weightmap = TSV.readWeightMap(new File(weights));

                for (Outline o : map.outlines) {
                    o.customWeight = (int) Math.round(Math.max(1, weightmap.get(o.label) * fac));
                }
            }

            for (Outline o : map.outlines) {
                o.trivialPartition();
            }

            File outdir = new File(output + "-dir");
            outdir.mkdirs();
            Hexastuff.main(map, hex, exact, outdir);

            File outfile = new File(output);
            outfile.getParentFile().mkdirs();

            try (IPEWriter write = IPEWriter.fileWriter(outfile)) {
                write.initialize();
                write.configureTextHandling(false, 10, false);
                write.setTextSerifs(true);

                Rectangle target = IPEWriter.getA4Size();
                target.grow(-16);

                cartogram(write, map, hex, target, CmdLine.hasSwitch("-dark", args));

            } catch (IOException ex) {
                Logger.getLogger(Cartogram.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (CmdLine.hasSwitch("-pdf", args)) {
                IPECommands cmd = IPECommands.create(CmdLine.findStringArgument("-pdf", args));
                cmd.convertIPEtoPDF(outfile);
            }

            Stopwatch.printAndClear();

            System.setOut(oldps);

            HeuristicRunner.configureGridMaps();
        } catch (CmdLine.InvalidArgumentsException ex) {
            HeuristicRunner.configureGridMaps();
            System.err.println(ex.getMessage());
            System.setOut(oldps);
        }

    }

    public static Polygon toBoundary(Coordinate c, boolean hex) {
        Vector2D ctr = c.toVector2D();
        Polygon P = new Polygon();
        Point2D[] boundary;
        if (hex) {
            boundary = HexagonalMap.standardBoundary;
        } else {
            double APOTHEM = 0.5;
            boundary = new Point2D[4];
            boundary[0] = new Point2D.Double(APOTHEM, -APOTHEM);
            boundary[1] = new Point2D.Double(APOTHEM, APOTHEM);
            boundary[2] = new Point2D.Double(-APOTHEM, APOTHEM);
            boundary[3] = new Point2D.Double(-APOTHEM, -APOTHEM);
        }
        for (Point2D pt : boundary) {
            P.addVertex(new Vector(ctr.getX() + pt.getX(), ctr.getY() + pt.getY()));
        }
        return P;
    }

    public static Rectangle bbox(Iterable<Coordinate> cells, boolean hex) {
        Rectangle r = new Rectangle();
        for (Coordinate c : cells) {
            r.includeGeometry(toBoundary(c, hex));
        }
        return r;
    }

    public static void cartogram(IPEWriter write, SiteMap site, boolean hex, Rectangle target, boolean dark) {

        Rectangle box = bbox(site.cells(), hex);
        Transform t = Transform.fitToBox(box, target);

        write.setStroke(dark ? ExtendedColors.black : ExtendedColors.white, 0.2, Dashing.SOLID);
        for (Outline o : site.outlines) {
            for (Partition p : o.partitions) {
                write.setFill(p.color, Hashures.SOLID);
                write.pushGroup();
                for (Coordinate c : p.mosaic.coordinateSet()) {
                    Polygon poly = toBoundary(c, hex);
                    write.draw(t.apply(poly));
                }
                write.popGroup();
            }
        }
    }
}
