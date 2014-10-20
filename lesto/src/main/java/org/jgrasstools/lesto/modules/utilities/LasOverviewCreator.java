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
package org.jgrasstools.lesto.modules.utilities;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.io.File;
import java.io.FilenameFilter;
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
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.jgrasstools.gears.io.las.core.ALasReader;
import org.jgrasstools.gears.io.las.core.ILasHeader;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Polygon;

@Description("Create an overview shapefile of a folder of las files.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("overview, vector, lidar, las")
@Label(JGTConstants.LESTO + "/utilities")
@Name("lasoverviewcreator")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class LasOverviewCreator extends JGTModel {
    @Description("Las file or folder path.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inLas = null;

    @Description("Output overview shapefile.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outOverview = null;

    @Execute
    public void process() throws Exception {
        checkNull(inLas, outOverview);

        File[] lasFiles = null;
        File inLasFile = new File(inLas);
        if (inLasFile.isDirectory()) {
            lasFiles = inLasFile.listFiles(new FilenameFilter(){
                public boolean accept( File dir, String name ) {
                    return name.toLowerCase().endsWith(".las");
                }
            });
        } else {
            lasFiles = new File[]{inLasFile};
        }

        CoordinateReferenceSystem crs = null;
        pm.beginTask("Creating overviews...", lasFiles.length);
        List<Polygon> overviewPolygons = new ArrayList<>();
        for( File file : lasFiles ) {
            try (ALasReader lasReader = ALasReader.getReader(file, null)) {
                lasReader.open();
                ILasHeader header = lasReader.getHeader();

                if (crs == null)
                    crs = header.getCrs();

                ReferencedEnvelope3D dataEnvelope = header.getDataEnvelope();
                Polygon polygon = FeatureUtilities.envelopeToPolygon(dataEnvelope);
                polygon.setUserData(file.getName());
                overviewPolygons.add(polygon);
            }
            pm.worked(1);
        }
        pm.done();

        SimpleFeatureCollection overviewFC = FeatureUtilities.featureCollectionFromGeometry(crs,
                overviewPolygons.toArray(GeometryUtilities.TYPE_POLYGON));
        dumpVector(overviewFC, outOverview);
    }
}
