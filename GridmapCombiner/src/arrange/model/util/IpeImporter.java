package arrange.model.util;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import arrange.Utils.Utils;
import static arrange.Utils.Utils.EPS;
import arrange.model.graph.PlanarStraightLineGraph;
import arrange.ipe.Document;
import arrange.ipe.IpeUtils;
import arrange.ipe.XMLParser;
import arrange.ipe.attributes.PointAttribute;
import arrange.ipe.objects.IpeObject;
import arrange.ipe.objects.Path;
import arrange.ipe.objects.Path.Operator;
import arrange.ipe.objects.Text;
import arrange.model.graph.PlanarStraightLineGraph.Edge;
import arrange.model.subdivision.Label;
import arrange.model.subdivision.Map;
import arrange.model.subdivision.PlanarSubdivisionAlgorithms;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public final class IpeImporter {

    // Comparator for horizontal sorting
    private static Comparator<Vector2D> horizontalComparator = new Comparator<Vector2D>() {
        @Override
        public int compare(Vector2D p1, Vector2D p2) {
            int comp = Double.compare(p1.getX(), p2.getX());
            if (comp == 0) {
                comp = Double.compare(p1.getY(), p2.getY());
            }
            return comp;
        }
    };
    // Comparator for binary search
    private static Comparator<Vector2D> distanceComparator = new Comparator<Vector2D>() {
        @Override
        public int compare(Vector2D p1, Vector2D p2) {
            if (Vector2D.difference(p1, p2).norm() < EPS) {
                return 0;
            }
            int comp = Double.compare(p1.getX(), p2.getX());
            if (comp == 0) {
                comp = Double.compare(p1.getY(), p2.getY());
            }
            return comp;
        }
    };

    private IpeImporter() {
    }

    private static class EdgeInfo implements Comparable<EdgeInfo> {

        public final int id1;
        public final int id2;
        public final Path path1;
        public Path path2;

        public EdgeInfo(int id1, int id2, Path path1) {
            if (id1 < id2) {
                this.id1 = id1;
                this.id2 = id2;
            } else {
                this.id1 = id2;
                this.id2 = id1;
            }
            this.path1 = path1;
        }

        @Override
        public int compareTo(EdgeInfo ei) {
            int comparison = Integer.compare(this.id1, ei.id1);
            if (comparison == 0) {
                return Integer.compare(this.id2, ei.id2);
            } else {
                return comparison;
            }
        }
    }

    public static Map importMap(String fileName) {
        File file = new File(fileName);
        XMLParser parser;
        try {
            parser = new XMLParser(file);
        } catch (FileNotFoundException ex) {
            return null;
        }
        Document document = parser.getDocument();
        IpeUtils.simplifyDocument(document);
        IpeUtils.setPrecision(document, 4);

        ArrayList<Vector2D> points = new ArrayList<>(256);
        ArrayList<Label> labels = new ArrayList<>();

        // Extract all points from paths to create vertices and text labels
        for (IpeObject object : document.objects()) {
            if (object instanceof Path) {
                Path path = (Path) object;
                ArrayList<Vector2D> boundaryPoints = extractPointsFromPath(path);
                points.addAll(boundaryPoints);
            } else if (object instanceof Text) {
                Text text = (Text) object;
                PointAttribute position = text.getPosition();
                labels.add(new Label(text.getText(), new Vector2D(position.getX(), position.getY())));
            }
        }

        // Sort coordinates and remove duplicates
        Collections.sort(points, horizontalComparator);
        for (int i = 0; i < points.size() - 1; i++) {
            Vector2D p1 = points.get(i);
            int j = i + 1;
            while (j < points.size() && points.get(j).getX() - p1.getX() < EPS) {
                Vector2D p2 = points.get(j);
                if (distanceComparator.compare(p1, p2) == 0) {
                    points.remove(j);
                } else {
                    j++;
                }
            }
        }

        // Create graph and add vertices
        PlanarStraightLineGraph graph = new PlanarStraightLineGraph();
        for (int i = 0; i < points.size(); i++) {
            graph.addVertex(points.get(i));
        }

        // Add edges to graph
        ArrayList<EdgeInfo> edges = new ArrayList<>(points.size());
        for (IpeObject object : document.objects()) {
            if (object instanceof Path) {
                Path path = (Path) object;
                int lastId = -1;
                int startId = -1;
                for (Operator operator : path.operators()) {
                    if (operator instanceof Path.MoveTo) {
                        Path.MoveTo moveTo = (Path.MoveTo) operator;
                        PointAttribute point = moveTo.getPoint();
                        Vector2D last = new Vector2D(point.getX(), point.getY());
                        lastId = getVertexId(points, last, distanceComparator);
                        startId = lastId;
                    } else if (operator instanceof Path.LineTo) {
                        Path.LineTo lineTo = (Path.LineTo) operator;
                        PointAttribute point = lineTo.getPoint();
                        Vector2D next = new Vector2D(point.getX(), point.getY());
                        int nextId = getVertexId(points, next, distanceComparator);
                        edges.add(new EdgeInfo(lastId, nextId, path));
                        lastId = nextId;
                    } else if (operator instanceof Path.ClosePath) {

                        edges.add(new EdgeInfo(lastId, startId, path));
                    }
                }
            }
        }

        // Remove duplicates
        Collections.sort(edges);
        for (int i = 0; i < edges.size() - 1; i++) {
            EdgeInfo ei1 = edges.get(i);
            EdgeInfo ei2 = edges.get(i + 1);
            if (ei1.compareTo(ei2) == 0) {
                ei1.path2 = ei2.path1;
                edges.remove(i + 1);
                i--;
            }
        }

//        removeCrossings(edges,graph);
        // Add edges to graph
        for (EdgeInfo ei : edges) {
            if (ei.id1 == ei.id2) {
                StringBuilder sb = new StringBuilder();
                sb.append("Error: cannot create edge with the same source and target");
                sb.append(System.lineSeparator());
                sb.append("       Problem at ");
                sb.append(graph.getVertex(ei.id1).getPosition());
                System.out.println(sb.toString());
                throw new RuntimeException(sb.toString());
            }
            graph.addEdge(ei.id1, ei.id2);
        }

        // Create map
        Map map = new Map(graph);
       // System.out.println("map created");
        // Assign colors to faces
        for (int i = 0; i < edges.size(); i++) {
            EdgeInfo edgeInfo = edges.get(i);
            Map.Halfedge h = map.getHalfedge(2 * i);
            Map.Face f1 = h.getFace();
            Map.Face f2 = h.getTwin().getFace();

            Path p1 = edgeInfo.path1;
            Path p2 = edgeInfo.path2;
            if (p2 == null) {
                // One of the faces is unbounded
                if (f1.isBounded()) {
                    f1.setColor(p1.getFillColor().getColor());
                } else {
                    f2.setColor(p1.getFillColor().getColor());
                }
            } else {
                if (faceMatchesPath(f1, p1)) {
                    f1.setColor(p1.getFillColor().getColor());
                    f2.setColor(p2.getFillColor().getColor());
                } else {
                    f1.setColor(p2.getFillColor().getColor());
                    f2.setColor(p1.getFillColor().getColor());
                }
            }
        }

        // Assign labels to faces
        for (Label l : labels) {
            Map.Face f = (Map.Face) PlanarSubdivisionAlgorithms.containingFace(map, l.getPosition());
            f.setLabel(l);
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

        // Return the map
        return map;
    }

    private static Integer getVertexId(List<Vector2D> points, Vector2D key, Comparator<Vector2D> comparator) {
        int index = Collections.binarySearch(points, key, comparator);
        if (index < 0) {
            index = -index - 1;
            while (index >= 0 && comparator.compare(key, points.get(index)) != 0) {
                index--;
            }
        }
        if (index >= 0) {
            return index;
        } else {
            return null;
        }
    }

    private static boolean faceMatchesPath(Map.Face face, Path path) {
        ArrayList<Vector2D> pathPoints = extractPointsFromPath(path);
        List<? extends Map.Vertex> faceVertices = face.getBoundaryVertices();
        Vector2D firstPoint = pathPoints.get(0);
        int startVertex = -1;
        for (int i = 0; i < faceVertices.size(); i++) {
            Map.Vertex faceVertex = faceVertices.get(i);
            if (distanceComparator.compare(faceVertex.getPosition(), firstPoint) == 0) {
                startVertex = i;
                break;
            }
        }

        if (startVertex == -1) {
            return false;
        } else {
            Vector2D secondPoint = pathPoints.get(1);
            Map.Vertex next = faceVertices.get((startVertex + 1) % faceVertices.size());
            Map.Vertex previous = faceVertices.get((startVertex + faceVertices.size() - 1) % faceVertices.size());
            if (distanceComparator.compare(next.getPosition(), secondPoint) == 0) {
                for (int i = 2; i < pathPoints.size(); i++) {
                    Vector2D pathPoint = pathPoints.get(i);
                    Map.Vertex v = faceVertices.get((startVertex + i) % faceVertices.size());
                    if (distanceComparator.compare(v.getPosition(), pathPoint) != 0) {
                        return false;
                    }
                }
            } else if (distanceComparator.compare(previous.getPosition(), secondPoint) == 0) {
                for (int i = 2; i < pathPoints.size(); i++) {
                    Vector2D pathPoint = pathPoints.get(i);
                    Map.Vertex v = faceVertices.get((startVertex + faceVertices.size() - i) % faceVertices.size());
                    if (distanceComparator.compare(v.getPosition(), pathPoint) != 0) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private static ArrayList<Vector2D> extractPointsFromPath(Path path) {
        ArrayList<Vector2D> points = new ArrayList<>(path.getNumOperators());
        for (Operator operator : path.operators()) {
            if (operator instanceof Path.MoveTo) {
                Path.MoveTo moveTo = (Path.MoveTo) operator;
                PointAttribute point = moveTo.getPoint();
                points.add(new Vector2D(point.getX(), point.getY()));
            } else if (operator instanceof Path.LineTo) {
                Path.LineTo lineTo = (Path.LineTo) operator;
                PointAttribute point = lineTo.getPoint();
                points.add(new Vector2D(point.getX(), point.getY()));
            }
        }
        return points;
    }
}
