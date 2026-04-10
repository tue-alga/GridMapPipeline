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
public class CellShifter {

    private MosaicCartogram currentGrid;
    
    public CellShifter(MosaicCartogram currentGrid) {
        this.currentGrid = currentGrid;

    }

    /**
     * Shifts the cells to optimize the quality using the guiding shape
     * @param region
     * @param guidingShape 
     */
    public void performCellShift(MosaicCartogram.MosaicRegion region, MosaicCartogram.CellRegion guidingShape) {
        //-1:no direction, 0:right,1:rightTop,2:leftTop,3:left,4:leftBottom,5:rightBottom 
        updateCoordinates(region, guidingShape);

    }

    private void updateCoordinates(MosaicCartogram.MosaicRegion region, MosaicCartogram.CellRegion guidingShape) {

        //Gets all the coordinates inside the guidingshape and the region
        List<Coordinate> candidates = getCandidates(region, guidingShape);

        //for each Coordinate, check whether it should stay the same or if it should be added to a different cell
        boolean done = false;
        while (!done) {
            done = true;
            for (Coordinate c : candidates) {
                Vertex oldVertex = currentGrid.getVertex(c);
                //check if removing coordinate c from the region improves the quality
                //Or if adding c to a neighbouring region improves the quality
                if ((oldVertex != null && removeCoordinate(c)) || addCoordinate(c)) {
                    //an improvement could be made, so we continue
                    done = false;
                }
            }
        }
    }

    private List<Coordinate> getCandidates(MosaicCartogram.MosaicRegion region, MosaicCartogram.CellRegion guidingShape) {
        List<Coordinate> candidates = new LinkedList();
        //Add all those inside the guiding shape.
        candidates.addAll(guidingShape.coordinateSet());
        //Add all those that occupy the region associated to this guiding shape
        candidates.addAll(region.coordinateSet());
        return candidates;
    }

    /**
     * Removes the coordinate from the region if this improves the quality while
     * maintaining the connectivity
     *
     * @param c
     * @return
     */
    private boolean removeCoordinate(Coordinate c) {
        Vertex currentVertex = currentGrid.getVertex(c);
        if (currentVertex == null) {
            System.out.println("warning, tried to remove a coordinate from a non-existent region");
            return false;
        }
        MosaicCartogram.MosaicRegion currentRegion = currentGrid.getRegion(c);

        //remove it and store the qualities
        Pair<Double, Double> currentQuality = currentGrid.getGridQualityPair();
        currentGrid.removeCell(c);
        Pair<Double, Double> newQuality = currentGrid.getGridQualityPair();

        if (newQuality.compareTo(currentQuality) == -1) {
            //it improved the quality, test the connectivity as well
            if (currentRegion.isValid()) {
                //still connected, so removing this is good
                return true;
            }
        }

        //Undo the removal as it either got worse or it was invalid
        currentGrid.setVertex(c, currentVertex);

        return false;
    }

    /**
     * Adds the coordinate to an adjacent region if this improves the quality
     * while maintaining the connectivity
     *
     * @param c
     * @return
     */
    private boolean addCoordinate(Coordinate c) {
        Vertex currentVertex = currentGrid.getVertex(c);

        //possible vertices are all adjacent vertices
        Set<Vertex> possibleVertices = new HashSet();
        for (Coordinate neighbour : c.neighbours()) {
            Vertex vertex = currentGrid.getVertex(neighbour);
            //don't allow null regions
            if (vertex != null) {
                possibleVertices.add(vertex);
            }
        }

        //don't allow additions to self
        possibleVertices.remove(currentVertex);

        //determine the best region to add it to
        Vertex bestVertex = currentVertex;
        Pair<Double, Double> bestQuality = currentGrid.getGridQualityPair();

        for (Vertex targetVertex : possibleVertices) {
            MosaicCartogram.MosaicRegion currentRegion = currentGrid.getRegion(c);

            currentGrid.removeCell(c);
            currentGrid.setVertex(c, targetVertex);

            MosaicCartogram.MosaicRegion targetRegion = currentGrid.getRegion(c);

            Pair<Double, Double> newQuality = currentGrid.getGridQualityPair();
            if (newQuality.compareTo(bestQuality) == -1) {
                {
                    //improved
                    if (targetRegion.isValid() && (currentRegion == null || currentRegion.isValid())) {
                        bestVertex = targetVertex;
                        bestQuality = newQuality;
                        continue;
                    }
                }
            }
            //not improved or not valid
            currentGrid.removeCell(c);
            if (bestVertex != null) {
                currentGrid.setVertex(c, bestVertex);
            }

        }

        //coordinate is now in the best region
        if (bestVertex != currentVertex) {
            return true;
        } else {
            //no improvement
            return false;
        }
    }
}
