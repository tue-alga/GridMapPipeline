package arrange.algorithms;

import java.util.List;
import arrange.model.HexagonalMap;
import arrange.model.HexagonalMap.BarycentricCoordinate;
import arrange.model.Network;
import arrange.model.subdivision.PlanarSubdivision;
import arrange.model.subdivision.PlanarSubdivisionAlgorithms;
import arrange.model.graph.DepthFirstSearchTraverser;
import arrange.model.graph.PlanarStraightLineGraph;
import arrange.model.util.ElementList;
import arrange.model.util.Vector2D;
import arrange.Utils.Utils;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class GreedyNetworkToHexagonGrid {

    private final Network network;
    private final PlanarSubdivision subdivision;
    private final PlanarStraightLineGraph dualGraph;
    private final HexagonalMap grid;
    private final ElementList<BarycentricCoordinate> coordinates;

    public GreedyNetworkToHexagonGrid(Network network, PlanarSubdivision subdivision, PlanarStraightLineGraph dualGraph) {
        this.network = network;
        this.subdivision = subdivision;
        this.dualGraph = dualGraph;
        this.grid = new HexagonalMap();
        this.coordinates = new ElementList<>(network.numberOfVertices(), null);
    }

    public HexagonalMap computeHexagonGrid() {

        if (!PlanarSubdivisionAlgorithms.isTriangulation(subdivision)
                || dualGraph.numberOfVertices() == 0) {
            return null;
        }

        BarycentricCoordinate origin = new BarycentricCoordinate(0, 0, 0);
        BarycentricCoordinate neighbour1 = origin.plus(new BarycentricCoordinate(0, -1, 0));
        BarycentricCoordinate neighbour2 = origin.plus(new BarycentricCoordinate(1, 0, 0));

        Network.Vertex leftmost = network.getVertex(0);
        for (Network.Vertex v : network.vertices()) {
            if (v.getPosition().getX() < leftmost.getPosition().getX()) {
                leftmost = v;
            }
        }

        PlanarSubdivision.Vertex leftmostSub = toSubdivisionVertex(leftmost);
        List<? extends PlanarSubdivision.Halfedge> outgoing = leftmostSub.getOutgoingHalfedges();
        PlanarSubdivision.Vertex topmostSub = outgoing.get(0).getTarget();
        PlanarSubdivision.Face initialFace = outgoing.get(0).getTwin().getFace();
        for (PlanarSubdivision.Halfedge h : outgoing) {
            Vector2D p1 = toNetworkVertex(h.getTarget()).getPosition();
            Vector2D p2 = leftmost.getPosition();
            Vector2D p3 = toNetworkVertex(topmostSub).getPosition();
            if (Utils.triangleOrientation(p1, p2, p3) == 1) {
                topmostSub = h.getTarget();
                initialFace = h.getTwin().getFace();
            }
        }

        Network.Vertex topmost = toNetworkVertex(topmostSub);
        Network.Vertex nextTopmost = null;
        for (PlanarSubdivision.Vertex faceVertex : initialFace.getBoundaryVertices()) {
            if (faceVertex != leftmostSub && faceVertex != topmostSub) {
                nextTopmost = toNetworkVertex(faceVertex);
                break;
            }
        }
        grid.setVertex(origin, leftmost);
        grid.setVertex(neighbour1, topmost);
        grid.setVertex(neighbour2, nextTopmost);
        coordinates.set(leftmost, origin);
        coordinates.set(topmost, neighbour1);
        coordinates.set(nextTopmost, neighbour2);

        class LocalVisitor extends DepthFirstSearchTraverser.Visitor<PlanarStraightLineGraph.Vertex, PlanarStraightLineGraph.Edge> {

            @Override
            public void discoverVertex(DepthFirstSearchTraverser<PlanarStraightLineGraph.Vertex, PlanarStraightLineGraph.Edge> traverser, PlanarStraightLineGraph.Vertex v) {
                PlanarStraightLineGraph.Vertex currentVertex = (PlanarStraightLineGraph.Vertex) v;
                PlanarStraightLineGraph.Vertex predecessor = (PlanarStraightLineGraph.Vertex) traverser.getPredecessor(v);
                if (predecessor != null) {
                    PlanarSubdivision.Face currentFace = toSubdivisionFace(currentVertex);
                    PlanarSubdivision.Face predecessorFace = toSubdivisionFace(predecessor);
                    Network.Vertex nextToAdd = null;
                    Network.Vertex neighbour1 = null;
                    Network.Vertex neighbour2 = null;
                    for (PlanarSubdivision.Halfedge h : currentFace.getBoundaryHalfedges()) {
                        if (h.getTwin().getFace() == predecessorFace) {
                            nextToAdd = toNetworkVertex(h.getNext().getTarget());
                            neighbour1 = toNetworkVertex(h.getSource());
                            neighbour2 = toNetworkVertex(h.getTarget());
                            break;
                        }
                    }
                    if (nextToAdd != null) {
                        BarycentricCoordinate n1 = coordinates.get(neighbour1);
                        BarycentricCoordinate n2 = coordinates.get(neighbour2);
                        BarycentricCoordinate free = getFreeCommonNeighbour(n1, n2);
                        if (free != null) {
                            grid.setVertex(free, nextToAdd);
                            coordinates.set(nextToAdd, free);
                        } else {
                            System.out.println("FAILED!!!");
                        }
                    }
                }
            }
        }
        LocalVisitor localVisitor = new LocalVisitor();
        DepthFirstSearchTraverser<PlanarStraightLineGraph.Vertex, PlanarStraightLineGraph.Edge> traverser = new DepthFirstSearchTraverser<>(dualGraph, localVisitor);

        traverser.traverse(toDualGraphVertex(initialFace));
        return grid;
    }

    private BarycentricCoordinate getFreeCommonNeighbour(BarycentricCoordinate c1, BarycentricCoordinate c2) {
        BarycentricCoordinate difference = c1.minus(c2);
        int dx = difference.getX();
        int dy = difference.getY();
        int dz = difference.getZ();
        BarycentricCoordinate rotateLeft = new BarycentricCoordinate(dy, dz, dx);
        BarycentricCoordinate rotateRight = new BarycentricCoordinate(dz, dx, dy);
        BarycentricCoordinate previous = rotateLeft.plus(c1);
        BarycentricCoordinate next = rotateRight.plus(c1);
        if (grid.getVertex(previous) == null) {
            return previous;
        } else if (grid.getVertex(next) == null) {
            return next;
        } else {
            System.out.println("Error, no free hexagon available!!");
            return null;
        }
    }

    private Network.Vertex toNetworkVertex(PlanarSubdivision.Vertex v) {
        return network.getVertex(v.getId());
    }

    private PlanarSubdivision.Vertex toSubdivisionVertex(Network.Vertex v) {
        return subdivision.getVertex(v.getId());
    }

    private PlanarSubdivision.Face toSubdivisionFace(PlanarStraightLineGraph.Vertex v) {
        return subdivision.getFace(v.getId());
    }

    private PlanarStraightLineGraph.Vertex toDualGraphVertex(PlanarSubdivision.Face f) {
        return dualGraph.getVertex(f.getId());
    }
}
