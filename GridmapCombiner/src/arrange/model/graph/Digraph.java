package arrange.model.graph;

/**
 * Class that models a directed graph.
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class Digraph extends GenericDigraph<Digraph.Vertex, Digraph.Edge> {

    /**
     * Creates an empty digraph.
     */
    public Digraph() {
        super();
    }

    /**
     * Creates a digraph with 'numVertices' vertices and no edges.
     */
    public Digraph(int numVertices) {
        super(numVertices);
    }

    /**
     * Creates a Digraph object with the same structure as the given digraph.
     * The new object is completely independent of the original, i.e., this
     * constructor returns a deep copy of the given graph. Corresponding
     * vertices and edges will have the same id in both instances.
     */
    public Digraph(GenericDigraph<?, ?> other) {
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
            return Digraph.this.getSource(this);
        }

        public Vertex getTarget() {
            return Digraph.this.getTarget(this);
        }
    }
}
