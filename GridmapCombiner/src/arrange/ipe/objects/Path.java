package arrange.ipe.objects;

import java.awt.Color;
import java.util.ArrayList;
import arrange.ipe.attributes.ColorAttribute;
import arrange.ipe.attributes.PenAttribute;
import arrange.ipe.attributes.PointAttribute;
import arrange.ipe.style.Gradient;
import arrange.ipe.style.Opacity;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public final class Path extends IpeObject {

    private ArrayList<Operator> operators = new ArrayList<>();
    private ColorAttribute strokeColor = null;
    private ColorAttribute fillColor = new ColorAttribute.White();
    private PenAttribute pen = null;
    private FillRule fillRule = null;
    private Gradient gradient = null;
    private Opacity opacity = null;
    private String name = null;

    public void moveTo(double x, double y) {
        operators.add(new MoveTo(x, y));
    }

    public void lineTo(double x, double y) {
        operators.add(new LineTo(x, y));
    }

    public void closePath() {
        operators.add(new ClosePath());
    }

    public Iterable<Operator> operators() {
        return operators;
    }

    public Operator getOperator(int i) {
        return operators.get(i);
    }

    public int getNumOperators() {
        return operators.size();
    }

    public void addOperator(Operator operator) {
        operators.add(operator);
    }

    public Operator removeOperator(int i) {
        return operators.remove(i);
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

    public ColorAttribute getFillColor() {
        return fillColor;
    }

    public void setFillColor(ColorAttribute fillColor) {
        this.fillColor = fillColor;
    }

    public void setFillColor(Color color) {
        this.fillColor = new ColorAttribute.RGB(color);
    }

    public PenAttribute getPen() {
        return pen;
    }

    public void setPen(PenAttribute pen) {
        this.pen = pen;
    }

    public void setPen(float width) {
        this.pen = new PenAttribute.Real(width);
    }

    public void setPen(double width) {
        this.pen = new PenAttribute.Real(width);
    }

    public FillRule getFillRule() {
        return fillRule;
    }

    public void setFillRule(FillRule fillRule) {
        this.fillRule = fillRule;
    }

    public Gradient getGradient() {
        return gradient;
    }

    public void setGradient(Gradient gradient) {
        this.gradient = gradient;
    }

    public Opacity getOpacity() {
        return opacity;
    }

    public void setOpacity(Opacity opacity) {
        this.opacity = opacity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toXMLString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<path");
        sb.append(commonAttributes());
        if (strokeColor != null) {
            sb.append(" stroke=\"");
            sb.append(strokeColor.toXMLString());
            sb.append("\"");
        }
        if (name != null) {
            sb.append(" name=\"");
            sb.append(name);
            sb.append("\"");
        }
        if (fillColor != null) {
            sb.append(" fill=\"");
            sb.append(fillColor.toXMLString());
            sb.append("\"");
        }
        if (pen != null) {
            sb.append(" pen=\"");
            sb.append(pen.toXMLString());
            sb.append("\"");
        }
        if (fillRule != null) {
            sb.append(" fillrule=\"");
            if (fillRule == FillRule.WIND) {
                sb.append("wind");
            } else if (fillRule == FillRule.EOFILL) {
                sb.append("eofill");
            }
            sb.append("\"");
        }
        if (gradient != null) {
            sb.append(" gradient=\"");
            sb.append(gradient.getName());
            sb.append("\"");
        }
        if (opacity != null) {
            sb.append(" opacity=\"");
            sb.append(opacity.getName());
            sb.append("\"");
        }
        sb.append(">");
        sb.append(System.lineSeparator());
        for (Operator op : operators) {
            sb.append(op.toXMLString());
            sb.append(System.lineSeparator());
        }
        sb.append("</path>");
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    public static abstract class Operator {

        public abstract String toXMLString();
    }

    public static final class MoveTo extends Operator {

        private PointAttribute point;

        public MoveTo(PointAttribute point) {
            this.point = point;
        }

        public MoveTo(double x, double y) {
            this.point = new PointAttribute(x, y);
        }

        public PointAttribute getPoint() {
            return point;
        }

        public void setPoint(PointAttribute point) {
            this.point = point;
        }

        @Override
        public String toXMLString() {
            return point.toXMLString() + " m";
        }
    }

    public static final class LineTo extends Operator {

        private PointAttribute point;

        public LineTo(PointAttribute point) {
            this.point = point;
        }

        public LineTo(double x, double y) {
            this.point = new PointAttribute(x, y);
        }

        public PointAttribute getPoint() {
            return point;
        }

        public void setPoint(PointAttribute point) {
            this.point = point;
        }

        @Override
        public String toXMLString() {
            return point.toXMLString() + " l";
        }
    }

    public static final class ClosePath extends Operator {

        @Override
        public String toXMLString() {
            return "h";
        }
    }

    public static enum FillRule {

        WIND, EOFILL;
    }
}
