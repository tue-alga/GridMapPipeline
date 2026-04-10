package arrange.ipe.objects;

import java.awt.Color;
import arrange.ipe.attributes.ColorAttribute;
import arrange.ipe.attributes.PointAttribute;
import arrange.ipe.style.Opacity;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public final class Text extends IpeObject {

    private String text = null;
    private ColorAttribute strokeColor = null;
    private Type type = null;
    private PointAttribute position = null;
    private Double width = null;
    private Double height = null;
    private Double depth = null;
    private VerticalAlignment verticalAlignment = null;
    private HorizontalAlignment horizontalAlignment = null;
    private Opacity opacity = null;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ColorAttribute getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeColor(ColorAttribute strokeColor) {
        this.strokeColor = strokeColor;
    }

    public void setStrokeColor(Color color) {
        this.strokeColor = new ColorAttribute.RGB(color);
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public PointAttribute getPosition() {
        return position;
    }

    public void setPosition(PointAttribute position) {
        this.position = position;
    }

    public void setPosition(double x, double y) {
        position = new PointAttribute(x, y);
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Double getDepth() {
        return depth;
    }

    public void setDepth(Double depth) {
        this.depth = depth;
    }

    public VerticalAlignment getVerticalAlignment() {
        return verticalAlignment;
    }

    public void setVerticalAlignment(VerticalAlignment verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
    }

    public HorizontalAlignment getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public void setHorizontalAlignment(HorizontalAlignment horizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment;
    }

    public Opacity getOpacity() {
        return opacity;
    }

    public void setOpacity(Opacity opacity) {
        this.opacity = opacity;
    }

    @Override
    public String toXMLString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<text");
        sb.append(commonAttributes());
        if (strokeColor != null) {
            sb.append(" stroke=\"");
            sb.append(strokeColor.toXMLString());
            sb.append("\"");
        }
        if (type != null) {
            sb.append(" type=\"");
            if (type == Type.LABEL) {
                sb.append("label");
            } else if (type == Type.MINIPAGE) {
                sb.append("minipage");
            }
            sb.append("\"");
        }
        if (position != null) {
            sb.append(" pos=\"");
            sb.append(position.toXMLString());
            sb.append("\"");
        }
        if (width != null) {
            sb.append(" width=\"");
            sb.append(String.format("%.3f", width));
            sb.append("\"");
        }
        if (height != null) {
            sb.append(" height=\"");
            sb.append(String.format("%.3f", height));
            sb.append("\"");
        }
        if (depth != null) {
            sb.append(" depth=\"");
            sb.append(depth);
            sb.append("\"");
        }
        if (verticalAlignment != null) {
            sb.append(" valign=\"");
            switch (verticalAlignment) {
                case TOP:
                    sb.append("top");
                    break;
                case BOTTOM:
                    sb.append("bottom");
                    break;
                case CENTER:
                    sb.append("center");
                    break;
                case BASELINE:
                    sb.append("baseline");
                    break;
            }
            sb.append("\"");
        }
        if (horizontalAlignment != null) {
            sb.append(" halign=\"");
            switch (horizontalAlignment) {
                case LEFT:
                    sb.append("left");
                    break;
                case RIGHT:
                    sb.append("right");
                    break;
                case CENTER:
                    sb.append("center");
                    break;
            }
            sb.append("\"");
        }
        if (opacity != null) {
            sb.append(" opacity=\"");
            sb.append(opacity.getName());
            sb.append("\"");
        }
        sb.append(">");
        sb.append(System.lineSeparator());
        sb.append(text);
        sb.append(System.lineSeparator());
        sb.append("</text>");
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    public static enum Type {

        LABEL, MINIPAGE;
    }

    public static enum VerticalAlignment {

        TOP, BOTTOM, CENTER, BASELINE;
    }

    public static enum HorizontalAlignment {

        LEFT, RIGHT, CENTER;
    }
}
