package arrange.ipe.attributes;

import arrange.ipe.style.SymbolicPen;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public abstract class PenAttribute {

    public abstract float getWidth();

    public abstract String toXMLString();

    public static final class Real extends PenAttribute {

        private final float width;

        public Real(float width) {
            this.width = width;
        }

        public Real(double width) {
            this((float) width);
        }

        @Override
        public float getWidth() {
            return width;
        }

        @Override
        public String toXMLString() {
            return String.format("%.2f", width);
        }
    }

    public static final class Symbolic extends PenAttribute {

        private final SymbolicPen symbolicPen;

        public Symbolic(SymbolicPen symbolicPen) {
            this.symbolicPen = symbolicPen;
        }

        public SymbolicPen getSymbolicPen() {
            return symbolicPen;
        }

        @Override
        public float getWidth() {
            return symbolicPen.getPenAttribute().getWidth();
        }

        @Override
        public String toXMLString() {
            return symbolicPen.getName();
        }
    }
}
