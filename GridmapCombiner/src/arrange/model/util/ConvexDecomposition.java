package arrange.model.util;

import java.util.ArrayList;
import java.util.List;
import arrange.Utils.Utils;

/**
 * Given the vertices of a simple polygon in counterclockwise order, returns a
 * decomposition into convex polygons. No guarantees are given with respect to
 * the number of polygons returned, i.e., it might be way more than the minimum
 * needed. In the worst case, returns a triangulation using quadratic time.
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public final class ConvexDecomposition<V extends Position2D> {

    private final ArrayList<ArrayList<V>> polygons = new ArrayList<>();
    private final ArrayList<Pair<V, V>> edges = new ArrayList<>();

    public ConvexDecomposition(List<? extends V> vertices) {
        execute(vertices);
    }

    public int numberOfPolygons() {
        return polygons.size();
    }

    public int numberOfEdges() {
        return edges.size();
    }

    public Iterable<ArrayList<V>> polygons() {
        return polygons;
    }

    public Iterable<Pair<V, V>> edges() {
        return edges;
    }

    public ArrayList<V> getPolygon(int index) {
        return polygons.get(index);
    }

    public Pair<V, V> getEdge(int index) {
        return edges.get(index);
    }

    private void execute(List<? extends V> vertices) {
        if (vertices.size() < 3) {
            throw new IllegalArgumentException("input polygon must have at least three vertices");
        } else if (vertices.size() == 3) {
            polygons.add(new ArrayList<>(vertices));
        } else {
            // Find reflex vertex
            V reflex = null;
            CircularListIterator<? extends V> cit = new CircularListIterator<>(vertices);
            V previous = cit.previous();
            cit.next();
            final V first = cit.next();
            V current = first;
            V next = cit.next();
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
                V closestCandidate = null;
                V reflexNext = next;
                V reflexPrevious = previous;
                double minDistance = Double.POSITIVE_INFINITY;
                for (V candidate : vertices) {
                    if (candidate != reflex && candidate != reflexNext && candidate != reflexPrevious) {
                        boolean isVisible = true;
                        cit = new CircularListIterator<>(vertices);
                        current = cit.next();
                        next = cit.next();
                        do {
                            if (current != reflex && current != candidate
                                    && next != reflex && next != candidate) {
                                Vector2D intersection = Utils.lineSegmentIntersection(reflex.getPosition(),
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
                                Vector2D diff = Vector2D.difference(reflex.getPosition(),
                                        candidate.getPosition());
                                double distance = diff.norm();
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
                    ArrayList<V> polygon1 = new ArrayList<>();
                    ArrayList<V> polygon2 = new ArrayList<>();
                    for (V v : vertices) {
                        polygon1.add(v);
                        if (v == reflex || v == closestCandidate) {
                            ArrayList<V> aux = polygon1;
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
