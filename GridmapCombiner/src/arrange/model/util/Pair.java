package arrange.model.util;

/**
 *
 * @author Wouter Meulemans
 */
public final class Pair<T1, T2> implements Comparable<Pair<T1, T2>> {

    private T1 first;
    private T2 second;

    public Pair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }

    public Pair() {
    }

    public T1 getFirst() {
        return first;
    }

    public void setFirst(T1 first) {
        this.first = first;
    }

    public T2 getSecond() {
        return second;
    }

    public void setSecond(T2 second) {
        this.second = second;
    }

    @Override
    public String toString() {
        return "[" + first.toString() + ", " + second.toString() + "]";
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Pair<T1, T2> other = (Pair<T1, T2>) obj;
        if (this.first != other.first && (this.first == null || !this.first.equals(other.first))) {
            return false;
        }
        if (this.second != other.second && (this.second == null || !this.second.equals(other.second))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + (this.first != null ? this.first.hashCode() : 0);
        hash = 11 * hash + (this.second != null ? this.second.hashCode() : 0);
        return hash;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int compareTo(Pair<T1, T2> p) {
        int cmp1 = ((Comparable<T1>) first).compareTo(p.first);
        if (cmp1 == 0) {
            return ((Comparable<T2>) second).compareTo(p.second);
        } else {
            return cmp1;
        }
    }
}
