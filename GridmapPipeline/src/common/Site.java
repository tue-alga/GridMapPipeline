package common;

import common.gridmath.GridMath.Coordinate;
import java.awt.Color;
import nl.tue.geometrycore.geometry.Vector;

/**
 * Represents a point to be represented as a cell in the grid map. Typically,
 * this is the centroid or some other representative point of a region. It
 * further has a textual label, and optionally a color to be used in drawing the
 * map.
 *
 * @author Max Sondag, Wouter Meulemans
 */
public class Site extends Vector {

    private String label;
    private Color color;
    private Outline outline; // containing outline
    private Partition partition; // containing partition
    private Coordinate cell; // assigned cell
    private String layer;

    public Site(Vector v, String label, Color color) {
        this(v.getX(), v.getY(), label, color);
    }

    public Site(double x, double y, String label) {
        this(x, y, label, null);
    }

    public Site(double x, double y, String label, Color color) {
        super(x, y);
        this.label = label;
        this.color = color;
        this.outline = null;
        this.partition = null;
        this.cell = null;
        this.layer = null;

    }

    public String getLabel() {
        return label;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Outline getOutline() {
        return outline;
    }

    public void setOutline(Outline partition) {
        this.outline = partition;
    }

    public Partition getPartition() {
        return partition;
    }

    public void setPartition(Partition partition) {
        this.partition = partition;
    }

    public Coordinate getCell() {
        return cell;
    }

    public void setCell(Coordinate cell) {
        this.cell = cell;
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    @Override
    public String toString() {
        return "Site[" + getX() + " " + getY() + " " + label + " " + color + " " + outline + " " + cell + " " + layer + ']';
    }

    public String labelXYColor() {
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();
        return label + "\t" + getX() + "\t" + getY() + "\t" + red + "\t" + green + "\t" + blue;
    }

    public String partitionLabelXYColor() {
        return outline.label + "\t" + label + "\t" + getX() + "\t" + getY() + "\t" + color.getRed() + "\t" + color.getGreen() + "\t" + color.getBlue();
    }

}
