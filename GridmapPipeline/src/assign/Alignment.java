package assign;

import common.Partition;
import common.util.Transform;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;

/**
 *
 * @author Wouter Meulemans
 */
public abstract class Alignment {
    
     private final String name;

    public Alignment(String name) {
        this.name = name;
    }
    
    public abstract Transform construct(Partition p);

    @Override
    public String toString() {
        return name;
    }

    public static Vector centroid(List<? extends Vector> vs) {
        Vector c = Vector.origin();
        for (Vector v : vs) {
            c.translate(v);
        }
        c.scale(1.0 / vs.size());
        return c;
    }
}
