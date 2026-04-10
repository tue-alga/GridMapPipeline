package main;

import arrange.DeformAlgorithm;
import arrange.MosaicConstants;
import arrange.util.Random;
import combine.CombineAlgorithm;
import combine.combinations.AgnosticCombination;
import common.Outline;
import common.SiteMap;
import common.Stage;
import common.gridmath.GridGeometry;
import common.gridmath.GridGeometrySpawner;
import common.util.Stopwatch;
import evaluate.Evaluation;
import io.IO;
import io.TSV;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.tue.geometrycore.geometry.linear.Rectangle;
import nl.tue.geometrycore.gui.debug.DebugRenderer;
import nl.tue.geometrycore.io.ipe.IPECommands;
import nl.tue.geometrycore.io.ipe.IPEWriter;
import partition.PartitionAlgorithm;

/**
 *
 * @author Wouter Meulemans
 */
public class Cartogram {

    public static void main(String[] args) {
        run(args);
    }

    public static boolean run(String[] args) {
        PrintStream oldps = System.out;

        String intermediate_out = null;
        boolean success = false;

        try {
            String input = CmdLine.findStringArgument("-input", args);
            String output = CmdLine.findStringArgument("-output", args);
            MosaicConstants.configureMosaicCartograms();
            MosaicConstants.EXACT_TILES = CmdLine.hasSwitch("-exact", args);

            if (CmdLine.hasSwitch("-intermediates", args)) {
                MosaicConstants.DRAW_INTERMEDIATES = new DebugRenderer(input + " -> " + output);
                MosaicConstants.DRAW_INTERMEDIATES_DETAIL = CmdLine.findIntegerArgument("-intermediates", args);
                intermediate_out = CmdLine.findStringArgument("-intermediates", 2, args, null);
                if (intermediate_out == null) {
                    MosaicConstants.DRAW_INTERMEDIATES.show();
                }
            }

            GridGeometry geom = GridGeometrySpawner.inferFromString(CmdLine.findStringArgument("-grid", args, "SQUARE"));

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
            SiteMap map = IO.read(new File(input), Stage.INPUT);

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

            PartitionAlgorithm.trivialPartition(map);

            DeformAlgorithm deform = new DeformAlgorithm(geom);
            deform.run(map);

            CombineAlgorithm combine = new CombineAlgorithm(new AgnosticCombination());
            combine.run(map);

            File outfile = new File(output);
            outfile.getParentFile().mkdirs();

            IO.write(outfile, map);

            if (CmdLine.hasSwitch("-pdf", args)) {
                File pdfipe = new File(output.substring(0, output.length() - 4) + "-pdf.ipe");
                File pdf = new File(output.substring(0, output.length() - 4) + ".pdf");

                try (IPEWriter write = IPEWriter.fileWriter(pdfipe)) {
                    write.initialize();
                    write.configureTextHandling(false, 10, false);
                    write.setTextSerifs(true);

                    Rectangle target = IPEWriter.getA4Size();
                    target.grow(-16);

                    boolean bds = CmdLine.hasSwitch("-boundaries", args);
                    if (bds) {
                        write.newPage("cartogram", "boundaries");
                    } else {
                        write.newPage("cartogram");
                    }
                    write.setLayer("cartogram");

                    Evaluation.cartogram(write, map, target, false, CmdLine.hasSwitch("-dark", args), bds ? "boundaries" : null);

                } catch (IOException ex) {
                    Logger.getLogger(Cartogram.class.getName()).log(Level.SEVERE, null, ex);
                }

                IPECommands cmd = IPECommands.create(CmdLine.findStringArgument("-pdf", args));
                cmd.convertIPEtoPDF(pdfipe, pdf);

                pdfipe.delete();
            }

            Stopwatch.printAndClear();

            success = true;
            return true;

        } catch (CmdLine.InvalidArgumentsException ex) {
            printCommandsAndExit(ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (MosaicConstants.DRAW_INTERMEDIATES != null) {
                    if (intermediate_out != null) {
                        File f = new File(intermediate_out);
                        f.getParentFile().mkdirs();
                        MosaicConstants.DRAW_INTERMEDIATES.saveAllPages(f);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            System.setOut(oldps);
            MosaicConstants.reset();
        }
        return success;
    }

    private static void printCommandsAndExit(String error) {
        System.out.println("-------------------------------------------------------------------------");
        System.out.println("This program takes a variable number of arguments.");
        System.out.println("Only the -input, -output and -weights arguments are required.");
        System.out.println("");
        System.out.println(" -input PATH     Uses PATH as path to an ipe or wkt input");
        System.out.println(" -output PATH    Uses PATH as path to the ipe output");
        System.out.println(" -log PATH       Optional: Uses PATH as path to a .txt file for output of standard-out");
        System.out.println(" -pdf PATH       Optional: Converts the endresult to a PDF file, using PATH as the path to the IPE /bin/ folder");
        System.out.println(" -exact          If specified, forces the result to have the exact number");
        System.out.println("                   of tiles, instead of ensuring correct topology.");
        System.out.println(" -grid SHAPE     Runs the cell-arrangement algorithm with");
        System.out.println("                   cell shapes specified by SHAPE: value must derive to");
        System.out.println("                   a GridGeometry (see source for details). Typical values are");
        System.out.println("                   HEXAGON, HEXAGON!r!30 and SQUARE.");
        System.out.println("                 Omitting SHAPE uses a SQUARE grid.");
        System.out.println(" -weights PATH   Uses PATH as path to a tsv file to parse weights");
        System.out.println(" -weights random m M  Sets weights randomly in the interval [m,M].");
        System.out.println(" -weights area m M    Sets weights based on area of the outline, scaled to");
        System.out.println("                        the interval [m,M]. The smallest outline will obtain weight m,");
        System.out.println("                        the largest weight M. Using m < 1, makes for fully proportional scaling.");
        System.out.println(" -dark           If specified, uses dark outlines for cells, instead of white.");
        System.out.println(" -boundaries     If specified, boundary of each part is constructed as a set of line segments");
        System.out.println("                   of tiles, instead of ensuring correct topology.");
        System.out.println(" -intermediates k PATH  If specified, tracks intermediates at detail level k (>= 0), and outputs them to the PATH if it ends with ipe, or otherwise shows a debug GUI.");
        System.out.println("-------------------------------------------------------------------------");
        System.out.println(error);
        System.exit(1);
    }
}
