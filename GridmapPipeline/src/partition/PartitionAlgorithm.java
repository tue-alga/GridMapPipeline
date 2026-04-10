package partition;

import common.Outline;
import common.Partition;
import common.Site;
import common.SiteMap;
import common.Stage;
import coloring.ColorPicker;
import common.util.Stopwatch;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.util.DoubleUtil;
import nl.tue.geometrycore.util.ListUtil;

/**
 *
 * @author Wouter Meulemans
 */
public class PartitionAlgorithm {

    public static void trivialPartition(SiteMap map) {
        map.revertToStage(Stage.INPUT);

        for (Outline o : map.outlines) {
            o.trivialPartition();
        }

        map.stage = Stage.PARTITIONED;
    }

    private final double dilationThreshold;
    private final int productivityThreshold;
    private final CutType cutType;
    private final boolean nonCrossingMode;
    
    public PartitionAlgorithm() {
        productivityThreshold = Integer.MAX_VALUE/2;  // nothing will be partitioned...
        // rest is irrelevant
        dilationThreshold = Double.NaN;
        cutType = CutType.SHORTEST;
        nonCrossingMode = true;
    }

    public PartitionAlgorithm(CutType cutType, double dilationThreshold, int productivityThreshold) {
        this(cutType, dilationThreshold, productivityThreshold, cutType.nonCrossingCuts());
    }

    /**
     * For experimenting, we can override nonCrossingMode. If running
     * nonCrossingMode with a method that generates crossing cuts, behavior is
     * undefined.
     */
    public PartitionAlgorithm(CutType cutType, double dilationThreshold, int productivityThreshold, boolean nonCrossingMode) {
        this.dilationThreshold = dilationThreshold;
        this.productivityThreshold = productivityThreshold;
        this.cutType = cutType;
        this.nonCrossingMode = nonCrossingMode;
    }

    public void run(SiteMap map) {
        map.revertToStage(Stage.INPUT);

        System.out.println("------ RUNNING PARTITION ---------------------------");
        System.out.println("  p = " + productivityThreshold + ", d = " + dilationThreshold + ", ct = "+cutType);

        Stopwatch sw = Stopwatch.get("partition").start();

        for (Outline outline : map.outlines) {
            if (outline.sites.size() >= 2 * productivityThreshold) {
                // otherwise, no productive cut can exist
                run(outline);
            } else {
                outline.trivialPartition();
            }
        }

        map.stage = Stage.PARTITIONED;

        sw.stop();
    }

    private void run(Outline outline) {

        System.out.println("Finding cuts");
        Stopwatch sw = Stopwatch.get("- cuts").start();
        List<Cut> cuts = cutType.generateCuts(outline);
        sw.stop();
        System.out.println("  " + cuts.size() + " cuts found");

        // after we have our list of attached cuts, we take O(cuts log cuts + partitionsize min(sites, cuts)) time
        // see annotations below. 
        // As cuts = O(partitionsize) and assuming sites = m < partitionsize = n, 
        //  we take O(n log n + mn) time
        sw = Stopwatch.get("- init").start();
        Cut virtualRoot = nonCrossingMode ? createCutHierarchy(outline, cuts) : initializeLengths(outline, cuts);

        filterLowDilationCuts(virtualRoot, cuts);
        System.out.println("  " + cuts.size() + " cuts remaining");

        if (nonCrossingMode) {
            initializeCountsBySifting(virtualRoot, outline);
            // we no longer need the full tree at this point, clean it up, such that it can store the applied tree instead
            // nor do we need the polygon cache
            for (Cut c : cuts) {
                c.children.clear();
                c.parent = null;
                c.polygon_justbelow = null;
            }
            virtualRoot.children.clear();
            virtualRoot.polygon_justbelow = null;
        } else {
            initializeCountsGeneric(outline, cuts); // asymptotically, this is as fast as the above, but in practice this is slower
        }

        // NB: shortest first
        // O(cuts log cuts) time
        cuts.sort((Cut c1, Cut c2) -> Double.compare(c1.length(), c2.length()));
        sw.stop();

        System.out.println("Performing cuts");
        sw = Stopwatch.get("- perform").start();
        int applyCount = applyCuts(virtualRoot, cuts);
        sw.stop();
        System.out.println("  " + applyCount + " applied");

        sw = Stopwatch.get("- construct").start();
        if (applyCount == 0) {
            outline.trivialPartition();
        } else {
            System.out.println("Construction new partitions");
            constructPartitions(virtualRoot, outline);
        }
        sw.stop();
    }

    private Cut initializeLengths(final Outline outline, final List<Cut> cuts) {

        final int nVtx = outline.vertexCount();
        double[] perimeterToVertex = new double[nVtx];
        perimeterToVertex[0] = 0;
        for (int i = 1; i < nVtx; i++) {
            perimeterToVertex[i] = perimeterToVertex[i - 1] + outline.vertex(i).distanceTo(outline.vertex(i - 1));
        }
        for (Cut c : cuts) {
            final double startDist = perimeterToVertex[c.start_attach.edge] + c.start_attach.dist;
            final double endDist = perimeterToVertex[c.end_attach.edge] + c.end_attach.dist;
            c.subtree_length = endDist - startDist;
        }

        Cut virtualRoot = new Cut();
        virtualRoot.start_attach = new Attachment(outline.vertex(0), virtualRoot, 0);
        virtualRoot.end_attach = new Attachment(outline.vertex(0), virtualRoot, outline.vertexCount() - 1, outline.edge(-1).length());
        virtualRoot.subtree_length = perimeterToVertex[nVtx - 1] + outline.vertex(nVtx - 1).distanceTo(outline.vertex(0));
        virtualRoot.subtree_size = outline.sites.size();
        return virtualRoot;
    }

    private Cut createCutHierarchy(final Outline outline, final List<Cut> cuts) {

        List<Attachment> attaches = new ArrayList();
        for (Cut c : cuts) {
            attaches.add(c.start_attach);
            attaches.add(c.end_attach);
        }
        // sort the endpoints of the cuts in O(cuts log cuts) time
        attaches.sort(Attachment.cut_comparator);

        Cut virtualRoot = new Cut();
        virtualRoot.start_attach = new Attachment(outline.vertex(0), virtualRoot, 0);
        virtualRoot.end_attach = new Attachment(outline.vertex(0), virtualRoot, outline.vertexCount() - 1, outline.edge(-1).length());
        virtualRoot.subtree_length = outline.perimeter();

        // sweep through the cut endpoints to build the full cut tree in O(cuts) time
        Cut parent = virtualRoot;
        for (Attachment a : attaches) {
            Cut c = a.cut;
            if (a.isStartAttach()) {

                c.parent = parent;
                parent.children.add(c);
                parent = c;
            } else {
                // end a subtree: compute its subtree length

                Attachment prev = c.start_attach;

                for (int childindex = 0; childindex < c.children.size(); childindex++) {
                    Cut child = c.children.get(childindex);
                    c.subtree_length += prev.perimeterTo(outline, child.start_attach);
                    c.subtree_length += child.subtree_length;
                    prev = child.end_attach;
                }
                c.subtree_length += prev.perimeterTo(outline, c.end_attach);

                parent = c.parent;
            }
        }

        return virtualRoot;
    }

    private void initializeCountsGeneric(final Outline outline, final List<Cut> cuts) {
        for (Site site : outline.sites) {
            determineBelow(site, outline, cuts, (c) -> {
                c.subtree_size++;
            });
        }
    }

    private void determineBelow(final Site site, final Outline outline, final List<Cut> cuts, final Consumer<Cut> onBelowCut) {
        // runs in O(outline + cuts) time
        final int nVtx = outline.vertexCount();

        // just to be sure: what if site lies on the boundary
        for (int i = 0; i < nVtx; i++) {
            if (outline.vertex(i).isApproximately(site)) {

                Attachment at = new Attachment(site, null, i, site.distanceTo(outline.vertex(i)));
                determineBelowOnBoundary(at, cuts, onBelowCut);
            }
        }
        for (int i = 0; i < nVtx; i++) {
            if (outline.edge(i).onBoundary(site)) {
                // just check cut intervals

                Attachment at = new Attachment(site, null, i, site.distanceTo(outline.vertex(i)));
                determineBelowOnBoundary(at, cuts, onBelowCut);
            }
        }

        final double[] totAnglePrefix = new double[nVtx];
        totAnglePrefix[0] = 0;

        Vector dirPrev = Vector.subtract(outline.vertex(0), site);
        dirPrev.normalize();

        for (int i = 1; i < nVtx; i++) {
            Vector dirCurr = Vector.subtract(outline.vertex(i), site);
            dirCurr.normalize();

            double angle = dirPrev.computeSignedAngleTo(dirCurr, false, false);
            totAnglePrefix[i] = totAnglePrefix[i - 1] + angle;
            dirPrev = dirCurr;
        }

        // its either 0 or a multiple of 2 PI
        // 0  -> outside
        // !0 -> inside
        for (Cut c : cuts) {

            double totalAngle = totAnglePrefix[c.end_attach.edge] - totAnglePrefix[c.start_attach.edge];

            // to still process:
            //    add the angle of the cut
            //    subtract the angle of the bit along the edge of start attach
            //    add the angle of the bit along the edge of end attach
            Vector cutStartDir = Vector.subtract(c.start_attach, site);
            cutStartDir.normalize();
            Vector cutEndDir = Vector.subtract(c.end_attach, site);
            cutEndDir.normalize();

            // NB: from end to start, as it closes the polygon
            totalAngle += cutEndDir.computeSignedAngleTo(cutStartDir, false, false);

            if (c.start_attach.dist > DoubleUtil.EPS) {
                Vector startDir = Vector.subtract(outline.vertex(c.start_attach.edge), site);
                startDir.normalize();
                totalAngle -= startDir.computeSignedAngleTo(cutStartDir, false, false);
            }

            if (c.end_attach.dist > DoubleUtil.EPS) {
                Vector endDir = Vector.subtract(outline.vertex(c.end_attach.edge), site);
                endDir.normalize();
                totalAngle += endDir.computeSignedAngleTo(cutEndDir, false, false);
            }

            // NB: fairly generous imprecision, since the value should theoretically be 0 or a multiple of 2Pi
            if (!DoubleUtil.close(totalAngle, 0, 0.5)) {
                // contained!
                onBelowCut.accept(c);
            }
        }

    }

    private void determineBelowOnBoundary(final Attachment site, final List<Cut> cuts, final Consumer<Cut> onBelowCut) {
        for (Cut c : cuts) {
            if (c.containsStictly(site)) { // NB: as the comparison is strict, it is assigned towards the root when it lies on the cut
                onBelowCut.accept(c);
            }
        }
    }

    private void constructPolygon(Polygon construct, Cut cut, Polygon original) {

        construct.addVertex(cut.start_attach);
        Attachment prev = cut.start_attach;

        for (Cut a : cut.children) {
            for (Vector v : prev.verticesTo(original, a.start_attach)) {
                construct.addVertex(v);
            }
            if (!construct.vertex(-1).isApproximately(a.start_attach)) {
                construct.addVertex(a.start_attach);
            }
            construct.addVertex(a.end_attach);
            prev = a.end_attach;
        }

        for (Vector v : prev.verticesTo(original, cut.end_attach)) {
            construct.addVertex(v);
        }
        // NB: second term is relevant only for the virtualRoot...
        if (!construct.vertex(-1).isApproximately(cut.end_attach) && !construct.vertex(0).isApproximately(cut.end_attach)) {
            construct.addVertex(cut.end_attach);
        }

    }

    // returns true if the site was positioned
    private boolean siftSite(Cut cut, Site s, Polygon original) {

        if (cut.polygon_justbelow == null) {
            cut.polygon_justbelow = new Polygon();
            constructPolygon(cut.polygon_justbelow, cut, original);
        }
        if (cut.polygon_justbelow.contains(s)) {
            cut.subtree_size++;
            cut.sites_justbelow.add(s);
            return true;
        }

        // test if its in a child subtree
        for (Cut c : cut.children) {
            if (siftSite(c, s, original)) {
                cut.subtree_size++;
                return true;
            }
        }

        return false;
    }

    private void filterLowDilationCuts(final Cut virtualRoot, final List<Cut> cuts) {
        // get rid of cuts that'll not be used due to dilation being ineffective
        // takes O(cuts) time
        int i = 0;
        while (i < cuts.size()) {
            Cut cut = cuts.get(i);
            double length_above = virtualRoot.subtree_length - cut.subtree_length;
            double cutlength = cut.length();

            double dilation = Math.min(length_above / cutlength, cut.subtree_length / cutlength);

            if (dilation < dilationThreshold) {
                // dilation can only decrease futher
                if (nonCrossingMode) {
                    cut.parent.children.remove(cut);
                    for (Cut child : cut.children) {
                        child.parent = cut.parent;
                        cut.parent.children.add(child);
                    }
                }

                ListUtil.swapRemove(i, cuts);
            } else {
                i++;
            }
        }

        // re-sort all the children cuts (NB: they are disjoint)
        if (nonCrossingMode) {
            for (Cut c : cuts) {
                c.children.sort(Cut.disjoint_comparator);
            }
        }
    }

    private void initializeCountsBySifting(final Cut virtualRoot, final Outline outline) {
        // we do a preorder traversal for each site, to find the cut that contains the site in its subtree, but none of its children contains it
        // the total complexity of the polygons is O(partitionsize + cuts) = O(partitionsize)
        // so, this takes O(sites * partitionsize) time
        for (Site s : outline.sites) {
            siftSite(virtualRoot, s, outline);
        }
    }

    private int applyCuts(final Cut virtualRoot, final List<Cut> cuts) {
        int applyCount = 0;

        mainloop:
        for (Cut cut : cuts) {

            // find the lowest ancestor of the cut that is applied
            // we do so by descending the applied tree
            // takes O(min(sites,cuts)) time
            Cut root = virtualRoot;

            //System.out.println("  finding root");
            descendloop:
            while (true) {
                //System.out.println("  " + root);
                for (Cut subroot : root.children) {
                    if (subroot.contains(cut)) {
                        root = subroot;
                        continue descendloop;
                    } else if (subroot.containsStictly(cut.start_attach) || subroot.containsStictly(cut.end_attach)) {
                        // contains one of the endpoints strictly, but is not contained, must intersect, so this cut cannot be applied
                        continue mainloop;
                    }
                }
                break;
            }

            double length_below, length_above;
            int count_below, count_above;

            length_below = cut.subtree_length;
            length_above = root.subtree_length + root.length() - cut.subtree_length;
            count_below = cut.subtree_size;
            count_above = root.subtree_size - cut.subtree_size;

            for (Cut ac : root.children) {
                if (cut.contains(ac)) {
                    count_below -= ac.subtree_size;
                    length_below -= ac.subtree_length - ac.length(); // change to length caused by this applied cut
                } else {
                    count_above -= ac.subtree_size;
                    length_above -= ac.subtree_length - ac.length(); // change to length caused by this applied cut                        
                }
            }

            double cutlength = cut.length();

            int productivity = Math.min(count_below, count_above);
            double dilation = Math.min(length_below / cutlength, length_above / cutlength);

            //System.out.println("cut " + productivity + " , " + dilation);
            if (productivity >= productivityThreshold && dilation >= dilationThreshold) {

                // applying a cut is a matter of placing it in the tree
                // NB: we dont need to keep children sorted
                {
                    // move all of the root's children that are within the cut to the subtree of this newly applied cut
                    // and another O(min(sites,cuts)) step
                    int i = 0;
                    while (i < root.children.size()) {
                        Cut ac = root.children.get(i);
                        if (cut.contains(ac)) {
                            // move
                            cut.children.add(ac);
                            ListUtil.swapRemove(i, root.children);
                        } else {
                            i++;
                        }
                    }
                }
                // and place this cut into the roots children
                root.children.add(cut);
                applyCount++;
            }
        }

        return applyCount;
    }

    private void constructPartitions(final Cut virtualRoot, final Outline outline) {

        ColorPicker.reset();
        
        // Construct all partitions, augment labels & assign colors
        // assign the sites to their new partition
        Stack<Cut> toApply = new Stack();
        toApply.add(virtualRoot);

        // as we apply at most O(min(sites,cuts)) cuts due to productivity >= 1,
        // we know that the resulting complexity is O(partitionsize min(sites,cuts)) overall
        int partitionNumber = 1;
        while (!toApply.isEmpty()) {
            Cut cut = toApply.pop();

            Partition p = outline.addPartition();
            p.color = ColorPicker.getNewColor();
            p.label = outline.label + ":" + partitionNumber;
            p.outline = outline;
            partitionNumber++;

            // NB: they are disjoint so start attaches cannot be identical
            cut.children.sort(Cut.disjoint_comparator);
            constructPolygon(p, cut, outline);

            toApply.addAll(cut.children);
        }

        // reassign sites
        // this takes O(sites * (partitionsize + appliedcuts)) = O(sizes * partitionsize) time.
        // NB: sites exactly on a cut could be assigned differently, but observe:
        // - in the initial sifting, we do a preorder traversal: a site is assigned to a parent in the tree, rather than a child, if it lies on a common cut
        // - in making the cuts, we do a preorder traveersal: a partition higher up in the cut tree is done first and thus is earlier in the list of partitions
        // this should make this (somewhat) deterministic, unless very degenerate cases with double precision trigger different decisions
        for (Site s : outline.sites) {
            // and find the new partition
            for (Partition p : outline.partitions) {
                if (p.contains(s)) {
                    p.sites.add(s);
                    s.setPartition(p);
                    break;
                }
            }
        }

        // For precision sake, in noncrossingmode, one could run the same method as before:
        // determining the cut furthest from the root that contains the site below.
        // But this is considerably slower in practice...
//        for (Site s : outline.sites) {
//            Cut[] smallest = {virtualRoot};
//            determineBelow(s, outline, applied, (c) -> smallest[0] = c);
//
//            for (int i = 0; i < applied.size(); i++) {
//                if (applied.get(i) == smallest[0]) {
//                    Partition p = outline.partitions.get(i);
//                    p.sites.add(s);
//                    s.setPartition(p);
//                }
//            }
//        }
    }
}
