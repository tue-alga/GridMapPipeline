package arrange.model.graph;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public abstract class AbstractGraph<V extends AbstractVertex, E extends AbstractEdge> {

    /**
     * Adds a vertex to the graph. Vertex ids assigned previously remain valid.
     */
    public abstract V addVertex();

    /**
     * Adds an edge to the graph between the specified vertices. Edge ids
     * assigned previously remain valid.
     */
    public abstract E addEdge(V source, V target);

    /**
     * Adds an edge to the graph between the vertices with the specified ids.
     * Edge ids assigned previously remain valid.
     */
    public abstract E addEdge(int sourceId, int targetId);

    /**
     * Removes a vertex and the edges incident to it. Vertex and edge ids may
     * change after this operation.
     */
    public abstract void removeVertex(V v);

    /**
     * Removes an edge from the graph. Edge ids may change after this operation.
     */
    public abstract void removeEdge(E e);

    /**
     * Number of vertices.
     */
    public abstract int numberOfVertices();

    /**
     * Number of edges.
     */
    public abstract int numberOfEdges();

    /**
     * Iterable to an unmodifiable list of vertices in which the position of
     * each vertex corresponds to its id.
     */
    public abstract Iterable<V> vertices();

    /**
     * Iterable to an unmodifiable list of edges in which the position of each
     * edge corresponds to its id.
     */
    public abstract Iterable<E> edges();

    /**
     * Returns the vertex with the corresponding id.
     */
    public abstract V getVertex(int id);

    /**
     * Returns the edge with the corresponding id.
     */
    public abstract E getEdge(int id);

    /**
     * Returns the edge between the specified vertices, or null if it does not
     * exist.
     */
    public abstract E getEdge(V source, V target);

    /**
     * Degree of a vertex.
     */
    public abstract int getDegree(V v);

    /**
     * Indegree of a vertex.
     */
    public abstract int getIndegree(V v);

    /**
     * Outdegree of a vertex.
     */
    public abstract int getOutdegree(V v);

    /**
     * Incident edges.
     */
    public abstract Iterable<E> incidentEdges(V v);

    /**
     * Retrieve the incident edge at the specified index.
     */
    public abstract E getIncidentEdge(V v, int index);

    /**
     * Incoming edges.
     */
    public abstract Iterable<E> incomingEdges(V v);

    /**
     * Retrieve the incoming edge at the specified index.
     */
    public abstract E getIncomingEdge(V v, int index);

    /**
     * Outgoing edges.
     */
    public abstract Iterable<E> outgoingEdges(V v);

    /**
     * Retrieve the outgoing edge at the specified index.
     */
    public abstract E getOutgoingEdge(V v, int index);

    /**
     * Neighbors of the specified vertex.
     */
    public abstract Iterable<V> neighbours(V v);

    /**
     * Retrieve the neighbor at the specified index.
     */
    public abstract V getNeighbour(V v, int index);

    /**
     * In-neighbors of the specified vertex.
     */
    public abstract Iterable<V> inneighbours(V v);

    /**
     * Retrieve the in-neighbor at the specified index.
     */
    public abstract V getInneighbour(V v, int index);

    /**
     * Out-neighbors of the specified vertex.
     */
    public abstract Iterable<V> outneighbours(V v);

    /**
     * Retrieve the out-neighbor at the specified index.
     */
    public abstract V getOutneighbour(V v, int index);

    /**
     * Returns the source of the specified edge.
     */
    public abstract V getSource(E e);

    /**
     * Returns the target of the specified edge.
     */
    public abstract V getTarget(E e);

    /**
     * Removes all vertices and edges from the graph.
     */
    public abstract void clear();

    /**
     * Creates a new vertex. This method should be overridden by subclasses.
     */
    protected abstract V createVertex();

    /**
     * Creates a new edge. This method should be overridden by subclasses.
     */
    protected abstract E createEdge();
}
