package assign;

import common.util.Transform;
import assign.alignments.BoundingBoxAgnosticAlignment;
import assign.alignments.NonuniformAgnosticAlignment;
import assign.alignments.UniformAgnosticAlignment;
import common.Partition;
import common.gridmath.GridMath.Coordinate;
import java.util.ArrayList;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;

/**
 *
 * @author Wouter Meulemans
 */
public abstract class AgnosticAlignment extends Alignment {

    public AgnosticAlignment(String name) {
        super(name);
    }
    
    @Override
    public Transform construct(Partition p) {
        List<Vector> centroids = new ArrayList();
        for (Coordinate c : p.cells) {
            centroids.add(c.toVector());
        }
        return construct(p.sites, centroids);
    }

    public abstract Transform construct(List<? extends Vector> sites, List<? extends Vector> cells);

    public static AgnosticAlignment[] methods = {
        new BoundingBoxAgnosticAlignment(),
        new NonuniformAgnosticAlignment(),
        new UniformAgnosticAlignment()
    };
}
