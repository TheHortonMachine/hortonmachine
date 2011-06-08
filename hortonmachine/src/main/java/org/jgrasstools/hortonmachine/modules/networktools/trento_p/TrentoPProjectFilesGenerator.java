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
package org.jgrasstools.hortonmachine.modules.networktools.trento_p;

import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Utility.makeLineStringShp;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Utility.makePolygonShp;

import java.io.File;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.referencing.CRS;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.ITrentoPType;
import org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.TrentoPFeatureType.PipesTrentoP;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description("Generates the input shapefiles for  a TrentoP simulation.")
@Author(name = "Daniele Andreis")
@Keywords("TrentoP")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class TrentoPProjectFilesGenerator extends JGTModel {

    @Description("The folder into which to create the base files.")
    @In
    public String inFolder = null;

    @Description("The code defining the coordinate reference system, composed by authority and code number (ex. EPSG:4328).")
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description("The name of the .shp file. By deafault it is network.shp")
    @In
    public String pShapeFileName = "network.shp";

    @Description("The name of the .shp file. By deafault it is network.shp")
    @In
    public String pAreaShapeFileName = "area.shp";

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Execute
    public void process() throws Exception {
        checkNull(inFolder, pCode);

        CoordinateReferenceSystem crs = CRS.decode(pCode);

        File baseFolder = new File(inFolder);

        pm.beginTask("Create epanet project shapefiles...", 7);
        pm.worked(1);
        ITrentoPType[] values = PipesTrentoP.values();
        makeLineStringShp(values, baseFolder, crs, pShapeFileName, null);
        makePolygonShp(values, baseFolder, crs, pAreaShapeFileName);

        pm.done();
    }

}
