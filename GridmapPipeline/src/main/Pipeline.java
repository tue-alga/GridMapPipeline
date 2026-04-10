package main;

import assign.AssignAlgorithm;
import common.SiteMap;
import common.Stage;
import io.IPE;
import java.io.File;
import arrange.DeformAlgorithm;
import assign.AgnosticAlignment;
import assign.alignments.UniformAgnosticAlignment;
import combine.Combination;
import combine.CombineAlgorithm;
import combine.combinations.AgglomerativeCombination;
import combine.combinations.AgnosticCombination;
import combine.combinations.AssignedCombination;
import common.gridmath.GridGeometry;
import common.gridmath.GridGeometrySpawner;
import common.gridmath.grids.SquareGeometry;
import common.util.Stopwatch;
import partition.CutGenerator;
import partition.CutType;
import partition.PartitionAlgorithm;

/**
 *
 * @author Wouter Meulemans
 */
public class Pipeline {

    public static void main(String[] args) {
        Pipeline pl = new Pipeline();
        pl.parseArguments(args);
        pl.execute();
    }

    // NB: default values are shown below, but this is largely irrelevant
    // general
    private File input;
    private File output;
    private boolean exportall = false;
    // partition settings
    private boolean runDecomposition = true; // if false, just runs a trivial partition
    private int productivity = 10;
    private double dilation = 3.0;
    // deform settings
    private boolean runArrange = true;
    private GridGeometry gridType = new SquareGeometry();
    // assign settings
    private boolean runAssign = true;
    private AgnosticAlignment alignment = new UniformAgnosticAlignment();
    // combine settings
    private boolean runCombine = true;
    private Combination combine = new AssignedCombination();

    private void parseArguments(String[] args) {
        try {
            input = new File(CmdLine.findStringArgument("-input", args));
            output = new File(CmdLine.findStringArgument("-output", args));
            exportall = CmdLine.hasSwitch("-exportall", args);

            runDecomposition = CmdLine.hasSwitch("-decompose", args);
            if (runDecomposition) {
                productivity = CmdLine.findIntegerArgument("-decompose", 1, args);
                dilation = CmdLine.findDoubleArgument("-decompose", 2, args);
                CutGenerator.command = CmdLine.findStringArgument("-decompose", 3, args, null);
                if (!(new File(CutGenerator.findCommand())).exists()) {
                    System.err.println("Cannot find the CmdMedialAxis program at " + CutGenerator.findCommand());
                    System.exit(1);
                }
            }

            runArrange = CmdLine.hasSwitch("-arrange", args);
            if (runArrange) {
                String grid = CmdLine.findStringArgument("-arrange", args, "SQUARE");
                GridGeometry geom = GridGeometrySpawner.inferFromString(grid);
                if (geom == null) {
                    System.out.println("Warning: unrecognized grid: " + grid + ". Using default SQUARE instead.");
                } else {
                    gridType = geom;
                }
            }

            runAssign = CmdLine.hasSwitch("-assign", args);
            if (runAssign) {
                String method = CmdLine.findStringArgument("-assign", args, "CV");
                boolean found = false;
                for (AgnosticAlignment align : AgnosticAlignment.methods) {
                    if (align.toString().equals(method)) {
                        alignment = align;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    System.out.println("Warning: unrecognized alignment: " + method + ". Using default OPTIMAL instead.");
                }
            }

            runCombine = CmdLine.hasSwitch("-combine", args);
            if (runCombine) {
                String comb = CmdLine.findStringArgument("-combine", args, "ASSIGN");
                if (comb.equals("AGNOSTIC")) {
                    combine = new AgnosticCombination();
                } else if (comb.equals("AGGLOMERATIVE")) {
                    combine = new AgglomerativeCombination();
                }else {
                    if (!comb.equals("ASSIGN")) {
                        System.out.println("Warning: unrecognized combination: " + comb + ". Using default ASSIGN instead.");
                    }
                    combine = new AssignedCombination();
                }
            }
        } catch (CmdLine.InvalidArgumentsException ex) {
            printCommandsAndExit(ex.getMessage());
        }
    }

    private static void printCommandsAndExit(String error) {
        System.out.println("-------------------------------------------------------------------------");
        System.out.println("This program takes a variable number of arguments.");
        System.out.println("Only the -input and -output arguments are required.");
        System.out.println("");
        System.out.println(" -input PATH     Uses PATH as path to an ipe input");
        System.out.println(" -output PATH    Uses PATH as path to the ipe output");
        System.out.println(" -exportall      Enables exporting all intermediate results,");
        System.out.println("                   which only exports the last result computed.");
        System.out.println("");
        System.out.println(" -decompose P D PATH  Runs the decomposition algorithm with");
        System.out.println("                 productivity P and dilation D.");
        System.out.println("                 PATH specifies the location of the CmdMedialAxis program");
        System.out.println("                 program, but can be omitted for guessing standard paths.");
        System.out.println("");
        System.out.println(" -arrange SHAPE  Runs the cell-arrangement algorithm with");
        System.out.println("                   cell shapes specified by SHAPE: value must derive to");
        System.out.println("                   a GridGeometry (see source for details). Typical values are");
        System.out.println("                   HEXAGON, HEXAGON!r!30 and SQUARE.");
        System.out.println("                 Omitting SHAPE uses a SQUARE grid.");
        System.out.println("");
        System.out.println(" -assign ALIGN   Runs the assignment algorithm with alignment");
        System.out.println("                   specified by ALIGN: value must be either");
        System.out.println("                   OPTIMAL (default), CENTROID_VAR or BBOX.");
        System.out.println("                 Omitting ALIGN uses the default value.");
        System.out.println("");
        System.out.println(" -combine METHOD Runs the combine algorithm with method");
        System.out.println("                   specified by METHOD: value must be either");
        System.out.println("                   ASSIGNED (default), AGNOSTIC or AGGLOMERATIVE.");
        System.out.println("                 Omitting ALIGN uses the default value.");
        System.out.println("");
        System.out.println("Omitting earlier steps attempts to read results of the earlier steps");
        System.out.println("  from the input file; for the decomposition step, a trivial");
        System.out.println("  decomposition is used, if this was not found.");
        System.out.println("-------------------------------------------------------------------------");
        System.out.println(error);
        System.exit(1);
    }

    public void execute() {

        System.out.println("------ LOADING DATA --------------------------------");

        SiteMap map = new SiteMap();
        IPE.read(input, map, Stage.INPUT);

        if (runDecomposition) {
            ensure(map, Stage.INPUT);

            PartitionAlgorithm pa = new PartitionAlgorithm(CutType.SHORTEST, dilation, productivity);
            pa.run(map);
        }

        if (runArrange) {
            ensure(map, Stage.PARTITIONED);

            DeformAlgorithm da = new DeformAlgorithm(gridType);
            da.run(map);
        }

        if (runAssign) {
            ensure(map, Stage.DEFORMED);

            AssignAlgorithm aa = new AssignAlgorithm(alignment, null);
            aa.run(map);
        }

        if (runCombine) {
            if (combine.requiresAssigned()) {
                ensure(map, Stage.ASSIGNED);
            } else {
                ensure(map, Stage.DEFORMED);
            }

            CombineAlgorithm ca = new CombineAlgorithm(combine);
            ca.run(map);
        }

        System.out.println("------ EXPORTING RESULT ----------------------------");

        output.getParentFile().mkdirs();
        IPE.write(output, map, exportall ? Stage.ALL_STAGES : map.stage);
        
        Stopwatch.printAndClear();
    }

    private void ensure(SiteMap map, byte stage) {
        while (stage > map.stage) {
            boolean part = Stage.next(map.stage) == Stage.PARTITIONED;
            IPE.read(input, map, Stage.next(map.stage));
            if (part && map.stage < Stage.PARTITIONED) {
                // failed, used trivial instead
                PartitionAlgorithm.trivialPartition(map);
            }
        }
    }
}
