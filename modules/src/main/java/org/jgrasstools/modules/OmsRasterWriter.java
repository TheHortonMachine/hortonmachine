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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERWRITER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERWRITER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERWRITER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERWRITER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERWRITER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERWRITER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERWRITER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERWRITER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERWRITER_file_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSRASTERWRITER_inRaster_DESCRIPTION;
import static org.jgrasstools.gears.libs.modules.JGTConstants.ESRIGRID;
import static org.jgrasstools.gears.libs.modules.JGTConstants.GEOTIF;
import static org.jgrasstools.gears.libs.modules.JGTConstants.GEOTIFF;
import static org.jgrasstools.gears.libs.modules.JGTConstants.GRASS;

import java.io.File;

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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.gce.arcgrid.ArcGridFormat;
import org.geotools.gce.arcgrid.ArcGridWriteParams;
import org.geotools.gce.arcgrid.ArcGridWriter;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.gce.grassraster.GrassCoverageWriter;
import org.geotools.gce.grassraster.JGrassMapEnvironment;
import org.geotools.gce.grassraster.JGrassRegion;
import org.geotools.gce.grassraster.format.GrassCoverageFormat;
import org.geotools.gce.grassraster.format.GrassCoverageFormatFactory;
import org.jgrasstools.gears.io.grasslegacy.GrassLegacyGridCoverage2D;
import org.jgrasstools.gears.io.grasslegacy.OmsGrassLegacyWriter;
import org.jgrasstools.gears.io.grasslegacy.utils.GrassLegacyUtilities;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;

@Description(OMSRASTERWRITER_DESCRIPTION)
@Author(name = OMSRASTERWRITER_AUTHORNAMES, contact = OMSRASTERWRITER_AUTHORCONTACTS)
@Keywords(OMSRASTERWRITER_KEYWORDS)
@Label(OMSRASTERWRITER_LABEL)
@Name("_" + OMSRASTERWRITER_NAME)
@Status(OMSRASTERWRITER_STATUS)
@License(OMSRASTERWRITER_LICENSE)
public class OmsRasterWriter extends JGTModel {

    @Description(OMSRASTERWRITER_inRaster_DESCRIPTION)
    @In
    public GridCoverage2D inRaster = null;

    @Description(OMSRASTERWRITER_file_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String file = null;

    private boolean hasWritten = false;

    @Execute
    public void process() throws Exception {
        if (!concatOr(!hasWritten, doReset)) {
            return;
        }
        checkNull(inRaster);

        if (inRaster.getName().toString().equals("dummy")) {
            pm.message("WARNING: Not writing dummy raster to file.");
            return;
        }

        String pType = null;
        // guess from the extension
        if (file.toLowerCase().endsWith(ESRIGRID)) {
            pType = ESRIGRID;
        } else if (file.toLowerCase().endsWith(GEOTIFF) || file.toLowerCase().endsWith(GEOTIF)) {
            pType = GEOTIFF;
        } else if (CoverageUtilities.isGrass(file)) {
            pType = GRASS;
        } else
            throw new ModelsIllegalargumentException("Can't recognize the data format. Supported are: asc, tiff, grass.", this
                    .getClass().getSimpleName());

        File mapFile = new File(file);
        try {
            pm.beginTask("Writing coverage: " + mapFile.getName(), IJGTProgressMonitor.UNKNOWN);

            if (pType.equals(ESRIGRID)) {
                writeArcGrid(mapFile);
            } else if (pType.equals(GEOTIFF)) {
                writeGeotiff(mapFile);
            } else if (pType.equals(GRASS)) {
                writeGrass(mapFile);
            } else {
                throw new ModelsIllegalargumentException("Data type not supported: " + pType, this.getClass().getSimpleName());
            }
            hasWritten = true;
        } finally {
            pm.done();
        }

    }

    private void writeArcGrid( File mapFile ) throws Exception {
        final ArcGridFormat format = new ArcGridFormat();
        final ArcGridWriteParams wp = new ArcGridWriteParams();
        final ParameterValueGroup paramWrite = format.getWriteParameters();
        paramWrite.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
        ArcGridWriter gtw = (ArcGridWriter) format.getWriter(new File(file));
        gtw.write(inRaster, (GeneralParameterValue[]) paramWrite.values().toArray(new GeneralParameterValue[1]));
        gtw.dispose();
    }

    private void writeGeotiff( File mapFile ) throws Exception {
        final GeoTiffFormat format = new GeoTiffFormat();
        final GeoTiffWriteParams wp = new GeoTiffWriteParams();
        wp.setCompressionMode(GeoTiffWriteParams.MODE_DEFAULT);
        wp.setTilingMode(GeoToolsWriteParams.MODE_DEFAULT);
        final ParameterValueGroup paramWrite = format.getWriteParameters();
        paramWrite.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
        GeoTiffWriter gtw = (GeoTiffWriter) format.getWriter(mapFile);
        gtw.write(inRaster, (GeneralParameterValue[]) paramWrite.values().toArray(new GeneralParameterValue[1]));
    }

    private void writeGrass( File mapFile ) throws Exception {
        File cellFile = mapFile;
        JGrassMapEnvironment mapEnvironment = new JGrassMapEnvironment(cellFile);
        GeneralParameterValue[] readParams = null;
        JGrassRegion jGrassRegion = null;
        boolean doLarge = false;
        if (inRaster instanceof GrassLegacyGridCoverage2D) {
            doLarge = true;
        }
        // if (doActive) {
        // jGrassRegion = mapEnvironment.getActiveRegion();
        // if (!doLarge) {
        // readParams =
        // CoverageUtilities.createGridGeometryGeneralParameter(jGrassRegion.getCols(),
        // jGrassRegion.getRows(), jGrassRegion.getNorth(), jGrassRegion.getSouth(),
        // jGrassRegion.getEast(),
        // jGrassRegion.getWest(), mapEnvironment.getCoordinateReferenceSystem());
        // }
        // }

        if (!doLarge) {
            GrassCoverageFormat format = new GrassCoverageFormatFactory().createFormat();
            GrassCoverageWriter writer = format.getWriter(mapEnvironment.getCELL(), null);
            writer.write(inRaster, readParams);
            writer.dispose();
        } else {
            GrassLegacyGridCoverage2D gd2 = (GrassLegacyGridCoverage2D) inRaster;
            OmsGrassLegacyWriter writer = new OmsGrassLegacyWriter();
            writer.geodata = gd2.getData();
            writer.file = file;
            if (jGrassRegion == null)
                jGrassRegion = mapEnvironment.getActiveRegion();
            writer.inWindow = GrassLegacyUtilities.jgrassRegion2legacyWindow(jGrassRegion);
            writer.writeRaster();
        }

    }

    public static void writeRaster( String path, GridCoverage2D coverage ) throws Exception {
        OmsRasterWriter writer = new OmsRasterWriter();
        writer.inRaster = coverage;
        writer.file = path;
        writer.process();
    }
}
