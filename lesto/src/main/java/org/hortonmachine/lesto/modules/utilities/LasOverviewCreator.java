/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.lesto.modules.utilities;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Polygon;

@Description("Creates a vector of the extents of the las files of a folder, with their names, number of points and range of elevations.")
@Documentation(LasOverviewCreator.DOCUMENTATION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("overview, vector, lidar, las")
@Label(HMConstants.LESTO + "/utilities")
@Name("lasoverviewcreator")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class LasOverviewCreator extends HMModel {
    @Description("Las file or folder path.")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inLas = null;

    @Description("The overview: a rectangle per file, with its name (file), number of points (points) and lowest and highest elevation (minelev, maxelev).")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outOverview = null;

    public static final String DOCUMENTATION = """
            The rectangles are the bounds written in the headers of the files, and the number of points and the elevations come from the headers too: the points are not read, so the overview of a large folder is quick. The reference system is the one of the prj file of the first las file.

            Only the files ending in .las are read, without the subfolders. The overview shows how a survey is divided in files, and where a file is, before opening it.
            """;

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

        if (lasFiles == null || lasFiles.length == 0) {
            throw new ModelsIllegalargumentException("No las files found in: " + inLas, this);
        }

        CoordinateReferenceSystem crs = null;
        List<Object[]> overviewValues = new ArrayList<>();
        pm.beginTask("Creating overviews...", lasFiles.length);
        for( File file : lasFiles ) {
            try (ALasReader lasReader = ALasReader.getReader(file, null)) {
                lasReader.open();
                ILasHeader header = lasReader.getHeader();

                if (crs == null)
                    crs = header.getCrs();

                ReferencedEnvelope3D dataEnvelope = header.getDataEnvelope();
                Polygon polygon = GeometryUtilities.createPolygonFromEnvelope(dataEnvelope);
                overviewValues.add(new Object[]{polygon, file.getName(), header.getRecordsCount(), dataEnvelope.getMinZ(),
                        dataEnvelope.getMaxZ()});
            }
            pm.worked(1);
        }
        pm.done();

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("overview");
        b.setCRS(crs);
        b.add("the_geom", Polygon.class);
        b.add("file", String.class);
        b.add("points", Long.class);
        b.add("minelev", Double.class);
        b.add("maxelev", Double.class);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(b.buildFeatureType());
        DefaultFeatureCollection overviewFC = new DefaultFeatureCollection();
        for( Object[] values : overviewValues ) {
            builder.addAll(values);
            overviewFC.add(builder.buildFeature(null));
        }
        dumpVector(overviewFC, outOverview);
    }
}
