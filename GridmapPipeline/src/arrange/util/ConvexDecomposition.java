package arrange.util;

import arrange.algorithms.graph.PlanarSubdivision;
import java.util.ArrayList;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.util.Pair;

/**
 * Given the vertices of a simple polygon in counterclockwise order, returns a
 * decomposition into convex polygons. No guarantees are given with respect to
 * the number of polygons returned, i.e., it might be way more than the minimum
 * needed. In the worst case, returns a triangulation using quadratic time.
 *
 * @author Rafael Cano
 */
public final class ConvexDecomposition {

    private final ArrayList<ArrayList<PlanarSubdivision.Vertex>> polygons = new ArrayList<>();
    private final ArrayList<Pair<PlanarSubdivision.Vertex, PlanarSubdivision.Vertex>> edges = new ArrayList<>();

    public ConvexDecomposition(List<PlanarSubdivision.Vertex> vertices) {
        execute(vertices);
    }

    public int numberOfPolygons() {
        return polygons.size();
    }

    public int numberOfEdges() {
        return edges.size();
    }

    public Iterable<ArrayList<PlanarSubdivision.Vertex>> polygons() {
        return polygons;
    }

    public Iterable<Pair<PlanarSubdivision.Vertex, PlanarSubdivision.Vertex>> edges() {
        return edges;
    }

    public ArrayList<PlanarSubdivision.Vertex> getPolygon(int index) {
        return polygons.get(index);
    }

    public Pair<PlanarSubdivision.Vertex, PlanarSubdivision.Vertex> getEdge(int index) {
        return edges.get(index);
    }

    private void execute(List<PlanarSubdivision.Vertex> vertices) {
        if (vertices.size() < 3) {
            throw new IllegalArgumentException("input polygon must have at least three vertices");
        } else if (vertices.size() == 3) {
            polygons.add(new ArrayList<>(vertices));
        } else {
            // Find reflex vertex
            PlanarSubdivision.Vertex reflex = null;
            CircularListIterator<PlanarSubdivision.Vertex> cit = new CircularListIterator<>(vertices);
            PlanarSubdivision.Vertex previous = cit.previous();
            cit.next();
            final PlanarSubdivision.Vertex first = cit.next();
            PlanarSubdivision.Vertex current = first;
            PlanarSubdivision.Vertex next = cit.next();
            do {
                int sign = Utils.triangleOrientation(previous.getPosition(), current.getPosition(), next.getPosition());
                if (sign < 0) {
                    reflex = current;
                    break;
                }
                previous = current;
                current = next;
                next = cit.next();
            } while (current != first);
            if (reflex == null) {
                // Polygon is convex
                polygons.add(new ArrayList<>(vertices));
            } else {
                // Connect the reflex vertex to some other vertex
                PlanarSubdivision.Vertex closestCandidate = null;
                PlanarSubdivision.Vertex reflexNext = next;
                PlanarSubdivision.Vertex reflexPrevious = previous;
                double minDistance = Double.POSITIVE_INFINITY;
                for (PlanarSubdivision.Vertex candidate : vertices) {
                    if (candidate != reflex && candidate != reflexNext && candidate != reflexPrevious) {
                        boolean isVisible = true;
                        cit = new CircularListIterator<>(vertices);
                        current = cit.next();
                        next = cit.next();
                        do {
                            if (current != reflex && current != candidate
                                    && next != reflex && next != candidate) {
                                Vector intersection = Utils.lineSegmentIntersection(reflex.getPosition(),
                                        candidate.getPosition(), current.getPosition(), next.getPosition());
                                if (intersection != null) {
                                    isVisible = false;
                                    break;
                                }
                            }
                            current = next;
                            next = cit.next();
                        } while (current != first);
                        if (isVisible) {
                            // Test if the line segment is contained in the polygon
                            int s1 = Utils.triangleOrientation(reflex, reflexNext, candidate);
                            int s2 = Utils.triangleOrientation(reflex, candidate, reflexPrevious);
                            if (s1 > 0 || s2 > 0) {
                                Vector diff = Vector.subtract(reflex.getPosition(),
                                        candidate.getPosition());
                                double distance = diff.length();
                                if (distance < minDistance) {
                                    minDistance = distance;
                                    closestCandidate = candidate;
                                }
                            }
                        }
                    }
                }
                if (closestCandidate != null) {
                    edges.add(new Pair<>(reflex, closestCandidate));
                    ArrayList<PlanarSubdivision.Vertex> polygon1 = new ArrayList<>();
                    ArrayList<PlanarSubdivision.Vertex> polygon2 = new ArrayList<>();
                    for (PlanarSubdivision.Vertex v : vertices) {
                        polygon1.add(v);
                        if (v == reflex || v == closestCandidate) {
                            ArrayList<PlanarSubdivision.Vertex> aux = polygon1;
                            polygon1 = polygon2;
                            polygon2 = aux;
                            polygon1.add(v);
                        }
                    }
                    execute(polygon1);
                    execute(polygon2);
                } else {
                    throw new RuntimeException();
                }
            }
        }
    }
}
