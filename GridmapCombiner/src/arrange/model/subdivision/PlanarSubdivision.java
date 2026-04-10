package arrange.model.subdivision;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import arrange.Utils.Utils;
import arrange.model.graph.AbstractEdge;
import arrange.model.graph.AbstractGraph;
import arrange.model.graph.AbstractVertex;
import arrange.model.graph.CrossingFinder;
import arrange.model.graph.GenericGraph;
import arrange.model.graph.PlanarStraightLineGraph;
import arrange.model.graph.PlanarStraightLineGraphAlgorithms;
import arrange.model.util.ElementList;
import arrange.model.util.Identifier;
import arrange.model.util.IpeExporter;
import arrange.model.util.Position2D;
import arrange.model.util.Vector2D;

/**
 * Planar subdivision implemented using a DCEL data structure. The algorithm is
 * not prepared to work with holes. Corresponding vertices of the original graph
 * and the subdivision have the same id. The halfedges corresponding to an edge
 * with id eId have ids 2*eId and 2*eId+1.
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class PlanarSubdivision {

    private final ArrayList<Vertex> vertices;
    private final ArrayList<Halfedge> halfedges;
    private final ArrayList<Face> faces;

    public PlanarSubdivision(GenericGraph<? extends Position2D, ?> graph) {
        if (PlanarStraightLineGraphAlgorithms.hasCrossings(graph)) {
            IpeExporter.exportGraph(graph, "graph.ipe");
            throw new RuntimeException("cannot embed graph, crossings detected");
        }

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

    public Iterable<? extends Vertex> vertices() {
        return Collections.unmodifiableList(vertices);
    }

    public Vertex getVertex(int id) {
        return vertices.get(id);
    }

    public Iterable<? extends Halfedge> halfedges() {
        return Collections.unmodifiableList(halfedges);
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

    public Iterable<? extends Face> faces() {
        return Collections.unmodifiableList(faces);
    }

    public Iterable<? extends Face> boundedFaces() {
        if (!faces.isEmpty()) {
            return Collections.unmodifiableList(faces.subList(0, faces.size() - 1));
        } else {
            return Collections.unmodifiableList(faces);
        }
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

    /**
     * Computes the weak dual graph of this subdivision. Corresponding vertices
     * and faces have the same id.
     */
    public <V extends AbstractVertex & Position2D, E extends AbstractEdge> void computeWeakDual(GenericGraph<V, E> graph) {
        graph.clear();
        for (Face f : boundedFaces()) {
            Vector2D position = null;
            if (f instanceof Map.Face mf) {                
                if (mf.getLabel() != null) {
                    position = mf.getLabel().getPosition();
                }
            } 
            if (position == null) {
                position = Utils.meanPosition(f.getBoundaryVertices());            
            }
            V v = graph.addVertex();
            v.getPosition().setX(position.getX());
            v.getPosition().setY(position.getY());
        }
        ElementList<Boolean> done = new ElementList<>(faces.size(), false);
        ElementList<Boolean> added = new ElementList<>(faces.size(), false);
        for (Face f : boundedFaces()) {
            int faceId = f.getId();
            List<? extends Halfedge> boundaryHalfedges = f.getBoundaryHalfedges();
            for (Halfedge h : boundaryHalfedges) {
                Face neighbour = h.getTwin().getFace();
                if (neighbour.isBounded()) {
                    int neighbourId = neighbour.getId();
                    if (!done.get(neighbourId) && !added.get(neighbourId) && notCrossing(graph, faceId, neighbourId)) {
                        graph.addEdge(faceId, neighbourId);
                        added.set(neighbourId, true);
                    }
                }
            }
            for (Halfedge h : boundaryHalfedges) {
                int neighbourId = h.getTwin().getFace().getId();
                added.set(neighbourId, false);
            }
            done.set(faceId, true);
        }
    }

    /**
     * Computes the dual graph of this subdivision. Corresponding vertices and
     * faces have the same id.
     */
    public void computeDual(PlanarStraightLineGraph graph) {
        graph.clear();
        for (Face f : faces()) {
            Vector2D position = Utils.meanPosition(f.getBoundaryVertices());
            graph.addVertex(position);
        }
        ElementList<Boolean> added = new ElementList<>(faces.size(), false);
        for (Face f : boundedFaces()) {
            int faceId = f.getId();
            for (Halfedge h : f.getBoundaryHalfedges()) {
                int neighbourId = h.getTwin().getFace().getId();
                if (faceId < neighbourId && !added.get(neighbourId)) {
                    graph.addEdge(faceId, neighbourId);
                    added.set(neighbourId, true);
                }
            }
            for (Halfedge h : f.getBoundaryHalfedges()) {
                int neighbourId = h.getTwin().getFace().getId();
                added.set(neighbourId, false);
            }
        }
    }

    /**
     * Creates a new subdivision restricted to the faces in the specified list.
     * Assumes that no new faces are created, i.e., the faces of the new
     * subdivision are either the ones in the given list or the unbounded face.
     * The ids of the new faces in the restricted subdivision match the index of
     * the associated original face in the given list. Twin halfedges have
     * consecutive ids. No other guarantees are given on ids.
     */
    public PlanarSubdivision restrictToFaces(List<? extends Face> restrictedFaces) {
        PlanarSubdivision restricted = new PlanarSubdivision();
        restrictToFaces(restrictedFaces, restricted);
        return restricted;
    }

    protected final void restrictToFaces(List<? extends Face> restrictedFaces, PlanarSubdivision restricted) {
        ElementList<Vertex> correspondingVertices = new ElementList<>(vertices.size(), null);
        ElementList<Halfedge> correspondingHalfedges = new ElementList<>(halfedges.size(), null);
        ElementList<Face> correspondingFaces = new ElementList<>(faces.size(), null);
        // Create faces and vertices
        for (Face oldFace : restrictedFaces) {
            if (!oldFace.isBounded()) {
                throw new RuntimeException("restricted faces must be bounded");
            }
            Face newFace = restricted.createFace();
            newFace.setId(restricted.nextFaceId());
            newFace.setBounded(true);
            restricted.faces.add(newFace);
            correspondingFaces.set(oldFace, newFace);
            for (Vertex oldVertex : oldFace.getBoundaryVertices()) {
                Vertex newVertex = restricted.createVertex();
                newVertex.setId(restricted.nextVertexId());
                newVertex.setPosition(new Vector2D(oldVertex.getPosition()));
                restricted.vertices.add(newVertex);
                correspondingVertices.set(oldVertex, newVertex);
            }
        }
        // Create unbounded face
        Face unboundedFace = restricted.createFace();
        unboundedFace.setId(restricted.nextFaceId());
        unboundedFace.setBounded(false);
        restricted.faces.add(unboundedFace);
        correspondingFaces.set(getUnboundedFace(), unboundedFace);
        // Create halfedges
        for (Halfedge oldHalfedge : halfedges) {
            Vertex oldSource = oldHalfedge.getSource();
            Vertex oldTarget = oldHalfedge.getTarget();
            Vertex newSource = correspondingVertices.get(oldSource);
            Vertex newTarget = correspondingVertices.get(oldTarget);
            if (newSource != null && newTarget != null) {
                Halfedge newHalfedge = restricted.createHalfedge();
                newHalfedge.setId(restricted.nextHalfedgeId());
                newHalfedge.setSource(newSource);
                Face oldFace = oldHalfedge.getFace();
                Face newFace = correspondingFaces.get(oldFace);
                if (newFace != null) {
                    newHalfedge.setFace(newFace);
                } else {
                    newHalfedge.setFace(unboundedFace);
                }
                restricted.halfedges.add(newHalfedge);
                correspondingHalfedges.set(oldHalfedge, newHalfedge);
            }
        }
        // Finalize vertices
        for (Vertex oldVertex : vertices) {
            Vertex newVertex = correspondingVertices.get(oldVertex);
            if (newVertex != null) {
                for (Halfedge oldHalfedge : oldVertex.getOutgoingHalfedges()) {
                    Halfedge newHalfedge = correspondingHalfedges.get(oldHalfedge);
                    if (newHalfedge != null) {
                        newVertex.addOutgoingHalfegde(newHalfedge);
                    }
                }
            }
        }
        // Finalize halfedges
        for (Halfedge oldHalfedge : halfedges) {
            Halfedge newHalfedge = correspondingHalfedges.get(oldHalfedge);
            if (newHalfedge != null) {
                Halfedge oldTwin = oldHalfedge.getTwin();
                Halfedge newTwin = correspondingHalfedges.get(oldTwin);
                newHalfedge.setTwin(newTwin);
            }
        }
        for (Vertex v : restricted.vertices) {
            List<? extends Halfedge> outgoingHalfedges = v.getOutgoingHalfedges();
            int total = outgoingHalfedges.size();
            for (int i = 0; i < total; i++) {
                Halfedge current = outgoingHalfedges.get(i);
                Halfedge twin = current.getTwin();
                Halfedge next = outgoingHalfedges.get((i + 1) % total);
                twin.setNext(next);
                next.setPrevious(twin);
            }
        }
        // Finalize faces
        for (Face oldFace : restrictedFaces) {
            Face newFace = correspondingFaces.get(oldFace);
            for (Halfedge oldHalfedge : oldFace.getBoundaryHalfedges()) {
                Halfedge newHalfedge = correspondingHalfedges.get(oldHalfedge);
                newFace.addBoundaryHalfedge(newHalfedge);
            }
            for (Vertex oldVertex : oldFace.getBoundaryVertices()) {
                Vertex newVertex = correspondingVertices.get(oldVertex);
                newFace.addBoundaryVertex(newVertex);
            }
        }
        ElementList<Boolean> processed = new ElementList<>(restricted.numberOfHalfedges(), false);
        for (Halfedge e : restricted.halfedges) {
            if (e.getFace() == unboundedFace && !processed.get(e)) {
                ArrayList<Vertex> hole = new ArrayList<>();
                Halfedge first = e;
                Halfedge current = e;
                do {
                    hole.add(current.getSource());
                    processed.set(current, true);
                    current = current.getNext();
                } while (current != first);
                unboundedFace.addHole(hole);
            }
        }
    }

    protected Vertex createVertex() {
        return new Vertex();
    }

    protected Halfedge createHalfedge() {
        return new Halfedge();
    }

    protected Face createFace() {
        return new Face();
    }

    private int nextVertexId() {
        return vertices.size();
    }

    private int nextHalfedgeId() {
        return halfedges.size();
    }

    private int nextFaceId() {
        return faces.size();
    }

    private <V extends AbstractVertex & Position2D, E extends AbstractEdge> void initialize(AbstractGraph<V, E> graph) {
        // Create vertices
        for (int i = 0; i < graph.numberOfVertices(); i++) {
            V oldVertex = graph.getVertex(i);
            Vertex newVertex = createVertex();
            newVertex.setId(nextVertexId());
            newVertex.getPosition().setX(oldVertex.getPosition().getX());
            newVertex.getPosition().setY(oldVertex.getPosition().getY());
            vertices.add(newVertex);
        }

        // Create halfedges
        for (int i = 0; i < graph.numberOfEdges(); i++) {
            E oldEdge = graph.getEdge(i);
            Vertex sourceH1 = vertices.get(graph.getSource(oldEdge).getId());
            Vertex sourceH2 = vertices.get(graph.getTarget(oldEdge).getId());
            Halfedge h1 = createHalfedge();
            Halfedge h2 = createHalfedge();
            h1.setId(nextHalfedgeId());
            h1.setSource(sourceH1);
            sourceH1.addOutgoingHalfegde(h1);
            h1.setTwin(h2);
            halfedges.add(h1);
            h2.setId(nextHalfedgeId());
            h2.setSource(sourceH2);
            sourceH2.addOutgoingHalfegde(h2);
            h2.setTwin(h1);
            halfedges.add(h2);
        }

        // Sort outgoing halfedges in clockwise order around each vertex and set
        // set the next and previous fields of each halfedge
        for (Vertex v : vertices) {
            v.sortOutgoingHalfedges();
            List<? extends Halfedge> outgoingHalfedges = v.getOutgoingHalfedges();
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
                Vector2D leftmostPosition = h.getSource().getPosition();
                int leftmostIndex = 0;
                int i = 0;
                do {
                    boundaryVertices.add(current.getSource());
                    boundaryHalfedges.add(current);
                    Vector2D currentPosition = current.getSource().getPosition();
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
                Vector2D previous = h2.getPrevious().getSource().getPosition();
                Vector2D next = h2.getTarget().getPosition();
                if (Utils.triangleOrientation(previous, leftmostPosition, next) == 1) {
                    Face f = createFace();
                    f.setId(nextFaceId());
                    f.setBounded(true);
                    f.setBoundaryHalfedges(boundaryHalfedges);
                    f.setBoundaryVertices(boundaryVertices);
                    for (Halfedge bh : boundaryHalfedges) {
                        bh.setFace(f);
                    }
                    faces.add(f);
                } else {
                    if (unboundedFace == null) {
                        unboundedFace = createFace();
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
            unboundedFace.setId(nextFaceId());
            faces.add(unboundedFace);
        }

    }

    /**
     * Returns true if adding an edge from faceId to neighbourId does not add a
     * crossing edge to the graph
     *
     * @param <V>
     * @param <E>
     * @param graph
     * @param faceId
     * @param neighbourId
     * @return
     */
    private <V extends AbstractVertex & Position2D, E extends AbstractEdge> boolean notCrossing(GenericGraph<V, E> graph, int sourceId, int targetId) {
        //get position of source and target
        V eiSource = graph.getVertex(sourceId);
        V eiTarget = graph.getVertex(targetId);
        Vector2D piSource = eiSource.getPosition();
        Vector2D piTarget = eiTarget.getPosition();

        for (E ej : graph.edges()) {
            //get position of edges source and target
            V ejSource = graph.getSource(ej);
            V ejTarget = graph.getTarget(ej);
            Vector2D pjSource = eiSource.getPosition();
            Vector2D pjTarget = eiTarget.getPosition();

            //if they share a start or end vertex they do not cross
            if (eiSource == ejSource || eiSource == ejTarget || eiTarget == ejSource || eiTarget == ejTarget) {
                continue;
            }
            Vector2D crossing = Utils.lineSegmentIntersection(piSource, piTarget, pjSource, pjTarget);
            if (crossing != null) {
                //they cross
                //System.out.println("cross");
                return false;
            }
        }
        //no crossings with any edges
        return true;
    }

    public class Vertex implements Identifier, Position2D {

        private int id = -1;
        private Vector2D position = new Vector2D(0, 0);
        private final ArrayList<Halfedge> outgoingHalfedges = new ArrayList<>();

        protected Vertex() {
        }

        @Override
        public final int getId() {
            return id;
        }

        @Override
        public final Vector2D getPosition() {
            return position;
        }

        public final void setPosition(Vector2D position) {
            this.position = position;
        }

        public List<? extends Halfedge> getOutgoingHalfedges() {
            return Collections.unmodifiableList(outgoingHalfedges);
        }

        private void setId(int id) {
            this.id = id;
        }

        private void addOutgoingHalfegde(Halfedge h) {
            outgoingHalfedges.add(h);
        }

        private void sortOutgoingHalfedges() {
            Comparator<Halfedge> comparator = new Comparator<Halfedge>() {
                @Override
                public int compare(Halfedge h1, Halfedge h2) {
                    Vector2D center = h1.getSource().getPosition();
                    Vector2D v1 = h1.getTarget().getPosition();
                    Vector2D v2 = h2.getTarget().getPosition();
                    return Utils.counterclockwiseCompare(center, v2, v1);
                }
            };
            Collections.sort(outgoingHalfedges, comparator);
        }
        /////////////////////// REMOVE ////////////////////////////
        public String name;

        @Override
        public String toString() {
            return name;
        }
    }

    public class Halfedge implements Identifier {

        private int id = -1;
        private Vertex source = null;
        private Halfedge twin = null;
        private Halfedge next = null;
        private Halfedge previous = null;
        private Face face = null;

        protected Halfedge() {
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

        private void setId(int id) {
            this.id = id;
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

        private int id = -1;
        private boolean bounded = true;
        private ArrayList<Vertex> boundaryVertices = new ArrayList<>();
        private ArrayList<Halfedge> boundaryHalfedges = new ArrayList<>();
        private final ArrayList<List<Vertex>> holes = new ArrayList<>();

        protected Face() {
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

        public List<? extends Vertex> getBoundaryVertices() {
            return boundaryVertices;
        }

        public List<? extends Halfedge> getBoundaryHalfedges() {
            return Collections.unmodifiableList(boundaryHalfedges);
        }

        public int numberOfHoles() {
            return holes.size();
        }

        public List<? extends List<? extends Vertex>> getHoles() {
            return Collections.unmodifiableList(holes);
        }

        public Path2D.Double toPath2D() {
            Path2D.Double path = new Path2D.Double();
            Vector2D first = boundaryVertices.get(0).getPosition();
            path.moveTo(first.getX(), first.getY());
            for (int i = 1; i < boundaryVertices.size(); i++) {
                Vector2D pos = boundaryVertices.get(i).getPosition();
                path.lineTo(pos.getX(), pos.getY());
            }
            path.closePath();
            return path;
        }

        private void setId(int id) {
            this.id = id;
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

        private void addBoundaryVertex(Vertex v) {
            boundaryVertices.add(v);
        }

        private void addBoundaryHalfedge(Halfedge h) {
            boundaryHalfedges.add(h);
        }

        private void addHole(List<Vertex> hole) {
            holes.add(hole);
        }
    }

    private static class PlaneSweep {

        PlanarSubdivision ps;
        ArrayList<Event> events;

        PlaneSweep(PlanarSubdivision ps) {
            this.ps = ps;
            // Create events
            for (Halfedge h : ps.halfedges) {
                events.add(new Event(h, null));
            }
            // Sort in ascending x order (ascending y to break ties)
            Collections.sort(events);
            // Set the type of each event
            ElementList<Boolean> seen = new ElementList<>(ps.halfedges.size(), false);
            for (Event e : events) {
                Halfedge h = e.halfedge;
                if (seen.get(h.getTwin())) {
                    e.type = EventType.END_SEGMENT;
                } else {
                    e.type = EventType.BEGIN_SEGMENT;
                    seen.set(h, true);
                }
            }
        }

        class Event implements Comparable<Event> {

            Halfedge halfedge;
            EventType type;

            Event(Halfedge halfedge, EventType type) {
                this.halfedge = halfedge;
                this.type = type;
            }

            @Override
            public int compareTo(Event e2) {
                Vector2D p1 = this.halfedge.getSource().getPosition();
                Vector2D p2 = e2.halfedge.getSource().getPosition();
                int compare = Double.compare(p1.getX(), p2.getX());
                if (compare == 0) {
                    return Double.compare(p1.getY(), p2.getY());
                } else {
                    return compare;
                }
            }
        }

        enum EventType {

            BEGIN_SEGMENT, END_SEGMENT;
        }
    }
}
