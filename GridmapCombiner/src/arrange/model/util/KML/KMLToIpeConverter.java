package arrange.model.util.KML;

import java.awt.geom.Area;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static arrange.Utils.Utils.EPS;
import arrange.model.subdivision.Map;
import arrange.model.util.KML.KMLPolygon;
import arrange.model.util.Pair;

/**
 *
 * @author Max Sondag
 */
public class KMLToIpeConverter {

    public static void main(String args[]) {
//        String inputFileName = "C:\\Development\\mosaic-maps\\data\\Dialect\\UK-mapSimplified.kml";
//        String outputFileName = "C:\\Development\\mosaic-maps\\data\\Dialect\\UK-mapSimplified.ipe";
        String inputFileName = "C:\\Development\\mosaic-maps\\data\\Dialect\\UK-map.kml";
        String outputFileName = "C:\\Development\\mosaic-maps\\data\\Dialect\\UK-map.ipe";
        boolean generateVornoi = false;
//        String inputFileName = "C:\\Development\\mosaic-maps\\data\\Dialect\\Nederland\\NL.kml";
//        String outputFileName = "C:\\Development\\mosaic-maps\\data\\Dialect\\Nederland\\NL.ipe";
//        boolean generateVornoi = true;

        KMLToIpeConverter kmlToIpeConverter = new KMLToIpeConverter();
        kmlToIpeConverter.convertMap(inputFileName, outputFileName, generateVornoi);
    }

    public void convertMap(String inputFileName, String outputFileName, boolean generateVornoi) {
        System.out.println("start generating new map with vornoi " + (generateVornoi ? "enabled" : "disabled") );
        //For vornoi we require all regions, for largestFace only a single one 
        List<KMLPolygon> regions = getKMLRegions(inputFileName,!generateVornoi);
        System.out.println("regions generated");
        List<KMLPoint> vornoiPoints = new ArrayList();
        if (generateVornoi) {
            vornoiPoints = getKMLVornoiPoints(inputFileName);
            System.out.println("Vornoi points extracted");
        }

        trimData(regions);
        System.out.println("data trimmed");

        Raster raster = cleanMapData(regions, vornoiPoints, 1000);
        System.out.println("Map rasterizd");

        //if vornoi the regions are defined by kml points. Otherwise by regions
        List<String> names;
        if (generateVornoi) {
            names = getVornoiIds(vornoiPoints);
        } else {
            names = getNames(regions);
        }

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFileName, false));
            //first lines of ipe file
            bw.write(getIpePreamble());
            //Write the face matrices. First face is slightly different
            boolean first = true;
            for (String name : names) {
                RasterRegion r = raster.getRasterRegion(name);
                if (r == null) {
                    System.out.println("r.name");
                }
                String ipePoly = r.convertRegionToIpe(first);
                bw.write(ipePoly);
                first = false;
            }
            //Write the labels. First label is slightly different
            first = true;
            for (String name : names) {
                RasterRegion r = raster.getRasterRegion(name);

                String ipeLabel = r.convertLabelToIpe(first);
                bw.write(ipeLabel);
                first = false;
            }
            //closing line of ipe
            bw.write("</page>\n"
                    + "</ipe>");
            bw.flush();
            bw.close();

        } catch (IOException ex) {
            Logger.getLogger(KMLToIpeConverter.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println(
                "map generated");
    }

    private String getXMLdescription(String inputFileName) {
        //get xmlDescription
        String completeXMLDescription = "";
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(inputFileName));
            String line = null;
            while ((line = br.readLine()) != null) {
                completeXMLDescription += line;

            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(KMLToIpeConverter.class
                    .getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(KMLToIpeConverter.class
                    .getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();

            } catch (IOException ex) {
                Logger.getLogger(KMLToIpeConverter.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        return completeXMLDescription;
    }

    /**
     * If singleFace == true, the kml region will be trimmed to the single
     * largest face
     *
     * @param inputFileName
     * @param singleFace
     * @return
     */
    private List<KMLPolygon> getKMLRegions(String inputFileName, boolean singleFace) {
        List<KMLPolygon> polygons = new ArrayList();
        String completeXMLDescription = getXMLdescription(inputFileName);

        while (!"".equals(completeXMLDescription)) {
            int startIndex = completeXMLDescription.indexOf("<Placemark>");
            int endIndex = completeXMLDescription.indexOf("</Placemark>");

            if (startIndex == -1 || endIndex == -1) {
                break;
            }
            String xmlDescription = completeXMLDescription.substring(startIndex, endIndex);
            KMLPolygon polygon = new KMLPolygon(xmlDescription);
            //Polygon holds the regions from the xml description

            List<KMLPolygon> facePolygons = new ArrayList();
            if (singleFace) {
                //we only need the largest face
                facePolygons.add(polygon);
            } else {
                //we need all faces
                facePolygons.addAll(polygon.getAllFaces());
            }
            //add all required faces
            for (KMLPolygon p : facePolygons) {
                if (p.isValid()) {
                    //every second placemark holds the label instead of the map
                    polygons.add(p);
                }
            }
            completeXMLDescription = completeXMLDescription.substring(endIndex + 12);
        }
        return polygons;
    }

    private String getIpePreamble() {
        return "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE ipe SYSTEM \"ipe.dtd\">\n"
                + "<ipe version=\"70005\" creator=\"Ipe 7.1.2\">\n"
                + "<info created=\"D:20100318161905\" modified=\"D:20141205115315\"/>\n"
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
                + "<path fill=\"sym-stroke\" fillrule=\"eofill\">\n"
                + "0.6 0 0 0.6 0 0 e\n"
                + "0.4 0 0 0.4 0 0 e\n"
                + "</path>\n"
                + "<path fill=\"sym-fill\">\n"
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
                + "<path fill=\"sym-fill\">\n"
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
                + "<color name=\"cyan\" value=\"0.553 0.827 0.78\"/>\n"
                + "<color name=\"lightyellow\" value=\"1 1 0.702\"/>\n"
                + "<color name=\"lightpurple\" value=\"0.745 0.729 0.855\"/>\n"
                + "<color name=\"red\" value=\"0.984 0.502 0.447\"/>\n"
                + "<color name=\"seablue\" value=\"0.502 0.694 0.827\"/>\n"
                + "<color name=\"orange\" value=\"0.992 0.706 0.384\"/>\n"
                + "<color name=\"green\" value=\"0.702 0.871 0.412\"/>\n"
                + "<color name=\"pink\" value=\"0.988 0.804 0.898\"/>\n"
                + "<color name=\"gray\" value=\"0.851\"/>\n"
                + "<color name=\"purple\" value=\"0.737 0.502 0.741\"/>\n"
                + "<color name=\"lightgreen\" value=\"0.8 0.922 0.773\"/>\n"
                + "<color name=\"yellow\" value=\"1 0.929 0.435\"/>\n"
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
                + "<tiling name=\"falling\" angle=\"-60\" step=\"4\" width=\"1\"/>\n"
                + "<tiling name=\"rising\" angle=\"30\" step=\"4\" width=\"1\"/>\n"
                + "</ipestyle>\n"
                + "<page>\n"
                + "<layer name=\"Countries\"/>\n"
                + "<layer name=\"Labels\"/>\n"
                + "<view layers=\"Countries Labels\" active=\"Countries\"/>";
    }

    private void trimData(List<KMLPolygon> regions) {
        int decimals = 3;
        for (KMLPolygon p : regions) {
            p.trimData(decimals);
        }
    }

    /**
     * Removes points that are too close to each other, Rasterizes the region
     * and returns the resulting raster.
     *
     * @param regions
     */
    private Raster cleanMapData(List<KMLPolygon> regions, List<KMLPoint> vornoiCenters, int rasterSize) {

        //Points that are on top of each other can't exist in the algorithm so we remove them
        for (KMLPolygon p : regions) {
            //remove all points closer than 0.1 apart
            p.removeClosePoints(EPS * 10);
        }

        int scaleFactor = 1000;
        for (KMLPolygon p : regions) {
            //Scale it such that it fits better in an integer grid
            p.scale(scaleFactor);
        }
        for (KMLPoint p : vornoiCenters) {
            //Scale it such that it fits better in an integer grid
            p.scale(scaleFactor);
        }

        System.out.println("rasterSize = " + rasterSize);
        Raster raster = new Raster(regions, vornoiCenters, rasterSize, rasterSize);

        return raster;
    }

    private List<String> getNames(List<KMLPolygon> regions) {
        ArrayList<String> names = new ArrayList();
        for (KMLPolygon p : regions) {
            names.add(p.name);
        }
        return names;
    }

    private List<String> getVornoiIds(List<KMLPoint> vornoiPoints) {
        ArrayList<String> names = new ArrayList();
        for (KMLPoint p : vornoiPoints) {
            names.add(p.id);
        }
        return names;
    }

    private List<KMLPoint> getKMLVornoiPoints(String inputFileName) {
        List<KMLPoint> vornoiPoints = new ArrayList();
        String completeXMLDescription = getXMLdescription(inputFileName);

        while (!"".equals(completeXMLDescription)) {
            int startIndex = completeXMLDescription.indexOf("<Placemark>");
            int endIndex = completeXMLDescription.indexOf("</Placemark>");

            if (startIndex == -1 || endIndex == -1) {
                break;
            }
            String xmlDescription = completeXMLDescription.substring(startIndex, endIndex);
            KMLPoint point = new KMLPoint(xmlDescription);
            if (point.isValid()) {
                //every second placemark holds the label instead of the map
                vornoiPoints.add(point);
            }
            completeXMLDescription = completeXMLDescription.substring(endIndex + 12);
        }
        return vornoiPoints;
    }

}
