package io;

import coloring.ColorPicker;
import common.gridmath.GridMath.Coordinate;
import common.Outline;
import common.Partition;
import common.Site;
import common.SiteMap;
import common.Stage;
import common.gridmath.GridGeometry;
import common.gridmath.GridGeometrySpawner;
import common.gridmath.GridMath;
import common.gridmath.util.CoordinateSet;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.tue.geometrycore.geometry.BaseGeometry;
import nl.tue.geometrycore.geometry.GeometryType;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.geometry.mix.GeometryGroup;
import nl.tue.geometrycore.geometryrendering.styling.Dashing;
import nl.tue.geometrycore.geometryrendering.styling.Hashures;
import nl.tue.geometrycore.geometryrendering.styling.TextAnchor;
import nl.tue.geometrycore.io.ReadItem;
import nl.tue.geometrycore.io.ipe.IPEReader;
import nl.tue.geometrycore.io.ipe.IPEWriter;
import partition.PartitionAlgorithm;

/**
 *
 * @author Wouter Meulemans
 */
public class IPE {

    public static final String outline_prefix = "outline";
    public static final String site_prefix = "sites";
    public static final String partition_prefix = "partition";
    public static final String deform_prefix = "cells";
    public static final String assign_prefix = "gridmap";
    
    public static void write(File f, SiteMap map) {
        write(f,map, Stage.ALL_STAGES);
    }

    public static void write(File f, SiteMap map, byte stages) {

        try (IPEWriter write = IPEWriter.fileWriter(f)) {

            write.configureTextHandling(false, 9, true);
            write.setTextSerifs(true);
            write.setTextStyle(TextAnchor.CENTER, 9);

            write.initialize();

            int pc = 0;
            if ((stages & Stage.PARTITIONED) > 0 && map.stage >= Stage.PARTITIONED) {
                writePartitioned(write, map);
                pc++;
            } else if ((stages & Stage.INPUT) > 0 && map.stage >= Stage.INPUT) {
                // NB: writing partitioned also includes the input, so we skip this if the above is performed
                writeInput(write, map);
                pc++;
            }

            if ((stages & Stage.DEFORMED) > 0 && map.stage >= Stage.DEFORMED) {
                writeDeformed(write, map);
                pc++;
            }

            if ((stages & Stage.ASSIGNED) > 0 && map.stage >= Stage.ASSIGNED) {
                writeAssigned(write, map);
                pc++;
            }

            if (pc == 0) {
                System.err.println("Warning: wrote an IPE file with zero pages? Map stage " + map.stage + " vs requested stages " + stages);
            }

        } catch (IOException ex) {
            Logger.getLogger(IPE.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Writes the map's outlines (with labels grouped) and sites (as
     * point-labels, colored) to the given ipe file.
     */
    private static void writeInput(IPEWriter write, SiteMap map) {

        List<String> layers = new ArrayList<>();
        layers.add(outline_prefix);
        for (Site s : map.sites()) {
            if (!layers.contains(s.getLayer())) {
                layers.add(s.getLayer());
            }
        }
        write.newPage(layers.toArray(new String[0]));

        writeOutlines(write, map);
        writeSites(write, map);
    }

    private static void writeOutlines(IPEWriter write, SiteMap map) {
        write.setLayer(outline_prefix);
        write.setStroke(Color.black, 0.4, Dashing.SOLID);
        for (Outline p : map.outlines) {
            write.setFill(p.color, Hashures.SOLID);
            write.pushGroup();
            write.draw(p);
            write.draw(p.getLabelPoint(), p.label);
            write.popGroup();
        }
    }

    private static void writeSites(IPEWriter write, SiteMap map) {
        for (Site s : map.sites()) {
            write.setLayer(s.getLayer());
            write.setStroke(s.getColor(), 0.4, Dashing.SOLID);
            write.draw(s, s.getLabel());
        }
    }

    /**
     * Writes the map's outlines (with labels grouped), sites (as point-labels,
     * colored), and partitions (grouped in layers per outline) to the given ipe
     * file.
     */
    private static void writePartitioned(IPEWriter write, SiteMap map) {
        List<String> layers = new ArrayList<>();
        layers.add(outline_prefix);
        for (Outline o : map.outlines) {
            layers.add(partition_prefix + "_" + o.label);
        }
        for (Site s : map.sites()) {
            if (!layers.contains(s.getLayer())) {
                layers.add(s.getLayer());
            }
        }
        write.newPage(layers.toArray(new String[0]));

        writeOutlines(write, map);

        write.setStroke(Color.black, 0.4, Dashing.SOLID);
        for (Partition p : map.partitions()) {
            write.setLayer(partition_prefix + "_" + p.outline.label);
            write.setFill(p.color, Hashures.SOLID);
            write.pushGroup();
            write.draw(p);
            write.draw(p.getLabelPoint(), p.label);
            write.popGroup();
        }

        writeSites(write, map);
    }

    /**
     * Writes the map's cartogram, with layers indicating the original partition
     * label.
     */
    private static void writeDeformed(IPEWriter write, SiteMap map) {
        String fullprefix = deform_prefix + "_" + map.gridmath.geometry().toString() + "_";
        List<String> layers = new ArrayList<>();
        for (Partition p : map.partitions()) {
            layers.add(fullprefix + p.label);
        }
        write.newPage(layers.toArray(new String[0]));

        write.setStroke(Color.black, 0.4, Dashing.SOLID);
        for (Partition p : map.partitions()) {
            write.setLayer(fullprefix + p.label);
            write.setFill(p.color, Hashures.SOLID);
            for (Coordinate cell : p.cells) {
                write.draw(cell.getBoundary());
            }
        }
    }

    /**
     * Writes the assigned grid map, with layers indicating the original site's
     * layer. One layer contains the grid map cells, the other the associated
     * labels.
     */
    private static void writeAssigned(IPEWriter write, SiteMap map) {

        List<String> layers = new ArrayList<>();
        for (Site s : map.sites()) {
            if (!layers.contains(assign_prefix + "_" + s.getLayer())) {
                layers.add(assign_prefix + "_" + s.getLayer());
                layers.add(assign_prefix + "_" + s.getLayer() + "_labels");
            }
        }
        write.newPage(layers.toArray(new String[0]));

        write.setStroke(Color.black, 0.4, Dashing.SOLID);
        for (Site s : map.sites()) {
            write.setLayer(assign_prefix + "_" + s.getLayer());
            write.setFill(s.getColor(), Hashures.SOLID);
            write.draw(s.getCell().getBoundary());
        }

        for (Site s : map.sites()) {
            write.setLayer(assign_prefix + "_" + s.getLayer() + "_labels");
            write.draw(s.getCell().toVector(), s.getLabel());
        }
    }
    public static SiteMap read(File f) {
        return read(f, Stage.ALL_STAGES);
    }
    
    public static SiteMap read(File f, byte stages) {
        SiteMap map = new SiteMap();
        read(f, map, stages);
        return map;
    }

    public static void read(File f, SiteMap map, byte stages) {
        
        ColorPicker.reset();

        try (IPEReader read = IPEReader.fileReader(f)) {
            read.setBezierSampling(2);
            List<List<ReadItem>> pages = read.readPages();

            if ((stages & Stage.INPUT) > 0) {

                // find a page that has an outline and sites layer
                List<ReadItem> inputpage = findPageByLayerPrefixes(pages, site_prefix, outline_prefix);

                if (inputpage != null) {
                    map.revertToStage(Stage.EMPTY);
                    readInput(inputpage, map);
                    map.stage = Stage.INPUT;
                } else {
                    System.err.println("Warning: tried reading input but no input/partition page found in file.");
                }

            }

            if ((stages & Stage.PARTITIONED) > 0) {

                List<ReadItem> partitionpage = findPageByLayerPrefixes(pages, partition_prefix);

                if (partitionpage != null) {
                    if (map.stage < Stage.INPUT) {
                        System.err.println("Warning: trying to read a deformed into an empty map");
                    }
                    map.revertToStage(Stage.INPUT);
                    readPartition(partitionpage, map);
                    map.stage = Stage.PARTITIONED;
                } else {
                    System.err.println("Warning: tried reading partition but no partition page found in file.");
                }
            }

            if ((stages & Stage.DEFORMED) > 0) {

                List<ReadItem> deformedpage = findPageByLayerPrefixes(pages, deform_prefix);

                if (deformedpage != null) {

                    if (map.stage < Stage.INPUT) {
                        System.err.println("Warning: trying to read a deformed into an empty map");
                    }
                    map.revertToStage(Stage.PARTITIONED);
                    if (map.stage < Stage.PARTITIONED) {
                        PartitionAlgorithm.trivialPartition(map);
                    }

                    readDeformed(deformedpage, map);
                    map.stage = Stage.DEFORMED;
                } else {
                    System.err.println("Warning: tried reading deformed but no deformed page found in file.");
                }
            }

            if ((stages & Stage.ASSIGNED) > 0) {

                List<ReadItem> assignedpage = findPageByLayerPrefixes(pages, assign_prefix);

                if (assignedpage != null) {
                    map.revertToStage(Stage.DEFORMED);
                    if (map.stage < Stage.DEFORMED) {
                        System.err.println("Warning: trying to read an assignment into an undeformed map");
                    }
                    readAssigned(assignedpage, map);
                    map.stage = Stage.ASSIGNED;
                } else {
                    System.err.println("Warning: tried reading assigned but no assigned page found in file.");
                }
            }

        } catch (IOException ex) {
            Logger.getLogger(IPE.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static List<ReadItem> findPageByLayerPrefixes(List<List<ReadItem>> pages, String... prefixes) {
        final int total = prefixes.length;
        for (List<ReadItem> page : pages) {
            boolean[] found = new boolean[total];
            Arrays.fill(found, false);

            int count = 0;
            for (ReadItem ri : page) {
                for (int i = 0; i < total; i++) {
                    if (!found[i] && ri.getLayer().startsWith(prefixes[i])) {
                        found[i] = true;
                        count++;
                        if (count == total) {
                            return page;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static void readInput(List<ReadItem> items, SiteMap map) {

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
                System.err.println("Unexpected geometry: "+ri.getGeometry().getGeometryType());
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
    }

    private static void readPartition(List<ReadItem> items, SiteMap map) {

        // read partitions
        items.forEach((ri) -> {
            if (!ri.getLayer().startsWith(partition_prefix)) {
                return;
            }

            String outlinelabel = ri.getLayer().substring(partition_prefix.length() + 1);
            Outline outline = null;
            for (Outline o : map.outlines) {
                if (o.label.equals(outlinelabel)) {
                    outline = o;
                    break;
                }
            }

            if (outline == null) {
                System.err.println("No outline found for layer " + ri.getLayer());
                return;
            }

            if (ri.getGeometry().getGeometryType() == GeometryType.GEOMETRYGROUP) {
                GeometryGroup<? extends BaseGeometry> group = (GeometryGroup) (ri.getGeometry());
                Partition p = null;
                for (BaseGeometry part : group.getParts()) {
                    if (part.getGeometryType() == GeometryType.POLYGON) {
                        Polygon polygon = (Polygon) part;
                        p = new Partition(polygon.vertices());
                        p.color = ri.getFill();
                        if (p.color == null) {
                            p.color = ColorPicker.getNewColor();
                        }
                        p.label = ri.getString();
                        p.outline = outline;
                        outline.partitions.add(p);
                    } else if (part.getGeometryType() == GeometryType.VECTOR) {
                        p.labelPoint = (Vector) part;
                    }
                }
            } else if (ri.getGeometry().getGeometryType() == GeometryType.POLYGON) {
                // read the shape only, label is to come later
                Polygon polygon = (Polygon) ri.getGeometry();
                Partition p = new Partition(polygon.vertices());
                p.color = ri.getFill();
                if (p.color == null) {
                    p.color = ColorPicker.getNewColor();
                }
                p.outline = outline;
                outline.partitions.add(p);

            } else if (ri.getString() != null && ri.getGeometry().getGeometryType() == GeometryType.VECTOR) {
                // NB: assumes that labels are placed above the actual partition polygon
                Vector v = (Vector) ri.getGeometry();
                for (Partition p : outline.partitions) {
                    if (p.contains(v)) {
                        p.labelPoint = v;
                        break;
                    }
                }
            }
        });

        // create dummy labels for unlabeled outlines
        int i = 0;
        for (Partition p : map.partitions()) {
            i++;
            if (p.label == null) {
                p.label = p.outline.label + ":" + i;
            }
        }

        // assign all cells to respective partitions
        for (Outline o : map.outlines) {
            for (Site s : o.sites) {
                for (Partition p : o.partitions) {
                    if (p.contains(s)) {
                        p.sites.add(s);
                        s.setPartition(p);
                        break;
                    }
                }
                if (s.getPartition() == null) {
                    System.err.println("No partition found for site " + s);
                }
            }
        }
    }

    private static void readDeformed(List<ReadItem> items, SiteMap map) {

        for (ReadItem ri : items) {
            if (!ri.getLayer().startsWith(deform_prefix)) {
                continue;
            }

            Polygon polygon = (Polygon) ri.getGeometry();

            if (map.gridmath == null) {
                String gridname = ri.getLayer();
                gridname = gridname.substring(deform_prefix.length() + 1); // skip over first _
                gridname = gridname.substring(0, gridname.indexOf("_")); // get until the second _
                GridGeometry geom = GridGeometrySpawner.inferFromString(gridname);
                map.gridmath = new GridMath(geom);

                for (Partition p : map.partitions()) {
                    p.cells = new CoordinateSet(map.gridmath);
                }
            }

            String partitionLabel = ri.getLayer();
            partitionLabel = partitionLabel.substring(deform_prefix.length() + 1); // skip over first _
            partitionLabel = partitionLabel.substring(partitionLabel.indexOf("_") + 1); // skip over second _

            Partition partition = null;
            for (Partition p : map.partitions()) {
                if (p.label.equals(partitionLabel)) {
                    partition = p;
                    break;
                }
            }

            if (partition == null) {
                System.err.println("No partition found for partition found for layer " + partitionLabel);
                continue;
            }

            Coordinate coordinate = map.gridmath.getContainingCell(polygon.centroid());
            partition.cells.add(coordinate);

        }
    }

    private static void readAssigned(List<ReadItem> items, SiteMap map) {
        // read the point labels for matching
        items.forEach((ri) -> {
            if (!ri.getLayer().startsWith(assign_prefix)) {
                return;
            }

            if (ri.getString() == null || ri.toGeometry().getGeometryType() != GeometryType.VECTOR) {
                return;
            }

            String label = ri.getString();
            Vector pt = (Vector) ri.toGeometry();

            Coordinate coord = map.gridmath.getContainingCell(pt);

            for (Site s : map.sites()) {
                if (s.getLabel().equals(label)) {
                    s.setCell(coord);
                    return; // NB: RETURN!
                }
            }

            System.err.println("No site labeled " + label + " found");
        });
    }

}
