package arrange.model.util.KML;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import arrange.model.subdivision.Map;
import arrange.model.util.Pair;

/**
 *
 * @author Max Sondag
 */
public class Raster {

    private int minX, maxX, minY, maxY;

    private int rasterWidth, rasterHeight;
    private double squareWidth, squareHeight;
    private RasterSquare[][] rasterSquare;
    private HashMap<String, RasterRegion> regions;

    public Raster(List<KMLPolygon> regions, List<KMLPoint> vornoiCenters, int rasterWidth, int rasterHeight) {

        this.rasterWidth = rasterWidth;
        this.rasterHeight = rasterHeight;

        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        //Find min and max values
        for (KMLPolygon region : regions) {
            for (Pair<Double, Double> coord : region.largestFace) {
                double x = coord.getFirst();
                double y = coord.getSecond();
                minX = Math.min(x, minX);
                maxX = Math.max(x, maxX);
                minY = Math.min(y, minY);
                maxY = Math.max(y, maxY);
            }
        }

        //initialize the size of the grid 
        initializeSize((int) Math.floor(minX), (int) Math.ceil(maxX), (int) Math.floor(minY), (int) Math.ceil(maxY));
        //Initialize the rastersquares according to the size of the grid
        initializeRaster();
        //Fill in the raster according to the regions
        if (vornoiCenters == null || vornoiCenters.isEmpty()) {
            fillRaster(regions);
            System.out.println("Raster filled in");
        } else {
            fillRasterVornoi(regions, vornoiCenters);
            System.out.println("vornoi calculated and raster filled in");
        }

        //get the rasterRegions
        List<String> names = new ArrayList();
        if (vornoiCenters == null || vornoiCenters.isEmpty()) {
            for (KMLPolygon region : regions) {
                names.add(region.name);
            }
        } else {
            for (KMLPoint vornoiCenter : vornoiCenters) {
                names.add(vornoiCenter.id);
            }
        }

        //Takes the largest region for each name and stores it. Removes the other regions per name
        fillRasterRegions(names);

        /*
         Filling holes makes sure that each region is a simple shape and does not have holes
         */
        fillHoles();
    }

    private void initializeSize(int minX, int maxX, int minY, int maxY) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }

    private void initializeRaster() {

        int totalWidth = maxX - minX;
        int totalHeight = maxY - minY;

        squareWidth = Math.ceil((double) totalWidth / rasterWidth);
        squareHeight = Math.ceil((double) totalHeight / rasterHeight);

        rasterSquare = new RasterSquare[rasterWidth][rasterHeight];
        for (int i = 0; i < rasterWidth; i++) {
            double x = minX + i * squareWidth;
            for (int j = 0; j < rasterHeight; j++) {
                double y = minY + j * squareHeight;
                RasterSquare s = new RasterSquare(x, y, squareWidth, squareHeight, i, j);
                rasterSquare[i][j] = s;
            }
        }
    }

    private void fillRaster(List<KMLPolygon> regions) {
        for (KMLPolygon region : regions) {
            Area area = region.area;
            String id = region.name;

            assignAreaToSquares(area, id);
        }
    }

    private void fillRasterVornoi(List<KMLPolygon> regions, List<KMLPoint> vornoiCenters) {
        //First fill all areas with "notNull" label so we know which points to label
        for (KMLPolygon region : regions) {
            Area area = region.area;
            String id = "notNull";
            assignAreaToSquares(area, id);
        }
        for (int i = 0; i < rasterWidth; i++) {
            for (int j = 0; j < rasterHeight; j++) {
                RasterSquare r = rasterSquare[i][j];
                if ("notNull".equals(r.id)) {
                    KMLPoint p = getClosestPoint(r, vornoiCenters);
                    r.id = p.id;
                }
            }
        }
    }

    /**
     * For each rectangle that intersect the area, the rectangle gets id
     * {@code id}
     *
     * @param area
     * @param id
     */
    private void assignAreaToSquares(Area area, String id) {
        Rectangle2D r = area.getBounds2D();

        //get the minimum and maximum i and j coordinates where the area is contained in
        int minI = (int) Math.floor(((r.getMinX() - minX) / squareWidth));
        int maxI = (int) Math.ceil(((r.getMaxX() - minX) / squareWidth));

        int minJ = (int) Math.floor(((r.getMinY() - minY) / squareHeight));
        int maxJ = (int) Math.ceil(((r.getMaxY() - minY) / squareHeight));

        for (int i = minI; i < maxI; i++) {
            for (int j = minJ; j < maxJ; j++) {
                RasterSquare s = rasterSquare[i][j];
                s.assignArea(area, id);
            }
        }
    }

    public HashMap<String, Area> getRasterAreas(List<String> ids) {
        HashMap<String, Area> areaMapping = new HashMap();
        for (String id : ids) {
            List<RasterSquare> largestRegion = getLargestRegionById(id);
            Area regionArea = regionToArea(largestRegion);
            areaMapping.put(id, regionArea);
        }
        return areaMapping;
    }

    private List<RasterSquare> getLargestRegionById(String id) {

        List<List<RasterSquare>> regions = getConnectedRegions(id);

        List<RasterSquare> largestRegion = new ArrayList();
        for (List<RasterSquare> region : regions) {
            if (region.size() > largestRegion.size()) {
                largestRegion = region;
            }
        }

        return largestRegion;
    }

    private List<RasterSquare> getRastersSquaresById(String id) {
        List<RasterSquare> squareList = new ArrayList();
        for (int i = 0; i < rasterWidth; i++) {
            for (int j = 0; j < rasterHeight; j++) {
                RasterSquare s = rasterSquare[i][j];
                if (id.equals(s.id)) {
                    squareList.add(s);
                }
            }
        }
        return squareList;
    }

    /**
     * Squares only contain the squares with the given id
     *
     * @param id
     * @param squares
     * @return
     */
    private List<List<RasterSquare>> getConnectedRegions(String id) {
        List<RasterSquare> squares = getRastersSquaresById(id);

        List<List<RasterSquare>> regions = new ArrayList();
        while (!squares.isEmpty()) {
            List<RasterSquare> region = new ArrayList();
            Stack<RasterSquare> unhandledSquares = new Stack();
            //take a initial square for the region
            RasterSquare initialSquare = squares.get(0);
            region.add(initialSquare);
            unhandledSquares.add(initialSquare);

            String regionId = id;

            //Use a bfs to search through the regions
            while (!unhandledSquares.isEmpty()) {
                RasterSquare currentSquare = unhandledSquares.pop();
                //find which new squares are adjacent to this square
                for (RasterSquare square : getAdjacentSquares(currentSquare)) {
                    //if the square was not already added to the region
                    if (regionId.equals(square.id) && !unhandledSquares.contains(square) && !region.contains(square)) {
                        region.add(square);
                        unhandledSquares.add(square);
                    }
                }
            }
            //Connected component is complete
            regions.add(region);
            //Remove all square in the region from the available squared, these are already handled
            squares.removeAll(region);
        }
        return regions;
    }

    /**
     * Gets all horizontal and vertical adjacent squares
     *
     * @param currentSquare
     * @return
     */
    public List<RasterSquare> getAdjacentSquares(RasterSquare currentSquare) {
        RasterSquare left = getSquareToLeft(currentSquare);
        RasterSquare right = getSquareToRight(currentSquare);
        RasterSquare top = getSquareAbove(currentSquare);
        RasterSquare bottom = getSquareBelow(currentSquare);

        List<RasterSquare> adjacentSquares = new ArrayList();
        if (left != null) {
            adjacentSquares.add(left);
        }
        if (right != null) {
            adjacentSquares.add(right);
        }
        if (top != null) {
            adjacentSquares.add(top);
        }
        if (bottom != null) {
            adjacentSquares.add(bottom);
        }

        return adjacentSquares;
    }

    /**
     * gets all diagonal adjacent squares
     *
     * @param currentSquare
     */
    public List<RasterSquare> getDiagonalAdjacentSquares(RasterSquare currentSquare) {
        int i = currentSquare.i;
        int j = currentSquare.j;
        List<RasterSquare> adjacentSquares = new ArrayList();
        RasterSquare leftTop, rightTop, leftBottom, rightBottom;
        if (i != 0 && j != 0) {
            leftTop = rasterSquare[i - 1][j - 1];
            adjacentSquares.add(leftTop);
        }
        if (i != (rasterWidth - 1) && j != 0) {
            rightTop = rasterSquare[i + 1][j - 1];
            adjacentSquares.add(rightTop);
        }
        if (i != 0 && j != (rasterHeight - 1)) {
            leftBottom = rasterSquare[i - 1][j + 1];
            adjacentSquares.add(leftBottom);
        }
        if (i != (rasterWidth - 1) && j != (rasterHeight - 1)) {
            rightBottom = rasterSquare[i + 1][j + 1];
            adjacentSquares.add(rightBottom);
        }
        return adjacentSquares;
    }

    public List<RasterSquare> getAllAdjacentSquares(RasterSquare currentSquare) {
        ArrayList<RasterSquare> adjacentSquares = new ArrayList();
        adjacentSquares.addAll(getAdjacentSquares(currentSquare));
        adjacentSquares.addAll(getDiagonalAdjacentSquares(currentSquare));
        return adjacentSquares;
    }

    public Area regionToArea(List<RasterSquare> region) {
        Area regionArea = new Area();
        for (RasterSquare rs : region) {
            Rectangle2D.Double rectangle = new Rectangle2D.Double(rs.x, rs.y, rs.width, rs.height);
            Area squareArea = new Area(rectangle);
            regionArea.add(squareArea);
        }
        return regionArea;
    }

    public RasterRegion getRasterRegion(String name) {
        return regions.get(name);
    }

    /**
     * Fills in the rasterregions. Each region is the largest connected region
     * of rasterSquares. Removes the other regions
     */
    private void fillRasterRegions(List<String> names) {
        regions = new HashMap();

        for (String name : names) {
            List<RasterSquare> largestRegionById = getLargestRegionById(name);
            RasterRegion region = new RasterRegion(name, largestRegionById, this);
            regions.put(name, region);
            //remove the smaller regions
            List<RasterSquare> smallerRegions = getRastersSquaresById(name);
            smallerRegions.removeAll(largestRegionById);
            for (RasterSquare r : smallerRegions) {
                r.id = null;
            }

        }
    }

    public RasterSquare getSquareToLeft(RasterSquare r) {
        int i = r.i;
        int j = r.j;
        if (i != 0) {
            return rasterSquare[i - 1][j];
        }
        return null;

    }

    public RasterSquare getSquareBelow(RasterSquare r) {
        int i = r.i;
        int j = r.j;
        if (j != (rasterHeight - 1)) {
            return rasterSquare[i][j + 1];

        }
        return null;
    }

    public RasterSquare getSquareToRight(RasterSquare r) {
        int i = r.i;
        int j = r.j;
        if (i != (rasterWidth - 1)) {
            return rasterSquare[i + 1][j];

        }
        return null;

    }

    public RasterSquare getSquareAbove(RasterSquare r) {
        int i = r.i;
        int j = r.j;
        if (j != 0) {
            return rasterSquare[i][j - 1];
        }
        return null;
    }

    /**
     * Fill all holes in the graph. Holes are defined as a connected region of
     * Rastersquares with id equal to null that is not adjacent to the edge.
     */
    private void fillHoles() {
        System.out.println("start filling holes");
        ArrayList<RasterSquare> nullSquares = getAllNullSquares();
        //Get all components of null cells that are connected to each other
        ArrayList<ArrayList<RasterSquare>> components = getComponents(nullSquares);
        System.out.println("all components found");
        System.out.println("components.size() = " + components.size());
        for (ArrayList<RasterSquare> component : components) {
            //if a component is connected to edge it is not a hole
            if (!connectedToEdge(component)) {
                //it is a hole
                fillHole(component);
            }
        }
        System.out.println("All components filled");
    }

    /**
     * Returns all squares with id = null
     *
     * @return
     */
    private ArrayList<RasterSquare> getAllNullSquares() {
        ArrayList<RasterSquare> nullSquares = new ArrayList();
        for (int i = 1; i < rasterWidth - 1; i++) {
            for (int j = 1; j < rasterHeight - 1; j++) {
                RasterSquare r = rasterSquare[i][j];
                if (r.id == null) {
                    nullSquares.add(r);
                }
            }
        }
        return nullSquares;
    }

    /**
     * Performs repeated breadth first searches to return all connected regions
     * of null squares. nullSquares will become empt
     *
     * @param nullSquares
     * @return
     */
    private ArrayList<ArrayList<RasterSquare>> getComponents(ArrayList<RasterSquare> nullSquares) {
        ArrayList<ArrayList<RasterSquare>> components = new ArrayList();
        while (!nullSquares.isEmpty()) {
            ArrayList<RasterSquare> component = getComponentFromSquare(nullSquares.get(0));
            components.add(component);
            nullSquares.removeAll(component);
        }
        return components;
    }

    /**
     * Performs a bfs to return the component formed by all null-squares that
     * are (in)directly connected to initialSquare through null-squares.
     * Modifies nullSquares by removing all squares in the component
     *
     * @param initialSquare
     * @param nullSquares
     * @return
     */
    private ArrayList<RasterSquare> getComponentFromSquare(RasterSquare initialSquare) {
        ArrayList<RasterSquare> processedSquares = new ArrayList();
        Stack<RasterSquare> unprocessedSquares = new Stack();

        unprocessedSquares.push(initialSquare);

        while (!unprocessedSquares.isEmpty()) {
            RasterSquare currentSquare = unprocessedSquares.pop();
            List<RasterSquare> adjacentSquares = getAdjacentSquares(currentSquare);
            for (RasterSquare r : adjacentSquares) {
                if (r.id == null && !unprocessedSquares.contains(r) && !processedSquares.contains(r)) {
                    unprocessedSquares.add(r);
                }
            }
            processedSquares.add(currentSquare);
        }
        return processedSquares;
    }

    /**
     * Returns if this component is connected to the edge of the grid
     *
     * @param component
     * @return
     */
    private boolean connectedToEdge(ArrayList<RasterSquare> component) {
        for (RasterSquare r : component) {
            if (r.i == 0 || r.i == (rasterWidth - 1) || r.j == 0 || r.j == (rasterHeight - 1)) {
                //it is connected to the edge
                return true;
            }
        }
        return false;
    }

    /**
     * The component is the hole of null cells. It will be added to the adjacent
     * regions.
     *
     * @param component
     */
    private void fillHole(ArrayList<RasterSquare> component) {
        //Discrete vornoi can give rise to unconnected components. Will thus not be implemented
        //Instead we will find a cell on the edge of the hole and assign it to an adjacent region.
        //We will keep finding cells and assigning them a neighbour based on a BFS. We are using a BFS
        //as this will prevent long stretched areas of a region.

        //We assign regions based on how often we already assigned a nul square to this region.
        //The more often it is assigned, the lower the prioritiy. This prevents long stretches
        //and keeps the region mostly in the same shape
        if (component.isEmpty()) {
            return;
        }
        //holds how often each id has been assigned
        HashMap<String, Integer> idsAssigned = new HashMap();
        Set<RasterSquare> processedCells = new HashSet();

        Queue<RasterSquare> unprocessedCells = new LinkedList();
        RasterSquare initialSquare = getCellAdjacentToRegion(component);
        unprocessedCells.add(initialSquare);

        while (!unprocessedCells.isEmpty()) {
            //assign the id from the neighbours
            RasterSquare r = unprocessedCells.poll();
            List<RasterSquare> adjacentSquares = getAdjacentSquares(r);

            String newId = assignNewId(adjacentSquares, idsAssigned);
            r.id = newId;
            if (newId == null) {
                System.err.println("Bfs for filling holes failed");
            }

            //update the bfs
            processedCells.add(r);

            for (RasterSquare adjacentSquare : adjacentSquares) {
                if (!processedCells.contains(adjacentSquare) && !unprocessedCells.contains(adjacentSquare) && adjacentSquare.id == null) {
                    unprocessedCells.add(adjacentSquare);
                }
            }
        }
    }

    /**
     * Returns a cell that is at the edge from the cells in region
     *
     * @param region
     * @return
     */
    private RasterSquare getCellAdjacentToRegion(ArrayList<RasterSquare> region) {
        for (RasterSquare r : region) {
            List<RasterSquare> adjacentSquares = getAdjacentSquares(r);
            for (RasterSquare adjacentR : adjacentSquares) {
                if (adjacentR.id != null) {
                    return r;
                }
            }
        }
        return null;
    }

    /**
     * Returns the id that should be assigned according to the adjacent squares
     * and the idsAssigned hashmap. The id that is assigned will by the id
     * present in adjacentSquares that has the lowest value in idsAssigned
     *
     * @param adjacentSquares
     * @param idsAssigned
     * @return
     */
    private String assignNewId(List<RasterSquare> adjacentSquares, HashMap<String, Integer> idsAssigned) {
        String bestId = null;
        int minAssigned = Integer.MAX_VALUE;
        for (RasterSquare r : adjacentSquares) {
            String id = r.id;
            if (id == null) {
                continue;
            }
            int count = 0;
            if (idsAssigned.containsKey(id)) {
                count = idsAssigned.get(id);
            }
            if (count < minAssigned) {
                bestId = id;
                minAssigned = count;
            }
        }
        return bestId;
    }

    private KMLPoint getClosestPoint(RasterSquare r, List<KMLPoint> vornoiCenters) {
        double x = r.x + r.width / 2;
        double y = r.y + r.height / 2;

        double bestDist = Double.MAX_VALUE;
        KMLPoint bestPoint = null;
        for (KMLPoint p : vornoiCenters) {
            double squaredDistance = Math.pow(x - p.x, 2) + Math.pow(y - p.y, 2);
            if (squaredDistance < bestDist) {
                bestDist = squaredDistance;
                bestPoint = p;
            }
        }
        return bestPoint;
    }

}
