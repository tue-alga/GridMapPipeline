package arrange.model.util;

import java.util.Set;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public interface Multiset<E> extends Set<E> {

    public int add(E e, int times);

    public int removeOne(E e);

    public int getMultiplicity(E e);

    public Set<Entry<E>> entrySet();

    static interface Entry<E> {

        public E getElement();

        public void setMultiplicity(int multiplicity);

        public int getMultiplicity();
    }
}
