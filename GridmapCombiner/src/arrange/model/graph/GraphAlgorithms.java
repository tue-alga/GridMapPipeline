package arrange.model.graph;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Basic graph algorithms.
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class GraphAlgorithms {

    private GraphAlgorithms() {
    }

    /**
     * Connectivity test using DFS. Stops immediately if more than one component
     * is found.
     */
    public static <V extends AbstractVertex, E extends AbstractEdge> boolean isConnected(GenericGraph<V, E> g) {
        class LocalVisitor extends DepthFirstSearchTraverser.Visitor<V, E> {

            boolean first = true;
            boolean isConnected = true;

            @Override
            public void startVertex(DepthFirstSearchTraverser<V, E> traverser, V v) {
                if (!first) {
                    isConnected = false;
                    traverser.stop();
                } else {
                    first = false;
                }

            }
        }

        LocalVisitor visitor = new LocalVisitor();
        DepthFirstSearchTraverser<V, E> traverser = new DepthFirstSearchTraverser<>(g, visitor);
        traverser.traverse();
        return visitor.isConnected;
    }

    /**
     * Acyclicity test using DFS. Stops immediately if a cycle is found.
     */
    public static <V extends AbstractVertex, E extends AbstractEdge> boolean isAcyclic(GenericGraph<V, E> g) {
        class LocalVisitor extends DepthFirstSearchTraverser.Visitor<V, E> {

            boolean isAcyclic = true;

            @Override
            public void preExploreEdge(DepthFirstSearchTraverser<V, E> traverser, E e) {
                if (traverser.getEdgeType(e) == DepthFirstSearchTraverser.EdgeType.BACK) {
                    isAcyclic = false;
                    traverser.stop();
                }
            }
        }

        LocalVisitor visitor = new LocalVisitor();
        DepthFirstSearchTraverser<V, E> traverser = new DepthFirstSearchTraverser<>(g, visitor);
        traverser.traverse();
        return visitor.isAcyclic;
    }

    /**
     * Returns the cut edges of the specified graph. Horrible quadratic time
     * implementation, needs to be improved if anything more than simple
     * preprocessing is required.
     */
    public static <V extends AbstractVertex, E extends AbstractEdge> LinkedHashSet<E> cutEdges(GenericGraph<V, E> g) {
        LinkedHashSet<E> edges = new LinkedHashSet<>();
        for (E e : g.edges()) {
            Graph copy = new Graph(g);
            Graph.Edge eCopy = copy.getEdge(e.getId());
            Graph.Vertex source = copy.getSource(eCopy);
            Graph.Vertex target = copy.getTarget(eCopy);
            copy.removeEdge(eCopy);
            List<Graph.Vertex> path = DijkstraShortestPath.shortestPath(copy, source, target);
            if (path == null) {
                edges.add(e);
            }
        }
        return edges;
    }
}
