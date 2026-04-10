package arrange.algorithms.graph;

import common.dual.Dual;
import common.dual.Edge;
import common.dual.Vertex;
import java.util.HashSet;
import java.util.Set;
import nl.tue.geometrycore.algorithms.dsp.DepthFirstSearch;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.LineSegment;

/**
 *
 * @author Rafael Cano, Max Sondag, Wouter Meulemans
 */
public final class Separator {

    public final Vertex v1;
    public final Vertex v2;
    public final Set<Vertex> component1;
    public final double desiredAngle;

    public Separator(Dual dual, Edge cutEdge) {
        Vertex source = cutEdge.getStart();
        Vertex target = cutEdge.getEnd();

        DepthFirstSearch<Dual, LineSegment, Vertex, Edge> dfs = new DepthFirstSearch<>(dual);

        int sourceSize = dfs.run(source, cutEdge);
        int targetSize = dual.vertexCount() - sourceSize;

        // all elements in the source components have a prev != null, except the source
        // all elements in the target component have a prev == null
        component1 = new HashSet<>();
        if (sourceSize <= targetSize) {
            v1 = source;
            v2 = target;
            component1.add(source);
            for (Vertex v : dual.getVertices()) {
                if (dfs.getPrevious(v) != null) {
                    component1.add(v);
                }
            }
        } else {
            v1 = target;
            v2 = source;
            for (Vertex v : dual.getVertices()) {
                if (dfs.getPrevious(v) == null && v != source) {
                    component1.add(v);
                }
            }
        }
                
        Vector diffBaryFace = Vector.subtract(v2.getPartition().centroid(), v1.getPartition().centroid());
        desiredAngle = Math.atan2(diffBaryFace.getY(), diffBaryFace.getX());   
    }
}
