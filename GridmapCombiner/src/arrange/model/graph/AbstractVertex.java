package arrange.model.graph;

import arrange.model.util.Identifier;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public abstract class AbstractVertex implements Identifier {

    private int id = -1;

    protected AbstractVertex() {
    }

    @Override
    public final int getId() {
        return id;
    }

    final void setId(int id) {
        this.id = id;
    }
}
