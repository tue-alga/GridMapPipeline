/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package integration;

import arrange.model.Cartogram.MosaicCartogram.CellRegion;
import arrange.model.Cartogram.MosaicCartogram.Coordinate;
import arrange.model.HexagonalMap;
import arrange.model.util.Vector2D;
import arrange.parameter.ParameterManager.Application.GridType;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Polygon;

/**
 *
 * @author wmeulema
 */
public class WKT {

    public static void write(File file, SiteMap map) {
        try (BufferedWriter write = new BufferedWriter(new FileWriter(file))) {

            write.append("STAGE " + map.stage);
            write.newLine();

            // input 
            if (map.stage >= Stage.INPUT) {
                writeOutlinesWithSites(write, map);
            }

            // partitioned
            if (map.stage >= Stage.PARTITIONED) {
                writePartitions(write, map);
            }

            // cartogram
            if (map.stage >= Stage.DEFORMED) {
                writeCartogram(write, map);
            }

            // assignment
            if (map.stage >= Stage.ASSIGNED) {
                writeAssignment(write, map);
            }

        } catch (IOException ex) {
            Logger.getLogger(WKT.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void writeOutlinesWithSites(BufferedWriter write, SiteMap map) throws IOException {
        write.append("OUTLINES " + map.outlines.size());
        write.newLine();

        for (int out_index = 0; out_index < map.outlines.size(); out_index++) {
            Outline out = map.outlines.get(out_index);
            write.append("OUT " + out_index
                    + "\t" + writeString(out.label)
                    + "\t" + vectorToString(out.labelPoint)
                    + "\t" + colorToString(out.color));
            write.newLine();
            writePolygon(write, out);

            write.append("SITECOUNT " + out.sites.size());
            write.newLine();

            for (int site_index = 0; site_index < out.sites.size(); site_index++) {
                Site s = out.sites.get(site_index);
                write.append("SITE " + out_index + " " + site_index
                        + "\t" + writeString(s.getLabel())
                        + "\t" + vectorToString(s)
                        + "\t" + colorToString(s.getColor())
                        + "\t" + writeString(s.getLayer()));
                write.newLine();
            }
        }
    }

    private static void writePartitions(BufferedWriter write, SiteMap map) throws IOException {
        int cnt = 0;
        for (Outline out : map.outlines) {
            cnt += out.partitions.size();
        }

        write.append("PARTITIONS " + cnt);
        write.newLine();

        for (int out_index = 0; out_index < map.outlines.size(); out_index++) {
            Outline out = map.outlines.get(out_index);
            for (int part_index = 0; part_index < out.partitions.size(); part_index++) {
                Partition part = out.partitions.get(part_index);
                write.append("PART " + out_index + " " + part_index
                        + "\t" + writeString(part.label)
                        + "\t" + vectorToString(part.labelPoint)
                        + "\t" + colorToString(part.color));
                write.newLine();
                writePolygon(write, part);
                boolean first = true;
                for (Site s : part.sites) {
                    if (first) {
                        first = false;
                        write.append("" + part.outline.sites.indexOf(s));
                    } else {
                        write.append("\t" + part.outline.sites.indexOf(s));
                    }
                }
                write.newLine();
            }
        }
    }

    private static void writeCartogram(BufferedWriter write, SiteMap map) throws IOException {
        write.append("CARTOGRAM " + (map.gridType == GridType.HEXAGONAL ? "HEXAGON" : "SQUARE"));
        write.newLine();

        for (int out_index = 0; out_index < map.outlines.size(); out_index++) {
            Outline out = map.outlines.get(out_index);
            for (int part_index = 0; part_index < out.partitions.size(); part_index++) {
                Partition part = out.partitions.get(part_index);

                if (part.mosaic.getGuidingShape() == null) {
                    write.append("PART " + out_index + " " + part_index);
                    write.newLine();
                    writeCoordinateSet(write, part.mosaic);
                } else {
                    write.append("PART " + out_index + " " + part_index
                            + "\t" + part.mosaic.factor
                            + "\t" + vectorToString(part.mosaic.guidingShapeTranslation));
                    write.newLine();
                    writeCoordinateSet(write, part.mosaic);
                    writeCoordinateSet(write, part.mosaic.getGuidingShape());
                    writeQualityMap(write, part.mosaic.qualitymap, part.mosaic.totalTranslation);
                }
            }
        }
    }

    private static void writeAssignment(BufferedWriter write, SiteMap map) throws IOException {
        write.append("ASSIGNMENT");
        write.newLine();

        for (int i = 0; i < map.outlines.size(); i++) {
            Outline out = map.outlines.get(i);
            for (int j = 0; j < out.sites.size(); j++) {
                Site s = out.sites.get(j);
                write.append("SITE " + i + " " + j
                        + "\t" + coordinateToString(s.getCell()));
                write.newLine();
            }
        }
    }

    private static final String NULL = "<NULL>";

    private static String writeString(String s) {
        if (s == null) {
            return NULL;
        }
        return s;
    }

    private static String vectorToString(Vector v) {
        if (v == null) {
            return NULL;
        }
        return v.getX() + " " + v.getY();
    }

    private static String vectorToString(Vector2D v) {
        if (v == null) {
            return NULL;
        }
        return v.getX() + " " + v.getY();
    }

    private static String coordinateToString(Coordinate c) {
        if (c == null) {
            return NULL;
        }
        Coordinate cn = c.normalize();
        int x = cn.getComponents()[0];
        int y = cn.getComponents()[1];
        if (cn instanceof HexagonalMap.BarycentricCoordinate) {
            x = x - y;
            y = -y;
        }
        return x + " " + y;
    }

    private static String colorToString(Color c) {
        if (c == null) {
            return NULL;
        }
        return c.getRed() + " " + c.getGreen() + " " + c.getBlue();
    }

    private static void writePolygon(BufferedWriter write, Polygon p) throws IOException {
        write.append(vectorToString(p.vertex(0)));
        for (int j = 1; j < p.vertexCount(); j++) {
            write.append("\t" + vectorToString(p.vertex(j)));
        }
        write.newLine();
    }

    private static void writeCoordinateSet(BufferedWriter write, CellRegion set) throws IOException {
        boolean first = true;
        for (Coordinate c : set) {
            if (first) {
                first = false;
            } else {
                write.append("\t");
            }
            write.append(coordinateToString(c));
        }
        write.newLine();
    }

    private static void writeQualityMap(BufferedWriter write, LinkedHashMap<Coordinate, Double> map, Coordinate delta) throws IOException {
        boolean first = true;
        for (Entry<Coordinate, Double> e : map.entrySet()) {
            if (first) {
                first = false;
            } else {
                write.append("\t");
            }
            Coordinate c = e.getKey().plus(delta);
            double area = -e.getValue();
            write.append(coordinateToString(c) + ";" + area);
        }
        write.newLine();
    }

    public static SiteMap read(File file) {
        SiteMap map = new SiteMap();

        try (BufferedReader read = new BufferedReader(new FileReader(file))) {

            map.stage = Byte.parseByte(read.readLine().split(" ")[1]);

            if (map.stage >= Stage.INPUT) {
                readOutlinesWithSites(read, map);
            }

        } catch (IOException ex) {
            Logger.getLogger(WKT.class.getName()).log(Level.SEVERE, null, ex);
        }

        return map;
    }

    private static void readOutlinesWithSites(BufferedReader read, SiteMap map) throws IOException {

        int num_outlines = Integer.parseInt(read.readLine().split(" ")[1]);

        while (num_outlines > 0) {

            Outline out = new Outline();
            map.outlines.add(out);

            String[] vals = read.readLine().split("\t");
            out.label = readString(vals[1]);
            out.labelPoint = stringToVector(vals[2]);
            out.color = stringToColor(vals[3]);

            readPolygon(read, out);

            int num_sites = Integer.parseInt(read.readLine().split(" ")[1]);

            while (num_sites > 0) {

                String[] sitevals = read.readLine().split("\t");
                String label = readString(sitevals[1]);
                Vector pos = stringToVector(sitevals[2]);
                Color col = stringToColor(sitevals[3]);
                String layer = readString(sitevals[4]);

                Site s = new Site(pos, label, col);
                s.setLayer(layer);
                out.sites.add(s);

                num_sites--;
            }

            num_outlines--;
        }
    }

    private static String readString(String s) {
        if (s.equals(NULL)) {
            return null;
        }
        return s;
    }

    private static Vector stringToVector(String s) {
        if (s.equals(NULL)) {
            return null;
        }
        String[] xy = s.split(" ");
        return new Vector(Double.parseDouble(xy[0]), Double.parseDouble(xy[1]));
    }

    private static Color stringToColor(String s) {
        if (s.equals(NULL)) {
            return null;
        }
        String[] rgb = s.split(" ");
        return new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
    }

    private static void readPolygon(BufferedReader read, Polygon p) throws IOException {
        String[] vs = read.readLine().split("\t");
        for (String v : vs) {
            p.addVertex(stringToVector(v));
        }
    }
}
