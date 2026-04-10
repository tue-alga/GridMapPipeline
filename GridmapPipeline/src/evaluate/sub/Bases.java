package evaluate.sub;

import arrange.DeformAlgorithm;
import assign.AssignAlgorithm;
import assign.alignments.UniformAgnosticAlignment;
import combine.CombineAlgorithm;
import combine.combinations.AgglomerativePartitionCombination;
import combine.combinations.AssignedCombination;
import common.gridmath.GridGeometry;
import common.gridmath.grids.HexagonGeometry;
import common.gridmath.grids.SquareGeometry;
import evaluate.Script;
import evaluate.Evaluation;
import evaluate.GridmapScript;
import java.io.IOException;
import java.util.List;
import nl.tue.geometrycore.io.ipe.IPEWriter;
import partition.CutType;
import partition.PartitionAlgorithm;

/**
 *
 * @author Wouter Meulemans
 */
public class Bases extends Evaluation {

    public Bases() {
        super("Bases", false, false);
    }

    @Override
    public void addScripts(List<Script> scripts) {
        GridmapScript gb = new GridmapScript(dataroot + "GB_constituencies.wkt", baseroot + "gb");
        gb.setPartition(new PartitionAlgorithm(CutType.COMBINED, 3, 15));
        scripts.add(gb);

        GridmapScript uk = new GridmapScript(dataroot + "UK_constituencies.wkt", baseroot + "uk");
        uk.setPartition(new PartitionAlgorithm(CutType.COMBINED, 3, 15));
        scripts.add(uk);

        GridmapScript nlmain = new GridmapScript(dataroot + "NL_municipalities2017_main.wkt", baseroot + "nlmain");
        nlmain.setPartition(new PartitionAlgorithm(CutType.COMBINED, 3, 10));
        scripts.add(nlmain);

        GridmapScript nl = new GridmapScript(dataroot + "NL_municipalities2017.wkt", baseroot + "nl");
        nl.setPartition(new PartitionAlgorithm(CutType.COMBINED, 3, 10));
        scripts.add(nl);

        GridGeometry[] geoms = {
            new SquareGeometry(),
            new HexagonGeometry()
        };
        String[] inputs = {"uk", "gb", "nl", "nlmain"};

        AssignAlgorithm assign = new AssignAlgorithm(new UniformAgnosticAlignment(), null);
        CombineAlgorithm combine_nl = new CombineAlgorithm(new AgglomerativePartitionCombination());
        CombineAlgorithm combine_uk = new CombineAlgorithm(new AssignedCombination());

        for (String input : inputs) {
            for (GridGeometry geom : geoms) {
                DeformAlgorithm deform = new DeformAlgorithm(geom);

                GridmapScript s = new GridmapScript(baseroot + input + ".wkt", baseroot + input + "-" + geom);
                s.setDeform(deform);
                s.setAssign(assign);
                s.setCombine(input.startsWith("nl") ? combine_nl : combine_uk);
                scripts.add(s);
            }
        }
    }

    @Override
    protected void computeOutcomes(IPEWriter write) throws IOException {
        // nothing
    }

}
