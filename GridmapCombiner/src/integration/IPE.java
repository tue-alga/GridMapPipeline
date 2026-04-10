/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package integration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.tue.geometrycore.geometry.BaseGeometry;
import nl.tue.geometrycore.geometry.GeometryType;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.geometry.mix.GeometryGroup;
import nl.tue.geometrycore.io.ReadItem;
import nl.tue.geometrycore.io.ipe.IPEReader;

/**
 *
 * @author wmeulema
 */
public class IPE {

    public static final String outline_prefix = "outline";
    public static final String site_prefix = "sites";
    public static final String partition_prefix = "partition";
    public static final String deform_prefix = "cells";
    public static final String assign_prefix = "gridmap";

    public static SiteMap read(File f) {
        SiteMap map = new SiteMap();
        read(f, map, Stage.ALL_STAGES);
        return map;
    }

    public static void read(File f, SiteMap map, byte stages) {

        ColorPicker.reset();

        try (IPEReader read = IPEReader.fileReader(f)) {
            read.setBezierSampling(2);
            List<ReadItem> items = read.read();
            // read outlines: polygons or polygons grouped with a point label
            items.forEach((ri) -> {
                if (!ri.getLayer().startsWith(outline_prefix)) {
                    return;
                }

                if (ri.getGeometry().getGeometryType() == GeometryType.GEOMETRYGROUP) {
                    GeometryGroup<? extends BaseGeometry> group = (GeometryGroup) (ri.getGeometry());
                    Outline o = null;
                    for (BaseGeometry part : group.getParts()) {
                        if (part.getGeometryType() == GeometryType.POLYGON) {
                            Polygon polygon = (Polygon) part;
                            o = new Outline(polygon.vertices());
                            o.ensureCCW();
                            o.removeDegeneracies();
                            o.label = ri.getString();
                            o.color = ri.getFill();
                            if (o.color == null) {
                                o.color = ColorPicker.getNewColor();
                            }
                            map.outlines.add(o);
                        } else if (part.getGeometryType() == GeometryType.VECTOR) {
                            o.labelPoint = (Vector) part;
                        }
                    }
                } else if (ri.getGeometry().getGeometryType() == GeometryType.POLYGON) {
                    // read a polygon only, laebl is to come later
                    Polygon polygon = (Polygon) ri.getGeometry();
                    Outline o = new Outline(polygon.vertices());
                    o.ensureCCW();
                    o.removeDegeneracies();
                    o.color = ri.getFill();
                    if (o.color == null) {
                        o.color = ColorPicker.getNewColor();
                    }
                    map.outlines.add(o);
                } else if (ri.getString() != null || ri.getGeometry().getGeometryType() == GeometryType.VECTOR) {
                    // NB: assumes that the label is placed above the polygon in ipe
                    Vector v = (Vector) ri.getGeometry();
                    Outline o = map.findOutline(v);
                    if (o != null) {
                        o.label = ri.getString();
                        o.labelPoint = v;
                    } else {
                        System.out.println("No outline found for label " + ri.getString());
                    }
                } else {
                    System.err.println("Unexpected geometry: " + ri.getGeometry().getGeometryType());
                }
            });

            // create dummy labels for unlabeled outlines
            int i = 0;
            for (Outline o : map.outlines) {
                i++;
                if (o.label == null) {
                    o.label = "o" + i;
                    o.labelPoint = o.centroid();
                }
            }

            // read the polygon sites (if present; assumed to be unlabeled)
            int[] is = {0};
            items.forEach((ri) -> {
                if (!ri.getLayer().startsWith(site_prefix)) {
                    return;
                }

                Site s;
                if (ri.getGeometry().getGeometryType() == GeometryType.POLYGON) {
                    Polygon p = (Polygon) ri.getGeometry();
                    s = new Site(p.centroid(), "s" + (++is[0]), ri.getFill());
                    s.setLayer(ri.getLayer());
                } else {
                    return;
                }

                s.setLayer(ri.getLayer());
                Outline o = map.findOutline(s);
                if (o != null) {
                    o.sites.add(s);
                    s.setOutline(o);
                } else {
                    System.out.println("No outline found for site " + s);
                }
            });

            // find labels and/or positions
            items.forEach((ri) -> {
                if (!ri.getLayer().startsWith(site_prefix)) {
                    return;
                }

                if (ri.getString() == null || ri.getGeometry().getGeometryType() != GeometryType.VECTOR) {
                    return;
                }
                Vector v = (Vector) ri.getGeometry();
                Site s = new Site(v, ri.getString(), ri.getStroke());
                s.setLayer(ri.getLayer());
                Outline o = map.findOutline(s);
                if (o != null) {
                    o.sites.add(s);
                    s.setOutline(o);
                } else {
                    System.out.println("No outline found for site " + s);
                }
            });

        } catch (IOException ex) {
            Logger.getLogger(IPE.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
