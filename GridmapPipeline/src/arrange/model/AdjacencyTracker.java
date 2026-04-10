package arrange.model;

import arrange.model.MosaicCartogram.MosaicRegion;
import java.util.Arrays;

/**
 *
 * @author Wouter Meulemans
 */
public class AdjacencyTracker {

    private final int[][] counts; // count[i][j] stores number of adjacencies for regions i and j, with i >= j

    public AdjacencyTracker(MosaicRegion[] regions) {
        final int n = regions.length;
        counts = new int[n][];
        for (int i = 0; i < n; i++) {
            counts[i] = new int[i+1];
        }
    }

    public AdjacencyTracker(AdjacencyTracker other) {
        this.counts = new int[other.counts.length][];
        for (int i = 0; i < other.counts.length; i++) {
            this.counts[i] = Arrays.copyOf(other.counts[i], other.counts[i].length);
        }
    }

    public void increase(MosaicRegion a, MosaicRegion b) {
        int i = a.getId();
        int j = b.getId();
        if (i < j) {
            i = j;
            j = a.getId();
        }
        counts[i][j]++;
    }

    public void decrease(MosaicRegion a, MosaicRegion b) {
        int i = a.getId();
        int j = b.getId();
        if (i < j) {
            i = j;
            j = a.getId();
        }
        counts[i][j]--;
    }

    public void set(MosaicRegion a, MosaicRegion b, int value) {
        int i = a.getId();
        int j = b.getId();
        if (i < j) {
            i = j;
            j = a.getId();
        }
        counts[i][j] = value;
    }

    public int get(MosaicRegion a, MosaicRegion b) {
        int i = a.getId();
        int j = b.getId();
        if (i < j) {
            i = j;
            j = a.getId();
        }
        return counts[i][j];
    }

    public boolean areAdjacent(MosaicRegion a, MosaicRegion b) {
        return get(a, b) > 0;
    }

}
