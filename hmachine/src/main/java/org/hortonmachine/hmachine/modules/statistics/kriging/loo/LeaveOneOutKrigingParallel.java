package org.hortonmachine.hmachine.modules.statistics.kriging.loo;


import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.libs.monitor.LogProgressMonitor;
import org.hortonmachine.hmachine.modules.statistics.kriging.pointcase.KrigingPointCase;

import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;

public class LeaveOneOutKrigingParallel extends HMModel {

	@Description("The .shp of the measurement point, containing the position of the stations.")
	@In
	public SimpleFeatureCollection inStations = null;

	@Description("The field of the vector of stations, defining the id.")
	@In
	public String fStationsid = null;

	@Description("The field of the vector of stations, defining the elevation.")
	@In
	public String fStationsZ = null;

	@Description("The type of theoretical semivariogram: exponential, gaussian, spherical, pentaspherical "
			+ "linear, circular, bessel, periodic, hole, logaritmic, power, spline")
	@In
	public String pSemivariogramType = null;

	@Description("The HM with the measured data to be interpolated.")
	@In
	public HashMap<Integer, double[]> inData = null;

	@Description("The progress monitor.")
	@In
	public IHMProgressMonitor pm = new LogProgressMonitor();

	@Description("Include zeros in computations (default is true).")
	@In
	public boolean doIncludeZero = true;

	@Description("The range if the models runs with the gaussian variogram.")
	@In
	public double range;

	@Description("The sill if the models runs with the gaussian variogram.")
	@In
	public double sill;

	@Description("Is the nugget if the models runs with the gaussian variogram.")
	@In
	public double nugget;

	@Description("In the case of kriging with neighbor, maxdist is the maximum distance within the algorithm has to consider the stations")
	@In
	public double maxdist;

	@Description("In the case of kriging with neighbor, inNumCloserStations is the number of stations the algorithm has to consider")
	@In
	public int inNumCloserStations;

	@Description("Switch for detrended mode.")
	@In
	public boolean doDetrended;

	/** transform to log. */
	@In
	public boolean doLogarithmic = false;

	/** bounded output to 0. */
	@In
	public boolean boundedToZero = false;

	@Description("The Experimental/Theoretical Variogram.")
	@In
	public HashMap<Integer, double[]> inTheoreticalVariogram;

	@In
	public String tStart = null;

	@In
	public int tTimeStep = 60;

	@Description("Numero di thread per la parallelizzazione (default 4).")
	@In
	public int nThreads = 4;

	@Description("The hashmap with the interpolated results")
	@Out
	public HashMap<Integer, double[]> outData = null;

	// --- internal ---
	private boolean isFirstStep = true;
	private final Map<Integer, SimpleFeatureCollection> featureCollectionMap = new HashMap<>();

	/**
	 * Vista su inData che “nasconde” una chiave (excludedId) senza modificare inData.
	 * Estende HashMap perché spesso KrigingPointCase espone inData come HashMap.
	 * È read-only.
	 */
	public static final class ExcludingMap extends HashMap<Integer, double[]> {

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
		public boolean containsKey(Object key) {
			return !isExcluded(key) && delegate.containsKey(key);
		}

		@Override
		public double[] get(Object key) {
			return isExcluded(key) ? null : delegate.get(key);
		}

		@Override
		public int size() {
			return (excludedId != null && delegate.containsKey(excludedId)) ? delegate.size() - 1 : delegate.size();
		}

		@Override
		public boolean isEmpty() {
			return size() == 0;
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

		@Override
		public Set<Integer> keySet() {
			// implementazione sicura se Kriging usa keySet()
			return entrySet().stream().map(Entry::getKey).collect(Collectors.toCollection(LinkedHashSet::new));
		}

		@Override
		public Collection<double[]> values() {
			return entrySet().stream().map(Entry::getValue).collect(Collectors.toList());
		}

		// read-only: evita side effects sul delegate
		@Override
		public double[] put(Integer key, double[] value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public double[] remove(Object key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void putAll(Map<? extends Integer, ? extends double[]> m) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
	}

	@Execute
	public void executeKriging() throws Exception {

		if (inStations == null || inData == null) {
			throw new IllegalArgumentException("inStations e inData non possono essere null");
		}

		initializeFeatureCollections();

		ConcurrentHashMap<Integer, double[]> out = new ConcurrentHashMap<>();
		List<String> warnings = Collections.synchronizedList(new ArrayList<>());

		int max = Runtime.getRuntime().availableProcessors();
		int threads = Math.max(1, Math.min(nThreads, max));
		ExecutorService pool = Executors.newFixedThreadPool(threads);

		try {
			List<Callable<Void>> tasks = new ArrayList<>(featureCollectionMap.size());

			for (var e : featureCollectionMap.entrySet()) {
				final int idToCheck = e.getKey();
				final SimpleFeatureCollection fc = e.getValue();

				tasks.add(() -> {

					if (fc == null) {
						warnings.add("Warning: no feature found for station id " + idToCheck);
						return null;
					}
					if (!inData.containsKey(idToCheck)) {
						return null;
					}

					KrigingPointCase kriging = buildKrigingInstance();

					ExcludingMap exMap = new ExcludingMap(inData);
					exMap.setExcludedId(idToCheck);

					kriging.inStations = inStations;
					kriging.setProvider(null);

					kriging.inData = exMap;
					kriging.inInterpolate = fc;

					try {
						kriging.execute();
						double[] value = kriging.outData.get(idToCheck);
						if (value != null) {
							out.put(idToCheck, value);
						}
					} catch (Exception ex) {
						warnings.add("Error kriging for id " + idToCheck + ": " + ex.getMessage());
					}
					return null;
				});
			}

			pool.invokeAll(tasks);

		} finally {
			pool.shutdown();
		}

		outData = new HashMap<>(out);

		for (String w : warnings) {
			pm.message(w);
		}
		pm.done();
	}

	private void initializeFeatureCollections() {
		if (!isFirstStep) {
			return;
		}

		SimpleFeatureIterator iterator = inStations.features();
		try {
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				int id = ((Long) feature.getProperty(fStationsid).getValue()).intValue();
				featureCollectionMap.put(id, DataUtilities.collection(feature));
			}
		} finally {
			iterator.close();
		}
		isFirstStep = false;
	}

	private KrigingPointCase buildKrigingInstance() {
		KrigingPointCase kriging = new KrigingPointCase();

		kriging.boundedToZero = boundedToZero;
		kriging.maxdist = maxdist;
		kriging.inNumCloserStations = inNumCloserStations;

		kriging.pSemivariogramType = pSemivariogramType;
		kriging.nugget = nugget;
		kriging.sill = sill;
		kriging.range = range;

		kriging.tStart = tStart;
		kriging.tTimeStep = tTimeStep;

		kriging.fStationsid = fStationsid;
		kriging.fInterpolateid = fStationsid;

		kriging.doDetrended = doDetrended;
		kriging.fPointZ = fStationsZ;
		kriging.fStationsZ = fStationsZ;

		kriging.doLogarithmic = doLogarithmic;
		kriging.doIncludeZero = doIncludeZero;

		kriging.inTheoreticalVariogram = inTheoreticalVariogram;

		return kriging;
	}
}

