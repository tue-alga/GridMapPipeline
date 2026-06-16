package io;

import common.Site;
import common.SiteMap;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Wouter Meulemans (w.meulemans@tue.nl)
 */
public class TSVGrid {

    public static void write(File file, SiteMap map) {

        try (BufferedWriter write = new BufferedWriter(new FileWriter(file))) {

            for (Site s : map.sites()) {
                write.write(s.getCell().x + "\t" + s.getCell().y + "\t" + s.getLabel() + "\n");
            }

        } catch (IOException ex) {
            Logger.getLogger(TSVGrid.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
