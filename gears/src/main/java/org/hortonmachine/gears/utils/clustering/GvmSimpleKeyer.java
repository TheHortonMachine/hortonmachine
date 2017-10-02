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
 * A convenience class that reduces the task of choosing a key for a cluster to
 * that of choosing-between/combining two non-null keys.
 * 
 * @author Tom Gibara
 * 
 * @param <K>
 *            the key type
 */

public abstract class GvmSimpleKeyer<K> implements GvmKeyer<K> {

	@Override
	public K mergeKeys(GvmCluster<?,K> c1, GvmCluster<?,K> c2) {
		K k1 = c1.getKey();
		K k2 = c2.getKey();
		if (k1 == null) return k2;
		if (k2 == null) return k1;
		return combineKeys(k1, k2);
	}
	
	@Override
	public K addKey(GvmCluster<?,K> cluster, K k2) {
		K k1 = cluster.getKey();
		if (k1 == null) return k2;
		if (k2 == null) return k1;
		return combineKeys(k1, k2);
	}
	
	/**
	 * Combines two keys. Combining two keys may totally discard information
	 * from one, both or none of the supplied keys.
	 * 
	 * @param k1
	 *            a key, not null
	 * @param k2
	 *            a key, not null
	 * 
	 * @return a combined key
	 */
	protected abstract K combineKeys(K k1, K k2);
	
}
