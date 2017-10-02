/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.hmachine.modules.networktools.epanet.core;
/**
 * Interface for all epanet feature types.
 */
public interface IEpanetType {
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
     * Getter for the file name.
     * 
     * @return the file name.
     */
    public String getShapefileName();

    /**
     * Getter for the name.
     * 
     * @return the name of the type.
     */
    public String getName();

    /**
     * Getter for the type prefix.
     * 
     * @return the prefix.
     */
    public String getPrefix();
}