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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.adige;

import java.io.IOException;
import java.util.HashMap;

import org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IDischargeContributor;
import org.joda.time.DateTime;

/**
 * Interface for models that can be used inside the {@link OmsAdige} framework.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
public interface IAdigeEngine {

    /**
     * Add an object that contributes to the discharge in a section. 
     * 
     * @param dischargeContributor the {@link IDischargeContributor}.
     */
    public void addDischargeContributor( IDischargeContributor dischargeContributor );

    /**
     * Calculate the solution for the current timestep.
     * 
     * @param currentTimstamp the current time and date.
     * @param modelTimestepInMinutes the timestep used in the model.
     * @param internalTimestepInMinutes the internal subtimestep used for computation.
     * @param previousSolution the solution of the previous timestep, used to calculate the current solution.
     * @param rainArray the array of rain data per basin.
     * @param etpArray the array of etp data per basin.
     * @return the current calculated solution.
     * @throws IOException 
     */
    public double[] solve( DateTime currentTimstamp, int modelTimestepInMinutes, double internalTimestepInMinutes,
            double[] previousSolution, double[] rainArray, double[] etpArray ) throws IOException;

    /**
     * Getter for the discharge calculated for the current times.
     * 
     * @return the discharge.
     */
    public HashMap<Integer, double[]> getDischarge();

    /**
     * Getter for the subsuperficial discharge calculated for the current times.
     * 
     * @return the discharge.
     */
    public HashMap<Integer, double[]> getSubDischarge();
}
