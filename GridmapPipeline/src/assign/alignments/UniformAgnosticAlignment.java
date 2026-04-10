/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package assign.alignments;

import assign.AgnosticAlignment;
import static assign.AgnosticAlignment.centroid;
import common.util.Transform;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;

/**
 *
 * @author wmeulema
 */
public class UniformAgnosticAlignment extends AgnosticAlignment {

    public UniformAgnosticAlignment() {
        super("Uniform-Agnostic");
    }

    @Override
    public Transform construct(List<? extends Vector> sites, List<? extends Vector> cells) {

        // we map cells to sites
        // so, "sites" is "right"
        // and "cells" is "left"
        Vector sitecentroid = centroid(sites);
        Vector cellcentroid = centroid(cells);

        double site_sumofsquares = 0;
        for (Vector site : sites) {
            double d = site.squaredDistanceTo(sitecentroid);
            site_sumofsquares += d;
        }

        double cell_sumofsquares = 0;
        for (Vector cell : cells) {
            double d = cell.squaredDistanceTo(cellcentroid);
            cell_sumofsquares += d;
        }

        double scale = Math.sqrt(cell_sumofsquares / site_sumofsquares);

        sitecentroid.scale(scale);

        // then translate to align
        Vector translate = Vector.subtract(cellcentroid, sitecentroid);

        return new Transform(translate, scale);
    }

}
