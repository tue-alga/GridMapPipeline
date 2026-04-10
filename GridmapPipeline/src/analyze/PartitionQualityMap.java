package analyze;

import common.Partition;
import common.SiteMap;
import common.gridmath.GridMath;
import common.util.Transform;
import nl.tue.geometrycore.geometryrendering.GeometryRenderer;
import nl.tue.geometrycore.geometryrendering.styling.Dashing;
import nl.tue.geometrycore.geometryrendering.styling.ExtendedColors;
import nl.tue.geometrycore.geometryrendering.styling.Hashures;

/**
 *
 * @author Wouter Meulemans
 */
public class PartitionQualityMap extends QualityMap<Partition> {

    public PartitionQualityMap(String name, boolean highIsGood) {
        super(name, highIsGood);
    }

    @Override
    public void render(GeometryRenderer write, SiteMap map, Transform cartogramTransform) {

        write.setStroke(ExtendedColors.black, 1, Dashing.SOLID);
        for (Partition p : map.partitions()) {
            write.setFill(getColor(p), Hashures.SOLID);
            for (GridMath.Coordinate c : p.cells) {
                write.draw(cartogramTransform.apply(c.getBoundary()));
            }
        }
    }

}
