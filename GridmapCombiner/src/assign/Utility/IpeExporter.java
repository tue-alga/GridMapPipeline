package assign.Utility;

import java.awt.Color;
import java.awt.Rectangle;

/**
 *
 * @author max
 */
public class IpeExporter {

    /**
     * Get the default preamble
     *
     * @return
     */
    public static String getPreambleNoStart() {
        return defaultPreamble();
    }

    /**
     * Get the default and start a page
     *
     * @return
     */
    public static String getPreamble() {
        String s = defaultPreamble();
        s += startPage();
        return s;
    }

    /**
     * Get the preamble with extra definitions and start the page
     *
     * @param extra
     * @return
     */
    public static String getPreamble(String extra) {
        String s = defaultPreamble();
        s += extra;
        s += startPage();
        return s;
    }

    /**
     * Start a page
     *
     * @return
     */
    private static String startPage() {
        String startPage = "<page>\n"
                           + "<layer name=\"alpha\"/>\n"
                           + "<layer name=\"text\"/>\n"
                           + "<view layers=\"alpha\" active=\"alpha\"/>\n";
        return startPage;
    }

    /**
     * End the document
     *
     * @return
     */
    public static String endIpe() {
        return "</page>\n"
               + "</ipe>";
    }

    /**
     * Draw a single hexagon
     *
     * @param centerX
     * @param centerY
     * @param sideLength
     * @param labelText
     * @param color
     * @param province
     * @return
     */
    public static String getHexagon(double centerX, double centerY, double sideLength, String labelText, Color color, String province) {
        double red = color.getRed() / 255.0;
        double green = color.getGreen() / 255.0;
        double blue = color.getBlue() / 255.0;

        //bottom
        double x1 = centerX;
        double y1 = centerY - sideLength;
        //bottomRight
        double x2 = x1 + 0.5 * Math.sqrt(3);
        double y2 = y1 + sideLength / 2;
        //topRight
        double x3 = x2;
        double y3 = y2 + sideLength;
        //top
        double x4 = centerX;
        double y4 = centerY + sideLength;
        //topleft
        double x5 = x4 - 0.5 * Math.sqrt(3);
        double y5 = y4 - sideLength / 2;
        //bottomLeft
        double x6 = x5;
        double y6 = y5 - sideLength;

        String ipeRect = "<path layer=\"" + province + "\" stroke=\"black\" "
                         + "fill=\"" + red + " " + green + " " + blue + "\">\n"
                         + x1 + " " + y1 + " m\n"
                         + x2 + " " + y2 + " l\n"
                         + x3 + " " + y3 + " l\n"
                         + x4 + " " + y4 + " l\n"
                         + x5 + " " + y5 + " l\n"
                         + x6 + " " + y6 + " l\n"
                         + "h\n"
                         + "</path>\n";

        if ("".equals(labelText)) {
            return ipeRect;
        }
        String ipeLabel = "<text size=\"small\" layer=\"text\" transformations=\"translations\" pos=\"" + centerX + " " + centerY + "\" stroke=\"black\" type=\"label\" width=\"4.981\" height=\"6.42\" depth=\"0\" halign=\"center\" valign=\"center\">" + labelText + "</text>\n";
        return ipeRect + ipeLabel;

    }

    /**
     * Draw a single rectangle.
     *
     * @param x
     * @param y
     * @param w
     * @param h
     * @param labelText
     * @param color
     * @param province
     * @return
     */
    public static String getRectangle(double x, double y, double w, double h, String labelText, Color color, String province) {
        double red = color.getRed() / 255.0;
        double green = color.getGreen() / 255.0;
        double blue = color.getBlue() / 255.0;
        String ipeRect = "<path layer=\"" + province + "\" stroke=\"black\" "
                         + "fill=\"" + red + " " + green + " " + blue + "\">\n"
                         + x + " " + (y + h) + " m\n"
                         + x + " " + y + " l\n"
                         + (x + w) + " " + y + " l\n"
                         + (x + w) + " " + (y + h) + " l\n"
                         + "h\n"
                         + "</path>\n";

        double outputX = x + w / 2.0;
        double outputY = y + h / 2.0;

        if ("".equals(labelText)) {
            return ipeRect;
        }
        String ipeLabel = "<text size=\"small\" layer=\"text\" transformations=\"translations\" pos=\"" + outputX + " " + outputY + "\" stroke=\"black\" type=\"label\" width=\"4.981\" height=\"6.42\" depth=\"0\" halign=\"center\" valign=\"center\">" + labelText + "</text>\n";
        return ipeRect + ipeLabel;

    }

    /**
     * Draw text at the specified position
     *
     * @param x
     * @param y
     * @param text
     * @param halign
     * @return
     */
    public static String getString(double x, double y, String text, String halign) {
        String label = "<text size=\"small\" layer=\"text\" transformations=\"translations\" pos=\"" + x + " " + y + "\" stroke=\"black\" type=\"label\" width=\"4.981\" height=\"6.42\" depth=\"0\" halign=\"" + halign + "\" valign=\"center\">" + text + "</text>\n";
        return label;
    }

    /**
     * Draw huge text at the specified position
     *
     * @param x
     * @param y
     * @param text
     * @param halign
     * @return
     */
    public static String getHugeString(double x, double y, String text, String halign) {
        String label = "<text layer=\"text\" transformations=\"translations\" pos=\"" + x + " " + y + "\" stroke=\"red\" type=\"label\" width=\"4.981\" height=\"6.42\" depth=\"0\" size=\"Huge\" halign=\"" + halign + "\" valign=\"center\">" + text + "</text>\n";
        return label;
    }

    /**
     * Draw tiny text at the specified position
     *
     * @param x
     * @param y
     * @param l
     * @param halign
     * @return
     */
    public static String getTinyString(double x, double y, String l, String halign) {
        String label = "<text layer=\"text\" transformations=\"translations\" pos=\"" + x + " " + y + "\" stroke=\"red\" type=\"label\" width=\"4.981\" height=\"6.42\" depth=\"0\" size=\"tiny\" halign=\"" + halign + "\" valign=\"center\">" + l + "</text>\n";
        return label;
    }

    /**
     * Draw a line
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param thickness
     * @return
     */
    public static String getLine(double x1, double y1, double x2, double y2, String thickness) {
        return "<path stroke=\"black\" "
               + "pen=\"" + thickness + "\" "
               + "cap=\"1\" join=\"1\">\n"
               + x1 + " " + y1 + " m\n"
               + x2 + " " + y2 + " l\n"
               + "</path>";
    }

    /**
     * Return the default preamble.
     *
     * @return
     */
    private static String defaultPreamble() {
        return "<?xml version=\"1.0\"?>\n"
               + "<!DOCTYPE ipe SYSTEM \"ipe.dtd\">\n"
               + "<ipe version=\"70107\" creator=\"Ipe 7.2.4\">\n"
               + "<info created=\"D:20161002113229\" modified=\"D:20161002113229\"/>\n"
               + "<preamble>\\renewcommand\\familydefault{\\sfdefault} \n \\usepackage{mathptmx} \n \\usepackage{graphicx}</preamble>\n"
               + "<ipestyle name=\"basic\">\n"
               + "<symbol name=\"arrow/arc(spx)\">\n"
               + "<path stroke=\"sym-stroke\" fill=\"sym-stroke\" pen=\"sym-pen\">\n"
               + "0 0 m\n"
               + "-1 0.333 l\n"
               + "-1 -0.333 l\n"
               + "h\n"
               + "</path>\n"
               + "</symbol>\n"
               + "<symbol name=\"arrow/farc(spx)\">\n"
               + "<path stroke=\"sym-stroke\" fill=\"white\" pen=\"sym-pen\">\n"
               + "0 0 m\n"
               + "-1 0.333 l\n"
               + "-1 -0.333 l\n"
               + "h\n"
               + "</path>\n"
               + "</symbol>\n"
               + "<symbol name=\"arrow/ptarc(spx)\">\n"
               + "<path stroke=\"sym-stroke\" fill=\"sym-stroke\" pen=\"sym-pen\">\n"
               + "0 0 m\n"
               + "-1 0.333 l\n"
               + "-0.8 0 l\n"
               + "-1 -0.333 l\n"
               + "h\n"
               + "</path>\n"
               + "</symbol>\n"
               + "<symbol name=\"arrow/fptarc(spx)\">\n"
               + "<path stroke=\"sym-stroke\" fill=\"white\" pen=\"sym-pen\">\n"
               + "0 0 m\n"
               + "-1 0.333 l\n"
               + "-0.8 0 l\n"
               + "-1 -0.333 l\n"
               + "h\n"
               + "</path>\n"
               + "</symbol>\n"
               + "<symbol name=\"mark/circle(sx)\" transformations=\"translations\">\n"
               + "<path fill=\"sym-stroke\">\n"
               + "0.6 0 0 0.6 0 0 e\n"
               + "0.4 0 0 0.4 0 0 e\n"
               + "</path>\n"
               + "</symbol>\n"
               + "<symbol name=\"mark/disk(sx)\" transformations=\"translations\">\n"
               + "<path fill=\"sym-stroke\">\n"
               + "0.6 0 0 0.6 0 0 e\n"
               + "</path>\n"
               + "</symbol>\n"
               + "<symbol name=\"mark/fdisk(sfx)\" transformations=\"translations\">\n"
               + "<group>\n"
               + "<path fill=\"sym-fill\">\n"
               + "0.5 0 0 0.5 0 0 e\n"
               + "</path>\n"
               + "<path fill=\"sym-stroke\" fillrule=\"eofill\">\n"
               + "0.6 0 0 0.6 0 0 e\n"
               + "0.4 0 0 0.4 0 0 e\n"
               + "</path>\n"
               + "</group>\n"
               + "</symbol>\n"
               + "<symbol name=\"mark/box(sx)\" transformations=\"translations\">\n"
               + "<path fill=\"sym-stroke\" fillrule=\"eofill\">\n"
               + "-0.6 -0.6 m\n"
               + "0.6 -0.6 l\n"
               + "0.6 0.6 l\n"
               + "-0.6 0.6 l\n"
               + "h\n"
               + "-0.4 -0.4 m\n"
               + "0.4 -0.4 l\n"
               + "0.4 0.4 l\n"
               + "-0.4 0.4 l\n"
               + "h\n"
               + "</path>\n"
               + "</symbol>\n"
               + "<symbol name=\"mark/square(sx)\" transformations=\"translations\">\n"
               + "<path fill=\"sym-stroke\">\n"
               + "-0.6 -0.6 m\n"
               + "0.6 -0.6 l\n"
               + "0.6 0.6 l\n"
               + "-0.6 0.6 l\n"
               + "h\n"
               + "</path>\n"
               + "</symbol>\n"
               + "<symbol name=\"mark/fsquare(sfx)\" transformations=\"translations\">\n"
               + "<group>\n"
               + "<path fill=\"sym-fill\">\n"
               + "-0.5 -0.5 m\n"
               + "0.5 -0.5 l\n"
               + "0.5 0.5 l\n"
               + "-0.5 0.5 l\n"
               + "h\n"
               + "</path>\n"
               + "<path fill=\"sym-stroke\" fillrule=\"eofill\">\n"
               + "-0.6 -0.6 m\n"
               + "0.6 -0.6 l\n"
               + "0.6 0.6 l\n"
               + "-0.6 0.6 l\n"
               + "h\n"
               + "-0.4 -0.4 m\n"
               + "0.4 -0.4 l\n"
               + "0.4 0.4 l\n"
               + "-0.4 0.4 l\n"
               + "h\n"
               + "</path>\n"
               + "</group>\n"
               + "</symbol>\n"
               + "<symbol name=\"mark/cross(sx)\" transformations=\"translations\">\n"
               + "<group>\n"
               + "<path fill=\"sym-stroke\">\n"
               + "-0.43 -0.57 m\n"
               + "0.57 0.43 l\n"
               + "0.43 0.57 l\n"
               + "-0.57 -0.43 l\n"
               + "h\n"
               + "</path>\n"
               + "<path fill=\"sym-stroke\">\n"
               + "-0.43 0.57 m\n"
               + "0.57 -0.43 l\n"
               + "0.43 -0.57 l\n"
               + "-0.57 0.43 l\n"
               + "h\n"
               + "</path>\n"
               + "</group>\n"
               + "</symbol>\n"
               + "<symbol name=\"arrow/fnormal(spx)\">\n"
               + "<path stroke=\"sym-stroke\" fill=\"white\" pen=\"sym-pen\">\n"
               + "0 0 m\n"
               + "-1 0.333 l\n"
               + "-1 -0.333 l\n"
               + "h\n"
               + "</path>\n"
               + "</symbol>\n"
               + "<symbol name=\"arrow/pointed(spx)\">\n"
               + "<path stroke=\"sym-stroke\" fill=\"sym-stroke\" pen=\"sym-pen\">\n"
               + "0 0 m\n"
               + "-1 0.333 l\n"
               + "-0.8 0 l\n"
               + "-1 -0.333 l\n"
               + "h\n"
               + "</path>\n"
               + "</symbol>\n"
               + "<symbol name=\"arrow/fpointed(spx)\">\n"
               + "<path stroke=\"sym-stroke\" fill=\"white\" pen=\"sym-pen\">\n"
               + "0 0 m\n"
               + "-1 0.333 l\n"
               + "-0.8 0 l\n"
               + "-1 -0.333 l\n"
               + "h\n"
               + "</path>\n"
               + "</symbol>\n"
               + "<symbol name=\"arrow/linear(spx)\">\n"
               + "<path stroke=\"sym-stroke\" pen=\"sym-pen\">\n"
               + "-1 0.333 m\n"
               + "0 0 l\n"
               + "-1 -0.333 l\n"
               + "</path>\n"
               + "</symbol>\n"
               + "<symbol name=\"arrow/fdouble(spx)\">\n"
               + "<path stroke=\"sym-stroke\" fill=\"white\" pen=\"sym-pen\">\n"
               + "0 0 m\n"
               + "-1 0.333 l\n"
               + "-1 -0.333 l\n"
               + "h\n"
               + "-1 0 m\n"
               + "-2 0.333 l\n"
               + "-2 -0.333 l\n"
               + "h\n"
               + "</path>\n"
               + "</symbol>\n"
               + "<symbol name=\"arrow/double(spx)\">\n"
               + "<path stroke=\"sym-stroke\" fill=\"sym-stroke\" pen=\"sym-pen\">\n"
               + "0 0 m\n"
               + "-1 0.333 l\n"
               + "-1 -0.333 l\n"
               + "h\n"
               + "-1 0 m\n"
               + "-2 0.333 l\n"
               + "-2 -0.333 l\n"
               + "h\n"
               + "</path>\n"
               + "</symbol>\n"
               + "<pen name=\"heavier\" value=\"0.8\"/>\n"
               + "<pen name=\"fat\" value=\"1.2\"/>\n"
               + "<pen name=\"ultrafat\" value=\"2\"/>\n"
               + "<symbolsize name=\"large\" value=\"5\"/>\n"
               + "<symbolsize name=\"small\" value=\"2\"/>\n"
               + "<symbolsize name=\"tiny\" value=\"1.1\"/>\n"
               + "<arrowsize name=\"large\" value=\"10\"/>\n"
               + "<arrowsize name=\"small\" value=\"5\"/>\n"
               + "<arrowsize name=\"tiny\" value=\"3\"/>\n"
               + "<color name=\"red\" value=\"1 0 0\"/>\n"
               + "<color name=\"green\" value=\"0 1 0\"/>\n"
               + "<color name=\"blue\" value=\"0 0 1\"/>\n"
               + "<color name=\"yellow\" value=\"1 1 0\"/>\n"
               + "<color name=\"orange\" value=\"1 0.647 0\"/>\n"
               + "<color name=\"gold\" value=\"1 0.843 0\"/>\n"
               + "<color name=\"purple\" value=\"0.627 0.125 0.941\"/>\n"
               + "<color name=\"gray\" value=\"0.745\"/>\n"
               + "<color name=\"brown\" value=\"0.647 0.165 0.165\"/>\n"
               + "<color name=\"navy\" value=\"0 0 0.502\"/>\n"
               + "<color name=\"pink\" value=\"1 0.753 0.796\"/>\n"
               + "<color name=\"seagreen\" value=\"0.18 0.545 0.341\"/>\n"
               + "<color name=\"turquoise\" value=\"0.251 0.878 0.816\"/>\n"
               + "<color name=\"violet\" value=\"0.933 0.51 0.933\"/>\n"
               + "<color name=\"darkblue\" value=\"0 0 0.545\"/>\n"
               + "<color name=\"darkcyan\" value=\"0 0.545 0.545\"/>\n"
               + "<color name=\"darkgray\" value=\"0.663\"/>\n"
               + "<color name=\"darkgreen\" value=\"0 0.392 0\"/>\n"
               + "<color name=\"darkmagenta\" value=\"0.545 0 0.545\"/>\n"
               + "<color name=\"darkorange\" value=\"1 0.549 0\"/>\n"
               + "<color name=\"darkred\" value=\"0.545 0 0\"/>\n"
               + "<color name=\"lightblue\" value=\"0.678 0.847 0.902\"/>\n"
               + "<color name=\"lightcyan\" value=\"0.878 1 1\"/>\n"
               + "<color name=\"lightgray\" value=\"0.827\"/>\n"
               + "<color name=\"lightgreen\" value=\"0.565 0.933 0.565\"/>\n"
               + "<color name=\"lightyellow\" value=\"1 1 0.878\"/>\n"
               + "<dashstyle name=\"dashed\" value=\"[4] 0\"/>\n"
               + "<dashstyle name=\"dotted\" value=\"[1 3] 0\"/>\n"
               + "<dashstyle name=\"dash dotted\" value=\"[4 2 1 2] 0\"/>\n"
               + "<dashstyle name=\"dash dot dotted\" value=\"[4 2 1 2 1 2] 0\"/>\n"
               + "<textsize name=\"large\" value=\"\\large\"/>\n"
               + "<textsize name=\"small\" value=\"\\small\"/>\n"
               + "<textsize name=\"tiny\" value=\"\\tiny\"/>\n"
               + "<textsize name=\"Large\" value=\"\\Large\"/>\n"
               + "<textsize name=\"LARGE\" value=\"\\LARGE\"/>\n"
               + "<textsize name=\"huge\" value=\"\\huge\"/>\n"
               + "<textsize name=\"Huge\" value=\"\\Huge\"/>\n"
               + "<textsize name=\"footnote\" value=\"\\footnotesize\"/>\n"
               + "<textstyle name=\"center\" begin=\"\\begin{center}\" end=\"\\end{center}\"/>\n"
               + "<textstyle name=\"itemize\" begin=\"\\begin{itemize}\" end=\"\\end{itemize}\"/>\n"
               + "<textstyle name=\"item\" begin=\"\\begin{itemize}\\item{}\" end=\"\\end{itemize}\"/>\n"
               + "<gridsize name=\"4 pts\" value=\"4\"/>\n"
               + "<gridsize name=\"8 pts (~3 mm)\" value=\"8\"/>\n"
               + "<gridsize name=\"16 pts (~6 mm)\" value=\"16\"/>\n"
               + "<gridsize name=\"32 pts (~12 mm)\" value=\"32\"/>\n"
               + "<gridsize name=\"10 pts (~3.5 mm)\" value=\"10\"/>\n"
               + "<gridsize name=\"20 pts (~7 mm)\" value=\"20\"/>\n"
               + "<gridsize name=\"14 pts (~5 mm)\" value=\"14\"/>\n"
               + "<gridsize name=\"28 pts (~10 mm)\" value=\"28\"/>\n"
               + "<gridsize name=\"56 pts (~20 mm)\" value=\"56\"/>\n"
               + "<anglesize name=\"90 deg\" value=\"90\"/>\n"
               + "<anglesize name=\"60 deg\" value=\"60\"/>\n"
               + "<anglesize name=\"45 deg\" value=\"45\"/>\n"
               + "<anglesize name=\"30 deg\" value=\"30\"/>\n"
               + "<anglesize name=\"22.5 deg\" value=\"22.5\"/>\n"
               + "<opacity name=\"10%\" value=\"0.1\"/>\n"
               + "<opacity name=\"30%\" value=\"0.3\"/>\n"
               + "<opacity name=\"50%\" value=\"0.5\"/>\n"
               + "<opacity name=\"75%\" value=\"0.75\"/>\n"
               + "<tiling name=\"falling\" angle=\"-60\" step=\"4\" width=\"1\"/>\n"
               + "<tiling name=\"rising\" angle=\"30\" step=\"4\" width=\"1\"/>\n"
               + "</ipestyle>\n";
    }

}
