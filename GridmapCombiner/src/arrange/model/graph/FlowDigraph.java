package arrange.model.graph;

/**
 * Digraph aimed at flow problems. Edges have capacities and weights, vertices
 * have a supply. A positive vertex supply means that it is actually supplying
 * flow to the network. A negative supply represents the opposite, i.e., a
 * demand.
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class FlowDigraph extends GenericDigraph<FlowDigraph.Vertex, FlowDigraph.Edge> {

    public final static int MAX_VALUE = 1000000;

    /**
     * Creates an empty FlowDigraph.
     */
    public FlowDigraph() {
        super();
    }

    /**
     * Creates a FlowDigraph with 'numVertices' vertices and no edges.
     */
    public FlowDigraph(int numVertices) {
        super(numVertices);
    }

    /**
     * Creates a Digraph object with the same structure as the given digraph.
     * The new object is completely independent of the original, i.e., this
     * constructor returns a deep copy of the given graph. Corresponding
     * vertices and edges will have the same id in both instances.
     */
    public FlowDigraph(GenericDigraph<?, ?> other) {
        super(other);
        for (AbstractEdge abs : other.edges()) {
            Edge e = getEdge(abs.getId());
            if (abs instanceof Capacity) {
                int capacity = ((Capacity) abs).getCapacity();
                e.setCapacity(capacity);
            }
            if (abs instanceof Weight) {
                int weight = ((Weight) abs).getWeight();
                e.setWeight(weight);
            }
        }
        for (AbstractVertex abs : other.vertices()) {
            if (abs instanceof Supply) {
                Vertex v = getVertex(abs.getId());
                int supply = ((Supply) abs).getSupply();
                v.setSupply(supply);
            }
        }
    }

    @Override
    protected Vertex createVertex() {
        return new Vertex();
    }

    @Override
    protected Edge createEdge() {
        return new Edge();
    }

    public class Vertex extends AbstractVertex implements Supply, Capacity {

        private int supply = 0;
        private int capacity = MAX_VALUE;

        protected Vertex() {
            super();
        }

        @Override
        public int getSupply() {
            return supply;
        }

        public void setSupply(int supply) {
            this.supply = supply;
        }

        @Override
        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public String name;

        @Override
        public String toString() {
            return "Vertex " + name + ", supply = " + supply + ", cap = " + numberString(capacity);
        }

        private String numberString(int n) {
            if (n == MAX_VALUE) {
                return "INF";
            } else {
                return Integer.toString(n);
            }
        }
    }

    public class Edge extends AbstractEdge implements Capacity, Weight {

        private int capacity = MAX_VALUE;
        private int weight = 0;

        protected Edge() {
            super();
        }

        public Vertex getSource() {
            return FlowDigraph.this.getSource(this);
        }

        public Vertex getTarget() {
            return FlowDigraph.this.getTarget(this);
        }

        @Override
        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        @Override
        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }

        @Override
        public String toString() {
            return "Edge " + getSource().name + " -> " + getTarget().name
                    + ", cap = " + numberString(capacity) + ", cost = " + weight;
        }

        private String numberString(int n) {
            if (n == MAX_VALUE) {
                return "INF";
            } else {
                return Integer.toString(n);
            }
        }
    }
}
