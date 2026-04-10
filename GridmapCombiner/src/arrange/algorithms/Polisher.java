package arrange.algorithms;

import arrange.algorithms.Moves.Move;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import arrange.algorithms.Moves.ReleaseMove;
import arrange.algorithms.Moves.TakeMove;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileWriter;
import arrange.model.Cartogram.MosaicCartogram;
import arrange.model.Cartogram.MosaicCartogram.MosaicRegion;
import arrange.model.Network;
import arrange.model.graph.FlowDigraph;
import arrange.model.graph.SuccessiveShortestPathMininumCostFlow;
import arrange.model.util.ElementList;

public final class Polisher {

    private final boolean VERBOSE = false;
    private final int MAX_ITERATIONS = 40;
    private FlowDigraph flowDigraph;
    private ElementList<MosaicCartogram.Coordinate> vertexToCoordinate;
    private ArrayList<FlowDigraph.Edge> watchEdges;

    private final MosaicHeuristic mosaicHeuristic;
    private final Network weakDual;
    private MosaicCartogram currentGrid;

    /**
     * Holds all the arcs for debugging purposes.
     */
    private StringBuilder arcString;

    //debugOnly
    int printCount = 0;

    public Polisher(MosaicHeuristic mosaicHeuristic) {
        this.mosaicHeuristic = mosaicHeuristic;
        this.currentGrid = mosaicHeuristic.currentGrid;
        this.weakDual = mosaicHeuristic.weakDual;
    }

    public void polish(final boolean exact) {
        MosaicCartogram bestMosaic = currentGrid.duplicate();
        int bestHexError = mosaicHeuristic.totalHexError();
       // System.out.println("Error before fixing flow = " + bestHexError);
        int hexError;
        int iterations = 0;

        /** setup the flow and execute it iteratively.
         * Keep updating as long as we can improve the hexerror. Can't do it in
         * 1 go as moves might become infeasible due to other moves.
         */
        boolean changed;
        do {
            setupFlow(false);
            changed = execute(false);

            hexError = mosaicHeuristic.totalHexError();
            if (hexError <= bestHexError) {
                bestMosaic = currentGrid.duplicate();
                bestHexError = hexError;
            }
        } while (changed && hexError > 0 && iterations++ < MAX_ITERATIONS);

        /**
         * in case we need the exact amount of tiles, use the flow once more,
         * while not maintaining adjacencies. It will be changed by 1 flow at a
         * time to ensure all moves are valid.
         */
        if (exact) {
            while (hexError > 0) {
                setupFlow(true);
                boolean change = execute(true);

                hexError = mosaicHeuristic.totalHexError();
                if (hexError <= bestHexError) {
                    bestMosaic = currentGrid.duplicate();
                    bestHexError = hexError;
                }

                if (!change) {
                    System.err.println("Can't get the exact number. Should not happen.");
                    break;
                }
            }
        }
        //System.out.println("Error after fixing flow = " + bestHexError);
        currentGrid = bestMosaic.duplicate();

    }

    /**
     * Sets up the flow problem. Only allows arcs between valid moves. In case
     * exact is true, also allows arc that break adjacencies and introduce
     * holes.
     *
     * @param exact
     */
    private void setupFlow(final boolean exact) {
        arcString = new StringBuilder();//debug only
        flowDigraph = new FlowDigraph();
        vertexToCoordinate = new ElementList<>(currentGrid.numberOfCells());
        watchEdges = new ArrayList<>();

        LinkedHashMap<MosaicCartogram.Coordinate, FlowDigraph.Vertex> coordinateToVertex = new LinkedHashMap<>();
        ArrayList<FlowDigraph.Vertex> seaVertices = new ArrayList<>();

        ElementList<ArrayList<FlowDigraph.Vertex>> regionVertices = getRegionVertices();
        LinkedHashSet<MosaicCartogram.Coordinate> boundaryCoordinates = getBoundaryCoordinates();

        ArrayList<Set<MosaicCartogram.Coordinate>> currentHoles = MosaicCartogram.computeHoleBoundaries(currentGrid, currentGrid.getCoordinateSet());

        //generate all boundary vertices.
        for (MosaicCartogram.Coordinate c : boundaryCoordinates) {
            //initialize a flow vertex for the boundary region
            FlowDigraph.Vertex v = flowDigraph.addVertex();
            v.name = c.normalize().toString();
            v.setSupply(0);
            v.setCapacity(1);

            //store the vertex to coordinate and vice versa.
            coordinateToVertex.put(c, v);
            vertexToCoordinate.add(c);
        }

        //assign each vertex to either a sea region or to the mosaicregion
        for (MosaicCartogram.Coordinate c : boundaryCoordinates) {
            //add flow vertices to the region
            FlowDigraph.Vertex flowVc = coordinateToVertex.get(c);
            MosaicCartogram.MosaicRegion regionC = currentGrid.getRegion(c);
            if (regionC != null) {
                regionVertices.get(regionC).add(flowVc);
            } else {
                seaVertices.add(flowVc);
            }
        }

        //add edges between vertices
        for (MosaicCartogram.Coordinate c : boundaryCoordinates) {
            MosaicCartogram.MosaicRegion regionC = currentGrid.getRegion(c);
            for (MosaicCartogram.Coordinate d : c.neighbours()) {
                if (!boundaryCoordinates.contains(d)) {
                    //no edges to non-boundary regions
                    continue;
                }
                //both c and d are boundary coordinates
                MosaicCartogram.MosaicRegion regionD = currentGrid.getRegion(d);

                if (regionC == regionD) {
                    //no edges within the same region, and at least one of them is not null.
                    continue;
                }

                addArc(currentHoles, coordinateToVertex, c, d, exact);
            }
        }

        //Print a ipe file with all the arcs and hexerrors. Only used for debug purposes.
        //printCurrent(pathString, currentGrid);
        // Create supply vertices for the mosaicRegions. Total supply is equal to inverse of sea supply.
        int seaSupply = initializeRegionSupply(regionVertices, exact);

        //Create supply vertex for the sea region.
        initializeSea(seaVertices, seaSupply);

        //increase all the weights by the minimum such that there are no negative values
        makeAllPositive();

        if (VERBOSE) {
            for (MosaicCartogram.MosaicRegion region : currentGrid.regions()) {
                System.out.println("Supply " + region.getMapFace().getLabel().getText() + " = " + (-region.getHexError()));
            }
            System.out.println("Supply #SEA# = " + seaSupply);
        }

    }

    /**
     * Solves the flow problem and applies each valid move. Note that moves may
     * become invalid due to other moves.
     *
     * @param exact if exact is true, moves that break topology and introduce
     *              holes are allowed. Otherwise we cannot guarantee correct amount of
     *              adjacencies.
     * @return
     */
    private boolean execute(final boolean exact) {
        SuccessiveShortestPathMininumCostFlow<FlowDigraph.Vertex, FlowDigraph.Edge> mcf = new SuccessiveShortestPathMininumCostFlow<>(flowDigraph);
        if (mcf.getStatus() != SuccessiveShortestPathMininumCostFlow.Status.FEASIBLE) {
            throw new RuntimeException("infeasible flow model");
        }
        boolean changed = false;

        for (FlowDigraph.Edge e : watchEdges) {
            if (mcf.getFlow(e) > 0) {
                // System.out.println(e + ", flow = " + mcf.getFlow(e));
                MosaicCartogram.Coordinate cSource = vertexToCoordinate.get(e.getSource());
                MosaicCartogram.Coordinate cTarget = vertexToCoordinate.get(e.getTarget());
                MosaicCartogram.MosaicRegion rSource = currentGrid.getRegion(cSource);
                MosaicCartogram.MosaicRegion rTarget = currentGrid.getRegion(cTarget);

                if (rSource == rTarget) {//can't do a move if they are from the same one
                    continue;
                }

                Move m;
                if (rSource != null && rTarget == null) { //give away to a sea region
                    m = new ReleaseMove(currentGrid, cSource);
                    m.evaluate();
                } else { //take away from either a sea region or a different region
                    ArrayList<Set<MosaicCartogram.Coordinate>> currentHoles = MosaicCartogram.computeHoleBoundaries(currentGrid, currentGrid.getCoordinateSet());
                    TakeMove tm = new TakeMove(weakDual, currentGrid, cSource, rTarget.getVertex());
                    tm.evaluateWithHole(currentHoles);
                    if (tm.createsHole && !exact) { //if it creates a hole, don't take it.
                        continue;
                    }
                    m = tm;
                }
                //execute the move if is is valid or (exact and keeps the regions connected. 
                //Moves should not create holes.
                if (m.isValid() || (exact && m.isConnected())) {
                    m.execute();
                    changed = true;
                }
            }
        }
        return changed;
    }

    private int distance(MosaicCartogram.Coordinate c, MosaicCartogram.CellRegion r) {
        int minDistance = Integer.MAX_VALUE;
        for (MosaicCartogram.Coordinate d : r) {
            int distance = c.minus(d).norm();
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        return minDistance;
    }

    private int depth(MosaicCartogram.Coordinate c, MosaicCartogram.CellRegion r) {
        int minDepth = Integer.MAX_VALUE;
        for (MosaicCartogram.Coordinate d : r.neighbours()) {
            int depth = c.minus(d).norm();
            if (depth < minDepth) {
                minDepth = depth;
            }
        }
        return minDepth;
    }

    private ElementList<ArrayList<FlowDigraph.Vertex>> getRegionVertices() {
        ElementList<ArrayList<FlowDigraph.Vertex>> regionVertices = new ElementList<>();
        for (int i = 0; i < currentGrid.numberOfRegions(); i++) {
            regionVertices.add(new ArrayList<>());
        }
        return regionVertices;
    }

    private LinkedHashSet<MosaicCartogram.Coordinate> getBoundaryCoordinates() {
        // Add one vertex per boundary cell
        LinkedHashSet<MosaicCartogram.Coordinate> boundaryCoordinates = new LinkedHashSet<>();
        for (MosaicCartogram.MosaicRegion region : currentGrid.regions()) {
            for (MosaicCartogram.Coordinate c : region.neighbours()) {
                boundaryCoordinates.add(c);
                if (currentGrid.getVertex(c) == null) {
                    for (MosaicCartogram.Coordinate d : c.neighbours()) {
                        if (currentGrid.getVertex(d) != null) {
                            boundaryCoordinates.add(d);
                        }
                    }
                }
            }
        }
        return boundaryCoordinates;
    }

    /**
     * Adds an arc between the vertices associated to c and d if it is a valid
     * arc.
     *
     * @param currentHoles
     * @param coordinateToVertex
     * @param c
     * @param d
     * @param exact              if true: Validity of an arc only requires that
     *                           both arc are connected. Otherwise requires
     *                           topology and hole-free as well.,
     */
    private void addArc(ArrayList<Set<MosaicCartogram.Coordinate>> currentHoles,
                        LinkedHashMap<MosaicCartogram.Coordinate, FlowDigraph.Vertex> coordinateToVertex,
                        MosaicCartogram.Coordinate c,
                        MosaicCartogram.Coordinate d,
                        final boolean exact) {

        MosaicCartogram.MosaicRegion regionC = currentGrid.getRegion(c);
        MosaicCartogram.MosaicRegion regionD = currentGrid.getRegion(d);

        Point2D cP = c.toPoint2D();
        Point2D dP = d.toPoint2D();

        boolean validArc;
        if (regionC == null) {
            validArc = addArcFromSea(coordinateToVertex, currentHoles, c, d, exact);
        } else if (regionD == null) {
            //release vertex c to the sea region
            validArc = addArcToSea(coordinateToVertex, c, d, exact);
        } else { //region C != null and region D != null
            validArc = addArcBetweenBoundaries(coordinateToVertex, currentHoles, c, d, exact);
        }

        if (validArc) {
            arcString.append("<path stroke=\"black\" cap=\"1\" join=\"1\" arrow=\"normal/normal\" >\n"
                             + cP.getX() + " " + cP.getY() + " m\n"
                             + dP.getX() + " " + dP.getY() + " l\n"
                             + "</path>");
        }
    }

    /**
     * Returns true if the arc is added
     *
     * @param coordinateToVertex
     * @param c
     * @param d
     * @param exact
     * @return
     */
    private boolean addArcToSea(LinkedHashMap<MosaicCartogram.Coordinate, FlowDigraph.Vertex> coordinateToVertex,
                                MosaicCartogram.Coordinate c,
                                MosaicCartogram.Coordinate d, 
                                final boolean exact) {
        MosaicCartogram.MosaicRegion regionC = currentGrid.getRegion(c);
        //regionD is a seas region
        FlowDigraph.Vertex flowVc = coordinateToVertex.get(c);
        FlowDigraph.Vertex flowVd = coordinateToVertex.get(d);

        ReleaseMove rm = new ReleaseMove(currentGrid, c);
        rm.evaluate();
        if ((rm.isValid()) || (exact && rm.isConnected())) {

            MosaicCartogram.CellRegion shapeC = regionC.getGuidingShape();
            int weight;
            if (shapeC.contains(c)) {
                weight = depth(c, shapeC);
            } else {
                weight = -distance(c, shapeC);
            }
            createArc(flowVc, flowVd, 1, weight);
            return true;
        }
        return false;
    }

    private boolean addArcFromSea(LinkedHashMap<MosaicCartogram.Coordinate, FlowDigraph.Vertex> coordinateToVertex,
                                  ArrayList<Set<MosaicCartogram.Coordinate>> currentHoles,
                                  final MosaicCartogram.Coordinate c,
                                  final MosaicCartogram.Coordinate d,
                                  final boolean exact) {
        //regionC = is sea region
        MosaicCartogram.MosaicRegion regionD = currentGrid.getRegion(d);
        FlowDigraph.Vertex flowVc = coordinateToVertex.get(c);
        FlowDigraph.Vertex flowVd = coordinateToVertex.get(d);

        TakeMove tm = new TakeMove(weakDual, currentGrid, c, regionD.getVertex());
        tm.evaluateWithHole(currentHoles);
        if ((tm.isValid() && !tm.createsHole)
            || (exact && tm.isConnected())) {
            MosaicCartogram.CellRegion shapeD = regionD.getGuidingShape();
            int weight;
            if (shapeD.contains(c)) {
                weight = -depth(c, shapeD);
            } else {
                weight = distance(c, shapeD);
            }
            createArc(flowVc, flowVd, 1, weight);
            return true;
        }
        return false;
    }

    private boolean addArcBetweenBoundaries(LinkedHashMap<MosaicCartogram.Coordinate, FlowDigraph.Vertex> coordinateToVertex,
                                            ArrayList<Set<MosaicCartogram.Coordinate>> currentHoles,
                                            final MosaicCartogram.Coordinate c,
                                            final MosaicCartogram.Coordinate d,
                                            final boolean exact) {
        MosaicCartogram.MosaicRegion regionC = currentGrid.getRegion(c);
        MosaicCartogram.MosaicRegion regionD = currentGrid.getRegion(d);
        FlowDigraph.Vertex flowVc = coordinateToVertex.get(c);
        FlowDigraph.Vertex flowVd = coordinateToVertex.get(d);

        TakeMove tm = new TakeMove(weakDual, currentGrid, c, regionD.getVertex());
        tm.evaluateWithHole(currentHoles);

        //is all constraints are satisfied OR we are ignoring adjacency constraints and all regeions are still connected.
        if ((!tm.createsHole && tm.isValid())
            || (exact && tm.isConnected())) {

            MosaicCartogram.CellRegion shapeC = regionC.getGuidingShape();
            int weight;
            if (shapeC.contains(c)) {
                weight = depth(c, shapeC);
            } else {
                weight = -distance(c, shapeC);
            }
            MosaicCartogram.CellRegion shapeD = regionD.getGuidingShape();
            if (shapeD.contains(c)) {
                weight -= depth(c, shapeD);
            } else {
                weight += distance(c, shapeD);
            }

            createArc(flowVc, flowVd, 1, weight);
            return true;
        }
        return false;
    }

    /**
     * Creates a flow edge with the specified paramters in the flowdiagram.
     *
     * @param flowVc
     * @param flowVd
     * @param capacity
     * @param weight
     */
    private void createArc(FlowDigraph.Vertex flowVc, FlowDigraph.Vertex flowVd, int capacity, int weight) {
        FlowDigraph.Edge e = flowDigraph.addEdge(flowVc, flowVd);
        watchEdges.add(e);
        e.setCapacity(capacity);
        e.setWeight(weight);
    }

    /**
     * Initialize all supply vertices for the regions, and returns the total hex
     * error for the seaSupply.
     * This makes sure all flows are feasible
     *
     * @param regionVertices
     * @param exact          Only allow 1 unit of flow per time for exact. This
     *                       ensures that each move remains valid.
     * @return
     */
    private int initializeRegionSupply(ElementList<ArrayList<FlowDigraph.Vertex>> regionVertices, final boolean exact) {
        int seaSupply = 0;

        for (MosaicCartogram.MosaicRegion region : currentGrid.regions()) {
            //initialize the supply vertex
            int supply = -region.getHexError();
            if (exact) {//allow only 1 unit of supply in total.
                if (supply > 1) {
                    supply = 1;
                }
                if (supply < -1) {
                    supply = -1;
                }
                if (seaSupply > 0) {
                    supply = 0;
                }
            }

            FlowDigraph.Vertex u = flowDigraph.addVertex();
            u.name = region.getMapFace().getLabel().getText();
            u.setSupply(supply);
            //add an arc between the supply vertex of the region and all its associated boundary vertices.
            ArrayList<FlowDigraph.Vertex> neighbours = regionVertices.get(region);
            for (FlowDigraph.Vertex v : neighbours) {
                flowDigraph.addEdge(u, v);
                flowDigraph.addEdge(v, u);
            }

            seaSupply -= supply;
        }

        return seaSupply;
    }

    /**
     * Initialize the supply vertex for the sea region and set it adjacent to
     * all sea boundary vertices.
     *
     * @param seaVertices
     * @param seaSupply
     */
    private void initializeSea(ArrayList<FlowDigraph.Vertex> seaVertices, int seaSupply) {
        FlowDigraph.Vertex seaVertex = flowDigraph.addVertex();
        seaVertex.name = "#SEA#";

        seaVertex.setSupply(seaSupply);
        for (FlowDigraph.Vertex v : seaVertices) {
            flowDigraph.addEdge(seaVertex, v);
            flowDigraph.addEdge(v, seaVertex);
        }

    }

    private void makeAllPositive() {
        int minWeight = Integer.MAX_VALUE;
        for (FlowDigraph.Edge e : flowDigraph.edges()) {
            minWeight = Math.min(minWeight, e.getWeight());
        }
        if (minWeight < 0) {
            for (FlowDigraph.Edge e : flowDigraph.edges()) {
                e.setWeight(e.getWeight() - minWeight);
            }
        }
    }

    /**
     * prints a map showing the arcs between regions for debugging purposes.
     *
     * @param pathString
     * @param currentGrid
     */
    private void printCurrent(StringBuilder pathString, MosaicCartogram currentGrid) {
        String outputString = "";
        String preamble = "<?xml version=\"1.0\"?>\n"
                          + "<!DOCTYPE ipe SYSTEM \"ipe.dtd\">\n"
                          + "<ipe version=\"70005\" creator=\"Java IpeLib\">\n"
                          + "<ipestyle name=\"basic\">\n"
                          + "<pen name=\"heavier\" value=\"0.80\"/>\n"
                          + "<pen name=\"fat\" value=\"1.20\"/>\n"
                          + "<pen name=\"ultrafat\" value=\"2.00\"/>\n"
                          + "<color name=\"red\" value=\"1.000 0.000 0.000\"/>\n"
                          + "<color name=\"green\" value=\"0.000 1.000 0.000\"/>\n"
                          + "<color name=\"blue\" value=\"0.000 0.000 1.000\"/>\n"
                          + "<color name=\"yellow\" value=\"1.000 1.000 0.000\"/>\n"
                          + "<color name=\"orange\" value=\"1.000 0.647 0.000\"/>\n"
                          + "<color name=\"gold\" value=\"1.000 0.843 0.000\"/>\n"
                          + "<color name=\"purple\" value=\"0.627 0.125 0.941\"/>\n"
                          + "<color name=\"gray\" value=\"0.745\"/>\n"
                          + "<color name=\"brown\" value=\"0.647 0.165 0.165\"/>\n"
                          + "<color name=\"navy\" value=\"0.000 0.000 0.502\"/>\n"
                          + "<color name=\"pink\" value=\"1.000 0.753 0.796\"/>\n"
                          + "<color name=\"seagreen\" value=\"0.180 0.545 0.341\"/>\n"
                          + "<color name=\"turquoise\" value=\"0.251 0.878 0.816\"/>\n"
                          + "<color name=\"violet\" value=\"0.933 0.510 0.933\"/>\n"
                          + "<color name=\"darkblue\" value=\"0.000 0.000 0.545\"/>\n"
                          + "<color name=\"darkcyan\" value=\"0.000 0.545 0.545\"/>\n"
                          + "<color name=\"darkgray\" value=\"0.663\"/>\n"
                          + "<color name=\"darkgreen\" value=\"0.000 0.392 0.000\"/>\n"
                          + "<color name=\"darkmagenta\" value=\"0.545 0.000 0.545\"/>\n"
                          + "<color name=\"darkorange\" value=\"1.000 0.549 0.000\"/>\n"
                          + "<color name=\"darkred\" value=\"0.545 0.000 0.000\"/>\n"
                          + "<color name=\"lightblue\" value=\"0.678 0.847 0.902\"/>\n"
                          + "<color name=\"lightcyan\" value=\"0.878 1.000 1.000\"/>\n"
                          + "<color name=\"lightgray\" value=\"0.827\"/>\n"
                          + "<color name=\"lightgreen\" value=\"0.565 0.933 0.565\"/>\n"
                          + "<color name=\"lightyellow\" value=\"1.000 1.000 0.878\"/>\n"
                          + "</ipestyle>\n"
                          + "<page>\n"
                          + "<layer name=\"cells\"/>\n"
                          + "<group layer=\"cells\">\n";

        String cells = "";

        for (MosaicRegion r : currentGrid.regions()) {
            Color color = r.getVertex().getColor();
            for (MosaicCartogram.Coordinate c : r) {
                String cell = getSquare(c, color);
                cells += cell + "\n";
            }
        }

        outputString += preamble;
        outputString += cells;
        outputString += "</group>\n";
        outputString += pathString.toString();

        for (MosaicRegion r : currentGrid.regions()) {
            int hexError = r.getHexError();
            MosaicCartogram.Coordinate barycenter = r.barycenter();
            Point2D p = barycenter.toPoint2D();
            if (hexError != 0) {
                System.out.println("hexError for R = " + hexError);

                outputString += "<text transformations=\"translations\" "
                                + "pos=\"" + p.getX() + " " + p.getY() + "\" "
                                + "stroke=\"black\" type=\"label\" width=\"4.981\" height=\"6.42\" depth=\"0\" valign=\"baseline\">"
                                + hexError
                                + "</text>";
            }
        }

        outputString += "</page>\n"
                        + "</ipe>";

        try {
            FileWriter fw = new FileWriter(new File("../Data/output/debug" + printCount + ".ipe"));
            fw.append(outputString);
            fw.flush();
            fw.close();
        } catch (Exception e) {

        }
        printCount++;

    }

    /**
     * Returns a ipe-string for a square at coordinate c with color {@code}
     * color.
     *
     * @param c
     * @param color
     * @return
     */
    private String getSquare(MosaicCartogram.Coordinate c, Color color) {
        Point2D cP = c.toPoint2D();
        double midX = cP.getX();
        double midY = cP.getY();

        double r = ((double) color.getRed()) / 255.0;
        double g = ((double) color.getGreen()) / 255.0;
        double b = ((double) color.getBlue()) / 255.0;

        String square = "<path fill=\"" + r + " " + g + " " + b + "\">\n"
                        + (midX - 0.5) + " " + (midY - 0.5) + " m\n"
                        + (midX + 0.5) + " " + (midY - 0.5) + " l\n"
                        + (midX + 0.5) + " " + (midY + 0.5) + " l\n"
                        + (midX - 0.5) + " " + (midY + 0.5) + " l\n"
                        + "h\n"
                        + "</path>";
        return square;
    }

}
