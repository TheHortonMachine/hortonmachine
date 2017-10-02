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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core;

import java.util.HashMap;

/**
 * Interface for all those objects that can supply a discharge in a given point of the network.
 * 
 * <p>The point of the network is defined by its pfafstetter number.</p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface IDischargeContributor {

    /**
     * Returns the discharge for the given location of the network.
     * 
     * @param pfafstetterNumber the number identifying the network position.
     * @return the discharge for the given network point. Double.NaN has to be 
     *                      returned for invalid values.
     */
    public abstract Double getDischarge( String pfafstetterNumber );

    /**
     * Returns the discarge of the contributor merged with the main discharge.
     * 
     * <p>This is useful because the contributor knows if it has to
     * sumor subtract.
     * 
     * @param contributorDischarge the discharge provided by the contributor itself
     *                  (taken from the result of {@link #getDischarge(String)}.
     * @param inputDischarge the input discharge in the contributing point. May 
     *                      be useful to calculate the output discharge. 
     * @return the merged discharge.
     */
    public abstract double mergeWithDischarge(double contributorDischarge, double inputDischarge );

    public abstract void setCurrentData( HashMap<Integer, double[]> currentDataMap );

}