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
 * A pair of clusters, each element of which is distinct.
 * 
 * @author Tom Gibara
 *
 * @param <K> the key type
 */

class GvmClusterPair<S extends GvmSpace, K> {

	/**
	 * The first cluster in this collection.
	 */
	
	final GvmCluster<S, K> c1;
	
	/**
	 * The second cluster in this collection.
	 */
	
	final GvmCluster<S, K> c2;
	
	/**
	 * The index of this pair within a heap of pairs.
	 */
	
	int index;
	
	/**
	 * The amount the global variance would increase if this pair was merged.
	 */
	
	double value;
	
	/**
	 * Constructs a new pair and computes its value.
	 * 
	 * @param c1 a cluster, not equal to c2
	 * @param c2 a cluster, not equal to c1
	 */
	
	GvmClusterPair(GvmCluster<S,K> c1, GvmCluster<S,K> c2) {
		if (c1 == c2) throw new IllegalArgumentException();
		this.c1 = c1;
		this.c2 = c2;
		update();
	}

	// object methods
	
	// package methods
	
	/**
	 * Updates the value of the pair.
	 */
	
	void update() {
		value = c1.test(c2) - c1.var - c2.var;
	}
	
}
