package arrange.model.util;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class LinkedHashMultiset<E> extends AbstractSet<E> implements Serializable, Cloneable, Multiset<E> {

    private LinkedHashMap<E, Entry<E>> elements;

    public LinkedHashMultiset() {
        elements = new LinkedHashMap<>();
    }

    public LinkedHashMultiset(LinkedHashMultiset<E> other) {
        elements = new LinkedHashMap<>();
        for (Map.Entry<E, Entry<E>> mapEntry : other.elements.entrySet()) {
            E key = mapEntry.getKey();
            elements.put(key, new Entry<>(mapEntry.getValue()));
        }
    }

    public LinkedHashMultiset(int initialCapacity) {
        elements = new LinkedHashMap<>(initialCapacity);
    }

    public LinkedHashMultiset(int initialCapacity, float loadFactor) {
        elements = new LinkedHashMap<>(initialCapacity, loadFactor);
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    @SuppressWarnings("element-type-mismatch")
    public boolean contains(Object o) {
        return elements.containsKey(o);
    }

    @Override
    public int getMultiplicity(E e) {
        Entry entry = elements.get(e);
        if (entry == null) {
            return 0;
        } else {
            return entry.multiplicity;
        }
    }

    @Override
    public Iterator<E> iterator() {
        return elements.keySet().iterator();
    }

    @Override
    public Set<Multiset.Entry<E>> entrySet() {
        return new EntrySet();
    }

    @Override
    public boolean add(E e) {
        Entry entry = elements.get(e);
        if (entry == null) {
            elements.put(e, new Entry<>(e, 1));
            return true;
        } else {
            entry.multiplicity += 1;
            return false;
        }
    }

    @Override
    public int add(E e, int times) {
        Entry entry = elements.get(e);
        if (entry == null) {
            elements.put(e, new Entry<>(e, times));
            return times;
        } else {
            entry.multiplicity += times;
            return entry.multiplicity;
        }
    }

    @Override
    @SuppressWarnings("element-type-mismatch")
    public boolean remove(Object o) {
        if (elements.remove(o) == null) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public int removeOne(E e) {
        Entry entry = elements.get(e);
        if (entry == null) {
            return -1;
        } else {
            if (entry.multiplicity == 1) {
                elements.remove(e);
                return 0;
            } else {
                entry.multiplicity--;
                return entry.multiplicity;
            }
        }
    }

    @Override
    public void clear() {
        elements.clear();
    }

    @Override
    public LinkedHashMultiset<E> clone() {
        return new LinkedHashMultiset<>(this);
    }

    private static class Entry<E> implements Multiset.Entry<E> {

        private final E element;
        private int multiplicity;

        private Entry(E element, int multiplicity) {
            this.element = element;
            this.multiplicity = multiplicity;
        }

        private Entry(Entry<E> other) {
            this.element = other.element;
            this.multiplicity = other.multiplicity;
        }

        @Override
        public E getElement() {
            return element;
        }

        @Override
        public void setMultiplicity(int multiplicity) {
            this.multiplicity = multiplicity;
        }

        @Override
        public int getMultiplicity() {
            return multiplicity;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (getClass() != o.getClass()) {
                return false;
            }
            Entry other = (Entry) o;
            if (this.element.equals(other.element)
                    && this.multiplicity == other.multiplicity) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 79 * hash + Objects.hashCode(this.element);
            hash = 79 * hash + this.multiplicity;
            return hash;
        }
    }

    private class EntrySet extends AbstractSet<Multiset.Entry<E>> {

        @Override
        public Iterator<Multiset.Entry<E>> iterator() {
            return new EntryIterator();
        }

        @Override
        @SuppressWarnings("element-type-mismatch")
        public boolean contains(Object o) {
            Entry entry = elements.get(o);
            if (entry == null) {
                return false;
            }
            return entry.equals(o);
        }

        @Override
        public boolean remove(Object o) {
            return LinkedHashMultiset.this.remove(o);
        }

        @Override
        public int size() {
            return elements.size();
        }

        @Override
        public void clear() {
            LinkedHashMultiset.this.clear();
        }
    }

    private final class EntryIterator implements Iterator<Multiset.Entry<E>> {

        Iterator<Entry<E>> iterator;

        public EntryIterator() {
            iterator = elements.values().iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Multiset.Entry<E> next() {
            return iterator.next();
        }

        @Override
        public void remove() {
            iterator.remove();
        }
    }
}
