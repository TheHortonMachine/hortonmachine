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

import java.util.List;

/**
 * Allows multiple keys to be associated with clusters in the form of a lists
 * which may be concatenated when clusters merge.
 * 
 * @author Tom Gibara
 * 
 * @param <K>
 *            type of key
 */

//TODO if hierachial clustering is supported, lists will need to be treated as unmodifiable
public class GvmListKeyer<K> extends GvmSimpleKeyer<List<K>> {

	protected List<K> combineKeys(List<K> list1, List<K> list2) {
		list1.addAll(list2);
		return list1;
	}
	
}
