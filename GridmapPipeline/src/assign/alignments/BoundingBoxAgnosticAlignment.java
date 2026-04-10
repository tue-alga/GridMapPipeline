package assign.alignments;

import assign.AgnosticAlignment;
import common.util.Transform;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Rectangle;

/**
 *
 * @author Wouter Meulemans
 */
public class BoundingBoxAgnosticAlignment extends AgnosticAlignment {

    public BoundingBoxAgnosticAlignment() {
        super("BBox-Agnostic");
    }

    @Override
    public Transform construct(List<? extends Vector> sites, List<? extends Vector> cells) {
        Rectangle sitebox = Rectangle.byBoundingBox(sites);
        Rectangle cellbox = Rectangle.byBoundingBox(cells);
        
        // we first scale to be the same size
        double scalex = cellbox.width() / sitebox.width();
        double scaley = cellbox.height() / sitebox.height();

        sitebox.scale(scalex, scaley);

        // then translate to align
        Vector translate = Vector.subtract(cellbox.leftBottom(), sitebox.leftBottom());
        return new Transform(translate, scalex, scaley);
    }
}
