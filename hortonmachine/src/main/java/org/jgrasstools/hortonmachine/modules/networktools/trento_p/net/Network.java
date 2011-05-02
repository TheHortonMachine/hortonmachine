package org.jgrasstools.hortonmachine.modules.networktools.trento_p.net;

import org.jgrasstools.hortonmachine.modules.networktools.trento_p.TrentoP;

/**
 * This is a simple interface for the program TrentoP.
 * 
 * <p>
 * It is used to implement two object which is the "project" or the
 * "calibration".
 * </p>
 * 
 * @see {@link NetworkBuilder}, {@link NetworkCalibration}, {@link TrentoP}.
 * @author Daniele Andreis.
 * 
 * 
 */
public interface Network {
    /**
     * Run the model TrentoP.
     * 
     * @throws Exception
     */
    public void geoSewer() throws Exception;
}
