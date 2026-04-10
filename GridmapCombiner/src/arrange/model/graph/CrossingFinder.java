package arrange.model.graph;

import arrange.Utils.Utils;
import arrange.model.util.Position2D;
import arrange.model.util.Vector2D;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class CrossingFinder<V extends AbstractVertex & Position2D, E extends AbstractEdge> {

    private final AbstractGraph<V, E> g;
    private boolean hasCrossings;
    private E firstEdge;
    private E secondEdge;
    private Vector2D crossingPoint;

    public CrossingFinder(AbstractGraph<V, E> g) {
        this.g = g;
        computeCrossing();
    }

    public void recompute() {
        computeCrossing();
    }

    public boolean hasCrossings() {
        return hasCrossings;
    }

    public E getFirstEdge() {
        return firstEdge;
    }

    public E getSecondEdge() {
        return secondEdge;
    }

    public Vector2D getCrossingPoint() {
        return crossingPoint;
    }

    private void computeCrossing() {
        hasCrossings = false;
        firstEdge = null;
        secondEdge = null;
        crossingPoint = null;
        for (int i = 0; i < g.numberOfEdges(); i++) {
            E ei = g.getEdge(i);
            V eiSource = g.getSource(ei);
            V eiTarget = g.getTarget(ei);
            Vector2D piSource = eiSource.getPosition();
            Vector2D piTarget = eiTarget.getPosition();
            for (int j = i + 1; j < g.numberOfEdges(); j++) {
                E ej = g.getEdge(j);
                V ejSource = g.getSource(ej);
                V ejTarget = g.getTarget(ej);
                Vector2D pjSource = ejSource.getPosition();
                Vector2D pjTarget = ejTarget.getPosition();
                if (eiSource == ejSource || eiSource == ejTarget || eiTarget == ejSource || eiTarget == ejTarget) {
                    continue;
                }
                Vector2D crossing = Utils.lineSegmentIntersection(piSource, piTarget, pjSource, pjTarget);
                if (crossing != null) {
                    //if the crossing is on the line, ignore it
                    if (distance(piSource, piTarget, crossing) < Utils.EPS) {
                        continue;
                    }
                    if (distance(pjSource, pjTarget, crossing) < Utils.EPS) {
                        continue;
                    }

                    firstEdge = ei;
                    secondEdge = ej;
                    crossingPoint = crossing;
                   // System.out.println(crossingPoint);
                    return;
                }
            }
        }
    }

    private double distance(Vector2D v1, Vector2D v2, Vector2D crossing) {
        //check whether the point is on the line. if it is, we are fine.
        Vector2D closestPoint = Utils.closestPoint(crossing, v1, v2);
        Vector2D distanceV = closestPoint.subtract(crossing);

        return distanceV.getX() * distanceV.getX() + distanceV.getY() * distanceV.getY();
    }
}
