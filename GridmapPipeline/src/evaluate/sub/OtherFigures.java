package evaluate.sub;

import arrange.DeformAlgorithm;
import assign.AssignAlgorithm;
import assign.alignments.UniformAgnosticAlignment;
import common.SiteMap;
import common.gridmath.grids.HexagonGeometry;
import evaluate.Evaluation;
import static evaluate.Evaluation.assignment;
import static evaluate.Evaluation.partition;
import static evaluate.Evaluation.sites;
import evaluate.GridmapScript;
import evaluate.Script;
import io.WKT;
import java.io.File;
import java.io.IOException;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Rectangle;
import nl.tue.geometrycore.io.ipe.IPEWriter;
import partition.CutType;
import partition.PartitionAlgorithm;

/**
 *
 * @author Wouter Meulemans
 */
public class OtherFigures extends Evaluation {

    public OtherFigures() {
        super("OtherFigures");
    }

    @Override
    public void addScripts(List<Script> scripts) {
        GridmapScript am = new GridmapScript(dataroot + "americas.wkt", outroot + "americas-hex");
        am.setPartition(new PartitionAlgorithm(CutType.COMBINED, 3, 5));
        am.setDeform(new DeformAlgorithm(new HexagonGeometry()));
        am.setAssign(new AssignAlgorithm(new UniformAgnosticAlignment(), null));
        scripts.add(am);
    }

    @Override
    protected void computeOutcomes(IPEWriter write) throws IOException {
        String[] maps = {
            "americas-hex"
        };
        
        int mapindex =0;
        for (String m : maps) {

            mapindex++;
            System.out.println("Map " + m);
            SiteMap map = WKT.read(new File(outroot + m + ".wkt"));
           
            double w = 160;
            double h = 250;
            double sw = 4;
            double sh = 20;
            double off = 6;

            Vector o = new Vector(16, 450);

            int r = 0;
            int c = 0;

            String[] layers = new String[5];
            layers[0] = "partition";
            layers[1] = "sites";
            layers[2] = "result";
            layers[3] = "labels";
            layers[4] = "partboundaries";

            write.newPage(layers);

            {
                Rectangle rect = Rectangle.byCornerAndSize(Vector.addSeq(o, Vector.right(c * (w + sw)), Vector.down(r * (h + sh))), w, h);

                write.setLayer(layers[0]);
                partition(write, map, rect, false);

                write.setLayer(layers[1]);
                sites(write, map, true, rect);
            }
            c++;

            {
                Rectangle rect = Rectangle.byCornerAndSize(Vector.addSeq(o, Vector.right(c * (w + sw)), Vector.down(r * (h + sh))), w, h);
                write.setLayer(layers[2]);
                assignment(write, map, rect, false, true, layers[4]);
            }
            c++;
        }
        
    }

}
