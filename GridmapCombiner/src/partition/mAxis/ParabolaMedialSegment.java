/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package partition.mAxis;

import partition.PartitionPolygon;
import partition.PartitionSegment;
import partition.Utility;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Line;
import nl.tue.geometrycore.geometry.linear.LineSegment;

/**
 *
 * @author msondag
 */
public class ParabolaMedialSegment extends MedialSegment {

    //Definition of the parabola
    Vector focusPoint;
    Line parabolaLine;

    //alternative definition of the line for the line of the parabola. ax+bc+c=0
    double a, b, c;

    ParabolaMedialSegment(Vector p1, Vector p2, Vector p3, String a, String b, String c) {
        super(p1, p2);
        this.focusPoint = p3;
        this.a = Double.parseDouble(a);
        this.b = Double.parseDouble(b);
        this.c = Double.parseDouble(c);
        parabolaLine = getLineFromCgalLine(this.a, this.b, this.c);
    }

    private Line getLineFromCgalLine(double a, double b, double c) {
        //cgal line is defined as ax+by+c=0

        Vector origin;
        Vector direction;
        if (b != 0) {
            //Rewrite to y=... and solving for x=0
            origin = new Vector(0, -c / b);
            //direction. d/dx of the y=....
            direction = new Vector(1, -a / b);
        } else {//vertical line
            //Rewrite to x=... and solving for y=0
            origin = new Vector(-c / a, 0);
            //horizontal line
            direction = new Vector(0, 1);
        }
        //direction.normalize();
        //normalize it. For some problem this is giving library conflicts so just do it manually
        double x = direction.getX();
        double y = direction.getY();
        double len = Math.sqrt(x*x+y*y);
        direction = new Vector(x/len,y/len);
        

        Line l = new Line(origin, direction);
        return l;
    }

    @Override
    public void addDefiningGeometries(PartitionPolygon polygon) {
        definingPoint1 = focusPoint;

        //faster way possible
        List<Vector> startProjectionPoints = getProjectionPoints(polygon, getStart());
        List<Vector> endProjectionPoints = getProjectionPoints(polygon, getEnd());



        //lines formed by 1 more line segment, both need a point on it
        for (PartitionSegment ps : polygon.getSegments()) {
            Vector startVector = null;
            for (Vector v : startProjectionPoints) {
                if (!v.isEqual(focusPoint) && ps.onBoundary(v)) {
                    startVector = v;
                    break;
                }
            }
            if (startVector == null) {
                continue;
            }
            Vector endVector = null;
            for (Vector v : endProjectionPoints) {
                if (!v.isEqual(focusPoint) && ps.onBoundary(v)) {
                    endVector = v;
                    break;
                }
            }
            if (endVector == null) {
                continue;
            }

            definingLine1 = new LineSegment(startVector, endVector);
            break;//can only be 1 segment for a parabola
        }
    }

    /**
     * Returns the oppositePoint point of p on this segment or an empty list if
     * it is not part of any. Returns either the focus, or the endpoints of the
     * defining line segment
     *
     * @return
     */
    @Override
    public List<Vector> getOppositePoints(Vector p) {

        if (p.isEqual(definingPoint1)) {
            if (definingLine1 == null) {
                //Utility.debug("p = " + p);
                return null;
            }
            return Arrays.asList(definingLine1.getStart(), definingLine1.getEnd());
        } else if (definingLine1.onBoundary(p)) {
            return Arrays.asList(definingPoint1);
        } else {
          //  System.out.println("Vector " + p + " is not on the parabola " + this);
            return new ArrayList();
        }
    }

    public List<LineSegment> toLineSegments() {

        //parabola equation: y= aPx^2+bPy%2+cp^x+dy+exy+f=0
        List<LineSegment> segments = new ArrayList();
        Vector start = getStart();
        Vector end = getEnd();
        double startX = start.getX();
        double endX = end.getX();

        double startY = start.getY();
        double endY = end.getY();

        boolean startTop = eq(formulaTop(startX), startY, 1);
        boolean endTop = eq(formulaTop(endX), endY, 1);

        if (startTop && endTop) {
            //both on the top
            segments.addAll(topSegment(start, end));
        } else if (!startTop && !endTop) {
//            //both on the bottom
            segments.addAll(bottomSegment(start, end));
        } else if (!startTop && endTop) {
            return Arrays.asList(new LineSegment(start, end));
            //TODO: Error in formula for getBottom/getTop, seems to sometimes return from top/bottom.
            //For now just return a straight line

//            System.err.println("Not working yet");
//            //start at bottom, end at top. Need to interpolate through peak
//            Vector peak = getPeak();
//            segments.addAll(bottomSegment(start, peak));
//            segments.addAll(topSegment(peak, end));
        } else if (startTop && !endTop) {
            return Arrays.asList(new LineSegment(start, end));
            //TODO: Error in formula for getBottom/getTop, seems to sometimes return from top/bottom.
            //For now just return a straight line

//            //start at top, end at bottom. Need to interpolate through peak     
//            Vector peak = getPeak();
//            segments.addAll(topSegment(start, peak));
//            segments.addAll(bottomSegment(peak, end));
        }
        return segments;
    }

    private List<LineSegment> topSegment(Vector p1, Vector p2) {
        List<LineSegment> segments = new ArrayList();

        double diff = p2.getX() - p1.getX();

        for (int i = 0; i < 10; i++) {
            double x1 = p1.getX() + diff / 10 * i;
            double x2 = p1.getX() + diff / 10 * (i + 1);
            double y1 = formulaTop(x1);
            double y2 = formulaTop(x2);
            LineSegment ls = new LineSegment(new Vector(x1, y1), new Vector(x2, y2));
            segments.add(ls);
        }
        return segments;
    }

    private List<LineSegment> bottomSegment(Vector p1, Vector p2) {
        List<LineSegment> segments = new ArrayList();

        double diff = p2.getX() - p1.getX();

        for (int i = 0; i < 10; i++) {
            double x1 = p1.getX() + diff / 10 * i;
            double x2 = p1.getX() + diff / 10 * (i + 1);
            double y1 = formulaBottom(x1);
            double y2 = formulaBottom(x2);
            LineSegment ls = new LineSegment(new Vector(x1, y1), new Vector(x2, y2));
            segments.add(ls);
        }
        return segments;
    }

    public double formulaTop(double x) {

        //parabola equation: y= aPx^2+bPy%2+cp^x+dy+exy+f=0
        double px = focusPoint.getX();
        double py = focusPoint.getY();

        double t = a * a + b * b;
        double aP = t - (a * a);
        double bP = t - (b * b);
        double cP = (-2 * t * px) - (2 * c * a);
        double d = (-2 * t * py) - (2 * c * b);
        double e = -2 * a * b;
        double f = (-c * c) + (t * px * px) + (t * py * py);

        //solve for y on the top
        return (-1 / (2 * bP)) * (d + e * x + Math.sqrt((d + e * x) * (d + e * x) - 4 * bP * (x * (aP * x + cP) + f)));
    }

    public double formulaBottom(double x) {

        //parabola equation: y= aPx^2+bPy%2+cp^x+dy+exy+f=0
        double px = focusPoint.getX();
        double py = focusPoint.getY();

        double t = a * a + b * b;
        double aP = t - (a * a);
        double bP = t - (b * b);
        double cP = (-2 * t * px) - (2 * c * a);
        double d = (-2 * t * py) - (2 * c * b);
        double e = -2 * a * b;
        double f = (-c * c) + (t * px * px) + (t * py * py);

        //solve for y on the bottom
        return (-1 / (2 * bP)) * (d + e * x - Math.sqrt((d + e * x) * (d + e * x) - 4 * bP * (x * (aP * x + cP) + f)));
    }

    private boolean eq(double x1, double x2, int digits) {
        double x1P = Math.round(x1 * Math.pow(10, digits)) / Math.pow(10, digits);
        double x2P = Math.round(x2 * Math.pow(10, digits)) / Math.pow(10, digits);

        return x1P == x2P;
    }

}
