package arrange.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import arrange.Utils.Utils;
import arrange.model.subdivision.PlanarSubdivision;
import arrange.model.subdivision.PlanarSubdivisionAlgorithms;
import arrange.model.util.CircularListIterator;
import arrange.model.util.ElementList;
import arrange.model.util.IpeExporter;

/**
 * Given a triangular graph as input, computes a Schnyder wood. Quadratic time
 * implementation.
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class SchnyderWood {

    private final Network graph;
    private final PlanarSubdivision subdivision;
    private final ElementList<PlanarSubdivision.Vertex> parentT1;
    private final ElementList<PlanarSubdivision.Vertex> parentT2;
    private final ElementList<PlanarSubdivision.Vertex> parentT3;
    private final Network.Vertex rootT1;
    private final Network.Vertex rootT2;
    private final Network.Vertex rootT3;

    public SchnyderWood(Network graph, PlanarSubdivision subdivision) {
        this.graph = new Network(graph);
        this.subdivision = subdivision;
        this.parentT1 = new ElementList<>(graph.numberOfVertices(), null);
        this.parentT2 = new ElementList<>(graph.numberOfVertices(), null);
        this.parentT3 = new ElementList<>(graph.numberOfVertices(), null);
        if (!PlanarSubdivisionAlgorithms.isTriangulation(subdivision)) {
            throw new RuntimeException("input graph must be a triangulation");
        }
        PlanarSubdivision.Vertex sRootT1 = Utils.topmost(subdivision.vertices());
        PlanarSubdivision.Face unbounded = subdivision.getUnboundedFace();
        if (unbounded.numberOfHoles() != 1) {
            throw new RuntimeException("unbounded face should have exactly one hole");
        }
        List<? extends PlanarSubdivision.Vertex> externalVertices = unbounded.getHoles().get(0);
        CircularListIterator<? extends PlanarSubdivision.Vertex> cit = new CircularListIterator<>(externalVertices);
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
            ArrayList<Network.Vertex> neighboursX = null;
            Set<Network.Vertex> commonNeighboursX = null;
            for (int i = 0; i < graph.getDegree(rootT1); i++) {
                Network.Vertex candidate = graph.getNeighbour(rootT1, i);
                if (candidate != rootT2 && candidate != rootT3) {
                    Set<Network.Vertex> commonNeighbours = commonNeighbours(rootT1, candidate);
                    if (commonNeighbours.size() == 2) {
                        x = candidate;
                        neighboursX = new ArrayList<>(graph.getDegree(x));
                        for (Network.Vertex v : graph.neighbours(x)) {
                            neighboursX.add(v);
                        }
                        commonNeighboursX = commonNeighbours;
                        break;
                    }
                }
            }
            if (x == null) {
                IpeExporter.exportGraph(graph, "contracted.ipe");
                throw new RuntimeException("no contractible edge found");
            }
            // Erase edges from "deleted" vertex (not really deleted, only edges are erased)
            while (graph.getDegree(x) > 0) {
                Network.Edge e = graph.getIncidentEdge(x, 0);
                Network.Vertex v = graph.getNeighbour(x, 0);
                graph.removeEdge(e);
                if (v != rootT1 && !commonNeighboursX.contains(v)) {
                    graph.addEdge(rootT1, v);
                }
            }
            execute(numActive - 1);
            for (Network.Vertex v : neighboursX) {
                if (v != rootT1 && !commonNeighboursX.contains(v)) {
                    parentT1.set(v, subdivision.getVertex(x.getId()));
                }
            }
            parentT1.set(x, subdivision.getVertex(rootT1.getId()));
        }
    }

    private Set<Network.Vertex> commonNeighbours(Network.Vertex u, Network.Vertex v) {
        LinkedHashSet<Network.Vertex> su = new LinkedHashSet<>();
        LinkedHashSet<Network.Vertex> sv = new LinkedHashSet<>();
        for (Network.Vertex neighbour : graph.neighbours(u)) {
            su.add(neighbour);
        }
        for (Network.Vertex neighbour : graph.neighbours(v)) {
            sv.add(neighbour);
        }
        su.retainAll(sv);
        return su;
    }
}
