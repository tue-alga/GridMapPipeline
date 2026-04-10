package combine.combinations;

import common.gridmath.GridMath.Coordinate;
import common.Partition;
import common.dual.Dual;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;

/**
 *
 * @author Wouter Meulemans
 */
public class AgnosticCombination extends OffsetSearcher {

    public AgnosticCombination() {
        super("Agnostic");
    }

    @Override
    protected Vector getClosestCoordinateAndAngle(List<Dual> placedComponents, Dual curComponent, Coordinate[] placedCurClosest) {

        Vector bestAngle = null;
        double bestLength = Double.MAX_VALUE;

        //face is from the input map. A region can have multiple faces, one for each region defined in the input map
        for (Partition curP : curComponent.primalPartitions()) {
            Vector curCentroid = curP.centroid();
            Coordinate curPbary = curP.cells.barycenter();

            for (Dual placed : placedComponents) {
                for (Partition placedP : placed.primalPartitions()) {
                    Vector placedCentroid = placedP.centroid();

                    double squaredLength = placedCentroid.squaredDistanceTo(curCentroid);
                    if (squaredLength < bestLength) {
                        bestAngle = Vector.subtract(curCentroid, placedCentroid);
                        bestLength = squaredLength;

                        placedCurClosest[0] = placedP.cells.barycenter();
                        placedCurClosest[1] = curPbary;

                    }
                }
            }
        }

        return bestAngle;
    }

    @Override
    public boolean requiresAssigned() {
        return false;
    }
}
