package arrange.model.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import arrange.model.util.ElementList;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class DijkstraShortestPath<V extends AbstractVertex, E extends AbstractEdge> {

    public static final int MAX_VALUE = 1000000;
    private final AbstractGraph<V, E> g;
    private ElementList<V> parentVertex;
    private ElementList<E> parentEdge;
    private ElementList<Integer> distances;

    public DijkstraShortestPath(AbstractGraph<V, E> g) {
        this.g = g;
        checkGraph();
    }

    /**
     * Computes the shortest path from s to all other vertices.
     */
    public void shortestPath(V s) {
        parentVertex = new ElementList<>(g.numberOfVertices(), null);
        parentEdge = new ElementList<>(g.numberOfVertices(), null);
        distances = new ElementList<>(g.numberOfVertices(), MAX_VALUE);
        distances.set(s, 0);
        Interrupter<V> interrupter = new Interrupter<V>() {
            @Override
            public boolean objectiveAccomplished(V v) {
                return false;
            }
        };
        runAlgorithm(interrupter);
    }

    /**
     * Distance to the source computed with the last call to the algorithm on
     * this instance. The distance will be MAX_VALUE if the vertex cannot be
     * reached or if the search was interrupted before it was reached.
     */
    public int getDistance(V v) {
        return distances.get(v);
    }

    /**
     * Returns the edges on the shortest path between v and the source in the
     * order in which they appear in the path.
     */
    public ArrayList<E> getShortestPathEdges(V v) {
        return reconstructEdgePath(v);
    }

    /**
     * Returns the shortest path connecting any vertex from s1 to any vertex in
     * s2.
     */
    public static <V2 extends AbstractVertex, E2 extends AbstractEdge> ArrayList<V2> shortestPath(AbstractGraph<V2, E2> g, final Set<V2> s1, final Set<V2> s2) {
        DijkstraShortestPath<V2, E2> dsp = new DijkstraShortestPath<>(g);
        return dsp.shortestPath(s1, s2);
    }

    private ArrayList<V> shortestPath(final Set<V> s1, final Set<V> s2) {
        parentVertex = new ElementList<>(g.numberOfVertices(), null);
        parentEdge = new ElementList<>(g.numberOfVertices(), null);
        distances = new ElementList<>(g.numberOfVertices(), MAX_VALUE);
        for (V v : s1) {
            distances.set(v, 0);
        }
        Interrupter<V> interrupter = new Interrupter<V>() {
            @Override
            public boolean objectiveAccomplished(V v) {
                return s2.contains(v);
            }
        };
        V last = runAlgorithm(interrupter);
        return reconstructVertexPath(last);
    }

    /**
     * Returns the shorted path from u to v.
     */
    public static <V2 extends AbstractVertex, E2 extends AbstractEdge> ArrayList<V2> shortestPath(AbstractGraph<V2, E2> g, V2 s, V2 t) {
        DijkstraShortestPath<V2, E2> dsp = new DijkstraShortestPath<>(g);
        return dsp.shortestPath(s, t);
    }

    private ArrayList<V> shortestPath(final V s, final V t) {
        parentVertex = new ElementList<>(g.numberOfVertices(), null);
        parentEdge = new ElementList<>(g.numberOfVertices(), null);
        distances = new ElementList<>(g.numberOfVertices(), MAX_VALUE);
        distances.set(s, 0);
        Interrupter<V> interrupter = new Interrupter<V>() {
            @Override
            public boolean objectiveAccomplished(V vprime) {
                return vprime == t;
            }
        };
        V last = runAlgorithm(interrupter);
        return reconstructVertexPath(last);
    }

    private V runAlgorithm(Interrupter<V> interrupter) {
        TreeSet<V> queue = new TreeSet<>(new VertexComparator(distances));
        for (V v : g.vertices()) {
            queue.add(v);
        }

        V last = null;
        while (!queue.isEmpty()) {
            V u = queue.pollFirst();
            int distU = distances.get(u);
            if (distU == MAX_VALUE) {
                return null;
            }
            if (interrupter.objectiveAccomplished(u)) {
                last = u;
                break;
            } else {
                for (int i = 0; i < g.getOutdegree(u); i++) {
                    V v = g.getOutneighbour(u, i);
                    E e = g.getOutgoingEdge(u, i);
                    int w = weight(e);
                    int distV = distances.get(v);
                    if (distV > distU + w) {
                        parentVertex.set(v, u);
                        parentEdge.set(v, e);
                        queue.remove(v);
                        distances.set(v, distU + w);
                        queue.add(v);
                    }
                }
            }
        }
        return last;
    }

    private int weight(E e) {
        if (e instanceof Weight) {
            return ((Weight) e).getWeight();
        } else {
            return 1;
        }
    }

    private ArrayList<V> reconstructVertexPath(V v) {
        if (v == null) {
            return null;
        } else {
            ArrayList<V> path = new ArrayList<>(g.numberOfVertices());
            do {
                path.add(v);
                v = parentVertex.get(v);
            } while (v != null);
            Collections.reverse(path);
            return path;
        }
    }

    private ArrayList<E> reconstructEdgePath(V v) {
        if (v == null) {
            return null;
        } else {
            ArrayList<E> path = new ArrayList<>(g.numberOfVertices());
            E e = parentEdge.get(v);
            while (e != null) {
                path.add(e);
                v = parentVertex.get(v);
                e = parentEdge.get(v);
            }
            Collections.reverse(path);
            return path;
        }
    }

    private void checkGraph() {
        for (E e : g.edges()) {
            if (weight(e) < 0) {
                throw new RuntimeException("negative weights");
            }
        }
    }

    private class VertexComparator implements Comparator<V> {

        private final ElementList<Integer> distances;

        public VertexComparator(ElementList<Integer> distances) {
            this.distances = distances;
        }

        @Override
        public int compare(V v1, V v2) {
            if (v1 == v2) {
                return 0;
            }
            int compare = distances.get(v1).compareTo(distances.get(v2));
            if (compare == 0) {
                return Integer.compare(v1.getId(), v2.getId());
            } else {
                return compare;
            }
        }
    }

    private interface Interrupter<V> {

        public boolean objectiveAccomplished(V v);
    }
}
