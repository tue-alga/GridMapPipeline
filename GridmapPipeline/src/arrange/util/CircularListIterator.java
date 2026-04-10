package arrange.util;

import java.util.List;
import java.util.ListIterator;

/**
 *
 * @author Rafael Cano
 */
public class CircularListIterator<E> implements ListIterator<E> {

    private final List<E> list;
    private ListIterator<E> listIterator;

    public CircularListIterator(List<E> list) {
        this.list = list;
        this.listIterator = list.listIterator();
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public E next() {
        if (!listIterator.hasNext()) {
            listIterator = list.listIterator();
        }
        return listIterator.next();
    }

    @Override
    public boolean hasPrevious() {
        return true;
    }

    @Override
    public E previous() {
        if (!listIterator.hasPrevious()) {
            listIterator = list.listIterator(list.size());
        }
        return listIterator.previous();
    }

    @Override
    public int nextIndex() {
        if (!listIterator.hasNext()) {
            listIterator = list.listIterator();
        }
        return listIterator.nextIndex();
    }

    @Override
    public int previousIndex() {
        if (!listIterator.hasPrevious()) {
            listIterator = list.listIterator(list.size());
        }
        return listIterator.previousIndex();
    }

    @Override
    public void remove() {
        listIterator.remove();
    }

    @Override
    public void set(E e) {
        listIterator.set(e);
    }

    @Override
    public void add(E e) {
        if (!listIterator.hasPrevious()) {
            listIterator = list.listIterator(list.size());
        }
        listIterator.add(e);
    }
}
