package arrange.ipe.objects;

import arrange.ipe.attributes.PointAttribute;
import arrange.ipe.style.Symbol;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class SymbolInstance extends IpeObject {

    private Symbol symbol;
    private PointAttribute position = null;
    private String label = null;

    public SymbolInstance(Symbol symbol) {
        this.symbol = symbol;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public IpeObject getSymbolObject() {
        return symbol.getObject();
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

    @Override
    public String toXMLString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<use");
        sb.append(commonAttributes());
        sb.append(" name=\"");
        sb.append(symbol.getName());
        sb.append("\"");
        if (label != null) {
            sb.append(" label=\"");
            sb.append(label);
            sb.append("\"");
        }
        if (position != null) {
            sb.append(" pos=\"");
            sb.append(position.toXMLString());
            sb.append("\"");
        }
        sb.append("/>");
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
