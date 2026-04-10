/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assign;

/**
 * A single cell in a mosaic catogram
 *
 * @author msondag
 */
public class MosaicCell {

    //coordinates of the centroid.
    public double x, y;
    //which group it belong it.
    public String label;
    public arrange.model.Cartogram.MosaicCartogram.Coordinate coord; // backmapping

    public MosaicCell(arrange.model.Cartogram.MosaicCartogram.Coordinate coord, String label) {
        this.coord = coord;
        this.x = coord.toVector2D().getX();
        this.y = coord.toVector2D().getY();
        this.label = label;
    }

    public MosaicCell(double x, double y, String label) {
        this.x = x;
        this.y = y;
        this.label = label;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }

    public void minus(double x, double y) {
        this.x -= x;
        this.y -= y;
    }

    public void scale(double scaleX, double scaleY) {
        x = x * scaleX;
        y = y * scaleY;
    }

}
