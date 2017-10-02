/*
 * Copyright 2007 Tom Gibara
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.hortonmachine.gears.utils.clustering;

import java.util.Arrays;

/**
 * Maintains a heap of cluster pairs.
 * 
 * @author Tom Gibara
 *
 * @param <K> the key type
 */

class GvmClusterPairs<S extends GvmSpace, K> {

	private GvmClusterPair<S,K>[] pairs;
	private int size;

	@SuppressWarnings("unchecked")
	GvmClusterPairs(int initialCapacity) {
		pairs = new GvmClusterPair[initialCapacity];
		size = 0;
	}

	GvmClusterPairs(GvmClusterPairs<S,K> that) {
		pairs = Arrays.copyOf(that.pairs, that.size);
		size = pairs.length;
	}

    public boolean add(GvmClusterPair<S,K> e) {
        if (e == null) throw new IllegalArgumentException("null pair");
        int i = size;
        if (i >= pairs.length) grow(i + 1);
        size = i + 1;
        if (i == 0) {
            pairs[0] = e;
            e.index = 0;
        } else {
            heapifyUp(i, e);
        }
        return true;
    }

    public GvmClusterPair<S,K> peek() {
    	return size == 0 ? null : pairs[0];
    }

    public boolean remove(GvmClusterPair<S,K> pair) {
		int i = indexOf(pair);
		if (i == -1) return false;
	    removeAt(i);
	    return true;
    }

    public void reprioritize(GvmClusterPair<S,K> pair) {
    	int i = indexOf(pair);
    	if (i == -1) throw new IllegalArgumentException("no such pair");
    	pair.update();
        GvmClusterPair<S,K> parent = i == 0 ? null : pairs[ (i - 1) >>> 1 ];
        if (parent != null && parent.value > pair.value) {
        	heapifyUp(i, pair);
        } else {
        	heapifyDown(i, pair);
        }
    }

    public int size() {
        return size;
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
        	GvmClusterPair<S,K> e = pairs[i];
        	e.index = -1;
            pairs[i] = e;
        }
        size = 0;
    }

    private void grow(int minCapacity) {
        if (minCapacity < 0) throw new IllegalStateException("can't grow, maximum number of elements exceeded");
        int oldCapacity = pairs.length;
        int newCapacity = ((oldCapacity < 64)? ((oldCapacity + 1) * 2): ((oldCapacity / 2) * 3));
        if (newCapacity < 0) newCapacity = Integer.MAX_VALUE;
        if (newCapacity < minCapacity) newCapacity = minCapacity;
        pairs = Arrays.copyOf(pairs, newCapacity);
    }

    private int indexOf(GvmClusterPair<S,K> pair) {
    	return pair == null ? -1 : pair.index;
    }

    private GvmClusterPair<S,K> removeAt(int i) {
        int s = --size;
        if (s == i) { // removing last element
        	pairs[i].index = -1;
            pairs[i] = null;
        } else {
            GvmClusterPair<S,K> moved = pairs[s];
            pairs[s] = null;
            moved.index = -1;
            heapifyDown(i, moved);
            if (pairs[i] == moved) {
                heapifyUp(i, moved);
                if (pairs[i] != moved) return moved;
            }
        }
        return null;
    }

    private void heapifyUp(int k, GvmClusterPair<S,K> pair) {
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            GvmClusterPair<S,K> e = pairs[parent];
            if (pair.value >= e.value) break;
            pairs[k] = e;
            e.index = k;
            k = parent;
        }
        pairs[k] = pair;
        pair.index = k;
    }

    private void heapifyDown(int k, GvmClusterPair<S,K> pair) {
        int half = size >>> 1;
        while (k < half) {
            int child = (k << 1) + 1;
            GvmClusterPair<S,K> c = pairs[child];
            int right = child + 1;
            if (right < size && c.value > pairs[right].value) c = pairs[child = right];
            if (pair.value <= c.value) break;
            pairs[k] = c;
            c.index = k;
            k = child;
        }
        pairs[k] = pair;
        pair.index = k;
    }

}
