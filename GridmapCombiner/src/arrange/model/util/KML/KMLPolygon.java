package arrange.model.util.KML;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import arrange.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import arrange.Utils.Utils;
import static arrange.Utils.Utils.EPS;
import arrange.geom.Polygon;
import arrange.model.util.Pair;

/**
 *
 * @author Max Sondag
 */
public class KMLPolygon {

    public String name;
    public List<Pair<Double, Double>> largestFace;
    public Polygon facePolygon;
    public Area area;
    private List<List<Pair<Double, Double>>> inputFaces;

    /**
     * Xml description contains the full text between placemarks
     *
     * @param xmlDescription
     */
    public KMLPolygon(String xmlDescription) {
        this.name = getName(xmlDescription);
        inputFaces = getFaces(xmlDescription);
        this.largestFace = getLargestFace(inputFaces);
        if (!largestFace.isEmpty()) {
            //not a valid polygon
            this.facePolygon = getPolygon(largestFace);
            area = facePolygon.convertToArea();
        }
    }

    public KMLPolygon(List<Pair<Double, Double>> inputFace, String name) {
        this.name = name;
        this.inputFaces = new ArrayList();
        this.inputFaces.add(inputFace);

        this.largestFace = inputFace;
        if (!largestFace.isEmpty()) {
            //not a valid polygon
            this.facePolygon = getPolygon(largestFace);
            area = facePolygon.convertToArea();
        }

    }

    public List<KMLPolygon> getAllFaces() {
        if (inputFaces.size() > 1) {
            System.out.println("Consists of multiple regions");
        }
        ArrayList<KMLPolygon> polygons = new ArrayList();
        for (List<Pair<Double, Double>> inputFace : inputFaces) {
            KMLPolygon p = new KMLPolygon(inputFace, name);
            if (p.isValid()) {
                polygons.add(p);
            }
        }
        return polygons;
    }

    public boolean isValid() {
        return !largestFace.isEmpty();
    }

    private String getName(String xmlDescription) {
        int startIndex = xmlDescription.indexOf("<name>") + 6;
        int endIndex = xmlDescription.indexOf("</name>");
        if (startIndex == -1 || endIndex == -1) {
            throw new IllegalStateException(xmlDescription + "Does not have a name");
        }
        String name = xmlDescription.substring(startIndex, endIndex);

//        //Tabs is reserved for data
        name = name.replace("\t", ";");
//        System.out.println("name = " + name);
//        
        //UK dataset only as it is wrognly formatted
        int whiteIndex = name.indexOf(" ");
        if (whiteIndex == -1) {
            System.out.println("name = " + name);
        } else {
            name = name.substring(0, whiteIndex);
        }
        return name;
    }

    private List<List<Pair<Double, Double>>> getFaces(String xmlDescription) {
        List<List<Pair<Double, Double>>> faces = new ArrayList();
        String description = "" + xmlDescription;
        while (!"".equals(description)) {
            int startIndex = description.indexOf("<coordinates>");
            int endIndex = description.indexOf("</coordinates>");
            if (startIndex == 20 || endIndex == -1) {
                break;
            }
            String coordinates = description.substring(startIndex, endIndex);
            //make sure it starts with a digit or a -
            while (!(coordinates.startsWith("-") || Character.isDigit(coordinates.charAt(0)))) {
                coordinates = coordinates.substring(1);
            }

            List<Pair<Double, Double>> faceCoordinates = getFaceCoordinates(coordinates);
            faces.add(faceCoordinates);
            description = description.substring(endIndex + 22);
        }
        return faces;
    }

    private List<Pair<Double, Double>> getFaceCoordinates(String coordinateString) {
        List<Pair<Double, Double>> faceCoordinates = new ArrayList();
        while (!"".equals(coordinateString)) {
            int endFirst = coordinateString.indexOf(",");
            int endSecond = coordinateString.indexOf(",", endFirst + 1);
            if (endFirst == -1 || endSecond == -1) {
                break;
            }
            double x = Double.parseDouble(coordinateString.substring(0, endFirst));
            double y = Double.parseDouble(coordinateString.substring(endFirst + 1, endSecond));
            Pair<Double, Double> coordinatePair = new Pair(x, y);
            faceCoordinates.add(coordinatePair);

            int endCoordinate = coordinateString.indexOf(" ");
            if (endCoordinate == -1) {
                break;
            }
            coordinateString = coordinateString.substring(endCoordinate + 1);
        }
        return faceCoordinates;
    }

    private List<Pair<Double, Double>> getLargestFace(List<List<Pair<Double, Double>>> multiCoordinates) {
        double largestArea = 0;
        List<Pair<Double, Double>> largestFace = new ArrayList();
        for (List<Pair<Double, Double>> coordinates : multiCoordinates) {
            Polygon p = getPolygon(coordinates);
            if (p == null) {
                //invalid polygon
                continue;
            }
            double area = p.getArea();
            if (area > largestArea) {
                largestArea = area;
                largestFace = coordinates;
            }
        }
        return largestFace;
    }

    /**
     * Returns the polygon formed by the coordinates
     *
     * @param coordinates
     * @return
     */
    private Polygon getPolygon(List<Pair<Double, Double>> coordinates) {
        ArrayList<Point2D> pointList = new ArrayList();
        for (Pair<Double, Double> pair : coordinates) {
            Point2D point = new Point2D(pair.getFirst(), pair.getSecond());
            pointList.add(point);
        }
        if (pointList.isEmpty()) {
            return null;
        }
        Polygon p = new Polygon(pointList);
        return p;
    }

    public String convertFaceToIpe(boolean first) {
        String returnString = "";
        if (first) {
            returnString += "<path layer=\"Countries\" matrix=\"1 0 0 1 0.0324984 -0.0460355\" stroke=\"black\" fill=\"0.38 0.576 0.812\" pen=\"1\">";
        } else {
            returnString += "<path matrix=\"1 0 0 1 0.0324984 -0.0460355\" stroke=\"black\" fill=\"0.702 0.573 0.365\" pen=\"1\"> \r\n";
        }

        for (int i = 0; i < largestFace.size(); i++) {
            Pair<Double, Double> coordinate = largestFace.get(i);
            returnString += "" + coordinate.getFirst() + " " + coordinate.getSecond();
            if (i == 0) {
                returnString += " m";
            } else {
                returnString += " l";
            }
            returnString += "\r\n";
        }

        returnString += "h\r\n";
        returnString += "</path>\r\n";

        return returnString;
    }

    public String convertLabelToIpe(boolean first) {
        Pair<Double, Double> pointInFace = getPointInFace(largestFace);
        if (first) {
            //example
            //      <text layer="Labels" matrix="1 0 0 1 -128.249 194.523" transformations="translations" pos="232.397 356.522" stroke="black" type="label" width="16.604" height="6.808" depth="0" halign="center" valign="center">WA</text>
            return "<text layer=\"Labels\" matrix=\"1 0 0 1 " + pointInFace.getFirst() + " " + pointInFace.getSecond() + "\" transformations=\"translations\" pos=\"0 0\" stroke=\"black\" type=\"label\" width=\"16.604\" height=\"6.808\" depth=\"0\" halign=\"center\" valign=\"center\">" + name + "</text>\r\n";
        } else {
            //example
            //<text  matrix = "1 0 0 1 -145.37 155.8" transformations = "translations" pos = "232.397 356.522" stroke = "black" type = "label" width = "15.082" height = "6.808" depth = "0" halign = "center" valign = "center" > OR <  / text >
            return "<text matrix=\"1 0 0 1 " + pointInFace.getFirst() + " " + pointInFace.getSecond() + "\" transformations=\"translations\" pos=\"0 0\" stroke=\"black\" type=\"label\" width=\"15.082\" height=\"6.808\" depth=\"0\" halign=\"center\" valign=\"center\">" + name + "</text>\r\n";
        }
    }

    public Pair<Double, Double> getPointInFace(List<Pair<Double, Double>> largestFace) {

        Point2D centroid = facePolygon.getCentroid();
        return new Pair(centroid.getX(), centroid.getY());
    }

    public void removeEndPoint() {
        largestFace.remove(largestFace.size() - 1);
        facePolygon = getPolygon(largestFace);
        area = facePolygon.convertToArea();
    }

    public void removeClosePoints(double minDistance) {
        List<Pair<Double, Double>> newFace = new ArrayList();
        Pair<Double, Double> lastCoordinate = null;
        for (Pair<Double, Double> coordinate : largestFace) {
            if (lastCoordinate != null) {
                double diff = Math.abs(lastCoordinate.getFirst() - coordinate.getFirst()) + Math.abs(lastCoordinate.getSecond() - coordinate.getSecond());
                //neglible difference, is seen as intersection so we do not add it.
                if (diff < minDistance) {
                    continue;
                }
            }
            newFace.add(coordinate);
            lastCoordinate = coordinate;
        }
        largestFace = newFace;
        facePolygon = getPolygon(largestFace);
        area = facePolygon.convertToArea();
    }

    public void trimData(int decimals) {
        ArrayList<Pair<Double, Double>> trimmedFace = new ArrayList();
        for (Pair<Double, Double> coordinate : largestFace) {
            double x = trimDecimal(coordinate.getFirst(), decimals);
            double y = trimDecimal(coordinate.getSecond(), decimals);
            Pair<Double, Double> pair = new Pair(x, y);
            trimmedFace.add(pair);
        }
        largestFace = trimmedFace;
        facePolygon = getPolygon(largestFace);
        area = facePolygon.convertToArea();
    }

    private double trimDecimal(double x, int decimals) {
        x = x * Math.pow(10, decimals);
        x = Math.floor(x);
        x = x * Math.pow(0.1, decimals);
        return x;
    }

    public void removeDuplicateStartEndPoint() {
        int last = largestFace.size() - 1;
        if (largestFace.get(0).equals(largestFace.get(last))) {
            //they are a duplicate
            largestFace.remove(last);
            facePolygon = getPolygon(largestFace);
            area = facePolygon.convertToArea();
        }
    }

    //Updates the area of the region
    public void updateArea(Area rasterizedArea) {
        area = rasterizedArea;
        largestFace = areaToPair(area);
        facePolygon = getPolygon(largestFace);
    }

    private List<Pair<Double, Double>> areaToPair(Area area) {
        //Loop over 
        List<Pair<Double, Double>> pairList = new ArrayList();
        PathIterator pathIterator = area.getPathIterator(null);
        while (!pathIterator.isDone()) {
            double[] doubles = new double[6];
            pathIterator.currentSegment(doubles);
            double x = doubles[0];
            double y = doubles[1];
            pairList.add(new Pair(x, y));
            pathIterator.next();
        }
        return pairList;
    }

    public void scale(int scaleFactor) {
        List<Pair<Double, Double>> newFace = new ArrayList();
        for (Pair<Double, Double> coordinate : largestFace) {
            double x = coordinate.getFirst() * scaleFactor;
            double y = coordinate.getSecond() * scaleFactor;
            newFace.add(new Pair<Double, Double>(x, y));
        }
        largestFace = newFace;
        facePolygon = getPolygon(largestFace);
        area = facePolygon.convertToArea();
    }
}
