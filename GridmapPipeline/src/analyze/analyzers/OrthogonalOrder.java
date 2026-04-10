package analyze.analyzers;

import analyze.Analyzer;
import analyze.SiteQualityMap;
import common.Site;
import common.SiteMap;
import common.Stage;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.util.DoubleUtil;

/**
 *
 * @author Wouter Meulemans
 */
public class OrthogonalOrder extends Analyzer<SiteQualityMap> {

    public OrthogonalOrder(int k) {
        super("OrthogonalOrder", k);
    }

    @Override
    public SiteQualityMap run(SiteMap map) {
        if (map.stage < Stage.ASSIGNED) {
            return null;
        }

        SiteQualityMap qmap = new SiteQualityMap(toString(), false);

        int scnt = 0;
        for (Site s : map.sites()) {

            scnt++;

            Vector sv = s.getCell().toVector();

            int sxv = 0;
            int syv = 0;

            for (Site nbr : neighborhoodOf(s, map)) {
                Vector ssv = nbr.getCell().toVector();
                if (DoubleUtil.close(ssv.getX(), sv.getX()) || DoubleUtil.close(nbr.getX(), s.getX())) {
                    // same X is always ok
                } else if (Math.signum(ssv.getX() - sv.getX()) != Math.signum(nbr.getX() - s.getX())) {
                    sxv++;
                }
                if (DoubleUtil.close(ssv.getY(), sv.getY()) || DoubleUtil.close(nbr.getY(), s.getY())) {
                    // same Y is always ok
                } else if (Math.signum(ssv.getY() - sv.getY()) != Math.signum(nbr.getY() - s.getY())) {
                    syv++;
                }
            }

            qmap.putQuality(s, sxv + syv);
        }

        qmap.process();
        qmap.setMinimum(0);
        qmap.setMaximum(k == 0 ? 2 * (scnt - 1) : 2 * k);

        return qmap;
    }

}
