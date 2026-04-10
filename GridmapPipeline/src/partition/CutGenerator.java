package partition;

import common.Outline;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.LineSegment;
import nl.tue.geometrycore.util.DoubleUtil;
import nl.tue.geometrycore.util.ListUtil;

/**
 *
 * @author Max Sondag, Wouter Meulemans
 */
public class CutGenerator {

    private static final String[] candidateLocations = {
        "./", "../precompiled/", "./precompiled/", "../CmdMedialAxis/x64/Release/", "../CmdMedialAxis/x64/Debug/"
    };
    private static final String programName = "CmdMedialAxis.exe";

    public static String command = null;

    public static String findCommand() {
        if (command != null) {
            return command;
        }

        for (String loc : candidateLocations) {
            File f = new File(loc, programName);
            if (f.exists()) {
                return loc + programName;
            }
        }
        return null;
    }

    /**
     * Candidate cuts start at a corner and at a different point on the polygon.
     * The set of candidate cuts is planar.
     */
    public static List<Cut> getCandidateCuts(Outline outline, CutType cutType) {

        List<Cut> rawCuts = new ArrayList();
        cutsFromMedialAxis(outline, rawCuts, cutType);
        removeDuplicateCuts(rawCuts);

        return rawCuts;
    }

    /**
     * Remove duplicate cuts.
     *
     * @param rawCuts
     */
    private static void removeDuplicateCuts(List<Cut> rawCuts) {

        rawCuts.sort(Cut.attach_comparator);

        int i = rawCuts.size() - 1;
        // NB: invariant that everything with index > i is unique in the entire set
        // and everything with index <= i is sorted (~equal cuts must be adjacent)
        while (i > 0) {
            if (rawCuts.get(i).sameCut(rawCuts.get(i - 1))) {
                ListUtil.swapRemove(i, rawCuts);
            }
            i--;
        }
    }

    private static void cutsFromMedialAxis(Outline outline, List<Cut> rawCuts, CutType cutType) {
        String cmd = findCommand();

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process p = pb.start();
            PrintWriter writer = new PrintWriter(p.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            for (int i = 0; i < outline.vertexCount(); i++) {
                Vector u = outline.vertex(i);
                Vector v = outline.vertex(i + 1);
                writer.println("s " + u.getX() + " " + u.getY() + " " + v.getX() + " " + v.getY());
            }
            writer.println("end");
            writer.flush();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] split = line.split(" ");
                // 0: type (s,src,tar)
                // 1: edgenum
                // 2: type
                // 3: edgenum
                // 4: geom (r,s,l,ps)
                // 5+: coords for geom               

                // NB: infinite geometries (line or ray) must lie outside
                if (!split[4].equals("s") && !split[4].equals("ps")) {
                    continue;
                }

                Vector start = new Vector(Double.parseDouble(split[5]), Double.parseDouble(split[6]));
                Vector end = new Vector(Double.parseDouble(split[7]), Double.parseDouble(split[8]));

                if (start.isApproximately(end)) {
                    // degenerate (point) segment
                    continue;
                }

                int edge_A = Integer.parseInt(split[1]);
                boolean src_A;
                switch (split[0]) {
                    case "tar":
                        edge_A--; // NB: no break on purpose
                    case "src":
                        src_A = true;
                        break;
                    default:
                        src_A = false;
                        break;
                }

                int edge_B = Integer.parseInt(split[3]);
                boolean src_B;
                switch (split[2]) {
                    case "tar":
                        edge_B--;// NB: no break on purpose
                    case "src":
                        src_B = true;
                        break;
                    default:
                        src_B = false;
                        break;
                }

                if (edge_A == edge_B) {
                    // same edge, either leaf or precision issue
                    // either case, doesn't generate a cut
                    continue;
                }

                if (!src_A && !src_B) {
                    // two proper edges, the segment will not generate cuts
                    continue;
                }

                // ensureedge A is earlier than edge B
                if (edge_A > edge_B) {
                    int t = edge_B;
                    edge_B = edge_A;
                    edge_A = t;
                    boolean tb = src_B;
                    src_B = src_A;
                    src_A = tb;
                }

                if (edge_A == edge_B - 1 || (edge_A == 0 && edge_B == outline.edgeCount() - 1)) {
                    // adjacent edges, do not generate a cut
                    continue;
                }

                // test interior: vertices must be reflex, edges must have the points lie on interior side
                if (src_A) {
                    Vector u = outline.vertex(edge_A - 1);
                    Vector v = outline.vertex(edge_A);
                    Vector w = outline.vertex(edge_A + 1);

                    boolean reflex = Vector.crossProduct(Vector.subtract(v, u), Vector.subtract(w, u)) < 0;
                    if (!reflex) {
                        continue;
                    }
                } else {
                    Vector v = outline.vertex(edge_A);
                    Vector w = outline.vertex(edge_A + 1);

                    boolean leftOf = Vector.crossProduct(Vector.subtract(w, v), Vector.subtract(start, v)) > 0;
                    if (!leftOf) {
                        continue;
                    }
                }

                if (src_B) {
                    Vector u = outline.vertex(edge_B - 1);
                    Vector v = outline.vertex(edge_B);
                    Vector w = outline.vertex(edge_B + 1);

                    boolean reflex = Vector.crossProduct(Vector.subtract(v, u), Vector.subtract(w, u)) < 0;
                    if (!reflex) {
                        continue;
                    }
                } else {
                    Vector v = outline.vertex(edge_B);
                    Vector w = outline.vertex(edge_B + 1);

                    boolean leftOf = Vector.crossProduct(Vector.subtract(w, v), Vector.subtract(start, v)) > 0;
                    if (!leftOf) {
                        continue;
                    }
                }

                // so, it is interior, and at least one reflex vertex
                // lets construct the cuts
                if (src_A && src_B) {
                    Cut c = new Cut();
                    rawCuts.add(c);
                    c.start_attach = new Attachment(outline.vertex(edge_A), c, edge_A);
                    c.end_attach = new Attachment(outline.vertex(edge_B), c, edge_B);
                } else if (src_A) {
                    Vector v_A = outline.vertex(edge_A);
                    LineSegment e_B = outline.edge(edge_B);
                    Vector v_B = e_B.getStart();

                    switch (cutType) {
                        case SHORTEST: {
                            Cut c = new Cut();
                            rawCuts.add(c);
                            c.start_attach = new Attachment(v_A, c, edge_A);

                            Vector pt_start = e_B.closestPoint(start);
                            double dist_start = pt_start.distanceTo(v_B);

                            Vector pt_end = e_B.closestPoint(end);
                            double dist_end = pt_end.distanceTo(v_B);

                            Vector pt_min = e_B.closestPoint(v_A);
                            double dist_min = pt_min.distanceTo(v_B);

                            double low = Math.min(dist_start, dist_end);
                            double high = Math.max(dist_start, dist_end);

                            // if min lies in the interval of the parabolic segment, it's the best cut
                            // otherwise, add the shorter of the two end points
                            if (DoubleUtil.inOpenInterval(dist_min, low, high)) {
                                c.end_attach = new Attachment(pt_min, c, edge_B, dist_min);
                            } else if (pt_start.distanceTo(start) <= pt_end.distanceTo(end)) {
                                c.end_attach = new Attachment(pt_start, c, edge_B, dist_start);
                            } else {
                                c.end_attach = new Attachment(pt_end, c, edge_B, dist_end);
                            }
                            break;
                        }
                        case COMBINED: {
                            Vector pt_start = e_B.closestPoint(start);
                            double dist_start = pt_start.distanceTo(v_B);

                            Vector pt_end = e_B.closestPoint(end);
                            double dist_end = pt_end.distanceTo(v_B);

                            Vector pt_min = e_B.closestPoint(v_A);
                            double dist_min = pt_min.distanceTo(v_B);

                            double low = Math.min(dist_start, dist_end);
                            double high = Math.max(dist_start, dist_end);

                            // if min lies in the interval of the parabolic segment
                            if (DoubleUtil.inOpenInterval(dist_min, low, high)) {
                                Cut c = new Cut();
                                rawCuts.add(c);
                                c.start_attach = new Attachment(v_A, c, edge_A);
                                c.end_attach = new Attachment(pt_min, c, edge_B, dist_min);
                            }
                            // NB: no break! also add endpoints: 
                        }
                        case ENDPOINTS: {
                            Cut cs = new Cut();
                            cs.start_attach = new Attachment(outline.vertex(edge_A), cs, edge_A);
                            Vector startmap = outline.edge(edge_B).closestPoint(start);
                            cs.end_attach = new Attachment(startmap, cs, edge_B, outline.vertex(edge_B).distanceTo(startmap));
                            rawCuts.add(cs);

                            Cut ce = new Cut();
                            ce.start_attach = new Attachment(outline.vertex(edge_A), ce, edge_A);
                            Vector endmap = outline.edge(edge_B).closestPoint(end);
                            ce.end_attach = new Attachment(endmap, ce, edge_B, outline.vertex(edge_B).distanceTo(endmap));
                            rawCuts.add(ce);
                            break;
                        }
                    }

                } else { // src_B (because at least one is true: checked earlier

                    Vector v_B = outline.vertex(edge_B);
                    LineSegment e_A = outline.edge(edge_A);
                    Vector v_A = e_A.getStart();

                    switch (cutType) {
                        case SHORTEST: {
                            Cut c = new Cut();
                            rawCuts.add(c);
                            c.end_attach = new Attachment(v_B, c, edge_B);

                            Vector pt_start = e_A.closestPoint(start);
                            double dist_start = pt_start.distanceTo(v_A);

                            Vector pt_end = e_A.closestPoint(end);
                            double dist_end = pt_end.distanceTo(v_A);

                            Vector pt_min = e_A.closestPoint(v_B);
                            double dist_min = pt_min.distanceTo(v_A);

                            double low = Math.min(dist_start, dist_end);
                            double high = Math.max(dist_start, dist_end);

                            // if min lies in the interval of the parabolic segment, it's the best cut
                            // otherwise, add the shorter of the two end points
                            if (DoubleUtil.inOpenInterval(dist_min, low, high)) {
                                c.start_attach = new Attachment(pt_min, c, edge_A, dist_min);
                            } else if (pt_start.distanceTo(start) <= pt_end.distanceTo(end)) {
                                c.start_attach = new Attachment(pt_start, c, edge_A, dist_start);
                            } else {
                                c.start_attach = new Attachment(pt_end, c, edge_A, dist_end);
                            }
                            break;
                        }
                        case COMBINED: {

                            Vector pt_start = e_A.closestPoint(start);
                            double dist_start = pt_start.distanceTo(v_A);

                            Vector pt_end = e_A.closestPoint(end);
                            double dist_end = pt_end.distanceTo(v_A);

                            Vector pt_min = e_A.closestPoint(v_B);
                            double dist_min = pt_min.distanceTo(v_A);

                            double low = Math.min(dist_start, dist_end);
                            double high = Math.max(dist_start, dist_end);

                            // if min lies in the interval of the parabolic segment
                            if (DoubleUtil.inOpenInterval(dist_min, low, high)) {
                                Cut c = new Cut();
                                rawCuts.add(c);
                                c.end_attach = new Attachment(v_B, c, edge_B);
                                c.start_attach = new Attachment(pt_min, c, edge_A, dist_min);
                            }
                            // NB: no break! also add endpoints: 
                        }
                        case ENDPOINTS: {
                            Cut cs = new Cut();
                            Vector startmap = outline.edge(edge_A).closestPoint(end);
                            cs.start_attach = new Attachment(startmap, cs, edge_A, outline.vertex(edge_A).distanceTo(startmap));
                            cs.end_attach = new Attachment(outline.vertex(edge_B), cs, edge_B);
                            rawCuts.add(cs);

                            Cut ce = new Cut();
                            Vector endmap = outline.edge(edge_A).closestPoint(end);
                            ce.start_attach = new Attachment(endmap, ce, edge_A, outline.vertex(edge_A).distanceTo(endmap));
                            ce.end_attach = new Attachment(outline.vertex(edge_B), ce, edge_B);
                            rawCuts.add(ce);
                            break;
                        }
                    }
                }

            }

        } catch (IOException ex) {
            Logger.getLogger(CutGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
