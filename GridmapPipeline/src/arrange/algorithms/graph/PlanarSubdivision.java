package arrange.algorithms.graph;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import arrange.util.Utils;
import arrange.util.ElementList;
import arrange.util.Identifier;
import arrange.util.Position2D;
import nl.tue.geometrycore.geometry.Vector;

/**
 * Planar subdivision implemented using a DCEL data structure. The algorithm is
 * not prepared to work with holes. Corresponding vertices of the original graph
 * and the subdivision have the same id. The halfedges corresponding to an edge
 * with id eId have ids 2*eId and 2*eId+1.
 *
 * @author Rafael Cano
 */
public class PlanarSubdivision {

    private final ArrayList<Vertex> vertices;
    private final ArrayList<Halfedge> halfedges;
    private final ArrayList<Face> faces;

    public PlanarSubdivision(Network graph) {

        assert !graph.hasCrossings() : "cannot embed graph, crossings detected";

        vertices = new ArrayList<>(graph.numberOfVertices());
        halfedges = new ArrayList<>(2 * graph.numberOfEdges());
        faces = new ArrayList<>(3 * graph.numberOfVertices());
        initialize(graph);
    }

    protected PlanarSubdivision() {
        vertices = new ArrayList<>();
        halfedges = new ArrayList<>();
        faces = new ArrayList<>();
    }

    public Iterable<Vertex> vertices() {
        return vertices;
    }

    public Vertex getVertex(int id) {
        return vertices.get(id);
    }

    public Iterable<Halfedge> halfedges() {
        return halfedges;
    }

    public Halfedge getHalfedge(int id) {
        return halfedges.get(id);
    }

    public Halfedge getHalfedge(Vertex source, Vertex target) {
        for (Halfedge h : source.getOutgoingHalfedges()) {
            if (h.getTarget() == target) {
                return h;
            }
        }
        return null;
    }

    public Iterable<Face> faces() {
        return faces;
    }

    public Iterable<Face> boundedFaces() {
        if (!faces.isEmpty()) {
            return faces.subList(0, faces.size() - 1);
        } else {
            return faces;
        }
    }

    public boolean isTriangulation() {
        for (Face f : boundedFaces()) {
            if (f.getBoundaryVertices().size() != 3) {
                return false;
            }
        }
        return true;
    }

    public Face getFace(int id) {
        return faces.get(id);
    }

    public Face getUnboundedFace() {
        if (!faces.isEmpty()) {
            return faces.get(faces.size() - 1);
        } else {
            return null;
        }
    }

    public final int numberOfVertices() {
        return vertices.size();
    }

    public final int numberOfHalfedges() {
        return halfedges.size();
    }

    public final int numberOfFaces() {
        return faces.size();
    }

    public final int numberOfBoundedFaces() {
        return (faces.isEmpty() ? 0 : faces.size() - 1);
    }

    private void initialize(Network graph) {
        // Create vertices
        for (int i = 0; i < graph.numberOfVertices(); i++) {
            Network.Vertex oldVertex = graph.getVertex(i);
            Vertex newVertex = new Vertex(vertices.size());
            newVertex.getPosition().set(oldVertex.getPosition());
            vertices.add(newVertex);
        }

        // Create halfedges
        for (int i = 0; i < graph.numberOfEdges(); i++) {
            Network.Edge oldEdge = graph.getEdge(i);
            Vertex sourceH1 = vertices.get(oldEdge.getSource().getId());
            Vertex sourceH2 = vertices.get(oldEdge.getTarget().getId());
            Halfedge h1 = new Halfedge(halfedges.size());
            h1.setSource(sourceH1);
            sourceH1.addOutgoingHalfegde(h1);
            halfedges.add(h1);

            Halfedge h2 = new Halfedge(halfedges.size());
            h2.setSource(sourceH2);
            sourceH2.addOutgoingHalfegde(h2);
            halfedges.add(h2);

            h1.setTwin(h2);
            h2.setTwin(h1);
        }

        // Sort outgoing halfedges in clockwise order around each vertex and set
        // set the next and previous fields of each halfedge
        for (Vertex v : vertices) {
            v.sortOutgoingHalfedges();
            List<Halfedge> outgoingHalfedges = v.outgoingHalfedges;
            int total = outgoingHalfedges.size();
            for (int i = 0; i < total; i++) {
                Halfedge current = outgoingHalfedges.get(i);
                Halfedge twin = current.getTwin();
                Halfedge next = outgoingHalfedges.get((i + 1) % total);
                twin.setNext(next);
                next.setPrevious(twin);
            }
        }

        // Create faces
        ElementList<Boolean> done = new ElementList<>(halfedges.size(), false);
        Face unboundedFace = null;
        for (Halfedge h : halfedges) {
            if (!done.get(h)) {
                ArrayList<Vertex> boundaryVertices = new ArrayList<>();
                ArrayList<Halfedge> boundaryHalfedges = new ArrayList<>();
                Halfedge current = h;
                Vector leftmostPosition = h.getSource().getPosition();
                int leftmostIndex = 0;
                int i = 0;
                do {
                    boundaryVertices.add(current.getSource());
                    boundaryHalfedges.add(current);
                    Vector currentPosition = current.getSource().getPosition();
                    if (currentPosition.getX() < leftmostPosition.getX()) {
                        leftmostPosition = currentPosition;
                        leftmostIndex = i;
                    } else if (currentPosition.getX() == leftmostPosition.getX()) {
                        if (currentPosition.getY() < leftmostPosition.getY()) {
                            leftmostPosition = currentPosition;
                            leftmostIndex = i;
                        }
                    }
                    i++;
                    done.set(current, true);
                    current = current.getNext();
                } while (current != h);
                Halfedge h2 = boundaryHalfedges.get(leftmostIndex);
                Vector previous = h2.getPrevious().getSource().getPosition();
                Vector next = h2.getTarget().getPosition();
                if (Utils.triangleOrientation(previous, leftmostPosition, next) == 1) {
                    Face f = new Face(faces.size());
                    f.setBounded(true);
                    f.setBoundaryHalfedges(boundaryHalfedges);
                    f.setBoundaryVertices(boundaryVertices);
                    for (Halfedge bh : boundaryHalfedges) {
                        bh.setFace(f);
                    }
                    faces.add(f);
                } else {
                    if (unboundedFace == null) {
                        unboundedFace = new Face(-1);
                        unboundedFace.setBounded(false);
                    }
                    unboundedFace.addHole(boundaryVertices);
                    for (Halfedge bh : boundaryHalfedges) {
                        bh.setFace(unboundedFace);
                    }
                }
            }
        }

        // Add the unbounded face last, so its id is larger than all the others
        // (this is helpful when constructing the weak dual)
        if (unboundedFace != null) {
            unboundedFace.id = faces.size();
            faces.add(unboundedFace);
        }

    }

    public class Vertex implements Identifier, Position2D {

        private final int id;
        private Vector position = new Vector(0, 0);
        private final ArrayList<Halfedge> outgoingHalfedges = new ArrayList<>();

        protected Vertex(int id) {
            this.id = id;
        }

        @Override
        public final int getId() {
            return id;
        }

        @Override
        public final Vector getPosition() {
            return position;
        }

        public final void setPosition(Vector position) {
            this.position = position;
        }

        public List<Halfedge> getOutgoingHalfedges() {
            return Collections.unmodifiableList(outgoingHalfedges);
        }

        private void addOutgoingHalfegde(Halfedge h) {
            outgoingHalfedges.add(h);
        }

        private void sortOutgoingHalfedges() {
            Collections.sort(outgoingHalfedges, (Halfedge h1, Halfedge h2) -> {
                Vector center = h1.getSource().getPosition();
                Vector v1 = h1.getTarget().getPosition();
                Vector v2 = h2.getTarget().getPosition();
                return Utils.counterclockwiseCompare(center, v2, v1);
            });
        }
    }

    public class Halfedge implements Identifier {

        private final int id;
        private Vertex source = null;
        private Halfedge twin = null;
        private Halfedge next = null;
        private Halfedge previous = null;
        private Face face = null;

        protected Halfedge(int id) {
            this.id = id;
        }

        @Override
        public final int getId() {
            return id;
        }

        public Vertex getSource() {
            return source;
        }

        public Vertex getTarget() {
            return getTwin().getSource();
        }

        public Halfedge getTwin() {
            return twin;
        }

        public Halfedge getNext() {
            return next;
        }

        public Halfedge getPrevious() {
            return previous;
        }

        public Face getFace() {
            return face;
        }

        private void setSource(Vertex source) {
            this.source = source;
        }

        private void setTwin(Halfedge twin) {
            this.twin = twin;
        }

        private void setNext(Halfedge next) {
            this.next = next;
        }

        private void setPrevious(Halfedge previous) {
            this.previous = previous;
        }

        private void setFace(Face face) {
            this.face = face;
        }
    }

    public class Face implements Identifier {

        private int id;
        private boolean bounded = true;
        private ArrayList<Vertex> boundaryVertices = new ArrayList<>();
        private ArrayList<Halfedge> boundaryHalfedges = new ArrayList<>();
        private final ArrayList<List<Vertex>> holes = new ArrayList<>();

        protected Face(int id) {
            this.id = id;
        }

        @Override
        public final int getId() {
            return id;
        }

        public final int numberOfSides() {
            return boundaryVertices.size();
        }

        public final boolean isBounded() {
            return bounded;
        }

        public List<Vertex> getBoundaryVertices() {
            return boundaryVertices;
        }

        public List<Halfedge> getBoundaryHalfedges() {
            return boundaryHalfedges;
        }

        public int numberOfHoles() {
            return holes.size();
        }

        public List<List<Vertex>> getHoles() {
            return holes;
        }

        public Path2D.Double toPath2D() {
            Path2D.Double path = new Path2D.Double();
            Vector first = boundaryVertices.get(0).getPosition();
            path.moveTo(first.getX(), first.getY());
            for (int i = 1; i < boundaryVertices.size(); i++) {
                Vector pos = boundaryVertices.get(i).getPosition();
                path.lineTo(pos.getX(), pos.getY());
            }
            path.closePath();
            return path;
        }

        private void setBounded(boolean bounded) {
            this.bounded = bounded;
        }

        private void setBoundaryVertices(ArrayList<Vertex> boundaryVertices) {
            this.boundaryVertices = boundaryVertices;
        }

        private void setBoundaryHalfedges(ArrayList<Halfedge> boundaryHalfedges) {
            this.boundaryHalfedges = boundaryHalfedges;
        }

        private void addHole(List<Vertex> hole) {
            holes.add(hole);
        }
    }
}
