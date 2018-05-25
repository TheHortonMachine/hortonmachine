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
 * Controls how new keys are added to clusters and how keys are combined when clusters merge.
 * Any number of strategies are possible including:
 * 
 * some applications may simply discard all keys (if the salient information is simply the cluster sizes),
 * some applications may choose the key of the largest cluster (if the salient information is an exemplar for each cluster),
 * and some applications may choose to maintain a list of keys (if every key needs to be mapped back to a cluster). 
 * 
 * @author Tom Gibara
 *
 * @param <K>
 */

public interface GvmKeyer<K> {

	/**
	 * Called when two clusters are being merged. One key needs to be
	 * chosen/synthesized from those of the clusters being merged.
	 * 
	 * @param c1
	 *            the cluster with the greater mass
	 * @param c2
	 *            the cluster with the lesser mass
	 * @return a key for the cluster that combines those of c1 and c2, may be
	 *         null
	 */
	K mergeKeys(GvmCluster<?,K> c1, GvmCluster<?,K> c2);

	/**
	 * Called when a key is being added to a cluster.
	 * 
	 * @param cluster
	 *            a cluster
	 * @param key
	 *            the key for a newly clustered coordinate
	 * @return the key to be assigned to the new cluster, may be null
	 */
	K addKey(GvmCluster<?,K> cluster, K key);

}
