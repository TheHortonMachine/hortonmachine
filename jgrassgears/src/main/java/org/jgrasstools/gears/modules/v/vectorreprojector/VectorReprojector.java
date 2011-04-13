/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.jgrasstools.gears.modules.v.vectorreprojector;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
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

@Description("Module for vector reprojection")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Crs, Reprojection, Vector")
@Label(JGTConstants.VECTORPROCESSING)
@Status(Status.EXPERIMENTAL)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class VectorReprojector extends JGTModel {

    @Description("The feature collection that has to be reprojected.")
    @In
    public SimpleFeatureCollection inGeodata;

    @Description("The code defining the target coordinate reference system, composed by authority and code number (ex. EPSG:4328).")
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

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

    @Description("The reprojected feature collection.")
    @Out
    public SimpleFeatureCollection outGeodata = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outGeodata == null, doReset)) {
            return;
        }

        CoordinateReferenceSystem targetCrs = CRS.decode(pCode);
        if (pForceCode != null) {
            pm.beginTask("Forcing input crs...", IJGTProgressMonitor.UNKNOWN);
            CoordinateReferenceSystem forcedCrs = CRS.decode(pForceCode);
            inGeodata = new ForceCoordinateSystemFeatureResults(inGeodata, forcedCrs);
            pm.done();
        }

        pm.beginTask("Reprojecting features...", IJGTProgressMonitor.UNKNOWN);
        outGeodata = new ReprojectingFeatureCollection(inGeodata, targetCrs);
        pm.done();
    }

}
