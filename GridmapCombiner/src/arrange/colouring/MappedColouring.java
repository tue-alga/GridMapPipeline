package arrange.colouring;

import java.awt.Color;
import java.util.HashMap;

import arrange.model.subdivision.Map;
import arrange.model.subdivision.Map.Face;

public class MappedColouring extends Colouring {

    protected HashMap<Double, Color> mapping;
    
    public MappedColouring(Map map) {
        mapping = new HashMap<>();
        for (Face face : map.boundedFaces()) {
            mapping.put(face.getWeight(), face.getColor());
        }
    }
    
    @Override
    public void assignColours(Map map) {
        for (Face face : map.boundedFaces()) {
            face.setColor(mapping.get(face.getWeight()));
        }
    }

}
