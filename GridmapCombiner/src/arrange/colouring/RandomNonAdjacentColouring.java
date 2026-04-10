package arrange.colouring;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

import arrange.colouring.colourschemes.ColourScheme;
import arrange.model.subdivision.Map;
import arrange.model.subdivision.Map.Face;
import arrange.model.subdivision.Map.Halfedge;

public class RandomNonAdjacentColouring extends Colouring {

    protected ColourScheme colourScheme;
    
    public RandomNonAdjacentColouring(ColourScheme colourScheme) {
        this.colourScheme = colourScheme;
    }
    
    @Override
    public void assignColours(Map map) {
        // reset face colours
        for (Face face : map.boundedFaces()) {
            face.setColor(null);
        }
        
        // set face colours
        for (Face face : map.boundedFaces()) {
            // find colours of neighbouring faces
            Set<Color> neighbourColours = new HashSet<>();
            for (Halfedge he : face.getBoundaryHalfedges()) {
                Face twinFace = he.getTwin().getFace();
                if (twinFace.isBounded()) {
                    neighbourColours.add(twinFace.getColor());
                }
            }
            // set face colour
            Color faceColour;
            do {
                faceColour = this.colourScheme.getRandomColour();
            } while (neighbourColours.contains(faceColour));
            face.setColor(faceColour);
        }
    }

}
