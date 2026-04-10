package evaluate.sub;

import common.SiteMap;
import evaluate.Script;
import evaluate.Evaluation;
import io.WKT;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Rectangle;
import nl.tue.geometrycore.geometryrendering.styling.Dashing;
import nl.tue.geometrycore.geometryrendering.styling.TextAnchor;
import nl.tue.geometrycore.io.ipe.IPEWriter;

/**
 *
 * @author Wouter Meulemans
 */
public class Teaser extends Evaluation {

    public Teaser() {
        super("Teaser");
    }

    @Override
    public void addScripts(List<Script> scripts) {
        // nothing
    }

    @Override
    protected void computeOutcomes(IPEWriter write) throws IOException {
        SiteMap map = WKT.read(new File(baseroot + "uk-HEXAGON.wkt"));

        double w = 96;
        double h = 155;
        double s = 4;

        Vector o = new Vector(10, 50);

        Rectangle input = Rectangle.byCornerAndSize(o, w, h);
        Rectangle partition = Rectangle.byCornerAndSize(Vector.add(o, Vector.right(w + s)), w, h);
        Rectangle arrange = Rectangle.byCornerAndSize(Vector.add(o, Vector.right(2 * (w + s))), w, h);
        Rectangle assign = Rectangle.byCornerAndSize(Vector.add(o, Vector.right(3 * (w + s))), w, h);
        Rectangle compose = Rectangle.byCornerAndSize(Vector.add(o, Vector.right(4 * (w + s))), w, h);

        write.newPage("input", "sites", "partition", "arrange", "assign", "compose", "labels");

        write.setLayer("input");
        outlines(write, map, input);

        write.setLayer("sites");
        sites(write, map, false, input);

        write.setLayer("partition");
        partition(write, map, partition, true);

        write.setLayer("arrange");
        cartogram(write, map, arrange, true, false, null);

        write.setLayer("assign");
        assignment(write, map, assign, true, false, null);

        write.setLayer("compose");
        assignment(write, map, compose, false, false, null);

        write.setLayer("labels");
        write.setTextStyle(TextAnchor.BASELINE_CENTER, 9);
        write.setStroke(Color.black, 0.2, Dashing.SOLID);
        double off = 5;
        write.draw(Vector.add(input.leftBottom(), new Vector(w / 2, -off)), "input");
        write.draw(Vector.add(partition.leftBottom(), new Vector(w / 2, -off)), "partition");
        write.draw(Vector.add(arrange.leftBottom(), new Vector(w / 2, -off)), "arrange");
        write.draw(Vector.add(assign.leftBottom(), new Vector(w / 2, -off)), "assign");
        write.draw(Vector.add(compose.leftBottom(), new Vector(w / 2, -off)), "compose");
    }

}
