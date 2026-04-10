package analyze.analyzers;

import analyze.Analyzer;
import analyze.PartitionQualityMap;
import common.Partition;
import common.SiteMap;
import common.Stage;
import common.gridmath.GridMath.Coordinate;

/**
 *
 * @author Wouter Meulemans
 */
public class GuidingShapeFit extends Analyzer<PartitionQualityMap> {

    public enum Variant {
        countpct,
        quality
    }

    private Variant var;

    public GuidingShapeFit(Variant var) {
        super("GuidingShapeFit-" + var);
        this.var = var;
    }

    @Override
    public PartitionQualityMap run(SiteMap map) {
        if (map.stage < Stage.DEFORMED) {
            return null;
        }

        PartitionQualityMap qmap = new PartitionQualityMap(toString(), true);
        for (Partition p : map.partitions()) {
            switch (var) {
                case countpct:
                    runCount(p, qmap);
                    break;
                case quality:
                    runQuality(p, qmap);
                    break;
            }
        }

        qmap.process();        
        qmap.setMinimum(0);

        return qmap;
    }

    private void runCount(Partition p, PartitionQualityMap qmap) {
        if (p.guide == null) {
            return;
        }
        double cell_pct = p.cells.intersectionSize(p.guide) / (double) p.guide.size();
        qmap.putQuality(p, cell_pct);
    }

    private void runQuality(Partition p, PartitionQualityMap qmap) {
        if (p.guide == null) {
            return;
        }
        double qual = 0;
        for (Coordinate c : p.cells) {
            qual += p.guide.desirability(c);
        }
        double qual_pct = qual / p.guide.getQuality(); 

        qmap.putQuality(p, qual_pct);
    }

}
