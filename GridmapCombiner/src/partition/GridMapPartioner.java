/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package partition;

import integration.Outline;
import integration.Partition;
import integration.SiteMap;
import integration.Stage;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.tue.geometrycore.geometry.BaseGeometry;
import nl.tue.geometrycore.geometry.GeometryType;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.LineSegment;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.io.ReadItem;
import nl.tue.geometrycore.io.ipe.IPEReader;
import nl.tue.geometrycore.io.ipe.IPEWriter;
import nl.tue.geometrycore.util.Pair;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import integration.Stopwatch;

/**
 *
 * @author msondag
 */
public class GridMapPartioner {

    File inputIpeFile;
    File outputIpeFile;
    File siteDataFile;
    List<PartitionPolygon> inputPolygons;
    List<Site> sites;

    private double dilationThreshold;
    private int productivityThreshold;
    private boolean pretest;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        //System.out.println("Only 4 digits precision in the data allowed by default.");
        //Example arg        
        //-i ../Data/maps/nederlandProvinces.ipe -s ../Data/maps/Sites/NetherlandsMunicipalitySitesColor.tsv -o ../Data/output/partition.ipe  -d 0.2 -p 4 
        //-i ../Data/maps/nederlandOutline.ipe -s ../Data/maps/Sites/NetherlandsMunicipalitySitesColor.tsv -o ../Data/output/partition.ipe  -d 0.33 -p 4 
        //-i ../Data/maps/debugPolygons/testPolygon.ipe -s ../Data/maps/Sites/testPolygonSites.tsv -o ../Data/output/partition.ipe  -d 0.33 -p 3
        new GridMapPartioner(args);
    }

    public static void main(SiteMap sitemap, double dilation, int productivity, boolean pretest, File outdir) throws IOException {
        //System.out.println("Only 4 digits precision in the data allowed by default.");
        //Example arg        
        //-i ../Data/maps/nederlandProvinces.ipe -s ../Data/maps/Sites/NetherlandsMunicipalitySitesColor.tsv -o ../Data/output/partition.ipe  -d 0.2 -p 4 
        //-i ../Data/maps/nederlandOutline.ipe -s ../Data/maps/Sites/NetherlandsMunicipalitySitesColor.tsv -o ../Data/output/partition.ipe  -d 0.33 -p 4 
        //-i ../Data/maps/debugPolygons/testPolygon.ipe -s ../Data/maps/Sites/testPolygonSites.tsv -o ../Data/output/partition.ipe  -d 0.33 -p 3
        new GridMapPartioner(sitemap, dilation, productivity, pretest, outdir);

        sitemap.stage = Stage.PARTITIONED;
    }

    public GridMapPartioner(String[] args) throws IOException {
        pretest = false;
        parseArgs(args);
        initializeOld();
        partition();
        //Utility.printTimer();
    }

    public GridMapPartioner(SiteMap sitemap, double dilation, int productivity, boolean pretest, File outdir) throws IOException {
        dilationThreshold = dilation;
        productivityThreshold = productivity;
        this.pretest = pretest;
        outputIpeFile = new File(outdir, "partition.ipe");
        initializeNew(sitemap);

        List<PartitionPolygon> partitionedPolygons = partition();

        for (PartitionPolygon pp : partitionedPolygons) {
            Outline outline = pp.outline;

            Partition p = outline.addPartition();
            p.color = ColorPicker.getNewColor();
            p.label = outline.label + ":" + (outline.partitions.size() + 1);
            p.outline = outline;
            p.vertices().addAll(pp.vertices);
        }

        for (Outline outline : sitemap.outlines) {
            for (integration.Site s : outline.sites) {
                // and find the new partition
                for (Partition p : outline.partitions) {
                    if (p.contains(s)) {
                        p.sites.add(s);
                        s.setPartition(p);
                        break;
                    }
                }
            }
        }

        //Utility.printTimer();
    }

    public GridMapPartioner(String inputIpeFile, String pointDataFile, String outputIpeFile) throws IOException {
        this.inputIpeFile = new File(inputIpeFile);
        this.siteDataFile = new File(pointDataFile);
        this.outputIpeFile = new File(outputIpeFile);
        initializeOld();
        partition();
    }

    HashMap<String, Long> times = new HashMap();

    private boolean checkContainment() {
        boolean ok = true;
        for (Site s : sites) {
            boolean contained = false;
            for (PartitionPolygon p : inputPolygons) {
                if (p.containsPoint(s.point)) {
                    contained = true;
                }
            }
            if (!contained) {
                System.err.println("Not all points are in the inputPolygons:" + s.point + s.label);
                ok = false;
            }
        }
        return ok;
    }

    private void initializeOld() throws IOException {

        readInputPolygons();
        readPointDataFile();

        assert checkContainment();
    }

    private void initializeNew(SiteMap sitemap) {
        inputPolygons = new ArrayList();
        sites = new ArrayList();

        for (Outline outline : sitemap.outlines) {

            List<PartitionSegment> psSegment = new ArrayList();
            for (LineSegment ls : outline.edges()) {
                psSegment.add(new PartitionSegment(ls));
            }

            PartitionPolygon pp = new PartitionPolygon(psSegment);
            pp.outline = outline;

            pp.removeDegeneracies();

            inputPolygons.add(pp);

            pp.sites = new ArrayList();
            for (integration.Site s : outline.sites) {
                Site site = new Site(s.getX(), s.getY(), s.getLabel());
                site.color = s.getColor();
                sites.add(site);
                pp.sites.add(site);
            }
        }
    }

    private List<PartitionPolygon> partition() throws IOException {

        System.out.println("------ RUNNING PARTITION ---------------------------");
        System.out.println("  p = " + productivityThreshold + ", d = " + dilationThreshold + (pretest ? ", pretest" : ""));

        Stopwatch sw = Stopwatch.get("partition").start();

        List<PartitionPolygon> partitionedPolygons = new ArrayList();
        for (PartitionPolygon p : inputPolygons) {
            List<PartitionPolygon> partitions = partitionPolygon(p);
            for (PartitionPolygon pp : partitions) {
                pp.outline = p.outline;
            }
            partitionedPolygons.addAll(partitions);
        }

        //in case the polygons are neighboring, we need to add vertices at cut places.
        System.out.println("Construction new partitions");
        Stopwatch sw2 = Stopwatch.get("- construct").start();
        addExtraVertices(partitionedPolygons);
        sw2.stop();

        sw.stop();

        writeToIpe(partitionedPolygons);

//        for (String key : times.keySet()) {
//            System.out.println(key + ":" + times.get(key));
//        }
        return partitionedPolygons;
    }

    private void readInputPolygons() throws IOException {
        IPEReader reader = IPEReader.fileReader(inputIpeFile);
        List<ReadItem> items = reader.read();

        inputPolygons = new ArrayList();

        for (ReadItem item : items) {
            BaseGeometry geometry = item.getGeometry();
            if (geometry.getGeometryType() == GeometryType.POLYGON) {
                Polygon p = (Polygon) geometry.toGeometry();

                List<PartitionSegment> psSegment = new ArrayList();
                for (LineSegment ls : p.edges()) {
                    psSegment.add(new PartitionSegment(ls));
                }

                PartitionPolygon pp = new PartitionPolygon(psSegment);

                pp.removeDegeneracies();

                inputPolygons.add(pp);
            }
        }
    }

    /**
     * Partitions a specific polygon
     *
     * @param inputPolygon
     */
    private List<PartitionPolygon> partitionPolygon(PartitionPolygon inputPolygon) {

        if (pretest && inputPolygon.sites.size() < productivityThreshold * 2) {
            List<PartitionPolygon> result = new ArrayList();
            result.add(inputPolygon);
            return result;
        }

        System.out.println("Finding cuts");
        Stopwatch sw = Stopwatch.get("- cuts").start();

        CutGenerator cg = new CutGenerator(inputPolygon, dilationThreshold);
        List<Cut> cuts = cg.getCandidateCuts();

        sw.stop();

        sw = Stopwatch.get("- init").start();
        sortCutsByLength(cuts);

        GraphStructure gs = generateGraphStructure(inputPolygon, cuts);
        Set<Cut> usedCuts = new HashSet();

        sw.stop();

        System.out.println("Performing cuts");
        sw = Stopwatch.get("- perform").start();

        //process each cut, and store the resulting polygons
        List<PartitionPolygon> partitionedPolygons = new ArrayList();
        partitionedPolygons.add(inputPolygon);
        for (Cut c : cuts) {

            //get the new partitionPolygons after this cut.
            //We do not know in which partition the cut is, so we have to go in all of them.
            List<PartitionPolygon> updatedList = new ArrayList();
            for (PartitionPolygon p : partitionedPolygons) {
                if (!p.containsCut(c)) {
                    //not in partition polygon, so no change.
                    updatedList.add(p);
                } else {
                    //need to recompute dilation as the polygon gets chopped up
                    c.computeDilation(p);
                    if (c.dilation > dilationThreshold) {
                        //skip this cut, it has too little dilation
                        updatedList.add(p);
                        continue;
                    }
                    //check if the cut is productive
                    boolean productive = gs.isProductive(c, usedCuts, productivityThreshold);
                    if (productive) {
                        Pair<PartitionPolygon, PartitionPolygon> splitPolygons = p.splitPolygon(c);
                        updatedList.add(splitPolygons.getFirst());
                        updatedList.add(splitPolygons.getSecond());
                        usedCuts.add(c);
                    } else {
                        updatedList.add(p);
                    }
                }
            }
            //went through all the polygons and updated the list
            partitionedPolygons = updatedList;
        }

        sw.stop();
        System.out.println("  " + (partitionedPolygons.size() - 1) + " applied");

        return partitionedPolygons;
    }

    private void writeToIpe(List<PartitionPolygon> outputPolygons) throws IOException {
        outputIpeFile.getParentFile().mkdirs();

        IPEWriter fileWriter = IPEWriter.fileWriter(outputIpeFile);

        fileWriter.initialize();
        fileWriter.newPage();

        Collections.sort(outputPolygons, (PartitionPolygon p1, PartitionPolygon p2) -> (Double.compare(p1.getMinY(), p2.getMinY())));

        for (PartitionPolygon pp : outputPolygons) {
            //fix the precision to 3 digits
            pp.fixedPrecision(3);
            //this might cause degeneracies, so remove those.
            pp.removeDegeneracies();
            fileWriter.appendCustomPathCommand(pp.toIpe());
        }
        fileWriter.close();

    }

    private void sortCutsByLength(List<Cut> cuts) {
        //shorest cut first
        cuts.sort((Cut c1, Cut c2) -> Double.compare(c1.getLength(), c2.getLength()));
    }

    private void readPointDataFile() {
        sites = new ArrayList();
        try {
            List<String> lines = Files.readAllLines(siteDataFile.toPath());
            for (String line : lines) {
                String[] split = line.split("\t");
                String label = split[0];
                double x = Double.parseDouble(split[1]);
                double y = Double.parseDouble(split[2]);
                Site s = new Site(x, y, label);
                int red = Integer.parseInt(split[3]);
                int green = Integer.parseInt(split[4]);
                int blue = Integer.parseInt(split[5]);
                s.color = new Color(red, green, blue);
                sites.add(s);

            }
        } catch (IOException ex) {
            Logger.getLogger(GridMapPartioner.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void parseArgs(String[] args) {
//        System.out.println("printing arguments");
//        for (String arg : args) {
//            System.out.println("arg = " + arg);
//        }
        Options options = new Options();

        Option input = new Option("i", "input", true, "input ipe map");
        input.setRequired(true);
        options.addOption(input);

        Option site = new Option("s", "sites", true, "input site map");
        site.setRequired(true);
        options.addOption(site);

        Option output = new Option("o", "output", true, "output ipe location");
        output.setRequired(true);
        options.addOption(output);

        Option dilation = new Option("d", "dilation", true, "dilation threshold between 0 and 1");
        dilation.setRequired(true);
        options.addOption(dilation);

        Option productivity = new Option("p", "productivity", true, "productive threshold. Greater or larger than 1");
        productivity.setRequired(true);
        options.addOption(productivity);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }

        inputIpeFile = new File(cmd.getOptionValue("input"));
        siteDataFile = new File(cmd.getOptionValue("sites"));
        outputIpeFile = new File(cmd.getOptionValue("output"));

        dilationThreshold = Double.parseDouble(cmd.getOptionValue("dilation", "" + dilationThreshold));
        productivityThreshold = Integer.parseInt(cmd.getOptionValue("productivity", "" + productivityThreshold));
    }

    private void addExtraVertices(List<PartitionPolygon> partitionedPolygons) {
        //inefficient, can use cuts instead
        // System.out.println("adding extra vertices");
        //Add a vertex if it ends on the interior of a segment from a different polygon.
        for (PartitionPolygon p1 : partitionedPolygons) {
            for (Vector v : p1.getVertices()) {
                for (PartitionPolygon p2 : partitionedPolygons) {
                    if (p1.equals(p2)) {
                        continue;
                    }
                    PartitionSegment toSplit = null;
                    for (PartitionSegment s : p2.getSegments()) {
                        if (s.onBoundary(v) && !s.isApproxEndpoint(v)) {
                            toSplit = s;
                            break;
                        }
                    }
                    if (toSplit != null) {
                        toSplit.splitSegment(v);
                    }
                }
            }
        }
    }

    private GraphStructure generateGraphStructure(PartitionPolygon inputP, List<Cut> cuts) {
        List<Cut> remainingCuts = new ArrayList(cuts);

        PartitionPolygon inputPCopy = inputP.copy();

        List<PartitionPolygon> polygons = new ArrayList();
        polygons.add(inputPCopy);

        while (!remainingCuts.isEmpty()) {
            Cut c = remainingCuts.get(0);
            for (PartitionPolygon p : polygons) {
                if (p.containsCut(c)) {
                    Pair<PartitionPolygon, PartitionPolygon> splitPolygons = p.splitPolygon(c);
                    if (splitPolygons.getFirst().segments.size() < 3 || splitPolygons.getSecond().segments.size() < 3) {
                        System.out.println("Split err??");
                        System.out.println("Cut " + c.start + " -> " + c.end);
                        System.out.println("P");
                        for (Vector v : p.vertices) {
                            System.out.println("  " + v);
                        }

                        System.out.println("First");
                        for (Vector v : splitPolygons.getFirst().vertices) {
                            System.out.println("  " + v);
                        }

                        System.out.println("Second");
                        for (Vector v : splitPolygons.getSecond().vertices) {
                            System.out.println("  " + v);
                        }

                    }
                    polygons.add(splitPolygons.getFirst());
                    polygons.add(splitPolygons.getSecond());
                    polygons.remove(p);
                    break;
                }
            }
            remainingCuts.remove(0);
        }
        //Polygon fully partitioned. start generating the graph
        GraphStructure gs = new GraphStructure(polygons, cuts, pretest ? inputP.sites : sites);
        return gs;
    }

}
