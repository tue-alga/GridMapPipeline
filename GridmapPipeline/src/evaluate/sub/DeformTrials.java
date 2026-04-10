package evaluate.sub;

import arrange.DeformAlgorithm;
import common.gridmath.GridGeometry;
import common.gridmath.grids.HexagonGeometry;
import common.gridmath.grids.SquareGeometry;
import evaluate.Script;
import evaluate.Evaluation;
import evaluate.GridmapScript;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Rectangle;
import nl.tue.geometrycore.geometryrendering.styling.Dashing;
import nl.tue.geometrycore.geometryrendering.styling.ExtendedColors;
import nl.tue.geometrycore.geometryrendering.styling.Hashures;
import nl.tue.geometrycore.geometryrendering.styling.TextAnchor;
import nl.tue.geometrycore.io.ipe.IPEWriter;

/**
 *
 * @author Wouter Meulemans
 */
public class DeformTrials extends Evaluation {

    public DeformTrials() {
        super("DeformTrials");
    }

    @Override
    public void addScripts(List<Script> scripts) {

        GridGeometry[] geoms = {
            new SquareGeometry(),
            new HexagonGeometry()
        };
        String[] inputs = {
            "gb",
            "uk",
            "nlmain",
            "nl"
        };

        int reps = 3;

        for (GridGeometry geom : geoms) {
            DeformAlgorithm deform = new DeformAlgorithm(geom);

            for (String input : inputs) {

                String cs = input + "-" + geom.toString();

                GridmapScript s = new GridmapScript(baseroot + input + ".wkt", outroot + cs);
                s.setDeform(deform);
                s.setRepetitions(reps);
                scripts.add(s);
            }
        }
    }

    @Override
    protected void computeOutcomes(IPEWriter write) throws IOException {

        double chartwidth = 85;
        double barheight = 8;
        double hsep = 16;
        double vsep = 16;
        double gap = 1;
        double top_offset = 4;
        double bottom_offset = 2;
        double left_offset = 2;

        List<Result> dss = new ArrayList();
        for (File f : collectLogFiles()) {

            List<String> lines = Files.readAllLines(f.toPath());
            Result ds = new Result();
            ds.initSetting(f.getName());
            int off = ds.old ? 1 : 0;
            ds.total = readTotalTiming(lines.get(lines.size() - 8 - off));
            ds.embedder = readTotalTiming(lines.get(lines.size() - 7 - off));
            ds.guides = readTotalTiming(lines.get(lines.size() - 6 - off));
            ds.slides = readTotalTiming(lines.get(lines.size() - 5 - off));
            ds.force = readTotalTiming(lines.get(lines.size() - 4 - off));
            ds.reshape = readTotalTiming(lines.get(lines.size() - 3 - off));
            ds.finalizer = readTotalTiming(lines.get(lines.size() - 2 - off));
            dss.add(ds);
        }

        dss.sort((a, b) -> {
            int r = a.map.compareTo(b.map);
            if (r != 0) {
                return r;
            }

            r = -Boolean.compare(a.hex, b.hex);
            if (r != 0) {
                return r;
            }

            r = Boolean.compare(a.old, b.old);
            if (r != 0) {
                return r;
            }

            return 0;

        });

        String[] maps = {"nlmain", "nl", "gb", "uk"};
        Map<String, String> mapnames = new HashMap();
        mapnames.put("nlmain", "NL-main");
        mapnames.put("nl", "NL");
        mapnames.put("gb", "GB");
        mapnames.put("uk", "UK");
        int reps = 3;

        {

            write.newPage(maps);

            int mapi = -1;
            for (String map : maps) {
                mapi++;
                
                int row = mapi % 2;
                int col = mapi / 2;
                
                write.setLayer(map);

                List<Result> mdss = filter(dss, (Result ds) -> ds.map.equals(map));

                int i = 0;
                int max = maxint(mdss, (Result ds) -> ds.total);

                int maxSec = totalMillisToAvgSec(max, reps) + 1;
                if (maxSec % 5 > 0) {
                    maxSec += (5 - maxSec % 5);
                }
                max = avgSecToTotalMillis(maxSec, reps);
                
                int numbars = mdss.size();

                Vector origin = new Vector(100 + col * (chartwidth + hsep), 500 - row * (barheight * numbars + (numbars+1) * gap + vsep));
                Rectangle area = Rectangle.byCornerAndSize(origin, chartwidth, barheight * numbars + gap * (numbars + 1));

                for (Result ds : mdss) {
                    double t = origin.getX();
                    double bottom = origin.getY() + gap + (barheight + gap) * i;

                    if (ds.old && col == 0) {
                        write.setStroke(ExtendedColors.black, 0.4, Dashing.SOLID);
                        write.setFill(null, Hashures.SOLID);
                        write.setTextStyle(TextAnchor.RIGHT, 9);
                        write.draw(new Vector(t - left_offset, bottom + barheight / 2.0), "MSS");
                        write.draw(new Vector(t - left_offset, bottom - gap - barheight + barheight / 2.0), "new");
                        write.setTextStyle(TextAnchor.LEFT, 9);
                        write.draw(new Vector(t - 40, bottom - gap / 2.0), ds.hex ? "Hex" : "Sqr");
                    }

                    writeBar(write,
                            origin.getX(), origin.getY() + gap + (barheight + gap) * i,
                            barheight, chartwidth, max,
                            ds.total,
                            new int[]{
                                ds.embedder,
                                ds.guides,
                                ds.slides,
                                ds.force,
                                ds.reshape,
                                ds.finalizer
                            },
                            new Color[]{
                                ExtendedColors.darkRed,
                                ExtendedColors.darkBlue,
                                ExtendedColors.darkGreen,
                                ExtendedColors.darkPurple,
                                ExtendedColors.darkOrange,
                                ExtendedColors.lightRed
                            }
                    );
                    i++;
                }

                if (mapi == 1) {
                    String[] legend = {"Embedder", "Guides", "Slides", "Force", "Reshape", "Finalize", "Other"};
                    Color[] colors = {
                        ExtendedColors.darkRed,
                        ExtendedColors.darkBlue,
                        ExtendedColors.darkGreen,
                        ExtendedColors.darkPurple,
                        ExtendedColors.darkOrange,
                        ExtendedColors.lightRed,
                        ExtendedColors.darkGray
                    };
                    for (int L = 0; L < legend.length; L++) {
                        int lcol = L % 4;
                        int lrow = L / 4;
                        Vector lb = Vector.addSeq(area.leftBottom(), Vector.down(vsep*1.5 + lrow * (barheight+gap)), Vector.right(0 + lcol * 50));
                        Rectangle R = Rectangle.byCornerAndSize(lb, barheight, barheight);
                        write.setStroke(null, 0.4, Dashing.SOLID);
                        write.setFill(colors[L], Hashures.SOLID);
                        write.draw(R);
                        write.setStroke(Color.black, 0.4, Dashing.SOLID);
                        write.setTextStyle(TextAnchor.LEFT, 7);
                        write.draw(Vector.add(R.rightBottom(), new Vector(left_offset, barheight / 2.0)), legend[L]);
                    }
                }

                write.setFill(null, Hashures.SOLID);
                write.setStroke(ExtendedColors.black, 0.4, Dashing.SOLID);
                write.draw(area);

                write.setTextStyle(TextAnchor.BASELINE_CENTER, 9);
                write.draw(Vector.add(area.topSide().getPointAt(0.5), Vector.up(top_offset)), mapnames.get(map));

                write.setTextStyle(TextAnchor.TOP, 9);
                write.draw(Vector.add(area.leftBottom(), Vector.down(bottom_offset)), "0");
                write.draw(Vector.add(area.rightBottom(), Vector.down(bottom_offset)), "" + maxSec);
            }
        }

        for (String map : maps) {

            List<Result> oldres = filter(dss, (Result ps) -> ps.map.equals(map) && ps.old);
            List<Result> newres = filter(dss, (Result ps) -> ps.map.equals(map) && !ps.old);

            double oldavg = average(oldres, (Result ps) -> (double) ps.total) / reps;
            double newavg = average(newres, (Result ps) -> (double) ps.total) / reps;
                        
            double oldavg_sqr = average(filter(oldres, (ps) -> !ps.hex), (Result ps) -> (double) ps.total) / reps;
            double newavg_sqr = average(filter(newres, (ps) -> !ps.hex), (Result ps) -> (double) ps.total) / reps;
            
            double oldavg_hex = average(filter(oldres, (ps) -> ps.hex), (Result ps) -> (double) ps.total) / reps;
            double newavg_hex = average(filter(newres, (ps) -> ps.hex), (Result ps) -> (double) ps.total) / reps;

            System.out.println("Map: " + map);
            System.out.println("  all: " + df3digits.format(oldavg / newavg));
            System.out.println("     > " + df3digits.format(oldavg) + " ms");
            System.out.println("     > " + df3digits.format(newavg) + " ms");
            System.out.println("  sqr: " + df3digits.format(oldavg_sqr / newavg_sqr));
            System.out.println("     > " + df3digits.format(oldavg_sqr) + " ms");
            System.out.println("     > " + df3digits.format(newavg_sqr) + " ms");
            System.out.println("  hex: " + df3digits.format(oldavg_hex / newavg_hex));
            System.out.println("     > " + df3digits.format(oldavg_hex) + " ms");
            System.out.println("     > " + df3digits.format(newavg_hex) + " ms");
        }
    }

    private class Result {

        // setting
        String map;
        boolean old;
        boolean hex;
        // times
        int total;
        int embedder;
        int guides;
        int slides;
        int force;
        int reshape;
        int finalizer;

        void initSetting(String name) {
            map = name.substring(0, name.indexOf("-"));
            old = name.contains("old");
            hex = name.contains("HEXAGON");
        }

    }
}
