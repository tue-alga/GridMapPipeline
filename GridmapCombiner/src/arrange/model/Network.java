package arrange.model;

import java.awt.Color;
import arrange.model.graph.AbstractEdge;
import arrange.model.graph.AbstractVertex;
import arrange.model.graph.GenericGraph;
import arrange.model.util.Position2D;
import arrange.model.util.Random;
import arrange.model.util.Vector2D;

/**
 * A representation of an undirected network (graph).
 *
 * <p>
 * This network is represented by a set of vertices, that each contain their
 * neighbors.</p>
 */
public class Network extends GenericGraph<Network.Vertex, Network.Edge> {

    private int nextVertexColor = 0;
    private static final Color[] vertexColors = new Color[]{
        new Color(240, 134, 130), // red
        new Color(70, 95, 92), // dark red
        new Color(90, 230, 110), // green
        new Color(19, 183, 83), // dark green
        new Color(28, 179, 255), // blue
        new Color(93, 130, 185), // dark blue
        new Color(55, 235, 85), // yellow
        new Color(93, 115, 176), // purple
        new Color(50, 89, 137), // dark purple
        new Color(55, 150, 40), // orange
        new Color(217, 128, 34), // dark orange
        new Color(179, 146, 93), // brown
        new Color(137, 112, 71), // dark brown
    };

    /**
     * Creates a new network without vertices or edges.
     */
    public Network() {
        super();
    }

    /**
     * Creates a new network with a given number of vertices and no edges.
     *
     * @param numVertices The vertex count.
     */
    public Network(int numVertices) {
        super(numVertices);
    }

    /**
     * Copy constructor.
     *
     * @param toCopy Network to make a copy of.
     */
    public Network(Network graph) {
        super(graph);
        Network originalGraph = graph;
        for (int i = 0; i < originalGraph.numberOfVertices(); i++) {
            Vertex originalVertex = originalGraph.getVertex(i);
            Vertex newVertex = (Vertex) super.getVertex(i);
            newVertex.setColor(originalVertex.getColor());
            newVertex.getPosition().setX(originalVertex.getPosition().getX());
            newVertex.getPosition().setY(originalVertex.getPosition().getY());
            newVertex.getVelocity().setX(originalVertex.getVelocity().getX());
            newVertex.getVelocity().setY(originalVertex.getVelocity().getY());
        }
    }

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

    /**
     * Layouts the network with a force-based layout algorithm. When this method
     * returns, all vertices will have an appropriate x and y coordinate.
     *
     * @param width The width of the containment.
     * @param height The height of the containment.
     * @param eps A small value that indicates the threshold to stop the
     * algorithm.
     * @param initialize Whether the vertices' position should be randomized
     * before starting.
     */
    public void layout(double width, double height, double eps, boolean initialize) {

        // initialize
        if (initialize) {
            randomizePositions(width, height);
        }

        double totalKineticEnergy;

        do {
            totalKineticEnergy = doLayoutStep(1, null, width, height);

        } while (totalKineticEnergy > eps);
    }

    /**
     * Assigns every vertex a random position (uniformly distributed), inside
     * the given bounds. This is meant to be used as an initial position for the
     * layout algorithm.
     *
     * @param width The maximum for the <i>x</i> value.
     * @param height The maximum for the <i>y</i> value.
     */
    public void randomizePositions(double width, double height) {
        for (Vertex v : vertices()) {
            v.getPosition().setX((double) (Random.nextDouble() * width));
            v.getPosition().setY((double) (Random.nextDouble() * height));
            v.getVelocity().setX(0);
            v.getVelocity().setY(0);
        }
    }

    /**
     * Executes a number of steps in the layout algorithm.
     *
     * @param count The number of steps to execute.
     * @param fixed If non-<code>null</code>, the given vertex is kept fixed.
     * That is, the vertex is not being moved. This is useful, for example,
     * while dragging a vertex; it is unwanted that this vertex is moving in the
     * meantime.
     * @return The "total kinetic energy", that indicates how fast the vertices
     * in the graph are still moving.
     */
    public synchronized double doLayoutStep(int count, Vertex fixed, double width, double height) {

        // damping constant
        final double damping = 0.997f;

        // time step
        final double dt = 0.004f;

        // note: this loop is unrolled one time, since we only need to maintain
        // the totalKineticEnergy in the last iteration
        for (int i = 0; i < count - 1; i++) {

            for (Vertex v : vertices()) {
                if (v == fixed) {
                    continue;
                }

                Vector2D force = new Vector2D(0, 0);

                for (Vertex v2 : vertices()) {
                    if (v != v2) {
                        Vector2D coulomb = coulombRepulsion(v, v2);
                        force.add(coulomb);
                    }
                }

                for (Vertex v2 : neighbours(v)) {
                    Vector2D hooke = hookeAttraction(v, v2);
                    force.add(hooke);
                }

                // v.velocity = (v.velocity + dt*force)*damping
                v.getVelocity().add(force.multiply(dt)).multiply(damping);
                // v.position += dt*v.velocity
                v.getPosition().add(Vector2D.product(dt, v.getVelocity()));
            }
        }

        double totalKineticEnergy = 0;

        for (Vertex v : vertices()) {
            if (v == fixed) {
                continue;
            }

            Vector2D force = new Vector2D(0, 0);

            for (Vertex v2 : vertices()) {
                if (v != v2) {
                    Vector2D coulomb = coulombRepulsion(v, v2);
                    force.add(coulomb);
                }
            }

            for (Vertex v2 : neighbours(v)) {
                Vector2D hooke = hookeAttraction(v, v2);
                force.add(hooke);
            }

            // try to keep particles inside the border
            force.setX(force.getX() - (2 * v.getPosition().getX() / width - 1) * 1000);
            force.setY(force.getY() - (2 * v.getPosition().getY() / height - 1) * 1000);

            // v.velocity = (v.velocity + dt*force)*damping
            v.getVelocity().add(force.multiply(dt)).multiply(damping);

            // v.position += dt*v.velocity
            v.getPosition().add(Vector2D.product(dt, v.getVelocity()));

            totalKineticEnergy += Vector2D.dotProduct(v.getVelocity(), v.getVelocity());
        }

        return totalKineticEnergy;
    }

    /**
     * Returns the force that the first vertex experiences from the second
     * vertex.
     *
     * @param v The first vertex.
     * @param v2 The second vertex.
     * @return The force, as an {@link Vector2D}.
     */
    private Vector2D coulombRepulsion(Vertex v, Vertex v2) {

        // the "ideal" distance between vertices
        double k = 25d;

        double d = v.distance(v2);
        assert d > 0;

        double f = -(k * k) / d;
        assert f < 0;

        Vector2D force = Vector2D.difference(v2.getPosition(), v.getPosition()).multiply(f / d);
        return force;
    }

    /**
     * Returns the force that the first vertex experiences from the second
     * vertex.
     *
     * @param v The first vertex.
     * @param v2 The second vertex.
     * @return The force, as an {@link Vector2D}.
     */
    private Vector2D hookeAttraction(Vertex v, Vertex v2) {

        // the "ideal" distance between vertices
        double k = 100f;

        double d = v.distance(v2);
        assert d >= 0;

        double f = (d * d) / k;
        assert f >= 0;

        Vector2D force = Vector2D.difference(v2.getPosition(), v.getPosition()).multiply(f / d);
        return force;
    }

    

    public class Vertex extends AbstractVertex implements Position2D {

        private Color color;
        private Vector2D position = new Vector2D(0, 0);
        private Vector2D velocity = new Vector2D(0, 0);

        /**
         * Creates a new (normal) vertex with some nice color.
         */
        protected Vertex() {
            super();
            color = vertexColors[nextVertexColor++];
            if (nextVertexColor >= vertexColors.length) {
                nextVertexColor = 0;
            }
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
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

        public Vector2D getVelocity() {
            return velocity;
        }

        public void setVelocity(Vector2D velocity) {
            this.velocity = velocity;
        }
    }

    public class Edge extends AbstractEdge {

        protected Edge() {
            super();
        }

        public Vertex getSource() {
            return Network.this.getSource(this);
        }

        public Vertex getTarget() {
            return Network.this.getTarget(this);
        }

        public double length() {
            return getSource().distance(getTarget());
        }
    }
}
