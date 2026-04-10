package arrange.model.graph;

/**
 * Class that models an undirected graph.
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class Graph extends GenericGraph<Graph.Vertex, Graph.Edge> {

    /**
     * Creates an empty graph.
     */
    public Graph() {
        super();
    }

    /**
     * Creates a graph with 'numVertices' vertices and no edges.
     */
    public Graph(int numVertices) {
        super(numVertices);
    }

    /**
     * Creates a Graph object with the same structure as the given graph. The
     * new object is completely independent of the original, i.e., this
     * constructor returns a deep copy of the given graph. Corresponding
     * vertices and edges will have the same id in both instances.
     */
    public Graph(GenericGraph<?, ?> other) {
        super(other);
    }

    @Override
    protected Vertex createVertex() {
        return new Vertex();
    }

    @Override
    protected Edge createEdge() {
        return new Edge();
    }

    public class Vertex extends AbstractVertex {

        protected Vertex() {
            super();
        }
    }

    public class Edge extends AbstractEdge {

        protected Edge() {
            super();
        }

        public Vertex getSource() {
            return Graph.this.getSource(this);
        }

        public Vertex getTarget() {
            return Graph.this.getTarget(this);
        }
    }
}