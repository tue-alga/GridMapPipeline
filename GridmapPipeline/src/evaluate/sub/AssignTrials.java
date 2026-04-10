package evaluate.sub;

import analyze.Analyzer;
import analyze.analyzers.Displacement;
import assign.AssignAlgorithm;
import assign.alignments.BoundingBoxAgnosticAlignment;
import assign.alignments.NonuniformAgnosticAlignment;
import assign.alignments.UniformAgnosticAlignment;
import assign.aware.NonuniformAwareAlignment;
import assign.aware.UniformAwareAlignment;
import common.gridmath.GridGeometry;
import common.gridmath.grids.HexagonGeometry;
import common.gridmath.grids.SquareGeometry;
import evaluate.Script;
import evaluate.Evaluation;
import evaluate.GridmapScript;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nl.tue.geometrycore.io.ipe.IPEWriter;

/**
 *
 * @author Wouter Meulemans
 */
public class AssignTrials extends Evaluation {

    public AssignTrials() {
        super("AssignTrials", true, false);
    }

    @Override
    public void addScripts(List<Script> scripts) {

        GridGeometry[] geoms = {
            new SquareGeometry(),
            new HexagonGeometry()
        };
        String[] inputs = {
            "uk",
            "nl"
        };

        AssignAlgorithm[] assigns = {
            new AssignAlgorithm(new BoundingBoxAgnosticAlignment(), null),
            new AssignAlgorithm(new NonuniformAgnosticAlignment(), null),
            new AssignAlgorithm(new UniformAgnosticAlignment(), null),
            new AssignAlgorithm(new NonuniformAgnosticAlignment(), new NonuniformAwareAlignment()),
            new AssignAlgorithm(new UniformAgnosticAlignment(), new UniformAwareAlignment())
        };
        String[] assignnames = {
            "BBOX",
            "NONUNI",
            "UNI",
            "NONUNIREF",
            "UNIREF"
        };
        Analyzer[] analysis = {
            new Displacement(Displacement.AlignMode.Uni),
            new Displacement(Displacement.AlignMode.NonUni)
        };

        for (int i = 0; i < assigns.length; i++) {
            AssignAlgorithm assign = assigns[i];

            for (GridGeometry geom : geoms) {
                for (String input : inputs) {

                    String cs = input + "-" + geom + "-" + assignnames[i];

                    GridmapScript s = new GridmapScript(baseroot + input + "-" + geom + ".wkt", outroot + cs);
                    s.setAssign(assign);
                    s.setAnalyzers(analysis);
                    scripts.add(s);
                }
            }
        }

    }

    @Override
    protected void computeOutcomes(IPEWriter write) throws IOException {

        Map<String, String> mapnames = new HashMap();
        mapnames.put("nl", "NL");
        mapnames.put("uk", "UK");

        Map<String, String> gridnames = new HashMap();
        gridnames.put("HEXAGON", "Hex");
        gridnames.put("SQUARE", "Sqr");

        String[] methods = {"BBOX", "UNI", "UNIREF", "NONUNI", "NONUNIREF"};
        Map<String, Integer> methodorder = new HashMap();
        for (int i = 0; i < methods.length; i++) {
            methodorder.put(methods[i], i);
        }
        Map<String, String> methodnames = new HashMap();
        methodnames.put("BBOX", "BOX");
        methodnames.put("UNI", "UNI");
        methodnames.put("UNIREF", "UREF");
        methodnames.put("NONUNI", "NON");
        methodnames.put("NONUNIREF", "NREF");

        List<Result> aqs = new ArrayList();
        for (File f : collectLogFiles()) {

            List<String> lines = Files.readAllLines(f.toPath());
            Result qs = new Result();
            String[] ns = f.getName().substring(0, f.getName().length() - 4).split("-");
            qs.map = ns[0];
            qs.grid = ns[1];
            qs.method = ns[2];

            qs.method_rmsed = Double.parseDouble(last(lines.get(lines.size() - 23), " "));

            qs.uni_dis_avg = Double.parseDouble(firstTrim(lines.get(lines.size() - 20), " "));
            qs.uni_dis_max = Double.parseDouble(last(lines.get(lines.size() - 19), " "));
            qs.uni_dis_90 = Double.parseDouble(last(lines.get(lines.size() - 18), " "));
            qs.uni_dis_75 = Double.parseDouble(last(lines.get(lines.size() - 17), " "));
            qs.uni_rmsed = Double.parseDouble(last(lines.get(lines.size() - 14), " "));

            qs.nonuni_dis_avg = Double.parseDouble(firstTrim(lines.get(lines.size() - 12), " "));
            qs.nonuni_dis_max = Double.parseDouble(last(lines.get(lines.size() - 11), " "));
            qs.nonuni_dis_90 = Double.parseDouble(last(lines.get(lines.size() - 10), " "));
            qs.nonuni_dis_75 = Double.parseDouble(last(lines.get(lines.size() - 9), " "));
            qs.nonuni_rmsed = Double.parseDouble(last(lines.get(lines.size() - 6), " "));

            aqs.add(qs);
        }

        aqs.sort((a, b) -> {
            int r = a.map.compareTo(b.map);
            if (r != 0) {
                return r;
            }

            r = a.grid.compareTo(b.grid);
            if (r != 0) {
                return r;
            }

            r = -Integer.compare(methodorder.get(a.method), methodorder.get(b.method));
            if (r != 0) {
                return r;
            }

            return 0;

        });

        System.out.println("\\bfseries method  & \\bfseries u-rms   & \\bfseries u-avgd & \\bfseries u-maxd & \\bfseries n-rms   & \\bfseries n-avgd & \\bfseries n-maxd \\\\");
        System.out.println("\\midrule");
        for (String method : methods) {
            List<Result> faqs = filter(aqs, (Result aq) -> aq.method.equals(method));

            String line = methodnames.get(method) + " ";
            while (line.length() < 8) {
                line += " ";
            }
            System.out.print(line);
            //System.out.print("& "+df.format(average(faqs, (AssignQuality aq) -> aq.method_rmsed)));

            System.out.print(" & " + df3digits.format(average(faqs, (Result aq) -> aq.uni_rmsed)));
            System.out.print(" & " + df3digits.format(average(faqs, (Result aq) -> aq.uni_dis_avg)));
            System.out.print(" & " + df3digits.format(average(faqs, (Result aq) -> aq.uni_dis_max)));
            //System.out.print(" & "+df.format(average(faqs, (AssignQuality aq) -> aq.uni_dis_90)));
            //System.out.print(" & "+df.format(average(faqs, (AssignQuality aq) -> aq.uni_dis_75)));

            System.out.print(" & " + df3digits.format(average(faqs, (Result aq) -> aq.nonuni_rmsed)));
            System.out.print(" & " + df3digits.format(average(faqs, (Result aq) -> aq.nonuni_dis_avg)));
            System.out.print(" & " + df3digits.format(average(faqs, (Result aq) -> aq.nonuni_dis_max)));
            //System.out.print(" & "+df.format(average(faqs, (AssignQuality aq) -> aq.nonuni_dis_90)));
            //System.out.print(" & "+df.format(average(faqs, (AssignQuality aq) -> aq.nonuni_dis_75)));

            System.out.println(" \\\\");
        }

        System.out.println("");

        System.out.println("\\bfseries method  & \\bfseries m-rms  & \\bfseries u-rms   & \\bfseries u-avgd & \\bfseries u-maxd & \\bfseries u-90d  & \\bfseries u-75d  \\\\");
        System.out.println("\\midrule");
        for (String method : methods) {
            List<Result> faqs = filter(aqs, (Result aq) -> aq.method.equals(method));

            String line = methodnames.get(method) + " ";
            while (line.length() < 8) {
                line += " ";
            }
            System.out.print(line);
            System.out.print("& " + df3digits.format(average(faqs, (Result aq) -> aq.method_rmsed)));

            System.out.print(" & " + df3digits.format(average(faqs, (Result aq) -> aq.uni_rmsed)));
            System.out.print(" & " + df3digits.format(average(faqs, (Result aq) -> aq.uni_dis_avg)));
            System.out.print(" & " + df3digits.format(average(faqs, (Result aq) -> aq.uni_dis_max)));
            System.out.print(" & " + df3digits.format(average(faqs, (Result aq) -> aq.uni_dis_90)));
            System.out.print(" & " + df3digits.format(average(faqs, (Result aq) -> aq.uni_dis_75)));

            System.out.println(" \\\\");
        }

        System.out.println("\\bottomrule");
        System.out.println("\\vspace{\\baselineskip}\\\\");
        System.out.println("\\toprule");
        System.out.println("\\bfseries method  & \\bfseries m-rms  & \\bfseries n-rms   & \\bfseries n-avgd & \\bfseries n-maxd & \\bfseries n-90d  & \\bfseries n-75d  \\\\");
        System.out.println("\\midrule");
        for (String method : methods) {
            List<Result> faqs = filter(aqs, (Result aq) -> aq.method.equals(method));

            String line = methodnames.get(method) + " ";
            while (line.length() < 8) {
                line += " ";
            }
            System.out.print(line);
            System.out.print("& " + df3digits.format(average(faqs, (Result aq) -> aq.method_rmsed)));

            System.out.print(" & " + df3digits.format(average(faqs, (Result aq) -> aq.nonuni_rmsed)));
            System.out.print(" & " + df3digits.format(average(faqs, (Result aq) -> aq.nonuni_dis_avg)));
            System.out.print(" & " + df3digits.format(average(faqs, (Result aq) -> aq.nonuni_dis_max)));
            System.out.print(" & " + df3digits.format(average(faqs, (Result aq) -> aq.nonuni_dis_90)));
            System.out.print(" & " + df3digits.format(average(faqs, (Result aq) -> aq.nonuni_dis_75)));

            System.out.println(" \\\\");
        }

//        File outputfile = new File("../Charts/assignquality.ipe");
//        IPEWriter write = IPEWriter.fileWriter(outputfile);
//
//        configure(write);
//        {
//            write.newPage(maps);
//
//            int mapi = -1;
//            double prevh = 0;
//            for (String map : maps) {
//                mapi++;
//                write.setLayer(map);
//
//                List<AssignQuality> maqs = filter(aqs, (AssignQuality aq) -> aq.map.equals(map));
//
//                int i = 0;
//                int max = (int) Math.ceil(1.01 * max(maqs, (AssignQuality aq) -> aq.method_rmsed));
//
//                Vector origin = new Vector(100, 500 - prevh);
//                Rectangle area = Rectangle.byCornerAndSize(origin, chartwidth, barheight * maqs.size() + gap * (maqs.size() + 1));
//                prevh += area.height() + vsep;
//
//                for (AssignQuality aq : maqs) {
//                    double t = origin.getX();
//                    double bottom = origin.getY() + gap + (barheight + gap) * i;
//
//                    write.setStroke(ExtendedColors.black, 0.4, Dashing.SOLID);
//                    write.setFill(null, Hashures.SOLID);
//                    write.setTextStyle(TextAnchor.RIGHT, 9);
//                    write.draw(new Vector(t - left_offset, bottom + barheight / 2.0), methodnames.get(aq.method));
//
//                    if (aq.method.equals("OPTIMAL")) {
//                        write.setTextStyle(TextAnchor.LEFT, 9);
//                        write.draw(new Vector(t - 50, bottom - gap / 2.0), gridnames.get(aq.grid));
//                    }
//
//                    write.setStroke(null, 0, Dashing.SOLID);
//
//                    write.setFill(ExtendedColors.darkOrange, Hashures.SOLID);
//                    write.draw(Rectangle.byCornerAndSize(new Vector(origin.getX(), origin.getY() + gap + (barheight + gap) * i),
//                            chartwidth * aq.method_rmsed / max, barheight / 3));
//
//                    write.setFill(ExtendedColors.darkBlue, Hashures.SOLID);
//                    write.draw(Rectangle.byCornerAndSize(new Vector(origin.getX(), origin.getY() + gap + (barheight + gap) * i + barheight / 3),
//                            chartwidth * aq.uni_rmsed / max, barheight / 3));
//
//                    write.setFill(ExtendedColors.darkRed, Hashures.SOLID);
//                    write.draw(Rectangle.byCornerAndSize(new Vector(origin.getX(), origin.getY() + gap + (barheight + gap) * i + 2 * barheight / 3),
//                            chartwidth * aq.uni_dis_avg / max, barheight / 3));
//                    i++;
//                }
//
//                write.setFill(null, Hashures.SOLID);
//                write.setStroke(ExtendedColors.black, 0.4, Dashing.SOLID);
//                write.draw(area);
//
//                write.setTextStyle(TextAnchor.BASELINE_CENTER, 9);
//                write.draw(Vector.add(area.topSide().getPointAt(0.5), Vector.up(top_offset)), mapnames.get(map));
//
//                write.setTextStyle(TextAnchor.TOP, 9);
//                write.draw(Vector.add(area.leftBottom(), Vector.down(bottom_offset)), "0");
//                write.draw(Vector.add(area.rightBottom(), Vector.down(bottom_offset)), "" + max);
//
////                if (mapi == 1) {
////                    String[] legend = {"Embedder", "Guides", "Slides", "Force", "Reshape", "Finalize", "Other"};
////                    Color[] colors = {
////                        ExtendedColors.darkRed,
////                        ExtendedColors.darkBlue,
////                        ExtendedColors.darkGreen,
////                        ExtendedColors.darkPurple,
////                        ExtendedColors.darkOrange,
////                        ExtendedColors.lightRed,
////                        ExtendedColors.darkGray
////                    };
////                    for (int L = 0; L < legend.length; L++) {
////                        Vector lb = Vector.addSeq(area.rightBottom(), Vector.right(left_offset), Vector.up(1 + L * barheight + L * gap));
////                        Rectangle R = Rectangle.byCornerAndSize(lb, barheight, barheight);
////                        write.setStroke(null, 0.4, Dashing.SOLID);
////                        write.setFill(colors[legend.length - 1 - L], Hashures.SOLID);
////                        write.draw(R);
////                        write.setStroke(Color.black, 0.4, Dashing.SOLID);
////                        write.setTextStyle(TextAnchor.LEFT, 9);
////                        write.draw(Vector.add(R.rightBottom(), new Vector(left_offset, barheight / 2.0)), legend[legend.length - 1 - L]);
////                    }
////                }
//            }
//        }
//
//        write.close();
//
//        IPECommands cmd = IPECommands.create("C:/Program Files/ipe-7.2.24/bin/");
//        cmd.convertIPEtoPDF(outputfile);
    }

    private static class Result {

        // setting
        String map;
        String grid;
        String method;
        // measures
        double method_rmsed;
        double uni_rmsed;
        double uni_dis_avg;
        double uni_dis_max;
        double uni_dis_90;
        double uni_dis_75;
        double nonuni_rmsed;
        double nonuni_dis_avg;
        double nonuni_dis_max;
        double nonuni_dis_90;
        double nonuni_dis_75;

    }

}
