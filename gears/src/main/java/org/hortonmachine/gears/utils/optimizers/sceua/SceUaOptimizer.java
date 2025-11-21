package org.hortonmachine.gears.utils.optimizers.sceua;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Implementation of the SCE-UA (Shuffled Complex Evolution) global optimization
 * algorithm by Duan, Sorooshian & Gupta (1992), adapted for single-objective
 * minimization.
 *
 * Typical usage with a direct objective:
 *
 * <pre>
 * SceUaOptimizer optimizer = new SceUaOptimizer(bounds, params -&gt; myObjective(params), SceUaConfig.builder().build());
 * SceUaResult result = optimizer.optimize();
 * </pre>
 *
 * Typical usage with a simulation + error function (e.g. KGE):
 *
 * <pre>
 * SimulationFunction sim = params -&gt; runModel(params);
 * ErrorFunction err = (simulated, observed) -&gt; KGE.kgeCost(simulated, observed);
 * ObjectiveFunction obj = params -&gt; err.evaluate(sim.simulate(params), observedQ);
 *
 * SceUaOptimizer optimizer = new SceUaOptimizer(bounds, obj, config);
 * </pre>
 */
public final class SceUaOptimizer {

	/**
	 * Objective function: maps a parameter vector to a scalar cost to MINIMIZE.
	 */
	@FunctionalInterface
	public interface ObjectiveFunction {
		double evaluate(double[] params) throws Exception;
	}

	private final ParameterBounds[] bounds;
	private final ObjectiveFunction objective;
	private final SceUaConfig config;

	// Internal settings (can be tuned if desired)
	private final int populationSize; // npt in SCE-UA
	private final int pointsInComplex; // number of points per complex
	private final int simplexSize; // number of points in simplex (q)
	private final int evolutionStepsPerComplex; // number of evolution steps per reshuffle

	/**
	 * Creates a new SCE-UA optimizer.
	 *
	 * @param bounds    array of parameter bounds (one per parameter)
	 * @param objective objective function to MINIMIZE
	 * @param config    algorithm configuration
	 */
	public SceUaOptimizer(List<ParameterBounds> bounds, ObjectiveFunction objective, SceUaConfig config) {
		this(bounds.toArray(new ParameterBounds[0]), objective, config);
	}

	public SceUaOptimizer(ParameterBounds[] bounds, ObjectiveFunction objective, SceUaConfig config) {
		if (bounds == null || bounds.length == 0) {
			throw new IllegalArgumentException("At least one parameter bound is required.");
		}
		if (objective == null) {
			throw new IllegalArgumentException("Objective function must not be null.");
		}
		if (config == null) {
			throw new IllegalArgumentException("Config must not be null.");
		}
		this.bounds = bounds.clone();
		this.objective = objective;
		this.config = config;

		int n = bounds.length;

		// Standard SCE-UA defaults:
		// npt = 2 * n + 1, but ensure enough points per complex.
		int npt = Math.max(2 * n + 1, config.getComplexCount() * (n + 1));
		this.populationSize = npt;

		this.pointsInComplex = npt / config.getComplexCount();
		this.simplexSize = n + 1;
		this.evolutionStepsPerComplex = 2 * n + 1;
	}
	
	/**
	 * Runs the optimization in single-threaded mode.
	 *
	 * @return SceUaResult with best parameters and objective value found.
	 */
	public SceUaResult optimize() {
	    return runOptimization(false, 1);
	}

	/**
	 * Runs the optimization in parallel across complexes.
	 *
	 * @param threadCount number of threads to use (>=1). If 1, behaves like optimize().
	 * @return SceUaResult with best parameters and objective value found.
	 */
	public SceUaResult optimizeParallel(int threadCount) {
	    if (threadCount < 1) {
	        throw new IllegalArgumentException("threadCount must be >= 1");
	    }
	    boolean parallel = threadCount > 1;
	    return runOptimization(parallel, threadCount);
	}

	private SceUaResult runOptimization(boolean parallel, int threadCount) {
	    Random rnd = config.getRandom();
	    int n = bounds.length;
	    int m = config.getComplexCount();
	    int npt = populationSize;
	    int ncs = pointsInComplex;

	    // Population: npt individuals of length n
	    double[][] pop = new double[npt][n];
	    double[] obj = new double[npt];

	    int evalCount = 0;

	    try {
	    	// 1) Initialize population uniformly within bounds
	    	for (int i = 0; i < npt; i++) {
	    	    for (int j = 0; j < n; j++) {
	    	        double u = rnd.nextDouble();
	    	        pop[i][j] = bounds[j].denormalize(u);
	    	    }
	    	}

	    	// 1b) Evaluate initial population (single-threaded or parallel)
	    	if (!parallel || threadCount == 1) {
	    	    for (int i = 0; i < npt; i++) {
	    	        obj[i] = objective.evaluate(pop[i]);
	    	        evalCount++;
	    	    }
	    	} else {
	    	    java.util.concurrent.ExecutorService executor =
	    	            java.util.concurrent.Executors.newFixedThreadPool(threadCount);
	    	    try {
	    	        java.util.List<java.util.concurrent.Future<Void>> futures =
	    	                new java.util.ArrayList<>(npt);
	    	        for (int i = 0; i < npt; i++) {
	    	            final int idx = i;
	    	            futures.add(executor.submit(() -> {
	    	                obj[idx] = objective.evaluate(pop[idx]);
	    	                return null;
	    	            }));
	    	        }
	    	        for (java.util.concurrent.Future<Void> f : futures) {
	    	            f.get();   // propagate any exceptions
	    	        }
	    	        evalCount += npt;  // exactly npt evaluations
	    	    } catch (java.util.concurrent.ExecutionException e) {
	    	        throw new RuntimeException("Error during initial population evaluation", e.getCause());
	    	    } catch (InterruptedException e) {
	    	        Thread.currentThread().interrupt();
	    	        throw new RuntimeException("Interrupted during initial population evaluation", e);
	    	    } finally {
	    	        executor.shutdown();
	    	    }
	    	}


	        // Sort by objective
	        sortPopulation(pop, obj);

	        int iter = 0;
	        while (iter < config.getMaxIterations() && evalCount < config.getMaxEvaluations()) {

	            if (config.isVerbose() && iter % 10 == 0) {
	                System.out.printf("SCE-UA iter=%d, best=%.6f%n", iter, obj[0]);
	            }

	            // Check convergence based on objective standard deviation
	            double stdObj = std(obj);
	            if (stdObj < config.getObjectiveStdTolerance()) {
	                if (config.isVerbose()) {
	                    System.out.printf("Converged: std(obj)=%.3e%n", stdObj);
	                }
	                break;
	            }

	            // 2) Partition population into complexes
	            // complexes[c][i] = index in pop of i-th member of complex c
	            int[][] complexes = new int[m][ncs];
	            for (int c = 0; c < m; c++) {
	                for (int i = 0; i < ncs; i++) {
	                    int idx = c + i * m;
	                    if (idx >= npt) {
	                        idx = npt - 1; // safety
	                    }
	                    complexes[c][i] = idx;
	                }
	            }

	            // 3) Evolve each complex (single-threaded or parallel)
	            if (!parallel || threadCount == 1) {
	                // --- single-threaded (original behaviour) ---
	                for (int c = 0; c < m; c++) {
	                    for (int step = 0; step < evolutionStepsPerComplex; step++) {
	                        if (evalCount >= config.getMaxEvaluations()) {
	                            break;
	                        }
	                        evalCount += evolveComplex(pop, obj, complexes[c], rnd);
	                    }
	                }
	            } else {
	                // --- parallel across complexes ---
	                ExecutorService executor = Executors.newFixedThreadPool(threadCount);
	                try {
	                    // Per-complex RNGs to avoid sharing Random across threads
	                    Random[] complexRands = new Random[m];
	                    for (int c = 0; c < m; c++) {
	                        complexRands[c] = new Random(rnd.nextLong());
	                    }

	                    // Submit one task per complex
	                    java.util.List<Future<Integer>> futures = new java.util.ArrayList<>(m);
	                    for (int c = 0; c < m; c++) {
	                        final int complexIndex = c;
	                        Callable<Integer> task = () -> {
	                            int localEval = 0;
	                            Random localRnd = complexRands[complexIndex];
	                            for (int step = 0; step < evolutionStepsPerComplex; step++) {
	                                // NOTE: we don't enforce maxEvaluations inside each task;
	                                // stopping is controlled at outer loop level.
	                                localEval += evolveComplex(pop, obj, complexes[complexIndex], localRnd);
	                            }
	                            return localEval;
	                        };
	                            futures.add(executor.submit(task));
	                        }

	                        for (Future<Integer> f : futures) {
	                            try {
	                                evalCount += f.get();
	                            } catch (ExecutionException e) {
	                                throw new RuntimeException("Error in SCE-UA complex evolution", e.getCause());
	                            }
	                        }

	                    } finally {
	                        executor.shutdown();
	                    }
	                    // We may slightly overshoot maxEvaluations in this iteration,
	                    // but the outer while-loop will stop as soon as evalCount >= maxEvaluations.
	                }

	            // 4) Shuffle: recombine complexes (they already operate on pop)
	            // Re-sort the population
	            sortPopulation(pop, obj);

	            iter++;
	        }

	        // Return best solution
	        return new SceUaResult(pop[0].clone(), obj[0], iter, evalCount);

	    } catch (Exception e) {
	        throw new RuntimeException("Error during SCE-UA optimization", e);
	    }
	}


	/**
	 * Evolves a single complex for one step using a simplex-like move with
	 * probabilistic selection that favors better points.
	 *
	 * @return number of objective evaluations performed (1 or 2)
	 */
	private int evolveComplex(double[][] pop, double[] obj, int[] complex, Random rnd) throws Exception {

	    int n = bounds.length;
	    int ncs = complex.length;
	    int evaluations = 0;

	    // Build local complex arrays (sorted by objective)
	    double[][] cPop = new double[ncs][n];
	    double[] cObj = new double[ncs];
	    for (int i = 0; i < ncs; i++) {
	        int idx = complex[i];
	        System.arraycopy(pop[idx], 0, cPop[i], 0, n);
	        cObj[i] = obj[idx];
	    }

	    // Sort local complex
	    sortPopulation(cPop, cObj);

	    // Select simplex points (q = n+1) with triangular probability favoring best
	    int q = simplexSize;
	    if (q > ncs) {
	        q = ncs;
	    }
	    int[] simplexIdx = sampleWithTriangularProb(ncs, q, rnd);

	    // Build simplex arrays
	    double[][] simplex = new double[q][n];
	    double[] simplexObj = new double[q];
	    for (int i = 0; i < q; i++) {
	        int idx = simplexIdx[i];
	        System.arraycopy(cPop[idx], 0, simplex[i], 0, n);
	        simplexObj[i] = cObj[idx];
	    }

	    // Sort simplex so simplex[0] is best, simplex[q-1] is worst
	    sortPopulation(simplex, simplexObj);

	    double[] best = simplex[0];
	    double[] worst = simplex[q - 1];
	    double worstObj = simplexObj[q - 1];

	    // Compute centroid of all points except worst
	    double[] centroid = new double[n];
	    Arrays.fill(centroid, 0.0);
	    for (int i = 0; i < q - 1; i++) {
	        for (int j = 0; j < n; j++) {
	            centroid[j] += simplex[i][j];
	        }
	    }
	    for (int j = 0; j < n; j++) {
	        centroid[j] /= (q - 1);
	    }

	    // Reflection step
	    double alpha = 1.0; // reflection coefficient
	    double[] reflected = new double[n];
	    for (int j = 0; j < n; j++) {
	        reflected[j] = centroid[j] + alpha * (centroid[j] - worst[j]);
	        reflected[j] = clamp(reflected[j], bounds[j].getLower(), bounds[j].getUpper());
	    }

	    double reflectedObj = objective.evaluate(reflected);
	    evaluations++;

	    // If reflection improves the worst, accept it
	    if (reflectedObj < worstObj) {
	        worst = reflected;
	        worstObj = reflectedObj;
	    } else {
	        // Otherwise, random point near best
	        double beta = 0.5; // spread factor
	        for (int j = 0; j < n; j++) {
	            double u = (rnd.nextDouble() - 0.5) * 2.0; // [-1,1]
	            double span = (bounds[j].getUpper() - bounds[j].getLower());
	            reflected[j] = best[j] + beta * u * span;
	            reflected[j] = clamp(reflected[j], bounds[j].getLower(), bounds[j].getUpper());
	        }
	        reflectedObj = objective.evaluate(reflected);
	        evaluations++;
	        worst = reflected;
	        worstObj = reflectedObj;
	    }

	    // Replace the worst point in the local complex
	    cPop[ncs - 1] = worst;
	    cObj[ncs - 1] = worstObj;
	    sortPopulation(cPop, cObj);

	    // Write back into global population
	    for (int i = 0; i < ncs; i++) {
	        int idx = complex[i];
	        System.arraycopy(cPop[i], 0, pop[idx], 0, n);
	        obj[idx] = cObj[i];
	    }

	    return evaluations;
	}


	private static double clamp(double v, double lo, double hi) {
		return (v < lo) ? lo : (v > hi ? hi : v);
	}

	private static void sortPopulation(double[][] pop, double[] obj) {
		Integer[] idx = new Integer[obj.length];
		for (int i = 0; i < obj.length; i++) {
			idx[i] = i;
		}
		Arrays.sort(idx, Comparator.comparingDouble(i -> obj[i]));
		double[][] popCopy = new double[pop.length][];
		double[] objCopy = new double[obj.length];
		for (int i = 0; i < idx.length; i++) {
			popCopy[i] = pop[idx[i]];
			objCopy[i] = obj[idx[i]];
		}
		System.arraycopy(popCopy, 0, pop, 0, pop.length);
		System.arraycopy(objCopy, 0, obj, 0, obj.length);
	}

	private static double std(double[] values) {
		int n = values.length;
		if (n <= 1)
			return 0.0;
		double mean = 0.0;
		for (double v : values)
			mean += v;
		mean /= n;
		double var = 0.0;
		for (double v : values) {
			double d = v - mean;
			var += d * d;
		}
		var /= (n - 1);
		return Math.sqrt(var);
	}

	/**
	 * Samples k indices from [0, n-1] with triangular probability that favors
	 * smaller indices (better points), as recommended in SCE-UA.
	 */
	private static int[] sampleWithTriangularProb(int n, int k, Random rnd) {
		int[] result = new int[k];
		for (int s = 0; s < k; s++) {
			// triangular distribution over {0..n-1}
			double u = rnd.nextDouble();
			double sum = 0.0;
			for (int i = 0; i < n; i++) {
				// probability ~ (n - i) (simple decreasing weights)
				double weight = (double) (n - i);
				sum += weight;
			}
			double target = u * sum;
			double cum = 0.0;
			for (int i = 0; i < n; i++) {
				double weight = (double) (n - i);
				cum += weight;
				if (cum >= target) {
					result[s] = i;
					break;
				}
			}
		}
		return result;
	}
}
