package partition;

import common.Outline;
import java.util.List;

/**
 *
 * @author Wouter Meulemans
 */
public enum CutType {

    ENDPOINTS, SHORTEST, COMBINED;

    public boolean nonCrossingCuts() {
        // all implemented methods generate noncrossing cuts.
        // If a method is to be added that uses crossing cuts, then it should return false here
        return true;
    }

    public List<Cut> generateCuts(Outline outline) {
        return CutGenerator.getCandidateCuts(outline, this); // current generator just switches based on the current two types...
    }
}
