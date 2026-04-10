package arrange.model.graph;

import arrange.model.util.Position2D;
import arrange.model.util.Vector2D;

/**
 * Class that models a straight line planar graph.
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class PlanarStraightLineGraph
        extends GenericGraph<PlanarStraightLineGraph.Vertex, PlanarStraightLineGraph.Edge> {

    /**
     * Creates an empty graph.
     */
    public PlanarStraightLineGraph() {
        super();
    }

    /**
     * Creates a graph with 'numVertices' vertices and no edges.
     */
    public PlanarStraightLineGraph(int numVertices) {
        super(numVertices);
    }

    /**
     * Creates a Graph object with the same structure as the given graph. The
     * new object is completely independent of the original, i.e., this
     * constructor returns a deep copy of the given graph. Corresponding
     * vertices and edges will have the same id in both instances.
     */
    public PlanarStraightLineGraph(PlanarStraightLineGraph graph) {
        super(graph);
        if (graph instanceof PlanarStraightLineGraph) {
            PlanarStraightLineGraph originalGraph = (PlanarStraightLineGraph) graph;
            for (int i = 0; i < numberOfVertices(); i++) {
                Vertex originalVertex = originalGraph.getVertex(i);
                Vertex newVertex = (Vertex) super.getVertex(i);
                newVertex.getPosition().setX(originalVertex.getPosition().getX());
                newVertex.getPosition().setY(originalVertex.getPosition().getY());
            }
        }
    }

    /**
     * Adds a vertex to the graph with at the specified position. Vertex ids
     * assigned previously remain valid.
     */
    public Vertex addVertex(Vector2D position) {
        Vertex v = addVertex();
        v.setPosition(position);
        return v;
    }

    @Override
    protected Vertex createVertex() {
        return new Vertex();
    }

    @Override
    protected Edge createEdge() {
        return new Edge();
    }

    public class Vertex extends AbstractVertex implements Position2D {

        private Vector2D position = new Vector2D(0, 0);

        protected Vertex() {
            super();
        }

        @Override
        public Vector2D getPosition() {
            return position;
        }

        public void setPosition(Vector2D position) {
            this.position = position;
        }

        public double distance(Vertex v) {
            return Vector2D.difference(position, v.position).norm();
        }
    }

    public class Edge extends AbstractEdge {

        protected Edge() {
            super();
        }

        public Vertex getSource() {
            return PlanarStraightLineGraph.this.getSource(this);
        }

        public Vertex getTarget() {
            return PlanarStraightLineGraph.this.getTarget(this);
        }

        public double length() {
            return getSource().distance(getTarget());
        }
    }
}
