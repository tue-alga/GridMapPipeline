package combine.combinations;

import common.gridmath.GridMath.Coordinate;
import common.Partition;
import common.Site;
import common.dual.Dual;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;

/**
 *
 * @author Wouter Meulemans
 */
public class AssignedCombination extends OffsetSearcher {

    public AssignedCombination() {
        super("ClosestAssigned");
    }

    @Override
    protected Vector getClosestCoordinateAndAngle(List<Dual> placedComponents, Dual curComponent, Coordinate[] placedCurClosest) {

        Vector bestAngle = null;
        double bestLength = Double.MAX_VALUE;

        //face is from the input map. A region can have multiple faces, one for each region defined in the input map
        for (Partition cur_p : curComponent.primalPartitions()) {
            for (Site s : cur_p.sites) {

                for (Dual placed : placedComponents) {
                    for (Partition placed_p : placed.primalPartitions()) {
                        for (Site placed_s : placed_p.sites) {

                            double squaredLength = placed_s.squaredDistanceTo(s);
                            if (squaredLength < bestLength) {
                                bestAngle = Vector.subtract(s, placed_s);
                                bestLength = squaredLength;

                                placedCurClosest[0] = placed_s.getCell();
                                placedCurClosest[1] = s.getCell();
                            }
                        }
                    }
                }
            }
        }

        return bestAngle;
    }

    @Override
    public boolean requiresAssigned() {
        return true;
    }

}
