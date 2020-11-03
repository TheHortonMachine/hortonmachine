/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org (C) HydroloGIS -
 * www.hydrologis.com
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
package org.hortonmachine.hmachine.modules.networktools.trento_p.net;


/**
 * This is a simple interface for the program OmsTrentoP.
 * 
 * <p>
 * It is used to implement two object which is the "project" or the
 * "calibration".
 * </p>
 * 
 * @see {@link NetworkBuilder}, {@link NetworkCalibration}, {@link OmsTrentoP}.
 * @author Daniele Andreis.
 * 
 * 
 */
public interface Network {
    /**
     * Run the model.
     * 
     * @throws Exception
     */
    public void geoSewer() throws Exception;
}
