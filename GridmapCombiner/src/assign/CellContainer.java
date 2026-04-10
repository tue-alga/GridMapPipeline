/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assign;

import assign.Utility.IpeExporter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author msondag
 */
public class CellContainer {

    /**
     * All cells that are in the gridmap
     */
    ArrayList<Cell> cells = new ArrayList();

    /**
     * Generate the container by adding a cell for each MosaicCell.
     *
     * @param cellMap
     */
    public CellContainer(MosaicCellMap cellMap) {
        addCells(cellMap.getAllMosaicCells());
    }

    public String toIpe(boolean useHexTiles) {
        String ipeString = "";

        
        //Gradient is deprecated due to visual artifacts in rendering in pdf viewers.
//        //sets the gradient and assigns a symbol to each cell
//        String gradientPreamble = getGradientString(cellArray);
//        //set the preamble
//        String ipeString = IpeExporter.getPreamble(gradientPreamble);
        ipeString = IpeExporter.getPreambleNoStart();

        //groups the provinces together if present
        HashMap<String, List<Cell>> provinceGrouping = new HashMap();

        //20 for squares
        double cellSize = 1; //=20;
        for (Cell c : cells) {
            String province = c.province;
            List provinceList = provinceGrouping.getOrDefault(province, new ArrayList());
            provinceList.add(c);
            provinceGrouping.put(province, provinceList);
        }

        ipeString += "<page>\n";
        ipeString += "<layer name=\"text\"/>\n";
        String layers = "text ";
        for (String province : provinceGrouping.keySet()) {
            ipeString += "<layer name=\"" + province + "\"/>\n";
            layers += province + " ";
        }

        ipeString += "<view layers=\"" + layers + "\" active=\"noProvince\"/>\n";
        for (String province : provinceGrouping.keySet()) {
            for (Cell cell : provinceGrouping.get(province)) {
                double x = cell.mosaicCell.x;
                double y = cell.mosaicCell.y;
                if (useHexTiles) {
                    ipeString += IpeExporter.getHexagon(x, y, cellSize,
                                                        cell.label, cell.color, province);
                } else {
                    ipeString += IpeExporter.getRectangle(x * cellSize, y * cellSize, cellSize, cellSize,
                                                          cell.label, cell.color, province);
                }
            }
        }
//        //display each cell. gradient is deprecated.
//        for (Cell c : cellArray) {
//            ipeString += c.toGradientIpe() + "\n";
//        }
        ipeString += IpeExporter.endIpe();
        return ipeString;
    }

    private void addCells(List<MosaicCell> coordinates) {
        for (MosaicCell c : coordinates) {
            Cell cell = new Cell(c);
            cells.add(cell);
        }
    }

    /**
     * Returns a list of all cells belonging to one parition with label
     * {@code label}
     *
     * @param label
     * @return
     */
    public List<Cell> getCellsByLabel(String label) {
        ArrayList<Cell> labelCells = new ArrayList();
        for (Cell c : cells) {
            if (c.label.equals(label)) {
                labelCells.add(c);
            }
        }
        return labelCells;
    }
}
