/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package partition.mAxis;

import partition.Cut;
import partition.PartitionSegment;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.curved.Circle;
import nl.tue.geometrycore.geometry.linear.LineSegment;
import nl.tue.geometrycore.geometryrendering.styling.Dashing;
import nl.tue.geometrycore.geometryrendering.styling.Hashures;
import nl.tue.geometrycore.io.ipe.IPEWriter;

/**
 *
 * @author msondag
 */
public class MedialAxisToIpe {
    
    MedialAxis ma;
    
    public MedialAxisToIpe(MedialAxis ma) {
        this.ma = ma;
        
    }
    
    public void toIpe(String fileLocation) {
        List<Corner> corners = ma.getCorners();
        //draw cuts 
        List<Cut> rawCuts = new ArrayList();
        for (Corner c : corners) {
            Set<Cut> cuts = c.getCuts();
            rawCuts.addAll(cuts);
        }
        toIpe(fileLocation, rawCuts);
    }
    
    void toIpe(String fileLocation, List<Cut> rawCuts) {
        try {
            IPEWriter writer = IPEWriter.fileWriter(new File(fileLocation));
            writer.initialize();
            
            writer.newPage("polygon", "medialaxis", "intparabolahelper", "corner", "cuts");
            writer.newView("polygon");
            writer.setLayer("polygon");
            for (PartitionSegment ps : ma.polygon.getSegments()) {
                LineSegment ls = new LineSegment(ps.getStart(), ps.getEnd());
                writer.setStroke(Color.black, 1, Dashing.SOLID);
                writer.draw(ls);
            }
            
            for (MedialSegment ms : (ma.medialSegments)) {
                writer.setLayer("medialaxis");
                
                if (ms.getClass() == ParabolaMedialSegment.class) {
//                    LineSegment ls = new LineSegment(ms.getStart(), ms.getEnd());
//                    writer.setStroke(Color.BLUE, 1, Dashing.dotted(1));
//                    writer.draw(ls);

                    ParabolaMedialSegment ps = (ParabolaMedialSegment) ms;
                    List<LineSegment> segments = ps.toLineSegments();
                    
                    for (LineSegment ls : segments) {
                        writer.setStroke(Color.BLUE, 1, Dashing.SOLID);
                        writer.draw(ls);
                    }
                    
                    writer.setLayer("intparabolahelper");
                    
                    writer.setStroke(Color.BLUE,
                                     1.5, Dashing.dotted(1.5));
                    writer.draw(new LineSegment(ps.focusPoint, ps.getStart()));
                    writer.draw(new LineSegment(ps.focusPoint, ps.getEnd()));
                    writer.setLayer("medialaxis");
                } else {
                    LineSegment ls = new LineSegment(ms.getStart(), ms.getEnd());
                    writer.setStroke(Color.PINK, 1, Dashing.SOLID);
                    writer.draw(ls);
                }
            }

            //corners
            List<Corner> corners = ma.getCorners();
            
            writer.setLayer("corner");
            for (Corner corner : corners) {
                writer.setStroke(Color.DARK_GRAY, 2, Dashing.SOLID);
                writer.setFill(Color.DARK_GRAY, Hashures.SOLID);
                Circle c = new Circle(corner.cornerPoint, 4);
                writer.draw(c);
            }

            //draw cuts 
            writer.setLayer("cuts");
            for (Cut cut : rawCuts) {
                writer.setStroke(new Color(165, 205, 226), 1, Dashing.dotted(1));
                writer.draw(cut.segment);
            }
            
            writer.close();
            
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(MedialAxis.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
