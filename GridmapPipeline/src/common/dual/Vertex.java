package common.dual;

import common.Partition;
import nl.tue.geometrycore.geometry.linear.LineSegment;
import nl.tue.geometrycore.graphs.simple.SimpleVertex;

/**
 *
 * @author Wouter Meulemans
 */
public class Vertex extends SimpleVertex<LineSegment, Vertex, Edge> {

    private Partition partition;

    public void setPartition(Partition partition) {
        this.partition = partition;
    }

    public Partition getPartition() {
        return partition;
    }

}
