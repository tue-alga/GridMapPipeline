/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assign;

import integration.Partition;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author msondag
 */
class SiteMap {

    //for each partition with a given label, holds all sites that belong to this list
    HashMap<String, List<Site>> siteMapping = new HashMap();

    public SiteMap(Path coordinatePath) {
        try {
            List<String> sites = Files.readAllLines(coordinatePath);
            readSites(sites);
        } catch (IOException ex) {
            Logger.getLogger(MosaicCellMap.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public SiteMap(integration.SiteMap sitemap) {
        
        for (Partition p : sitemap.partitions()) {
            List<Site> sites = new ArrayList();
            
            for (integration.Site s : p.sites) {                
                Site site  = new Site(p.label, s.getLabel(), new Coordinate(s.getX(), s.getY()), s.getColor());
                site.original = s;
                sites.add(site);
            }
            
            siteMapping.put(p.label, sites);
        }

    }

    //From a list of sites, generate the sitemap.
    private void readSites(List<String> sites) {
        for (String line : sites) {
            String[] split = line.split("\t");
            String parent = split[0];
            String label = split[1];
            double x = Double.parseDouble(split[2]);
            double y = Double.parseDouble(split[3]);
            int red = Integer.parseInt(split[4]);
            int green = Integer.parseInt(split[5]);
            int blue = Integer.parseInt(split[6]);

            Site s = new Site(parent, label, new Coordinate(x, y), new Color(red, green, blue));
            List<Site> siteMap = siteMapping.getOrDefault(parent, new ArrayList<>());
            siteMap.add(s);
            siteMapping.put(parent, siteMap);
        }
    }

    //get a list of sites belong to a partition.
    public List<Site> get(String key) {
        return siteMapping.get(key);
    }

    //get the width of all sites
    public double getSiteWidth() {
        double maxX = Double.MIN_VALUE;
        double minX = Double.MAX_VALUE;
        for (List<Site> sites : siteMapping.values()) {
            for (Site s : sites) {
                maxX = Math.max(maxX, s.c.x);
                minX = Math.min(minX, s.c.x);
            }
        }

        //-1 in case there are no sites
        return Math.max(maxX - minX, -1);
    }

    //get the height of all sites
    public double getSiteHeight() {
        double maxY = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        for (List<Site> sites : siteMapping.values()) {
            for (Site s : sites) {
                maxY = Math.max(maxY, s.c.y);
                minY = Math.min(minY, s.c.y);
            }
        }

        //-1 in case there are no sites
        return Math.max(maxY - minY, -1);
    }

    /**
     * Shifts all sites such that they fit within the bounding box of
     * (0,0,targetW,targetH)
     *
     * @param targetH
     * @param targetW
     */
    public void shiftTarget(double targetH, double targetW) {

        //shift so they are within the start
        shiftToOrigin();

        //scale everything to fit within the box
        double currentW = getSiteWidth();
        double currentH = getSiteHeight();

        double scaleX = targetW / currentW;
        double scaleY = targetH / currentH;
        for (List<Site> sites : siteMapping.values()) {
            for (Site s : sites) {
                s.c.scale(scaleX, scaleY);
            }
        }
    }

    /**
     * Shifts the sites such that they start at the origin.
     */
    private void shiftToOrigin() {
        //find minimum
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        for (List<Site> sites : siteMapping.values()) {
            for (Site s : sites) {
                minX = Math.min(minX, s.c.x);
                minY = Math.min(minY, s.c.y);
            }
        }
        //shift by minimum
        for (List<Site> sites : siteMapping.values()) {
            for (Site s : sites) {
                s.c.x -= minX;
                s.c.y -= minY;
            }
        }
    }

    //return all sites
    public List<Site> getAllSites() {
        ArrayList<Site> sites = new ArrayList();
        for (List<Site> siteList : siteMapping.values()) {
            sites.addAll(siteList);
        }
        return sites;
    }

}
