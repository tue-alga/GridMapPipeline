package io;

import common.SiteMap;
import common.Stage;
import java.io.File;

/**
 *
 * @author Wouter Meulemans
 */
public class IO {
    
    public static SiteMap read(File file) {
        return read(file, Stage.ALL_STAGES);
    }
    
    public static SiteMap read(File file, byte stages) {
        if (file.getName().endsWith(".ipe")) {
            return IPE.read(file, stages);
        } else if (file.getName().endsWith(".wkt")) {
            return WKT.read(file);
        } else {
            System.err.println("Unexpected file extension for reading: " + file.getName());
            return null;
        }
    }
    
    public static void write(File file, SiteMap map) {
        if (file.getName().endsWith(".ipe")) {
            IPE.write(file, map);
        } else if (file.getName().endsWith(".wkt")) {
            WKT.write(file, map);
        } else {
            System.err.println("Unexpected file extension for writing: " + file.getName());
        }
    }
}
