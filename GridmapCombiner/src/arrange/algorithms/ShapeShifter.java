package arrange.algorithms;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import arrange.model.HexagonalMap;
import arrange.model.Cartogram.MosaicCartogram;
import arrange.model.Cartogram.MosaicCartogram.Cell;
import arrange.model.Cartogram.MosaicCartogram.Coordinate;
import arrange.model.Network.Vertex;
import arrange.model.util.Pair;

/**
 *
 * @author Max Sondag
 */
public class ShapeShifter {

    private MosaicCartogram currentGrid;
    //stores the last guiding shape properties
    private MosaicCartogram.CellRegion lastGuidingShape = null;
    private Set<Coordinate> lastCoordinateSet = null;
    //Stores the original values of the changed cells
    private HashMap<Coordinate, Vertex> changedRegions = new HashMap();

    public ShapeShifter(MosaicCartogram currentGrid) {
        this.currentGrid = currentGrid;

    }

    /**
     * Shifts the guiding shapes
     * @param region
     * @param guidingShape
     * @param direction
     * @return 
     */
    public void performShapeShift(MosaicCartogram.MosaicRegion region, MosaicCartogram.CellRegion guidingShape, int direction) {
        //-1:no direction, 0:right,1:rightTop,2:leftTop,3:left,4:leftBottom,5:rightBottom 
        lastGuidingShape = guidingShape;
        lastCoordinateSet = guidingShape.coordinateSet();
        shiftGuidingShape(guidingShape, direction);
    }
    
    public void undoLastShift() {
        //undo the moving of the guiding shape
        lastGuidingShape.setCoordinates(lastCoordinateSet);
    }

    private void shiftGuidingShape(MosaicCartogram.CellRegion guidingShape, int direction) {
        //-1:no direction, 0:right,1:rightTop,2:leftTop,3:left,4:leftBottom,5:rightBottom 
        int xIncrease = 0;
        int yIncrease = 0;
        int zIncrease = 0;
        switch (direction) {
            case 0:
                xIncrease = 1;
                break;
            case 1:
                yIncrease = -1;
                break;
            case 2:
                zIncrease = 1;
                break;
            case 3:
                xIncrease = -1;
                break;
            case 4:
                yIncrease = 1;
                break;
            case 5:
                zIncrease = -1;
                break;
        }
        guidingShape.translate(new HexagonalMap.BarycentricCoordinate(xIncrease, yIncrease, zIncrease));
    }
}
