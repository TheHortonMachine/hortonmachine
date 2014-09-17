/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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

import static org.jgrasstools.gears.libs.modules.Variables.CONIFER;
import static org.jgrasstools.gears.libs.modules.Variables.CUSTOM;
import static org.jgrasstools.gears.libs.modules.Variables.DECIDUOUS;
import static org.jgrasstools.gears.libs.modules.Variables.MIXED_PINES_AND_DECIDUOUS;
import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

import org.jgrasstools.gears.i18n.GearsMessages;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.lesto.modules.maxima.OmsRasterMaximaFinder;

@Description(OmsRasterMaximaFinder.OMSMAXIMAFINDER_DESCRIPTION)
@Author(name = GearsMessages.OMSHYDRO_AUTHORNAMES, contact = GearsMessages.OMSHYDRO_AUTHORCONTACTS)
@Keywords(OmsRasterMaximaFinder.OMSMAXIMAFINDER_KEYWORDS)
@Label(OmsRasterMaximaFinder.OMSMAXIMAFINDER_LABEL)
@Name(OmsRasterMaximaFinder.OMSMAXIMAFINDER_NAME)
@Status(OmsRasterMaximaFinder.OMSMAXIMAFINDER_STATUS)
@License(GearsMessages.OMSHYDRO_LICENSE)
public class RasterMaximaFinder extends JGTModel {

    @Description(OmsRasterMaximaFinder.inGeodata_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inDsmDtmDiff;

    @Description(OmsRasterMaximaFinder.pMode_DESCRIPTION)
    @UI("combo:" + CUSTOM + "," + MIXED_PINES_AND_DECIDUOUS + "," + DECIDUOUS + "," + CONIFER)
    @In
    public String pMode = CUSTOM;

    @Description(OmsRasterMaximaFinder.pThreshold_DESCRIPTION)
    @In
    public double pThreshold = 1.0;

    @Description(OmsRasterMaximaFinder.pSize_DESCRIPTION)
    @In
    public int pSize = 3;

    @Description(OmsRasterMaximaFinder.pPercent_DESCRIPTION)
    @In
    public int pPercent = 60;

    @Description(OmsRasterMaximaFinder.pMaxRadius_DESCRIPTION)
    @In
    public double pMaxRadius = 3.0;

    @Description(OmsRasterMaximaFinder.doCircular_DESCRIPTION)
    @In
    public boolean doCircular = true;

    @Description(OmsRasterMaximaFinder.pBorderDistanceThres_DESCRIPTION)
    @Unit("m")
    @In
    public double pBorderDistanceThres = -1.0;

    @Description(OmsRasterMaximaFinder.outMaxima_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outMaxima;

    @Description(OmsRasterMaximaFinder.outCircles_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outCircles;

    @Execute
    public void process() throws Exception {
        OmsRasterMaximaFinder maxFinder = new OmsRasterMaximaFinder();
        maxFinder.inDsmDtmDiff = getRaster(inDsmDtmDiff);
        maxFinder.pMode = pMode;
        maxFinder.pThreshold = pThreshold;
        maxFinder.pSize = pSize;
        maxFinder.pPercent = pPercent;
        maxFinder.pMaxRadius = pMaxRadius;
        maxFinder.doCircular = doCircular;
        maxFinder.pBorderDistanceThres = pBorderDistanceThres;
        maxFinder.process();
        dumpVector(maxFinder.outMaxima, outMaxima);
        dumpVector(maxFinder.outCircles, outCircles);
    }
}
