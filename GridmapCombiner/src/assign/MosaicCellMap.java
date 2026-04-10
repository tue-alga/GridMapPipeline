/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assign;

import integration.Partition;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author msondag
 */
public class MosaicCellMap {

    //For each parent label, holds all shape centroids with the same label
    HashMap<String, List<MosaicCell>> inputCellMapping = new HashMap();

    public MosaicCellMap(Path inputMapPath) {
        try {
            List<String> mapLines = Files.readAllLines(inputMapPath);
            readMap(mapLines);
        } catch (IOException ex) {
            Logger.getLogger(MosaicCellMap.class.getName()).log(Level.SEVERE, null, ex);
        }
        //shifts the map such that it align with the origin
        //Note that it does this both for the centroid and the cells.
        shiftMapToOrigin();

    }

    public MosaicCellMap(integration.SiteMap sitemap) {
        for (Partition p : sitemap.partitions()) {

            List<MosaicCell> cells = new ArrayList();

            for (arrange.model.Cartogram.MosaicCartogram.Coordinate c : p.mosaic.coordinateSet()) {
                cells.add(new MosaicCell(c, p.label));
            }

            inputCellMapping.put(p.label, cells);

        }

        //shifts the map such that it align with the origin
        //Note that it does this both for the centroid and the cells.
        shiftMapToOrigin();
    }

    private void readMap(List<String> mapLines) {
        for (int i = 0; i < mapLines.size(); i++) {
            String line = mapLines.get(i);
            if (line.startsWith("<path name=")) {
                readSquareOrHexLine(mapLines, i);
            }
        }
    }

    private void readSquareOrHexLine(List<String> mapLines, int i) {
        //get the label
        String line1 = mapLines.get(i);
        line1 = line1.substring(line1.indexOf("name=\"") + 6);
        String label = line1.substring(0, line1.indexOf("\""));

        //find the centroid of the cell
        double centerX;
        double centerY;

        //first check if it is a hex or a square
        String line5 = mapLines.get(i + 4);
        boolean square = line5.startsWith("h");

        if (square) {
            String line2 = mapLines.get(i + 1);
            String[] split = line2.split(" ");

            centerX = Double.parseDouble(split[0]) + 0.5;
            centerY = Double.parseDouble(split[1]) + 0.5;

        } else {//hex
            String line2 = mapLines.get(i + 1);
            String[] split2 = line2.split(" ");

            double x1 = Double.parseDouble(split2[0]);
            double y1 = Double.parseDouble(split2[1]);

            String[] split5 = line5.split(" ");
            double x2 = Double.parseDouble(split5[0]);
            double y2 = Double.parseDouble(split5[1]);

            //get the center of the hexagon
            centerX = x1 + (x2 - x1) / 2.0;
            centerY = y1 + (y2 - y1) / 2.0;
        }

        //add the square/hex to the mapping
        List<MosaicCell> list = inputCellMapping.getOrDefault(label, new ArrayList<>());
        list.add(new MosaicCell(centerX, centerY, label));
        inputCellMapping.put(label, list);

    }

//Depectracted as it results in visual artifacts. Can be used to generate gradients hexagonal maps.
//    private void readGradientLine(String line) {
//        
//        //remove everything before the label
//        line = line.substring(line.indexOf("label=\"") + 7);
//        String label = line.substring(0, line.indexOf("\""));
//        //remove everything before the position
//        line = line.substring(line.indexOf("pos=\"") + 5);
//        int x = (int) Double.parseDouble(line.substring(0, line.indexOf(" ")));
//        //remove x
//        line = line.substring(line.indexOf(" ") + 1);
//        int y = (int) Double.parseDouble(line.substring(0, line.indexOf("\"")));
//
//        List<Coordinate> list = inputCellMapping.getOrDefault(label, new ArrayList<>());
//        list.add(new Coordinate(x, y));
//        inputCellMapping.put(label, list);
//    }
    /**
     * Shifts both the centers and cell coordinates to the center.
     */
    private void shiftMapToOrigin() {

        //shift coordinate to origin
        //find minimum values.
        double minX = Integer.MAX_VALUE;
        double minY = Integer.MAX_VALUE;
        for (List<MosaicCell> coordList : inputCellMapping.values()) {
            for (MosaicCell coord : coordList) {
                minX = Math.min(minX, coord.x);
                minY = Math.min(minY, coord.y);
            }
        }
        //shift the values
        for (List<MosaicCell> coordList : inputCellMapping.values()) {
            for (MosaicCell coord : coordList) {
                coord.minus(minX, minY);
            }
        }
    }

    //get the width of the mosaic cartogram.
    public double getMosaicWidth() {
        double maxX = Double.MIN_VALUE;
        for (List<MosaicCell> coordList : inputCellMapping.values()) {
            for (MosaicCell coord : coordList) {
                maxX = Math.max(maxX, coord.x);
            }
        }
        return maxX;
    }

    //get the height of the mosaic cartogram.
    public double getMosaicHeight() {
        double maxY = Double.MIN_VALUE;
        for (List<MosaicCell> coordList : inputCellMapping.values()) {
            for (MosaicCell coord : coordList) {
                maxY = Math.max(maxY, coord.y);
            }
        }
        return maxY;
    }

    /**
     * returns all cells with the parent having label {@code key}
     *
     * @param key
     * @return
     */
    public List<MosaicCell> get(String key) {
        return inputCellMapping.get(key);
    }

    /**
     * Returns all parents labels.
     *
     * @return
     */
    public Iterable<String> getLabels() {
        return inputCellMapping.keySet();
    }

    /**
     * Returns all cells.
     *
     * @return
     */
    public List<MosaicCell> getAllMosaicCells() {
        List<MosaicCell> coordinates = new ArrayList();
        for (List<MosaicCell> cList : inputCellMapping.values()) {
            coordinates.addAll(cList);
        }
        return coordinates;
    }

}
