package common.mcf;

import common.mcf.FlowDigraph.Edge;
import common.mcf.FlowDigraph.Vertex;
import java.util.ArrayList;
import nl.tue.geometrycore.datastructures.priorityqueue.BasicIndexable;

/**
 * Digraph aimed at flow problems. Edges have capacities and weights, vertices
 * have a supply. A positive vertex supply means that it is actually supplying
 * flow to the network. A negative supply represents the opposite, i.e., a
 * demand.
 *
 * @author Rafael Cano, Wouter Meulemans
 */
public class FlowDigraph {

    public final static int MAX_VALUE = 1000000;

    private final ArrayList<Vertex> vertices;
    private final ArrayList<Edge> edges;

    /**
     * Creates an empty FlowDigraph.
     */
    public FlowDigraph() {
        vertices = new ArrayList<>();
        edges = new ArrayList<>();
    }

    public class Vertex extends BasicIndexable {

        private int supply = 0;
        private int capacity = MAX_VALUE;

        private final ArrayList<Edge> incomingEdges = new ArrayList<>();
        private final ArrayList<Edge> outgoingEdges = new ArrayList<>();

        final int id;

        public final int getId() {
            return id;
        }

        protected Vertex() {
            id = vertices.size();
        }

        public int getSupply() {
            return supply;
        }

        public void setSupply(int supply) {
            this.supply = supply;
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        private void addIncomingEdge(Edge e) {
            incomingEdges.add(e);
        }

        private void addOutgoingEdge(Edge e) {
            outgoingEdges.add(e);
        }

        public int getIndegree() {
            return incomingEdges.size();
        }

        public int getOutdegree() {
            return outgoingEdges.size();
        }

        public Iterable<Edge> getIncomingEdges() {
            return incomingEdges;
        }

        public Iterable<Edge> getOutgoingEdges() {
            return outgoingEdges;
        }

        public Edge getIncomingEdge(int index) {
            return incomingEdges.get(index);
        }

        public Edge getOutgoingEdge(int index) {
            return outgoingEdges.get(index);
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

    public class Edge {

        private final Vertex source;
        private final Vertex target;
        private int capacity = MAX_VALUE;
        private double weight = 0;

        final int id;

        public final int getId() {
            return id;
        }

        protected Edge(Vertex source, Vertex target) {
            this.source = source;
            this.target = target;
            id = edges.size();
        }

        public Vertex getSource() {
            return source;
        }

        public Vertex getTarget() {
            return target;
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
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

    public Vertex addVertex() {
        Vertex v = new Vertex();
        vertices.add(v);
        return v;
    }

    public Edge addEdge(Vertex source, Vertex target) {
        Edge e = new Edge(source, target);
        source.addOutgoingEdge(e);
        target.addIncomingEdge(e);
        edges.add(e);
        return e;
    }

    public Edge addEdge(int sourceId, int targetId) {
        return addEdge(vertices.get(sourceId), vertices.get(targetId));
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

    public int[] solve() {
        SuccessiveShortestPathMininumCostFlow mcf = new SuccessiveShortestPathMininumCostFlow(this);
        if (mcf.getStatus() != SuccessiveShortestPathMininumCostFlow.Status.FEASIBLE) {
            System.err.println("Warning: infeasible flow model");
            return null;
        } else {
            return mcf.flow;
        }
    }
}
