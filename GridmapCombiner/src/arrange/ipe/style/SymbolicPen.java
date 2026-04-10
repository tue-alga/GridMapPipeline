package arrange.ipe.style;

import arrange.ipe.attributes.PenAttribute;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public final class SymbolicPen {

    private final String name;
    private final PenAttribute penAttribute;

    public SymbolicPen(String name, PenAttribute.Real penAttribute) {
        this.name = name;
        this.penAttribute = penAttribute;
    }

    public String getName() {
        return name;
    }

    public PenAttribute getPenAttribute() {
        return penAttribute;
    }

    public String toXMLString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<pen name=\"");
        sb.append(name);
        sb.append("\" value=\"");
        sb.append(penAttribute.toXMLString());
        sb.append("\"/>");
        sb.append(System.lineSeparator());
        return sb.toString();
    }
}
