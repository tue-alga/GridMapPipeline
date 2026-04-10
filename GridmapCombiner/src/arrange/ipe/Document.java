package arrange.ipe;

import java.util.ArrayList;
import arrange.ipe.elements.Layer;
import arrange.ipe.objects.IpeObject;
import arrange.ipe.style.StyleSheet;
import arrange.ipe.style.SymbolicColor;
import arrange.ipe.style.SymbolicPen;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public final class Document {

    private Integer version = 70005;
    private String creator = "Java IpeLib";
    private ArrayList<Layer> layers = new ArrayList<>();
    private ArrayList<StyleSheet> styleSheets = new ArrayList<>();
    private ArrayList<IpeObject> objects = new ArrayList<>();

    public Document() {
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Iterable<Layer> layers() {
        return layers;
    }

    public Layer getLayer(int i) {
        return layers.get(i);
    }

    public int getNumLayers() {
        return layers.size();
    }

    public void addLayer(Layer layer) {
        layers.add(layer);
    }

    public Layer removeLayer(int i) {
        return layers.remove(i);
    }

    public Iterable<StyleSheet> styleSheets() {
        return styleSheets;
    }

    public StyleSheet getStyleSheet(int i) {
        return styleSheets.get(i);
    }

    public int getNumStyleSheets() {
        return styleSheets.size();
    }

    public void addStyleSheet(StyleSheet styleSheet) {
        styleSheets.add(styleSheet);
    }

    public StyleSheet removeStyleSheet(int i) {
        return styleSheets.remove(i);
    }

    public Iterable<IpeObject> objects() {
        return objects;
    }

    public IpeObject getObject(int i) {
        return objects.get(i);
    }

    public int getNumObjects() {
        return objects.size();
    }

    public void addObject(IpeObject object) {
        objects.add(object);
    }

    public IpeObject removeObject(int i) {
        return objects.remove(i);
    }

    public SymbolicPen lookUpSymbolicPen(String name) {
        for (int i = styleSheets.size() - 1; i >= 0; i--) {
            StyleSheet styleSheet = styleSheets.get(i);
            SymbolicPen symbolicPen = styleSheet.getSymbolicPen(name);
            if (symbolicPen != null) {
                return symbolicPen;
            }
        }
        return null;
    }

    public SymbolicColor lookUpSymbolicColor(String name) {
        for (int i = styleSheets.size() - 1; i >= 0; i--) {
            StyleSheet styleSheet = styleSheets.get(i);
            SymbolicColor symbolicColor = styleSheet.getSymbolicColor(name);
            if (symbolicColor != null) {
                return symbolicColor;
            }
        }
        return null;
    }

    public String toXMLString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>");
        sb.append(System.lineSeparator());
        sb.append("<!DOCTYPE ipe SYSTEM \"ipe.dtd\">");
        sb.append(System.lineSeparator());
        sb.append("<ipe");
        if (version != null) {
            sb.append(" version=\"");
            sb.append(version);
            sb.append("\"");
        }
        if (creator != null) {
            sb.append(" creator=\"");
            sb.append(creator);
            sb.append("\"");
        }
        sb.append(">");
        sb.append(System.lineSeparator());
        for (StyleSheet styleSheet : styleSheets) {
            sb.append(styleSheet.toXMLString());
        }
        sb.append("<page>");
        sb.append(System.lineSeparator());
        for (Layer layer : layers) {
            sb.append(layer.toXMLString());
        }
        for (IpeObject object : objects) {
            sb.append(object.toXMLString());
        }
        sb.append("</page>");
        sb.append(System.lineSeparator());
        sb.append("</ipe>");
        sb.append(System.lineSeparator());
        return sb.toString();
    }
}
