package arrange.algorithms;

import arrange.algorithms.graph.Separator;
import arrange.model.MosaicCartogram;
import arrange.model.MosaicCartogram.MosaicRegion;
import arrange.util.Utils;
import common.dual.Dual;
import common.dual.Edge;
import common.dual.Vertex;
import common.gridmath.GridMath.Coordinate;
import common.util.Stopwatch;
import java.util.ArrayList;
import java.util.List;
import nl.tue.geometrycore.algorithms.dsp.DepthFirstSearch;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.LineSegment;

/**
 *
 * @author Rafael Cano, Wouter Meulemans
 */
public class MosaicSlider {

    private final List<Separator> separators;

    public MosaicSlider(Dual dual) {
        this.separators = new ArrayList<>();
        for (Edge e : dual.getEdges()) {
            DepthFirstSearch<Dual, LineSegment, Vertex, Edge> dfs = new DepthFirstSearch<>(dual);
            if (!dfs.runTo(e.getStart(), e.getEnd(), e)) {
                separators.add(new Separator(dual, e));
            }
        }
    }

    public void run(MosaicCartogram grid) {
        Stopwatch sw = Stopwatch.get("- slides").start();
        
        boolean keepGoing;
        Coordinate[] uvs = grid.getGridMath().unitVectors();
        do {
            keepGoing = false;
            
            for (Separator separator : separators) {
                for (Coordinate direction : uvs) {
                    
                    if (directionImproves(grid, separator, direction)
                            && spaceNotOccupied(grid, separator, direction)) {
                        grid.translateRegions(separator.component1, direction);
                        if (!grid.isConnectedAndCorrectAdjacencies()) {
                            // revert, we disconnected something, or introduced a new adjacency
                            // TODO: this can probably be improved...
                            grid.translateRegions(separator.component1, direction.times(-1));
                        } else {
                            keepGoing = true;
                        }
                    }
                    
                }
            }
        } while (keepGoing);
        
        sw.stop();
    }

    private boolean spaceNotOccupied(MosaicCartogram grid, Separator separator, Coordinate direction) {

        for (Vertex cv : separator.component1) {
            MosaicRegion region = grid.getRegion(cv);
            for (Coordinate c : region.getAllocated()) {
                Vertex v = grid.getVertex(c.plus(direction));
                if (v != null && !separator.component1.contains(v)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean directionImproves(MosaicCartogram grid, Separator separator, Coordinate direction) {
        final Vertex v1 = separator.v1;
        final Vertex v2 = separator.v2;
        MosaicCartogram.MosaicRegion region1 = grid.getRegion(v1);
        MosaicCartogram.MosaicRegion region2 = grid.getRegion(v2);

        double desiredAngle = separator.desiredAngle;

        Vector baryRegion1 = region1.getAllocated().continuousBarycenter();
        Vector baryRegion2 = region2.getAllocated().continuousBarycenter();
        Vector newBaryRegion1 = Vector.add(baryRegion1, direction.patternCentroid());

        Vector diffBaryRegion = Vector.subtract(baryRegion2, baryRegion1);
        Vector diffNewBaryRegion = Vector.subtract(baryRegion2, newBaryRegion1);

        double currentAngle = Math.atan2(diffBaryRegion.getY(), diffBaryRegion.getX());
        double newAngle = Math.atan2(diffNewBaryRegion.getY(), diffNewBaryRegion.getX());

        double currentArc = Utils.angleDifference(desiredAngle, currentAngle);
        double newArc = Utils.angleDifference(desiredAngle, newAngle);

        return newArc < currentArc;
    }
}
