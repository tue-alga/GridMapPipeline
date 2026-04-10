package io;

import common.Outline;
import common.Site;
import common.SiteMap;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Wouter Meulemans
 */
public class TSV {

    /**
     * Reads a tsv file containing sites (label, x, y, r, g, b). r,g,b is
     * optional, black is assigned if not specified.
     *
     * @param f
     * @param map
     */
    public static void readSites(File f, SiteMap map) {
        try {
            List<String> lines = Files.readAllLines(f.toPath());
            for (String line : lines) {
                String[] split = line.split("\t");
                String label = split[0];
                double x = Double.parseDouble(split[1]);
                double y = Double.parseDouble(split[2]);
                Site s = new Site(x, y, label);
                if (split.length > 3) {
                    s.setColor(new Color(
                            Integer.parseInt(split[3]),
                            Integer.parseInt(split[4]),
                            Integer.parseInt(split[5])));
                } else {
                    s.setColor(Color.black);
                }

                Outline p = map.findOutline(s);
                if (p != null) {
                    p.sites.add(s);
                    s.setOutline(p);
                } else {
                    System.out.println("No partition found for " + s);
                }

            }
        } catch (IOException ex) {
            Logger.getLogger(TSV.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static Map<String, Double> readWeightMap(File file) {
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            Map<String, Double> map = new HashMap();
            for (String line : lines) {
                if (line.startsWith("#")) {
                    continue;
                }
                String[] split = line.split("\t");
                if (split.length == 2) {
                    map.put(split[0], Double.parseDouble(split[1]));
                }
            }
            return map;
        } catch (IOException ex) {
            Logger.getLogger(TSV.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
