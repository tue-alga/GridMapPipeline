package evaluate;

import arrange.MosaicConstants;
import arrange.util.Random;
import evaluate.sub.AssignTrials;
import evaluate.sub.Bases;
import evaluate.sub.CartogramTrials;
import evaluate.sub.ComposeTrials;
import evaluate.sub.DataStats;
import evaluate.sub.DeformTrials;
import evaluate.sub.FullPipeline;
import evaluate.sub.GridTypes;
import evaluate.sub.OtherFigures;
import evaluate.sub.PartitionTrials;
import evaluate.sub.Teaser;
import java.util.ArrayList;
import java.util.List;
import main.CmdLine;

/**
 *
 * @author Wouter Meulemans
 */
public class Main {

    public static void main(String[] args) throws CmdLine.InvalidArgumentsException {
               
        //runScripts(new OtherFigures());
        //runOutcomes(new FullPipeline());
        //return;
        
        Evaluation[] evals = {        
            new Bases(), // 0
            new DataStats(), // 1
            new Teaser(), // 2
            new PartitionTrials(), // 3
            new DeformTrials(), // 4
            new GridTypes(), // 5
            new AssignTrials(), // 6
            new ComposeTrials(), // 7
            new FullPipeline(), // 8
            new CartogramTrials(), // 9
            new OtherFigures() // 10                
        };
        
        if (CmdLine.hasSwitch("-select", args)) {
            String[] is = CmdLine.findStringArgument("-select", args).split(";");
            Evaluation[] select = new Evaluation[is.length];
            for (int i = 0; i < select.length; i++) {
                select[i] = evals[Integer.parseInt(is[i])];                
                System.out.println("Selecting "+select[i]);
            }
            evals = select;
        }
        
        if (CmdLine.hasSwitch("-scripts", args)) {
            runScripts(evals);
        }
        if (CmdLine.hasSwitch("-outcomes", args)) {
            runOutcomes(evals);
        }
    }
    
    private static void runScripts(Evaluation... evals) {

        System.out.println("---- RUNNING EVALUATIONS ----");
        int err = 0;
        for (Evaluation eval : evals) {

            Random.restart();
            System.gc();
            MosaicConstants.reset();

            System.out.println("Evaluation: " + eval.toString());
            List<Script> scripts = new ArrayList();
            eval.addScripts(scripts);
            int i = 1;

            for (Script script : scripts) {
                System.out.println("  Script " + i + " / " + scripts.size());
                script.printCase();
                script.run();
                if (script.errored) {
                    err++;
                    System.out.println("  << EXCEPTION THROWN >>");
                }
                Random.restart();
                System.gc();
                i++;
            }
        }
        System.out.println("---- DONE EVALUATIONS -------");
        System.out.println("  Errored scripts: " + err);
    }

    private static void runOutcomes(Evaluation... evals) {
        for (Evaluation eval : evals) {
            eval.computeOutcomes();
        }
    }
}
