package evaluate.sub;

import common.Outline;
import common.SiteMap;
import evaluate.Script;
import evaluate.Evaluation;
import io.WKT;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nl.tue.geometrycore.io.ipe.IPEWriter;

/**
 *
 * @author Wouter Meulemans
 */
public class DataStats extends Evaluation {

    public DataStats() {
        super("DataStats", true, false);
    }

    @Override
    public void addScripts(List<Script> scripts) {
        // none
    }

    @Override
    protected void computeOutcomes(IPEWriter write) throws IOException {
        String[] inputs = {"uk", "gb", "nl", "nlmain"};
        Map<String, String> mapnames = new HashMap();
        mapnames.put("nlmain", "NL-main");
        mapnames.put("gb", "GB");
        mapnames.put("nl", "NL");
        mapnames.put("uk", "UK");

        System.out.println("\\bfseries map & \\bfseries outlines & \\bfseries complexity & \\bfseries sites & \\bfseries parts \\\\");
        System.out.println("\\midrule");

        for (String input : inputs) {
            SiteMap map = WKT.read(new File(baseroot + input + ".wkt"));

            int numoutlines = map.outlines.size();
            int numvertices = 0;
            int numsites = 0;
            int numparts = 0;
            for (Outline out : map.outlines) {
                numvertices += out.vertexCount();
                numsites += out.sites.size();
                numparts += out.partitions.size();
            }

            System.out.println(mapnames.get(input) + " & " + numoutlines + " & " + numvertices + " & " + numsites + " & "+ numparts + " \\\\");

        }
    }

}
