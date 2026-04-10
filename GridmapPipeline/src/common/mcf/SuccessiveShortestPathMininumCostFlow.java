package common.mcf;

import common.mcf.FlowDigraph.Edge;
import common.mcf.FlowDigraph.Vertex;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import nl.tue.geometrycore.datastructures.priorityqueue.IndexedPriorityQueue;

/**
 *
 * @author Rafael Cano
 */
public class SuccessiveShortestPathMininumCostFlow {

    private final FlowDigraph graph;
    private FlowDigraph residual;
    private Edge[] opposite;
    private ArrayList<Edge> reversed;
    int[] flow; // NB: just one array here...
    private int[] imbalance;
    private Status status;

    SuccessiveShortestPathMininumCostFlow(FlowDigraph graph) {
        this.graph = graph;
        initialize();
        execute();
        polishSolution();
    }

    public Status getStatus() {
        return status;
    }

    private void initialize() {
        // Initialize residual network
        residual = new FlowDigraph();
        reversed = new ArrayList<>();
        Vertex[] incoming = new Vertex[graph.numberOfVertices()];
        Vertex[] outgoing = new Vertex[graph.numberOfVertices()];
        // Create vertices and remove capacities
        for (Vertex v : graph.vertices()) {
            int supply = v.getSupply();
            if (v.getCapacity() < FlowDigraph.MAX_VALUE) {
                Vertex inV = residual.addVertex();
                Vertex outV = residual.addVertex();
                incoming[v.id] = inV;
                outgoing[v.id] = outV;
                if (supply < 0) {
                    inV.setSupply(supply);
                    outV.setSupply(0);
                } else {
                    inV.setSupply(0);
                    outV.setSupply(supply);
                }
            } else {
                Vertex newV = residual.addVertex();
                incoming[v.id] = newV;
                outgoing[v.id] = newV;
                newV.setSupply(supply);
            }
        }
        // Create residual edges
        for (Edge e : graph.edges()) {
            Edge newE;
            double w = e.getWeight();
            int c = e.getCapacity();
            Vertex source = outgoing[e.getSource().id];
            Vertex target = incoming[e.getTarget().id];
            if (w >= 0) {
                newE = residual.addEdge(source, target);
                newE.setCapacity(c);
                newE.setWeight(w);
            } else {
                newE = residual.addEdge(target, source);
                newE.setCapacity(c);
                newE.setWeight(-w);
                source.setSupply(source.getSupply() - c);
                target.setSupply(target.getSupply() + c);
                reversed.add(newE);
            }
        }
        // Create edges between split vertices
        for (Vertex v : graph.vertices()) {
            Vertex inV = incoming[v.id];
            Vertex outV = outgoing[v.id];
            if (inV != outV) {
                Edge e = residual.addEdge(inV, outV);
                e.setWeight(0);
                e.setCapacity(v.getCapacity());
            }
        }
        // Add artificial vertex to guarantee connectivity
        FlowDigraph.Vertex artificial = residual.addVertex();
        for (FlowDigraph.Vertex v : residual.vertices()) {
            if (v != artificial) {
                FlowDigraph.Edge e = residual.addEdge(artificial, v);
                FlowDigraph.Edge f = residual.addEdge(v, artificial);
                e.setWeight(FlowDigraph.MAX_VALUE / 10 - 1);
                f.setWeight(FlowDigraph.MAX_VALUE / 10 - 1);
            }
        }
        // Add opposite edges
        opposite = new Edge[residual.numberOfEdges() * 2];
        int originalEdges = residual.numberOfEdges();
        for (int i = 0; i < originalEdges; i++) {
            Edge e = residual.getEdge(i);
            Vertex source = e.getSource();
            Vertex target = e.getTarget();
            Edge f = residual.addEdge(target, source);
            f.setCapacity(0);
            f.setWeight(-e.getWeight());
            opposite[e.id] = f;
            opposite[f.id] = e;
        }
        // Initialize arrays
        flow = new int[residual.numberOfEdges()];
        Arrays.fill(flow, 0);
        imbalance = new int[residual.numberOfEdges()];
        for (Vertex v : residual.vertices()) {
            imbalance[v.id] = v.getSupply();
        }
    }

    private void execute() {
        Vertex ve = getExcessVertex();
        Vertex vf = getDeficitVertex();
        DijkstraShortestPath dsp = new DijkstraShortestPath();

        while (ve != null && vf != null) {

            dsp.shortestPath(ve);

            if (Double.isInfinite(dsp.distances[vf.id])) {
                break;
            }

            ArrayList<Edge> path = dsp.getShortestPathEdges(vf);
            int imbVe = imbalance[ve.id];
            int imbVf = imbalance[vf.id];
            int delta = Math.min(imbVe, -imbVf);
            for (Edge e : path) {
                int c = e.getCapacity();
                if (c < delta) {
                    delta = c;
                }
            }
            if (delta <= 0) {
                throw new RuntimeException();
            }
            imbalance[ve.id] -= delta;
            imbalance[vf.id] += delta;
            for (Edge e : path) {
                flow[e.id] += delta;
                Edge f = opposite[e.id];
                e.setCapacity(e.getCapacity() - delta);
                f.setCapacity(f.getCapacity() + delta);
            }
            for (Vertex v : residual.vertices()) {
                double d = dsp.distances[v.id];
                if (Double.isInfinite((d))) {
                    continue;
                }
                for (Edge e : v.getIncomingEdges()) {
                    e.setWeight(e.getWeight() - d);
                }
                for (Edge e : v.getOutgoingEdges()) {
                    e.setWeight(e.getWeight() + d);
                }
            }

            ve = getExcessVertex();
            vf = getDeficitVertex();
        }

        if (ve == null && vf == null) {
            status = Status.FEASIBLE;
        } else if (ve == null && vf != null) {
            status = Status.DEFICIT;
        } else if (ve != null && vf == null) {
            status = Status.EXCESS;
        } else {
            status = Status.EXCESS_AND_DEFICIT;
        }
    }

    private void polishSolution() {
        // Remove overlaps
        for (int i = 0; i < residual.numberOfEdges(); i++) {
            Edge e = residual.getEdge(i);
            Edge f = opposite[e.id];
            if (e.id < f.id) {
                int flowE = flow[e.id];
                int flowF = flow[f.id];
                flow[e.id] = flowE - flowF;
                flow[f.id] = 0;
            }
        }
        // Fix reversed edges
        for (Edge e : reversed) {
            Edge original = graph.getEdge(e.id);
            flow[e.id] = original.getCapacity() - flow[e.id];
        }
    }

    private Vertex getExcessVertex() {
        for (Vertex v : residual.vertices()) {
            if (imbalance[v.id] > 0) {
                return v;
            }
        }
        return null;
    }

    private Vertex getDeficitVertex() {
        for (Vertex v : residual.vertices()) {
            if (imbalance[v.id] < 0) {
                return v;
            }
        }
        return null;
    }

    public enum Status {

        FEASIBLE, EXCESS, DEFICIT, EXCESS_AND_DEFICIT;
    }

    private class DijkstraShortestPath {

        private Vertex[] parentVertex;
        private Edge[] parentEdge;
        private double[] distances;

        public double getDistance(Vertex v) {
            return distances[v.id];
        }

        public void shortestPath(Vertex s) {

            final int n = residual.numberOfVertices();
            parentVertex = new Vertex[n];
            parentEdge = new Edge[n];
            distances = new double[n];
            Arrays.fill(distances, Double.POSITIVE_INFINITY);
            distances[s.id] = 0;

            IndexedPriorityQueue<Vertex> queue = new IndexedPriorityQueue<>(12, (v1, v2) -> {
                if (v1 == v2) {
                    return 0;
                }
                int c = Double.compare(distances[v1.id], distances[v2.id]);
                if (c != 0) {
                    return c;
                }
                return Integer.compare(v1.id, v2.id);
            });
            queue.add(s);

            while (!queue.isEmpty()) {
                Vertex u = queue.poll();
                double distU = distances[u.id];

                for (Edge e : u.getOutgoingEdges()) {
                    Vertex v = e.getTarget();

                    if (e.getCapacity() == 0) {
                        continue;
                    }

                    double w = e.getWeight();
                    double distV = distances[v.id];
                    if (distV > distU + w) {
                        parentVertex[v.id] = u;
                        parentEdge[v.id] = e;
                        distances[v.id] = distU + w;
                         // hope for no negative weight cycles... this is how the old searcher in the mosaic code behaves. Makes it inefficient though
                        if (v.getIndex() < 0) {
                            queue.add(v);
                        } else {
                            queue.priorityIncreased(v);
                        }
                    } 
                }
            }
        }

        public ArrayList<Edge> getShortestPathEdges(Vertex v) {
            if (v == null) {
                return null;
            } else {
                ArrayList<Edge> path = new ArrayList<>(residual.numberOfVertices());
                Edge e = parentEdge[v.id];
                while (e != null) {
                    path.add(e);
                    v = parentVertex[v.id];
                    e = parentEdge[v.id];
                }
                Collections.reverse(path);
                return path;
            }
        }
    }
}
