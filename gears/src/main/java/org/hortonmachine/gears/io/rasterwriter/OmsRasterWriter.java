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
package org.hortonmachine.gears.io.rasterwriter;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERWRITER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERWRITER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERWRITER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERWRITER_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERWRITER_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERWRITER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERWRITER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERWRITER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERWRITER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSRASTERWRITER_STATUS;
import static org.hortonmachine.gears.libs.modules.HMConstants.ESRIGRID;
import static org.hortonmachine.gears.libs.modules.HMConstants.GEOTIF;
import static org.hortonmachine.gears.libs.modules.HMConstants.GEOTIFF;
import static org.hortonmachine.gears.libs.modules.HMConstants.GRASS;

import java.io.File;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.gce.arcgrid.ArcGridFormat;
import org.geotools.gce.arcgrid.ArcGridWriteParams;
import org.geotools.gce.arcgrid.ArcGridWriter;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.gce.grassraster.GrassCoverageWriter;
import org.geotools.gce.grassraster.JGrassMapEnvironment;
import org.geotools.gce.grassraster.format.GrassCoverageFormat;
import org.geotools.gce.grassraster.format.GrassCoverageFormatFactory;
import org.geotools.util.factory.Hints;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;

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

@Description(OMSRASTERWRITER_DESCRIPTION)
@Author(name = OMSRASTERWRITER_AUTHORNAMES, contact = OMSRASTERWRITER_AUTHORCONTACTS)
@Keywords(OMSRASTERWRITER_KEYWORDS)
@Label(OMSRASTERWRITER_LABEL)
@Name(OMSRASTERWRITER_NAME)
@Status(OMSRASTERWRITER_STATUS)
@License(OMSRASTERWRITER_LICENSE)
public class OmsRasterWriter extends HMModel {

    @Description(OMSRASTERWRITER_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster = null;

    @Description(OMSRASTERWRITER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String file = null;

    @Execute
    public void process() throws Exception {
        if (inRaster == null) {
            return;
        }

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
            throw new ModelsIllegalargumentException("Can't recognize the data format. Supported are: asc, tiff, grass.",
                    this.getClass().getSimpleName(), pm);

        File mapFile = new File(file);
        try {
            pm.beginTask("Writing coverage: " + mapFile.getName(), IHMProgressMonitor.UNKNOWN);

            if (pType.equals(ESRIGRID)) {
                writeArcGrid(mapFile);
            } else if (pType.equals(GEOTIFF)) {
                writeGeotiff(mapFile);
            } else if (pType.equals(GRASS)) {
                writeGrass(mapFile);
            } else {
                throw new ModelsIllegalargumentException("Data type not supported: " + pType, this.getClass().getSimpleName(),
                        pm);
            }
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
        GeoTiffWriter gtw = new GeoTiffWriter(mapFile, new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE));// (GeoTiffWriter) format.getWriter(mapFile);
        gtw.write(inRaster, (GeneralParameterValue[]) paramWrite.values().toArray(new GeneralParameterValue[1]));
    }
    
    private void writeGrass( File mapFile ) throws Exception {
        File cellFile = mapFile;
        JGrassMapEnvironment mapEnvironment = new JGrassMapEnvironment(cellFile);
        GeneralParameterValue[] readParams = null;
        GrassCoverageFormat format = new GrassCoverageFormatFactory().createFormat();
        GrassCoverageWriter writer = format.getWriter(mapEnvironment.getCELL(), null);
        writer.write(inRaster, readParams);
        writer.dispose();

    }

    public static void writeRaster( String path, GridCoverage2D coverage ) throws Exception {
        OmsRasterWriter writer = new OmsRasterWriter();
        writer.inRaster = coverage;
        writer.file = path;
        writer.process();
    }
}
