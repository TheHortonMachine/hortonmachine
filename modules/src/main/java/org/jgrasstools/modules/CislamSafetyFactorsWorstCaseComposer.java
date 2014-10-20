/*
 * This file is part of the "CI-slam module": an addition to JGrassTools
 * It has been contributed by Marco Foi (www.mcfoi.it) and Cristiano Lanni
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
package org.jgrasstools.modules;

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSSAFETYFACTORCOMPOSER_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSSAFETYFACTORCOMPOSER_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSSAFETYFACTORCOMPOSER_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSSAFETYFACTORCOMPOSER_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSSAFETYFACTORCOMPOSER_inRasters_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSSAFETYFACTORCOMPOSER_outSafetyFactorTotal_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSSAFETYFACTORCOMPOSER_pReturnTime_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_SUBMODULES_LABEL;

import java.util.ArrayList;
import java.util.List;

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

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utility_models.OmsSafetyFactorGeomechanic;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utility_models.OmsSafetyFactorsWorstCaseComposer;


@Description(OMSCISLAM_OMSSAFETYFACTORCOMPOSER_DESCRIPTION)
@Author(name = OMSCISLAM_AUTHORNAMES, contact = OMSCISLAM_AUTHORCONTACTS)
@Keywords(OMSCISLAM_OMSSAFETYFACTORCOMPOSER_KEYWORDS)
@Label(OMSCISLAM_SUBMODULES_LABEL)
@Name(OMSCISLAM_OMSSAFETYFACTORCOMPOSER_NAME)
@Status(OMSCISLAM_OMSSAFETYFACTORCOMPOSER_STATUS)
@License(OMSCISLAM_LICENSE)
public class CislamSafetyFactorsWorstCaseComposer extends JGTModel {

    @Description(OMSCISLAM_OMSSAFETYFACTORCOMPOSER_inRasters_DESCRIPTION)
    @In
    public List<String> inRasters;
    
    @Description(OMSCISLAM_OMSSAFETYFACTORCOMPOSER_pReturnTime_DESCRIPTION)
    @In
    public int pReturnTime;
    
    @Description(OMSCISLAM_OMSSAFETYFACTORCOMPOSER_outSafetyFactorTotal_DESCRIPTION)
    @Out
    public String outSafetyFactorTotal;
    
    @Execute
    public void process() throws Exception {

    	OmsSafetyFactorsWorstCaseComposer omsModel = new OmsSafetyFactorsWorstCaseComposer();
    	
    	List<GridCoverage2D> rasterList = new ArrayList<GridCoverage2D>();
    	for (String inRaster : inRasters){
    		rasterList.add(getRaster(inRaster));
    	}    	
		omsModel.inRasters = rasterList;
		omsModel.pReturnTime = pReturnTime;
		omsModel.process();
    	dumpRaster(omsModel.outSafetyFactorTotal, outSafetyFactorTotal);    	
    }
}