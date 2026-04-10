package arrange.ipe.style;

import java.awt.Color;
import arrange.ipe.attributes.ColorAttribute;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public final class SymbolicColor {

    private final String name;
    private final ColorAttribute colorAttribute;

    public SymbolicColor(String name, ColorAttribute.RGB colorAttribute) {
        this.name = name;
        this.colorAttribute = colorAttribute;
    }

    public SymbolicColor(String name, ColorAttribute.Gray colorAttribute) {
        this.name = name;
        this.colorAttribute = colorAttribute;
    }

    public SymbolicColor(String name, Color color) {
        this.name = name;
        this.colorAttribute = new ColorAttribute.RGB(color);
    }

    public String getName() {
        return name;
    }

    public ColorAttribute getColorAttribute() {
        return colorAttribute;
    }

    public String toXMLString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<color name=\"");
        sb.append(name);
        sb.append("\" value=\"");
        sb.append(colorAttribute.toXMLString());
        sb.append("\"/>");
        sb.append(System.lineSeparator());
        return sb.toString();
    }
}
