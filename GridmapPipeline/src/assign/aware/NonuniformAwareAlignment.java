package assign.aware;

import static assign.Alignment.centroid;
import assign.AwareAlignment;
import common.util.Transform;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;

/**
 *
 * @author Wouter Meulemans
 */
public class NonuniformAwareAlignment extends AwareAlignment {
    
    public NonuniformAwareAlignment() {
        super("Nonuniform-Aware");
    }
    
    @Override
    public Transform construct(List<? extends Vector> sites, List<? extends Vector> cells) {
        
        Vector sitecentroid = centroid(sites);
        Vector cellcentroid = centroid(cells);

        double sumofxsqr = 0, sumofysqr = 0;
        double sumofxdot = 0, sumofydot = 0;
        for (int i = 0; i < sites.size(); i++) {
            Vector dsite = Vector.subtract(sites.get(i), sitecentroid);
            Vector dcell = Vector.subtract(cells.get(i), cellcentroid);

            sumofxsqr += dsite.getX()*dsite.getX();
            sumofxdot += dsite.getX() * dcell.getX();
            
            sumofysqr += dsite.getY() * dsite.getY();
            sumofydot += dsite.getY() * dcell.getY();
        }

        double scaleX = sumofxdot / sumofxsqr;
        double scaleY = sumofydot / sumofysqr;

        sitecentroid.scale(scaleX, scaleY);

        // then translate to align
        Vector translate = Vector.subtract(cellcentroid, sitecentroid);

        return new Transform(translate, scaleX, scaleY);
    }
}
