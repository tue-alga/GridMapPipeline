package arrange.gui;

import arrange.algorithms.ForceDirectedLayout;
import java.awt.Color;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import arrange.algorithms.MosaicHeuristic;
import arrange.colouring.Colouring;
import arrange.colouring.RandomNonAdjacentColouring;
import arrange.colouring.colourschemes.ColourSchemes;
import arrange.geom.Point2D;
import arrange.geom.Polygon;
import arrange.model.ComponentManager;
import arrange.model.ComponentManager.Component;
import arrange.model.Cartogram.MosaicCartogram;
import arrange.model.Cartogram.MosaicCartogram.MosaicRegion;
import arrange.model.Network;
import arrange.model.graph.PlanarStraightLineGraph;
import arrange.model.subdivision.Label;
import arrange.model.subdivision.Map;
import arrange.model.subdivision.Map.Face;
import arrange.model.subdivision.PlanarSubdivisionAlgorithms;
import arrange.model.util.ElementList;
import arrange.model.util.IpeExporter;
import arrange.model.util.IpeImporter;
import arrange.model.util.KML.KMLToIpeConverter;
import arrange.model.util.Vector2D;
import arrange.parameter.ParameterManager;
import arrange.parameter.ParameterManager.Application.GridType;
import integration.Partition;
import integration.SiteMap;
import integration.Stopwatch;
import java.util.HashMap;
import nl.tue.geometrycore.geometry.Vector;

public class MainGUI {

    public MainGUI() {
    }

    public void run(boolean readParameters) {
        //System.out.println("3");
        HeuristicRunner runner = new HeuristicRunner(readParameters);
        runner.run();
    }

    public void run(SiteMap sitemap, boolean hex, boolean exact, File outdir) {

        HeuristicRunner runner = new HeuristicRunner(sitemap, hex, exact, outdir);
        runner.run();
    }

    private void importData(String fileName, Map map) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(new File(fileName)));
            ElementList<Boolean> hasData = new ElementList<>(map.numberOfBoundedFaces(), false);
            String line = br.readLine();
            while (line != null) {
                String[] components = line.split("\t");
                if (components.length == 2) {
                    Map.Face f = map.getFace(components[0]);
                    if (f == null) {
//                        System.out.println("Warning: face '" + components[0] + "' not found");
                    } else {
                        if (hasData.get(f)) {
                            throw new RuntimeException("multiple data values found for face " + f.getLabel().getText());
                        }
                        hasData.set(f, true);
                        double weight = Double.parseDouble(components[1]);
                        f.setWeight(weight);
                    }
                }
                line = br.readLine();
            }
            for (Map.Face f : map.boundedFaces()) {
                if (!hasData.get(f)) {
                    throw new RuntimeException("no data found for face " + f.getLabel().getText());
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(IpeExporter.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(IpeExporter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void importColors(String fileName, Map map) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(new File(fileName)));
            ElementList<Boolean> hasColor = new ElementList<>(map.numberOfBoundedFaces(), false);
            String line = br.readLine();
            while (line != null) {
                String[] components = line.split("\\s+");
                if (components.length == 4) {
                    Map.Face f = map.getFace(components[0]);
                    if (f == null) {
                        System.out.println("Warning: face '" + components[0] + "' not found");
                    } else {
                        if (hasColor.get(f)) {
                            throw new RuntimeException("multiple colors found for face " + f.getLabel().getText());
                        }
                        hasColor.set(f, true);
                        int r = Integer.parseInt(components[1]);
                        int g = Integer.parseInt(components[2]);
                        int b = Integer.parseInt(components[3]);
                        f.setColor(new Color(r, g, b));
                    }
                }
                line = br.readLine();
            }
            for (Map.Face f : map.boundedFaces()) {
                if (!hasColor.get(f) && !f.getLabel().getText().equals("*SEA*")) {
                    throw new RuntimeException("no color found for face " + f.getLabel().getText());
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(IpeExporter.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(IpeExporter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void importColorsVec(String fileName, Map map) {
        try (Scanner s = new Scanner(new FileReader(new File(fileName)))) {
            ElementList<Boolean> hasColor = new ElementList<>(map.numberOfBoundedFaces(), false);
            int num = s.nextInt();
            if (num != 3) {
                System.err.println("Only RGB is supported.");
                return;
            }
            while (s.hasNext()) {
                String faceName = s.nextLine().trim();
                if (faceName.isEmpty()) {
                    continue;
                }
                Face f = map.getFace(faceName);
                if (f == null) {
                    System.err.println("Warning: face '" + faceName + "' not found");
                } else {
                    if (hasColor.get(f)) {
                        System.err.println("Warning: multiple colors found for face "
                                + f.getLabel().getText() + ", using last one");
                    }
                    hasColor.set(f, true);
                    float red = s.nextFloat();
                    float green = s.nextFloat();
                    float blue = s.nextFloat();
                    f.setColor(new Color(red, green, blue));
                }
            }
            for (Map.Face f : map.boundedFaces()) {
                if (!hasColor.get(f) && !f.getLabel().getText().equals("*SEA*")) {
                    System.err.println("Warning: no color found for face " + f.getLabel().getText());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class HeuristicRunner implements Runnable {

        private final SiteMap sitemap;
        private final HashMap<Partition, Map.Face> facemap;
        private final HashMap<Map.Face, Partition> facebackmap;
        private final String MAP_FILE_NAME;
        private final String DATA_FILE_NAME;
        private final String MOSAIC_FILE_NAME;
        private final String COLOR_FILE_NAME;
        private final boolean COLOR_MAP;
        private final boolean VORNOI_ENABLED;
        private final String IPE_FILE_NAME;
        private final String STATS_FILE_NAME;
        private final Double RESOLUTION;
        private final Double UNIT_DATA;
        private final GridType TYPE;
        private final boolean FINALIZE_ONLY;
        private final boolean ANIMATION_ONLY;
        private final boolean EXACT_TILES;
        private final boolean EXIT_APP = false;
        private Map map = null;
        private ComponentManager manager = null;
        private final Double unitData;

        public HeuristicRunner(SiteMap sitemap, boolean hex, boolean exact, File outdir) {

            this.sitemap = sitemap;
            facemap = new HashMap();
            facebackmap = new HashMap();

            MAP_FILE_NAME = null;
            DATA_FILE_NAME = null;
            MOSAIC_FILE_NAME = null;
            COLOR_FILE_NAME = null;
            COLOR_MAP = false;
            VORNOI_ENABLED = false;
            IPE_FILE_NAME = (new File(outdir, "cartogram.ipe")).getAbsolutePath();
            STATS_FILE_NAME = null;
            RESOLUTION = null;
            UNIT_DATA = 1.0;
            TYPE = hex ? GridType.HEXAGONAL : GridType.SQUARE;
            FINALIZE_ONLY = false;
            ANIMATION_ONLY = false;

            EXACT_TILES = exact;

            unitData = initialize();

        }

        public HeuristicRunner(boolean readParameters) {
            sitemap = null;
            facemap = null;
            facebackmap = null;

            if (!readParameters) {
                //MAP_FILE_NAME = "usa.ipe";
                MAP_FILE_NAME = "europe-animation.ipe";
                //MAP_FILE_NAME = "italy.ipe";
                //DATA_FILE_NAME = "wfb-table/GDP real growth rate.dat";
                DATA_FILE_NAME = "europe-pop.dat";
                //DATA_FILE_NAME = "usa-starbucks.dat";
                //DATA_FILE_NAME = null;
                //MOSAIC_FILE_NAME = "coordinates-starbucks-finalize.coo";
                //MOSAIC_FILE_NAME = "starbucks-almost-done.coo";
                //MOSAIC_FILE_NAME = "coordinates-eu.coo";
                MOSAIC_FILE_NAME = null;
                //COLOR_FILE_NAME = "worldmapper-colours.col";
                COLOR_FILE_NAME = null;
                COLOR_MAP = false;
                VORNOI_ENABLED = false;
                //IPE_FILE_NAME = "world-population.ipe";
                IPE_FILE_NAME = null;
                //STATS_FILE_NAME = "teste.csv";
                STATS_FILE_NAME = null;
                //RESOLUTION = 20.0;
                RESOLUTION = null;
                //UNIT_DATA = null;
                UNIT_DATA = 2E+6;
                TYPE = GridType.HEXAGONAL;
                FINALIZE_ONLY = false;
                ANIMATION_ONLY = false;
                EXACT_TILES = false;

            } else {
                MAP_FILE_NAME = ParameterManager.Application.getMapFileName();
                DATA_FILE_NAME = ParameterManager.Application.getDataFileName();
                MOSAIC_FILE_NAME = ParameterManager.Application.getMosaicFileName();
                COLOR_FILE_NAME = ParameterManager.Application.getColorFileName();
                COLOR_MAP = ParameterManager.Application.getColorMap();
                VORNOI_ENABLED = ParameterManager.Application.getVornoiEnabled();
                IPE_FILE_NAME = ParameterManager.Application.getIpeFileName();
                STATS_FILE_NAME = ParameterManager.Application.getStatsFileName();
                RESOLUTION = ParameterManager.Application.getMosaicResolution();
                UNIT_DATA = ParameterManager.Application.getUnitData();
                TYPE = ParameterManager.Application.getGridType();
                FINALIZE_ONLY = false;
                ANIMATION_ONLY = false;
                EXACT_TILES = ParameterManager.Application.getExactTiles();
            }
            unitData = initialize();

        }

        public static double SCALING_THRESHOLD = 10;//7 or 10
        public static int MAX_NO_IMPROVE_SCALING = 5000;
        public static int MAX_NO_IMPROVE_FINAL = 5000;
        public static final double SCALING_FACTOR = 1.4142;

        public static void configureGridMaps() {
            SCALING_THRESHOLD = 10;
            MAX_NO_IMPROVE_SCALING = 5000;
            MAX_NO_IMPROVE_FINAL = 5000;
            ForceDirectedLayout.configureGridMap();
        }

        public static void configureMosaicCartograms() {
            SCALING_THRESHOLD = 20;
            MAX_NO_IMPROVE_SCALING = 500;
            MAX_NO_IMPROVE_FINAL = 3000;
            ForceDirectedLayout.configureMosaicCartogram();
        }

        @Override
        public void run() {

            System.out.println("------ DEFORMING INTO CARTOGRAM -------------------");
            System.out.println("  grid = " + TYPE);

            sitemap.gridType = TYPE;

            Stopwatch sw = Stopwatch.get("deform").start();

            int totalTiles = 0;
            for (Map.Face f : map.boundedFaces()) {
                totalTiles += Math.max(1, (int) Math.round(f.getWeight() / unitData));
            }
            double averageTiles = (double) totalTiles / (double) map.numberOfBoundedFaces();
            double currentUnitData = unitData * averageTiles / SCALING_THRESHOLD;
            int scalingIteration = 1;

            while (currentUnitData > SCALING_FACTOR * unitData) {
                if (scalingIteration == 1) {
                    manager = new ComponentManager(map, TYPE, currentUnitData, 5);
                    if (MOSAIC_FILE_NAME != null) {
                        manager.initializeComponentsFromFile(MOSAIC_FILE_NAME);
                    } else {
                        manager.initializeComponentsFromEmbedding();
                    }
                } else {
                    manager.updateUnitData(currentUnitData);
                }

                System.out.println("  Scaling = " + String.format("%.2f", currentUnitData));

                for (Component component : manager.components()) {
                    MosaicCartogram componentCartogram = component.getCartogram();
                    Map componentMap = component.getMap();
                    Network componentWeakDual = component.getWeakDual();
                    MosaicHeuristic heuristic = new MosaicHeuristic(componentMap, componentWeakDual, componentCartogram);
                    componentCartogram = heuristic.execute(MAX_NO_IMPROVE_SCALING, false, false);//no need for exact tiles yet
                    component.setCartogram(componentCartogram);
                }
                currentUnitData /= SCALING_FACTOR;
                scalingIteration++;
            }

            System.out.println("  Finalizing");
            if (manager == null) {
                manager = new ComponentManager(map, TYPE, unitData, 5);
                if (MOSAIC_FILE_NAME != null) {
                    //System.out.println("initializeComponentsFromFile");
                    manager.initializeComponentsFromFile(MOSAIC_FILE_NAME);
                } else {
                    //System.out.println("InitializeComponentsFromEmbedding");
                    manager.initializeComponentsFromEmbedding();
                }
            } else {
                manager.updateUnitData(unitData);
            }
            for (Component component : manager.components()) {
                MosaicCartogram componentCartogram = component.getCartogram();
                Map componentMap = component.getMap();
                Network componentWeakDual = component.getWeakDual();
                MosaicHeuristic heuristic = new MosaicHeuristic(componentMap, componentWeakDual, componentCartogram);
                componentCartogram = heuristic.execute(MAX_NO_IMPROVE_FINAL, true, EXACT_TILES);//finalize it. If specified use the exact amount of tiles

                component.setCartogram(componentCartogram);
            }

            sw.stop();

            System.out.println("------ COMBINING OUTLINES --------------------------");
            sw = Stopwatch.get("combine").start();
            MosaicCartogram mergedCartogram = manager.mergeCartograms();
            sw.stop();

            if (IPE_FILE_NAME == null) {
                IpeExporter.exportCartogram(mergedCartogram, "cartogram.ipe");
            } else {
                IpeExporter.exportCartogram(mergedCartogram, IPE_FILE_NAME);
            }

            for (MosaicRegion mr : mergedCartogram.regions()) {
                facebackmap.get(mr.getMapFace()).mosaic = mr;
            }
        }

        private double symDiff(MosaicRegion r, MosaicCartogram cartogram) {
            // Region stuff
            ArrayList<java.awt.geom.Point2D> outline = r.computeOutlinePoints();
            ArrayList<Point2D> outline2 = new ArrayList<>(outline.size());
            for (java.awt.geom.Point2D p : outline) {
                outline2.add(new Point2D(p.getX(), p.getY()));
            }
            Polygon polyRegion = new Polygon(outline2);
            Vector2D regionCentroid = new Vector2D(polyRegion.getCentroid().getX(), polyRegion.getCentroid().getY());

            // Original face stuff
            Map.Face f = r.getMapFace();
            double faceArea = f.getArea();
            double regionArea = r.size() * cartogram.getCellArea();
            double factor = Math.sqrt(regionArea / faceArea);
            Vector2D newCentroid = Vector2D.product(f.getCentroid(), factor);
            Vector2D translation = Vector2D.difference(regionCentroid, newCentroid);
            Path2D facePath = new Path2D.Double();
            boolean first = true;
            for (Map.Vertex v : f.getBoundaryVertices()) {
                Vector2D position = v.getPosition();
                double vx = position.getX() * factor + translation.getX();
                double vy = position.getY() * factor + translation.getY();
                if (first) {
                    facePath.moveTo(vx, vy);
                    first = false;
                } else {
                    facePath.lineTo(vx, vy);
                }
            }
            facePath.closePath();
            Area a1 = new Area(facePath);
            Area a2 = polyRegion.convertToArea();
            a1.exclusiveOr(a2);
            List<Polygon> xor = Polygon.areaToPolygon(a1);
            double areaXor = 0;
            for (Polygon p : xor) {
                areaXor += p.getSignedArea();
            }
            if (areaXor < 0) {
                areaXor = -areaXor;
            }
//            System.out.println(f.getLabel().getText() + ": " + areaXor / regionArea);
//            IpeExporter exporter = new IpeExporter();
//            exporter.appendVertex(facePath, null);
//            exporter.appendVertex(polyRegion.convertToPath(), null);
//            exporter.exportToFile("symdifftest.ipe");
            return areaXor / regionArea;
        }

        private double initialize() {

            if (RESOLUTION != null && UNIT_DATA != null) {
                throw new RuntimeException("Resolution and Unit Data cannot be set simultaneously");
            }
            if (sitemap != null) {

                ArrayList<Vector> points = new ArrayList<>(256);

                // Extract all points from paths to create vertices and text labels
                for (Partition p : sitemap.partitions()) {
                    for (Vector v : p.vertices()) {
                        boolean newv = true;
                        for (Vector pt : points) {
                            if (pt.isApproximately(v)) {
                                newv = false;
                                break;
                            }
                        }
                        if (newv) {
                            points.add(v.clone());
                        }
                    }
                }

                // Create graph and add vertices
                PlanarStraightLineGraph graph = new PlanarStraightLineGraph();
                for (int i = 0; i < points.size(); i++) {
                    graph.addVertex(new Vector2D(points.get(i).getX(), points.get(i).getY()));
                }

                // Add edges to graph
                for (Partition p : sitemap.partitions()) {
                    PlanarStraightLineGraph.Vertex first = null;
                    PlanarStraightLineGraph.Vertex prev = null;
                    for (Vector v : p.vertices()) {

                        int index = -1;
                        for (int i = 0; i < points.size(); i++) {
                            if (v.isApproximately(points.get(i))) {
                                index = i;
                            }
                        }

                        PlanarStraightLineGraph.Vertex next = (PlanarStraightLineGraph.Vertex) graph.getVertex(index);

                        if (prev != null && graph.getEdge(prev, next) == null) {
                            graph.addEdge(prev, next);
                        }

                        if (first == null) {
                            first = next;
                        }
                        prev = next;
                    }

                    if (graph.getEdge(prev, first) == null) {
                        graph.addEdge(prev, first);
                    }
                }

                // Create map
                map = new Map(graph);

                // Assign labels & colors to faces
                for (Partition p : sitemap.partitions()) {

                    Vector2D lpos = new Vector2D(p.getLabelPoint().getX(), p.getLabelPoint().getY());

                    Map.Face f = (Map.Face) PlanarSubdivisionAlgorithms.containingFace(map, lpos);

                    facemap.put(p, f);
                    facebackmap.put(f, p);

                    Label l = new Label(p.label, lpos);
                    f.setLabel(l);
                    f.setColor(p.color);
                    f.setWeight(p.getWeight());
                    if (l.getText().equals("*SEA*")) {
                        f.setArtificial(true);
                        f.setColor(Color.WHITE);
                    } else {
                        f.setArtificial(false);
                    }
                    if (!f.isBounded()) {
                        System.out.println("Warning: label contained in unbounded face " + l.getPosition());
                    }
                }

                for (Map.Face f : map.boundedFaces()) {
                    if (f.getLabel() == null) {
                        throw new RuntimeException("map region without label " + f.getCentroid());
                    }
                }

                Network weakDual = new Network();
                map.computeWeakDual(weakDual);

            } else {
                String mapFileName = MAP_FILE_NAME;
                if (MAP_FILE_NAME.endsWith(".kml")) {
                    // System.out.println("kmlConverter");
                    KMLToIpeConverter kmlToIpeConverter = new KMLToIpeConverter();
                    kmlToIpeConverter.convertMap(MAP_FILE_NAME, MAP_FILE_NAME.replace(".kml", ".ipe"), VORNOI_ENABLED);
                    mapFileName = MAP_FILE_NAME.replace(".kml", ".ipe");
                }
                //  System.out.println("6");
                map = IpeImporter.importMap(mapFileName);
                //  System.out.println("7");
                if (COLOR_MAP) {
                    Colouring c = new RandomNonAdjacentColouring(ColourSchemes.getOxygenColourScheme());
                    c.assignColours(map);
                }

//            mapPanel.setMap(map);
                if (DATA_FILE_NAME != null) {
                    importData(DATA_FILE_NAME, map);
                } else {
                    for (Map.Face face : map.boundedFaces()) {
                        face.setWeight(face.getArea());
                    }
                }

                if (COLOR_FILE_NAME != null) {
                    if (COLOR_FILE_NAME.endsWith("vec")) {
                        importColorsVec(COLOR_FILE_NAME, map);
                    } else {
                        importColors(COLOR_FILE_NAME, map);
                    }
                }
            }

            double unit;
            if (RESOLUTION == null) {
                unit = UNIT_DATA;
            } else {
                double dataSum = 0;
                for (Map.Face face : map.boundedFaces()) {
                    dataSum += face.getWeight();
                }
                double totalTiles = RESOLUTION * map.numberOfBoundedFaces();
                unit = dataSum / totalTiles;
            }
            return unit;
        }

    }
}
