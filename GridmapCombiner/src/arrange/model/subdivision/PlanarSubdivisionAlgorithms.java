package arrange.model.subdivision;

import arrange.Utils.Utils;
import arrange.model.subdivision.PlanarSubdivision.Face;
import arrange.model.subdivision.PlanarSubdivision.Halfedge;
import arrange.model.subdivision.PlanarSubdivision.Vertex;
import arrange.model.util.Vector2D;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class PlanarSubdivisionAlgorithms {

    /**
     * Tests whether this subdivision is a triangulation. The outer face is not
     * considered.
     */
    public static boolean isTriangulation(PlanarSubdivision subdivision) {
        for (Face f : subdivision.boundedFaces()) {
            if (f.getBoundaryVertices().size() != 3) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the face that contains the given point. If the point lies outside
     * of the convex hull, the unbounded face is returned. Linear time
     * implementation.
     */
    public static Face containingFace(PlanarSubdivision subdivision, Vector2D point) {
        // Find minimum x coordinate
        double minX = Double.POSITIVE_INFINITY;
        for (Vertex v : subdivision.vertices()) {
            double x = v.getPosition().getX();
            if (x < minX) {
                minX = x;
            }
        }

        // Create horizontal line segment to test for intersections
        Vector2D point2 = new Vector2D(minX - 1, point.getY());

        // Find last halfedge to the left of the point
        Halfedge lastLeft = null;
        Vector2D lastIntersection = new Vector2D(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        for (Halfedge h : subdivision.halfedges()) {
            Vector2D pSource = h.getSource().getPosition();
            Vector2D pTarget = h.getTarget().getPosition();
            // Make sure that the point is to the left of the halfedge
            if (pSource.getY() > pTarget.getY()) {
                Vector2D intersection = Utils.lineSegmentIntersection(point, point2, pSource, pTarget);
                if (intersection != null && intersection.getX() > lastIntersection.getX()) {
                    lastIntersection = intersection;
                    lastLeft = h;
                }
            }
        }

        // Return the face to the left of the halfedge
        if (lastLeft == null) {
            return subdivision.getUnboundedFace();
        } else {
            return lastLeft.getFace();
        }
    }
}
