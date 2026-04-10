package analyze;

import analyze.analyzers.Deformation;
import analyze.analyzers.Displacement;
import analyze.analyzers.GuidingShapeFit;
import analyze.analyzers.OrthogonalOrder;
import analyze.analyzers.SiteGuideFit;
import common.Site;
import common.SiteMap;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 *
 * @author Wouter Meulemans
 */
public abstract class Analyzer<T extends QualityMap<?>> {

    private final String name;
    protected final int k;
    
    public Analyzer(String name) {
        this.name = name;
        this.k = -1;
    }

    public Analyzer(String name, int k) {
        this.name = name + (k == 0 ? "-ALL" : "-"+k);
        this.k = k;
    }

    public abstract T run(SiteMap map);

    @Override
    public String toString() {
        return name;
    }

    public List<Site> neighborhoodOf(Site site, SiteMap map) {
        if (k == 0) {
            // shorthand for ALL
            List<Site> list = new ArrayList();
            for (Site s : map.sites()) {
                if (s != site) {
                    list.add(s);
                }
            };
            return list;
        } else if (k > 0) {
            PriorityQueue<Site> queue = new PriorityQueue<>(
                    (a, b) -> Double.compare(b.distanceTo(site), a.distanceTo(site)) // furtherst first
            );

            for (Site s : map.sites()) {
                if (s != site) {
                    queue.add(s);
                    if (queue.size() > k) {
                        queue.remove();
                    }
                }
            }

            return new ArrayList(queue);
        } else {
            return null;
        }
    }

    public static Analyzer[] methods = {
        new GuidingShapeFit(GuidingShapeFit.Variant.countpct),
        new GuidingShapeFit(GuidingShapeFit.Variant.quality),
        new SiteGuideFit(SiteGuideFit.Variant.miss),
        new SiteGuideFit(SiteGuideFit.Variant.deviation),
        new OrthogonalOrder(0),
        new OrthogonalOrder(10),
        new Deformation(3),
        new Deformation(10),
        new Deformation(25),
        new Displacement(Displacement.AlignMode.Uni),
        new Displacement(Displacement.AlignMode.NonUni),
        new Displacement(Displacement.AlignMode.Guide)
    };
}
