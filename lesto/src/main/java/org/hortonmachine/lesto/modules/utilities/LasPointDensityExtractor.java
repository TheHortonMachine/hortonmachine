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
import oms3.annotations.Unit;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;

@Description("Creates a vector of squares over a las file, with the number of points of each return and their density in each square.")
@Documentation(LasPointDensityExtractor.DOCUMENTATION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("las, density, vector")
@Label(HMConstants.LESTO + "/utilities")
@Name("laspointdensityextractor")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class LasPointDensityExtractor extends HMModel {

    @Description("Las file path.")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inFile;

    @Description("The size of the square cells of the grid, starting from the south-west corner of the data.")
    @Unit("m")
    @In
    public double pGridStep = 1.0;

    @Description("The output squares of the grid that have points, with the count of the points of each return (imp1 to imp5), their total and their density in points per square unit.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outFile;

    public static final String DOCUMENTATION = """
            The squares start from the south-west corner of the bounds of the file and cover them all: the last column and row can stick out of the data, and their density, on the whole area of the square, is lower than the true one. The points on the borders between squares go to the square on their north-east, the ones on the north and east bounds of the data to the last squares.

            The returns from 1 to 5 are counted each in its field; points with a return number out of this range count only in the total. The squares without points are not written.

            The density shows where a survey is dense enough for a given use, as an elevation model of a given resolution, and where the flight lines overlap. The first returns are the first surface hit, the canopy where there is forest, and the following ones come from under it: their counts show how much the laser goes through the vegetation.
            """;

    @Execute
    public void process() throws Exception {
        checkNull(inFile, outFile);

        if (pGridStep <= 0) {
            throw new ModelsIllegalargumentException("The grid step has to be major than 0.", this);
        }

        CoordinateReferenceSystem crs;
        int cols;
        int rows;
        double west;
        double south;
        /*
         * the count of [total, imp1, imp2, imp3, imp4, imp5] of each cell
         */
        int[][][] counts;
        final File lasFile = new File(inFile);
        try (ALasReader lasReader = ALasReader.getReader(lasFile, null)) {
            lasReader.open();

            ILasHeader header = lasReader.getHeader();
            crs = header.getCrs();
            ReferencedEnvelope3D dataEnvelope = header.getDataEnvelope();
            west = dataEnvelope.getMinX();
            south = dataEnvelope.getMinY();
            cols = Math.max(1, (int) Math.ceil(dataEnvelope.getWidth() / pGridStep));
            rows = Math.max(1, (int) Math.ceil(dataEnvelope.getHeight() / pGridStep));
            counts = new int[cols][rows][6];

            final long recordsCount = header.getRecordsCount();
            pm.beginTask("Sorting las data...", (int) recordsCount);
            while( lasReader.hasNextPoint() ) {
                pm.worked(1);
                final LasRecord lasDot = lasReader.getNextPoint();
                // the points on the north and east bounds go to the last cells
                int col = Math.min(cols - 1, Math.max(0, (int) Math.floor((lasDot.x - west) / pGridStep)));
                int row = Math.min(rows - 1, Math.max(0, (int) Math.floor((lasDot.y - south) / pGridStep)));
                int[] cellCounts = counts[col][row];
                int impulse = lasDot.returnNumber;
                if (impulse >= 1 && impulse <= 5) {
                    cellCounts[impulse]++;
                }
                cellCounts[0]++;
            }
            pm.done();
        }

        DefaultFeatureCollection outGeodata = new DefaultFeatureCollection();

        final SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("lasdata");
        b.setCRS(crs);
        b.add("the_geom", Polygon.class);
        b.add("imp1", Integer.class);
        b.add("imp2", Integer.class);
        b.add("imp3", Integer.class);
        b.add("imp4", Integer.class);
        b.add("imp5", Integer.class);
        b.add("total", Integer.class);
        b.add("density", Double.class);
        final SimpleFeatureType type = b.buildFeatureType();
        final SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        double cellArea = pGridStep * pGridStep;
        for( int col = 0; col < cols; col++ ) {
            for( int row = 0; row < rows; row++ ) {
                int[] c = counts[col][row];
                if (c[0] == 0) {
                    continue;
                }
                double minX = west + col * pGridStep;
                double minY = south + row * pGridStep;
                Envelope envelope = new Envelope(minX, minX + pGridStep, minY, minY + pGridStep);
                Polygon polygon = GeometryUtilities.createPolygonFromEnvelope(envelope);
                Object[] objs = {polygon, c[1], c[2], c[3], c[4], c[5], c[0], c[0] / cellArea};
                builder.addAll(objs);
                outGeodata.add(builder.buildFeature(null));
            }
        }

        dumpVector(outGeodata, outFile);
    }

}
