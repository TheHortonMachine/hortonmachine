/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hecras;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;

/**
 * Common methods for sections extractors.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface HecrasSectionsExtractor {

    /**
     * Get the list of network points (with and without associated section).
     * 
     * @return the list of network points.
     */
    public abstract List<NetworkPoint> getOrderedNetworkPoints();

    /**
     * Get the number of sections on the river.
     * 
     * @return the number of sections.
     */
    public abstract int getSectionsNum();

    /**
     * Getter for the collection of section points.
     * 
     * @return the collection of section points.
     */
    public abstract SimpleFeatureCollection getSectionPointsCollection();

    /**
     * Getter for the collection of sections.
     * 
     * @return the collection of sections.
     */
    public abstract SimpleFeatureCollection getSectionsCollection();

}