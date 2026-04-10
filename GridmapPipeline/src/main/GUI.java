package main;

import analyze.Analyzer;
import analyze.QualityMap;
import assign.AssignAlgorithm;
import coloring.ColorScheme;
import coloring.OrthogonalScheme;
import common.Outline;
import common.Partition;
import common.Site;
import common.SiteMap;
import common.Stage;
import io.IPE;
import java.awt.geom.AffineTransform;
import javax.swing.JFileChooser;
import arrange.DeformAlgorithm;
import arrange.MosaicConstants;
import common.gridmath.GridMath.Coordinate;
import assign.AgnosticAlignment;
import assign.AwareAlignment;
import coloring.RadialScheme;
import combine.Combination;
import combine.CombineAlgorithm;
import common.util.Transform;
import common.gridmath.GridGeometry;
import common.gridmath.GridGeometrySpawner;
import common.util.Stopwatch;
import io.IO;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JSpinner;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Rectangle;
import nl.tue.geometrycore.geometryrendering.GeometryPanel;
import nl.tue.geometrycore.geometryrendering.glyphs.PointStyle;
import nl.tue.geometrycore.geometryrendering.styling.Dashing;
import nl.tue.geometrycore.geometryrendering.styling.ExtendedColors;
import nl.tue.geometrycore.geometryrendering.styling.Hashures;
import nl.tue.geometrycore.geometryrendering.styling.SizeMode;
import nl.tue.geometrycore.geometryrendering.styling.TextAnchor;
import nl.tue.geometrycore.gui.GUIUtil;
import nl.tue.geometrycore.gui.debug.DebugRenderer;
import nl.tue.geometrycore.gui.sidepanel.SideTab;
import nl.tue.geometrycore.gui.sidepanel.TabbedSidePanel;
import partition.CutType;
import partition.PartitionAlgorithm;

/**
 *
 * @author Wouter Meulemans
 */
public class GUI {

    public static void main(String[] args) {
        GUI gui = new GUI();
        gui.launch();
    }

    private SiteMap map = new SiteMap();
    private Rectangle bb = null;
    private boolean drawLabels = false;
    private int guideAlpha = 0;
    private List<QualityMap> qualityMaps = new ArrayList();

    private final double margin = 1.05; // 5% margin

    private final JFileChooser choose;
    private final DrawPanel draw;
    private final SidePanel side;
    private boolean launched = false;

    public GUI() {
        choose = new JFileChooser("../Data");
        draw = new DrawPanel();
        side = new SidePanel();
    }

    public void launch() {
        GUIUtil.makeMainFrame("Grid-Maps Pipeline", draw, side);
        launched = true;
        draw.zoomToFit();
    }

    public void loadInput() {
        int result = choose.showOpenDialog(draw);
        if (result == JFileChooser.APPROVE_OPTION) {
            IPE.read(choose.getSelectedFile(), map, Stage.INPUT);
            outlinesChanged();
        }
    }

    public void outlinesChanged() {
        bb = Rectangle.byBoundingBox(map.outlines);
        bb.scale(margin, bb.center());
        if (launched) {
            draw.zoomToFit();
        }
    }

    public void assignColors(ColorScheme scheme) {
        scheme.apply(map);
        if (launched) {
            draw.repaint();
        }
    }

    public void saveInput() {
        int result = choose.showSaveDialog(draw);
        if (result == JFileChooser.APPROVE_OPTION) {
            IPE.write(choose.getSelectedFile(), map, Stage.INPUT);
        }
    }

    public void loadPartition() {
        int result = choose.showOpenDialog(draw);
        if (result == JFileChooser.APPROVE_OPTION) {
            IPE.read(choose.getSelectedFile(), map, Stage.PARTITIONED);
            if (launched) {
                draw.repaint();
            }
        }
    }

    public void noPartition() {
        PartitionAlgorithm.trivialPartition(map);
        if (launched) {
            draw.repaint();
        }
    }

    public void runPartition(CutType cuttype, int productivity, double dilation, boolean noncrossing) {
        PartitionAlgorithm pa = new PartitionAlgorithm(cuttype, dilation, productivity, noncrossing);
        pa.run(map);
        Stopwatch.printAndClear();

        if (launched) {
            draw.repaint();
        }
    }

    public void savePartition() {
        int result = choose.showSaveDialog(draw);
        if (result == JFileChooser.APPROVE_OPTION) {
            IPE.write(choose.getSelectedFile(), map, Stage.PARTITIONED);
        }
    }

    public void loadDeform() {
        int result = choose.showOpenDialog(draw);
        if (result == JFileChooser.APPROVE_OPTION) {
            IPE.read(choose.getSelectedFile(), map, Stage.DEFORMED);
            if (launched) {
                draw.repaint();
            }
        }
    }

    public void runDeform(GridGeometry grid, boolean allowSelfRefine, int debug) {
        MosaicConstants.ALLOW_SELF_REFINEMENT = allowSelfRefine;
        if (debug >= 0) {
            MosaicConstants.DRAW_INTERMEDIATES_DETAIL = debug;
            MosaicConstants.DRAW_INTERMEDIATES = new DebugRenderer();
            MosaicConstants.DRAW_INTERMEDIATES.show();
        }

        DeformAlgorithm da = new DeformAlgorithm(grid);
        da.run(map);
        Stopwatch.printAndClear();
        MosaicConstants.ALLOW_SELF_REFINEMENT = false;

        if (launched) {
            draw.repaint();
        }

        if (debug >= 0) {
            MosaicConstants.DRAW_INTERMEDIATES = null;
            MosaicConstants.DRAW_INTERMEDIATES_DETAIL = -1;
        }
    }

    public void saveDeform() {
        int result = choose.showSaveDialog(draw);
        if (result == JFileChooser.APPROVE_OPTION) {
            IPE.write(choose.getSelectedFile(), map, Stage.DEFORMED);
        }

    }

    public void loadAssign() {
        int result = choose.showOpenDialog(draw);
        if (result == JFileChooser.APPROVE_OPTION) {
            IPE.read(choose.getSelectedFile(), map, Stage.DEFORMED);
            if (launched) {
                draw.repaint();
            }
        }
    }

    public void runAssign(AgnosticAlignment alignment, AwareAlignment refine) {
        AssignAlgorithm aa = new AssignAlgorithm(alignment, refine);
        aa.run(map);
        Stopwatch.printAndClear();

        if (launched) {
            draw.repaint();
        }
    }

    public void runCombine(Combination combine) {
        CombineAlgorithm ca = new CombineAlgorithm(combine);
        ca.run(map);
        Stopwatch.printAndClear();

        if (launched) {
            draw.repaint();
        }
    }

    public void saveAssign() {
        int result = choose.showSaveDialog(draw);
        if (result == JFileChooser.APPROVE_OPTION) {
            IPE.write(choose.getSelectedFile(), map, Stage.ASSIGNED);
        }
    }

    public void loadAll() {
        int result = choose.showOpenDialog(draw);
        if (result == JFileChooser.APPROVE_OPTION) {
            loadAll(choose.getSelectedFile());
        }
    }

    public void loadAll(File f) {
        map = IO.read(f);
        outlinesChanged();
    }

    public void saveAll() {
        int result = choose.showSaveDialog(draw);
        if (result == JFileChooser.APPROVE_OPTION) {
            saveAll(choose.getSelectedFile());
        }
    }

    public void saveAll(File f) {
        IO.write(f, map);
    }

    private class DrawPanel extends GeometryPanel {

        @Override
        protected void drawScene() {
            if (map.stage <= Stage.EMPTY) {
                return;
            }

            setSizeMode(SizeMode.VIEW);

            // draw all frames
            Vector captionLoc = Vector.add(bb.topSide().getPointAt(0.5), Vector.up(convertViewToWorld(4)));
            Vector labelOffset = Vector.right(convertViewToWorld(4));

            setStroke(ExtendedColors.darkGray, 1, Dashing.SOLID);
            setFill(null, Hashures.SOLID);
            draw(bb);
            for (int i = 1; i <= 3; i++) {
                draw(Rectangle.byCornerAndSize(Vector.add(bb.leftBottom(), Vector.right(i * bb.width())), bb.width(), bb.height()));
            }
            setTextStyle(TextAnchor.BOTTOM, 12);
            draw(captionLoc, "Input");
            draw(Vector.add(captionLoc, Vector.right(bb.width())), "Partitioned");
            draw(Vector.add(captionLoc, Vector.right(2 * bb.width())), "Deformed");
            draw(Vector.add(captionLoc, Vector.right(3 * bb.width())), "Assigned");

            setTextStyle(TextAnchor.TOP, 12);
            for (int i = 0; i < qualityMaps.size(); i++) {
                draw(Rectangle.byCornerAndSize(Vector.add(bb.leftBottom(), Vector.right(i * bb.width())), bb.width(), -bb.height()));
                draw(Vector.addSeq(captionLoc, Vector.right(i * bb.width()), Vector.down(2 * bb.height() + convertViewToWorld(4))), qualityMaps.get(i).toString());
            }

            // draw input
            if (map.stage >= Stage.INPUT) {

                setStroke(ExtendedColors.black, 1, Dashing.SOLID);
                for (Outline p : map.outlines) {
                    setFill(p.color, Hashures.SOLID);
                    draw(p);
                }

                setPointStyle(PointStyle.CIRCLE_SOLID, 4);
                setTextStyle(TextAnchor.LEFT, 9);
                for (Site s : map.sites()) {
                    setStroke(s.getColor() == null ? ExtendedColors.black : s.getColor(), 1, Dashing.SOLID);
                    draw(s);
                    if (drawLabels && s.getLabel() != null) {
                        draw(Vector.add(s, labelOffset), s.getLabel());
                    }
                }
            }

            // draw partition
            if (map.stage >= Stage.PARTITIONED) {
                pushMatrix(AffineTransform.getTranslateInstance(bb.width(), 0));

                setPointStyle(PointStyle.CIRCLE_SOLID, 4);
                for (Partition p : map.partitions()) {
                    setStroke(ExtendedColors.black, 1, Dashing.SOLID);
                    setFill(p.color, Hashures.SOLID);
                    draw(p);

                    for (Site s : p.sites) {
                        setStroke(s.getColor() == null ? ExtendedColors.black : s.getColor(), 1, Dashing.SOLID);
                        draw(s);
                    }
                }

                popMatrix();
            }

            // draw cartogram
            Transform t = null;
            if (map.stage >= Stage.DEFORMED) {
                pushMatrix(AffineTransform.getTranslateInstance(2 * bb.width(), 0));

                Rectangle cellbox = new Rectangle();
                for (Partition p : map.partitions()) {
                    for (Coordinate c : p.cells) {
                        cellbox.includeGeometry(c.getBoundary());
                    }
                }
                cellbox.scale(margin, cellbox.center()); // 1% margin
                t = Transform.fitToBox(cellbox, bb);

                setStroke(ExtendedColors.black, 1, Dashing.SOLID);
                for (Partition p : map.partitions()) {
                    setFill(p.color, Hashures.SOLID);
                    for (Coordinate c : p.cells) {
                        draw(t.apply(c.getBoundary()));
                    }
                }

                if (guideAlpha > 0) {
                    setStroke(ExtendedColors.black, 1, Dashing.SOLID);
                    setAlpha(guideAlpha / 100.0);
                    for (Partition p : map.partitions()) {
                        if (p.guide == null) {
                            // singleton
                            continue;
                        }
                        setFill(p.color.darker(), Hashures.SOLID);
                        for (Coordinate c : p.guide) {
                            draw(t.apply(c.getBoundary()));
                        }
                    }
                    setAlpha(1);
                }

                popMatrix();
            }

            // draw grid map
            if (map.stage >= Stage.ASSIGNED) {
                pushMatrix(AffineTransform.getTranslateInstance(3 * bb.width(), 0));

                setTextStyle(TextAnchor.CENTER, 9);
                for (Site s : map.sites()) {
                    if (s.getCell() != null) {
                        Color c = s.getColor();
                        setStroke(ExtendedColors.black, 1, Dashing.SOLID);
                        setFill(c, Hashures.SOLID);
                        draw(t.apply(s.getCell().getBoundary()));

                        if (drawLabels && s.getLabel() != null) {

                            float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
                            if (hsb[2] > 0.5) {
                                setStroke(ExtendedColors.black, 1, Dashing.SOLID);
                            } else {
                                setStroke(ExtendedColors.lightGray, 1, Dashing.SOLID);
                            }
                            draw(Vector.add(t.apply(s.getCell().toVector()),
                                    Vector.right(3 * bb.width())
                            ), s.getLabel());
                        }
                    } else {
                        System.err.println("Warning: site not assigned to a cell?");
                    }
                }

                popMatrix();
            }

            // draw the results of the most recent quality analyzer
            for (int i = 0; i < qualityMaps.size(); i++) {
                pushMatrix(AffineTransform.getTranslateInstance(i * bb.width(), -bb.height()));
                qualityMaps.get(i).render(this, map, t);
                popMatrix();
            }
        }

        @Override
        public Rectangle getBoundingRectangle() {
            if (bb == null) {
                return null;
            }
            if (qualityMaps.isEmpty()) {
                return Rectangle.byCornerAndSize(bb.leftBottom(),
                        bb.width() * 4,
                        bb.height());
            } else {
                return Rectangle.byCornerAndSize(Vector.add(bb.leftBottom(), Vector.down(bb.height())),
                        bb.width() * Math.max(4, qualityMaps.size()),
                        2 * bb.height());
            }
        }

        @Override
        protected void mousePress(Vector loc, int button, boolean ctrl, boolean shift, boolean alt) {
            if (alt && button == MouseEvent.BUTTON1) {
                Rectangle abb = bb.clone();
                abb.scale(1 / margin);
                double fx = (loc.getX() - abb.getLeft()) / abb.width();
                double fy = (loc.getY() - abb.getBottom()) / abb.height();

                System.out.println("Clicked relative: " + fx + " " + fy);

                if (shift) {
                    side.xspin.setValue(fx);
                    side.yspin.setValue(fy);
                }
            }
        }

        @Override
        protected void keyPress(int keycode, boolean ctrl, boolean shift, boolean alt) {

        }

    }

    private class SidePanel extends TabbedSidePanel {

        JSpinner xspin;
        JSpinner yspin;

        public SidePanel() {
            general();
            input();
            partition();
            deform();
            assign();
            combine();
            analyze();
        }

        private void input() {
            SideTab tab = addTab("Input");

            tab.addButton("Load input", (e) -> {
                loadInput();
            });

            tab.addButton("Assign ortho colors (auto)", (e) -> {
                assignColors(new OrthogonalScheme(bb.width() > bb.height()));
            });

            double[] xya = new double[]{0.54973821989, 0.67717717717, 90}; // defaults for NL municipality map
            tab.addLabel("Radial X-Y-angle");

            xspin = tab.addDoubleSpinner(xya[0], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1, (e, v) -> xya[0] = v);
            yspin = tab.addDoubleSpinner(xya[1], Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1, (e, v) -> xya[1] = v);
            tab.addDoubleSpinner(xya[2], 0, 360, 1, (e, v) -> xya[2] = v);

            tab.addButton("Assign radial colors", (e) -> {
                assignColors(new RadialScheme(new Vector(xya[0], xya[1]), xya[2]));
            });

            tab.addButton("Save input", (e) -> {
                saveInput();
            });
        }

        private void general() {
            SideTab tab = addTab("General");

            tab.addButton("Load all", (e) -> loadAll());

            tab.addCheckbox("Draw site labels", drawLabels, (e, v) -> {
                drawLabels = v;
                draw.repaint();
            });

            tab.addLabel("Guide opacity");
            tab.addIntegerSlider(guideAlpha, 0, 100, (e, v) -> {
                guideAlpha = v;
                draw.repaint();
            });

            tab.addButton("Save all", (e) -> saveAll());
        }

        private void partition() {
            SideTab tab = addTab("Partition");

            int[] productivity = new int[]{10};
            tab.makeSplit(4, 2);
            tab.addLabel("Productivity:");
            tab.addIntegerSpinner(productivity[0], 1, Integer.MAX_VALUE, 1, (e, v) -> productivity[0] = v);

            double[] dilation = new double[]{3};
            tab.makeSplit(4, 2);
            tab.addLabel("Dilation:");
            tab.addDoubleSpinner(dilation[0], 1, Double.MAX_VALUE, 1, (e, v) -> dilation[0] = v);

            CutType[] cuttype = new CutType[]{CutType.COMBINED};
            tab.makeSplit(4, 2);
            tab.addLabel("Cut types:");
            tab.addComboBox(CutType.values(), cuttype[0], (e, v) -> cuttype[0] = v);

            boolean[] noncrossing = new boolean[]{true};
            tab.addCheckbox("NonCrossingMode", noncrossing[0], (e, v) -> noncrossing[0] = v);

            tab.addButton("Execute", (e) -> runPartition(cuttype[0], productivity[0], dilation[0], noncrossing[0]));

            tab.addButton("Do not partition", (e) -> noPartition());

            tab.addSeparator(4);

            tab.addButton("Load partition", (e) -> {
                loadPartition();
            });

            tab.addButton("Save partition", (e) -> {
                savePartition();
            });
        }

        private void deform() {
            SideTab tab = addTab("Deform");

            GridGeometrySpawner[] geom = new GridGeometrySpawner[]{GridGeometrySpawner.grids[0]};
            tab.makeSplit(4, 2);
            tab.addLabel("Grid:");
            tab.addComboBox(GridGeometrySpawner.grids, geom[0], (e, v) -> geom[0] = v);

            tab.makeSplit(4, 3);
            tab.addLabel("Shear");
            double[] h = new double[]{0, 0};
            tab.addDoubleSpinner(h[0], -Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.5, (e, v) -> h[0] = v);
            tab.addDoubleSpinner(h[1], -Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.5, (e, v) -> h[1] = v);

            tab.makeSplit(4, 3);
            tab.addLabel("Scale");
            double[] s = new double[]{1, 1};
            tab.addDoubleSpinner(s[0], 1, Double.POSITIVE_INFINITY, 0.5, (e, v) -> s[0] = v);
            tab.addDoubleSpinner(s[1], 1, Double.POSITIVE_INFINITY, 0.5, (e, v) -> s[1] = v);

            tab.makeSplit(4, 2);
            tab.addLabel("Rotate");
            double[] r = new double[]{0};
            tab.addDoubleSpinner(r[0], 0, 360, 0.5, (e, v) -> r[0] = v);

            JCheckBox allowRefine = tab.addCheckbox("Allow refinement", false, null);

            int[] debug = {-1};
            tab.makeSplit(4, 2);
            tab.addLabel("Debug level:");
            tab.addIntegerSpinner(debug[0], -1, 3, 1, (e, v) -> debug[0] = v);

            tab.addButton("Execute", (e) -> {
                GridGeometry grid = geom[0].spawn();

                if (h[0] != 0 || h[1] != 0) {
                    grid.shear(h[0], h[1]);
                }
                if (s[0] > 1 || s[1] > 1) {
                    grid.scale(s[0], s[1]);
                }
                if (r[0] != 0) {
                    grid.rotate(r[0]);
                }
                runDeform(grid, allowRefine.isSelected(), debug[0]);
            });

            tab.addSeparator(4);

            tab.addButton("Load cartogram", (e) -> {
                loadDeform();
            });

            tab.addButton("Save cartogram", (e) -> {
                saveDeform();
            });
        }

        private void assign() {
            SideTab tab = addTab("Assign");

            AgnosticAlignment[] alignment = {AgnosticAlignment.methods[2]};
            tab.makeSplit(4, 2);
            tab.addLabel("Alignment:");
            tab.addComboBox(AgnosticAlignment.methods, alignment[0], (e, v) -> alignment[0] = v);

            boolean[] enableRefine = {false};
            tab.addCheckbox("Refine", enableRefine[0], (e, v) -> enableRefine[0] = v);

            AwareAlignment[] refine = {AwareAlignment.methods[0]};
            tab.makeSplit(4, 2);
            tab.addLabel("Refine:");
            tab.addComboBox(AwareAlignment.methods, refine[0], (e, v) -> refine[0] = v);

            tab.addButton("Execute", (e) -> runAssign(alignment[0], enableRefine[0] ? refine[0] : null));

            tab.addSeparator(4);

            tab.addButton("Load assignment", (e) -> {
                loadAssign();
            });

            tab.addButton("Save assignment", (e) -> {
                saveAssign();
            });
        }

        private void combine() {
            SideTab tab = addTab("Combine");

            Combination[] combine = {Combination.methods[2]};
            tab.makeSplit(4, 2);
            tab.addLabel("Combination:");
            tab.addComboBox(Combination.methods, combine[0], (e, v) -> combine[0] = v);

            tab.addButton("Execute", (e) -> runCombine(combine[0]));
        }

        private void analyze() {
            SideTab tab = addTab("Analyze");

            tab.addButton("Clear results", (e) -> {
                qualityMaps.clear();
                draw.zoomToFit();
            });

            for (Analyzer an : Analyzer.methods) {
                tab.addButton(an.toString(), (e) -> {
                    QualityMap qmap = an.run(map);
                    if (qmap != null) {
                        qualityMaps.add(qmap);
                        draw.zoomToFit();
                    }
                });
            }

            tab.addSpace(2);

            tab.addButton("RUN ALL", (e) -> {
                for (Analyzer an : Analyzer.methods) {
                    QualityMap qmap = an.run(map);
                    if (qmap != null) {
                        qualityMaps.add(qmap);
                    }
                }
                draw.zoomToFit();
            });

        }
    }
}
