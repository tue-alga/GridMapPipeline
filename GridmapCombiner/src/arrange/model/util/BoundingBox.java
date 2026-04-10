package arrange.model.util;

import java.awt.geom.Point2D;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class BoundingBox {

    private double minX = Double.POSITIVE_INFINITY;
    private double maxX = Double.NEGATIVE_INFINITY;
    private double minY = Double.POSITIVE_INFINITY;
    private double maxY = Double.NEGATIVE_INFINITY;

    public BoundingBox() {
    }

    public BoundingBox(BoundingBox other) {
        this.minX = other.minX;
        this.maxX = other.maxX;
        this.minY = other.minY;
        this.maxY = other.maxY;
    }

    public double getMinX() {
        return minX;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxY() {
        return maxY;
    }

    public void add(BoundingBox other) {
        if (this.minX > other.minX) {
            this.minX = other.minX;
        }
        if (this.maxX < other.maxX) {
            this.maxX = other.maxX;
        }
        if (this.minY > other.minY) {
            this.minY = other.minY;
        }
        if (this.maxY < other.maxY) {
            this.maxY = other.maxY;
        }
    }

    public void add(double px, double py) {
        if (this.minX > px) {
            this.minX = px;
        }
        if (this.maxX < px) {
            this.maxX = px;
        }
        if (this.minY > py) {
            this.minY = py;
        }
        if (this.maxY < py) {
            this.maxY = py;
        }
    }

    public void add(Vector2D v) {
        add(v.getX(), v.getY());
    }

    public void add(Point2D p) {
        add(p.getX(), p.getY());
    }

    public void add(Position2D p) {
        Vector2D v = p.getPosition();
        add(v.getX(), v.getY());
    }

    public void translate(double tx, double ty) {
        minX += tx;
        maxX += tx;
        minY += ty;
        maxY += ty;
    }

    public void translate(Vector2D t) {
        translate(t.getX(), t.getY());
    }

    public boolean intersects(BoundingBox other) {
        return (this.maxX > other.minX && this.minX < other.maxX
                && this.maxY > other.minY && this.minY < other.maxY);
    }
}
