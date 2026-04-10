package arrange.algorithms.Moves;

import arrange.Utils.Utils;
import arrange.algorithms.Separator;
import arrange.model.Cartogram.MosaicCartogram;
import arrange.model.Network;
import arrange.model.subdivision.Map;
import arrange.model.util.Vector2D;

/**
 *
 * @author Max Sondag
 */
public final class SlideMove extends Move {

    private final Separator separator;
    private final MosaicCartogram.Coordinate direction;
    private Map map;

    public SlideMove(MosaicCartogram currentGrid, Map map, Separator separator, MosaicCartogram.Coordinate direction) {
        this.currentGrid = currentGrid;
        this.map = map;
        this.separator = separator;
        this.direction = direction;
    }

    @Override
    public double evaluate() {
        if (directionImproves()) {
            valid = true;
            outerloop:
            for (int index : separator.component1) {
                MosaicCartogram.MosaicRegion region = currentGrid.getRegion(index);
                for (MosaicCartogram.Coordinate c : region) {
                    Network.Vertex v = currentGrid.getVertex(c.plus(direction));
                    if (v != null && !separator.component1.contains(v.getId())) {
                        valid = false;
                        break outerloop;
                    }
                }
            }
            if (valid) {
                currentGrid.translateRegions(separator.component1, direction);
                if (!gridIsValid()) {
                    valid = false;
                }
                currentGrid.translateRegions(separator.component1, direction.times(-1));
            }
        } else {
            valid = false;
        }
        return 0;
    }

    @Override
    public double execute() {
        currentGrid.translateRegions(separator.component1, direction);
        return 0;
    }

    private boolean directionImproves() {
        int id1 = separator.v1.getId();
        int id2 = separator.v2.getId();
        MosaicCartogram.MosaicRegion region1 = currentGrid.getRegion(id1);
        MosaicCartogram.MosaicRegion region2 = currentGrid.getRegion(id2);
        Map.Face face1 = map.getFace(id1);
        Map.Face face2 = map.getFace(id2);
        Vector2D baryRegion1 = region1.continuousBarycenter();
        Vector2D baryRegion2 = region2.continuousBarycenter();
        Vector2D newBaryRegion1 = Vector2D.sum(baryRegion1, direction.toVector2D());
        Vector2D diffBaryFace = Vector2D.difference(face2.getCentroid(), face1.getCentroid());
        Vector2D diffBaryRegion = Vector2D.difference(baryRegion2, baryRegion1);
        Vector2D diffNewBaryRegion = Vector2D.difference(baryRegion2, newBaryRegion1);
        double desiredAngle = Math.atan2(diffBaryFace.getY(), diffBaryFace.getX());
        double currentAngle = Math.atan2(diffBaryRegion.getY(), diffBaryRegion.getX());
        double newAngle = Math.atan2(diffNewBaryRegion.getY(), diffNewBaryRegion.getX());
        double currentArc = Utils.angleDifference(desiredAngle, currentAngle);
        double newArc = Utils.angleDifference(desiredAngle, newAngle);
        if (newArc < currentArc) {
            return true;
        } else {
            return false;
        }
    }
}
