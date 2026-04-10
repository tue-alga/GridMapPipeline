package common.dual;

import common.Partition;
import common.SiteMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import nl.tue.geometrycore.datastructures.quadtree.PointQuadTree;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.LineSegment;
import nl.tue.geometrycore.geometry.linear.Rectangle;
import nl.tue.geometrycore.graphs.simple.SimpleGraph;

/**
 *
 * @author Wouter Meulemans
 */
public class Dual extends SimpleGraph<LineSegment, Vertex, Edge> {

    private int total_weight = -1;

    public int getTotalWeight() {
        return total_weight;
    }

    public Vertex addVertex(Partition p) {
        Vertex v = addVertex(p.getLabelPoint());
        v.setPartition(p);
        return v;
    }

    public Edge addEdge(Vertex a, Vertex b) {
        return addEdge(a, b, new LineSegment(a.clone(), b.clone()));
    }

    public Iterable<Partition> primalPartitions() {
        return () -> new Iterator<>() {
            Iterator<Vertex> vit = getVertices().iterator();

            @Override
            public boolean hasNext() {
                return vit.hasNext();
            }

            @Override
            public Partition next() {
                return vit.next().getPartition();
            }
        };
    }

    public List<Dual> connectedComponents() {

        List<Dual> components = new ArrayList();

        Vertex[] map = new Vertex[vertexCount()]; // from this dual to the component vertex

        for (Vertex v : getVertices()) {
            if (map[v.getGraphIndex()] == null) {

                Dual comp = new Dual();
                components.add(comp);

                Stack<Vertex> explore = new Stack<>();
                explore.push(v);
                map[v.getGraphIndex()] = comp.addVertex(v.getPartition());

                while (!explore.isEmpty()) {
                    Vertex ex = explore.pop();

                    for (Vertex nbr : ex.getNeighbors()) {
                        if (map[nbr.getGraphIndex()] == null) {
                            explore.push(nbr);
                            map[nbr.getGraphIndex()] = comp.addVertex(nbr.getPartition());
                        }

                        comp.addEdge(map[ex.getGraphIndex()], map[nbr.getGraphIndex()]); // NB: will only add the edge if it doesn't exist yet
                    }
                }

                comp.total_weight = 0;
                for (Vertex cv : comp.getVertices()) {
                    comp.total_weight += cv.getPartition().getWeight();
                }
            }
        }

        return components;
    }

    public static Dual construct(SiteMap map) {
        // we define adjacent as sharing >= 2 consecutive vertices, which works well enough for most cases
        Dual dual = new Dual();
        PointQuadTree<PartitionVertex> pqt = new PointQuadTree<>(Rectangle.byBoundingBox(map.outlines), 10);

        for (Partition p : map.partitions()) {

            Vertex vtx = dual.addVertex(p);
            List<PartitionVertex> prev = pqt.findStabbed(p.vertex(-1));
            for (Vector v : p.vertices()) {
                List<PartitionVertex> next = pqt.findStabbed(v);

                for (PartitionVertex nv : next) {
                    for (PartitionVertex pv : prev) {
                        if (nv.partition == pv.partition) {
                            if (vtx.getEdgeTo(nv.partition) == null) {
                                dual.addEdge(vtx, nv.partition);
                            }
                        }
                    }
                }

                prev = next;
            }

            for (Vector v : p.vertices()) {
                pqt.insert(new PartitionVertex(vtx, v));
            }
        }

        dual.total_weight = 0;
        for (Vertex v : dual.getVertices()) {
            dual.total_weight += v.getPartition().getWeight();
        }

        return dual;
    }

    public int vertexCount() {
        return getVertices().size();
    }

    public int edgeCount() {
        return getVertices().size();
    }

    private static class PartitionVertex extends Vector {

        Vertex partition;

        public PartitionVertex(Vertex vtx, Vector clone) {
            super(clone);
            this.partition = vtx;
        }
    }

}
