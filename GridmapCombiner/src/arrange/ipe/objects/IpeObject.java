package arrange.ipe.objects;

import arrange.ipe.attributes.MatrixAttribute;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public abstract class IpeObject {

    protected String layer = null;
    protected MatrixAttribute matrix = null;
    protected Transformations transformations = null;

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    public MatrixAttribute getMatrix() {
        return matrix;
    }

    public void setMatrix(MatrixAttribute matrix) {
        this.matrix = matrix;
    }

    public Transformations getTransformations() {
        return transformations;
    }

    public void setTransformations(Transformations transformations) {
        this.transformations = transformations;
    }

    public abstract String toXMLString();

    protected String commonAttributes() {
        StringBuilder sb = new StringBuilder();
        if (layer != null) {
            sb.append(" layer=\"");
            sb.append(layer);
            sb.append("\"");
        }
        if (matrix != null) {
            sb.append(" matrix=\"");
            sb.append(matrix.toXMLString());
            sb.append("\"");
        }
        if (transformations != null) {
            sb.append(" transformations=\"");
            switch (transformations) {
                case AFFINE:
                    sb.append("affine");
                    break;
                case RIGID:
                    sb.append("rigid");
                    break;
                case TRANSLATIONS:
                    sb.append("translations");
                    break;
            }
            sb.append("\"");
        }
        return sb.toString();
    }

    public static enum Transformations {

        AFFINE, RIGID, TRANSLATIONS;
    }
}
