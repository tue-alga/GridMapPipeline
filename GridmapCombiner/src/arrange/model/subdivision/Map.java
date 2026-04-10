package arrange.model.subdivision;

import java.awt.Color;
import java.util.List;
import arrange.model.graph.AbstractEdge;
import arrange.model.graph.AbstractVertex;
import arrange.model.graph.CrossingFinder;
import arrange.model.graph.GenericGraph;
import arrange.model.graph.PlanarStraightLineGraph;
import arrange.model.util.Position2D;
import arrange.model.util.Vector2D;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class Map extends PlanarSubdivision {

    public Map(PlanarStraightLineGraph graph) {
        super(graph);
        initialize();
    }

    protected Map() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterable<? extends Vertex> vertices() {
        return (Iterable<? extends Vertex>) super.vertices();
    }

    @Override
    public Vertex getVertex(int id) {
        return (Vertex) super.getVertex(id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterable<? extends Halfedge> halfedges() {
        return (Iterable<? extends Halfedge>) super.halfedges();
    }

    @Override
    public Halfedge getHalfedge(int id) {
        return (Halfedge) super.getHalfedge(id);
    }

    @Override
    public Halfedge getHalfedge(PlanarSubdivision.Vertex source, PlanarSubdivision.Vertex target) {
        return (Halfedge) super.getHalfedge(source, target);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterable<? extends Face> faces() {
        return (Iterable<? extends Face>) super.faces();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterable<? extends Face> boundedFaces() {
        return (Iterable<? extends Face>) super.boundedFaces();
    }

    @Override
    public Face getFace(int id) {
        return (Face) super.getFace(id);
    }

    public Face getFace(String label) {
        for (Face f : faces()) {
            Label l = f.getLabel();
            if (l != null && l.getText().equals(label)) {
                return f;
            }
        }
        return null;
    }

    public Vector2D getAverageCentroid() {
        Vector2D centroid = new Vector2D(0, 0);
        double totalArea = 0;
        for (Face f : faces()) {
            if (f.getCentroid() != null) {
                Vector2D fCentroid = new Vector2D(f.getCentroid().getX(), f.getCentroid().getY());
                Vector2D faceArea = fCentroid.multiply(f.area);
                centroid = centroid.add(faceArea);
                totalArea += f.area;
            }
        }
        centroid = centroid.multiply(1 / totalArea);
        return centroid;
    }

    @Override
    public Face getUnboundedFace() {
        return (Face) super.getUnboundedFace();
    }

    @Override
    public <V extends AbstractVertex & Position2D, E extends AbstractEdge> void computeWeakDual(GenericGraph<V, E> graph) {
        super.computeWeakDual(graph);
    }

    @Override
    public Map restrictToFaces(List<? extends PlanarSubdivision.Face> restrictedFaces) {
        Map restricted = new Map();
        restrictToFaces(restrictedFaces, restricted);
        for (int i = 0; i < restrictedFaces.size(); i++) {
            Face oldFace = (Face) restrictedFaces.get(i);
            Face newFace = restricted.getFace(i);
            newFace.setArea(oldFace.getArea());
            newFace.setCentroid(new Vector2D(oldFace.getCentroid()));
            newFace.setColor(oldFace.getColor());
            newFace.setLabel(new Label(oldFace.getLabel()));
            newFace.setWeight(oldFace.getWeight());
        }
        return restricted;
    }

    @Override
    protected Vertex createVertex() {
        return new Vertex();
    }

    @Override
    protected Halfedge createHalfedge() {
        return new Halfedge();
    }

    private void initialize() {
        // Set face areas and barycenters
        for (Face face : boundedFaces()) {
            double area = 0;
            double cx = 0;
            double cy = 0;
            List<? extends Vertex> vertices = face.getBoundaryVertices();
            int size = vertices.size();
            for (int i = 0; i < size; i++) {
                Vector2D p = vertices.get(i).getPosition();
                Vector2D q = vertices.get((i + 1) % size).getPosition();
                double increment = p.getX() * q.getY() - q.getX() * p.getY();
                area += increment;
                cx += increment * (p.getX() + q.getX());
                cy += increment * (p.getY() + q.getY());
            }
            area /= 2;
            cx /= 6 * area;
            cy /= 6 * area;
            face.setArea(area);
            face.setCentroid(new Vector2D(cx, cy));
        }
        getUnboundedFace().setArea(Double.POSITIVE_INFINITY);
    }

    @Override
    protected Face createFace() {
        return new Face();
    }

    public class Vertex extends PlanarSubdivision.Vertex {

        protected Vertex() {
            super();
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<? extends Halfedge> getOutgoingHalfedges() {
            return (List<? extends Halfedge>) super.getOutgoingHalfedges();
        }
    }

    public class Halfedge extends PlanarSubdivision.Halfedge {

        protected Halfedge() {
            super();
        }

        @Override
        public Vertex getSource() {
            return (Vertex) super.getSource();
        }

        @Override
        public Vertex getTarget() {
            return (Vertex) super.getTarget();
        }

        @Override
        public Halfedge getTwin() {
            return (Halfedge) super.getTwin();
        }

        @Override
        public Halfedge getNext() {
            return (Halfedge) super.getNext();
        }

        @Override
        public Halfedge getPrevious() {
            return (Halfedge) super.getPrevious();
        }

        @Override
        public Face getFace() {
            return (Face) super.getFace();
        }
    }

    public class Face extends PlanarSubdivision.Face {

        private double area;
        private Vector2D centroid;
        private Label label;
        private Color color;
        private double weight;
        private boolean artificial;

        protected Face() {
            super();
        }

        public double getArea() {
            return area;
        }

        public Vector2D getCentroid() {
            return centroid;
        }

        public Label getLabel() {
            return label;
        }

        public void setLabel(Label label) {
            this.label = label;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }

        public boolean isArtificial() {
            return artificial;
        }

        public void setArtificial(boolean artificial) {
            this.artificial = artificial;
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<? extends Vertex> getBoundaryVertices() {
            return (List<? extends Vertex>) super.getBoundaryVertices();
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<? extends Halfedge> getBoundaryHalfedges() {
            return (List<? extends Halfedge>) super.getBoundaryHalfedges();
        }

        @Override
        public String toString() {
            return label.getText();
        }

        private void setArea(double area) {
            this.area = area;
        }

        private void setCentroid(Vector2D centroid) {
            this.centroid = centroid;
        }
    }
}
