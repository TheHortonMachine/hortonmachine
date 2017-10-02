/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.nww.layers.defaults;

import org.hortonmachine.nww.shapes.FeatureStoreInfo;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Common interface for editable layers.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public interface NwwEditableVectorLayer extends NwwVectorLayer {

    
    public boolean isEditable();

    public FeatureStoreInfo getStoreInfo();
    
    public void add(SimpleFeature feature);

    public void reload();
}
