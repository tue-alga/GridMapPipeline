package arrange.model.graph;

import arrange.model.util.Position2D;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class PlanarStraightLineGraphAlgorithms {

    private PlanarStraightLineGraphAlgorithms() {
    }

    /**
     * Tests the given graph for edge crossings. Bad quadratic implementation.
     */
    public static <V extends AbstractVertex & Position2D, E extends AbstractEdge> boolean hasCrossings(AbstractGraph<V, E> g) {
        CrossingFinder<V, E> cf = new CrossingFinder<>(g);
        return cf.hasCrossings();
    }
}
