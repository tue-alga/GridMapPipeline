package arrange.model.graph;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Generic class that models an undirected graph.
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public abstract class GenericGraph<V extends AbstractVertex, E extends AbstractEdge>
        extends AbstractGraph<V, E> {

    private ArrayList<Vertex> vertices;
    private ArrayList<Edge> edges;

    /**
     * Creates an empty graph.
     */
    public GenericGraph() {
        this(0);
    }

    /**
     * Creates a graph with 'numVertices' vertices and no edges.
     */
    public GenericGraph(int numVertices) {
        vertices = new ArrayList<>(numVertices + 10);
        edges = new ArrayList<>(6 * numVertices + 10);
        initialize(numVertices);
    }

    /**
     * Creates a Graph object with the same structure as the given graph. The
     * new object is completely independent of the original, i.e., this
     * constructor returns a deep copy of the given graph. Corresponding
     * vertices and edges will have the same id in both instances.
     */
    public <V2 extends AbstractVertex, E2 extends AbstractEdge> GenericGraph(GenericGraph<V2, E2> other) {
        vertices = new ArrayList<>(other.numberOfVertices() + 10);
        edges = new ArrayList<>(other.numberOfEdges() + 10);
        initialize(other.numberOfVertices());
        for (E2 otherEdge : other.edges()) {
            Vertex source = vertices.get(other.getSource(otherEdge).getId());
            Vertex target = vertices.get(other.getTarget(otherEdge).getId());
            Edge e = new Edge(source, target);
            source.addLink(target, e);
            target.addLink(source, e);
            edges.add(e);
        }
    }

    @Override
    public V addVertex() {
        Vertex v = new Vertex();
        vertices.add(v);
        return v.getUserVertex();
    }

    @Override
    public E addEdge(V source, V target) {
        Vertex innerSource = vertices.get(source.getId());
        Vertex innerTarget = vertices.get(target.getId());
        Edge e = pAddEdge(innerSource, innerTarget);
        return e.getUserEdge();
    }

    @Override
    public E addEdge(int sourceId, int targetId) {
        Vertex innerSource = vertices.get(sourceId);
        Vertex innerTarget = vertices.get(targetId);
        Edge e = pAddEdge(innerSource, innerTarget);
        return e.getUserEdge();
    }

    @Override
    public void removeVertex(V v) {
        pRemoveVertex(vertices.get(v.getId()));
    }

    @Override
    public void removeEdge(E e) {
        pRemoveEdge(edges.get(e.getId()));
    }

    @Override
    public final int numberOfVertices() {
        return vertices.size();
    }

    @Override
    public final int numberOfEdges() {
        return edges.size();
    }

    @Override
    public Iterable<V> vertices() {
        return new VertexIterable(vertices);
    }
    
    

    @Override
    public Iterable<E> edges() {
        return new EdgeIterable(edges);
    }

    @Override
    public V getVertex(int id) {
        return vertices.get(id).getUserVertex();
    }

    @Override
    public E getEdge(int id) {
        return edges.get(id).getUserEdge();
    }

    @Override
    public E getEdge(V source, V target) {
        Edge e = pGetEdge(vertices.get(source.getId()), vertices.get(target.getId()));
        if (e != null) {
            return e.getUserEdge();
        }
        return null;
    }

    @Override
    public int getDegree(V v) {
        return vertices.get(v.getId()).getDegree();
    }

    /**
     * Equivalent to getDegree(v).
     */
    @Override
    public int getIndegree(V v) {
        return vertices.get(v.getId()).getDegree();
    }

    /**
     * Equivalent to getDegree(v).
     */
    @Override
    public int getOutdegree(V v) {
        return vertices.get(v.getId()).getDegree();
    }

    @Override
    public Iterable<E> incidentEdges(V v) {
        return new EdgeIterable(vertices.get(v.getId()).getIncidentEdges());
    }

    @Override
    public E getIncidentEdge(V v, int index) {
        return vertices.get(v.getId()).getIncidentEdge(index).getUserEdge();
    }

    /**
     * Equivalent to incidentEdges(v).
     */
    @Override
    public Iterable<E> incomingEdges(V v) {
        return new EdgeIterable(vertices.get(v.getId()).getIncidentEdges());
    }

    /**
     * Equivalent to getIncidentEdge(v, index).
     */
    @Override
    public E getIncomingEdge(V v, int index) {
        return vertices.get(v.getId()).getIncidentEdge(index).getUserEdge();
    }

    /**
     * Equivalent to incidentEdges(v).
     */
    @Override
    public Iterable<E> outgoingEdges(V v) {
        return new EdgeIterable(vertices.get(v.getId()).getIncidentEdges());
    }

    /**
     * Equivalent to getIncidentEdge(v, index).
     */
    @Override
    public E getOutgoingEdge(V v, int index) {
        return vertices.get(v.getId()).getIncidentEdge(index).getUserEdge();
    }

    @Override
    public Iterable<V> neighbours(V v) {
        return new VertexIterable(vertices.get(v.getId()).getNeighbours());
    }

    @Override
    public V getNeighbour(V v, int index) {
        return vertices.get(v.getId()).getNeighbour(index).getUserVertex();
    }

    /**
     * Equivalent to neighbours(v).
     */
    @Override
    public Iterable<V> inneighbours(V v) {
        return new VertexIterable(vertices.get(v.getId()).getNeighbours());
    }

    /**
     * Equivalent to getNeighbour(v, index).
     */
    @Override
    public V getInneighbour(V v, int index) {
        return vertices.get(v.getId()).getNeighbour(index).getUserVertex();
    }

    /**
     * Equivalent to neighbours(v).
     */
    @Override
    public Iterable<V> outneighbours(V v) {
        return new VertexIterable(vertices.get(v.getId()).getNeighbours());
    }

    /**
     * Equivalent to getNeighbour(v, index).
     */
    @Override
    public V getOutneighbour(V v, int index) {
        return vertices.get(v.getId()).getNeighbour(index).getUserVertex();
    }

    @Override
    public V getSource(E e) {
        return edges.get(e.getId()).getSource().getUserVertex();
    }

    @Override
    public V getTarget(E e) {
        return edges.get(e.getId()).getTarget().getUserVertex();
    }

    /**
     * Returns true if the graph contains an edge between the source and the
     * target, false otherwise.
     */
    public boolean hasEdge(V source, V target) {
        return getEdge(source, target) != null;
    }

    @Override
    public void clear() {
        vertices.clear();
        edges.clear();
    }

    /**
     * Initializes a graph with 'numVertices' vertices and no edges.
     */
    private void initialize(int numVertices) {
        for (int i = 0; i < numVertices; i++) {
            Vertex v = new Vertex();
            vertices.add(v);
        }
    }

    private Edge pAddEdge(Vertex source, Vertex target) {
        Edge e = new Edge(source, target);
        source.addLink(target, e);
        target.addLink(source, e);
        edges.add(e);
        return e;
    }

    private void pRemoveVertex(Vertex v) {
        // Mark edges to be deleted
        int first = Integer.MAX_VALUE;
        for (int i = 0; i < v.getDegree(); i++) {
            Vertex u = v.getNeighbour(i);
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
                    e.setId(i);
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
            vertices.get(i).setId(i);
        }
    }

    private void pRemoveEdge(Edge e) {
        Vertex source = e.getSource();
        Vertex target = e.getTarget();
        source.removeLink(target);
        target.removeLink(source);
        int id = e.getId();
        edges.remove(id);
        for (int i = id; i < edges.size(); i++) {
            edges.get(i).setId(i);
        }
    }

    private Edge pGetEdge(Vertex source, Vertex target) {
        if (source.getDegree() > target.getDegree()) {
            Vertex aux = source;
            source = target;
            target = aux;
        }
        int index = ((Vertex) source).indexOfNeighbour(target);
        if (index < 0) {
            return null;
        } else {
            return ((Vertex) source).getIncidentEdge(index);
        }
    }

    /**
     * Returns the next id available for a vertex.
     */
    private int nextVertexId() {
        return vertices.size();
    }

    /**
     * Returns the next id available for an edge.
     */
    private int nextEdgeId() {
        return edges.size();
    }

    /**
     * Inner class to model vertices.
     */
    private class Vertex {

        private final ArrayList<Edge> incidentEdges = new ArrayList<>();
        private final ArrayList<Vertex> neighbours = new ArrayList<>();
        private final V userVertex;

        private Vertex() {
            userVertex = createVertex();
            userVertex.setId(nextVertexId());
        }

        private V getUserVertex() {
            return userVertex;
        }

        private void addLink(Vertex v, Edge e) {
            neighbours.add(v);
            incidentEdges.add(e);
        }

        private void removeLink(Vertex v) {
            int i = neighbours.indexOf(v);
            neighbours.remove(i);
            incidentEdges.remove(i);
        }

        private int indexOfNeighbour(Vertex v) {
            return neighbours.indexOf(v);
        }

        private int getId() {
            return userVertex.getId();
        }

        private void setId(int id) {
            userVertex.setId(id);
        }

        private int getDegree() {
            return neighbours.size();
        }

        private Iterable<Edge> getIncidentEdges() {
            return incidentEdges;
        }

        private Iterable<Vertex> getNeighbours() {
            return neighbours;
        }

        private Edge getIncidentEdge(int index) {
            return incidentEdges.get(index);
        }

        private Vertex getNeighbour(int index) {
            return neighbours.get(index);
        }
    }

    /**
     * Inner class to model edges.
     */
    private class Edge {

        private final Vertex source;
        private final Vertex target;
        private final E userEdge;

        private Edge(Vertex source, Vertex target) {
            this.source = source;
            this.target = target;
            userEdge = createEdge();
            userEdge.setId(nextEdgeId());
        }

        private int getId() {
            return userEdge.getId();
        }

        private void setId(int id) {
            userEdge.setId(id);
        }

        private Vertex getSource() {
            return source;
        }

        private Vertex getTarget() {
            return target;
        }

        private E getUserEdge() {
            return userEdge;
        }
    }

    private final class VertexIterator implements Iterator<V> {

        private final Iterator<Vertex> it;

        public VertexIterator(Iterable<Vertex> iterable) {
            it = iterable.iterator();
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public V next() {
            return it.next().getUserVertex();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private final class VertexIterable implements Iterable<V> {

        private final Iterable<Vertex> iterable;

        public VertexIterable(Iterable<Vertex> iterable) {
            this.iterable = iterable;
        }

        @Override
        public Iterator<V> iterator() {
            return new VertexIterator(iterable);
        }
    }

    private final class EdgeIterator implements Iterator<E> {

        private final Iterator<Edge> it;

        public EdgeIterator(Iterable<Edge> iterable) {
            it = iterable.iterator();
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public E next() {
            return it.next().getUserEdge();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private final class EdgeIterable implements Iterable<E> {

        private final Iterable<Edge> iterable;

        public EdgeIterable(Iterable<Edge> iterable) {
            this.iterable = iterable;
        }

        @Override
        public Iterator<E> iterator() {
            return new EdgeIterator(iterable);
        }
    }
}
