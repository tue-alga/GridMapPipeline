package analyze.analyzers;

import analyze.Analyzer;
import analyze.SiteQualityMap;
import assign.Alignment;
import assign.alignments.GuideAlignment;
import assign.aware.NonuniformAwareAlignment;
import assign.aware.UniformAwareAlignment;
import common.Outline;
import common.Partition;
import common.Site;
import common.SiteMap;
import common.util.Transform;
import nl.tue.geometrycore.geometry.Vector;

/**
 *
 * @author Wouter Meulemans
 */
public class Displacement extends Analyzer<SiteQualityMap> {

    public enum AlignMode {
        Uni,
        NonUni,
        Guide
    }
    
    private final AlignMode mode;

    public Displacement(AlignMode mode) {
        super("Displacement-" + mode);
        this.mode = mode;
    }

    @Override
    public SiteQualityMap run(SiteMap map) {
        SiteQualityMap qmap = new SiteQualityMap(toString(), false);
        double total = 0;
        for (Partition p : map.partitions()) {
            total += analyze(p, qmap);
        }

        qmap.process();
        qmap.setMinimum(0);

        System.out.println("Total sq. Euclidean distance: " + total);
        int sitecount = 0;
        for (Outline o : map.outlines) {
            sitecount += o.sites.size();
        }
        System.out.println("Root mean sq. Euclidean distance: " + Math.sqrt(total / sitecount));
        return qmap;
    }

    private double analyze(Partition p, SiteQualityMap sqm) {
        Alignment align;
        switch (mode) {
            case Uni:
                align = new UniformAwareAlignment();            
                break;
            case NonUni:
                align = new NonuniformAwareAlignment();
                break;
            case Guide: 
                align = new GuideAlignment();
                break;
            default:
                align = null;
                System.err.println("Unexpected alignmode? "+mode);
                break;
        }

        if (p.sites.size() == 1) {
            sqm.putQuality(p.sites.get(0), 0);
            return 0;
        } else {
            Transform t = align.construct(p);

            double total = 0;
            for (Site s : p.sites) {
                Vector c = s.getCell().toVector();
                Vector ts = t.apply(s);

                double cst = ts.squaredDistanceTo(c);
                sqm.putQuality(s, Math.sqrt(cst));
                total += cst;
            }
            return total;
        }
    }

}
