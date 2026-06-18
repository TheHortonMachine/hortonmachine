package org.hortonmachine.hmachine.modules.statistics.kriging.loo;

import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public final class ExcludingMap extends HashMap<Integer, double[]> {

	private final HashMap<Integer, double[]> delegate;
	private Integer excludedId;

	public ExcludingMap(HashMap<Integer, double[]> delegate) {
		this.delegate = Objects.requireNonNull(delegate);
	}

	public void setExcludedId(Integer excludedId) {
		this.excludedId = excludedId;
	}

	private boolean isExcluded(Object key) {
		return excludedId != null && excludedId.equals(key);
	}

	@Override
	public double[] get(Object key) {
		return isExcluded(key) ? null : delegate.get(key);
	}

	@Override
	public boolean containsKey(Object key) {
		return !isExcluded(key) && delegate.containsKey(key);
	}

	@Override
	public int size() {
		return (excludedId != null && delegate.containsKey(excludedId)) ? delegate.size() - 1 : delegate.size();
	}

	@Override
	public Set<Entry<Integer, double[]>> entrySet() {
		return new AbstractSet<>() {
			@Override
			public Iterator<Entry<Integer, double[]>> iterator() {
				Iterator<Entry<Integer, double[]>> it = delegate.entrySet().iterator();
				return new Iterator<>() {
					Entry<Integer, double[]> next = advance();

					private Entry<Integer, double[]> advance() {
						while (it.hasNext()) {
							Entry<Integer, double[]> e = it.next();
							if (!isExcluded(e.getKey()))
								return e;
						}
						return null;
					}

					@Override
					public boolean hasNext() {
						return next != null;
					}

					@Override
					public Entry<Integer, double[]> next() {
						if (next == null)
							throw new NoSuchElementException();
						Entry<Integer, double[]> cur = next;
						next = advance();
						return cur;
					}
				};
			}

			@Override
			public int size() {
				return ExcludingMap.this.size();
			}
		};
	}

	// Per sicurezza: impedisci modifiche tramite la view
	@Override
	public double[] put(Integer key, double[] value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double[] remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends Integer, ? extends double[]> m) {
		throw new UnsupportedOperationException();
	}
}
