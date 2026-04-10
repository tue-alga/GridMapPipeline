package arrange.algorithms.sub;

import arrange.MosaicConstants;
import arrange.model.GuidingShape;
import common.gridmath.GridMath.Coordinate;
import arrange.util.Utils;
import arrange.model.MosaicCartogram;
import arrange.model.MosaicCartogram.MosaicRegion;
import common.dual.Dual;
import common.dual.Vertex;
import common.gridmath.AdjacencyType;
import common.gridmath.util.CoordinateSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;

/**
 *
 * @author Max Sondag, Wouter Meulemans
 */
public final class ForceDirectedLayout {

    private final double TIME_STEP;
    private final double MAXIMUM_NORM;

    private final int[][] sortedNeighbors;
    private final Vector[] forces;
    private final Vector[] continuousPositions;
    private final Coordinate[] discretePositions;
    private final int[] badIterations;

    private final MosaicCartogram cartogram;
    private final Dual dual;

    public ForceDirectedLayout(MosaicCartogram currentGrid, Dual dual) {
        this.cartogram = currentGrid;
        this.dual = dual;
        double side = currentGrid.getGridMath().getNonNeighborDistance();
        TIME_STEP = 2 * side / (50 * MosaicConstants.FORCE_INTENSITY);
        MAXIMUM_NORM = side / TIME_STEP;

        final int n = currentGrid.numberOfRegions();
        forces = new Vector[n];
        continuousPositions = new Vector[n];
        discretePositions = new Coordinate[n];
        badIterations = new int[n];
        Arrays.fill(badIterations, 0);

        sortedNeighbors = new int[n][];
        for (Vertex u : dual.getVertices()) {
            final int ui = u.getGraphIndex();
            List<Vertex> nbrs = new ArrayList(u.getDegree());
            for (Vertex nbr : u.getNeighbors()) {
                if (nbr.getGraphIndex() > ui) {
                    nbrs.add(nbr);
                }
            }
            sortedNeighbors[ui] = new int[nbrs.size()];
            int ni = 0;
            for (Vertex nbr : nbrs) {
                sortedNeighbors[ui][ni++] = nbr.getGraphIndex();
            }
            Arrays.sort(sortedNeighbors[ui]);
        }

        for (MosaicRegion region : currentGrid.regions()) {
            Vector barycenter = region.getGuidingShape().continuousBarycenter();
            continuousPositions[region.getId()] = barycenter;
            discretePositions[region.getId()] = currentGrid.getGridMath().getContainingCell(barycenter).localRoot();
        }
    }

    public boolean run() {

        boolean somethingchanged = false;

        //Total amount before we stop the looping
        int totalIterations = MosaicConstants.FORCE_TOTAL_ITERATIONS;

        if (MosaicConstants.FORCE_COMPUTE_ONCE) {
            computeForces();
        }

        int sinceLastShake = 0;
        while (!somethingchanged && totalIterations > 0) {
            
            if (++sinceLastShake > MosaicConstants.FORCE_ITERATIONS_BETWEEN_SHAKES) {
                sinceLastShake = 0;
                randomShake();
            }
            totalIterations--;

            for (MosaicRegion region : cartogram.regions()) {
                if (isShakeable(region)) {
                    int count = badIterations[region.getId()]++;
                    if (count > MosaicConstants.FORCE_MAXIMUM_BAD_ITERATIONS) {
                        randomShake(region);
                        badIterations[region.getId()] = 0;
                    }
                }
            }

            if (!MosaicConstants.FORCE_COMPUTE_ONCE) {
                computeForces();
            }

            for (Vertex u : dual.getVertices()) {
                final int uid = u.getGraphIndex();
                
                //move it continously
                Vector positionIncrement = forces[uid];
                Vector continuousPosition = continuousPositions[uid];
                continuousPosition.translate(positionIncrement);

                Coordinate newCoordinate = cartogram.getGridMath().getContainingCell(continuousPosition).localRoot();
                
                Coordinate discretePosition = discretePositions[uid];
                //if it moved discretely
                if (newCoordinate.equals(discretePosition)) {
                    continue;
                }

                //move the guiding shape
                Coordinate translate = newCoordinate.minus(discretePosition);
                MosaicRegion ru = cartogram.getRegion(u);

                ru.translateGuidingShape(translate);

                CoordinateSet au = ru.getAllocated();
                GuidingShape gu = ru.getGuidingShape();
                if (au.touches(gu, AdjacencyType.ROOKS)) {
                    discretePositions[uid] = newCoordinate;
                    somethingchanged = true;
                } else {
                    //guiding shape to far away from region, revert
                    ru.translateGuidingShape(translate.times(-1));
                    continuousPosition.untranslate(positionIncrement);

                }

            }//end of for loop
        } // end of while loop

        return somethingchanged;
    }

    /**
     * Shake all the regions, Returns true if a region has changed
     *
     * @return
     */
    private boolean randomShake() {
        boolean modified = false;
        for (MosaicRegion region : cartogram.regions()) {
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
    private boolean randomShake(MosaicRegion region) {
        GuidingShape guidingShape = region.getGuidingShape();
        if (isShakeable(region)) {
            Coordinate cRegion = region.getAllocated().randomCoordinate().localRoot();
            Coordinate cShape = guidingShape.randomCoordinate().localRoot();

            Coordinate t = cRegion.minus(cShape);
            //shake the guiding shaped based on position of guiding shape and region
            region.translateGuidingShape(t);

            //update variables
            Vector barycenter = guidingShape.continuousBarycenter();
            continuousPositions[region.getId()] = barycenter;
            discretePositions[region.getId()] = cartogram.getGridMath().getContainingCell(barycenter).localRoot();
            return true;
        }
        return false;
    }

    private boolean isShakeable(MosaicRegion region) {
        return region.getSymmetricDifference() >= region.getGuidingShape().size();
    }

    private void computeForces() {

        final List<Vertex> vs = dual.getVertices();
        final int n = vs.size();

        for (int i = 0; i < n; i++) {
            forces[i] = Vector.origin();
        }

        for (int i = 0; i < n; i++) {
            Vertex u = vs.get(i);
            MosaicRegion ru = cartogram.getRegion(u);
            int ni = 0;
            final int[] nbrs = sortedNeighbors[i];

            for (int j = i + 1; j < n; j++) {
                Vertex v = vs.get(j);
                MosaicRegion rv = cartogram.getRegion(v);

                if (ni < nbrs.length && nbrs[ni] == j) {
                    // neighbors!

                    //calculate the forces
                    Vector attraction = attractionForce(ru, rv);
                    forces[i].translate(attraction);
                    forces[j].untranslate(attraction);

                    neighborRepulsionForce(ru, forces[i], rv, forces[j]);

                    ni++;
                } else {
                    // nonneighbors!                    
                    nonNeighborRepulsionForce(ru, forces[i], rv, forces[j]);
                }
            }
        }

        // Truncate forces to maximum norm, and apply timestep
        for (int i = 0; i < n; i++) {
            final double len = forces[i].length();
            if (len > MAXIMUM_NORM) {
                forces[i].scale(MAXIMUM_NORM / len);
            }
            forces[i].scale(TIME_STEP);
        }
    }

    private Vector attractionForce(MosaicRegion r1, MosaicRegion r2) {

        GuidingShape g1 = r1.getGuidingShape();
        GuidingShape g2 = r2.getGuidingShape();

        Vector b1 = g1.continuousBarycenter();
        Vector b2 = g2.continuousBarycenter();

        // direction
        Vector force = Vector.subtract(b2, b1);

        // magnitude
        int distance = g1.distanceTo(g2);
        distance = Math.max(1, distance);
        Utils.zeroSafeNormalize(force);
        force.scale(MosaicConstants.FORCE_INTENSITY * MosaicConstants.FORCE_ATTRACTION_WEIGHT * distance);

        return force;
    }

    private void neighborRepulsionForce(MosaicRegion r1, Vector f1, MosaicRegion r2, Vector f2) {

        GuidingShape g1 = r1.getGuidingShape();
        GuidingShape g2 = r2.getGuidingShape();

        List<Coordinate> is = g1.intersectionWithGuide(g2);
        if (is.isEmpty()) {
            return;
        }

        Vector force = new Vector(0, 0);
        for (Coordinate c : is) {
            Vector p1 = g1.getCorrespondingMapPoint(c);
            Vector p2 = g2.getCorrespondingMapPoint(c);

            // direction based on actual map locations
            Vector component = Vector.subtract(p1, p2);
            Utils.zeroSafeNormalize(component);

            force.translate(component);
        }

        Utils.zeroSafeNormalize(force);
        force.scale(MosaicConstants.FORCE_INTENSITY * MosaicConstants.FORCE_REPULSION_WEIGHT);

        f1.translate(Vector.multiply(1 + is.size() / (double) g1.size(), force));
        f2.untranslate(Vector.multiply(1 + is.size() / (double) g2.size(), force));

    }

    private void nonNeighborRepulsionForce(MosaicRegion r1, Vector f1, MosaicRegion r2, Vector f2) {
        GuidingShape g1 = r1.getGuidingShape();
        GuidingShape g2 = r2.getGuidingShape();

        // NB: old code just use some intersection here. For really large maps, this may be preferable
        // Alternative would be to maintain some sort of bb per guiding shape (which doesnt reshape, so easy to update!)
        // and test with that first whether there can be an intersection
        List<Coordinate> is = g1.intersectionWithGuide(g2);
        if (is.isEmpty()) {
            return;
        }

        Vector force = Vector.origin();
        for (Coordinate c : is) {
            // direction based on actual map locations
            Vector p1 = g1.getCorrespondingMapPoint(c);
            Vector p2 = g2.getCorrespondingMapPoint(c);
            Vector comp = Vector.subtract(p1, p2);
            Utils.zeroSafeNormalize(comp);
            force.translate(comp);
        }

        Utils.zeroSafeNormalize(force);
        force.scale(MosaicConstants.FORCE_INTENSITY * MosaicConstants.FORCE_NON_NEIGHBOR_REPULSION_WEIGHT);

        f1.translate(Vector.multiply(1 + is.size() / (double) g1.size(), force));
        f2.untranslate(Vector.multiply(1 + is.size() / (double) g2.size(), force));
    }
}
