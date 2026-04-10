package arrange;

import arrange.algorithms.GridEmbedder;
import arrange.algorithms.MosaicFinalizer;
import arrange.algorithms.MosaicSlider;
import arrange.algorithms.MoveAndReshape;
import arrange.model.MosaicCartogram;
import arrange.model.MosaicCartogram.MosaicRegion;
import common.Partition;
import common.SiteMap;
import common.Stage;
import common.dual.Dual;
import common.dual.Vertex;
import common.gridmath.GridGeometry;
import common.gridmath.GridMath;
import common.gridmath.util.CoordinateSet;
import common.util.Stopwatch;
import java.util.Stack;
import partition.PartitionAlgorithm;

/**
 *
 * @author Wouter Meulemans
 */
public class DeformAlgorithm {

    private final GridGeometry grid;

    public DeformAlgorithm(GridGeometry grid) {
        this.grid = grid;
    }

    public void run(SiteMap map) {
        map.revertToStage(Stage.PARTITIONED);

        if (map.stage < Stage.PARTITIONED) {
            PartitionAlgorithm.trivialPartition(map);
        }

        System.out.println("------ DEFORMING INTO CARTOGRAM -------------------");
        System.out.println("  grid = " + grid.toString());

        Stopwatch sw = Stopwatch.get("deform").start();

        map.gridmath = new GridMath(grid);
        if (map.duals == null) {
            map.determineDuals();
        }
        
        for (Dual d : map.duals) {
            run(d, map.gridmath);
        }

        map.stage = Stage.DEFORMED;

        sw.stop();        
    }

    private void run(Dual dual, GridMath gridmath) {

        int nfaces = dual.vertexCount();
        int totalTiles = 0;
        for (Partition p : dual.primalPartitions()) {
            totalTiles += p.getWeight();
        }
        System.out.println("Computing cartogram for " + nfaces + " regions, " + totalTiles + " tiles");

        if (totalTiles == 1 && gridmath.getPatternComplexity() == 1) {

            assert dual.getVertices().size() == 1; // can only go wrong if there are partitions with 0 sites... 

            System.out.println("  Using origin");
            Partition p = dual.getVertices().get(0).getPartition();
            p.cells = new CoordinateSet(gridmath);
            p.cells.add(gridmath.origin());
            p.guide = null;

        } else if (nfaces == 1) {

            System.out.println("  Using guiding shape");
            MosaicCartogram cartogram = new MosaicCartogram(gridmath, dual);
            cartogram.initializeGuidingShapes(1);

            Vertex v = dual.getVertices().get(0);;
            Partition p = v.getPartition();
            p.guide = cartogram.getRegion(v).getGuidingShape();
            p.cells = new CoordinateSet(p.guide);

        } else {

            Stack<GridMath> gms = new Stack();
            Stack<Integer> fs = new Stack();
            int factor = 1;
            while (gridmath.geometry().getPatternlevel() != null) {
                gms.push(gridmath);
                fs.push(factor);

                factor *= gridmath.getPatternComplexity();
                gridmath = new GridMath(gridmath.geometry().getPatternlevel());
                factor /= gridmath.getPatternComplexity();

                System.out.println("Coarsening to " + gridmath.geometry() + ", factor = " + factor);
            }

            MosaicCartogram cartogram = new MosaicCartogram(gridmath, dual);

            GridEmbedder embedder = new GridEmbedder(dual);
            embedder.run(cartogram);

           // cartogram.export("embedded", 0);

            // "number of sites per tiles", will eventually converge to 1 
            // it starts higher to have fewer tiles to compute with, to speed up the process
            double averageTiles = (double) totalTiles / (double) nfaces;
            double currentUnitData = factor * averageTiles / MosaicConstants.INITIAL_AVG_WEIGHT;

            MosaicSlider slider = new MosaicSlider(dual);
            MoveAndReshape heuristic = new MoveAndReshape(dual);

            int srf = gridmath.geometry().getSelfRefinableFactor();

            int it = 1;
            while (currentUnitData > factor * MosaicConstants.SCALING_FACTOR) {

                System.out.println("  Scaling = " + String.format("%.2f", currentUnitData));

                cartogram = runRound(cartogram, slider, heuristic, false, currentUnitData, it);

                if (MosaicConstants.ALLOW_SELF_REFINEMENT && currentUnitData >= srf) {
                    currentUnitData /= srf;
                    cartogram.refineGridMath();
                } else {
                    currentUnitData /= MosaicConstants.SCALING_FACTOR;
                }
                it++;
            }

            System.out.println("  Scaling = " + factor);
            cartogram = runRound(cartogram, slider, heuristic,true, factor, it);
            it++;

            while (!gms.isEmpty()) {

                gridmath = gms.pop();
                factor = fs.pop();

                System.out.println("  Refining = " + factor);
                cartogram.refineGridMath(gridmath);

                cartogram = runRound(cartogram, slider, heuristic, true,factor, it);
                it++;
            }

            System.out.println("  Finalizing");
            MosaicFinalizer finalizer = new MosaicFinalizer(dual, cartogram);
            finalizer.run();
          // cartogram.export("final", 0);

            for (Vertex v : dual.getVertices()) {
                MosaicRegion region = cartogram.getRegion(v);
                Partition p = v.getPartition();
                p.cells = region.getAllocated();
                p.guide = region.getGuidingShape();
            }
        }

    }

    private MosaicCartogram runRound(MosaicCartogram cartogram, MosaicSlider slider, MoveAndReshape heuristic, boolean final_round, double factor, int it) {

        //System.out.println("Iteration " + it);
        //System.out.println("  Guides");
        cartogram.initializeGuidingShapes(factor);
       // cartogram.export("Iteration "+it+": guides done", 0);

        //System.out.println("  Performing slide moves");
        slider.run(cartogram);
       // cartogram.export("Iteration "+it+": slides done", 1);

        //System.out.println("  Move and reshaping");
        cartogram = heuristic.run(cartogram, final_round ? MosaicConstants.MAX_NO_IMPROVE_FINAL : MosaicConstants.MAX_NO_IMPROVE_SCALED); //no need for exact tiles yet
      //  cartogram.export("Iteration "+it+": move and reshape done", 1);

        //System.out.println("  Done");
        return cartogram;
    }
}
