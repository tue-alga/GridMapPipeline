package arrange.model.graph;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public abstract class GraphTraverser {

    public abstract void traverse();

    public abstract void stop();

    protected static class StoppedTraversalException extends Exception {

        public StoppedTraversalException() {
        }
    }
}
