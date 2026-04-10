package arrange.model.graph;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import arrange.model.util.ElementList;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class ConnectedComponents<V extends AbstractVertex, E extends AbstractEdge> {

    private final GenericGraph<V, E> g;
    private final ElementList<Integer> componentIndex;
    private final ArrayList<Set<V>> components;

    public ConnectedComponents(GenericGraph<V, E> g) {
        this.g = g;
        this.componentIndex = new ElementList<>(g.numberOfVertices(), 0);
        this.components = new ArrayList<>();
        findComponents();
    }

    public int numberOfComponents() {
        return components.size();
    }

    public Set<V> getComponent(int index) {
        return components.get(index);
    }

    public int getComponentIndex(V v) {
        return componentIndex.get(v);
    }

    public Iterable<Set<V>> components() {
        return components;
    }

    private void findComponents() {
        class LocalVisitor extends DepthFirstSearchTraverser.Visitor<V, E> {

            @Override
            public void startVertex(DepthFirstSearchTraverser<V, E> traverser, V v) {
                LinkedHashSet<V> newComponent = new LinkedHashSet<>();
                newComponent.add(v);
                components.add(newComponent);
                componentIndex.set(v, components.size() - 1);
            }

            @Override
            public void discoverVertex(DepthFirstSearchTraverser<V, E> traverser, V v) {
                int last = components.size() - 1;
                components.get(last).add(v);
                componentIndex.set(v, last);
            }
        }

        LocalVisitor visitor = new LocalVisitor();
        DepthFirstSearchTraverser<V, E> traverser = new DepthFirstSearchTraverser<>(g, visitor);
        traverser.traverse();
    }
}
