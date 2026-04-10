/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package partition;

import java.awt.Color;
import nl.tue.geometrycore.geometry.Vector;

/**
 *
 * @author msondag
 */
public class Site {
    
    public double x;
    public double y;
    public String label;
    public Vector point;
    public Color color;

    public Site(double x, double y, String label) {
        this.x = x;
        this.y = y;
        this.label = label;
        point = new Vector(x,y);
    }
    
}
