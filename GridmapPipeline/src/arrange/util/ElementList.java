package arrange.util;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author Rafael Cano
 */
public class ElementList<E> extends ArrayList<E> {

    /**
     * Constructs an empty list with an initial capacity of ten.
     */
    public ElementList() {
        super();
    }

    /**
     * Constructs a list containing the elements of the specified collection, in
     * the order they are returned by the collection's iterator.
     */
    public ElementList(Collection<? extends E> c) {
        super(c);
    }

    /**
     * Constructs an empty list with the specified initial capacity.
     */
    public ElementList(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Constructs a list containing n copies of the specified element.
     */
    public ElementList(int n, E element) {
        super(n);
        for (int i = 0; i < n; i++) {
            add(element);
        }
    }

    /**
     * Clears the contents of the list and refills it with n copies of the
     * specified element.
     */
    public void assign(int n, E element) {
        clear();
        for (int i = 0; i < n; i++) {
            add(element);
        }
    }

    /**
     * Returns the element at the position specified by the identifier.
     */
    public E get(Identifier identifier) {
        return super.get(identifier.getId());
    }

    /**
     * Replaces the element at the position specified by the identifier with the
     * specified element.
     */
    public E set(Identifier identifier, E element) {
        return super.set(identifier.getId(), element);
    }
}
