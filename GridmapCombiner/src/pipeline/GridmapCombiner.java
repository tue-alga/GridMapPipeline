package pipeline;

import integration.Stopwatch;
import arrange.main.Hexastuff;
import arrange.parameter.ParameterManager;
import assign.GridMapLP;
import integration.SiteMap;
import integration.Stage;
import integration.WKT;
import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.tue.geometrycore.geometry.BaseGeometry;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.io.ReadItem;
import nl.tue.geometrycore.io.ipe.IPEReader;
import org.apache.commons.cli.*;
import partition.GridMapPartioner;

/**
 * Main controller for generating a gridmap. Executes the required programs to
 * run the pipeline for gridmaps.
 *
 * @author msondag
 */
public class GridmapCombiner {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
    
        // WKT, dilation (inverse..), productivity, hexagons
         new GridmapCombiner("wkts/NL_municipalities2017.wkt", 0.33, 4, false); // teaser
         new GridmapCombiner("wkts/GB_localauthorities.wkt", 0.33, 15, true); // fig 7
         new GridmapCombiner("wkts/UK_constituencies.wkt", 0.33, 20, false); // fig 11


//which settings to generate gridmaps for. Needs at least 1 option from every categorgie.
        
        //We are constructing args in here for convenience, so we can generate multiple settings. 
        //Easily adapted to work via the commandline instead.
//        List<Double> dilations = new ArrayList(); //default 0.33
//        dilations.add(0.0);
//        dilations.add(0.33);
//        dilations.add(0.2);
//        dilations.add(0.5);

//        List<Integer> productivities = new ArrayList();//default 10
//        productivities.add(15);
        //  productivities.add(20);
//        productivities.add(10);
//        productivities.add(4);
//        productivities.add(10);
//        productivities.add(1);

        //Set the inputmap combined with where the sites are located. Sites need to have colors.
//        List<Pair<String, String>> mapAndSites = new ArrayList();
////        mapAndSites.add(new Pair("nederland2014Outline.ipe", "NetherlandsMunicipality2014SitesColor.tsv"));
//        mapAndSites.add(new Pair("nederlandOutline.ipe", "NetherlandsMunicipalitySitesColor.tsv"));
//        mapAndSites.add(new Pair("nederlandProvinces.ipe", "NetherlandsMunicipalitySitesColor.tsv"));
//
//        mapAndSites.add(new Pair("usaOutline.ipe", "USAStatesColor.tsv"));
//        mapAndSites.add(new Pair("usaOutlineAndDc.ipe", "USAStatesAndDCColor.tsv"));
        //  mapAndSites.add(new Pair("UKMap.ipe", "UKConstituenciesColor.tsv"));
//        mapAndSites.add(new Pair("UKMapLocalAuthoritiesStub.ipe", "UKLocalAuthoritiesColor.tsv"));

//        for (double dilation : dilations) {
//            for (int productivity : productivities) {
//                for (Pair<String, String> mapAndSite : mapAndSites) {
//                    String map = mapAndSite.getFirst();
//                    String sites = mapAndSite.getSecond();
//
//                    String outputFolderName = "combinedOutput/" + map + "Dil" + dilation + "Pro" + productivity + "/";
//                    // System.out.println("exporting to folder: " + outputFolderName);
//
//                    int length = 10;
//                    args = new String[length];
//                    args[0] = "-m";
//                    args[1] = "../Data/maps/" + map;
//                    args[2] = "-o";
//                    args[3] = "../Data/" + outputFolderName;
//                    args[4] = "-s";
//                    args[5] = "../Data/maps/Sites/" + sites;
//                    args[6] = "-d";
//                    args[7] = "" + dilation;
//                    args[8] = "-p";
//                    args[9] = "" + productivity;
//
//                    new GridmapCombiner(args);
//                    // System.out.println("Done with " + outputFolderName);
//                }
//            }
//        }
    }

    //input ipe path
    private String mapPath;
    //input site path
    private String siteDataPath;

    //output of mosaicmaps
    private String mosaicOutputPath;
    //output of gridmaps
    private String gridmapOutputPath;

    //folder for temporary output.
    private String outputPath;
    //all intermediate files that get generated after partition
    private String partitionOutputPath;
    private String mosaicMapWeightInputPath;
    private String labeledPartitionOutput;
    private String labeledSiteFile;

    //partitioning paramaters
    private Double dilationThreshold;
    private Integer productivityThreshold;

    //used to calculate runtimes
    HashMap<String, Long> timing = new HashMap();

    /**
     * Generates a gridmap from the arguments by calling .jar files of other
     * programs. Outputs intermediate files for each program.
     *
     * @param args
     */
    public GridmapCombiner(String wkt, double dilation, int productivity, boolean hex) throws IOException {

        File in = new File("../Data/" + wkt);
        File outdir = new File("../Output/" + wkt+"Dil"+dilation+"Prod"+productivity+(hex ? "HEX": "SQR"));

        PrintStream oldps = System.out;
        System.out.println("Starting " + wkt);
        try {
            File f = new File(outdir, "log.txt");
            f.getParentFile().mkdirs();
            System.setOut(new PrintStream(f));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GridmapCombiner.class.getName()).log(Level.SEVERE, null, ex);
            System.setOut(oldps);
        }

        SiteMap sitemap = WKT.read(in);
        
        oldps.println("  Partitioning");
        GridMapPartioner.main(sitemap, dilation, productivity, false, outdir);
        
        oldps.println("  Arranging");       
        Hexastuff.main(sitemap, hex, true, outdir);
                
        oldps.println("  Assigning");
        GridMapLP.main(sitemap, hex, outdir);

        sitemap.stage = Stage.ASSIGNED;                
        WKT.write(new File(outdir, "output.wkt"), sitemap);

        Stopwatch.printAndClear();

        System.setOut(oldps);

        System.out.println("Done");
    }

    /**
     * Generates a gridmap from the arguments by calling .jar files of other
     * programs. Outputs intermediate files for each program.
     *
     * @param args
     */
    public GridmapCombiner(String[] args) throws IOException {
        //get the settings
        parseArguments(args);

        //make a partition using the input map (and the input sites if density is enabled)
        //timing.put(mapPath + " start", System.currentTimeMillis());
        generatePartition();

        //timing.put(mapPath + " Partition generated", System.currentTimeMillis());
        //make a weight file using output of partition and input sites.
//        //also ensure that there is a label in each partition
        makeExtraFiles();
        //timing.put(mapPath + " Extra files generated", System.currentTimeMillis());
//        //Make a mosaic map using the weight file and the output of partition
        generateMosaicMap();
        //timing.put(mapPath + " Mosaic map generated", System.currentTimeMillis());
//        //make a gridmap using the output of the mosaic map and the input sites
        generateGridMap();
        //timing.put(mapPath + " grid map generated", System.currentTimeMillis());

        Stopwatch.printAndClear();

        //print the running times
//        for (String key : timing.keySet()) {
//            System.out.println("key = " + key);
//            System.out.println(timing.get(key));
//        }
    }

    private void parseArguments(String[] args) {
        Options options = new Options();

        //input specifiers
        Option input = new Option("m", "map", true, "input Ipe file of the map. Supplies The base polygons to be used for the map.");
        input.setRequired(true);
        options.addOption(input);

        Option inputData = new Option("s", "sites", true, "Input data file containing sites: (label, point coordinates, color (red green blue)) in tsv format");
        inputData.setRequired(true);
        options.addOption(inputData);

        //output specifiers
        Option outputData = new Option("o", "output", true, "Folder for the output files");
        outputData.setRequired(true);
        options.addOption(outputData);

        //Parameters for partitioning
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

        mapPath = cmd.getOptionValue("map");
        siteDataPath = cmd.getOptionValue("sites");
        outputPath = cmd.getOptionValue("output");

        partitionOutputPath = outputPath + "partition.ipe";
        labeledPartitionOutput = outputPath + "partitionLabeled.ipe";

        labeledSiteFile = outputPath + "siteLabeled.tsv";
        mosaicMapWeightInputPath = outputPath + "weights.tsv";
        mosaicOutputPath = outputPath + "mosaicMapOutput.ipe";
        gridmapOutputPath = outputPath + "gridMapOutput.ipe";

        File f = new File(outputPath);
        f.mkdir();
        //partitioning paramters
        dilationThreshold = Double.parseDouble(cmd.getOptionValue("dilation"));
        productivityThreshold = (int) Double.parseDouble(cmd.getOptionValue("productivity"));
    }

    /**
     * Generates a partition from the settings. Waits until the partition is
     * generated. Output partition is in {@code partitionOutputPath}
     */
    private void generatePartition() throws IOException {
        String[] args = {"-i", mapPath,
            "-s", siteDataPath,
            "-o", partitionOutputPath,
            "-d", dilationThreshold + "",
            "-p", productivityThreshold + ""};
        GridMapPartioner.main(args);
    }

    /**
     * Makes the extra required files once the partition is known. Weightfile:
     * amount of sites per region Sitefile with parent: Holds for each site
     * which region it is in. LabeledPartitionFile: Adds a label to each
     * partition region
     */
    private void makeExtraFiles() {
        try {
            File f = new File(partitionOutputPath);
            IPEReader ipeReader = IPEReader.fileReader(f);
            List<ReadItem> polygons = getPolygons(ipeReader);
            List<Site> sites = getSites();
            String weightOutputString = "";

            //will hold the labels to be added.
            ArrayList<String> labelStrings = new ArrayList();

            for (ReadItem ri : polygons) {
                int weight = 0;
                //get the site that is most central in the polygon ri.
                Site centralSite = getCentralSite((Polygon) ri.getGeometry(), sites);
                if (centralSite == null) {
                    //some regions may be empty. We do not represent these.
                    continue;
                }

                //count the amount of sites
                for (Site s : sites) {
                    Polygon p = (Polygon) ri.getGeometry();
                    if (p.contains(new Vector(s.c.x, s.c.y))) {
                        s.parent = centralSite.label;
                        weight++;
                    }
                }

                //give the region the name of one of the sites inside
                String label = centralSite.label;
                weightOutputString += label + "\t" //add to the weightfileString
                        + weight + "\n";
                labelStrings.add(getIpeLabel(centralSite.c, centralSite.label));

            }
            Files.write(Paths.get(mosaicMapWeightInputPath), weightOutputString.getBytes()); //Write the weight per region.
            writeLabeledFile(partitionOutputPath, labelStrings); //add labels to the ipe file
            writeSiteFile(labeledSiteFile, sites); //write a file that contains for each site which label it is in.

        } catch (IOException ex) {
            Logger.getLogger(GridmapCombiner.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Generates a mosaicmap in mosaicOutputPath.
     */
    private void generateMosaicMap() {
        String[] args = {"-map", labeledPartitionOutput,
            "-data", mosaicMapWeightInputPath,
            "-ipe", mosaicOutputPath,
            "-unit", "1",
            "-square"};
        if (mapPath.contains("LocalAuthorities")) {
            //local authorities needs a hex map
            args[args.length - 1] = "-hexagonal";
        }

        Hexastuff.main(args);
    }

    /**
     * Generates the gridmap in gridmapOutputPath
     */
    private void generateGridMap() throws IOException {

        String[] args = {
            "-m", mosaicOutputPath,
            "-s", labeledSiteFile,
            "-o", gridmapOutputPath,
            "-sqr"
        };

        //LocalAuthorities needs a hex map
        if (mapPath.contains("LocalAuthorities")) {
            args[args.length - 1] = "-hex";
        }

        GridMapLP.main(args);
    }

    private String getIpeLabel(Coordinate c, String label) {
        return "<text pos=\"" + c.x + " " + c.y + "\" stroke=\"black\" type=\"label\" layer=\"Labels\" valign=\"baseline\">" + label + "</text>";
    }

    /**
     * Puts regions from the same partition into an ipelayer.
     *
     * @param partitionOutputPath
     * @param labelStrings
     */
    private void writeLabeledFile(String partitionOutputPath, ArrayList<String> labelStrings) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(partitionOutputPath));

            List<String> outputLines = new ArrayList();
            for (String line : lines) {
                if (line.startsWith("<layer name=")) {
                    //add the layer "Labels"
                    outputLines.add(line);
                    outputLines.add("<layer name=\"Labels\"/>");
                    outputLines.add("<view layers=\"default Labels\" active=\"Labels\"/>");
                } else if (line.startsWith("</page>")) {
                    //add the labels at the end
                    outputLines.addAll(labelStrings);
                    outputLines.add(line);
                } else {
                    //nothing special, copy the line over.
                    outputLines.add(line);
                }
            }
            Files.write(Paths.get(labeledPartitionOutput), outputLines);

        } catch (IOException ex) {
            Logger.getLogger(GridmapCombiner.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Writes all sites to a file.
     *
     * @param labeledSiteFile
     * @param sites
     */
    private void writeSiteFile(String labeledSiteFile, List<Site> sites) {
        try {
            List<String> lines = new ArrayList();
            for (Site s : sites) {
                lines.add(s.toString());
            }
            Files.write(Paths.get(labeledSiteFile), lines);

        } catch (IOException ex) {
            Logger.getLogger(GridmapCombiner.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns all sites in the inputfile path format:
     * {label}\t{x}\t{y}\t{red}\t{green}\t{blue}. red/green/blue values between
     * 0 and 255.
     *
     * @return
     */
    private List<Site> getSites() {
        List<Site> sites = new ArrayList();
        try {
            List<String> lines = Files.readAllLines(Paths.get(siteDataPath));
            for (String line : lines) {
                String[] split = line.split("\t");
                String label = split[0];
                double x = Double.parseDouble(split[1]);
                double y = Double.parseDouble(split[2]);
                int r = Integer.parseInt(split[3]);
                int g = Integer.parseInt(split[4]);
                int b = Integer.parseInt(split[5]);
                Site s = new Site(label, new Coordinate(x, y), new Color(r, g, b));
                sites.add(s);

            }
        } catch (IOException ex) {
            Logger.getLogger(GridmapCombiner.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return sites;
    }

    /**
     * Returns all polygons in the file to be read. Only reads the first page of
     * the ipefile.
     *
     * @param ipeReader
     * @return
     * @throws IOException
     */
    private List<ReadItem> getPolygons(IPEReader ipeReader) throws IOException {
        List<ReadItem> polygonList = new ArrayList();
        List<ReadItem> items = ipeReader.read(1); //only read the firstpage
        for (ReadItem ri : items) {
            BaseGeometry geometry = ri.getGeometry();

            if (geometry.getClass() == Polygon.class) {
                polygonList.add(ri);
            }
        }
        return polygonList;
    }

    /**
     * Returns the site from {@code sites} that is 1: inside p. 2: The closest
     * to the average position of all sites in p.
     *
     * @param p
     * @param sites
     * @return
     */
    private Site getCentralSite(Polygon p, List<Site> sites) {
        List<Site> inPolygonSites = new ArrayList();
        for (Site s : sites) {
            if (p.contains(new Vector(s.c.x, s.c.y))) {
                inPolygonSites.add(s);
            }
        }
        //get the average centerX,centerY of the sites
        double centerX = 0;
        double centerY = 0;
        for (Site s : inPolygonSites) {
            centerX += s.c.x;
            centerY += s.c.y;
        }
        centerX = centerX / (double) inPolygonSites.size();
        centerY = centerY / (double) inPolygonSites.size();
        //return the site with the smallest distance to the center
        Site bestSite = null;
        double bestDistance = Double.MAX_VALUE;
        for (Site s : inPolygonSites) {
            double distance = s.c.distance(new Coordinate(centerX, centerY));
            if (distance < bestDistance) {
                bestDistance = distance;
                bestSite = s;
            }
        }
        return bestSite;
    }

    //Used for the commandlineparsing. Makes sure that all output from subprocesses spawned is shown when running this jar
    private void inheritIO(final InputStream src, final PrintStream dest) {
        new Thread(new Runnable() {
            public void run() {
                Scanner sc = new Scanner(src);
                while (sc.hasNextLine()) {
                    dest.println(sc.nextLine());
                }
            }
        }).start();
    }
}
