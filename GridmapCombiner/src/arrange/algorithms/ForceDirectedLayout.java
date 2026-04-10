package arrange.algorithms;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import arrange.model.Cartogram.MosaicCartogram;
import arrange.model.Cartogram.MosaicCartogram.CellRegion;
import arrange.model.Cartogram.MosaicCartogram.Coordinate;
import arrange.model.Cartogram.MosaicCartogram.MosaicRegion;
import arrange.model.Network;
import arrange.model.subdivision.Map;
import arrange.model.util.ElementList;
import arrange.model.util.Random;
import arrange.model.util.Vector2D;

/**
 *
 * @author Max Sondag
 */
public final class ForceDirectedLayout {

    private static double INTENSITY = 150.0;
    //attraction force between two neighbouring regions
    private static final double ATTRACTION_WEIGHT = 1;
    //Force when two neighbouring regions overlap
    private static double REPULSION_WEIGHT = 35.0;
    //force when two non-neighbouring regions overlap
    private static double NON_NEIGHBOUR_REPULSION_WEIGHT = 40.0;
    private static boolean ALL_NONNEIGHBOR = false;
    private final double TIME_STEP;
    private static final double MINIMUM_NORM = 5.0;
    private final double MAXIMUM_NORM;
    private static int MAXIMUM_ITERATIONS = 400;
    private static int MAXIMUM_BAD_ITERATIONS = 100;
    private static int FORCE_TOTAL_ITERATIONS = 20000;

    public static void configureGridMap() {

        INTENSITY = 150.0;
        REPULSION_WEIGHT = 35.0;
        NON_NEIGHBOUR_REPULSION_WEIGHT = 40.0;
        MAXIMUM_ITERATIONS = 400;
        MAXIMUM_BAD_ITERATIONS = 100;
        FORCE_TOTAL_ITERATIONS = 20000;
        ALL_NONNEIGHBOR = false;
    }

    public static void configureMosaicCartogram() {

        INTENSITY = 100.0;
        REPULSION_WEIGHT = 5.0;
        NON_NEIGHBOUR_REPULSION_WEIGHT = 2.0;
        MAXIMUM_ITERATIONS = 200;
        MAXIMUM_BAD_ITERATIONS = 50;
        FORCE_TOTAL_ITERATIONS = Integer.MAX_VALUE;
        ALL_NONNEIGHBOR = true;
    }

    private final ElementList<Vector2D> forces;
    private final ElementList<Vector2D> continuousPositions;
    private final ElementList<MosaicCartogram.Coordinate> discretePositions;
    private final ElementList<Boolean> blocked;
    private final ElementList<Integer> badIterations;

    private final MosaicCartogram currentGrid;
    private final Network weakDual;
    //Contains the guiding shape on the specified coordinate
    private final HashMap<Coordinate, Set<CellRegion>> regionsOnCoordinate;
    private final HashMap<Coordinate, Set<CellRegion>> neighbourRegionsOnCoordinate;

    public ForceDirectedLayout(MosaicCartogram currentGrid, Network weakDual) {
        this.currentGrid = currentGrid;
        this.weakDual = weakDual;
        TIME_STEP = 2 * currentGrid.getCellSide() / (50 * INTENSITY);
        MAXIMUM_NORM = currentGrid.getCellSide() / TIME_STEP;

        forces = new ElementList<>(currentGrid.numberOfRegions(), null);
        continuousPositions = new ElementList<>(currentGrid.numberOfRegions());
        discretePositions = new ElementList<>(currentGrid.numberOfRegions());
        blocked = new ElementList<>(currentGrid.numberOfRegions());
        badIterations = new ElementList<>(currentGrid.numberOfRegions(), 0);

        regionsOnCoordinate = new HashMap();
        neighbourRegionsOnCoordinate = new HashMap();
        for (MosaicCartogram.MosaicRegion region : currentGrid.regions()) {
            Vector2D barycenter = region.getGuidingShape().continuousBarycenter();
            continuousPositions.add(new Vector2D(barycenter));
            discretePositions.add(currentGrid.getContainingCell(barycenter));

            CellRegion guidingShape = region.getGuidingShape();
            for (Coordinate c : guidingShape.coordinateSet()) {
                if (!regionsOnCoordinate.containsKey(c)) {
                    regionsOnCoordinate.put(c, new HashSet());
                }
                regionsOnCoordinate.get(c).add(guidingShape);
            }
            for (Coordinate c : guidingShape.neighbours()) {
                if (!neighbourRegionsOnCoordinate.containsKey(c)) {
                    neighbourRegionsOnCoordinate.put(c, new HashSet());
                }
                neighbourRegionsOnCoordinate.get(c).add(guidingShape);
            }
        }
    }

    private void translateGuidingShape(MosaicRegion ru, Coordinate translate) {
        //remove guiding shape from regions on Coordinate and add on new location
        CellRegion guidingShape = ru.getGuidingShape();
        //start removing
        for (Coordinate c : guidingShape.coordinateSet()) {
            regionsOnCoordinate.get(c).remove(guidingShape);
            if (regionsOnCoordinate.get(c).isEmpty()) {
                regionsOnCoordinate.remove(c);
            }
        }
        for (Coordinate c : guidingShape.neighbours()) {
            neighbourRegionsOnCoordinate.get(c).remove(guidingShape);
            if (neighbourRegionsOnCoordinate.get(c).isEmpty()) {
                neighbourRegionsOnCoordinate.remove(c);
            }
        }
        //done removing
        ru.translateGuidingShape(translate);
        //start adding
        for (Coordinate c : guidingShape.coordinateSet()) {
            if (!regionsOnCoordinate.containsKey(c)) {
                regionsOnCoordinate.put(c, new HashSet());
            }
            regionsOnCoordinate.get(c).add(guidingShape);
        }
        for (Coordinate c : guidingShape.neighbours()) {
            if (!neighbourRegionsOnCoordinate.containsKey(c)) {
                neighbourRegionsOnCoordinate.put(c, new HashSet());
            }
            neighbourRegionsOnCoordinate.get(c).add(guidingShape);
        }
        //done adding
    }

    public boolean runModel(Map map) {

        ElementList<MosaicCartogram.Coordinate> translations = new ElementList<>(currentGrid.numberOfRegions(), currentGrid.zeroVector());
        blocked.assign(currentGrid.numberOfRegions(), false);
        boolean stop = false;

        //Total amount before we stop the looping
        int totalIterations = FORCE_TOTAL_ITERATIONS;

        int iterations = 0;
        double largestNorm = Double.POSITIVE_INFINITY;
        while (largestNorm > MINIMUM_NORM) {
            if (++iterations > MAXIMUM_ITERATIONS) {
                iterations = 0;
                randomShake();
            }
            totalIterations--;

            if (totalIterations < 0) {
                return false;
            }

            for (MosaicCartogram.MosaicRegion region : currentGrid.regions()) {
                if (isShakeable(region)) {
                    int count = badIterations.get(region) + 1;
                    badIterations.set(region, count);
                    if (count > MAXIMUM_BAD_ITERATIONS) {
                        randomShake(region);
                        badIterations.set(region, 0);
                    }
                }
            }

            computeForces();

            for (Network.Vertex u : weakDual.vertices()) {
                if (!blocked.get(u)) {
                    //move it continously
                    Vector2D positionIncrement = Vector2D.product(forces.get(u), TIME_STEP);
                    Vector2D continuousPosition = continuousPositions.get(u);
                    continuousPosition.add(positionIncrement);

                    MosaicCartogram.Coordinate newCoordinate = currentGrid.getContainingCell(continuousPosition);
                    MosaicCartogram.Coordinate discretePosition = discretePositions.get(u);
                    //if it moved discretely
                    if (!newCoordinate.equals(discretePosition)) {
                        //move the guiding shape
                        MosaicCartogram.Coordinate translate = newCoordinate.minus(discretePosition);
                        MosaicCartogram.MosaicRegion ru = currentGrid.getRegion(u.getId());
                        translateGuidingShape(ru, translate);

                        MosaicCartogram.CellRegion gu = ru.getGuidingShape();
                        if (ru.intersects(gu) || ru.intersectsNeighbours(gu)) {
                            //if the guidingshape is on top, or next to the region
                            discretePositions.set(u, newCoordinate);

                            //store how far this region has translated
                            MosaicCartogram.Coordinate translation = translations.get(ru).plus(translate);
                            translations.set(ru, translation);

                            //if a guiding shape has moved a full cell we can stop
                            if (translation.norm() > 0) {
                                stop = true;
                            }
                        } else {
                            //guiding shape to far away from region, revert
                            translateGuidingShape(ru, translate.times(-1));
                            continuousPosition.subtract(positionIncrement);
                        }
                    }
                }
            }//end of for loop
            //Still in while loop
            if (stop) {
                return true;
            }
        }
        return false;
    }

    /**
     * Shake all the regions, Returns true if a region has changed
     *
     * @return
     */
    private boolean randomShake() {
        boolean modified = false;
        for (MosaicCartogram.MosaicRegion region : currentGrid.regions()) {
            modified |= randomShake(region);
        }
        return modified;
    }

    /**
     * Shake a specific region, returns true if the region has changed
     *
     * @param region
     * @return
     */
    private boolean randomShake(MosaicCartogram.MosaicRegion region) {
        MosaicCartogram.CellRegion guidingShape = region.getGuidingShape();
        if (isShakeable(region)) {
            MosaicCartogram.Coordinate[] regionCoordinates = region.occupiedCoordinates();
            MosaicCartogram.Coordinate[] shapeCoordinates = guidingShape.occupiedCoordinates();

            //randomize the coordinates
            int rRegion = Random.nextInt(regionCoordinates.length);
            int rShape = Random.nextInt(shapeCoordinates.length);
            MosaicCartogram.Coordinate cRegion = regionCoordinates[rRegion];
            MosaicCartogram.Coordinate cShape = shapeCoordinates[rShape];

            MosaicCartogram.Coordinate t = cRegion.minus(cShape);
            //shake the guiding shaped based on position of guiding shape and region
            translateGuidingShape(region, t);
            //update variables
            Vector2D barycenter = new Vector2D(guidingShape.continuousBarycenter());
            continuousPositions.set(region, barycenter);
            discretePositions.set(region, currentGrid.getContainingCell(barycenter));

            return true;
        }
        return false;
    }

    private boolean isShakeable(MosaicCartogram.MosaicRegion region) {
        double error = (double) region.getSymmetricDifference() / region.getGuidingShape().size();
        return (error >= 1.0);
    }

    private void computeForces() {
        for (Network.Vertex u : weakDual.vertices()) {
            //stores the neighbours of u. 
            //TODO preprocess
            Set<Network.Vertex> neighbours = new HashSet();

            MosaicCartogram.MosaicRegion ru = currentGrid.getRegion(u.getId());
            Vector2D force = new Vector2D(0, 0);
            forces.set(ru, force);
            // Neighbours are affected by all forces
            for (Network.Vertex v : weakDual.neighbours(u)) {
                //store the neighbour
                neighbours.add(v);

                MosaicCartogram.MosaicRegion rv = currentGrid.getRegion(v.getId());
                //calculate the forces
                Vector2D attraction = attractionForce(ru, rv);
                Vector2D repulsion = neighbourRepulsionForce(ru, rv);
                //add the forces
                force.add(attraction);
                force.add(repulsion);
            }
            //neighbours now holds all the neighbors of v
            // Non-neighbours are only affected by repulsion
            for (Network.Vertex v : weakDual.vertices()) {
                if (!neighbours.contains(v)) {
                    MosaicCartogram.MosaicRegion rv = currentGrid.getRegion(v.getId());
                    Vector2D repulsion = nonNeighbourRepulsionForce(ru, rv);
                    force.add(repulsion);
                }
            }
        }

        // Truncate forces to maximum norm
        for (Network.Vertex u : weakDual.vertices()) {
            Vector2D force = forces.get(u);
            if (force.norm() > MAXIMUM_NORM) {
                force.normalize().multiply(MAXIMUM_NORM);
            }
        }
    }

    private Vector2D attractionForce(MosaicCartogram.MosaicRegion r1, MosaicCartogram.MosaicRegion r2) {

        Vector2D force = new Vector2D(0, 0);
        MosaicCartogram.CellRegion g1 = r1.getGuidingShape();
        MosaicCartogram.CellRegion g2 = r2.getGuidingShape();
        MosaicCartogram.Coordinate t1 = g1.barycenter();
        MosaicCartogram.Coordinate t2 = g2.barycenter();
        //////////////////////////////////////////////////////
        //get the direction of the force
        Vector2D b1 = g1.continuousBarycenter();
        b1.add(t1.toVector2D());
        Vector2D b2 = g2.continuousBarycenter();
        b2.add(t2.toVector2D());
        force.add(Vector2D.difference(b2, b1));
        //////////////////////////////////////////////////////
        //get the magnitude of the force
        int distance = distance(g1, t1, g2, t2);
        distance = Math.max(1, distance);
        force.normalize().multiply(INTENSITY * ATTRACTION_WEIGHT * distance);
        return force;
    }

    private Vector2D neighbourRepulsionForce(MosaicCartogram.MosaicRegion r1, MosaicCartogram.MosaicRegion r2) {

        MosaicCartogram.CellRegion g1 = r1.getGuidingShape();
        MosaicCartogram.CellRegion g2 = r2.getGuidingShape();

        int intersectionSize = 0;
        Vector2D force = new Vector2D(0, 0);
        for (MosaicCartogram.Coordinate c : g1) {
            //if the regions intersect
            if (regionsOnCoordinate.get(c).contains(g2)) {
                intersectionSize++;
                Vector2D p1 = r1.getCorrespondingMapPoint(c);
                Vector2D p2 = r2.getCorrespondingMapPoint(c);
                Vector2D component = Vector2D.difference(p1, p2).normalize();
                //direction of vector equals the direction of the 2 regions with regard
                //to each other
                force.add(component);
            }
        }
        if (intersectionSize == 0) {
            return new Vector2D(0, 0);
        }

        force.normalize();
        double factor = 1.0 + (double) (intersectionSize) / (double) (g1.size());
        force.multiply(INTENSITY * REPULSION_WEIGHT * factor);
        return force;

    }

    private Vector2D nonNeighbourRepulsionForce(MosaicCartogram.MosaicRegion r1, MosaicCartogram.MosaicRegion r2) {
        MosaicCartogram.CellRegion g1 = r1.getGuidingShape();
        MosaicCartogram.CellRegion g2 = r2.getGuidingShape();

        int intersectionSize = 0;
        Vector2D force = new Vector2D(0, 0);
        for (MosaicCartogram.Coordinate c : g1) {
            //For each coordinate, check if the coordinate or its neighbours contains the other region
            if (regionsOnCoordinate.get(c).contains(g2)
                    || (neighbourRegionsOnCoordinate.containsKey(c) && neighbourRegionsOnCoordinate.get(c).contains(g2))) {
                intersectionSize++;
                //get the direction of the force required to push them apart based
                //on the original map
                Vector2D p1 = r1.getCorrespondingMapPoint(c);
                Vector2D p2 = r2.getCorrespondingMapPoint(c);
                Vector2D component = Vector2D.difference(p1, p2).normalize();
                force.add(component);
                //stop as soon as we found the first one for performance reasons
                if (!ALL_NONNEIGHBOR) {
                    break;
                }
            }
        }

        if (intersectionSize == 0) {
            return new Vector2D(0, 0);
        }
        force.normalize();
        double factor = 1.0 + (double) (intersectionSize) / (double) (g1.size());
        force.multiply(INTENSITY * NON_NEIGHBOUR_REPULSION_WEIGHT * factor);
        return force;

    }

    //shortest distance between 2 cells
    private int distance(MosaicCartogram.CellRegion h1, MosaicCartogram.Coordinate t1,
            MosaicCartogram.CellRegion h2, MosaicCartogram.Coordinate t2) {
        int minimumDistance = Integer.MAX_VALUE;
        for (MosaicCartogram.Coordinate c1 : h1) {
            c1 = c1.plus(t1);
            for (MosaicCartogram.Coordinate c2 : h2) {
                c2 = c2.plus(t2);
                int distance = c1.minus(c2).norm();
                if (distance < minimumDistance) {
                    minimumDistance = distance;
                }
            }
            if (minimumDistance == 0) {
                break;
            }
        }
        return minimumDistance;
    }
}
