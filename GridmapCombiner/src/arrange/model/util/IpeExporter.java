package arrange.model.util;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import arrange.model.Cartogram.MosaicCartogram;
import arrange.model.Cartogram.MosaicCartogram.CellRegion;
import arrange.model.Cartogram.MosaicCartogram.Cell;
import arrange.model.Cartogram.MosaicCartogram.Coordinate;
import arrange.model.Cartogram.MosaicCartogram.MosaicRegion;
import arrange.model.Network;
import arrange.model.graph.AbstractEdge;
import arrange.model.graph.AbstractVertex;
import arrange.model.graph.GenericGraph;
import arrange.model.graph.PlanarStraightLineGraph;
import arrange.ipe.Document;
import arrange.ipe.IpeUtils;
import arrange.ipe.attributes.PointAttribute;
import arrange.ipe.elements.Layer;
import arrange.ipe.objects.Group;
import arrange.ipe.objects.IpeObject;
import arrange.ipe.objects.Path;
import arrange.ipe.objects.Path.LineTo;
import arrange.ipe.objects.Path.MoveTo;
import arrange.ipe.objects.Path.Operator;
import arrange.ipe.objects.SymbolInstance;
import arrange.ipe.objects.Text;
import arrange.ipe.style.Gradient;
import arrange.ipe.style.Layout;
import arrange.ipe.style.Opacity;
import arrange.ipe.style.StyleSheet;
import arrange.ipe.style.Symbol;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public final class IpeExporter {

    private static final int MARGIN = 20;
    private Document document = new Document();
    private LinkedHashSet<String> layers = new LinkedHashSet<>();
    private Color fillColor = null;
    private Color strokeColor = null;
    private Double strokeWidth = null;

    public IpeExporter() {
        document.addStyleSheet(IpeUtils.basicStyleSheet());
    }

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public Color getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeColor(Color strokeColor) {
        this.strokeColor = strokeColor;
    }

    public Double getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(Double strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public <V extends AbstractVertex & Position2D, E extends AbstractEdge> void append(GenericGraph<V, E> graph, String layer) {
        layers.add(layer);
        setStrokeColor(Color.BLACK);
        for (E edge : graph.edges()) {
            Vector2D source = graph.getSource(edge).getPosition();
            Vector2D target = graph.getTarget(edge).getPosition();
            Path path = new Path();
            setPathAttributes(path);
            path.setLayer(layer);
            path.moveTo(source.getX(), source.getY());
            path.lineTo(target.getX(), target.getY());
            document.addObject(path);
        }
        setFillColor(Color.WHITE);
//        for (V vertex : graph.vertices()) {
//            appendVertex(vertex, layer);
//        }
    }

    @SuppressWarnings("unchecked")
    public <V extends AbstractVertex & Position2D> void appendVertex(V vertex, String layer) {
        layers.add(layer);
        final double HALF_SIDE = 5.0;
        Vector2D position = vertex.getPosition();
        double px = position.getX();
        double py = position.getY();
        Path path = new Path();
        if (vertex instanceof Network.Vertex) {
            Network.Vertex vnet = (Network.Vertex) vertex;
            setFillColor(vnet.getColor());
        }
        setPathAttributes(path);
        setFillColor(null);
        path.setLayer(layer);
        path.moveTo(px - HALF_SIDE, py - HALF_SIDE);
        path.lineTo(px + HALF_SIDE, py - HALF_SIDE);
        path.lineTo(px + HALF_SIDE, py + HALF_SIDE);
        path.lineTo(px - HALF_SIDE, py + HALF_SIDE);
        path.closePath();
        document.addObject(path);
    }

    public void appendGraph(PlanarStraightLineGraph.Edge edge, String layer) {
        layers.add(layer);
        Vector2D source = edge.getSource().getPosition();
        Vector2D target = edge.getTarget().getPosition();
        Path path = new Path();
        setPathAttributes(path);
        path.setLayer(layer);
        path.moveTo(source.getX(), source.getY());
        path.lineTo(target.getX(), target.getY());
        document.addObject(path);
    }

    public void appendEdge(Network.Edge edge, String layer) {
        layers.add(layer);
        Vector2D source = edge.getSource().getPosition();
        Vector2D target = edge.getTarget().getPosition();
        Path path = new Path();
        setPathAttributes(path);
        path.setLayer(layer);
        path.moveTo(source.getX(), source.getY());
        path.lineTo(target.getX(), target.getY());
        document.addObject(path);
    }

    public void appendCells(Iterable<? extends Cell> cells, String layer) {
        layers.add(layer);
        StyleSheet gradientStyleSheet = new StyleSheet("gradients");
        document.addStyleSheet(gradientStyleSheet);
        int index = 0;
        for (Cell cell : cells) {
            // Create the gradient
            Color color = cell.getVertex().getColor();
            Point2D center = cell.getCenter();
            double cx = center.getX();
            double cy = center.getY();
            Gradient.Radial gradient = new Gradient.Radial("gradient" + index++);
            gradient.setExtend(true);
            double cellSide = cell.getContainingGrid().getCellSide();
            gradient.setFirstCircle(cx - 0.7 * cellSide, cy + 0.7 * cellSide, 0);
            gradient.setSecondCircle(cx, cy, cellSide);
            gradient.addStop(0.0, color.brighter());
            gradient.addStop(0.4, color);
            gradient.addStop(1.0, color.darker());
            gradientStyleSheet.addGradient(gradient);
            // Draw the cell
            Path path = new Path();
            path.setLayer(layer);
            path.setFillColor(color);
            path.setGradient(gradient);
            path.setPen(0.04);
            Point2D[] points = cell.getBoundaryPoints();
            for (int i = 0; i < points.length; i++) {
                Point2D p = points[i];
                if (i == 0) {
                    path.moveTo(p.getX(), p.getY());
                } else {
                    path.lineTo(p.getX(), p.getY());
                }
            }
            path.closePath();
            document.addObject(path);
        }
    }

    public void appendCartogram(MosaicCartogram cartogram, String layer) {
        String layerCells;
        if (layer != null) {
            layerCells = layer + "-cells";
        } else {
            layerCells = "cells";
        }
        layers.add(layerCells);
        // Create style sheet

        for (MosaicRegion region : cartogram.regions()) {

//            drawGradientRegion(region, layerCells, cartogram);
            drawSolidRegion(region, layerCells, cartogram);

//            // Draw outline
//            ArrayList<Point2D> outlinePoints = region.computeOutlinePoints();
//            Path outline = new Path();
//            Point2D first = outlinePoints.get(0);
//            outline.moveTo(first.getX(), first.getY());
//            for (int i = 1; i < outlinePoints.size(); i++) {
//                Point2D p = outlinePoints.get(i);
//                outline.lineTo(p.getX(), p.getY());
//            }
//            outline.closePath();
//            outline.setLayer(layerOutlines);
//            outline.setPen(0.3);
//            outline.setName(region.getMapFace().getLabel().getText());
//            document.addObject(outline);
        }
    }

    public void appendTransparentCartogram(MosaicCartogram cartogram, String layer) {
        // Create style sheet
        StyleSheet gradientStyleSheet = new StyleSheet("gradients");
        document.addStyleSheet(gradientStyleSheet);
        Opacity opacity = new Opacity("see-through", 0.3);
        gradientStyleSheet.addOpacity(opacity);
        final Point2D[] points = cartogram.getDefaultCellBoundaryPoints();
        for (MosaicRegion region : cartogram.regions()) {
            // Draw cells and add them to group
            Group group = new Group();
            group.setLayer(layer);
            Color color = region.getVertex().getColor();
            // Create default cell shape
            Path path = new Path();
            path.setFillColor(color);
            path.setPen(0.05);
            path.setStrokeColor(Color.DARK_GRAY);
            path.setOpacity(opacity);
            for (int i = 0; i < points.length; i++) {
                Point2D p = points[i];
                if (i == 0) {
                    path.moveTo(p.getX(), p.getY());
                } else {
                    path.lineTo(p.getX(), p.getY());
                }
            }
            path.closePath();
            Symbol defaultTile = new Symbol("tile" + region.getId());
            defaultTile.setObject(path);
            gradientStyleSheet.addSymbol(defaultTile);
            // Draw cells
            for (Coordinate c : region) {
                Cell cell = cartogram.getCell(c);
                Point2D center = cell.getCenter();
                double cx = center.getX();
                double cy = center.getY();
                SymbolInstance tile = new SymbolInstance(defaultTile);
                tile.setPosition(cx, cy);
                group.addObject(tile);
            }
            document.addObject(group);
            // Draw outline
//            ArrayList<Point2D> outlinePoints = region.computeOutlinePoints();
//            Path outline = new Path();
//            Point2D first = outlinePoints.get(0);
//            outline.moveTo(first.getX(), first.getY());
//            for (int i = 1; i < outlinePoints.size(); i++) {
//                Point2D p = outlinePoints.get(i);
//                outline.lineTo(p.getX(), p.getY());
//            }
//            outline.closePath();
//            outline.setLayer(layerOutlines);
//            outline.setPen(0.3);
//            document.addObject(outline);
        }
    }

    public void appendGuidingShapes(MosaicCartogram cartogram, String layer) {
        for (MosaicRegion region : cartogram.regions()) {
            ArrayList<Point2D> outlinePoints = region.getGuidingShape().computeOutlinePoints();
            Path outline = new Path();
            Point2D first = outlinePoints.get(0);
            outline.moveTo(first.getX(), first.getY());
            for (int i = 1; i < outlinePoints.size(); i++) {
                Point2D p = outlinePoints.get(i);
                outline.lineTo(p.getX(), p.getY());
            }
            outline.closePath();
            outline.setLayer(layer);
            outline.setPen(0.3);
            outline.setStrokeColor(region.getVertex().getColor().darker());
            document.addObject(outline);
        }
    }

    public void appendCellRegion(CellRegion region, String layer) {
        layers.add(layer);
        MosaicCartogram cartogram = region.containingCartogram();
        for (Coordinate c : region) {
            Point2D[] points = cartogram.getCell(c).getBoundaryPoints();
            Path path = new Path();
            path.setLayer(layer);
            path.setPen(0.04);
            path.moveTo(points[0].getX(), points[0].getY());
            for (int i = 1; i < points.length; i++) {
                path.lineTo(points[i].getX(), points[i].getY());
            }
            path.closePath();
            document.addObject(path);
        }
    }

    public void appendPath(Path2D path, String layer) {
        layers.add(layer);
        Path ipePath = new Path();
        setPathAttributes(ipePath);
        ipePath.setLayer(layer);
        PathIterator it = path.getPathIterator(null);
        while (!it.isDone()) {
            double[] values = new double[6];
            int type = it.currentSegment(values);
            switch (type) {
                case PathIterator.SEG_CLOSE:
                    ipePath.closePath();
                    break;
                case PathIterator.SEG_LINETO:
                    ipePath.lineTo(values[0], values[1]);
                    break;
                case PathIterator.SEG_MOVETO:
                    ipePath.moveTo(values[0], values[1]);
                    break;
            }
            it.next();
        }
        document.addObject(ipePath);
    }

    public void exportToFile(String fileName) {
        createLayers();
//        setPageLayout();
        BufferedWriter bw = null;
        try {
            File f = new File(fileName);
            bw = new BufferedWriter(new FileWriter(f));
          //  System.out.println("fileName = " + f.getAbsolutePath());
            bw.write(document.toXMLString());
        } catch (IOException ex) {
            Logger.getLogger(IpeExporter.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                bw.close();
            } catch (IOException ex) {
                Logger.getLogger(IpeExporter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static <V extends AbstractVertex & Position2D, E extends AbstractEdge> void exportGraph(GenericGraph<V, E> graph, String fileName) {
        IpeExporter exporter = new IpeExporter();
        exporter.append(graph, null);
        exporter.exportToFile(fileName);
    }

    public static void exportCells(Iterable<? extends Cell> cells, String fileName) {
        IpeExporter exporter = new IpeExporter();
        exporter.appendCells(cells, null);
        exporter.exportToFile(fileName);
    }

    public static void exportCartogram(MosaicCartogram cartogram, String fileName) {
       // System.out.println("make ipeexporter");
        IpeExporter exporter = new IpeExporter();
       // System.out.println("append cartogram");
        exporter.appendCartogram(cartogram, null);
       // System.out.println("exportToFile: " + fileName);
        exporter.exportToFile(fileName);
    }

    public static void exportTransparentCartogram(MosaicCartogram cartogram, String fileName) {
        IpeExporter exporter = new IpeExporter();
        exporter.appendTransparentCartogram(cartogram, null);
        exporter.exportToFile(fileName);
    }

    public static void exportGuidingShapes(MosaicCartogram cartogram, String fileName) {
        IpeExporter exporter = new IpeExporter();
        exporter.appendGuidingShapes(cartogram, null);
        exporter.exportToFile(fileName);
    }

    private void setPathAttributes(Path path) {
        if (fillColor != null) {
            path.setFillColor(fillColor);
        }
        if (strokeColor != null) {
            path.setStrokeColor(strokeColor);
        }
        if (strokeWidth != null) {
            path.setPen(strokeWidth);
        }
    }

    private void createLayers() {
        for (String name : layers) {
            if (name != null) {
                document.addLayer(new Layer(name));
            }
        }
    }

    private void setPageLayout() {
        BoundingBox bb = new BoundingBox();
        for (IpeObject object : document.objects()) {
            bb.add(estimateObjectBoundingBox(object));
        }
        int width = (int) (bb.getMaxX() - bb.getMinX()) + 10;
        int height = (int) (bb.getMaxY() - bb.getMinY()) + 10;
        StyleSheet styleSheet = new StyleSheet("page-layout");
        document.addStyleSheet(styleSheet);
        Layout layout = new Layout();
        layout.setPaperSize(width, height);
        layout.setFrameSize(1, 1);
        layout.setOrigin((int) (-bb.getMinX() + 5), (int) (-bb.getMinY() + 5));
        styleSheet.setLayout(layout);
    }

    /*
     * Warning: transnformation matrices are NOT considered!
     */
    private BoundingBox estimateObjectBoundingBox(IpeObject object) {
        if (object instanceof Group) {
            return estimateGroupBoundingBox((Group) object);
        } else if (object instanceof Path) {
            return estimatePathBoundingBox((Path) object);
        } else if (object instanceof Text) {
            return estimateTextBoundingBox((Text) object);
        } else if (object instanceof SymbolInstance) {
            return estimateSymbolInstanceBoundingBox((SymbolInstance) object);
        }
        throw new RuntimeException("Unknown IpeObject type");
    }

    private BoundingBox estimateGroupBoundingBox(Group group) {
        BoundingBox bb = new BoundingBox();
        for (IpeObject object : group.objects()) {
            BoundingBox objBB = estimateObjectBoundingBox(object);
            bb.add(objBB);
        }
        return bb;
    }

    private BoundingBox estimatePathBoundingBox(Path path) {
        BoundingBox bb = new BoundingBox();
        for (Operator op : path.operators()) {
            if (op instanceof MoveTo) {
                MoveTo m = (MoveTo) op;
                PointAttribute p = m.getPoint();
                bb.add(p.getX(), p.getY());
            } else if (op instanceof LineTo) {
                LineTo l = (LineTo) op;
                PointAttribute p = l.getPoint();
                bb.add(p.getX(), p.getY());
            }
        }
        return bb;
    }

    private BoundingBox estimateTextBoundingBox(Text text) {
        BoundingBox bb = new BoundingBox();
        PointAttribute p = text.getPosition();
        bb.add(p.getX(), p.getY());
        return bb;
    }

    private BoundingBox estimateSymbolInstanceBoundingBox(SymbolInstance instance) {
        BoundingBox bb = estimateObjectBoundingBox(instance.getSymbolObject());
        PointAttribute position = instance.getPosition();
        bb.translate(position.getX(), position.getY());
        return bb;
    }

    private void drawGradientRegion(MosaicRegion region, String layerCells, MosaicCartogram cartogram) {
        StyleSheet gradientStyleSheet = new StyleSheet("gradients");
        document.addStyleSheet(gradientStyleSheet);

        final Point2D[] points = cartogram.getDefaultCellBoundaryPoints();
        final double cellSide = cartogram.getCellSide();

        // Draw cells and add them to group
        Group group = new Group();
        group.setLayer(layerCells);
        Color color = region.getVertex().getColor();
        // Create the gradient

        String regionName = region.getMapFace().getLabel().getText();

        Gradient.Radial gradient = new Gradient.Radial("gradient " + region.getId() + " " + regionName);
        gradient.setExtend(true);
        gradient.setFirstCircle(-0.7 * cellSide, 0.7 * cellSide, 0);
        gradient.setSecondCircle(0, 0, cellSide);
        gradient.addStop(0.0, color.brighter());
        gradient.addStop(0.4, color);
        gradient.addStop(1.0, color.darker());
        gradientStyleSheet.addGradient(gradient);
        // Create default cell shape
        Path path = new Path();
        path.setFillColor(color);
        path.setGradient(gradient);
        for (int i = 0; i < points.length; i++) {
            Point2D p = points[i];
            if (i == 0) {
                path.moveTo(p.getX(), p.getY());
            } else {
                path.lineTo(p.getX(), p.getY());
            }
        }
        path.closePath();
        Symbol defaultTile = new Symbol("tile" + region.getId());
        defaultTile.setObject(path);
        gradientStyleSheet.addSymbol(defaultTile);
        // Draw cells
        for (Coordinate c : region) {
            Cell cell = cartogram.getCell(c);
            Point2D center = cell.getCenter();
            double cx = center.getX();
            double cy = center.getY();
            SymbolInstance tile = new SymbolInstance(defaultTile);
            tile.setPosition(cx, cy);
            tile.setLabel(regionName);
            group.addObject(tile);
        }
        document.addObject(group);
    }

    private void drawSolidRegion(MosaicRegion region, String layerCells, MosaicCartogram cartogram) {
        final double cellSide = cartogram.getCellSide();

        // Draw cells and add them to group
        Group group = new Group();
        group.setLayer(layerCells);
        Color color = region.getVertex().getColor();
        // Create the gradient
        String regionName = region.getMapFace().getLabel().getText();

        // Draw cells
        for (Coordinate c : region) {
            Cell cell = cartogram.getCell(c);

            Point2D[] bp = cell.getBoundaryPoints();
            Path path = new Path();
            path.setFillColor(color);
            path.moveTo(bp[0].getX(), bp[0].getY());
            for (int i = 1; i < bp.length; i++) {
                path.lineTo(bp[i].getX(), bp[i].getY());
            }
            path.closePath();
            path.setName(regionName);
            group.addObject(path);
        }
        document.addObject(group);
    }
}
