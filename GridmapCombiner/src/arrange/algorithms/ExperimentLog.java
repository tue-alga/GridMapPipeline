package arrange.algorithms;

import java.util.ArrayList;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class ExperimentLog {

    private int numberOfTiles;
    private int cartographicError;
    private int totalSymmetricDifference;
    private double totalNormalizedSymmetricDifference;
    private long totalTime;
    private long preprocessingTime;
    private long reshapeTime;
    private long flowTime;
    private ArrayList<RegionData> regionData = new ArrayList<>();

    public static ExperimentLog mergeLogs(ExperimentLog l1, ExperimentLog l2) {
        ExperimentLog merged = new ExperimentLog();
        merged.numberOfTiles = l1.numberOfTiles + l2.numberOfTiles;
        merged.cartographicError = l1.cartographicError + l2.cartographicError;
        merged.totalSymmetricDifference = l1.totalSymmetricDifference + l2.totalSymmetricDifference;
        merged.totalNormalizedSymmetricDifference = l1.totalNormalizedSymmetricDifference + l2.totalNormalizedSymmetricDifference;
        merged.totalTime = l1.totalTime + l2.totalTime;
        merged.preprocessingTime = l1.preprocessingTime + l2.preprocessingTime;
        merged.reshapeTime = l1.reshapeTime + l2.reshapeTime;
        merged.flowTime = l1.flowTime + l2.flowTime;
        merged.regionData.addAll(l1.regionData);
        merged.regionData.addAll(l2.regionData);
        return merged;
    }

    public int getNumberOfTiles() {
        return numberOfTiles;
    }

    public void setNumberOfTiles(int numberOfTiles) {
        this.numberOfTiles = numberOfTiles;
    }

    public int getTotalCartographicError() {
        return cartographicError;
    }

    public void setTotalCartographicError(int totalCartographicError) {
        this.cartographicError = totalCartographicError;
    }

    public int getTotalSymmetricDifference() {
        return totalSymmetricDifference;
    }

    public void setTotalSymmetricDifference(int totalSymmetricDifference) {
        this.totalSymmetricDifference = totalSymmetricDifference;
    }

    public double getTotalNormalizedSymmetricDifference() {
        return totalNormalizedSymmetricDifference;
    }

    public void setTotalNormalizedSymmetricDifference(double totalNormalizedSymmetricDifference) {
        this.totalNormalizedSymmetricDifference = totalNormalizedSymmetricDifference;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public long getPreprocessingTime() {
        return preprocessingTime;
    }

    public void setPreprocessingTime(long preprocessingTime) {
        this.preprocessingTime = preprocessingTime;
    }

    public long getReshapeTime() {
        return reshapeTime;
    }

    public void setReshapeTime(long reshapeTime) {
        this.reshapeTime = reshapeTime;
    }

    public long getFlowTime() {
        return flowTime;
    }

    public void setFlowTime(long flowTime) {
        this.flowTime = flowTime;
    }

    public void addRegionData(String name, int desiredSize, int actualSize, int symmetricDifference) {
        regionData.add(new RegionData(name, desiredSize, actualSize, symmetricDifference));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Number of tiles: ");
        sb.append(numberOfTiles);
        sb.append(System.lineSeparator());
        sb.append("Cartographic error: ");
        sb.append(cartographicError);
        sb.append(System.lineSeparator());
        sb.append("Total symmetric difference: ");
        sb.append(totalSymmetricDifference);
        sb.append(System.lineSeparator());
        sb.append("Normalized symmetric difference: ");
        sb.append(String.format("%.2f", totalNormalizedSymmetricDifference));
        sb.append(System.lineSeparator());
        sb.append("Total execution time: ");
        sb.append(totalTime);
        sb.append(System.lineSeparator());
        sb.append("Preprocessing time: ");
        sb.append(preprocessingTime);
        sb.append(System.lineSeparator());
        sb.append("Reshape time: ");
        sb.append(reshapeTime);
        sb.append(System.lineSeparator());
        sb.append("Flow model time: ");
        sb.append(flowTime);
        sb.append(System.lineSeparator());
        sb.append("Region summary:");
        sb.append(System.lineSeparator());
        sb.append(String.format("%8s", "Region"));
        sb.append(String.format("%14s", "Desired size"));
        sb.append(String.format("%13s", "Actual size"));
        sb.append(String.format("%12s", "Sym. Diff."));
        sb.append(String.format("%13s", "%Sym. Diff."));
        for (RegionData data : regionData) {
            double percentSymDiff = 100 * ((double) data.symmetricDifference) / ((double) data.desiredSize);
            sb.append(String.format("%8s", data.name));
            sb.append(String.format("%14d", data.desiredSize));
            sb.append(String.format("%13d", data.actualSize));
            sb.append(String.format("%12d", data.symmetricDifference));
            sb.append(String.format("%13.2f", percentSymDiff));
            sb.append("%");
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    private class RegionData {

        private final String name;
        private final int desiredSize;
        private final int actualSize;
        private final int symmetricDifference;

        public RegionData(String name, int desiredSize, int actualSize, int symmetricDifference) {
            this.name = name;
            this.desiredSize = desiredSize;
            this.actualSize = actualSize;
            this.symmetricDifference = symmetricDifference;
        }
    }
}
