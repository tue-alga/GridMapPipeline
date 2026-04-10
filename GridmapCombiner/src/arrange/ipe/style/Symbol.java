package arrange.ipe.style;

import arrange.ipe.objects.Group;
import arrange.ipe.objects.IpeObject;
import arrange.ipe.objects.Path;
import arrange.ipe.objects.Text;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class Symbol {

    private String name;
    private IpeObject object = null;
    private Boolean xForm = null;

    public Symbol(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public IpeObject getObject() {
        return object;
    }

    public void setObject(Group object) {
        this.object = object;
    }

    public void setObject(Path object) {
        this.object = object;
    }

    public void setObject(Text object) {
        this.object = object;
    }

    public Boolean getXForm() {
        return xForm;
    }

    public void setXForm(Boolean xForm) {
        this.xForm = xForm;
    }

    public String toXMLString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<symbol name=\"");
        sb.append(name);
        sb.append("\"");
        if (xForm != null) {
            sb.append(" xform=\"");
            if (xForm == true) {
                sb.append("yes\"");
            } else {
                sb.append("no\"");
            }
        }
        sb.append(">");
        sb.append(System.lineSeparator());
        sb.append(object.toXMLString());
        sb.append("</symbol>");
        sb.append(System.lineSeparator());
        return sb.toString();
    }
}
