package analyze.analyzers;

import analyze.Analyzer;
import analyze.SiteQualityMap;
import arrange.model.GuidingShape;
import common.Site;
import common.SiteMap;
import common.Stage;

/**
 *
 * @author Wouter Meulemans
 */
public class SiteGuideFit extends Analyzer<SiteQualityMap> {

    public enum Variant {
        miss,
        deviation
    }

    private Variant var;

    public SiteGuideFit(Variant var) {
        super("SiteGuideFit-" + var);
        this.var = var;
    }

    @Override
    public SiteQualityMap run(SiteMap map) {
        if (map.stage < Stage.ASSIGNED) {
            return null;
        }

        SiteQualityMap qmap = new SiteQualityMap(toString(), false);
        for (Site s : map.sites()) {
            switch (var) {
                case miss:
                    runCount(s, qmap);
                    break;
                case deviation:
                    runQuality(s, qmap);
                    break;
            }
        }

        qmap.process();
        qmap.setMinimum(0);
        qmap.setMaximum(1);

        return qmap;
    }

    private void runCount(Site s, SiteQualityMap qmap) {
        GuidingShape guide = s.getPartition().guide;

        if (guide == null) {
            // only happens for outlines(=partition) with 1 site
            qmap.putQuality(s, 0);
        } else {
            boolean hit = s.getPartition().guide.contains(s.getCell());
            if (hit) {
                qmap.putQuality(s, 0);
            } else {
                qmap.putQuality(s, 1);
            }
        }
    }

    private void runQuality(Site s, SiteQualityMap qmap) {
        GuidingShape guide = s.getPartition().guide;
        if (guide == null) {
            // only happens for outlines(=partition) with 1 site
            qmap.putQuality(s, 0);
        } else {
            qmap.putQuality(s,
                    1 - (guide.desirability(s.getCell()) / s.getCell().getBoundary().areaUnsigned()));
        }
    }

}
