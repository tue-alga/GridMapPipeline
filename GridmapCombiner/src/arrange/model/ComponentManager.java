package arrange.model;

import arrange.model.Cartogram.MosaicCartogram;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import arrange.Utils.Utils;
import arrange.algorithms.ExperimentLog;
import java.util.Collection;
import java.util.HashSet;
import arrange.model.Cartogram.MosaicCartogram.Cell;
import arrange.model.HexagonalMap.BarycentricCoordinate;
import arrange.model.SquareMap.EuclideanCoordinate;
import arrange.model.Cartogram.MosaicCartogram.Coordinate;
import arrange.model.Cartogram.MosaicCartogram.MosaicRegion;
import arrange.model.graph.ConnectedComponents;
import arrange.model.graph.CrossingFinder;
import arrange.model.graph.Digraph;
import arrange.model.graph.StronglyConnectedComponents;
import arrange.model.subdivision.Map;
import arrange.model.subdivision.Map.Face;
import arrange.model.util.ElementList;
import arrange.model.util.Identifier;
import arrange.model.util.Matrix;
import arrange.model.util.Pair;
import arrange.model.util.Vector2D;
import arrange.parameter.ParameterManager.Application.GridType;
import integration.Stopwatch;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class ComponentManager {

    private final Map map;
    private final GridType gridType;
    private final Network weakDual;
    private final ArrayList<Component> components;
    private final ElementList<Integer> faceToComponent;
    private final int guidingShapeSamples;
    private Double unitData = null;

    public ComponentManager(Map map, GridType type, double unitData, int guidingShapeSamples) {
        this.map = map;
        this.gridType = type;
        this.unitData = unitData;
        this.weakDual = new Network();
        map.computeWeakDual(weakDual);

        for (int i = 0; i < map.numberOfBoundedFaces(); i++) {
            Map.Face f = map.getFace(i);
            weakDual.getVertex(i).setColor(f.getColor());
        }
        this.components = new ArrayList<>();
        this.faceToComponent = new ElementList<>(map.numberOfBoundedFaces(), null);
        this.guidingShapeSamples = guidingShapeSamples;
        initialize();
    }

    public void updateUnitData(double unitData) {
        this.unitData = unitData;
        for (Component component : components) {
            MosaicCartogram componentCartogram = component.getCartogram();
            if (componentCartogram != null) {
                componentCartogram.computeDesiredRegions(unitData, guidingShapeSamples, true);
            }
        }
    }

    public Map getMap() {
        return map;
    }

    public Network getWeakDual() {
        return weakDual;
    }

    public int numberOfComponents() {
        return components.size();
    }

    public Component getComponent(int id) {
        return components.get(id);
    }

    public Iterable<Component> components() {
        return components;
    }

//    public void initializeComponentsFromFile(String fileName) {
//        for (Component component : components) {
//            Map componentMap = component.getMap();
//            Network componentWeakDual = component.getWeakDual();
//            MosaicCartogram componentCartogram = createCartogram(componentMap, componentWeakDual);
//            componentCartogram.computeDesiredRegions(unitData);
//            componentCartogram.importCoordinates(fileName);
//            component.setCartogram(componentCartogram);
//        }
//    }
    public void initializeComponentsFromFile(String fileName) {
        MosaicCartogram cartogram = createCartogram(map, weakDual);
        cartogram.computeDesiredRegions(unitData, guidingShapeSamples, true);
        cartogram.importCoordinates(fileName);
        for (Component component : components) {
            Map componentMap = component.getMap();
            Network componentWeakDual = component.getWeakDual();
            MosaicCartogram componentCartogram = createCartogram(componentMap, componentWeakDual);
            componentCartogram.computeDesiredRegions(unitData, guidingShapeSamples, true);
            for (Map.Face face : componentMap.boundedFaces()) {
                Map.Face originalFace = component.getOriginalFace(face);
                MosaicRegion region = cartogram.getRegion(originalFace.getId());
                MosaicRegion componentRegion = componentCartogram.getRegion(face.getId());
                Coordinate t = region.getGuidingShapeTranslation();
                componentRegion.translateGuidingShape(t);
                Network.Vertex v = componentRegion.getVertex();
                for (Coordinate c : region) {
                    componentCartogram.setVertex(c, v);
                }
            }
            component.setCartogram(componentCartogram);
        }
    }

    public void initializeComponentsFromEmbedding() {
        
        Stopwatch sw = Stopwatch.get("- embedder");
            
        for (Component component : components) {
//            if (component.getMap().getFace("GRC") == null) {
//                continue;
//            }

            Map componentMap = component.getMap();
            Network componentWeakDual = component.getWeakDual();             
            
            sw.start();
            GridEmbedder embedder = new GridEmbedder(componentMap, componentWeakDual);
            embedder.computeOrderlySpanningTreeSchnyder();
            embedder.computeHeights();            
            MosaicCartogram componentCartogram = createCartogram(componentMap, componentWeakDual);
            sw.stop();
            
            componentCartogram.computeDesiredRegions(unitData, guidingShapeSamples, true);
            
            sw.start();
            embedder.initializeCartogram(componentCartogram);
            component.setCartogram(componentCartogram);
            sw.stop();
        }
        
        
    }

    public Component getLargestComponent(StronglyConnectedComponents<Digraph.Vertex, Digraph.Edge> scc) {
        int largestSize = 0;
        Component largestComponent = null;

        for (Set<Digraph.Vertex> graphComponent : scc.components()) {
            for (Digraph.Vertex v : graphComponent) {
                Component component = components.get(v.getId());
                int cellAmount = component.componentCartogram.numberOfCells();
                if (cellAmount > largestSize) {
                    largestSize = cellAmount;
                    largestComponent = component;
                }
            }
        }
        return largestComponent;
    }

    public List<Set<Digraph.Vertex>> sortComponents(StronglyConnectedComponents<Digraph.Vertex, Digraph.Edge> scc) {
        //sort the components based on the distance to the largest component in the list
        int largestSize = 0;
        Set<Digraph.Vertex> largestSet = null;
        Component largestComponent = null;

        for (Set<Digraph.Vertex> graphComponent : scc.components()) {
            for (Digraph.Vertex v : graphComponent) {
                Component component = components.get(v.getId());
                int cellAmount = component.componentCartogram.numberOfCells();
                if (cellAmount > largestSize) {
                    largestSize = cellAmount;
                    largestSet = graphComponent;
                    largestComponent = component;
                }
            }
        }

        HashMap<Set<Digraph.Vertex>, Double> distanceList = new HashMap();

        for (Set<Digraph.Vertex> graphComponent : scc.components()) {
            if (graphComponent.equals(largestSet)) {
                continue;
            }
            for (Digraph.Vertex v : graphComponent) {

                Component component = components.get(v.getId());

                Vector2D cMiddle = largestComponent.componentMap.getAverageCentroid();
                Vector2D cCurrent = component.componentMap.getAverageCentroid();
                distanceList.put(graphComponent, Vector2D.difference(cCurrent, cMiddle).norm());
                break;
            }
        }
        //simple insertion sort
        List<Set<Digraph.Vertex>> sortedComponentsList = new ArrayList();
        sortedComponentsList.add(largestSet);

        while (!distanceList.isEmpty()) {
            Set<Digraph.Vertex> bestSet = null;
            double bestDistance = Double.MAX_VALUE;

            for (Set<Digraph.Vertex> graphComponent : scc.components()) {
                if (!distanceList.containsKey(graphComponent)) {
                    continue;
                }
                for (Digraph.Vertex v : graphComponent) {
                    Component component = components.get(v.getId());

                    Vector2D cMiddle = largestComponent.componentMap.getAverageCentroid();
                    Vector2D cCurrent = component.componentMap.getAverageCentroid();
                    double distance = Vector2D.difference(cCurrent, cMiddle).norm();
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestSet = graphComponent;
                    }
                    break;
                }
            }
            distanceList.remove(bestSet);
            sortedComponentsList.add(bestSet);
        }

        return sortedComponentsList;
    }

    public MosaicCartogram mergeCartograms() {
       // System.out.println("Merging cartograms");
        //Merges the different componoents of the cartogram in a single view.
        //TODO: reimplement to maintain directions between the components
        MosaicCartogram cartogram = createCartogram(map, weakDual);
        cartogram.computeDesiredRegions(unitData, guidingShapeSamples, false);
        Digraph precedenceGraph = computePrecedenceGraph();
        StronglyConnectedComponents<Digraph.Vertex, Digraph.Edge> scc = new StronglyConnectedComponents<>(precedenceGraph);

        //sort the connected components by distance to largest component
        List<Set<Digraph.Vertex>> sortedComponentsList = sortComponents(scc);

        Component largestComponent = getLargestComponent(scc);

        List<Component> placedComponents = new ArrayList();

        //place the initial component
        placedComponents.add(largestComponent);

        //offset is calculated, move the region in the correct position
        for (MosaicRegion region : largestComponent.getCartogram().regions()) {
            Map.Face originalFace = largestComponent.getOriginalFace(region.getMapFace());
            Network.Vertex originalVertex = weakDual.getVertex(originalFace.getId());
            for (Coordinate c : region) {
                cartogram.setVertex(c, originalVertex);
            }
        }

        for (Set<Digraph.Vertex> graphComponent : sortedComponentsList) {
            //each graphcomponents is a disconnected configuration
            for (Digraph.Vertex v : graphComponent) {//go through everything that is connected
                //get the component and the cartogram
                Component component = components.get(v.getId());
                if (component == largestComponent) {
                    //already placed
                    continue;
                }
                MosaicCartogram componentCartogram = component.getCartogram();

                //Determine where to place this component such that it doesn't overlap and is roughly in the right place.
                Coordinate offSet = getBestOffset(component, placedComponents, cartogram, componentCartogram);

                placedComponents.add(component);

                //offset is calculated, move the region in the correct position
                for (MosaicRegion region : componentCartogram.regions()) {
                    Map.Face originalFace = component.getOriginalFace(region.getMapFace());
                    Network.Vertex originalVertex = weakDual.getVertex(originalFace.getId());

                    //update both the region and the cartogram now that we know the offset.
                    //updating region is required for placing next regions.
                    Set<Coordinate> coordinates = new HashSet();
                    for (Coordinate c : region) {
                        c = c.plus(offSet);
                        cartogram.setVertex(c, originalVertex);
                        coordinates.add(c);
                    }
                    region.setCoordinates(coordinates);
                }
            }
        }

        //move the guiding shapes in the correct position
        for (MosaicRegion region : cartogram.regions()) {
            region.computeBestOverlay();
        }
        return cartogram;
    }

    /**
     *         //Find the coordinate of the cell c1 in any component of
     * placedComponents that was closest to any cell c2 of this component
     * //and the angle between c1 and b2. This gives an offset an a target
     * angle
     *
     * @param placedComponent
     * @param curComponent
     * @return
     */
    private Pair<Coordinate, Double> getClosestCoordinateAndAngle(List<Component> placedComponent, Component curComponent) {

        double bestLength = Double.MAX_VALUE;
        Pair<Vector2D, Vector2D> bestCentroids = null;//holds the distance between the two closest faces between curComponent and placedComponent
        Coordinate bestCoordinate = null;//holds the coordinate of a cell from the closest face in placedComponent.

        //face is from the input map. A region can have multiple faces, one for each region defined in the input map
        for (Face curF : curComponent.originalFaces) {
            Vector2D curCentroid = curF.getCentroid();
            for (Component c : placedComponent) {
                for (Face placedF : c.originalFaces) {
                    Vector2D placedCentroid = placedF.getCentroid();

                    Vector2D difference = Vector2D.difference(placedCentroid, curCentroid);
                    double squaredLength = difference.getX() * difference.getX() + difference.getY() * difference.getY();
                    if (squaredLength < bestLength) {
                        bestLength = squaredLength;
                        bestCentroids = new Pair(curCentroid, placedCentroid);

                        //find the region belonging to this face
                        MosaicRegion region = null;
                        for (MosaicRegion r : c.getCartogram().regions()) {
                            if (placedF.getLabel().getText().equals(r.getMapFace().getLabel().getText())) {
                                region = r;
                            }
                        }
                        bestCoordinate = region.barycenter();
                    }
                }
            }
        }
        //bestCentroids now holds the closest pair
        Vector2D dirVector = Vector2D.difference(bestCentroids.getFirst(), bestCentroids.getSecond());
        double angle = getAngleOffset(dirVector);

        return new Pair(bestCoordinate, angle);
//        Vector2D cMiddle = placedComponent.get(0).componentMap.getAverageCentroid();
//        Vector2D cCurrent = curComponent.componentMap.getAverageCentroid();
//        Vector2D dirVector = Vector2D.difference(cCurrent, cMiddle);
//        for (Cell c1 : placedComponent.get(0).componentCartogram.cells()) {
//            Coordinate coordinate = c1.getCoordinate();
//            return new Pair(coordinate, getAngleOffset(dirVector));
//        }
//        return null;
    }

    public ExperimentLog mergeLogs() {
        if (components.isEmpty()) {
            return new ExperimentLog();
        }
        ExperimentLog merged = components.get(0).getLog();
        for (int i = 1; i < components.size(); i++) {
            Component component = components.get(i);
            merged = ExperimentLog.mergeLogs(merged, component.getLog());
        }
        return merged;
    }

    private void initialize() {
        ConnectedComponents<Network.Vertex, Network.Edge> cc = new ConnectedComponents<>(weakDual);
        int currentId = 0;
        for (Set<Network.Vertex> comp : cc.components()) {
            ElementList<Map.Face> compFaces = new ElementList<>();
            for (Network.Vertex v : comp) {
                Map.Face f = map.getFace(v.getId());
                compFaces.add(f);
                faceToComponent.set(f, currentId);
            }
            Component component = new Component(currentId, compFaces);
            components.add(component);
            currentId++;
        }
    }

    private MosaicCartogram createCartogram(Map map, Network weakDual) {
        MosaicCartogram componentCartogram;
        switch (gridType) {
            case HEXAGONAL:
                componentCartogram = new HexagonalMap(map, weakDual);
                break;
            case SQUARE:
                componentCartogram = new SquareMap(map, weakDual);
                break;
            default:
                throw new RuntimeException("unexpected grid type");
        }
        return componentCartogram;
    }

    private Digraph computePrecedenceGraph() {
        int n = components.size();
        Matrix<Boolean> adjacencyMatrix = new Matrix<>(n, n, false);
        Map.Vertex leftmost = Utils.leftmost(map.vertices());
        double minX = leftmost.getPosition().getX() - 10;
        for (Component c1 : components) {
            LOOP:
            for (Component c2 : components) {
                if (c1 != c2) {
                    for (Map.Vertex v : c1.getMap().vertices()) {
                        Vector2D p0 = v.getPosition();
                        for (Map.Halfedge h : c2.getMap().halfedges()) {
                            Vector2D p1 = new Vector2D(minX, p0.getY());
                            Vector2D q0 = h.getSource().getPosition();
                            Vector2D q1 = h.getTarget().getPosition();
                            Vector2D intersection = Utils.lineSegmentIntersection(p0, p1, q0, q1);
                            if (intersection != null) {
                                adjacencyMatrix.set(c1, c2, true);
                                continue LOOP;
                            }
                        }
                    }
                }
            }
        }
        Digraph g = new Digraph(n);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (adjacencyMatrix.get(i, j)) {
                    g.addEdge(i, j);
                }
            }
        }
        return g;
    }

    private boolean hasConflict(MosaicCartogram cartogram, Coordinate offset, Iterable<Coordinate> coordinates) {
        for (Coordinate c : coordinates) {
            Coordinate offSettedC = c.plus(offset);
            if (cartogram.getVertex(offSettedC) != null) {
                //the cartogram on this spot is already filled
                return true;
            } else {
                for (Coordinate d : offSettedC.neighbours()) {
                    if (cartogram.getVertex(d) != null) {
                        //one of the neighbours of c is already filled.
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasConflict(MosaicCartogram cartogram, ArrayList<Coordinate> coordinates) {
        for (Coordinate c : coordinates) {
            if (cartogram.getVertex(c) != null) {
                //the cartogram on this spot is already filled
                return true;
            } else {
                for (Coordinate d : c.neighbours()) {
                    if (cartogram.getVertex(d) != null) {
                        //one of the neighbours of c is already filled.
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private double getAngleOffset(Vector2D dirVector) {
        return Math.toDegrees(Math.atan2(dirVector.getY(), dirVector.getX()));
    }

    /**
     * Determines the offset required to get the totaloffset as close as
     * possible within the desired halfplane.
     *
     * @param targetAngle
     * @param nullCoordinate
     * @param invalid        set of invalid candidates.
     * @return
     */
    private Coordinate getNextOffSet(double targetAngle, Coordinate nullCoordinate, Set<Coordinate> invalid) {

        int radius = 0;
        while (true) {
            //increase the radius every loop, considering only the coordinates on the ring
            radius++;
            Coordinate[] ring = nullCoordinate.ring(radius);

            Coordinate bestC = null;
            double bestAngle = 360;

            for (Coordinate c : ring) {
                if (invalid.contains(c)) {
                    continue;
                }
                //discard angles that are to large
                double angle = getAngleOffset(c.toVector2D());
                double angleDiff = angleDistance(angle, targetAngle);
                if (angleDiff > 90) {
                    continue;
                }
                //if this one has a better angle, use it.
                if (angleDiff < bestAngle) {
                    bestAngle = angleDiff;
                    bestC = c;
                }
            }

            if (bestC != null) {
                //found a candidate
                return bestC;
            }
        }
    }

    /**
     * Length (angular) of a shortest way between two angles. It will be in
     * range [0, 180].
     */
    private double angleDistance(double alpha, double beta) {
        double phi = Math.abs(beta - alpha) % 360;       // This is either the distance or 360 - distance
        double distance = phi > 180 ? 360 - phi : phi;
        return distance;
    }

    private Coordinate getCentroid(Iterable<MosaicRegion> regions) {

        Class<? extends Coordinate> coordinateClass = null;
        for (MosaicRegion r : regions) {
            coordinateClass = r.barycenter().getClass();
            break;
        }
        Coordinate centroid = null;

        if (coordinateClass == BarycentricCoordinate.class) {
            centroid = new BarycentricCoordinate(0, 0, 0);
        } else if (coordinateClass == EuclideanCoordinate.class) {
            centroid = new EuclideanCoordinate(0, 0);
        }

        double count = 0;
        for (MosaicRegion r : regions) {
            Coordinate barycenter = r.barycenter();
            centroid = centroid.plus(barycenter);
            count++;
        }
        double div = 1 / count;
        centroid = centroid.times(div);
//        int x = (int) Math.ceil(((double) centroid.getX()) * div);
//        int y = (int) Math.ceil(((double) centroid.getY()) * div);
//        int z = (int) Math.ceil(((double) centroid.getZ()) * div);
//        centroid = new BarycentricCoordinate(x, y, z);
        return centroid;
    }

    private Coordinate getNullCoordinate(MosaicCartogram componentCartogram) {
        ArrayList<Coordinate> componentCoordinates = new ArrayList<>(componentCartogram.numberOfCells());
        for (Coordinate c : componentCartogram.coordinates()) {
            componentCoordinates.add(c);
        }

        if (componentCoordinates.get(0).getClass() == BarycentricCoordinate.class) {
            return new BarycentricCoordinate(0, 0, 0);
        } else if (componentCoordinates.get(0).getClass() == EuclideanCoordinate.class) {
            return new EuclideanCoordinate(0, 0);
        }
        System.err.println("Need new null offset, not yet implemented");
        return null;
    }

    private Coordinate getNullCoordinate(Coordinate c) {
        if (c.getClass() == BarycentricCoordinate.class) {
            return new BarycentricCoordinate(0, 0, 0);
        } else if (c.getClass() == EuclideanCoordinate.class) {
            return new EuclideanCoordinate(0, 0);
        }
        System.err.println("Need new null offset, not yet implemented");
        return null;
    }

    private Coordinate getBestOffset(Component curComponent, List<Component> placedComponents, MosaicCartogram cartogram, MosaicCartogram componentCartogram) {

        Coordinate nullCoordinate = getNullCoordinate(componentCartogram);

        //Find the coordinate of the cell c1 in any component of placedComponents that was closest to any cell c2 of this component 
        //and the angle between c1 and b2. This gives an offset an a target angle
        Pair<Coordinate, Double> pair = getClosestCoordinateAndAngle(placedComponents, curComponent);
        if (pair == null) {
            return getNullCoordinate(componentCartogram);
        }

        //each time we try an offset, and check whether this is a feasible offset. If it is not, we try the next in the list.
        //baseoffset holds the initial position, extra offset the displation
        Coordinate baseOffSet = pair.getFirst();
        //stay in the halfplane of the angle while moving.
        double angle = pair.getSecond();

        Coordinate extraOffSet = nullCoordinate;
        Coordinate totalOffSet = baseOffSet.plus(extraOffSet);

        Iterable<Coordinate> coordinates = (Iterable<Coordinate>) componentCartogram.coordinates();

        //holds which coordinates have already been tried
        Set<Coordinate> attempted = new HashSet();
        attempted.add(nullCoordinate);

        while (hasConflict(cartogram, totalOffSet, coordinates)) {
            //get a different offset
            extraOffSet = getNextOffSet(angle, nullCoordinate, attempted);
            //calculate the new total offset
            totalOffSet = baseOffSet.plus(extraOffSet);
            attempted.add(extraOffSet);
        }
        return totalOffSet;
    }

    private boolean boundingBoxComponentIntersect(Component c1, Component c2) {
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        for (Coordinate c : c1.componentCartogram.coordinates()) {
            double x = c.toPoint2D().getX();
            double y = c.toPoint2D().getY();
            minX = Math.min(x, minX);
            maxX = Math.max(x, maxX);
            minY = Math.min(y, minY);
            maxY = Math.max(y, maxY);
        }
        return false;
    }

    public class Component implements Identifier {

        private final int id;
        private final Map componentMap;
        private final Network componentWeakDual;
        private final ElementList<Map.Face> originalFaces;
        private MosaicCartogram componentCartogram;
        private ExperimentLog experimentLog;
//        private BoundingBox mapBox;
//        private BoundingBox cartogramBox;

        private Component(int id, ElementList<Map.Face> originalFaces) {
            this.id = id;
            this.originalFaces = originalFaces;
            this.componentMap = map.restrictToFaces(originalFaces);
            this.componentWeakDual = new Network();
            componentMap.computeWeakDual(componentWeakDual);
            for (int i = 0; i < componentMap.numberOfBoundedFaces(); i++) {
                Map.Face f = componentMap.getFace(i);
                componentWeakDual.getVertex(i).setColor(f.getColor());
            }
        }

        @Override
        public int getId() {
            return id;
        }

        public Map getMap() {
            return componentMap;
        }

        public Network getWeakDual() {
            return componentWeakDual;
        }

        public Map.Face getOriginalFace(Map.Face face) {
            return originalFaces.get(face);
        }

        public MosaicCartogram getCartogram() {
            return componentCartogram;
        }

        public void setCartogram(MosaicCartogram cartogram) {
            this.componentCartogram = cartogram;
        }

        public ExperimentLog getLog() {
            return experimentLog;
        }

        public void setLog(ExperimentLog experimentLog) {
            this.experimentLog = experimentLog;
        }
//        private void computeBoundingBox() {
//            mapBox = new BoundingBox();
//            for (Map.Vertex v : componentMap.vertices()) {
//                mapBox.add(v);
//            }
//            cartogramBox = new BoundingBox();
//            for (Coordinate c : componentCartogram.coordinates()) {
//                Vector2D p = c.toVector2D();
//                cartogramBox.add(p);
//            }
//        }
    }
}
