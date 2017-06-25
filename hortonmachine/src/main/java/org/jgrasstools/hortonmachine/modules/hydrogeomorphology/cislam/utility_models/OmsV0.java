/*
 * This file is part of the "CI-slam module": an addition to JGrassTools
 * It has been entirely contributed by Marco Foi (www.mcfoi.it)
 * 
 * "CI-slam module" is free software: you can redistribute it and/or modify
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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utility_models;

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSV0_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_SUBMODULES_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSV0_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSV0_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSV0_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inAlfaVanGen_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inNVanGen_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inSoilThickness_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSV0_inPsiInitAtBedrock_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSV0_outV0_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inTheta_r_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inTheta_s_DESCRIPTION;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.Unit;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description(OMSCISLAM_OMSV0_DESCRIPTION)
@Author(name = OMSCISLAM_AUTHORNAMES, contact = OMSCISLAM_AUTHORCONTACTS)
@Keywords(OMSCISLAM_OMSV0_KEYWORDS)
@Label(OMSCISLAM_SUBMODULES_LABEL)
@Name(OMSCISLAM_OMSV0_NAME)
@Status(OMSCISLAM_OMSV0_STATUS)
@License(OMSCISLAM_LICENSE)
public class OmsV0 extends JGTModel {
	@Description(OMSCISLAM_inSoilThickness_DESCRIPTION)
	@Unit("m")
	@In
	public GridCoverage2D inSoilThickness = null;
	
	@Description(OMSCISLAM_inTheta_s_DESCRIPTION)
    @In
    public GridCoverage2D inTheta_s = null;

    @Description(OMSCISLAM_inTheta_r_DESCRIPTION)
    @In
    public GridCoverage2D inTheta_r = null;

	@Description(OMSCISLAM_OMSV0_inPsiInitAtBedrock_DESCRIPTION)
	@In
	public GridCoverage2D inPsiInitAtBedrock = null;

	@Description(OMSCISLAM_inAlfaVanGen_DESCRIPTION)
	@In
	public GridCoverage2D inAlfaVanGen = null;

	@Description(OMSCISLAM_inNVanGen_DESCRIPTION)
	@In
	public GridCoverage2D inNVanGen = null;

	@Description(OMSCISLAM_OMSV0_outV0_DESCRIPTION)
	@Out
	public GridCoverage2D outV0 = null;

	@Execute
	public void process() {
		if (!concatOr(outV0 == null, doReset)) {
			return;
		}
		checkNull(inSoilThickness, inTheta_r, inTheta_s, inPsiInitAtBedrock, inAlfaVanGen, inNVanGen);

		RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inSoilThickness);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        // Prepare objects for iterating over the maps
        RandomIter theta_rIter = RandomIterFactory.create(inTheta_r.getRenderedImage(), null);
        RandomIter theta_sIter = RandomIterFactory.create(inTheta_s.getRenderedImage(), null);
        RandomIter psiInitAtBedrockIter = RandomIterFactory.create(inPsiInitAtBedrock.getRenderedImage(), null);
        RandomIter soilThicknessIter = RandomIterFactory.create(inSoilThickness.getRenderedImage(), null);
        RandomIter alfaVanGenuchtenIter = RandomIterFactory.create(inAlfaVanGen.getRenderedImage(), null);
        RandomIter nVanGenuchtenIter = RandomIterFactory.create(inNVanGen.getRenderedImage(), null);
        
        WritableRaster mV0WR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, JGTConstants.doubleNovalue);
        WritableRandomIter mV0Iter = RandomIterFactory.createWritable(mV0WR, null);
        
        double mV0 = 0.0;
        pm.beginTask("Start computing V0 map (map of initial storage of soil moisture)...", rows);
        for(int r = 0; r < rows; r++){
        	for(int c = 0; c < cols; c++){
        		double theta_r = theta_rIter.getSampleDouble(c, r, 0);
        		double theta_s = theta_sIter.getSampleDouble(c, r, 0);
        		double psi_i = psiInitAtBedrockIter.getSampleDouble(c, r, 0);
        		double soilThickness = soilThicknessIter.getSampleDouble(c, r, 0);
        		double alfaVanGenuchten = alfaVanGenuchtenIter.getSampleDouble(c, r, 0);
        		double nVanGenuchten = nVanGenuchtenIter.getSampleDouble(c, r, 0);
        		if(!Double.isNaN(soilThickness)){
        			
        		    mV0 = calculateNodeInitialSoilMoisture_V0(soilThickness, theta_s, theta_r, psi_i, alfaVanGenuchten, nVanGenuchten);
        					
	        		//pm.message(Double.toString(mVwt));
        			mV0Iter.setSample(c, r, 0, mV0);
        		}
        	}
        	pm.worked(1);
        }
    	pm.done();
    	
        outV0 = CoverageUtilities.buildCoverage("V0", mV0WR, regionMap, inSoilThickness.getCoordinateReferenceSystem());
        
	}
	
	public static double calculateNodeInitialSoilMoisture_V0(double soilThickness, double theta_s, double theta_r, double psi_i, double alfaVanGenuchten, double nVanGenuchten){
	    double mV0 =   (theta_r*soilThickness)+
                (theta_s - theta_r)*
                (
                (soilThickness+psi_i)*
                Math.pow(1+
                        Math.pow(
                                (alfaVanGenuchten*(soilThickness+psi_i)),
                                (nVanGenuchten)
                        ),
                        (-1.0/nVanGenuchten)
                        )
                -
                psi_i*
                Math.pow(1+
                        Math.pow(
                                (alfaVanGenuchten*psi_i),
                                (nVanGenuchten)
                                ),
                        (-1.0/nVanGenuchten)
                        )
                );
        return mV0;
	}
}
