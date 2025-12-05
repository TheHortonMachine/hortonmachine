package org.hortonmachine.gears.utils;

import java.util.List;

/**
 * A supplier that cycles through a list of items and starts over when the end is reached.
 */
public class CyclicSupplier<T> {
    private final List<T> list;
    private int index = 0;

    public CyclicSupplier(List<T> list) {
        if (list.isEmpty()) throw new IllegalArgumentException();
        this.list = list;
    }

    public T next() {
        T val = list.get(index);
        index = (index + 1) % list.size();
        return val;
    }
}
