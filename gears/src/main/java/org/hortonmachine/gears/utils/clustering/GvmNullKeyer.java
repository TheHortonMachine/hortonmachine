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
 * Simply discards all keys.
 * 
 * @author Tom Gibara
 *
 * @param <K> the key type
 */

public class GvmNullKeyer<K> implements GvmKeyer<K> {

	@Override
	public K addKey(GvmCluster<?,K> cluster, K key) {
		return null;
	}
	
	@Override
	public K mergeKeys(GvmCluster<?,K> c1, GvmCluster<?,K> c2) {
		return null;
	}
	
}
