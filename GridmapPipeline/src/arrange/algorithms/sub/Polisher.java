package arrange.algorithms.sub;

import arrange.MosaicConstants;
import java.util.ArrayList;
import arrange.algorithms.moves.ReleaseMove;
import arrange.algorithms.moves.TakeMove;
import arrange.model.GuidingShape;
import arrange.model.MosaicCartogram;
import common.mcf.FlowDigraph;
import arrange.model.MosaicCartogram.MosaicRegion;
import common.gridmath.GridMath.Coordinate;
import arrange.util.ElementList;
import common.dual.Dual;
import common.gridmath.AdjacencyType;
import common.gridmath.util.CoordinateMap;
import common.gridmath.util.CoordinateSet;

/**
 *
 * @author Rafael Cano, Wouter Meulemans
 */
public final class Polisher {

    private FlowDigraph flowDigraph;
    private Coordinate[] vertexToCoordinate;
    private ArrayList<FlowDigraph.Edge> watchEdges;

    private final MosaicCartogram grid;

    public Polisher(Dual dual, MosaicCartogram grid) {
        this.grid = grid;
    }

    public void polish() {

        int tileError = grid.totalTileError();
        int it = MosaicConstants.POLISHER_MAX_ITERATIONS;

        boolean changed = true;
        while (tileError > 0 && changed && it > 0) {

            if (MosaicConstants.POLISHER_REPOSITION_GUIDES) {
                grid.repositionGuidingShapes();
            }

            setupFlow(false);
            changed = execute(false);

            // grid.export("Finalizing: polish " + (MosaicConstants.POLISHER_MAX_ITERATIONS - it), 2);
            int err = grid.totalTileError();

            assert err < tileError;

            tileError = err;
            it--;
        }

        /**
         * in case we need the exact number of tiles, use the flow once more,
         * while not maintaining adjacencies. It will be changed by 1 flow at a
         * time to ensure all moves are valid.
         */
        if (MosaicConstants.EXACT_TILES) {
            while (tileError > 0) {

                if (MosaicConstants.POLISHER_REPOSITION_GUIDES) {
                    grid.repositionGuidingShapes();
                }

                setupFlow(true);
                boolean change = execute(true);

                // grid.export("Finalizing: exact polish " + (MosaicConstants.POLISHER_MAX_ITERATIONS - it), 2);
                if (!change) {
                    System.err.println("Can't get the exact number. Should not happen.");
                    break;
                }

                int err = grid.totalTileError();
                assert err < tileError;
                tileError = err;

            }
        }
    }

    /**
     * Sets up the flow problem. Only allows arcs between valid moves. In case
     * exact is true, also allows arc that break adjacencies and introduce
     * holes.
     *
     * @param exact
     */
    private void setupFlow(final boolean exact) {
        flowDigraph = new FlowDigraph();
        watchEdges = new ArrayList<>();

        CoordinateMap<FlowDigraph.Vertex> coordinateToVertex = new CoordinateMap();
        ArrayList<FlowDigraph.Vertex> seaVertices = new ArrayList<>();

        ElementList<ArrayList<FlowDigraph.Vertex>> regionVertices = getRegionVertices();
        CoordinateSet boundaryCoordinates = getBoundaryCoordinates();

        vertexToCoordinate = new Coordinate[boundaryCoordinates.size()];

        //generate all boundary vertices.
        for (Coordinate c : boundaryCoordinates) {
            //initialize a flow vertex for the boundary region
            FlowDigraph.Vertex v = flowDigraph.addVertex();
            v.name = c.toString();
            v.setSupply(0);
            v.setCapacity(1);

            //store the vertex to coordinate and vice versa.
            coordinateToVertex.put(c, v);
            vertexToCoordinate[v.getId()] = c;
        }

        //assign each vertex to either a sea region or to the mosaicregion
        for (Coordinate c : boundaryCoordinates) {
            //add flow vertices to the region
            FlowDigraph.Vertex flowVc = coordinateToVertex.get(c);
            MosaicRegion regionC = grid.getRegion(c);
            if (regionC != null) {
                regionVertices.get(regionC).add(flowVc);
            } else {
                seaVertices.add(flowVc);
            }
        }

        //add edges between vertices
        for (Coordinate c : boundaryCoordinates) {
            MosaicRegion regionC = grid.getRegion(c);
            for (Coordinate d : c.adjacent(AdjacencyType.ROOKS)) {
                if (!boundaryCoordinates.contains(d)) {
                    //no edges to non-boundary regions
                    continue;
                }
                //both c and d are boundary coordinates
                MosaicRegion regionD = grid.getRegion(d);

                if (regionC == regionD) {
                    //no edges within the same region, and at least one of them is not null.
                    continue;
                }

                addArc(coordinateToVertex, c, d, exact);
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

    }

    /**
     * Solves the flow problem and applies each valid move. Note that moves may
     * become invalid due to other moves.
     *
     * @param exact if exact is true, moves that break topology and introduce
     * holes are allowed. Otherwise we cannot guarantee correct amount of
     * adjacencies.
     * @return
     */
    private boolean execute(final boolean exact) {

        int[] flow = flowDigraph.solve();
        if (flow == null) {
            return false;
        }

        boolean changed = false;
        for (FlowDigraph.Edge e : watchEdges) {
            if (flow[e.getId()] > 0) {
                Coordinate cSource = vertexToCoordinate[e.getSource().getId()];
                Coordinate cTarget = vertexToCoordinate[e.getTarget().getId()];
                MosaicRegion rSource = grid.getRegion(cSource);
                MosaicRegion rTarget = grid.getRegion(cTarget);

                if (rSource == rTarget) {//can't do a move if they are from the same one
                    continue;
                }

                //execute the move if is is valid or (exact and keeps the regions connected. 
                //Moves should not create holes.
                if (rSource != null && rTarget == null) { //give away to a sea region
                    ReleaseMove rm = new ReleaseMove(grid, cSource);
                    if (rm.canExecute(exact) && !rm.createsHole()) {
                        rm.execute();
                        changed = true;
                    }
                } else { //take away from either a sea region or a different region
                    TakeMove tm = new TakeMove(grid, cSource, rTarget.getVertex());
                    if (tm.canExecute(exact) && !tm.createsHole()) {
                        tm.execute();
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    private double weightComponent(Coordinate c, GuidingShape shape) {
        double weight;
        if (shape.contains(c)) {
            weight = depth(c, shape);
        } else {
            weight = -distance(c, shape);
        }
        weight += 0.25 * shape.desirability(c) / c.getBoundary().areaUnsigned();
        return weight;
    }

    private int distance(Coordinate c, CoordinateSet r) {
        int minDistance = Integer.MAX_VALUE;
        for (Coordinate d : r.neighbors(AdjacencyType.ROOKS)) {
            int distance = c.norm(d) + 1;
            if (distance == 0) {
                return 0;
            } else if (distance < minDistance) {
                minDistance = distance;
            }
        }
        return minDistance;
    }

    private int depth(Coordinate c, CoordinateSet r) {
        int minDepth = Integer.MAX_VALUE;
        for (Coordinate d : r.neighbors(AdjacencyType.ROOKS)) {
            int depth = c.norm(d);
            if (depth == 0) {
                return 0;
            } else if (depth < minDepth) {
                minDepth = depth;
            }
        }
        return minDepth;
    }

    private ElementList<ArrayList<FlowDigraph.Vertex>> getRegionVertices() {
        ElementList<ArrayList<FlowDigraph.Vertex>> regionVertices = new ElementList<>();
        for (int i = 0; i < grid.numberOfRegions(); i++) {
            regionVertices.add(new ArrayList<>());
        }
        return regionVertices;
    }

    private CoordinateSet getBoundaryCoordinates() {
        // Add one vertex per boundary cell
        CoordinateSet boundaryCoordinates = new CoordinateSet(grid.getGridMath());
        for (MosaicRegion region : grid.regions()) {
            for (Coordinate c : region.getAllocated().neighbors(AdjacencyType.ROOKS)) {
                boundaryCoordinates.add(c);
                if (grid.getVertex(c) == null) {
                    for (Coordinate d : c.adjacent(AdjacencyType.ROOKS)) {
                        if (grid.getVertex(d) != null) {
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
     * @param exact if true: Validity of an arc only requires that both arc are
     * connected. Otherwise requires topology and hole-free as well.,
     */
    private void addArc(CoordinateMap<FlowDigraph.Vertex> coordinateToVertex,
            Coordinate c,
            Coordinate d,
            final boolean exact) {

        MosaicRegion regionC = grid.getRegion(c);
        MosaicRegion regionD = grid.getRegion(d);

        if (regionC == null) {
            addArcFromSea(coordinateToVertex, c, d, exact);
        } else if (regionD == null) {
            //release vertex c to the sea region
            addArcToSea(coordinateToVertex, c, d, exact);
        } else { //region C != null and region D != null
            addArcBetweenBoundaries(coordinateToVertex, c, d, exact);
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
    private boolean addArcToSea(CoordinateMap<FlowDigraph.Vertex> coordinateToVertex,
            Coordinate c,
            Coordinate d,
            final boolean exact) {
        MosaicRegion regionC = grid.getRegion(c);
        //regionD is a seas region
        FlowDigraph.Vertex flowVc = coordinateToVertex.get(c);
        FlowDigraph.Vertex flowVd = coordinateToVertex.get(d);

        ReleaseMove rm = new ReleaseMove(grid, c);
        if (rm.canExecute(exact) && !rm.createsHole()) {

            GuidingShape shapeC = regionC.getGuidingShape();
            double weight = weightComponent(c, shapeC);

            createArc(flowVc, flowVd, 1, weight);
            return true;
        }
        return false;
    }

    private boolean addArcFromSea(CoordinateMap<FlowDigraph.Vertex> coordinateToVertex,
            final Coordinate c,
            final Coordinate d,
            final boolean exact) {
        //regionC = is sea region
        MosaicRegion regionD = grid.getRegion(d);
        FlowDigraph.Vertex flowVc = coordinateToVertex.get(c);
        FlowDigraph.Vertex flowVd = coordinateToVertex.get(d);

        TakeMove tm = new TakeMove(grid, c, regionD.getVertex());

        // we now also want to ensure that we do not create a hole anymore
        if (tm.canExecute(exact) && !tm.createsHole()) {
            GuidingShape shapeD = regionD.getGuidingShape();
            double weight = -weightComponent(c, shapeD);

            createArc(flowVc, flowVd, 1, weight);
            return true;
        }
        return false;
    }

    private boolean addArcBetweenBoundaries(CoordinateMap<FlowDigraph.Vertex> coordinateToVertex,
            final Coordinate c,
            final Coordinate d,
            final boolean exact) {
        MosaicRegion regionC = grid.getRegion(c);
        MosaicRegion regionD = grid.getRegion(d);
        FlowDigraph.Vertex flowVc = coordinateToVertex.get(c);
        FlowDigraph.Vertex flowVd = coordinateToVertex.get(d);

        TakeMove tm = new TakeMove(grid, c, regionD.getVertex());

        // we do not want to create a hole, but since this move swaps tiles between regions, the unoccupied tiles remain the same (and didnt have holes to begin with)
        if (tm.canExecute(exact)) {

            GuidingShape shapeC = regionC.getGuidingShape();
            GuidingShape shapeD = regionD.getGuidingShape();
            double weight = weightComponent(c, shapeC) - weightComponent(c, shapeD);

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
    private void createArc(FlowDigraph.Vertex flowVc, FlowDigraph.Vertex flowVd, int capacity, double weight) {
        FlowDigraph.Edge e = flowDigraph.addEdge(flowVc, flowVd);
        watchEdges.add(e);
        e.setCapacity(capacity);
        e.setWeight(weight);
    }

    /**
     * Initialize all supply vertices for the regions, and returns the total hex
     * error for the seaSupply. This makes sure all flows are feasible
     *
     * @param regionVertices
     * @param exact Only allow 1 unit of flow per time for exact. This ensures
     * that each move remains valid.
     * @return
     */
    private int initializeRegionSupply(ElementList<ArrayList<FlowDigraph.Vertex>> regionVertices, final boolean exact) {
        int seaSupply = 0;

        for (MosaicRegion region : grid.regions()) {
            //initialize the supply vertex
            int supply = -region.getTileError();
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
            u.name = region.getPartition().label;
            u.setSupply(supply);
            //add an arc between the supply vertex of the region and all its associated boundary vertices.
            ArrayList<FlowDigraph.Vertex> neighbors = regionVertices.get(region);
            for (FlowDigraph.Vertex v : neighbors) {
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
        double minWeight = Double.POSITIVE_INFINITY;
        for (FlowDigraph.Edge e : flowDigraph.edges()) {
            minWeight = Math.min(minWeight, e.getWeight());
        }
        if (minWeight < 0) {
            for (FlowDigraph.Edge e : flowDigraph.edges()) {
                e.setWeight(e.getWeight() - minWeight);
            }
        }
    }

}
