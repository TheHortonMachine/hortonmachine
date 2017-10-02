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

/**
 * A cluster of points.
 * 
 * @author Tom Gibara
 *
 * @param <K> the key type
 */

public class GvmCluster<S extends GvmSpace, K> {

	/**
	 * The set of clusters to which this cluster belongs
	 */
	
	final GvmClusters<S, K> clusters;
	
	/**
	 * The pairings of this cluster with all other clusters.
	 */
	
	final GvmClusterPair<S,K>[] pairs;

	/**
	 * Whether this cluster is in the process of being removed.
	 */
	
	boolean removed;
	
	/**
	 * The number of points in this cluster.
	 */
	
	int count;
	
	/**
	 * The total mass of this cluster.
	 */
	
	double m0;
	
	/**
	 * The mass-weighted coordinate sum.
	 */
	
	final Object m1;
	
	/**
	 * The mass-weighted coordinate-square sum.
	 */
	
	final Object m2;
	
	/**
	 * The computed variance of this cluster
	 */
	
	double var;
	
	/**
	 * The key associated with this cluster.
	 */
	
	K key;
	
	// constructors
	
	@SuppressWarnings("unchecked")
	GvmCluster(GvmClusters<S,K> clusters) {
		this.clusters = clusters;
		removed = false;
		count = 0;
		m0 = 0.0;
		m1 = clusters.space.newOrigin();
		m2 = clusters.space.newOrigin();
		pairs = new GvmClusterPair[clusters.capacity];
		update();
	}
	
	// public accessors
	
	/**
	 * The total mass of the cluster.
	 */

	public double getMass() {
		return m0;
	}
	
	/**
	 * The number of points in the cluster.
	 */
	
	public int getCount() {
		return count;
	}
	
	/**
	 * The computed variance of the cluster
	 */

	public double getVariance() {
		return var;
	}
	
	/**
	 * The key associated with the cluster, may be null.
	 */

	public K getKey() {
		return key;
	}

	// package methods

	/**
	 * Completely clears this cluster. All points and their associated mass is
	 * removed along with any key that was assigned to the cluster,
	 */
	
	void clear() {
		count = 0;
		m0 = 0.0;
		clusters.space.setToOrigin(m1);
		clusters.space.setToOrigin(m2);
		var = 0.0;
		key = null;
	}

	/**
	 * Sets this cluster equal to a single point.
	 * 
	 * @param m
	 *            the mass of the point
	 * @param pt
	 *            the coordinates of the point
	 */

	void set(final double m, final Object pt) {
		if (m == 0.0) {
			if (count != 0) {
				clusters.space.setToOrigin(m1);
				clusters.space.setToOrigin(m2);
			}
		} else {
			clusters.space.setToScaled(m1, m, pt);
			clusters.space.setToScaledSqr(m2, m, pt);
		}
		count = 1;
		m0 = m;
		var = 0.0;
	}
	
	/**
	 * Adds a point to the cluster.
	 * 
	 * @param m
	 *            the mass of the point
	 * @param pt
	 *            the coordinates of the point
	 */
	
	void add(final double m, final Object pt) {
		if (count == 0) {
			set(m, pt);
		} else {
			count += 1;
			
			if (m != 0.0) {
				m0 += m;
				clusters.space.addScaled(m1, m, pt);
				clusters.space.addScaledSqr(m2, m, pt);
				update();
			}
		}
	}
	
	/**
	 * Sets this cluster equal to the specified cluster
	 * 
	 * @param cluster a cluster, not this or null
	 */
	
	void set(GvmCluster<S,K> cluster) {
		if (cluster == this) throw new IllegalArgumentException("cannot set cluster to itself");
		
		m0 = cluster.m0;
		clusters.space.setTo(m1, cluster.m1);
		clusters.space.setTo(m2, cluster.m2);
		var = cluster.var;
	}
	
	/**
	 * Adds the specified cluster to this cluster.
	 * 
	 * @param cluster the cluster to be added
	 */
	
	void add(GvmCluster<S,K> cluster) {
		if (cluster == this) throw new IllegalArgumentException();
		if (cluster.count == 0) return; //nothing to do
		
		if (count == 0) {
			set(cluster);
		} else {
			count += cluster.count;
			//TODO accelerate add
			m0 += cluster.m0;
            clusters.space.add(m1, cluster.m1);
            clusters.space.add(m2, cluster.m2);
			update();
		}
	}
	
	/**
	 * Computes this clusters variance if it were to have a new point added to it.
	 * 
	 * @param m the mass of the point
	 * @param pt the coordinates of the point
	 * @return the variance of this cluster inclusive of the point
	 */
	
	double test(double m, Object pt) {
		return m0 == 0.0 && m == 0.0 ? 0.0 : clusters.space.variance(m0, m1, m2, m, pt) - var;
	}

	/**
	 * Computes the variance of a cluster that aggregated this cluster with the
	 * supplied cluster.
	 * 
	 * @param cluster
	 *            another cluster
	 * @return the combined variance
	 */

	//TODO: change for consistency with other test method : return increase in variance
	double test(GvmCluster<S,K> cluster) {
		return m0 == 0.0 && cluster.m0 == 0.0 ? 0.0 : clusters.space.variance(m0, m1, m2, cluster.m0, cluster.m1, cluster.m2);
	}

	// private utility methods
	
	/**
	 * Recompute this cluster's variance.
	 */
	
	private void update() {
		var = m0 == 0.0 ? 0.0 : clusters.space.variance(m0, m1, m2);
	}
	
}
