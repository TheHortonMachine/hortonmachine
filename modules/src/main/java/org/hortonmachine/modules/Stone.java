/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.modules;

import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.CAUCHY;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.GAUSSIAN;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.UNIFORM;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_KEYWORDS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_LABEL;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_LICENSE;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_NAME;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_STATUS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_inFriction_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_inNormalRestitution_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_inSources_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_inTangentialRestitution_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_outCounter_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_pAngleStochFunction_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_pAngleStochRange_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_pFrictionStochRange_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_pMaxTrajectories_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_pStep_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_pNormalRestitutionStochRange_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_pStartVelocity_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_pStochFunction_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_pStopVelocity_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone.OMSSTONE_pTangentialRestitutionStochRange_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSSTONE_DESCRIPTION)
@Author(name = OMSSTONE_AUTHORNAMES, contact = OMSSTONE_AUTHORCONTACTS)
@Keywords(OMSSTONE_KEYWORDS)
@Label(OMSSTONE_LABEL)
@Name("_" + OMSSTONE_NAME)
@Status(OMSSTONE_STATUS)
@License(OMSSTONE_LICENSE)
@Bibliography({OmsStone.BIBLIOGRAPHY_GUZZETTI_ET_AL_2002, OmsStone.BIBLIOGRAPHY_ALVIOLI_ET_AL_2021})
public class Stone extends HMModel {
    @Description(OMSSTONE_inElev_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inElev;

    @Description(OMSSTONE_inSources_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inSources;

    @Description(OMSSTONE_inNormalRestitution_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inNormalRestitution;

    @Description(OMSSTONE_inTangentialRestitution_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inTangentialRestitution;

    @Description(OMSSTONE_inFriction_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFriction;

    @Description(OMSSTONE_pStartVelocity_DESCRIPTION)
    @In
    public double pStartVelocity = 1.0;

    @Description(OMSSTONE_pStopVelocity_DESCRIPTION)
    @In
    public double pStopVelocity = 3.0;

    @Description(OMSSTONE_pAngleStochRange_DESCRIPTION)
    @In
    public int pAngleStochRange = 10;

    @Description(OMSSTONE_pNormalRestitutionStochRange_DESCRIPTION)
    @In
    public int pNormalRestitutionStochRange = 10;

    @Description(OMSSTONE_pTangentialRestitutionStochRange_DESCRIPTION)
    @In
    public int pTangentialRestitutionStochRange = 10;

    @Description(OMSSTONE_pFrictionStochRange_DESCRIPTION)
    @In
    public int pFrictionStochRange = 10;

    @Description(OMSSTONE_pAngleStochFunction_DESCRIPTION)
    @UI("combo:" + GAUSSIAN + "," + CAUCHY + "," + UNIFORM)
    @In
    public String pAngleStochFunction = GAUSSIAN;

    @Description(OMSSTONE_pStochFunction_DESCRIPTION)
    @UI("combo:" + GAUSSIAN + "," + UNIFORM)
    @In
    public String pStochFunction = GAUSSIAN;

    @Description(OMSSTONE_pStep_DESCRIPTION)
    @In
    public double pStep = 5.0;

    @Description(OMSSTONE_pMaxTrajectories_DESCRIPTION)
    @In
    public int pMaxTrajectories = 1000;

    @Description(OMSSTONE_outCounter_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outCounter = null;

    @Description(outMaxVelocity_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outMaxVelocity = null;

    @Description(outMaxDz_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outMaxDz = null;

    @Description(outTrajectories_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outTrajectories = null;

    public static final String outTrajectories_DESCRIPTION = "The optional vector file of the 3D lines of a sample of pMaxTrajectories trajectories, with the source cell, the maximum velocity [m/s] and the maximum height over the ground [m].";
    public static final String outMaxVelocity_DESCRIPTION = "The optional map of the maximum velocity of the boulders in each cell [m/s].";
    public static final String outMaxDz_DESCRIPTION = "The optional map of the maximum height of the trajectories over the ground in each cell [m].";

    @Execute
    public void process() throws Exception {
        checkNull(inElev, inSources, inNormalRestitution, inTangentialRestitution, inFriction, outCounter);
        OmsStone stone = new OmsStone();
        stone.pm = pm;
        stone.doProcess = doProcess;
        stone.doReset = doReset;
        stone.inElev = getRaster(inElev);
        stone.inSources = getRaster(inSources);
        stone.inNormalRestitution = getRaster(inNormalRestitution);
        stone.inTangentialRestitution = getRaster(inTangentialRestitution);
        stone.inFriction = getRaster(inFriction);
        stone.pStartVelocity = pStartVelocity;
        stone.pStopVelocity = pStopVelocity;
        stone.pAngleStochRange = pAngleStochRange;
        stone.pNormalRestitutionStochRange = pNormalRestitutionStochRange;
        stone.pTangentialRestitutionStochRange = pTangentialRestitutionStochRange;
        stone.pFrictionStochRange = pFrictionStochRange;
        stone.pAngleStochFunction = pAngleStochFunction;
        stone.pStochFunction = pStochFunction;
        stone.pStep = pStep;
        boolean doTrajectories = outTrajectories != null && outTrajectories.trim().length() > 0;
        stone.pMaxTrajectories = doTrajectories ? pMaxTrajectories : 0;
        stone.process();
        dumpRaster(stone.outCounter, outCounter);
        if (outMaxVelocity != null && outMaxVelocity.trim().length() > 0) {
            dumpRaster(stone.outMaxVelocity, outMaxVelocity);
        }
        if (outMaxDz != null && outMaxDz.trim().length() > 0) {
            dumpRaster(stone.outMaxDz, outMaxDz);
        }
        if (doTrajectories) {
            dumpVector(stone.outTrajectories, outTrajectories);
        }
    }
}
