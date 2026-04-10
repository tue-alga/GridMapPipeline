package arrange;

import common.gridmath.AdjacencyType;
import nl.tue.geometrycore.gui.debug.DebugRenderer;

/**
 *
 * @author Wouter Meulemans
 */
public class MosaicConstants {

    // parameters for mosaic heuristic
    public static final double SCALING_FACTOR = Math.sqrt(2);
    public static int INITIAL_AVG_WEIGHT = 10; // MosaicCartograms paper suggests 20, gridmaps pipeline used 10
    public static int MAX_NO_IMPROVE_SCALED = 5000;
    public static int MAX_NO_IMPROVE_FINAL = 5000;
    public static boolean ALLOW_SELF_REFINEMENT = false;

    // parameters for guiding shapes
    public static final int GUIDING_SHAPE_SAMPLES = 5; // NB: number of samples is this parameter, squared
    public static final int GUIDING_SHAPE_THREADS = 5; // NB: <= 1 disables threadpooling altogether

    // for the force directed layout
    public static double FORCE_INTENSITY = 150.0;
    public static final double FORCE_ATTRACTION_WEIGHT = 1; //attraction force between two neighboring regions
    public static double FORCE_REPULSION_WEIGHT = 35; //Force when two neighboring regions overlap    
    public static double FORCE_NON_NEIGHBOR_REPULSION_WEIGHT = 40.0;//force when two non-neighboring regions overlap
    public static final double FORCE_MINIMUM_NORM = 5.0;
    public static int FORCE_TOTAL_ITERATIONS = 20000;
    public static int FORCE_ITERATIONS_BETWEEN_SHAKES = 400;
    public static int FORCE_MAXIMUM_BAD_ITERATIONS = 100;
    public static final boolean FORCE_COMPUTE_ONCE = false;

    // polisher settings
    public static final int POLISHER_MAX_ITERATIONS = 40;
    public static final boolean POLISHER_REPOSITION_GUIDES = false;

    // general settings   
    public static boolean EXACT_TILES = true; // for grid maps, we need exact tiles. Leaving this parameter for more generic use of the code
    public static final AdjacencyType CONN_WITHIN_REGION = AdjacencyType.ROOKS; // a set of cells is considered connected via this topology
    public static final AdjacencyType CONN_BETWEEN_REGIONS = AdjacencyType.ROOKS; // two adjacent regions should keep connected via this topology
    public static AdjacencyType CONN_NONADJACENT_AVOIDS = AdjacencyType.ROOKS; // two nonadjacent regions should not be connected via this topology; should be equal to or superset of BETWEEN_REGIONS
    public static final AdjacencyType CONN_EXTERIOR = AdjacencyType.ROOKS; // the exterior is considered connected via this topology

    // DEBUG SETTINGS
    public static DebugRenderer DRAW_INTERMEDIATES = null; // set to null to disable
    public static int DRAW_INTERMEDIATES_DETAIL = -1; // -1 (disabled), 0 (coarse) to 3 (detailed)

    public static void reset() {
        ALLOW_SELF_REFINEMENT = false;
        DRAW_INTERMEDIATES = null;
        DRAW_INTERMEDIATES_DETAIL = -1;
        configureGridMaps();
    }

    public static void configureGridMaps() {

        EXACT_TILES = true;
        INITIAL_AVG_WEIGHT = 10;
        FORCE_INTENSITY = 150.0;
        FORCE_REPULSION_WEIGHT = 35.0;
        FORCE_NON_NEIGHBOR_REPULSION_WEIGHT = 40.0;
        FORCE_ITERATIONS_BETWEEN_SHAKES = 400;
        FORCE_MAXIMUM_BAD_ITERATIONS = 100;
        FORCE_TOTAL_ITERATIONS = 20000;
        MAX_NO_IMPROVE_SCALED = 5000;
        MAX_NO_IMPROVE_FINAL = 5000;
        CONN_NONADJACENT_AVOIDS = AdjacencyType.ROOKS;
    }

    public static void configureMosaicCartograms() {

        EXACT_TILES = false;
        INITIAL_AVG_WEIGHT = 20;
        FORCE_INTENSITY = 100.0;
        FORCE_REPULSION_WEIGHT = 5.0;
        FORCE_NON_NEIGHBOR_REPULSION_WEIGHT = 2.0;
        FORCE_ITERATIONS_BETWEEN_SHAKES = 200;
        FORCE_MAXIMUM_BAD_ITERATIONS = 50;
        FORCE_TOTAL_ITERATIONS = Integer.MAX_VALUE;
        MAX_NO_IMPROVE_SCALED = 500;
        MAX_NO_IMPROVE_FINAL = 3000;
        CONN_NONADJACENT_AVOIDS = AdjacencyType.QUEENS;
    }

}
