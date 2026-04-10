package arrange.ipe.attributes;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public final class MatrixAttribute {

    private final double[] matrix;

    public MatrixAttribute() {
        matrix = new double[]{1, 0, 0, 1, 0, 0};
    }

    public MatrixAttribute(double[] matrix) {
        this.matrix = matrix.clone();
    }

    public double get(int i) {
        return matrix[i];
    }

    public double getScaleX() {
        return matrix[0];
    }

    public double getScaleY() {
        return matrix[3];
    }

    public double getShearX() {
        return matrix[2];
    }

    public double getShearY() {
        return matrix[1];
    }

    public double getTranslateX() {
        return matrix[4];
    }

    public double getTranslateY() {
        return matrix[5];
    }

    public void setScale(double scaleX, double scaleY) {
        matrix[0] = scaleX;
        matrix[3] = scaleY;
    }

    public void setShear(double shearX, double shearY) {
        matrix[2] = shearX;
        matrix[1] = shearY;
    }

    public void setTranslation(double translationX, double translationY) {
        matrix[4] = translationX;
        matrix[5] = translationY;
    }

    public String toXMLString() {
        StringBuilder sb = new StringBuilder();
        sb.append(matrix[0]);
        for (int i = 1; i < 6; i++) {
            sb.append(" ");
            sb.append(matrix[i]);
        }
        return sb.toString();
    }
}
