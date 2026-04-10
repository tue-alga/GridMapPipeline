package arrange.ipe.style;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class Opacity {

    private String name;
    private float value;

    public Opacity(String name, float value) {
        this.name = name;
        this.value = value;
    }

    public Opacity(String name, double value) {
        this.name = name;
        this.value = (float) value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public String toXMLString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<opacity name=\"");
        sb.append(name);
        sb.append("\" value=\"");
        sb.append(String.format("%.3f", value));
        sb.append("\"/>");
        sb.append(System.lineSeparator());
        return sb.toString();
    }
}
