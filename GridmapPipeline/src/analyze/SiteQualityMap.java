package analyze;

import common.Site;
import common.SiteMap;
import common.util.Transform;
import nl.tue.geometrycore.geometryrendering.GeometryRenderer;
import nl.tue.geometrycore.geometryrendering.styling.Hashures;

/**
 *
 * @author Wouter Meulemans
 */
public class SiteQualityMap extends QualityMap<Site> {

    public SiteQualityMap(String name, boolean highIsGood) {
        super(name, highIsGood);
    }

    @Override
    public void render(GeometryRenderer write, SiteMap map, Transform cartogramTransform) {

        for (Site s : map.sites()) {
            if (s.getCell() != null) {
                write.setFill(getColor(s), Hashures.SOLID);
                write.draw(cartogramTransform.apply(s.getCell().getBoundary()));
            } else {
                System.err.println("Warning: site not assigned to a cell?");
            }
        }
    }

}
