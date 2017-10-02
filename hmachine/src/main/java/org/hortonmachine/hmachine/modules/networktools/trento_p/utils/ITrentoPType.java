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
package org.hortonmachine.hmachine.modules.networktools.trento_p.utils;

import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Interface used to implement a class which is used to create  SimpleFeatureType.
 * 
 * 
 * @see {@link SimpleFeatureType}
 * @author daniele andreis
 * 
 */
public interface ITrentoPType {
    /**
     * Getter for the binding class.
     * 
     * @return the binding class.
     */
    public Class< ? > getClazz();

    /**
     * Getter for the attribute name.
     * 
     * @return the attribute name.
     */
    public String getAttributeName();

    /**
     * Getter for the name.
     * 
     * @return the name of the type.
     */
    public String getName();

}
