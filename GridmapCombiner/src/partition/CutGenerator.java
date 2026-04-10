/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package partition;

import partition.mAxis.Corner;
import partition.mAxis.MedialAxis;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nl.tue.geometrycore.util.Pair;

/**
 *
 * @author msondag
 */
public class CutGenerator {

    private MedialAxis ma;
    private final PartitionPolygon polygon;

    private final double dilationThreshold;

    CutGenerator(PartitionPolygon polygon, double dilationThreshold) {
        this.polygon = polygon;
        this.dilationThreshold = dilationThreshold;
    }

    /**
     * Candidate cuts start at a corner and at a different point on the
     * polygon. The set of candidate cuts is planar.
     *
     * @return
     */
    public List<Cut> getCandidateCuts() {
        List<Cut> rawCuts = new ArrayList();
        //generate the medial axis. 
        //Utility.startTimer("medial axis");
        ma = new MedialAxis(polygon);
        //note that the medial axis also contains the exterior medial axis.
        //we remove any exterior cuts at the end, this is quicker than removing the exterior segments beforehand.
        //Utility.endTimer("medial axis");

//        //for debug/illustration purposes. Print the medial axis
//        int count = Utility.getNextCount();
//        ma.toIpe("medial" + count + ".ipe");


        //Remove the leaf nodes as cut's cannot go over this.
        ma.trimLeafs();

        //for debug/illustration purposes. Print the medial axis after trimming leaf nodes
//        ma.toIpe("medialTrim" + count +".ipe");
        //For each medial segment, define which boundary segments form it and are thus opposite of each other.
        //These define potential cuts
        ma.assignOpposites();

        //for debug/illustration purposes. Print the medial axis
//        ma.toIpe("medialOpposites" + count +".ipe");
        //get all corners and their opposite points
        List<Corner> corners = ma.getCorners();

        //extract the cuts from the corners
        for (Corner c : corners) {
            for (Cut cut : c.getCuts()) {
                cut.computeDilation(polygon);
                if (cut.dilation <= dilationThreshold) {
                    //add it if the dilation is "low" enough. i.e. High detour factor
                    rawCuts.add(cut);
                }
            }
        }

        
        System.out.println("  " + rawCuts.size() + " cuts found");
        
        //remove duplicate cuts
        removeDuplicateCuts(rawCuts);
        //remove cuts that start and end at the same segment. Should not happen, but precision errors are a thing and these don't form valid cus.
        removeSameSegmentCuts(rawCuts, polygon.getSegments());

        removeExteriorCuts(rawCuts, polygon);
        
        System.out.println("  " + rawCuts.size() + " cuts remaining");

        //for debug/illustration purposes. Print the medial axis and the rawcandidate cuts
//        ma.toIpe("medialCutsTrimmed" + count +".ipe", rawCuts);
        return rawCuts;
    }

    /**
     * Remove duplicate cuts.
     *
     * @param rawCuts
     */
    private void removeDuplicateCuts(List<Cut> rawCuts) {
        //keep on of every cut
        List<Cut> filteredCuts = new ArrayList();

        for (Cut c : rawCuts) {
            boolean present = false;
            for (Cut cPrime : filteredCuts) {
                if (c.equals(cPrime)) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                filteredCuts.add(c);
            }
        }
        rawCuts.clear();
        rawCuts.addAll(filteredCuts);
    }

    /**
     * remove cuts that start and end at the same segment.
     *
     * @param rawCuts
     * @param segments
     */
    private void removeSameSegmentCuts(List<Cut> rawCuts, List<PartitionSegment> segments) {
        Set<Cut> discardCuts = new HashSet();
        for (Cut c : rawCuts) {
            Pair<PartitionSegment, PartitionSegment> segmentEndpoints = c.getSegmentEndpoints(segments);
            PartitionSegment first = segmentEndpoints.getFirst();
            PartitionSegment second = segmentEndpoints.getSecond();
            //if they are the same we are done
            if (first == second) {
                discardCuts.add(c);
            }
            //if the cut is completely part of a segment it is also done,
            if (first.onBoundary(c.start) && first.onBoundary(c.end)) {
                discardCuts.add(c);
            }
            if (second.onBoundary(c.start) && second.onBoundary(c.end)) {
                discardCuts.add(c);
            }

        }
        rawCuts.removeAll(discardCuts);
        //System.out.println("Removing " + discardCuts.size() + " that start and end on the same segment");
    }

    /**
     *
     * @param rawCuts
     * @param polygon
     */
    private void removeExteriorCuts(List<Cut> rawCuts, PartitionPolygon polygon) {
        /**
         * Cuts can be over the exterior medial axis. Just remove these. Faster
         * to do it here than to remove the exterior medial axis itself.
         */
        Set<Cut> discardCuts = new HashSet();
        for (Cut c : rawCuts) {
            if (!polygon.containsCut(c)) {
                discardCuts.add(c);
            }
        }
        rawCuts.removeAll(discardCuts);
    }
}
