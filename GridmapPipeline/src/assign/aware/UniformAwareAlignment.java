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
public class UniformAwareAlignment extends AwareAlignment {

    public UniformAwareAlignment() {
        super("Uniform-Aware");
    }
    
    @Override
    public Transform construct(List<? extends Vector> sites, List<? extends Vector> cells) {
        
        Vector sitecentroid = centroid(sites);
        Vector cellcentroid = centroid(cells);

        double sumofsquares = 0;
        double sumofdots = 0;
        for (int i = 0; i < sites.size(); i++) {
            Vector dsite = Vector.subtract(sites.get(i), sitecentroid);
            Vector dcell = Vector.subtract(cells.get(i), cellcentroid);

            sumofsquares += dsite.squaredLength();
            sumofdots += Vector.dotProduct(dsite, dcell);
        }

        double scale = sumofdots / sumofsquares;

        sitecentroid.scale(scale);

        // then translate to align
        Vector translate = Vector.subtract(cellcentroid, sitecentroid);

        return new Transform(translate, scale);
    }
}
