package analyze.analyzers;

import analyze.Analyzer;
import analyze.SiteQualityMap;
import common.Site;
import common.SiteMap;
import common.Stage;
import java.util.ArrayList;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;

/**
 *
 * @author Wouter Meulemans
 */
public class Deformation extends Analyzer<SiteQualityMap> {

    public Deformation(int k) {
        super("Deformation", k);
    }

    @Override
    public SiteQualityMap run(SiteMap map) {
        if (map.stage < Stage.ASSIGNED) {
            return null;
        }

        SiteQualityMap qmap = new SiteQualityMap(toString(), false);
        for (Site s : map.sites()) {
            double q = run(map,s);
            qmap.putQuality(s, q);
        }
        
        qmap.process();
        qmap.setMinimum(0);
        
        return qmap;
    }

    private double run(SiteMap map, Site s) {

        List<Site> knn = neighborhoodOf(s, map);
        List<Vector> desired = new ArrayList();
        List<Vector> realized = new ArrayList();
        Vector spos = s.getCell().toVector();
        for (Site nbr : knn) {
            desired.add(Vector.subtract(nbr, s));
            realized.add(Vector.subtract(nbr.getCell().toVector(), spos));
        }

        double a = 0;
        double b = 0;
        double c = 0;
        for (int i = 0; i < knn.size(); i++) {
            Vector d = desired.get(i);
            Vector r = realized.get(i);
            // squared length of scale s * d - r
            // (s dx - rx)^2 + (s dy - ry)^2 
            // s^2 dx^2 - 2 s dx rx + rx^2 + s^2 dy^2 - 2 s dy ry + ry^2 
            // (dx^2 + dy^2) s^2 - 2(dx rx + dy ry) s + (rx^2 + ry^2)
            a += d.squaredLength();
            b -= 2 * Vector.dotProduct(d, r);
            c += r.squaredLength();
        }

        double scale = -b / (2 * a);
        double cost = a * scale * scale + b * scale + c;

        return cost / knn.size();
    }

}
