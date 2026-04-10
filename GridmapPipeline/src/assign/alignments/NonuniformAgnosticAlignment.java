/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package assign.alignments;

import assign.AgnosticAlignment;
import common.util.Transform;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;

/**
 *
 * @author wmeulema
 */
public class NonuniformAgnosticAlignment extends AgnosticAlignment {

    public NonuniformAgnosticAlignment() {
        super("Nonuniform-Agnostic");
    }
    
    @Override
    public Transform construct(List<? extends Vector> sites, List<? extends Vector> cells) {
        Vector sitecentroid = centroid(sites);
        Vector cellcentroid = centroid(cells);

        double siteXvar = 0;
        double siteYvar = 0;
        for (Vector site : sites) {
            double dx = site.getX() - sitecentroid.getX();
            double dy = site.getY() - sitecentroid.getY();
            siteXvar += dx * dx;
            siteYvar += dy * dy;
        }

        double cellXvar = 0;
        double cellYvar = 0;
        for (Vector cell : cells) {
            double dx = cell.getX() - cellcentroid.getX();
            double dy = cell.getY() - cellcentroid.getY();
            cellXvar += dx * dx;
            cellYvar += dy * dy;
        }

        double scalex = Math.sqrt(cellXvar / siteXvar);
        double scaley = Math.sqrt(cellYvar / siteYvar);

        sitecentroid.scale(scalex, scaley);

        // then translate to align
        Vector translate = Vector.subtract(cellcentroid, sitecentroid);

        return new Transform(translate, scalex, scaley);
    }

}
