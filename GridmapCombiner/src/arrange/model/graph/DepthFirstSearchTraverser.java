package arrange.model.graph;

import arrange.model.util.ElementList;

/**
 * Depth-first search on an undirected simple graph.
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class DepthFirstSearchTraverser<V extends AbstractVertex, E extends AbstractEdge> extends GraphTraverser {

    private final AbstractGraph<V, E> graph;
    private final Visitor<V, E> visitor;
    private boolean stop;
    private int time;
    private ElementList<VertexColor> color;
    private ElementList<Integer> discoveryTime;
    private ElementList<Integer> finishingTime;
    private ElementList<V> predecessor;
    private ElementList<EdgeType> edgeType;

    public DepthFirstSearchTraverser(AbstractGraph<V, E> graph, Visitor<V, E> visitor) {
        this.graph = graph;
        this.visitor = visitor;
    }

    public DepthFirstSearchTraverser(AbstractGraph<V, E> graph) {
        this.graph = graph;
        this.visitor = new Visitor<>();
    }

    @Override
    public void traverse() {
        initialize();
        try {
            for (V v : graph.vertices()) {
                if (color.get(v) == VertexColor.WHITE) {
                    startVertex(v);
                    visit(v);
                }
            }
        } catch (StoppedTraversalException ex) {
        }
    }

    public void traverse(V start) {
        initialize();
        try {
            startVertex(start);
            visit(start);
            for (V v : graph.vertices()) {
                if (color.get(v) == VertexColor.WHITE) {
                    startVertex(v);
                    visit(v);
                }
            }
        } catch (StoppedTraversalException ex) {
        }
    }

    @Override
    public void stop() {
        stop = true;
    }

    public Integer getDiscoveryTime(V v) {
        return discoveryTime.get(v);
    }

    public Integer getFinishingTime(V v) {
        return finishingTime.get(v);
    }

    public V getPredecessor(V v) {
        return predecessor.get(v);
    }

    public EdgeType getEdgeType(E e) {
        return edgeType.get(e);
    }

    private void initialize() {
        int nv = graph.numberOfVertices();
        int ne = graph.numberOfEdges();
        stop = false;
        time = 0;
        color = new ElementList<>(nv, VertexColor.WHITE);
        discoveryTime = new ElementList<>(nv, null);
        finishingTime = new ElementList<>(nv, null);
        predecessor = new ElementList<>(nv, null);
        edgeType = new ElementList<>(ne, EdgeType.NONE);
    }

    private void visit(V u) throws StoppedTraversalException {
        color.set(u, VertexColor.GRAY);
        discoveryTime.set(u, ++time);
        discoverVertex(u);

        for (int i = 0; i < graph.getDegree(u); i++) {
            V v = graph.getNeighbour(u, i);
            E e = graph.getIncidentEdge(u, i);
            if (color.get(v) == VertexColor.WHITE) {
                predecessor.set(v, u);
                edgeType.set(e, EdgeType.TREE);
                preExploreEdge(e);
                visit(v);
                postExploreEdge(e);
            } else {
                if (edgeType.get(e) == EdgeType.NONE) {
                    edgeType.set(e, EdgeType.BACK);
                    preExploreEdge(e);
                    postExploreEdge(e);
                }
            }
        }

        color.set(u, VertexColor.BLACK);
        finishingTime.set(u, ++time);
        finishVertex(u);
    }

    private void startVertex(V v) throws StoppedTraversalException {
        visitor.startVertex(this, v);
        if (stop) {
            throw new StoppedTraversalException();
        }
    }

    private void discoverVertex(V v) throws StoppedTraversalException {
        visitor.discoverVertex(this, v);
        if (stop) {
            throw new StoppedTraversalException();
        }
    }

    private void preExploreEdge(E e) throws StoppedTraversalException {
        visitor.preExploreEdge(this, e);
        if (stop) {
            throw new StoppedTraversalException();
        }
    }

    private void postExploreEdge(E e) throws StoppedTraversalException {
        visitor.postExploreEdge(this, e);
        if (stop) {
            throw new StoppedTraversalException();
        }
    }

    private void finishVertex(V v) throws StoppedTraversalException {
        visitor.finishVertex(this, v);
        if (stop) {
            throw new StoppedTraversalException();
        }
    }

    private static enum VertexColor {

        WHITE, GRAY, BLACK;
    }

    public static enum EdgeType {

        NONE, TREE, BACK;
    }

    public static class Visitor<V extends AbstractVertex, E extends AbstractEdge> {

        public void startVertex(DepthFirstSearchTraverser<V, E> traverser, V v) {
        }

        public void discoverVertex(DepthFirstSearchTraverser<V, E> traverser, V v) {
        }

        public void preExploreEdge(DepthFirstSearchTraverser<V, E> traverser, E e) {
        }

        public void postExploreEdge(DepthFirstSearchTraverser<V, E> traverser, E e) {
        }

        public void finishVertex(DepthFirstSearchTraverser<V, E> traverser, V v) {
        }
    }
}
