/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pipeline;

import java.awt.Color;

/**
 *
 * @author msondag
 */
class Site {

    String label;
    String parent;
    Coordinate c;
    Color color;

    public Site(String label, Coordinate c, Color color) {
        this.label = label;
        this.c = c;
        this.color = color;
    }

    @Override
    public String toString() {
        return parent + "\t" + label + "\t" + c.x + "\t" + c.y + "\t" + color.getRed() + "\t" + color.getGreen() + "\t" + color.getBlue();
    }
}
