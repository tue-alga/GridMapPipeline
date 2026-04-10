package arrange.model.util.KML;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import arrange.model.util.Pair;

/**
 *
 * @author Max Sondag
 */
public class RasterRegion {

    private Raster raster;
    private ArrayList<RasterSquare> region;
    private String regionName;

    public RasterRegion(String name, List<RasterSquare> largestRegionById, Raster raster) {
        this.region = new ArrayList();
        this.region.addAll(largestRegionById);
        this.regionName = name;
        this.raster = raster;
    }

    public String convertRegionToIpe(boolean first) {

        String returnString = "";
        if (first) {
            returnString += "<path layer=\"Countries\" matrix=\"1 0 0 1 0.0324984 -0.0460355\" stroke=\"black\" fill=\"0.38 0.576 0.812\" pen=\"1\">";
        } else {
            returnString += "<path matrix=\"1 0 0 1 0.0324984 -0.0460355\" stroke=\"black\" fill=\"0.702 0.573 0.365\" pen=\"1\"> \r\n";
        }
        returnString += "\r\n";
        if(regionName.equals("Wijdenes"))
        {
            System.out.println("wijdenes");
        }
        ArrayList<RasterLineSegment> outlineSegments = getOutlineSegments();
        Point.Double lastSharedPoint = null;
        for (int i = 0; i < outlineSegments.size(); i++) {
            RasterLineSegment s = outlineSegments.get(i);
            Point.Double p;
            //For each segment add the point that has not been added to the outline yet
            if (i == 0) {
                //For the first point we take the shared point
                p = getSharedPoint(s, outlineSegments.get(i + 1));
                returnString += "" + p.x + " " + p.y;
                returnString += " m";
            } else {
                //for every other point we take the unshared point
                p = getOtherPoint(s, lastSharedPoint);
                returnString += "" + p.x + " " + p.y;
                returnString += " l";
            }
            lastSharedPoint = p;
            returnString += "\r\n";
        }

        returnString += "h\r\n";
        returnString += "</path>\r\n";

        return returnString;
    }

    /**
     * Returns an arraylist of rasterlinesegments which form the outline of the
     * region
     *
     * @return
     */
    private ArrayList<RasterLineSegment> getOutlineSegments() {
        //holds the outline in order
        ArrayList<RasterLineSegment> outline = new ArrayList();

        if (region.isEmpty()) {
            //Region is empty, so the outline is empty as well
            System.err.println("Empty region:" + regionName);
            return outline;
        }

        //get the first outline segment
        RasterSquare initialSquare = getBoundarySquare();

        RasterLineSegment currentOutlineSegment = getBoundarySegment(initialSquare);
        outline.add(currentOutlineSegment);
        //outline has minimal size 4. Outline is complete is size >4 and last segments
        //ends on the first segment
        while (outline.size() < 4 || !currentOutlineSegment.hasSameEndpoint(outline.get(0))) {

            //Get all segments which are adjacent to currentOutlineSegment
            ArrayList<RasterLineSegment> adjacentSegments = getAdjacentSegments(currentOutlineSegment);

            //Filter out all segments that already occured in outline
            adjacentSegments.removeAll(outline);

            //Finally find the new outline segment
            for (RasterLineSegment segment : adjacentSegments) {
                if (isBoundarySegment(segment)) {
                    //newSegment is now the boundary segment
                    outline.add(segment);
                    currentOutlineSegment = segment;
                    break;
                }
            }
        }

        return outline;
    }

    /**
     * Returns all segments which share and endpoint with the current segment
     *
     * @param currentSegment
     * @return
     */
    private ArrayList<RasterLineSegment> getAdjacentSegments(RasterLineSegment currentSegment) {
        ArrayList<RasterLineSegment> adjacentSegments = new ArrayList();
        if (currentSegment.isHorizontal()) {
            RasterSquare top = currentSegment.r1;
            RasterSquare bottom = currentSegment.r2;

            boolean leftHandled = false;
            boolean rightHandled = false;
            if (top != null) {
                adjacentSegments.add(getLeftSegment(top));
                adjacentSegments.add(getRightSegment(top));

                RasterSquare topleft = raster.getSquareToLeft(top);
                if (topleft != null) {
                    adjacentSegments.add(getBottomSegment(topleft));
                    leftHandled = true;
                }

                RasterSquare topRight = raster.getSquareToRight(top);
                if (topRight != null) {
                    adjacentSegments.add(getBottomSegment(topRight));
                    rightHandled = true;
                }

            }
            if (bottom != null) {
                adjacentSegments.add(getLeftSegment(bottom));
                adjacentSegments.add(getRightSegment(bottom));

                RasterSquare bottomLeft = raster.getSquareToLeft(bottom);
                if (bottomLeft != null && !leftHandled) {
                    adjacentSegments.add(getTopSegment(bottomLeft));
                    leftHandled = true;
                }

                RasterSquare bottomRight = raster.getSquareToRight(bottom);
                if (bottomRight != null && !rightHandled) {
                    adjacentSegments.add(getTopSegment(bottomRight));
                    rightHandled = true;
                }
            }

        } else {
            //segment is vertical
            RasterSquare left = currentSegment.r1;
            RasterSquare right = currentSegment.r2;
            boolean topHandled = false;
            boolean bottomHandled = false;
            if (left != null) {
                adjacentSegments.add(getTopSegment(left));
                adjacentSegments.add(getBottomSegment(left));

                RasterSquare topLeft = raster.getSquareAbove(left);
                if (topLeft != null) {
                    adjacentSegments.add(getRightSegment(topLeft));
                    topHandled = true;
                }

                RasterSquare bottomLeft = raster.getSquareBelow(left);
                if (bottomLeft != null) {
                    adjacentSegments.add(getRightSegment(bottomLeft));
                    bottomHandled = true;
                }
            }

            if (right != null) {
                adjacentSegments.add(getTopSegment(right));
                adjacentSegments.add(getBottomSegment(right));

                RasterSquare topRight = raster.getSquareAbove(right);
                if (topRight != null && !topHandled) {
                    adjacentSegments.add(getLeftSegment(topRight));
                    topHandled = true;
                }

                RasterSquare bottomRight = raster.getSquareBelow(right);
                if (bottomRight != null && !bottomHandled) {
                    adjacentSegments.add(getLeftSegment(bottomRight));
                    bottomHandled = true;
                }
            }
        }

        return adjacentSegments;
    }

    /**
     *
     * Gets a boundary square
     *
     * @return
     */
    private RasterSquare getBoundarySquare() {
        for (RasterSquare r : region) {
            List<RasterSquare> adjacentSquares = raster.getAdjacentSquares(r);
            for (RasterSquare adjacentSquare : adjacentSquares) {
                if (isDifferentRegion(adjacentSquare)) {
                    //it is a boundary square as at least one of the adjacent
                    //rectangle is not part of this region
                    return r;
                }
            }
        }
        //there does not exist a boundary square, can not occur
        System.err.println("There is no boundary square");
        return null;
    }

    public String convertLabelToIpe(boolean first) {
        Pair<Double, Double> pointInFace = getRegionCentroid();
        if (pointInFace == null) {
            System.err.println("No centroid or label for region" + regionName);
            return "";
        }

        if (first) {
            //example
            //      <text layer="Labels" matrix="1 0 0 1 -128.249 194.523" transformations="translations" pos="232.397 356.522" stroke="black" type="label" width="16.604" height="6.808" depth="0" halign="center" valign="center">WA</text>
            return "<text layer=\"Labels\" matrix=\"1 0 0 1 " + pointInFace.getFirst() + " " + pointInFace.getSecond() + "\" transformations=\"translations\" pos=\"0 0\" stroke=\"black\" type=\"label\" width=\"16.604\" height=\"6.808\" depth=\"0\" halign=\"center\" valign=\"center\">" + regionName + "</text>\r\n";
        } else {
            //example
            //<text  matrix = "1 0 0 1 -145.37 155.8" transformations = "translations" pos = "232.397 356.522" stroke = "black" type = "label" width = "15.082" height = "6.808" depth = "0" halign = "center" valign = "center" > OR <  / text >
            return "<text matrix=\"1 0 0 1 " + pointInFace.getFirst() + " " + pointInFace.getSecond() + "\" transformations=\"translations\" pos=\"0 0\" stroke=\"black\" type=\"label\" width=\"15.082\" height=\"6.808\" depth=\"0\" halign=\"center\" valign=\"center\">" + regionName + "</text>\r\n";
        }
    }

    /**
     * Gets the linesegment arround r
     *
     * @param r
     * @return
     */
    private ArrayList<RasterLineSegment> getLineSegments(RasterSquare r) {
        RasterLineSegment top = getTopSegment(r);
        RasterLineSegment left = getLeftSegment(r);
        RasterLineSegment bottom = getBottomSegment(r);
        RasterLineSegment right = getRightSegment(r);

        ArrayList<RasterLineSegment> segments = new ArrayList();
        segments.add(top);
        segments.add(left);
        segments.add(bottom);
        segments.add(right);
        return segments;
    }

    private boolean isBoundarySegment(RasterLineSegment s) {
        //r1 is in region
        if (s.r1 != null && s.r1.id != null && s.r1.id.equals(regionName)) {
            //r2 is not 
            if (s.r2 == null || s.r2.id == null || !s.r2.id.equals(regionName)) {
                return true;
            }
        }

        //r2 is in region
        if (s.r2 != null && s.r2.id != null && s.r2.id.equals(regionName)) {
            //r1 is not 
            if (s.r1 == null || s.r1.id == null || !s.r1.id.equals(regionName)) {
                return true;
            }
        }
        //either r1 and r2 both in region or both not in region
        return false;
    }

    private RasterLineSegment getTopSegment(RasterSquare r) {
        RasterSquare above = raster.getSquareAbove(r);
        RasterLineSegment top = new RasterLineSegment(r.x, r.y, r.width, 0, above, r);
        return top;
    }

    private RasterLineSegment getBottomSegment(RasterSquare r) {
        RasterSquare below = raster.getSquareBelow(r);
        RasterLineSegment bottom = new RasterLineSegment(r.x, r.y + r.height, r.width, 0, r, below);
        return bottom;
    }

    private RasterLineSegment getLeftSegment(RasterSquare r) {
        RasterSquare toLeft = raster.getSquareToLeft(r);
        RasterLineSegment left = new RasterLineSegment(r.x, r.y, 0, r.height, toLeft, r);
        return left;
    }

    private RasterLineSegment getRightSegment(RasterSquare r) {
        RasterSquare toRight = raster.getSquareToRight(r);
        RasterLineSegment right = new RasterLineSegment(r.x + r.width, r.y, 0, r.height, r, toRight);
        return right;
    }

    private RasterLineSegment getBoundarySegment(RasterSquare currentSquare) {
        RasterLineSegment left = getLeftSegment(currentSquare);
        RasterLineSegment top = getTopSegment(currentSquare);
        RasterLineSegment right = getRightSegment(currentSquare);
        RasterLineSegment bottom = getBottomSegment(currentSquare);

        if (isDifferentRegion(left.r1) || isDifferentRegion(left.r2)) {
            return left;
        }
        if (isDifferentRegion(right.r1) || isDifferentRegion(right.r2)) {
            return right;
        }
        if (isDifferentRegion(top.r1) || isDifferentRegion(top.r2)) {
            return top;
        }
        if (isDifferentRegion(bottom.r1) || isDifferentRegion(bottom.r2)) {
            return bottom;
        }
        //does not have a boundary segmentF
        return null;
    }

    private boolean isDifferentRegion(RasterSquare r) {
        return r == null || r.id == null || !r.id.equals(regionName);
    }

    private Point.Double getSharedPoint(RasterLineSegment s, RasterLineSegment sPrime) {
        Point.Double s1 = new Point.Double(s.x1, s.y1);
        Point.Double s2 = new Point.Double(s.x2, s.y2);

        Point.Double sPrime1 = new Point.Double(sPrime.x1, sPrime.y1);
        Point.Double sPrime2 = new Point.Double(sPrime.x2, sPrime.y2);

        if (s1.equals(sPrime1) || s1.equals(sPrime2)) {
            return s1;
        } else {
            return s2;
        }
    }

    private Point.Double getUnsharedPoint(RasterLineSegment s, RasterLineSegment sPrime) {
        Point.Double s1 = new Point.Double(s.x1, s.y1);
        Point.Double s2 = new Point.Double(s.x2, s.y2);

        Point.Double sPrime1 = new Point.Double(sPrime.x1, sPrime.y1);

        if (s1.equals(sPrime1)) {
            //s1 is the shared point
            return s2;
        } else {
            return s1;
        }
    }

    private Point2D.Double getOtherPoint(RasterLineSegment s, Point2D.Double currentPoint) {
        Point.Double s1 = new Point.Double(s.x1, s.y1);
        Point.Double s2 = new Point.Double(s.x2, s.y2);

        if (s1.equals(currentPoint)) {
            //s1 is the shared point
            return s2;
        } else {
            return s1;
        }
    }

    /**
     * Returns the centroid of the square closest to the centroid of this region
     *
     * @return
     */
    private Pair<Double, Double> getRegionCentroid() {
        double x = 0;
        double y = 0;
        for (RasterSquare r : region) {
            x += r.x + r.width / 2;
            y += r.y + r.height / 2;
        }
        x = x / region.size();
        y = y / region.size();

        //find the square closest to the centroid.
        double bestDist = Double.MAX_VALUE;
        RasterSquare bestSquare = null;
        for (RasterSquare r : region) {
            double rx = r.x + r.width / 2;
            double ry = r.y + r.height / 2;
            double squaredDistance = Math.pow(rx - x, 2) + Math.pow(ry - y, 2);
            if (squaredDistance < bestDist) {
                bestDist = squaredDistance;
                bestSquare = r;
            }
        }
        if (bestSquare == null) {
            return null;
        }
        return new Pair(bestSquare.x + bestSquare.width / 2, bestSquare.y + bestSquare.height / 2);
    }

}
