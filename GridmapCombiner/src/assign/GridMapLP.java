/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assign;

import integration.Stage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import integration.Stopwatch;
import java.io.File;

/**
 *
 * @author msondag
 */
public class GridMapLP {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        new GridMapLP(args);
    }

    public static void main(integration.SiteMap sitemap, boolean hex, File outdir) throws IOException {
        new GridMapLP(sitemap, hex, outdir);

        sitemap.stage = Stage.ASSIGNED;
    }

    private Path inputMapPath;
    private Path sitePath;
    private Path outputPath;

    /**
     * Used in case provinces are present. In the output, cells are grouped in
     * ipelayers per province.
     */
    private Path provinceMapping = null;
    private CellContainer grid;
    private MosaicCellMap mosaicCellMap;
    private SiteMap siteMap;
    private boolean useHexTiles = false;

    public GridMapLP(String[] args) throws IOException {
        parseArguments(args);
        initializeOld();
        run();

    }

    public GridMapLP(integration.SiteMap sitemap, boolean hex, File outdir) throws IOException {
        initializeNew(sitemap, hex, outdir);
        run();

        for (Site site : siteMap.getAllSites()) {
            site.original.setCell(site.assigned.coord);
        }

    }

    private void initializeOld() {
        //cellmap holds the centroids and cell coordinates for each square/hex in the input
        mosaicCellMap = new MosaicCellMap(inputMapPath);
        //make a grid that fits all squares/hex on the map. 
        //Each cell in the gridmap is linked to a mosaicCell.
        grid = new CellContainer(mosaicCellMap);

        siteMap = new SiteMap(sitePath);
    }

    private void initializeNew(integration.SiteMap sitemap, boolean hex, File outdir) {

        mosaicCellMap = new MosaicCellMap(sitemap);
        grid = new CellContainer(mosaicCellMap);

        siteMap = new SiteMap(sitemap);

        useHexTiles = hex;
        outputPath = Paths.get(new File(outdir, "assigned.ipe").getAbsolutePath());

    }

    private void run() throws IOException {

        //check if the amount of sites matches with the amount of mosaic maps
        //does not check if this is correct per region, that can be done later.
        checkMosaicCellCount();

        System.out.println("------ ASSIGNING TO CELLS --------------------------");

        Stopwatch sw = Stopwatch.get("assign").start();

        //Shift the map such that the bounding box matches up 
        //with the bounding box of the centroids of the mosaicCells
        shiftSites();

        //add province data if we have it to the sites
        addProvinces();

        //go through each region on the map with the same label (component), and assign each site in the region to a cell in the component.
        double cost = 0;
        Stopwatch sw2 = Stopwatch.get("- solve");
        for (Component cm : getComponents()) {
            LinearProgram lp = new LinearProgram(cm);

            sw2.start();
            cost += lp.solveLP();
            sw2.stop();
        }

        sw.stop();

        // exclude analysis from running time
        System.out.println("Total sq. Euclidean distance: " + cost);
        int sitecount = siteMap.getAllSites().size();
        System.out.println("Avg displacement: " + Math.sqrt(cost / (sitecount * sitecount)));

        //check all sites assigned. Makes sure we don't have regions with too little assignments.
        //regions with too much assignment are automatically caught by checkMosaicCellsCount() now.
        checkSites();

        //write the grid to ipe.
        String outputString = grid.toIpe(useHexTiles);
        Files.writeString(outputPath, outputString);
    }

    private void parseArguments(String[] args) {
//        for (String s : args) {
//            System.out.println(s);
//        }

        Options options = new Options();

        //input specifiers
        Option mapOption = new Option("m", "inputMosaicMap", true, "input Ipe file of the mosaic map. Each tile must have a label");
        mapOption.setRequired(true);
        options.addOption(mapOption);

        Option siteOption = new Option("s", "sites", true, "Input file of sites. (parentLabel,label,x,y,red,green,blue) files format and tab seperated");
        siteOption.setRequired(true);
        options.addOption(siteOption);

        Option outputOption = new Option("o", "output", true, "Location of the output file");
        outputOption.setRequired(true);
        options.addOption(outputOption);

        Option tileOption = new Option("hex", "hex", false, "Use hex tiles");
        tileOption.setRequired(false);
        options.addOption(tileOption);
                
        Option provOption = new Option("p", "prov", true, "TSV file mapping regions to provinces");
        provOption.setRequired(false);
        options.addOption(provOption);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException ex) {
            Logger.getLogger(GridMapLP.class.getName()).log(Level.SEVERE, null, ex);
        }

        inputMapPath = Paths.get(cmd.getOptionValue("inputMosaicMap"));
        sitePath = Paths.get(cmd.getOptionValue("sites"));
        outputPath = Paths.get(cmd.getOptionValue("output"));
        if (cmd.hasOption("prov")) {
            provinceMapping = Paths.get(cmd.getOptionValue("prov"));
        } else {
            provinceMapping = null;
        }

        useHexTiles = cmd.hasOption("hex");
    }

    /**
     * shifts the sites such that their bounding box coincides with that of the
     * cells.
     */
    private void shiftSites() {
        double targetH = mosaicCellMap.getMosaicHeight();
        double targetW = mosaicCellMap.getMosaicWidth();

        siteMap.shiftTarget(targetH, targetW);

    }

    /**
     * For each partition region, return a component of the cells associated to
     * the region and the sites in the region.
     *
     * @return
     */
    private List<Component> getComponents() {
        List<Component> componentList = new ArrayList();
        for (String label : mosaicCellMap.getLabels()) {
            List<Cell> cells = grid.getCellsByLabel(label);
            List<Site> sites = siteMap.get(label);

            Component c = new Component(sites, cells);
            componentList.add(c);
        }
        return componentList;
    }

    /**
     * Verify that all sites are assigned, and there is not site assigned to
     * multiple cells.
     */
    private void checkSites() {
        List<String> labelsAssigned = new ArrayList();
        for (Cell c : grid.cells) {
            String label = c.label;
            if (label != "") {
                labelsAssigned.add(label);

            }
        }
        for (Site s : siteMap.getAllSites()) {
            if (!labelsAssigned.contains(s.label)) {
                System.err.println("Error: site " + s.label + "is not assigned a gridcell");
            }
            labelsAssigned.remove(s.label);
        }
        if (!labelsAssigned.isEmpty()) {
            for (String s : labelsAssigned) {
                System.err.println("Error: " + s + " is assigned multiple cells.");
            }
        }

    }

    /**
     * Verify that the amount of cells matches with the amount of sites.
     */
    private void checkMosaicCellCount() {
        int siteCount = siteMap.getAllSites().size();
        int mosaicCellCount = mosaicCellMap.getAllMosaicCells().size();

        if (siteCount != mosaicCellCount) {
            //not checking whether each region has the correct count.
            System.err.println("Site and mosaicCells do not match up. Misaligned mosaicCells minimally: " + Math.abs(siteCount - mosaicCellCount));
        }
    }

    private void addProvinces() throws IOException {
        if (provinceMapping != null) {
            List<String> readAllLines = Files.readAllLines(provinceMapping);
            for (String line : readAllLines) {
                String[] split = line.split("\t");
                String siteLabel = split[0];
                String province = split[1];

                for (String parent : siteMap.siteMapping.keySet()) {
                    for (Site s : siteMap.siteMapping.get(parent)) {
                        if (s.label.equals(siteLabel)) {
                            s.province = province;
                        }
                    }
                }
            }
        }
    }
}
