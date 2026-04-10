package arrange.model.util;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class WeightedObject<O, W extends Comparable<W>> implements Comparable<WeightedObject<O, W>> {

    private final O object;
    private final W weight;

    public WeightedObject(O object, W weight) {
        this.object = object;
        this.weight = weight;
    }

    public O getObject() {
        return object;
    }

    public W getWeight() {
        return weight;
    }

    @Override
    public int compareTo(WeightedObject<O, W> o) {
        return weight.compareTo(o.weight);
    }
}
