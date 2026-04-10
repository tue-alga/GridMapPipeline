package arrange.ipe;

import java.awt.Color;

import arrange.ipe.attributes.ColorAttribute;
import arrange.ipe.attributes.MatrixAttribute;
import arrange.ipe.attributes.PointAttribute;
import arrange.ipe.objects.Group;
import arrange.ipe.objects.IpeObject;
import arrange.ipe.objects.Path;
import arrange.ipe.objects.Path.Operator;
import arrange.ipe.objects.Text;
import arrange.ipe.style.StyleSheet;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public final class IpeUtils {

    /**
     * Rounds down all point to {@code digits} digits in their precision. Does
     * not work for curves
     *
     * @param document
     * @param digits
     */
    public static void setPrecision(Document document, int digits) {
        simplifyDocument(document);

        double precision = Math.pow(10, digits);

        for (int i = 0; i < document.getNumObjects(); i++) {
            IpeObject object = document.getObject(i);
            if (object instanceof Path) {
                Path path = (Path) object;
                for (Operator operator : path.operators()) {
                    if (operator instanceof Path.MoveTo) {
                        Path.MoveTo moveTo = (Path.MoveTo) operator;
                        PointAttribute p = moveTo.getPoint();
                        PointAttribute point = new PointAttribute(Math.floor(p.getX() * precision) / precision, Math.floor(p.getY() * precision) / precision);
                        moveTo.setPoint(point);
                    } else if (operator instanceof Path.LineTo) {
                        Path.LineTo lineTo = (Path.LineTo) operator;
                        PointAttribute p = lineTo.getPoint();
                        PointAttribute point = new PointAttribute(Math.floor(p.getX() * precision) / precision, Math.floor(p.getY() * precision) / precision);
                        lineTo.setPoint(point);
                    }
                }
            } else if (object instanceof Text) {
                Text text = (Text) object;
                PointAttribute p = text.getPosition();
                PointAttribute point = new PointAttribute(Math.floor(p.getX() * precision) / precision, Math.floor(p.getY() * precision) / precision);
                text.setPosition(point);
            }
        }
    }

    private IpeUtils() {
    }

    /**
     *
     * Simplifies a document to make it easier to use. The following actions are
     * executed:
     *
     * -All groups are ungrouped and the objects are taken to the top level.
     *
     * -All transformations are applied and the matrices of all objects are set
     * to null.
     */
    public static void simplifyDocument(Document document) {
        for (int i = 0; i < document.getNumObjects(); i++) {
            IpeObject object = document.getObject(i);
            if (object instanceof Path) {
                Path path = (Path) object;
                MatrixAttribute matrix = path.getMatrix();
                if (matrix != null) {
                    path.setMatrix(null);
                    for (Operator operator : path.operators()) {
                        if (operator instanceof Path.MoveTo) {
                            Path.MoveTo moveTo = (Path.MoveTo) operator;
                            PointAttribute point = applyTransformation(moveTo.getPoint(), matrix);
                            moveTo.setPoint(point);
                        } else if (operator instanceof Path.LineTo) {
                            Path.LineTo lineTo = (Path.LineTo) operator;
                            PointAttribute point = applyTransformation(lineTo.getPoint(), matrix);
                            lineTo.setPoint(point);
                        }
                    }
                }
            } else if (object instanceof Text) {
                Text text = (Text) object;
                MatrixAttribute matrix = text.getMatrix();
                if (matrix != null) {
                    text.setMatrix(null);
                    PointAttribute point = applyTransformation(text.getPosition(), matrix);
                    text.setPosition(point);
                }
            } else if (object instanceof Group) {
                document.removeObject(i);
                i--; // Other objects were shifted
                Group group = (Group) object;
                MatrixAttribute matrix = group.getMatrix();
                for (IpeObject innerObject : group.objects()) {
                    innerObject.setLayer(object.getLayer());
                    document.addObject(innerObject);
                    if (matrix != null) {
                        MatrixAttribute innerMatrix = innerObject.getMatrix();
                        if (innerMatrix != null) {
                            // Multiply the two transformation matrices
                            double[] newMatrix = new double[6];
                            newMatrix[0] = matrix.get(0) * innerMatrix.get(0) + matrix.get(1) * innerMatrix.get(2);
                            newMatrix[1] = matrix.get(0) * innerMatrix.get(1) + matrix.get(1) * innerMatrix.get(3);
                            newMatrix[2] = matrix.get(2) * innerMatrix.get(0) + matrix.get(3) * innerMatrix.get(2);
                            newMatrix[3] = matrix.get(2) * innerMatrix.get(1) + matrix.get(3) * innerMatrix.get(3);
                            newMatrix[4] = matrix.get(0) * innerMatrix.get(4) + matrix.get(1) * innerMatrix.get(5) + matrix.get(4);
                            newMatrix[5] = matrix.get(2) * innerMatrix.get(4) + matrix.get(3) * innerMatrix.get(5) + matrix.get(5);
                            innerObject.setMatrix(new MatrixAttribute(newMatrix));
                        } else {
                            innerObject.setMatrix(matrix);
                        }
                    }
                }
            }
        }
    }

    /**
     * Applies the transformation to the given vector. Returns a new transformed
     * vector. The original arguments remain the same.
     *
     * The matrix consists of 6 double values: {a, b, c, d, e, f}. It has to be
     * read as:
     *
     * | a c e |
     * | b d f |
     * | 0 0 1 |
     *
     * Every point (x, y) is treated as a column vector
     *
     * | x |
     * | y |
     * | 1 |
     *
     * The resulting point is the matrix multiplication of the two:
     */
    // | a c e |   | x |   |ax + cy + e|
    // | b d f | x | y | = |bx + dy + f|
    // | 0 0 1 |   | 1 |   |     1     |
    public static PointAttribute applyTransformation(PointAttribute v, MatrixAttribute matrix) {
        double x = matrix.get(0) * v.getX() + matrix.get(2) * v.getY() + matrix.get(4);
        double y = matrix.get(1) * v.getX() + matrix.get(3) * v.getY() + matrix.get(5);
        return new PointAttribute(x, y);
    }

    /**
     * Returns a new instance of the basic style sheet that is created by Ipe by
     * default.
     */
    public static StyleSheet basicStyleSheet() {
        StyleSheet basic = new StyleSheet("basic");
        ///////////////////////////// Symbolic pens ////////////////////////////
        basic.addSymbolicPen("heavier", 0.8);
        basic.addSymbolicPen("fat", 1.2);
        basic.addSymbolicPen("ultrafat", 2.0);
        //////////////////////////// Symbolic colors ///////////////////////////
        basic.addSymbolicColor("red", new ColorAttribute.RGB(1, 0, 0));
        basic.addSymbolicColor("green", new ColorAttribute.RGB(0, 1, 0));
        basic.addSymbolicColor("blue", new ColorAttribute.RGB(0, 0, 1));
        basic.addSymbolicColor("yellow", new ColorAttribute.RGB(1, 1, 0));
        basic.addSymbolicColor("orange", new ColorAttribute.RGB(1, 0.647, 0));
        basic.addSymbolicColor("gold", new ColorAttribute.RGB(1, 0.843, 0));
        basic.addSymbolicColor("purple", new ColorAttribute.RGB(0.627, 0.125, 0.941));
        basic.addSymbolicColor("gray", new ColorAttribute.Gray(0.745));
        basic.addSymbolicColor("brown", new ColorAttribute.RGB(0.647, 0.165, 0.165));
        basic.addSymbolicColor("navy", new ColorAttribute.RGB(0, 0, 0.502));
        basic.addSymbolicColor("pink", new ColorAttribute.RGB(1, 0.753, 0.796));
        basic.addSymbolicColor("seagreen", new ColorAttribute.RGB(0.18, 0.545, 0.341));
        basic.addSymbolicColor("turquoise", new ColorAttribute.RGB(0.251, 0.878, 0.816));
        basic.addSymbolicColor("violet", new ColorAttribute.RGB(0.933, 0.51, 0.933));
        basic.addSymbolicColor("darkblue", new ColorAttribute.RGB(0, 0, 0.545));
        basic.addSymbolicColor("darkcyan", new ColorAttribute.RGB(0, 0.545, 0.545));
        basic.addSymbolicColor("darkgray", new ColorAttribute.Gray(0.663));
        basic.addSymbolicColor("darkgreen", new ColorAttribute.RGB(0, 0.392, 0));
        basic.addSymbolicColor("darkmagenta", new ColorAttribute.RGB(0.545, 0, 0.545));
        basic.addSymbolicColor("darkorange", new ColorAttribute.RGB(1, 0.549, 0));
        basic.addSymbolicColor("darkred", new ColorAttribute.RGB(0.545, 0, 0));
        basic.addSymbolicColor("lightblue", new ColorAttribute.RGB(0.678, 0.847, 0.902));
        basic.addSymbolicColor("lightcyan", new ColorAttribute.RGB(0.878, 1, 1));
        basic.addSymbolicColor("lightgray", new ColorAttribute.Gray(0.827));
        basic.addSymbolicColor("lightgreen", new ColorAttribute.RGB(0.565, 0.933, 0.565));
        basic.addSymbolicColor("lightyellow", new ColorAttribute.RGB(1, 1, 0.878));
        return basic;
    }

    public static void addColorBrewerColors(StyleSheet sheet) {
        sheet.addSymbolicColor("cyan", new Color(141, 211, 199));
        sheet.addSymbolicColor("lightyellow", new Color(255, 255, 179));
        sheet.addSymbolicColor("lightpurple", new Color(190, 186, 218));
        sheet.addSymbolicColor("red", new Color(251, 128, 114));
        sheet.addSymbolicColor("seablue", new Color(128, 177, 211));
        sheet.addSymbolicColor("orange", new Color(253, 180, 98));
        sheet.addSymbolicColor("green", new Color(179, 222, 105));
        sheet.addSymbolicColor("pink", new Color(252, 205, 229));
        sheet.addSymbolicColor("gray", new Color(217, 217, 217));
        sheet.addSymbolicColor("purple", new Color(188, 128, 189));
        sheet.addSymbolicColor("lightgreen", new Color(204, 235, 197));
        sheet.addSymbolicColor("yellow", new Color(255, 237, 111));
    }
}
