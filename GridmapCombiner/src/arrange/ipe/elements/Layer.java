package arrange.ipe.elements;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class Layer {

    private String name;
    private Boolean editable = null;

    public Layer(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getEditable() {
        return editable;
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

    public String toXMLString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<layer name=\"");
        sb.append(name);
        sb.append("\"");
        if (editable != null) {
            sb.append(" edit=\"");
            if (editable) {
                sb.append("yes");
            } else {
                sb.append("no");
            }
            sb.append("\"");
        }
        sb.append("/>");
        sb.append(System.lineSeparator());
        return sb.toString();
    }
}
