package arrange.algorithms.graph;

import arrange.util.Identifier;
import arrange.util.Position2D;
import common.dual.Dual;
import nl.tue.geometrycore.geometry.Vector;
import java.util.ArrayList;
import nl.tue.geometrycore.geometry.linear.LineSegment;

/**
 * A representation of an undirected network (graph).
 *
 * <p>
 * This network is represented by a set of vertices, that each contain their
 * neighbors.</p>
 * 
 * @author Rafael Cano
 */
public class Network {

    private ArrayList<Vertex> vertices;
    private ArrayList<Edge> edges;

    /**
     * Creates a Network object with the same structure as the given graph. The
     * new object is completely independent of the original, i.e., this
     * constructor returns a deep copy of the given graph. Corresponding
     * vertices and edges will have the same id in both instances.
     */
    public Network(Network other) {
        vertices = new ArrayList<>(other.numberOfVertices() + 10);
        edges = new ArrayList<>(other.numberOfEdges() + 10);
        initialize(other.numberOfVertices());
        for (Edge otherEdge : other.edges()) {
            Vertex source = vertices.get(otherEdge.source.id);
            Vertex target = vertices.get(otherEdge.target.id);
            Edge e = new Edge(source, target);
            source.addLink(target, e);
            target.addLink(source, e);
            edges.add(e);
        }
    }

    public Network(Dual dual) {
        vertices = new ArrayList<>(dual.vertexCount()+ 10);
        edges = new ArrayList<>(dual.edgeCount() + 10);
        for (common.dual.Vertex v : dual.getVertices()) {
            addVertex(v);
        }
        for (common.dual.Edge e : dual.getEdges()) {
            addEdge(e.getStart().getGraphIndex(), e.getEnd().getGraphIndex());
        }
    }

    public Vertex addVertex(Vector position) {
        Vertex v = new Vertex(position);
        vertices.add(v);
        return v;
    }

    public Edge addEdge(Vertex source, Vertex target) {
        Edge e = new Edge(source, target);
        source.addLink(target, e);
        target.addLink(source, e);
        edges.add(e);
        return e;
    }

    public Edge addEdge(int sourceId, int targetId) {
        return addEdge(vertices.get(sourceId), vertices.get(targetId));
    }

    public void removeVertex(Vertex v) {
        // Mark edges to be deleted
        int first = Integer.MAX_VALUE;
        for (int i = 0; i < v.getDegree(); i++) {
            Vertex u = v.getNeighbor(i);
            u.removeLink(v);
            Edge e = v.getIncidentEdge(i);
            int id = e.getId();
            edges.set(id, null);
            if (id < first) {
                first = id;
            }
        }

        // Delete marked edges
        if (first != Integer.MAX_VALUE) {
            int i = first;
            int j = first + 1;
            while (j < edges.size()) {
                Edge e = edges.get(j);
                if (e != null) {
                    edges.set(i, e);
                    e.id = i;
                    i++;
                }
                j++;
            }
            edges.subList(i, j).clear();
        }

        // Delete vertex
        int id = v.getId();
        vertices.remove(id);
        for (int i = id; i < vertices.size(); i++) {
            vertices.get(i).id = i;
        }

    }

    public void removeEdge(Edge e) {
        Vertex source = e.getSource();
        Vertex target = e.getTarget();
        source.removeLink(target);
        target.removeLink(source);
        int id = e.getId();
        edges.remove(id);
        for (int i = id; i < edges.size(); i++) {
            edges.get(i).id = i;
        }
    }

    public final int numberOfVertices() {
        return vertices.size();
    }

    public final int numberOfEdges() {
        return edges.size();
    }

    public Iterable<Vertex> vertices() {
        return vertices;
    }

    public Iterable<Edge> edges() {
        return edges;
    }

    public Vertex getVertex(int id) {
        return vertices.get(id);
    }

    public Edge getEdge(int id) {
        return edges.get(id);
    }

    public Edge getEdge(Vertex source, Vertex target) {
        if (source.getDegree() > target.getDegree()) {
            Vertex aux = source;
            source = target;
            target = aux;
        }
        int index = source.indexOfNeighbor(target);
        if (index < 0) {
            return null;
        } else {
            return source.getIncidentEdge(index);
        }
    }

    /**
     * Returns true if the graph contains an edge between the source and the
     * target, false otherwise.
     */
    public boolean hasEdge(Vertex source, Vertex target) {
        return getEdge(source, target) != null;
    }

    public void clear() {
        vertices.clear();
        edges.clear();
    }

    /**
     * Initializes a graph with 'numVertices' vertices and no edges.
     */
    private void initialize(int numVertices) {
        for (int i = 0; i < numVertices; i++) {
            Vertex v = new Vertex(Vector.origin());
            vertices.add(v);
        }
    }

    public class Vertex implements Position2D, Identifier {

        private int id;
        private Vector position;
        private final ArrayList<Edge> incidentEdges = new ArrayList<>();
        private final ArrayList<Vertex> neighbors = new ArrayList<>();

        public Vertex(Vector position) {
            id = vertices.size();
            this.position = position;
        }

        @Override
        public final int getId() {
            return id;
        }

        private void addLink(Vertex v, Edge e) {
            neighbors.add(v);
            incidentEdges.add(e);
        }

        private void removeLink(Vertex v) {
            int i = neighbors.indexOf(v);
            neighbors.remove(i);
            incidentEdges.remove(i);
        }

        private int indexOfNeighbor(Vertex v) {
            return neighbors.indexOf(v);
        }

        public int getDegree() {
            return neighbors.size();
        }

        public Iterable<Edge> getIncidentEdges() {
            return incidentEdges;
        }

        public Iterable<Vertex> getNeighbors() {
            return neighbors;
        }

        public Edge getIncidentEdge(int index) {
            return incidentEdges.get(index);
        }

        public Vertex getNeighbor(int index) {
            return neighbors.get(index);
        }

        @Override
        public Vector getPosition() {
            return position;
        }

        public void setPosition(Vector position) {
            this.position = position;
        }
    }

    public class Edge implements Identifier {

        private int id;
        private final Vertex source;
        private final Vertex target;

        private Edge(Vertex source, Vertex target) {
            this.source = source;
            this.target = target;
            this.id = edges.size();
        }

        @Override
        public final int getId() {
            return id;
        }

        public Vertex getSource() {
            return source;
        }

        public Vertex getTarget() {
            return target;
        }
    }

    public boolean hasCrossings() {
        final int m = numberOfEdges();
        for (int i = 0; i < m; i++) {
            Edge e = getEdge(i);
            LineSegment ls_e = new LineSegment(e.getSource().position, e.getTarget().position);

            for (int j = i + 1; j < m; j++) {
                Edge f = getEdge(j);

                if (e.getSource() == f.getSource() || e.getTarget() == f.getSource()
                        || e.getSource() == f.getTarget() || e.getTarget() == f.getTarget()) {
                    continue;
                }

                LineSegment ls_f = new LineSegment(f.getSource().position, f.getTarget().position);

                if (!ls_e.intersect(ls_f).isEmpty()) {
                    return true;
                }

            }
        }

        return false;
    }
}
