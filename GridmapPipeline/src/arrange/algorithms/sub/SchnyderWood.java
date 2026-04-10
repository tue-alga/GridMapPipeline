package arrange.algorithms.sub;

import arrange.algorithms.graph.Network;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import arrange.algorithms.graph.PlanarSubdivision;
import arrange.util.CircularListIterator;
import arrange.util.ElementList;

/**
 * Given a triangular graph as input, computes a Schnyder wood. Quadratic time
 * implementation.
 *
 * @author Rafael Cano
 */
public class SchnyderWood {

    private final Network graph;
    private final PlanarSubdivision subdivision;
    private final ElementList<PlanarSubdivision.Vertex> parentT1;
    private final Network.Vertex rootT1;
    private final Network.Vertex rootT2;
    private final Network.Vertex rootT3;

    public SchnyderWood(Network graph, PlanarSubdivision subdivision) {
        this.graph = new Network(graph);
        this.subdivision = subdivision;
        this.parentT1 = new ElementList<>(graph.numberOfVertices(), null);
        if (!subdivision.isTriangulation()) {
            throw new RuntimeException("input graph must be a triangulation");
        }
        PlanarSubdivision.Vertex sRootT1 = null;
        // find topmost
        for (PlanarSubdivision.Vertex v : subdivision.vertices()) {
            if (sRootT1 == null || v.getPosition().getY() > sRootT1.getPosition().getY()) {
                sRootT1 = v;
            }
        }
        PlanarSubdivision.Face unbounded = subdivision.getUnboundedFace();
        if (unbounded.numberOfHoles() != 1) {
            throw new RuntimeException("unbounded face should have exactly one hole");
        }
        List<PlanarSubdivision.Vertex> externalVertices = unbounded.getHoles().get(0);
        CircularListIterator<PlanarSubdivision.Vertex> cit = new CircularListIterator<>(externalVertices);
        while (cit.previous() != sRootT1);
        PlanarSubdivision.Vertex sRootT2 = cit.previous();
        PlanarSubdivision.Vertex sRootT3 = cit.previous();
        rootT1 = this.graph.getVertex(sRootT1.getId());
        rootT2 = this.graph.getVertex(sRootT2.getId());
        rootT3 = this.graph.getVertex(sRootT3.getId());
        execute(graph.numberOfVertices());
        parentT1.set(rootT2, subdivision.getVertex(rootT1.getId()));
        parentT1.set(rootT3, subdivision.getVertex(rootT1.getId()));
    }

    public ElementList<PlanarSubdivision.Vertex> getParents() {
        return parentT1;
    }

    private void execute(int numActive) {
        if (numActive > 3) {
            // Find contractible edge
            Network.Vertex x = null;
            ArrayList<Network.Vertex> neighborsX = null;
            Set<Network.Vertex> commonNeighborsX = null;
            for (Network.Vertex candidate : rootT1.getNeighbors()) {
                if (candidate != rootT2 && candidate != rootT3) {
                    Set<Network.Vertex> commonNeighbors = commonNeighbors(rootT1, candidate);
                    if (commonNeighbors.size() == 2) {
                        x = candidate;
                        neighborsX = new ArrayList<>(x.getDegree());
                        for (Network.Vertex v : x.getNeighbors()) {
                            neighborsX.add(v);
                        }
                        commonNeighborsX = commonNeighbors;
                        break;
                    }
                }
            }

            assert x != null : "no contractible edge found";

            // Erase edges from "deleted" vertex (not really deleted, only edges are erased)
            while (x.getDegree() > 0) {
                Network.Edge e = x.getIncidentEdge(0);
                Network.Vertex v = x.getNeighbor(0);
                graph.removeEdge(e);
                if (v != rootT1 && !commonNeighborsX.contains(v)) {
                    graph.addEdge(rootT1, v);
                }
            }
            execute(numActive - 1);
            for (Network.Vertex v : neighborsX) {
                if (v != rootT1 && !commonNeighborsX.contains(v)) {
                    parentT1.set(v, subdivision.getVertex(x.getId()));
                }
            }
            parentT1.set(x, subdivision.getVertex(rootT1.getId()));
        }
    }

    private Set<Network.Vertex> commonNeighbors(Network.Vertex u, Network.Vertex v) {
        LinkedHashSet<Network.Vertex> su = new LinkedHashSet<>();
        LinkedHashSet<Network.Vertex> sv = new LinkedHashSet<>();
        for (Network.Vertex neighbor : u.getNeighbors()) {
            su.add(neighbor);
        }
        for (Network.Vertex neighbor : v.getNeighbors()) {
            sv.add(neighbor);
        }
        su.retainAll(sv);
        return su;
    }
}
