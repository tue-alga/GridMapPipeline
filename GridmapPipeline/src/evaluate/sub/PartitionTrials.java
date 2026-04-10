package evaluate.sub;

import common.Partition;
import common.SiteMap;
import evaluate.Script;
import evaluate.Evaluation;
import evaluate.GridmapScript;
import io.WKT;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import nl.tue.geometrycore.geometry.CyclicGeometry;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.LineSegment;
import nl.tue.geometrycore.geometry.linear.Rectangle;
import nl.tue.geometrycore.geometryrendering.styling.Dashing;
import nl.tue.geometrycore.geometryrendering.styling.ExtendedColors;
import nl.tue.geometrycore.geometryrendering.styling.Hashures;
import nl.tue.geometrycore.geometryrendering.styling.TextAnchor;
import nl.tue.geometrycore.io.ipe.IPEWriter;
import nl.tue.geometrycore.util.DoubleUtil;
import nl.tue.geometrycore.util.IntegerUtil;
import partition.CutType;
import partition.PartitionAlgorithm;

/**
 *
 * @author Wouter Meulemans
 */
public class PartitionTrials extends Evaluation {

    private final int[] prods = {1, 5, 10, 20, 30};
    private final double[] dils = {1, 3, 5};
    private final int reps = 3;
    private final CutType[] cts = CutType.values();
    private final TestMap[] maps = {
        new TestMap("gb", "GB", "GB_constituencies.wkt"),
        new TestMap("nlmain", "NL-main", "NL_municipalities2017_main.wkt")
    };

    private class TestMap {

        String tag;
        String file;
        String name;

        public TestMap(String tag, String name, String file) {
            this.tag = tag;
            this.name = name;
            this.file = file;
        }
    }

    public PartitionTrials() {
        super("PartitionTrials");
    }

    @Override
    public void addScripts(List<Script> scripts) {

        for (double dil : dils) {
            for (int prod : prods) {
                for (CutType ct : cts) {
                    PartitionAlgorithm partition = new PartitionAlgorithm(ct, dil, prod);

                    for (TestMap map : maps) {
                        String cs = map.tag + "-" + ct + "-" + dil + "-" + prod;

                        GridmapScript s = new GridmapScript(dataroot + map.file, outroot + cs);
                        s.setPartition(partition);
                        s.setRepetitions(reps);
                        scripts.add(s);
                    }
                }
            }
        }
    }

    @Override
    protected void computeOutcomes(IPEWriter write) throws IOException {
        speedCharts(write);
        cutTypes();
        cutTypeSpeeds();
    }

    private void speedCharts(IPEWriter write) throws IOException {
        double chartwidth = 85;
        double barheight = 8;
        double hsep = 8;
        double vsep = 12;
        double gap = 1;
        double top_offset = 4;
        double bottom_offset = 2;
        double left_offset = 2;

        List<Result> pss = new ArrayList();

        for (File f : collectFiles((File ff) -> ff.getName().endsWith(".txt") && ff.getName().contains("ENDPOINTS"))) {
            List<String> lines = Files.readAllLines(f.toPath());
            Result ps = new Result();
            ps.initSetting(f.getName());
            ps.total = readTotalTiming(lines.get(lines.size() - 6));
            ps.cuts = readTotalTiming(lines.get(lines.size() - 5));
            ps.init = readTotalTiming(lines.get(lines.size() - 4));
            ps.perform = readTotalTiming(lines.get(lines.size() - 3));
            ps.construct = readTotalTiming(lines.get(lines.size() - 2));
            readAppliedCuts(ps, lines);
            pss.add(ps);
        }

        pss.sort((a, b) -> {
            int r = a.map.compareTo(b.map);
            if (r != 0) {
                return r;
            }

            r = -Double.compare(a.dilation, b.dilation);
            if (r != 0) {
                return r;
            }

            r = -Integer.compare(a.productivity, b.productivity);
            if (r != 0) {
                return r;
            }

            r = Boolean.compare(a.old, b.old);
            if (r != 0) {
                return r;
            }

            return 0;

        });

        String[] alltags = new String[maps.length];
        for (int i = 0; i < alltags.length; i++) {
            alltags[i] = maps[i].tag;
        }

        {
            double d_off = 24;
            write.newPage(alltags);

            int mapi = -1;
            for (TestMap map : maps) {
                mapi++;
                write.setLayer(map.tag);

                List<Result> mpss = filter(pss, (Result ps) -> ps.map.equals(map.tag));

                int i = 0;
                int max = maxint(mpss, (Result ps) -> ps.total);

                int maxSec = totalMillisToAvgSec(max, reps) + 1;
                max = avgSecToTotalMillis(maxSec, reps);

                Vector origin = new Vector(100 + (chartwidth + hsep) * mapi, 54);
                Rectangle area = Rectangle.byCornerAndSize(origin, chartwidth, barheight * dils.length * 2 + gap * (dils.length * 2 + 1));

                for (double d : reverse(dils)) {
                    for (boolean old : new boolean[]{false, true}) {

                        double t = origin.getX();
                        double bottom = origin.getY() + gap + (barheight + gap) * i;

                        List<Result> agg = filter(mpss, (Result ps) -> ps.old == old && DoubleUtil.close(ps.dilation, d));

                        int total = 0;
                        int cuts = 0;
                        int init = 0;
                        int perform = 0;
                        int construct = 0;

                        for (Result ps : agg) {
                            total += ps.total;
                            cuts += ps.cuts;
                            init += ps.init;
                            perform += ps.perform;
                            construct += ps.construct;
                        }

                        if (mapi == 0 && old) {
                            write.setStroke(ExtendedColors.black, 0.4, Dashing.SOLID);
                            write.setFill(null, Hashures.SOLID);
                            write.setTextStyle(TextAnchor.RIGHT, labelsize);
                            write.draw(new Vector(t - left_offset, bottom + barheight / 2.0), "MSS");
                            write.draw(new Vector(t - left_offset, bottom - gap - barheight + barheight / 2.0), "new");
                            write.setTextStyle(TextAnchor.RIGHT, labelsize);
                            write.draw(new Vector(t - d_off, bottom - gap / 2.0), "" + (int) d);
                        }

                        write.setStroke(null, 0, Dashing.SOLID);

                        writeBar(write,
                                origin.getX(), origin.getY() + gap + (barheight + gap) * i,
                                barheight, chartwidth,
                                max * agg.size(), total,
                                new int[]{
                                    cuts,
                                    init,
                                    perform,
                                    construct
                                },
                                new Color[]{
                                    ExtendedColors.darkRed,
                                    ExtendedColors.darkBlue,
                                    ExtendedColors.darkOrange,
                                    ExtendedColors.darkGreen
                                });

                        i++;
                    }
                }

                write.setFill(null, Hashures.SOLID);
                write.setStroke(ExtendedColors.black, 0.4, Dashing.SOLID);
                write.draw(area);

                write.setTextStyle(TextAnchor.BASELINE_CENTER, fontsize);
                write.draw(Vector.add(area.topSide().getPointAt(0.5), Vector.up(top_offset)), map.name);

                write.setTextStyle(TextAnchor.TOP, labelsize);
                write.draw(Vector.add(area.leftBottom(), Vector.down(bottom_offset)), "0");
//                write.setTextStyle(TextAnchor.TOP, 9);
//                write.draw(Vector.add(ls.getEnd(), Vector.down(bottom_offset)), "" + (mapi + 1));
                write.setTextStyle(TextAnchor.TOP, labelsize);
                write.draw(Vector.add(area.rightBottom(), Vector.down(bottom_offset)), "" + maxSec);

                if (mapi == 0) {
                    write.setTextStyle(TextAnchor.BASELINE_RIGHT, 9);
                    write.draw(new Vector(origin.getX() - d_off, area.getTop() + top_offset), "$d$");

                    String[] legend = {"Cuts", "Init", "Perform", "Construct", "Other"};
                    Color[] colors = {
                        ExtendedColors.darkRed,
                        ExtendedColors.darkBlue,
                        ExtendedColors.darkOrange,
                        ExtendedColors.darkGreen,
                        ExtendedColors.darkGray
                    };
                    double[] offs = {
                        0,
                        35,
                        70,
                        120,
                        175
                    };
                    for (int L = 0; L < legend.length; L++) {
                        Vector lb = Vector.addSeq(area.leftBottom(), Vector.right(offs[L] - d_off), Vector.down(vsep));
                        Rectangle R = Rectangle.byCornerAndSize(lb, barheight, -barheight);
                        write.setStroke(null, 0.4, Dashing.SOLID);
                        write.setFill(colors[L], Hashures.SOLID);
                        write.draw(R);
                        write.setStroke(Color.black, 0.4, Dashing.SOLID);
                        write.setTextStyle(TextAnchor.LEFT, labelsize);
                        write.draw(Vector.add(R.rightBottom(), new Vector(left_offset, barheight / 2.0)), legend[L]);
                    }
                }
            }
        }

        {
            write.newPage(alltags);

            int mapi = -1;
            for (TestMap map : maps) {
                mapi++;
                write.setLayer(map.tag);

                List<Result> mpss = filter(pss, (Result ps) -> ps.map.equals(map.tag));

                int i = 0;
                int max = maxint(mpss, (Result ps) -> ps.total);

                int maxSec = totalMillisToAvgSec(max, reps) + 1;
//                if (maxSec % 5 > 0) {
//                    maxSec += (5 - maxSec % 5);
//                }
                max = avgSecToTotalMillis(maxSec, reps);

                Vector origin = new Vector(100 + (chartwidth + hsep) * mapi, 16);
                Rectangle area = Rectangle.byCornerAndSize(origin, chartwidth, barheight * mpss.size() + gap * (mpss.size() + 1));

                double d_off = 40;
                double p_off = 24;

                for (Result ps : mpss) {
                    double t = origin.getX();
                    double bottom = origin.getY() + gap + (barheight + gap) * i;

                    if (mapi == 0 && ps.old) {
                        write.setStroke(ExtendedColors.black, 0.4, Dashing.SOLID);
                        write.setFill(null, Hashures.SOLID);
                        write.setTextStyle(TextAnchor.RIGHT, labelsize);
                        write.draw(new Vector(t - left_offset, bottom + barheight / 2.0), "MSS");
                        write.draw(new Vector(t - left_offset, bottom - gap - barheight + barheight / 2.0), "new");
                        write.setTextStyle(TextAnchor.RIGHT, labelsize);
                        write.draw(new Vector(t - d_off, bottom - gap / 2.0), "" + (int) ps.dilation);
                        write.draw(new Vector(t - p_off, bottom - gap / 2.0), "" + ps.productivity);
                    }

                    write.setStroke(null, 0, Dashing.SOLID);

                    writeBar(write,
                            origin.getX(), origin.getY() + gap + (barheight + gap) * i,
                            barheight, chartwidth,
                            max, ps.total,
                            new int[]{
                                ps.cuts,
                                ps.init,
                                ps.perform,
                                ps.construct
                            },
                            new Color[]{
                                ExtendedColors.darkRed,
                                ExtendedColors.darkBlue,
                                ExtendedColors.darkOrange,
                                ExtendedColors.darkGreen
                            });

                    i++;
                }

                if (mapi == 1) {
                    String[] legend = {"Cuts", "Init", "Perform", "Construct", "Other"};
                    Color[] colors = {
                        ExtendedColors.darkRed,
                        ExtendedColors.darkBlue,
                        ExtendedColors.darkOrange,
                        ExtendedColors.darkGreen,
                        ExtendedColors.darkGray
                    };
                    for (int L = 0; L < legend.length; L++) {
                        Vector rb = Vector.addSeq(area.rightBottom(), Vector.left(left_offset), Vector.up(left_offset + L * barheight + L * gap));
                        Rectangle R = Rectangle.byCornerAndSize(rb, -barheight, barheight);
                        write.setStroke(null, 0.4, Dashing.SOLID);
                        write.setFill(colors[legend.length - 1 - L], Hashures.SOLID);
                        write.draw(R);
                        write.setStroke(Color.black, 0.4, Dashing.SOLID);
                        write.setTextStyle(TextAnchor.RIGHT, labelsize);
                        write.draw(Vector.add(R.leftBottom(), new Vector(-left_offset, barheight / 2.0)), legend[legend.length - 1 - L]);
                    }
                }

                write.setStroke(ExtendedColors.darkGray, 0.4, Dashing.SOLID);
                write.setFill(null, Hashures.SOLID);
//                LineSegment ls = area.leftSide();
//                ls.translate(Vector.right(chartwidth * avgSecToTotalMillis(mapi + 1, reps) / max));
//                write.draw(ls);

                write.setStroke(ExtendedColors.black, 0.4, Dashing.SOLID);
                write.draw(area);

                write.configureTextHandling(false, 11, false);
                write.setTextSerifs(true);

                write.setTextStyle(TextAnchor.BASELINE_CENTER, fontsize);
                write.draw(Vector.add(area.topSide().getPointAt(0.5), Vector.up(top_offset)), map.name);

                write.setTextStyle(TextAnchor.TOP, labelsize);
                write.draw(Vector.add(area.leftBottom(), Vector.down(bottom_offset)), "0");
//                write.setTextStyle(TextAnchor.TOP, 9);
//                write.draw(Vector.add(ls.getEnd(), Vector.down(bottom_offset)), "" + (mapi + 1));
                write.setTextStyle(TextAnchor.TOP, labelsize);
                write.draw(Vector.add(area.rightBottom(), Vector.down(bottom_offset)), "" + maxSec);
                if (mapi == 0) {
                    write.setTextStyle(TextAnchor.BASELINE_RIGHT, labelsize);
                    write.draw(new Vector(origin.getX() - d_off, area.getTop() + top_offset), "$d$");
                    write.draw(new Vector(origin.getX() - p_off, area.getTop() + top_offset), "$p$");
                }
            }
        }

        {
            write.newPage(alltags);

            int mapi = -1;
            for (TestMap map : maps) {
                mapi++;
                write.setLayer(map.tag);

                List<Result> mpss = filter(pss, (Result ps) -> ps.map.equals(map.tag) && !ps.old);

                int i = 0;
                int max = maxint(mpss, (Result ps) -> ps.total);

                int mult = 500;
                if (max % (reps * mult) > 0) {
                    max += reps * mult - (max % (reps * mult));
                }
                double maxSec = (max / reps) / 1000.0;

                Vector origin = new Vector(100 + (chartwidth + hsep) * mapi, 16);
                Rectangle area = Rectangle.byCornerAndSize(origin, chartwidth, barheight * mpss.size() + gap * (mpss.size() + 1));

                double d_off = 40;
                double p_off = 24;

                for (Result ps : mpss) {
                    double t = origin.getX();
                    double bottom = origin.getY() + gap + (barheight + gap) * i;

                    if (mapi == 0) {
                        write.setStroke(ExtendedColors.black, 0.4, Dashing.SOLID);
                        write.setFill(null, Hashures.SOLID);
                        write.setTextStyle(TextAnchor.RIGHT, labelsize);
                        write.draw(new Vector(t - (d_off - p_off), bottom + barheight / 2.0), "" + (int) ps.dilation);
                        write.draw(new Vector(t - left_offset, bottom + barheight / 2.0), "" + ps.productivity);
                    }

                    write.setStroke(null, 0, Dashing.SOLID);

                    writeBar(write,
                            origin.getX(), origin.getY() + gap + (barheight + gap) * i,
                            barheight, chartwidth,
                            max, ps.total,
                            new int[]{
                                ps.cuts,
                                ps.init,
                                ps.perform,
                                ps.construct
                            },
                            new Color[]{
                                ExtendedColors.darkRed,
                                ExtendedColors.darkBlue,
                                ExtendedColors.darkOrange,
                                ExtendedColors.darkGreen
                            });

                    i++;
                }

                if (mapi == 1) {
                    String[] legend = {"Cuts", "Init", "Perform", "Construct", "Other"};
                    Color[] colors = {
                        ExtendedColors.darkRed,
                        ExtendedColors.darkBlue,
                        ExtendedColors.darkOrange,
                        ExtendedColors.darkGreen,
                        ExtendedColors.darkGray
                    };
                    for (int L = 0; L < legend.length; L++) {
                        Vector rb = Vector.addSeq(area.rightBottom(), Vector.left(left_offset), Vector.up(left_offset + L * barheight + L * gap));
                        Rectangle R = Rectangle.byCornerAndSize(rb, -barheight, barheight);
                        write.setStroke(null, 0.4, Dashing.SOLID);
                        write.setFill(colors[legend.length - 1 - L], Hashures.SOLID);
                        write.draw(R);
                        write.setStroke(Color.black, 0.4, Dashing.SOLID);
                        write.setTextStyle(TextAnchor.RIGHT, labelsize);
                        write.draw(Vector.add(R.leftBottom(), new Vector(-left_offset, barheight / 2.0)), legend[legend.length - 1 - L]);
                    }
                }

                write.setStroke(ExtendedColors.darkGray, 0.4, Dashing.SOLID);
                write.setFill(null, Hashures.SOLID);
//                LineSegment ls = area.leftSide();
//                ls.translate(Vector.right(chartwidth * avgSecToTotalMillis(mapi + 1, reps) / max));
//                write.draw(ls);

                write.setStroke(ExtendedColors.black, 0.4, Dashing.SOLID);
                write.draw(area);

                write.configureTextHandling(false, 11, false);
                write.setTextSerifs(true);

                write.setTextStyle(TextAnchor.BASELINE_CENTER, fontsize);
                write.draw(Vector.add(area.topSide().getPointAt(0.5), Vector.up(top_offset)), map.name);

                write.setTextStyle(TextAnchor.TOP, labelsize);
                write.draw(Vector.add(area.leftBottom(), Vector.down(bottom_offset)), "0");
//                write.setTextStyle(TextAnchor.TOP, 9);
//                write.draw(Vector.add(ls.getEnd(), Vector.down(bottom_offset)), "" + (mapi + 1));
                write.setTextStyle(TextAnchor.TOP, labelsize);
                write.draw(Vector.add(area.rightBottom(), Vector.down(bottom_offset)), "" + maxSec);

                if (mapi == 0) {
                    write.setTextStyle(TextAnchor.BASELINE_RIGHT, labelsize);
                    write.draw(new Vector(origin.getX() - (d_off - p_off), area.getTop() + top_offset), "$d$");
                    write.draw(new Vector(origin.getX() - left_offset, area.getTop() + top_offset), "$p$");
                }
            }
        }

        for (TestMap map : maps) {

            List<Result> oldres = filter(pss, (Result ps) -> ps.map.equals(map.tag) && ps.old);
            List<Result> newres = filter(pss, (Result ps) -> ps.map.equals(map.tag) && !ps.old);

            double oldavg = average(oldres, (Result ps) -> (double) ps.total) / reps;
            double newavg = average(newres, (Result ps) -> (double) ps.total) / reps;

            double oldavg2 = average(filter(oldres, (ps) -> ps.dilation > 1.0001), (Result ps) -> (double) ps.total) / reps;
            double newavg2 = average(filter(newres, (ps) -> ps.dilation > 1.0001), (Result ps) -> (double) ps.total) / reps;

            System.out.println("Map: " + map.name);
            System.out.println("  all: " + df3digits.format(oldavg / newavg));
            System.out.println("     > " + df3digits.format(oldavg) + " ms");
            System.out.println("     > " + df3digits.format(newavg) + " ms");
            System.out.println("  no d=1: " + df3digits.format(oldavg2 / newavg2));
            System.out.println("     > " + df3digits.format(oldavg2) + " ms");
            System.out.println("     > " + df3digits.format(newavg2) + " ms");
        }
    }

    private class Result {

        // setting
        String map;
        boolean old;
        double dilation;
        int productivity;
        CutType ct;
        // timings
        int total;
        int cuts;
        int init;
        int perform;
        int construct;
        // data about cuts
        int numFound;
        int numRemaining;
        int numApplied;

        void initSetting(String name) {
            map = name.substring(0, name.indexOf("-"));
            old = name.contains("old");
            String[] ss = name.substring(0, name.length() - 4).split("-");
            dilation = Double.parseDouble(ss[ss.length - 2]);
            productivity = Integer.parseInt(ss[ss.length - 1]);

            ct = null;
            for (CutType c : cts) {
                if (c.toString().equals(ss[1])) {
                    ct = c;
                }
            }
            if (ct == null) {
                System.out.println("No cuttype: " + ss[1]);
            }

        }

    }

    private void readAppliedCuts(Result ps, List<String> lines) {

        ps.numFound = Integer.parseInt(lines.get(3).substring(2).split(" ")[0]);
        ps.numRemaining = Integer.parseInt(lines.get(4).substring(2).split(" ")[0]);
        ps.numApplied = Integer.parseInt(lines.get(6).substring(2).split(" ")[0]);
    }

    private void cutTypes() {

        int[][] wins = new int[cts.length][4];
        for (int i = 0; i < wins.length; i++) {
            for (int j = 0; j < wins[i].length; j++) {
                wins[i][j] = 0;
            }
        }

        int cases = 0;
        for (int p : prods) {
            if (p == 1) {
                continue;
            }
            for (double d : dils) {
                if (d <= 1.001) {
                    continue;
                }
                for (TestMap map : maps) {

                    cutTypes(p, d, map, wins);
                    cases++;
                }
            }
        }

        System.out.println("");
        System.out.println("TOTAL WINS out of " + cases + " cases");
        System.out.println("  Name       Cuts  Fatness  Dilation  Length");
        for (int i = 0; i < cts.length; i++) {
            String name = cts[i].name();
            System.out.print("  " + name + (name.length() < 9 ? " " : ""));

            System.out.print("  " + wins[i][0]);
            System.out.print("    " + wins[i][1]);
            System.out.print("          " + wins[i][2]);
            System.out.print("         " + wins[i][3]);

            System.out.println("");
        }
    }

    private void cutTypes(int p, double d, TestMap map, int[][] wins) {

        System.out.println(map.tag + ", d = " + d + ", p = " + p);
        SiteMap[] sitemaps = new SiteMap[cts.length];
        double[] fatness = new double[cts.length];
        double[] dilation = new double[cts.length];
        double[] length = new double[cts.length];
        int[] numcuts = new int[cts.length];
        for (int i = 0; i < cts.length; i++) {
            String name = cts[i].name();
            sitemaps[i] = WKT.read(new File(outroot + map.tag + "-" + name + "-" + d + "-" + p + ".wkt"));

            double fat = 0;
            double dil = 0;
            double len = 0;
            int cnt = 0;
            for (Partition part : sitemaps[i].partitions()) {
                fat += fatness(part);
                dil += dilation(part);
                len += length(part);
                cnt++;
            }
            dil /= 2 * cnt; // double counting...
            len /= 2 * cnt; // double counting...
            fat /= cnt;

            fatness[i] = fat;
            dilation[i] = dil;
            length[i] = len;
            numcuts[i] = (cnt - 1);
        }

        double mincut = IntegerUtil.max(numcuts);
        double bestfat = DoubleUtil.max(fatness); // higher is better
        double bestdil = DoubleUtil.max(dilation); // higher is better
        double bestlen = DoubleUtil.min(length); // lower is better

        System.out.println("  Name      Cuts    Fatness           Dilation           Length");
        for (int i = 0; i < cts.length; i++) {
            String name = cts[i].name();
            System.out.print("  " + name + (name.length() < 9 ? " " : ""));

            System.out.print("  " + numcuts[i]);
            if (numcuts[i] == mincut) {
                wins[i][0]++;
                System.out.print("*");
            } else {
                System.out.print(" ");
            }

            System.out.print("     " + fatness[i]);
            if (DoubleUtil.close(bestfat, fatness[i])) {
                wins[i][1]++;
                System.out.print("*");
            } else {
                System.out.print(" ");
            }

            System.out.print("  " + dilation[i]);
            if (DoubleUtil.close(bestdil, dilation[i])) {
                wins[i][2]++;
                System.out.print("*");
            } else {
                System.out.print(" ");
            }

            System.out.print("  " + length[i]);
            if (DoubleUtil.close(bestlen, length[i])) {
                wins[i][3]++;
                System.out.print("*");
            } else {
                System.out.print(" ");
            }

            System.out.println("");
        }
    }

    private double length(Partition p) {
        double len = 0;
        for (LineSegment ls : p.edges()) {
            if (p.outline.contains(ls.getPointAt(0.5), -DoubleUtil.EPS)) {
                len += ls.length();
            }
        }
        return len;
    }

    private double dilation(Partition p) {
        double dil = 0;
        for (LineSegment ls : p.edges()) {
            if (p.outline.contains(ls.getPointAt(0.5), -DoubleUtil.EPS)) {
                double len = ls.length();
                dil += (p.perimeter() - len) / len;
            }
        }
        return dil;
    }

    private double fatness(CyclicGeometry geom) {
        double area = geom.areaUnsigned();
        double perim = geom.perimeter();
        return 2 * Math.sqrt(Math.PI * area) / perim;
    }

    private void cutTypeSpeeds() throws IOException {

        List<Result> pss = new ArrayList();

        for (File f : collectFiles((File ff) -> ff.getName().endsWith(".txt") && !ff.getName().contains("-old"))) {
            List<String> lines = Files.readAllLines(f.toPath());
            Result ps = new Result();
            ps.initSetting(f.getName());
            ps.total = readTotalTiming(lines.get(lines.size() - 6));
            ps.cuts = readTotalTiming(lines.get(lines.size() - 5));
            ps.init = readTotalTiming(lines.get(lines.size() - 4));
            ps.perform = readTotalTiming(lines.get(lines.size() - 3));
            ps.construct = readTotalTiming(lines.get(lines.size() - 2));
            readAppliedCuts(ps, lines);
            pss.add(ps);
        }

        pss.sort((a, b) -> {
            int r = a.map.compareTo(b.map);
            if (r != 0) {
                return r;
            }

            r = -Double.compare(a.dilation, b.dilation);
            if (r != 0) {
                return r;
            }

            r = -Integer.compare(a.productivity, b.productivity);
            if (r != 0) {
                return r;
            }

            r = Boolean.compare(a.old, b.old);
            if (r != 0) {
                return r;
            }

            return 0;

        });

        List<Result> filtered = filter(pss, (Result r) -> r.productivity > 1 && r.dilation > 1.001);

        for (TestMap map : maps) {

            System.out.println("Map: " + map.name);
            for (CutType ct : cts) {
                List<Result> res = filter(filtered, (Result ps) -> ps.map.equals(map.tag) && ps.ct == ct);

                double found = average(res, (Result r) -> (double) r.numFound);
                double remaining = average(res, (Result r) -> (double) r.numRemaining);
                double applied = average(res, (Result r) -> (double) r.numApplied);
                double time = average(res, (Result r) -> (double) r.total) / reps;

                System.out.println("  " + ct + " :: found = " + found + "; remaining = " + remaining + "; applied = " + applied + "; time " + time + " ms");
            }
        }
    }
}
