
package io;

import common.Site;
import common.SiteMap;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.io.json.GeoJSONWriter;

/**
 *
 * @author Wouter Meulemans (w.meulemans@tue.nl)
 */
public class GeoJSON {

    public static void write(File file, SiteMap map) {        
        
        try (GeoJSONWriter write = GeoJSONWriter.fileWriter(file, false)) {
            write.initialize();
            
            for (Site s : map.sites()) {
                Polygon p = s.getCell().getBoundary();
                String[][] props = {{"label", "\""+s.getLabel() + "\""}};
                write.write(p, props);
            }
            
        } catch (IOException ex) {
            Logger.getLogger(GeoJSON.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
