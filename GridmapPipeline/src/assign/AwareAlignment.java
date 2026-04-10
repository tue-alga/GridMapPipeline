package assign;

import assign.aware.NonuniformAwareAlignment;
import assign.aware.UniformAwareAlignment;
import common.Partition;
import common.Site;
import common.util.Transform;
import java.util.ArrayList;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;

/**
 *
 * @author Wouter Meulemans
 */
public abstract class AwareAlignment extends Alignment {
    
     public AwareAlignment(String name) {
        super(name);
    }
    
    @Override
    public Transform construct(Partition p) {
        List<Vector> centroids = new ArrayList();
        for (Site s : p.sites) {
            centroids.add(s.getCell().toVector());
        }
        return construct(p.sites, centroids);
    }

    public abstract Transform construct(List<? extends Vector> sites, List<? extends Vector> cells);

    public static AwareAlignment[] methods = {
        new UniformAwareAlignment(),
        new NonuniformAwareAlignment()
    };
    
}
