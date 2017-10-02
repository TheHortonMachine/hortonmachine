package org.hortonmachine.modules;
///*
// * This file is part of HortonMachine (http://www.hortonmachine.org)
// * (C) HydroloGIS - www.hydrologis.com 
// * 
// * The HortonMachine is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package org.hortonmachine.modules;
//
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_AUTHORCONTACTS;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_AUTHORNAMES;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_DESCRIPTION;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_KEYWORDS;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_LABEL;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_LICENSE;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_NAME;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_STATUS;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_doLegacyGrass_DESCRIPTION;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_doLenient_DESCRIPTION;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_inPath_DESCRIPTION;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_inRasterBounds_DESCRIPTION;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_inRasterFile_DESCRIPTION;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_inVectorFile_DESCRIPTION;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_inWMS_DESCRIPTION;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_pCheckcolor_DESCRIPTION;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_pEast_DESCRIPTION;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_pEpsg_DESCRIPTION;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_pImagetype_DESCRIPTION;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_pMaxThreads_DESCRIPTION;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_pMaxzoom_DESCRIPTION;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_pMinzoom_DESCRIPTION;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_pName_DESCRIPTION;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_pNorth_DESCRIPTION;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_pSouth_DESCRIPTION;
//import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_pWest_DESCRIPTION;
//
//import java.util.List;
//
//import oms3.annotations.Author;
//import oms3.annotations.Description;
//import oms3.annotations.Execute;
//import oms3.annotations.In;
//import oms3.annotations.Keywords;
//import oms3.annotations.Label;
//import oms3.annotations.License;
//import oms3.annotations.Name;
//import oms3.annotations.Status;
//import oms3.annotations.UI;
//
//import org.geotools.coverage.grid.GridGeometry2D;
//import org.hortonmachine.gears.libs.modules.HMConstants;
//import org.hortonmachine.gears.libs.modules.HMModel;
//import org.hortonmachine.gears.modules.r.tmsgenerator.OmsTmsGenerator;
//
//@Description(OMSTMSGENERATOR_DESCRIPTION)
//@Author(name = OMSTMSGENERATOR_AUTHORNAMES, contact = OMSTMSGENERATOR_AUTHORCONTACTS)
//@Keywords(OMSTMSGENERATOR_KEYWORDS)
//@Label(HMConstants.MOBILE)
//@Name("_" + OMSTMSGENERATOR_NAME)
//@Status(OMSTMSGENERATOR_STATUS)
//@License(OMSTMSGENERATOR_LICENSE)
//public class TmsGenerator extends HMModel {
//
//    @Description(OMSTMSGENERATOR_inRasterFile_DESCRIPTION)
//    @UI(HMConstants.FILEIN_UI_HINT)
//    @In
//    public String inRasterFile = null;
//
//    @Description(OMSTMSGENERATOR_inRasterBounds_DESCRIPTION)
//    @In
//    public List<GridGeometry2D> inRasterBounds = null;
//
//    @Description(OMSTMSGENERATOR_inVectorFile_DESCRIPTION)
//    @UI(HMConstants.FILEIN_UI_HINT)
//    @In
//    public String inVectorFile = null;
//
//    @Description(OMSTMSGENERATOR_inWMS_DESCRIPTION)
//    @In
//    public String inWMS = null;
//
//    @Description(OMSTMSGENERATOR_pName_DESCRIPTION)
//    @In
//    public String pName = "tmstiles";
//
//    @Description(OMSTMSGENERATOR_pMinzoom_DESCRIPTION)
//    @In
//    public Integer pMinzoom = null;
//
//    @Description(OMSTMSGENERATOR_pMaxzoom_DESCRIPTION)
//    @In
//    public Integer pMaxzoom = null;
//
//    @Description(OMSTMSGENERATOR_pNorth_DESCRIPTION)
//    @UI(HMConstants.PROCESS_NORTH_UI_HINT)
//    @In
//    public Double pNorth = null;
//
//    @Description(OMSTMSGENERATOR_pSouth_DESCRIPTION)
//    @UI(HMConstants.PROCESS_SOUTH_UI_HINT)
//    @In
//    public Double pSouth = null;
//
//    @Description(OMSTMSGENERATOR_pWest_DESCRIPTION)
//    @UI(HMConstants.PROCESS_WEST_UI_HINT)
//    @In
//    public Double pWest = null;
//
//    @Description(OMSTMSGENERATOR_pEast_DESCRIPTION)
//    @UI(HMConstants.PROCESS_EAST_UI_HINT)
//    @In
//    public Double pEast = null;
//
//    @Description(OMSTMSGENERATOR_pEpsg_DESCRIPTION)
//    @UI(HMConstants.CRS_UI_HINT)
//    @In
//    public String pEpsg;
//
//    @Description(OMSTMSGENERATOR_doLenient_DESCRIPTION)
//    @In
//    public boolean doLenient = true;
//
//    @Description(OMSTMSGENERATOR_pImagetype_DESCRIPTION)
//    @In
//    public int pImagetype = 0;
//
//    @Description(OMSTMSGENERATOR_pCheckcolor_DESCRIPTION)
//    @In
//    public int[] pCheckcolor = new int[]{255, 255, 255};
//
//    @Description(OMSTMSGENERATOR_doLegacyGrass_DESCRIPTION)
//    @In
//    public Boolean doLegacyGrass = false;
//
//    @Description(OMSTMSGENERATOR_inPath_DESCRIPTION)
//    @In
//    public String inPath;
//
//    @Execute
//    public void process() throws Exception {
//        OmsTmsGenerator tmsgenerator = new OmsTmsGenerator();
//        tmsgenerator.inRasterFile = inRasterFile;
//        tmsgenerator.inRasterBounds = inRasterBounds;
//        tmsgenerator.inVectorFile = inVectorFile;
//        tmsgenerator.inWMS = inWMS;
//        tmsgenerator.pName = pName;
//        tmsgenerator.pMinzoom = pMinzoom;
//        tmsgenerator.pMaxzoom = pMaxzoom;
//        tmsgenerator.pNorth = pNorth;
//        tmsgenerator.pSouth = pSouth;
//        tmsgenerator.pWest = pWest;
//        tmsgenerator.pEast = pEast;
//        tmsgenerator.pEpsg = pEpsg;
//        tmsgenerator.doLenient = doLenient;
//        tmsgenerator.pImagetype = pImagetype;
//        tmsgenerator.pCheckcolor = pCheckcolor;
//        tmsgenerator.doLegacyGrass = doLegacyGrass;
//        tmsgenerator.inPath = inPath;
//        tmsgenerator.pm = pm;
//        tmsgenerator.doProcess = doProcess;
//        tmsgenerator.doReset = doReset;
//        tmsgenerator.process();
//    }
//}
