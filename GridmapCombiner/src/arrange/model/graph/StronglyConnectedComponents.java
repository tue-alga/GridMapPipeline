package arrange.model.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import arrange.model.graph.DepthFirstSearchTraverser.EdgeType;
import arrange.model.util.ElementList;

/**
 * Implementation of Tarjan's strongly connected components algorithm. The order
 * in which the strongly connected components are identified constitutes a
 * reverse topological sort of the DAG formed by the strongly connected
 * components.
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class StronglyConnectedComponents<V extends AbstractVertex, E extends AbstractEdge> {

    private final GenericDigraph<V, E> g;
    private final ElementList<Integer> componentIndex;
    private final ArrayList<Set<V>> components;

    public StronglyConnectedComponents(GenericDigraph<V, E> g) {
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
        LocalVisitor visitor = new LocalVisitor();
        DepthFirstSearchTraverser<V, E> dfs = new DepthFirstSearchTraverser<>(g, visitor);
        dfs.traverse();
    }

    

    private class LocalVisitor extends DepthFirstSearchTraverser.Visitor<V, E> {

        private int nextIndex = 0;
        private final ArrayDeque<V> stack = new ArrayDeque<>();
        private final ElementList<Integer> index = new ElementList<>(g.numberOfVertices(), null);
        private final ElementList<Integer> link = new ElementList<>(g.numberOfVertices(), null);
        private final ElementList<Boolean> inStack = new ElementList<>(g.numberOfVertices(), false);

        @Override
        public void discoverVertex(DepthFirstSearchTraverser<V, E> traverser, V v) {
            index.set(v, nextIndex);
            link.set(v, nextIndex);
            nextIndex++;
            stack.push(v);
            inStack.set(v, true);
        }

        @Override
        public void preExploreEdge(DepthFirstSearchTraverser<V, E> traverser, E e) {
            V v = g.getSource(e);
            V w = g.getTarget(e);
            if (traverser.getEdgeType(e) == EdgeType.BACK && inStack.get(w)) {
                int linkV = Math.min(link.get(v), index.get(w));
                link.set(v, linkV);
            }
        }

        @Override
        public void postExploreEdge(DepthFirstSearchTraverser<V, E> traverser, E e) {
            if (traverser.getEdgeType(e) == EdgeType.TREE) {
                V v = g.getSource(e);
                V w = g.getTarget(e);
                int linkV = Math.min(link.get(v), link.get(w));
                link.set(v, linkV);
            }
        }

        @Override
        public void finishVertex(DepthFirstSearchTraverser<V, E> traverser, V v) {
            if (link.get(v) == index.get(v)) {
                int currentIndex = components.size();
                Set<V> component = new LinkedHashSet<>();
                V w;
                do {
                    w = stack.pop();
                    inStack.set(w, false);
                    component.add(w);
                    componentIndex.set(w, currentIndex);
                } while (w != v);
                components.add(component);
            }
        }
    }
}
