package combine;

import common.SiteMap;
import common.util.Stopwatch;

/**
 *
 * @author Wouter Meulemans
 */
public class CombineAlgorithm {

    private final Combination method;

    public CombineAlgorithm(Combination method) {
        this.method = method;
    }

    public void run(SiteMap map) {
        System.out.println("------ COMBINING OUTLINES --------------------------");
        System.out.println("  method = " + method);

        Stopwatch sw = Stopwatch.get("combine").start();

        method.run(map);

        sw.stop();
    }
}
