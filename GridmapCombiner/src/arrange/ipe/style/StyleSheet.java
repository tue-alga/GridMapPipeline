package arrange.ipe.style;

import java.awt.Color;
import java.util.LinkedHashMap;
import arrange.ipe.attributes.ColorAttribute;
import arrange.ipe.attributes.PenAttribute;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public final class StyleSheet {

    private String name = null;
    private Layout layout = null;
    private LinkedHashMap<String, Symbol> symbols = new LinkedHashMap<>();
    private LinkedHashMap<String, Opacity> opacities = new LinkedHashMap<>();
    private LinkedHashMap<String, Gradient> gradients = new LinkedHashMap<>();
    private LinkedHashMap<String, SymbolicPen> symbolicPens = new LinkedHashMap<>();
    private LinkedHashMap<String, SymbolicColor> symbolicColors = new LinkedHashMap<>();

    public StyleSheet() {
    }

    public StyleSheet(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Layout getLayout() {
        return layout;
    }

    public void setLayout(Layout layout) {
        this.layout = layout;
    }

    public void addSymbol(Symbol symbol) {
        symbols.put(symbol.getName(), symbol);
    }

    public Symbol getSymbol(String name) {
        return symbols.get(name);
    }

    public void clearSymbols() {
        symbols.clear();
    }

    public void addOpacity(Opacity opacity) {
        opacities.put(opacity.getName(), opacity);
    }

    public Opacity getOpacity(String name) {
        return opacities.get(name);
    }

    public void clearOpacities() {
        opacities.clear();
    }

    public void addGradient(Gradient gradient) {
        gradients.put(gradient.getName(), gradient);
    }

    public Gradient getGradient(String name) {
        return gradients.get(name);
    }

    public void clearGradients() {
        gradients.clear();
    }

    public void addSymbolicPen(SymbolicPen symbolicPen) {
        symbolicPens.put(symbolicPen.getName(), symbolicPen);
    }

    public void addSymbolicPen(String name, double width) {
        SymbolicPen symbolicPen = new SymbolicPen(name, new PenAttribute.Real(width));
        symbolicPens.put(name, symbolicPen);
    }

    public SymbolicPen getSymbolicPen(String name) {
        return symbolicPens.get(name);
    }

    public void clearSymbolicPens() {
        symbolicPens.clear();
    }

    public void addSymbolicColor(SymbolicColor symbolicColor) {
        symbolicColors.put(symbolicColor.getName(), symbolicColor);
    }

    public void addSymbolicColor(String name, ColorAttribute.RGB colorAttribute) {
        symbolicColors.put(name, new SymbolicColor(name, colorAttribute));
    }

    public void addSymbolicColor(String name, ColorAttribute.Gray colorAttribute) {
        symbolicColors.put(name, new SymbolicColor(name, colorAttribute));
    }

    public void addSymbolicColor(String name, Color color) {
        symbolicColors.put(name, new SymbolicColor(name, color));
    }

    public SymbolicColor getSymbolicColor(String name) {
        return symbolicColors.get(name);
    }

    public void clearSymbolicColors() {
        symbolicColors.clear();
    }

    public String toXMLString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<ipestyle");
        if (name != null) {
            sb.append(" name=\"");
            sb.append(name);
            sb.append("\"");
        }
        sb.append(">");
        sb.append(System.lineSeparator());
        if (layout != null) {
            sb.append(layout.toXMLString());
        }
        for (Symbol symbol : symbols.values()) {
            sb.append(symbol.toXMLString());
        }
        for (Opacity opacity : opacities.values()) {
            sb.append(opacity.toXMLString());
        }
        for (Gradient gradient : gradients.values()) {
            sb.append(gradient.toXMLString());
        }
        for (SymbolicPen symbolicPen : symbolicPens.values()) {
            sb.append(symbolicPen.toXMLString());
        }
        for (SymbolicColor symbolicColor : symbolicColors.values()) {
            sb.append(symbolicColor.toXMLString());
        }
        sb.append("</ipestyle>");
        sb.append(System.lineSeparator());
        return sb.toString();
    }
}
