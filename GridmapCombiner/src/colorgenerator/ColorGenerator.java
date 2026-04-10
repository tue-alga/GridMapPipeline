/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package colorgenerator;

import java.awt.Color;
import java.awt.color.ColorSpace;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author msondag
 */
public class ColorGenerator {

    //uncomment the input file
    
//    String inputFile = "../Data/maps/Sites/Netherlands2014Sites.tsv";
//    String inputFile = "../Data/maps/Sites/NetherlandsMunicipalitySites.tsv";
//    String inputFile = "../Data/maps/Sites/USAStates.tsv";
    String inputFile = "../Data/maps/Sites/USAStatesAndDc.tsv";
//    String inputFile = "../Data/maps/Sites/UKConstituencies.tsv";
//    String inputFile = "../Data/maps/Sites/UKLocalAuthorities.tsv";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        // TODO code application logic here
        new ColorGenerator();
    }

    public ColorGenerator() throws IOException {
        //read the sites frot the input file
        List<String> lines = Files.readAllLines(Paths.get(inputFile));
        List<Site> sites = new ArrayList();
        for (String line : lines) {
            String[] split = line.split("\t");
            Site s = new Site(Double.parseDouble(split[1]), Double.parseDouble(split[2]), split[0]);
            sites.add(s);
        }

        //get bounding box
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        for (Site s : sites) {
            minX = Math.min(minX, s.x);
            maxX = Math.max(maxX, s.x);
            minY = Math.min(minY, s.y);
            maxY = Math.max(maxY, s.y);
        }

        //get color ranges
        //start at hsl color and change luminance value
        double xRange = maxX - minX;
        double yRange = maxY - minY;

        //for each site, get the color. Different color ranges for different maps.
        for (Site site : sites) {
            double xPercentage = (site.x - minX) / xRange;
            double yPercentage = (site.y - minY) / yRange;
            Color c = null;
            if (inputFile.contains("Netherlands")) {
                c = getNlColor(xPercentage, yPercentage);
            }
            if (inputFile.contains("UK")) {
                c = getUKColor(xPercentage, yPercentage);
            }
            if (inputFile.contains("USA")) {
                c = getUSAColor(xPercentage, yPercentage);
            }
            site.color = c;
            //print the color in tsv format.
            System.out.println(site.print());
        }

//        printGrid();
    }

    public Color getUKColor(double xPercentage, double yPercentage) {

        //hue vertical, brightness horizontal
        float huePercentage = (float) yPercentage;
        float brightnessPercentage = (float) xPercentage;

        float h = (0f + (360f - 0f) * huePercentage) / 360f;
        float s = 0.7f;
        float b = 1f - brightnessPercentage * 0.8f;

        Color c = Color.getHSBColor(h, s, b);
        return c;
    }

    public Color getUSAColor(double xPercentage, double yPercentage) {
        //hue horizontal, brightness vertical
        float huePercentage = (float) xPercentage;
        float brightnessPercentage = (float) yPercentage;

        float h = (0f + (360f - 0f) * huePercentage) / 360f;
        float s = 0.7f;
        float b = 1f - brightnessPercentage * 0.8f;

        Color c = Color.getHSBColor(h, s, b);
        return c;
    }

    public Color getNlColor(double xPercentage, double yPercentage) {
        //point in the ijsselmeer in the netherlands
        double middleX = 315.0 / 573.0;
        double middleY = 451.0 / 666.0;

        double angle = getAngle(middleX, middleY, xPercentage, yPercentage);
        angle = (angle + 90 + 360.0) % 360.0;//rotate so opening is at the top
        double distance = getDistance(middleX, middleY, xPercentage, yPercentage);

        double maxDistance = getDistance(middleX, middleY, 1, 1);
        maxDistance = Math.max(maxDistance, getDistance(middleX, middleY, 0, 0));
        maxDistance = Math.max(maxDistance, getDistance(middleX, middleY, 0, 1));
        maxDistance = Math.max(maxDistance, getDistance(middleX, middleY, 1, 0));

        float anglePercentage = (float) angle / 360f;
        float distancePercentage = (float) (distance / maxDistance);

        float h = (0f + (360f - 0f) * anglePercentage) / 360f;
        float s = 0.7f;
        float b = 1f - distancePercentage * 0.8f;

        Color c = Color.getHSBColor(h, s, b);
        return c;
    }

    public double getDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
    }

    public float getAngle(double x1, double y1, double x2, double y2) {
        float angle = (float) Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));

        if (angle < 0) {
            angle += 360;
        }

        return angle;
    }

    
    /**
     * Prints a 100 by 100 grid in the color of the gradient to be printed.
     * Used for debugging/color calibration.
     */
    private void printGrid() {
        for (double x = 0; x < 100; x++) {
            for (double y = 0; y < 100; y++) {
                Color c = getUSAColor(x / 100.0, y / 100.0);
                double red = ((double) c.getRed()) / 255.0;
                double green = ((double) c.getGreen()) / 255.0;
                double blue = ((double) c.getBlue()) / 255.0;

                System.out.println("<path fill=\"" + red + " " + green + " " + blue + "\">\n"
                                   + "" + x + " " + y + " m\n"
                                   + "" + (x + 1) + " " + y + " l\n"
                                   + "" + (x + 1) + " " + (y + 1) + " l\n"
                                   + "" + x + " " + (y + 1) + " l\n"
                                   + "h\n"
                                   + "</path>");
            }
        }
    }

}
