package arrange.ipe.attributes;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public final class PointAttribute {

    private final double x;
    private final double y;

    public PointAttribute(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String toXMLString() {
        return String.format("%.3f %.3f", x, y);
    }
}
