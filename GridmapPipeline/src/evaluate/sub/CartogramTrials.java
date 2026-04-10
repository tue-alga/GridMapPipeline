package evaluate.sub;

import evaluate.CartogramScript;
import evaluate.Evaluation;
import evaluate.Script;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import nl.tue.geometrycore.io.ipe.IPEWriter;

/**
 *
 * @author Wouter Meulemans
 */
public class CartogramTrials extends Evaluation {

    public CartogramTrials() {
        super("Cartograms");
    }

    @Override
    public void addScripts(List<Script> scripts) {

        // this uses the original settings
        scripts.add(new CartogramScript(new String[]{
            "-input", dataroot + "USA/usa.ipe",
            "-weights", dataroot + "USA/business_without_employees.tsv", "0.0001",
            "-grid", "SQUARE",
            "-output", outroot + "US_businesses.ipe",
            "-log", outroot + "US_businesses.txt",
            "-pdf", "C:/Program Files/ipe-7.2.24/bin/"
        }));

        scripts.add(new CartogramScript(new String[]{
            "-input", dataroot + "USA/usa-starbucks.ipe",
            "-weights", dataroot + "USA/starbucks.tsv", "1",
            "-grid", "HEXAGON",
            "-output", outroot + "US_starbucks.ipe",
            "-log", outroot + "US_starbucks.txt",
            "-pdf", "C:/Program Files/ipe-7.2.24/bin/",
            "-dark",
            "-boundaries"
        }));

        scripts.add(new CartogramScript(new String[]{
            "-input", dataroot + "EU/europe-noislands.ipe",
            "-weights", dataroot + "EU/population.tsv", "0.000008",
            "-grid", "HEXAGON",
            "-output", outroot + "EU_population.ipe",
            "-log", outroot + "EU_population.txt",
            "-pdf", "C:/Program Files/ipe-7.2.24/bin/"
        }));
    }

    private class Result {

        // setting
        String map;
        boolean old;
        // times
        int total;
        int embedder;
        int guides;
        int slides;
        int force;
        int reshape;
        int finalizer;

        void initSetting(String name) {
            old = name.contains("old");
            if (old) {
                map = name.substring(0, name.indexOf("-"));
            } else {
                map = name.substring(0, name.indexOf("."));
            }
        }

    }

    @Override
    protected void computeOutcomes(IPEWriter write) throws IOException {
        runningTimes();
    }

    private void runningTimes() throws IOException {
        List<Result> dss = new ArrayList();
        HashSet<String> maps = new HashSet();
        for (File f : collectLogFiles()) {

            List<String> lines = Files.readAllLines(f.toPath());
            Result ds = new Result();
            ds.initSetting(f.getName());
            int off = ds.old ? 1 : 1;
            ds.total = readTotalTiming(lines.get(lines.size() - 8 - off));
            ds.embedder = readTotalTiming(lines.get(lines.size() - 7 - off));
            ds.guides = readTotalTiming(lines.get(lines.size() - 6 - off));
            ds.slides = readTotalTiming(lines.get(lines.size() - 5 - off));
            ds.force = readTotalTiming(lines.get(lines.size() - 4 - off));
            ds.reshape = readTotalTiming(lines.get(lines.size() - 3 - off));
            ds.finalizer = readTotalTiming(lines.get(lines.size() - 2 - off));
            dss.add(ds);
            maps.add(ds.map);
        }

        dss.sort((a, b) -> {
            int r = a.map.compareTo(b.map);
            if (r != 0) {
                return r;
            }

            r = Boolean.compare(a.old, b.old);
            if (r != 0) {
                return r;
            }

            return 0;

        });

        for (String map : maps) {

            List<Result> oldres = filter(dss, (Result ps) -> ps.map.equals(map) && ps.old);
            List<Result> newres = filter(dss, (Result ps) -> ps.map.equals(map) && !ps.old);

            double oldavg = average(oldres, (Result ps) -> (double) ps.total) / 1000.0;
            double newavg = average(newres, (Result ps) -> (double) ps.total) / 1000.0;

            System.out.println("Map: " + map);
            System.out.println("  ratio: " + df3digits.format(oldavg / newavg));
            System.out.println("     > " + df3digits.format(oldavg) + " s");
            System.out.println("     > " + df3digits.format(newavg) + " s");
        }

    }

}
