package assign;

import common.mcf.FlowDigraph;
import common.gridmath.GridMath.Coordinate;
import common.Outline;
import common.Partition;
import common.Site;
import common.SiteMap;
import common.Stage;
import common.util.Stopwatch;
import common.util.Transform;
import nl.tue.geometrycore.geometry.Vector;

/**
 *
 * @author Wouter Meulemans
 */
public class AssignAlgorithm {

    private final AgnosticAlignment alignment;
    private final AwareAlignment refine;

    public AssignAlgorithm(AgnosticAlignment alignment, AwareAlignment refine) {
        this.alignment = alignment;
        this.refine = refine;
    }

    public void run(SiteMap map) {
        map.revertToStage(Stage.DEFORMED);

        System.out.println("------ ASSIGNING TO CELLS --------------------------");
        System.out.println("  alignment = " + alignment + ", refine = " + refine);

        Stopwatch sw = Stopwatch.get("assign").start();

        double cost = 0;
        for (Partition p : map.partitions()) {
            if (p.sites.size() == 1) {
                Site s = p.sites.get(0);
                Coordinate c = p.cells.iterator().next();
                s.setCell(c);
            } else {

                double c = solveFlow(p, alignment.construct(p));
                if (refine != null) {
                    c = solveFlow(p, refine.construct(p));
                }
                cost += c;
            }
        }

        sw.stop();

        // exclude analysis from running time
        System.out.println("Total sq. Euclidean distance: " + cost);
        int sitecount = 0;
        for (Outline o : map.outlines) {
            sitecount += o.sites.size();
        }
        System.out.println("Root mean sq. Euclidean distance: " + Math.sqrt(cost / sitecount));

        map.stage = Stage.ASSIGNED;
    }

    private static double solveFlow(Partition partition, Transform align) {
        FlowDigraph flownetwork = new FlowDigraph();

        for (Site s : partition.sites) {
            FlowDigraph.Vertex v = flownetwork.addVertex();
            v.setSupply(1);
            v.setCapacity(1);
        }

        Coordinate[] cs = partition.cells.extract();

        for (Coordinate c : cs) {
            FlowDigraph.Vertex v = flownetwork.addVertex();
            v.setSupply(-1);
            v.setCapacity(1);

            Vector vc = c.toVector();

            int i = 0;
            for (Site s : partition.sites) {
                FlowDigraph.Edge e = flownetwork.addEdge(flownetwork.getVertex(i++), v);
                e.setCapacity(1);
                e.setWeight(vc.squaredDistanceTo(align.apply(s)));
            }
        }

        Stopwatch sw = Stopwatch.get("- solve").start();
        int[] flow = flownetwork.solve();
        sw.stop();

        double cost = 0;
        for (FlowDigraph.Edge e : flownetwork.edges()) {
            if (flow[e.getId()] == 1) {
                cost += e.getWeight();

                Site s = partition.sites.get(e.getSource().getId());
                Coordinate c = cs[e.getTarget().getId() - partition.sites.size()];

                s.setCell(c);
            }
        }
        return cost;
    }
}
