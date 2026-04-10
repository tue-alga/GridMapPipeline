package arrange.colouring;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import arrange.model.subdivision.Map;
import arrange.model.subdivision.Map.Face;

public class PercentileColouring extends ValueColouring {

    public PercentileColouring(Color low, double min, Color high, double max) {
        super(low, min, high, max);
    }

    public PercentileColouring(Map map) {
        super(map);
    }
    
    @Override
    public void assignColours(Map map) {
        // get list of faces from the map
        int nbfs = map.numberOfBoundedFaces();
        List<Face> bfs = new ArrayList<>(nbfs);
        for (Face bf :  map.boundedFaces()) {
            bfs.add(bf);
        }
        // sort faces on weight
        Collections.sort(bfs, new Comparator<Face>() {
            @Override
            public int compare(Face f1, Face f2) {
                return Double.compare(f1.getWeight(), f2.getWeight());
            }
        });
        // assign colours to the faces
        for (int i = 0; i < nbfs; i++) {
            bfs.get(i).setColor(getColourForRel(((float) i) / nbfs));
        }
    }

}
