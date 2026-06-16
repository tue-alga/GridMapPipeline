
package io;

import common.Site;
import common.SiteMap;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.geometry.linear.Rectangle;
import nl.tue.geometrycore.geometryrendering.styling.Dashing;
import nl.tue.geometrycore.io.svg.SVGWriter;

/**
 *
 * @author Wouter Meulemans (w.meulemans@tue.nl)
 */
public class SVG {

    public static void write(File file, SiteMap map) {
        
        
        try (SVGWriter write = SVGWriter.fileWriter(file)) {
            Rectangle world = new Rectangle();
            for (Site s : map.sites()) {
                world.includeGeometry(s.getCell().getBoundary());
            }
            
            Rectangle view = world.clone();
            view.untranslate(view.leftBottom());
            view.scale(8);
            
            write.setTransformation(world, view);
            
            write.initialize();
            
            write.setStroke(Color.black, 0.25, Dashing.SOLID);
            
            for (Site s : map.sites()) {
                Polygon p = s.getCell().getBoundary();
                write.setCustomAttributes("label=\""+s.getLabel()+"\"");
                write.draw(p);
            }
            
        } catch (IOException ex) {
            Logger.getLogger(SVG.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
