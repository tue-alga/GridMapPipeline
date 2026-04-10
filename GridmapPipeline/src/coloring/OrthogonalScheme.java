package coloring;

import java.awt.Color;

/**
 * Colors orthogonally, using the xaxis for hue, and yaxis for brightness, or
 * vice versa. X-axis to hue is used for wider maps (e.g. US), and Y-axis to hue
 * is used for taller maps (e.g. UK).
 * 
 * @author Max Sondag, Wouter Meulemans
 */
public class OrthogonalScheme extends ColorScheme {

    private boolean xtohue;

    public OrthogonalScheme(boolean xtohue) {
        this.xtohue = xtohue;
    }

    @Override
    public Color assign(double xfrac, double yfrac) {
        float huePercentage = (float) (xtohue ? xfrac : yfrac);
        float brightnessPercentage = (float) (xtohue ? yfrac : xfrac);

        float h = (0f + (360f - 0f) * huePercentage) / 360f;
        float s = 0.7f;
        float b = 1f - brightnessPercentage * 0.8f;

        return Color.getHSBColor(h, s, b);
    }

}
