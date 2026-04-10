package evaluate;

import analyze.Analyzer;
import arrange.DeformAlgorithm;
import assign.AssignAlgorithm;
import combine.CombineAlgorithm;
import common.SiteMap;
import common.util.Stopwatch;
import io.IO;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import partition.PartitionAlgorithm;

/**
 *
 * @author Wouter Meulemans
 */
public class GridmapScript extends Script {

    private final String input;
    private final String output;
    private PartitionAlgorithm partition = null;
    private DeformAlgorithm deform = null;
    private AssignAlgorithm assign = null;
    private CombineAlgorithm combine = null;
    private Analyzer[] analyzers = null;
    private int repetitions = 1;

    public GridmapScript(String input, String output) {
        this.input = input;
        this.output = output;
    }

    public void setPartition(PartitionAlgorithm partition) {
        this.partition = partition;
    }

    public void setDeform(DeformAlgorithm deform) {
        this.deform = deform;
    }

    public void setAssign(AssignAlgorithm assign) {
        this.assign = assign;
    }

    public void setCombine(CombineAlgorithm combine) {
        this.combine = combine;
    }

    public void setAnalyzers(Analyzer[] analyzers) {
        this.analyzers = analyzers;
    }

    public void setRepetitions(int repetitions) {
        this.repetitions = repetitions;
    }

    @Override
    public void run() {

        PrintStream oldps = System.out;
        try {
            File f = new File(output + ".txt");
            f.getParentFile().mkdirs();
            System.setOut(new PrintStream(f));
        } catch (FileNotFoundException ex) {
            System.err.println("" + ex.getMessage());
            System.setOut(oldps);
        }

        SiteMap map = null;
        try {
            int reps = repetitions;
            do {
                map = IO.read(new File(input));
                if (partition != null) {
                    partition.run(map);
                }
                if (deform != null) {
                    deform.run(map);
                }
                if (assign != null) {
                    assign.run(map);
                }
                if (combine != null) {
                    combine.run(map);
                }
                reps--;
            } while (reps > 0);

            if (analyzers != null) {
                System.out.println("------ ANALYZERS ----------------------------------");
                for (Analyzer a : analyzers) {
                    a.run(map);
                }
            }

            IO.write(new File(output + ".wkt"), map);

            Stopwatch.printAndClear();

        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace(System.out);
            errored = true;
        }

        System.setOut(oldps);
    }

    @Override
    public void printCase() {
        System.out.println("  in: " + input);
        System.out.println("  out: " + output);
    }

}
