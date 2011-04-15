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
package org.jgrasstools.gears.modules.v.vectorreprojector;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.data.crs.ForceCoordinateSystemFeatureResults;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description("Module for vector reprojection.")
@Documentation("VectorReprojector.html")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("CRS, Reprojection, Vector")
@Label(JGTConstants.VECTORPROCESSING)
@Status(Status.CERTIFIED)
@Name("vreproject")
@License("General Public License Version 3 (GPLv3)")
public class VectorReprojector extends JGTModel {

    @Description("The vector that has to be reprojected.")
    @In
    public SimpleFeatureCollection inGeodata;

    @Description("The code defining the target coordinate reference system, composed by authority and code number (ex. EPSG:4328).")
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description("A flag to modify the axes order.")
    @In
    public Boolean doLongitudeFirst = null;

    @Description("A coordinate reference system on which to force the input, composed by authority and code number (ex. EPSG:4328).")
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pForceCode;

    @Description("Switch that set to true allows for some error due to different datums. If set to false, it won't reproject without Bursa Wolf parameters.")
    @In
    public boolean doLenient = true;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The output reprojected vector.")
    @Out
    public SimpleFeatureCollection outGeodata = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outGeodata == null, doReset)) {
            return;
        }

        CoordinateReferenceSystem targetCrs = null;
        if (doLongitudeFirst != null) {
            targetCrs = CRS.decode(pCode, doLongitudeFirst);
        }else{
            targetCrs = CRS.decode(pCode);
        }
        if (pForceCode != null) {
            pm.beginTask("Forcing input crs...", IJGTProgressMonitor.UNKNOWN);
            CoordinateReferenceSystem forcedCrs = CRS.decode(pForceCode);
            inGeodata = new ForceCoordinateSystemFeatureResults(inGeodata, forcedCrs);
            pm.done();
        }

        pm.beginTask("Reprojecting features...", IJGTProgressMonitor.UNKNOWN);
        try {
            outGeodata = new ReprojectingFeatureCollection(inGeodata, targetCrs);
        } finally {

            pm.done();
        }

    }

}
