package arrange.model.util.KML;

/**
 *
 * @author Max Sondag
 */
public class KMLPoint {

    public double x, y;
    private boolean coordinatesSet = false;
    public String id;

    public KMLPoint(double x, double y, String id) {
        this.x = x;
        this.y = y;
        coordinatesSet = true;
        this.id = id;
    }

    KMLPoint(String xmlDescription) {
        if (xmlDescription.contains("Point")) {
            //otherwise it is a region
            id = getId(xmlDescription);
            setCoordinates(xmlDescription);
        }

    }

    private String getId(String xmlDescription) {
        int startIndex = xmlDescription.indexOf("<name>") + 6;
        int endIndex = xmlDescription.indexOf("</name>");
        if (startIndex == -1 || endIndex == -1) {
            throw new IllegalStateException(xmlDescription + "Does not have a name");
        }
        String name = xmlDescription.substring(startIndex, endIndex);
        name = name.replace("\t" , ";");
        //Whitespace is reserved for data
        //UK dataset only as it is wrognly formatted
//        int whiteIndex = name.indexOf(" ");
//        if (whiteIndex == -1) {
//            System.out.println("name = " + name);
//        } else {
//            name = name.substring(0, whiteIndex);
//        }
        return name;
    }

    public void scale(int scaleFactor) {
        x *= scaleFactor;
        y *= scaleFactor;
    }

    public boolean isValid() {
        return (coordinatesSet && id != null);
    }

    private void setCoordinates(String xmlDescription) {

        int startIndex = xmlDescription.indexOf("<Point>") + 7;
        int endIndex = xmlDescription.indexOf("</Point>");
        if (startIndex == -1 || endIndex == -1) {
            throw new IllegalStateException(xmlDescription + "Does not have a \"Point\" property");
        }

        String pointProperty = xmlDescription.substring(startIndex, endIndex);

        startIndex = pointProperty.indexOf("<coordinates>") + 13;
        endIndex = pointProperty.indexOf("</coordinates>");
        if (startIndex == -1 || endIndex == -1) {
            throw new IllegalStateException(pointProperty + "no coordinates in point property");
        }
        String point = pointProperty.substring(startIndex, endIndex);

        int endFirst = point.indexOf(",");
        int endSecond = point.indexOf(",", endFirst + 1);
        if (endFirst == -1 || endSecond == -1) {
            throw new IllegalStateException(pointProperty + "improper coordinates in point property");
        }
        this.x = Double.parseDouble(point.substring(0, endFirst));
        this.y = Double.parseDouble(point.substring(endFirst + 1, endSecond));
        coordinatesSet = true;
    }
}
