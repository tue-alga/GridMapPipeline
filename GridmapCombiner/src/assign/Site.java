/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assign;

import java.awt.Color;

/**
 *
 * @author msondag
 */
class Site {

    /**
     * The partition label this site is part of
     */
    String parentLabel;
    /**
     * Label of this site
     */
    String label;
    /**
     * Original position of this site.
     */
    Coordinate c;
    /**
     * Color of this site
     */
    Color color;
    // integration only
    MosaicCell assigned;
    integration.Site original;
    
    

    /**
     * Province of this site. Only used to make layers more easily selectable.
     */
    String province = "noProvince";

    public Site(String parentLabel, String label, Coordinate c, Color color) {
        this.parentLabel = parentLabel;
        this.label = label;
        this.c = c;
        this.color = color;
    }

    @Override
    public String toString() {
        return parentLabel + "\t" + label + "\t" + c + "\t" + color.getRed() + "\t" + color.getGreen() + "\t" + color.getBlue();
    }

}
