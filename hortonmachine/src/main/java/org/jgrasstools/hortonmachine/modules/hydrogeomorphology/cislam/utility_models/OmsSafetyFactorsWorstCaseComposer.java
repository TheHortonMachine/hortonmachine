package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utility_models;

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
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utils.MapCalculationFunctions;


@Description(OMSCISLAM_OMSSAFETYFACTORCOMPOSER_DESCRIPTION)
@Author(name = OMSCISLAM_AUTHORNAMES, contact = OMSCISLAM_AUTHORCONTACTS)
@Keywords(OMSCISLAM_OMSSAFETYFACTORCOMPOSER_KEYWORDS)
@Label(OMSCISLAM_SUBMODULES_LABEL)
@Name(OMSCISLAM_OMSSAFETYFACTORCOMPOSER_NAME)
@Status(OMSCISLAM_OMSSAFETYFACTORCOMPOSER_STATUS)
@License(OMSCISLAM_LICENSE)
public class OmsSafetyFactorsWorstCaseComposer extends JGTModel {

    @Description(OMSCISLAM_OMSSAFETYFACTORCOMPOSER_inRasters_DESCRIPTION)
    @In
    public List<GridCoverage2D> inRasters;
    
    @Description(OMSCISLAM_OMSSAFETYFACTORCOMPOSER_pReturnTime_DESCRIPTION)
    @In
    public int pReturnTime;
    
    @Description(OMSCISLAM_OMSSAFETYFACTORCOMPOSER_outSafetyFactorTotal_DESCRIPTION)
    @Out
    public GridCoverage2D outSafetyFactorTotal;
    
    @Execute
    public void process(){
        
        GridCoverage2D[] inRastersArray = new GridCoverage2D[inRasters.size()];
    	
    	// gather maps
        int i = 0;
        for( GridCoverage2D mapGC : inRasters ) {
            inRastersArray[i] = mapGC;
            i++;
        }
        
        outSafetyFactorTotal = MapCalculationFunctions.computeSafetyFactorTOTAL(inRastersArray, pReturnTime, pm);
    	
    }    

}