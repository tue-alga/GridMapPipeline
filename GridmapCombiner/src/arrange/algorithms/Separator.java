package arrange.algorithms;

import java.util.LinkedHashSet;
import java.util.Set;
import arrange.model.Network;
import arrange.model.graph.ConnectedComponents;


public final class Separator {

    public final Network.Vertex v1;
    public final Network.Vertex v2;
    public final LinkedHashSet<Integer> component1;
    public final LinkedHashSet<Integer> component2;

    public Separator(Network weakDual, Network.Edge cutEdge) {
        Network.Vertex source = cutEdge.getSource();
        Network.Vertex target = cutEdge.getTarget();
        Network copy = new Network(weakDual);
        copy.removeEdge(copy.getEdge(cutEdge.getId()));
        ConnectedComponents<Network.Vertex, Network.Edge> cc = new ConnectedComponents<>(copy);
        if (cc.numberOfComponents() != 2) {
            throw new RuntimeException();
        }
        Set<Network.Vertex> s1 = cc.getComponent(0);
        Set<Network.Vertex> s2 = cc.getComponent(1);
        if (s1.size() > s2.size()) {
            Set<Network.Vertex> auxS = s1;
            s1 = s2;
            s2 = auxS;
            v1 = target;
            v2 = source;
        } else {
            v1 = source;
            v2 = target;
        }
        component1 = new LinkedHashSet<>();
        for (Network.Vertex v : s1) {
            component1.add(v.getId());
        }
        component2 = new LinkedHashSet<>();
        for (Network.Vertex v : s2) {
            component2.add(v.getId());
        }
    }
}
