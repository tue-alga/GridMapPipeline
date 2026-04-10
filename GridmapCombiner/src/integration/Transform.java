/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package integration;

import nl.tue.geometrycore.geometry.BaseGeometry;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Rectangle;

/**
 *
 * @author wmeulema
 */
public class Transform {

    private final Vector translate;
    private final double scaleX, scaleY;

    public Transform(Vector translate, double scale) {
        this.translate = translate;
        this.scaleX = scale;
        this.scaleY = scale;
    }

    public Transform(Vector translate, double scaleX, double scaleY) {
        this.translate = translate;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    @Override
    public String toString() {
        return "Transform[d " + translate.getX() + " " + translate.getY() + ", s " + scaleX + " " + scaleY + "]";
    }

    public <T extends BaseGeometry<T>> void applyDirect(T geom) {
        geom.scale(scaleX, scaleY);
        geom.translate(translate);
    }

    public <T extends BaseGeometry<T>> void inverseDirect(T geom) {
        geom.translate(-translate.getX(), -translate.getY());
        geom.scale(1.0 / scaleX, 1.0 / scaleY);
    }

    public <T extends BaseGeometry<T>> T apply(T geom) {
        T clone = geom.clone();
        applyDirect(clone);
        return clone;
    }

    public <T extends BaseGeometry<T>> T inverse(T geom) {
        T clone = geom.clone();
        inverseDirect(clone);
        return clone;
    }

    public static Transform fitToBox(Rectangle source, Rectangle target) {

        // we first scale to be the same size
        double scale = Math.min(target.width() / source.width(), target.height() / source.height());

        Vector c = source.center();
        c.scale(scale);

        // then translate to align
        Vector translate = Vector.subtract(target.center(), c);
        return new Transform(translate, scale);
    }

}
