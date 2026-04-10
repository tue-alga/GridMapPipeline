package arrange.model.graph;

import java.util.ArrayList;
import arrange.model.util.ElementList;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class SuccessiveShortestPathMininumCostFlow<V extends AbstractVertex & Supply & Capacity, E extends AbstractEdge & Capacity & Weight> {

    public final static int MAX_VALUE = 1000000;
    private final GenericDigraph<V, E> graph;
    private FlowDigraph residual;
    private ElementList<FlowDigraph.Edge> opposite;
    private ArrayList<FlowDigraph.Edge> reversed;
    private ElementList<Integer> flow;
    private ElementList<Integer> imbalance;
    private ElementList<Integer> potential;
    private Status status;

    public SuccessiveShortestPathMininumCostFlow(GenericDigraph<V, E> graph) {
        this.graph = graph;
        initialize();
        execute();
        polishSolution();
    }

    public int getFlow(E e) {
        return flow.get(e);
    }

    public Status getStatus() {
        return status;
    }

    private void initialize() {
        // Initialize residual network
        residual = new FlowDigraph();
        reversed = new ArrayList<>();
        ElementList<FlowDigraph.Vertex> incoming = new ElementList<>(graph.numberOfVertices());
        ElementList<FlowDigraph.Vertex> outgoing = new ElementList<>(graph.numberOfVertices());
        // Create vertices and remove capacities
        for (V v : graph.vertices()) {
            int supply = v.getSupply();
            if (v.getCapacity() < MAX_VALUE) {
                FlowDigraph.Vertex inV = residual.addVertex();
                FlowDigraph.Vertex outV = residual.addVertex();
                incoming.add(inV);
                outgoing.add(outV);
                if (supply < 0) {
                    inV.setSupply(supply);
                    outV.setSupply(0);
                } else {
                    inV.setSupply(0);
                    outV.setSupply(supply);
                }
            } else {
                FlowDigraph.Vertex newV = residual.addVertex();
                incoming.add(newV);
                outgoing.add(newV);
                newV.setSupply(supply);
            }
        }
        // Create residual edges
        for (E e : graph.edges()) {
            FlowDigraph.Edge newE;
            int w = e.getWeight();
            int c = e.getCapacity();
            FlowDigraph.Vertex source = outgoing.get(graph.getSource(e));
            FlowDigraph.Vertex target = incoming.get(graph.getTarget(e));
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
        for (V v : graph.vertices()) {
            FlowDigraph.Vertex inV = incoming.get(v);
            FlowDigraph.Vertex outV = outgoing.get(v);
            if (inV != outV) {
                FlowDigraph.Edge e = residual.addEdge(inV, outV);
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
                e.setWeight(MAX_VALUE / 10 - 1);
                f.setWeight(MAX_VALUE / 10 - 1);
            }
        }
        // Add opposite edges
        opposite = new ElementList<>(residual.numberOfEdges() * 2, null);
        int originalEdges = residual.numberOfEdges();
        for (int i = 0; i < originalEdges; i++) {
            FlowDigraph.Edge e = residual.getEdge(i);
            FlowDigraph.Vertex source = residual.getSource(e);
            FlowDigraph.Vertex target = residual.getTarget(e);
            FlowDigraph.Edge f = residual.addEdge(target, source);
            f.setCapacity(0);
            f.setWeight(-e.getWeight());
            opposite.set(e, f);
            opposite.set(f, e);
        }
        // Initialize arrays
        flow = new ElementList<>(residual.numberOfEdges(), 0);
        potential = new ElementList<>(residual.numberOfVertices(), 0);
        imbalance = new ElementList<>(residual.numberOfVertices());
        for (FlowDigraph.Vertex v : residual.vertices()) {
            imbalance.add(v.getSupply());
        }
    }

    private void execute() {
        FlowDigraph.Vertex ve = getExcessVertex();
        FlowDigraph.Vertex vf = getDeficitVertex();
        OUTER:
        while (ve != null && vf != null) {
            ///////////////////////// REMOVE ////////////////////////////
            for (FlowDigraph.Edge e : residual.edges()) {
                if (e.getCapacity() == 0) {
                    e.setCapacity(e.getWeight());
                    e.setWeight(MAX_VALUE);
                }
            }
            /////////////////////////////////////////////////////////////
            DijkstraShortestPath<FlowDigraph.Vertex, FlowDigraph.Edge> dsp = new DijkstraShortestPath<>(residual);
            dsp.shortestPath(ve);
            ///////////////////////// REMOVE ////////////////////////////
            for (FlowDigraph.Edge e : residual.edges()) {
                if (e.getWeight() == MAX_VALUE) {
                    e.setWeight(e.getCapacity());
                    e.setCapacity(0);
                }
            }
            /////////////////////////////////////////////////////////////
            for (FlowDigraph.Vertex v : residual.vertices()) {
                int d = dsp.getDistance(v);
                if (d >= MAX_VALUE) {
                    break OUTER;
                }
            }
            ArrayList<FlowDigraph.Edge> path = dsp.getShortestPathEdges(vf);
            int imbVe = imbalance.get(ve);
            int imbVf = imbalance.get(vf);
            int delta = Math.min(imbVe, -imbVf);
            for (FlowDigraph.Edge e : path) {
                int c = e.getCapacity();
                if (c < delta) {
                    delta = c;
                }
            }
            if (delta <= 0) {
                throw new RuntimeException();
            }
            imbalance.set(ve, imbVe - delta);
            imbalance.set(vf, imbVf + delta);
            for (FlowDigraph.Edge e : path) {
                flow.set(e, flow.get(e) + delta);
                FlowDigraph.Edge f = opposite.get(e);
                e.setCapacity(e.getCapacity() - delta);
                f.setCapacity(f.getCapacity() + delta);
            }
            for (FlowDigraph.Vertex v : residual.vertices()) {
                int p = potential.get(v);
                int d = dsp.getDistance(v);
                potential.set(v, p - d);
                for (FlowDigraph.Edge e : residual.incomingEdges(v)) {
                    e.setWeight(e.getWeight() - d);
                }
                for (FlowDigraph.Edge e : residual.outgoingEdges(v)) {
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
            FlowDigraph.Edge e = residual.getEdge(i);
            FlowDigraph.Edge f = opposite.get(e);
            if (e.getId() < f.getId()) {
                int flowE = flow.get(e);
                int flowF = flow.get(f);
                flow.set(e, flowE - flowF);
                flow.set(f, 0);
            }
        }
        // Fix reversed edges
        for (FlowDigraph.Edge e : reversed) {
            E original = graph.getEdge(e.getId());
            flow.set(e, original.getCapacity() - flow.get(e));
        }
    }

    private FlowDigraph.Vertex getExcessVertex() {
        for (FlowDigraph.Vertex v : residual.vertices()) {
            if (imbalance.get(v) > 0) {
                return v;
            }
        }
        return null;
    }

    private FlowDigraph.Vertex getDeficitVertex() {
        for (FlowDigraph.Vertex v : residual.vertices()) {
            if (imbalance.get(v) < 0) {
                return v;
            }
        }
        return null;
    }

    public enum Status {

        FEASIBLE, EXCESS, DEFICIT, EXCESS_AND_DEFICIT;
    }
}
