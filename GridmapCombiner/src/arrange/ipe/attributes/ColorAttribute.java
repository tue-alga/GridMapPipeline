package arrange.ipe.attributes;

import arrange.ipe.style.SymbolicColor;
import java.awt.Color;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public abstract class ColorAttribute {

    public abstract Color getColor();

    public abstract String toXMLString();

    public static final class RGB extends ColorAttribute {

        private final float red;
        private final float green;
        private final float blue;
        private final Color color;

        public RGB(float red, float green, float blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.color = new Color(red, green, blue);
        }

        public RGB(double red, double green, double blue) {
            this((float) red, (float) green, (float) blue);
        }
        
        public RGB(Color color) {
            float[] components = new float[3];
            color.getColorComponents(components);
            this.red = components[0];
            this.green = components[1];
            this.blue = components[2];
            this.color = color;
        }

        public float getRed() {
            return red;
        }

        public float getGreen() {
            return green;
        }

        public float getBlue() {
            return blue;
        }

        @Override
        public Color getColor() {
            return color;
        }

        @Override
        public String toXMLString() {
            return String.format("%.3f %.3f %.3f", red, green, blue);
        }
    }

    public static final class Gray extends ColorAttribute {

        private final float intensity;
        private final Color color;

        public Gray(float intensity) {
            this.intensity = intensity;
            this.color = new Color(intensity, intensity, intensity);
        }

        public Gray(double intensity) {
            this((float) intensity);
        }

        public float getIntensity() {
            return intensity;
        }

        @Override
        public Color getColor() {
            return color;
        }

        @Override
        public String toXMLString() {
            return String.format("%.3f", intensity);
        }
    }

    public static final class Symbolic extends ColorAttribute {

        private final SymbolicColor symbolicColor;

        public Symbolic(SymbolicColor symbolicColor) {
            this.symbolicColor = symbolicColor;
        }

        public SymbolicColor getSymbolicColor() {
            return symbolicColor;
        }

        @Override
        public Color getColor() {
            return symbolicColor.getColorAttribute().getColor();
        }

        @Override
        public String toXMLString() {
            return symbolicColor.getName();
        }
    }

    public static final class Black extends ColorAttribute {

        private static final Color color = new Color(0, 0, 0);

        @Override
        public Color getColor() {
            return color;
        }

        @Override
        public String toXMLString() {
            return "black";
        }
    }

    public static final class White extends ColorAttribute {

        private static final Color color = new Color(1, 1, 1);

        @Override
        public Color getColor() {
            return color;
        }

        @Override
        public String toXMLString() {
            return "white";
        }
    }
}
