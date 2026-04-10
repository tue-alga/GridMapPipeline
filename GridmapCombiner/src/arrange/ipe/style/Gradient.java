package arrange.ipe.style;

import java.awt.Color;
import java.util.ArrayList;
import arrange.ipe.attributes.ColorAttribute;
import arrange.ipe.attributes.MatrixAttribute;
import arrange.ipe.attributes.PointAttribute;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public abstract class Gradient {

    protected String name;
    protected Boolean extend = null;
    protected MatrixAttribute matrix = null;
    protected ArrayList<Stop> stops = new ArrayList<>();

    protected Gradient(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getExtend() {
        return extend;
    }

    public void setExtend(Boolean extend) {
        this.extend = extend;
    }

    public MatrixAttribute getMatrix() {
        return matrix;
    }

    public void setMatrix(MatrixAttribute matrix) {
        this.matrix = matrix;
    }

    public Iterable<Stop> stops() {
        return stops;
    }

    public Stop getStop(int i) {
        return stops.get(i);
    }

    public int getNumStops() {
        return stops.size();
    }

    public void addStop(double offset, ColorAttribute.RGB color) {
        stops.add(new Stop(offset, color));
    }

    public void addStop(double offset, Color color) {
        stops.add(new Stop(offset, color));
    }

    public Stop removeStop(int i) {
        return stops.remove(i);
    }

    public abstract String toXMLString();

    public static final class Axial extends Gradient {

        PointAttribute firstEndpoint = null;
        PointAttribute secondEndpoint = null;

        public Axial(String name) {
            super(name);
        }

        public PointAttribute getFirstEndpoint() {
            return firstEndpoint;
        }

        public void setFirstEndpoint(PointAttribute point) {
            this.firstEndpoint = point;
        }

        public void setFirstEndpoint(double x, double y) {
            firstEndpoint = new PointAttribute(x, y);
        }

        public PointAttribute getSecondEndpoint() {
            return secondEndpoint;
        }

        public void setSecondEndpoint(PointAttribute point) {
            this.secondEndpoint = point;
        }

        public void setSecondEndpoint(double x, double y) {
            secondEndpoint = new PointAttribute(x, y);
        }

        @Override
        public String toXMLString() {
            //MosaicMaps: the code that keeps on giving. Generating gradients for the tiles in principle works
            //however the pdf-render function of ipe either does not translate it 100% correct, or the pdf viewer can't handle it 100% correct.
            //Depending on the color, amount of cells, and zoom level of the pdf-viewer, tiles may appear to overlap by 1 pixel.
            StringBuilder sb = new StringBuilder();
            sb.append("<gradient name=\"");
            sb.append(name);
            sb.append("\" type=\"axial\"");
            if (extend != null) {
                sb.append(" extend=\"");
                if (extend == true) {
                    sb.append("yes");
                } else {
                    sb.append("no");
                }
                sb.append("\"");
            }
            sb.append(" coords=\"");
            sb.append(firstEndpoint.toXMLString());
            sb.append(" ");
            sb.append(secondEndpoint.toXMLString());
            sb.append("\"");
            if (matrix != null) {
                sb.append(" matrix=\"");
                sb.append(matrix.toXMLString());
                sb.append("\"");
            }
            sb.append(">");
            sb.append(System.lineSeparator());
            for (Stop stop : stops) {
                sb.append(stop.toXMLString());
            }
            sb.append("</gradient>");
            sb.append(System.lineSeparator());
            return sb.toString();
        }
    }

    public static final class Radial extends Gradient {

        PointAttribute firstCenter = null;
        Double firstRadius = null;
        PointAttribute secondCenter = null;
        Double secondRadius = null;

        public Radial(String name) {
            super(name);
        }

        public PointAttribute getFirstCenter() {
            return firstCenter;
        }

        public Double getFirstRadius() {
            return firstRadius;
        }

        public void setFirstCircle(PointAttribute center, double radius) {
            this.firstCenter = center;
            this.firstRadius = radius;
        }

        public void setFirstCircle(double x, double y, double radius) {
            this.firstCenter = new PointAttribute(x, y);
            this.firstRadius = radius;
        }

        public PointAttribute getSecondCenter() {
            return secondCenter;
        }

        public double getSecondRadius() {
            return secondRadius;
        }

        public void setSecondCircle(PointAttribute center, double radius) {
            this.secondCenter = center;
            this.secondRadius = radius;
        }

        public void setSecondCircle(double x, double y, double radius) {
            this.secondCenter = new PointAttribute(x, y);
            this.secondRadius = radius;
        }

        @Override
        public String toXMLString() {
            StringBuilder sb = new StringBuilder();
            sb.append("<gradient name=\"");
            sb.append(name);
            sb.append("\" type=\"radial\"");
            if (extend != null) {
                sb.append(" extend=\"");
                if (extend == true) {
                    sb.append("yes");
                } else {
                    sb.append("no");
                }
                sb.append("\"");
            }
            sb.append(" coords=\"");
            sb.append(firstCenter.toXMLString());
            sb.append(" ");
            sb.append(String.format("%.3f", firstRadius));
            sb.append(" ");
            sb.append(secondCenter.toXMLString());
            sb.append(" ");
            sb.append(String.format("%.3f", secondRadius));
            sb.append("\"");
            if (matrix != null) {
                sb.append(" matrix=\"");
                sb.append(matrix.toXMLString());
                sb.append("\"");
            }
            sb.append(">");
            sb.append(System.lineSeparator());
            for (Stop stop : stops) {
                sb.append(stop.toXMLString());
            }
            sb.append("</gradient>");
            sb.append(System.lineSeparator());
            return sb.toString();
        }
    }

    public static final class Stop {

        private final double offset;
        private final ColorAttribute.RGB color;

        public Stop(double offset, ColorAttribute.RGB color) {
            this.offset = offset;
            this.color = color;
        }

        public Stop(double offset, Color color) {
            this.offset = offset;
            this.color = new ColorAttribute.RGB(color);
        }

        public double getOffset() {
            return offset;
        }

        public ColorAttribute.RGB getColor() {
            return color;
        }

        public String toXMLString() {
            StringBuilder sb = new StringBuilder();
            sb.append("<stop offset=\"");
            sb.append(String.format("%.3f", offset));
            sb.append("\" color=\"");
            sb.append(color.toXMLString());
            sb.append("\"/>");
            sb.append(System.lineSeparator());
            return sb.toString();
        }
    }
}
