package analyze;

import common.SiteMap;
import common.util.Transform;
import java.awt.Color;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import nl.tue.geometrycore.geometryrendering.GeometryRenderer;
import nl.tue.geometrycore.geometryrendering.styling.ExtendedColors;
import nl.tue.geometrycore.util.DoubleUtil;

/**
 *
 * @author Wouter Meulemans
 */
public abstract class QualityMap<T> {

    public static final DecimalFormat df = new DecimalFormat("#0.000", DecimalFormatSymbols.getInstance(Locale.US));

    private final String name;
    private final boolean highIsGood;
    private double min, max;
    private final Map<T, Double> map = new HashMap<T, Double>();

    public QualityMap(String name, boolean highIsGood) {
        this.name = name;
        this.highIsGood = highIsGood;
    }

    @Override
    public String toString() {
        return name;
    }

    private void determineMinMax() {
        min = Double.POSITIVE_INFINITY;
        max = Double.NEGATIVE_INFINITY;

        for (Double v : map.values()) {
            min = Math.min(v, min);
            max = Math.max(v, max);
        }
    }

    private void print() {
        Summary summary = summarize();

        System.out.println(name);
        System.out.println("  " + df.format(summary.avg) + " avg, " + summary.count + " values");

        System.out.println("  max : " + summary.max);
        System.out.println("  90% : " + summary.pct90);
        System.out.println("  75% : " + summary.pct75);
        System.out.println("  min : " + summary.min);
    }

    public void process() {
        determineMinMax();
        print();
    }

    public double getMinimum() {
        return min;
    }

    public double getMaximum() {
        return max;
    }

    public void setMinimum(double m) {
        min = m;
    }

    public void setMaximum(double m) {
        max = m;
    }

    public double getActualMinimum() {
        double truemin = Double.POSITIVE_INFINITY;
        for (Double v : map.values()) {
            if (v < truemin) {
                truemin = v;
            }
        }
        return truemin;
    }

    public double getActualMaximum() {
        double truemax = Double.NEGATIVE_INFINITY;
        for (Double v : map.values()) {
            if (v > truemax) {
                truemax = v;
            }
        }
        return truemax;
    }

    public Color getColor(T elt) {
        double f = getRelativeQuality(elt);
        if (Double.isNaN(f)) {
            return ExtendedColors.gray;
        } else {
            // inspired by colorbrewer2.org 7-class oranges
            double highr = 254 / 255.0;
            double lowr = 140 / 255.0;
            double highg = 237 / 255.0;
            double lowg = 45 / 255.0;
            double highb = 222 / 255.0;
            double lowb = 4 / 255.0;

            if (!highIsGood) {
                f = 1 - f;
            }

            if (!DoubleUtil.inClosedInterval(f, 0, 1)) {
                f = DoubleUtil.clipValue(f, 0, 1);
                System.err.println("Warning: relative quality outside min-max range");
            }

            double r = DoubleUtil.interpolate(lowr, highr, f);
            double g = DoubleUtil.interpolate(lowg, highg, f);
            double b = DoubleUtil.interpolate(lowb, highb, f);

            return ExtendedColors.fromUnitRGB(r, g, b);
        }
    }

    public Color getMaxColor() {
        if (highIsGood) {
            double highr = 254 / 255.0;
            double highg = 237 / 255.0;
            double highb = 222 / 255.0;
            return ExtendedColors.fromUnitRGB(highr, highg, highb);
        } else {
            double lowr = 140 / 255.0;
            double lowg = 45 / 255.0;
            double lowb = 4 / 255.0;
            return ExtendedColors.fromUnitRGB(lowr, lowg, lowb);
        }
    }

    public Color getMinColor() {
        if (!highIsGood) {
            double highr = 254 / 255.0;
            double highg = 237 / 255.0;
            double highb = 222 / 255.0;
            return ExtendedColors.fromUnitRGB(highr, highg, highb);
        } else {
            double lowr = 140 / 255.0;
            double lowg = 45 / 255.0;
            double lowb = 4 / 255.0;
            return ExtendedColors.fromUnitRGB(lowr, lowg, lowb);
        }
    }

    public boolean isHighIsGood() {
        return highIsGood;
    }

    public double getRelativeQuality(T elt) {
        return (getQuality(elt) - min) / (max - min);
    }

    public double getQuality(T elt) {
        return map.getOrDefault(elt, Double.NaN);
    }

    public void putQuality(T elt, double qual) {
        map.put(elt, qual);
    }

    public abstract void render(GeometryRenderer write, SiteMap map, Transform cartogramTransform);

    public Summary summarize() {

        Summary summary = new Summary();

        summary.total = 0;
        summary.count = 0;
        List<Double> values = new ArrayList();
        for (Double v : map.values()) {
            summary.total += v;
            summary.count++;
            values.add(v);
        }

        if (highIsGood) {
            Collections.sort(values);
        } else {
            Collections.sort(values, (a, b) -> Double.compare(b, a));
        }
        summary.max = values.get(0);
        summary.pct90 = values.get((int) Math.ceil(values.size() / 10.0));
        summary.pct75 = values.get((int) Math.ceil(values.size() / 4.0));
        summary.min = values.get(values.size() - 1);
        summary.avg = summary.total / summary.count;

        return summary;
    }

    public class Summary {

        public double total;
        public double avg;
        public int count;
        public double min, max, pct90, pct75;
    }
}
