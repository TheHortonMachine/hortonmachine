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
package org.jgrasstools.gears.io.arcgrid;

import java.io.File;
import java.io.IOException;

import oms3.annotations.Author;
import oms3.annotations.Category;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.gce.arcgrid.ArcGridFormat;
import org.geotools.gce.arcgrid.ArcGridWriteParams;
import org.geotools.gce.arcgrid.ArcGridWriter;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;

@Description("Utility class for writing geotools coverages to arcgrids.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Arcgrid, Coverage, Raster, Writing")
@Category(JGTConstants.RASTERWRITER)
@Status(Status.CERTIFIED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class ArcgridCoverageWriter extends JGTModel {
    @Description("The coverage map that needs to be written.")
    @In
    public GridCoverage2D geodata = null;

    @Description("The output arcgrid path.")
    @In
    public String file = null;

    private boolean hasWritten = false;

    @Execute
    public void writeCoverage() throws IOException {
        if (!concatOr(!hasWritten, doReset)) {
            return;
        }

        final ArcGridFormat format = new ArcGridFormat();
        final ArcGridWriteParams wp = new ArcGridWriteParams();
        final ParameterValueGroup paramWrite = format.getWriteParameters();
        paramWrite.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString())
                .setValue(wp);
        ArcGridWriter gtw = (ArcGridWriter) format.getWriter(new File(file));
        gtw.write(geodata, (GeneralParameterValue[]) paramWrite.values().toArray(
                new GeneralParameterValue[1]));
        gtw.dispose();
        hasWritten = true;
    }

    /**
     * Utility method to quickly write a grid.
     * 
     * @param path the path to the new file.
     * @param coverage the coverage to write.
     * @throws Exception
     */
    public static void writeArcgrid( String path, GridCoverage2D coverage ) throws Exception {
        ArcgridCoverageWriter writer = new ArcgridCoverageWriter();
        writer.file = path;
        writer.geodata = coverage;
        writer.writeCoverage();
    }

}
