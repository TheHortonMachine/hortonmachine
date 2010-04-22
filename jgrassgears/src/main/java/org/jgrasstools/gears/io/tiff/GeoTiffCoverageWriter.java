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
package org.jgrasstools.gears.io.tiff;

import java.io.File;
import java.io.IOException;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Role;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IHMProgressMonitor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;

@Description("Utility class for writing geotools coverages to geotiffs.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Geotiff, Coverage, Raster, Writing")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class GeoTiffCoverageWriter extends JGTModel {
    @Role(Role.PARAMETER)
    @Description("The coverage map that needs to be written.")
    @In
    public GridCoverage2D geodata = null;

    @Role(Role.PARAMETER)
    @Description("The progress monitor.")
    @In
    public IHMProgressMonitor pm = new DummyProgressMonitor();

    @Role(Role.PARAMETER)
    @Description("The output geotiff file.")
    @Out
    public String file = null;

    private boolean hasWritten = false;

    @Execute
    public void writeCoverage() throws IOException {
        if (!concatOr(!hasWritten, doReset)) {
            return;
        }

        final GeoTiffFormat format = new GeoTiffFormat();
        final GeoTiffWriteParams wp = new GeoTiffWriteParams();
        wp.setCompressionMode(GeoTiffWriteParams.MODE_DEFAULT);
        wp.setTilingMode(GeoToolsWriteParams.MODE_DEFAULT);
        final ParameterValueGroup paramWrite = format.getWriteParameters();
        paramWrite.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString())
                .setValue(wp);
        GeoTiffWriter gtw = (GeoTiffWriter) format.getWriter(new File(file));
        gtw.write(geodata, (GeneralParameterValue[]) paramWrite.values().toArray(
                new GeneralParameterValue[1]));

        hasWritten = true;
    }
}
