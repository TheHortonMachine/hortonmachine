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

import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_LICENSE;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_OMSVWT_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_OMSVWT_KEYWORDS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_OMSVWT_NAME;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_OMSVWT_STATUS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_OMSVWT_outVwt_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_SUBMODULES_LABEL;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_inAlfaVanGen_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_inNVanGen_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_inSoilThickness_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_inTheta_r_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.HortonMessages.OMSCISLAM_inTheta_s_DESCRIPTION;

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


@Description(OMSCISLAM_OMSVWT_DESCRIPTION)
@Author(name = OMSCISLAM_AUTHORNAMES, contact = OMSCISLAM_AUTHORCONTACTS)
@Keywords(OMSCISLAM_OMSVWT_KEYWORDS)
@Label(OMSCISLAM_SUBMODULES_LABEL)
@Name(OMSCISLAM_OMSVWT_NAME)
@Status(OMSCISLAM_OMSVWT_STATUS)
@License(OMSCISLAM_LICENSE)
public class OmsVwt extends JGTModel {
	
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

    @Description(OMSCISLAM_inAlfaVanGen_DESCRIPTION)
    @In
    public GridCoverage2D inAlfaVanGen = null;

    @Description(OMSCISLAM_inNVanGen_DESCRIPTION)
    @In
    public GridCoverage2D inNVanGen = null;
    
    @Description(OMSCISLAM_OMSVWT_outVwt_DESCRIPTION)
    @Out
    public GridCoverage2D outVwt = null;
	
	@Execute
	public void process(){
		if (!concatOr(outVwt == null, doReset)) {
			return;
		}
		
		checkNull(inSoilThickness, inTheta_s, inTheta_r, inAlfaVanGen, inNVanGen);
		
		RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inSoilThickness);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        // Prepare objects for iterating over the maps
        RandomIter theta_rIter = RandomIterFactory.create(inTheta_r.getRenderedImage(), null);
        RandomIter theta_sIter = RandomIterFactory.create(inTheta_s.getRenderedImage(), null);
        RandomIter soilThicknessIter = RandomIterFactory.create(inSoilThickness.getRenderedImage(), null);
        RandomIter alfaVanGenuchtenIter = RandomIterFactory.create(inAlfaVanGen.getRenderedImage(), null);
        RandomIter nVanGenuchtenIter = RandomIterFactory.create(inNVanGen.getRenderedImage(), null);
        
        WritableRaster mVwtWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, JGTConstants.doubleNovalue);
        WritableRandomIter mVwtIter = RandomIterFactory.createWritable(mVwtWR, null);
        
        double mVwt = 0.0;
        pm.beginTask("Start computing Vwt map (map of storage of soil moisture needed to produce a perched water table)...", rows);
        for(int r = 0; r < rows; r++){
        	for(int c = 0; c < cols; c++){
        		double theta_r = theta_rIter.getSampleDouble(c, r, 0);
        		double theta_s = theta_sIter.getSampleDouble(c, r, 0);
        		double soilThickness = soilThicknessIter.getSampleDouble(c, r, 0);
        		double alfaVanGenuchten = alfaVanGenuchtenIter.getSampleDouble(c, r, 0);
        		double nVanGenuchten = nVanGenuchtenIter.getSampleDouble(c, r, 0);
        		if(theta_r!=0.0 && theta_s != 0.0){        		
	        		mVwt =
	        				(theta_r*soilThickness) +
	        				(theta_s-theta_r)*
	        				(soilThickness*(
	        						Math.pow(
	        							(1 + Math.pow(
	        								(alfaVanGenuchten*soilThickness),
	        								nVanGenuchten)
	        							),
	        							(-1.0/nVanGenuchten)
	        								)
	        						)
	        				);
	        		//pm.message(Double.toString(mVwt));
        			mVwtIter.setSample(c, r, 0, mVwt);
        		}
        	}
        	pm.worked(1);
        }
        pm.done();
    	
        outVwt = CoverageUtilities.buildCoverage("Vwt", mVwtWR, regionMap, inSoilThickness.getCoordinateReferenceSystem());
    }

}
