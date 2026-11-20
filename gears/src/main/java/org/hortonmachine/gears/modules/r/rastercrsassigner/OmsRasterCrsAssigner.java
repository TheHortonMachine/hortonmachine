/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.gears.modules.r.rastercrsassigner;

import java.io.File;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridGeometry2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.files.FileUtilities;

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

@Description(OmsRasterCrsAssigner.DESCRIPTION)
@Documentation(OmsRasterCrsAssigner.DOCUMENTATION)
@Author(name = OmsRasterCrsAssigner.AUTHORNAMES, contact = OmsRasterCrsAssigner.AUTHORCONTACTS)
@Keywords(OmsRasterCrsAssigner.KEYWORDS)
@Label(OmsRasterCrsAssigner.LABEL)
@Name(OmsRasterCrsAssigner.NAME)
@Status(OmsRasterCrsAssigner.STATUS)
@License(OmsRasterCrsAssigner.LICENSE)
public class OmsRasterCrsAssigner extends HMModel {

    @Description(inRaster_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster;

    @Description(pCode_DESCRIPTION)
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(outRaster_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster;

    public static final String DESCRIPTION = "Assigns a coordinate reference system to a raster with a missing or wrong one, without changing its cells: written inside GeoTIFFs, as a .prj file next to the other formats.";
    public static final String DOCUMENTATION = """
            The coordinate reference system is only declared, the coordinates of the raster are kept as they are: use it when the georeferencing is right but the system is unknown or wrong. To move the raster into another system use the reprojection instead.

            A GeoTIFF is rewritten with the system inside the file. Without an output file the input GeoTIFF is replaced, through a temporary file next to it. A .prj file next to the written GeoTIFF, which the readers would prefer to the system in the file, is updated with the new system.

            For the other formats, as the ESRI ascii grid, the .prj file next to the input is written (or replaced) and the output file is not used. GRASS rasters are not supported, since their system is the one of the location.
            """;
    public static final String KEYWORDS = "Raster, CRS, Projection, prj, GeoTIFF";
    public static final String LABEL = HMConstants.RASTERPROCESSING;
    public static final String NAME = "rcrsassign";
    public static final int STATUS = Status.EXPERIMENTAL;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String AUTHORNAMES = "Andrea Antonello";
    public static final String AUTHORCONTACTS = "https://g-ant.eu";
    public static final String inRaster_DESCRIPTION = "The raster to assign the coordinate reference system to.";
    public static final String pCode_DESCRIPTION = "The code of the coordinate reference system to assign, as EPSG:32632.";
    public static final String outRaster_DESCRIPTION = "The optional output GeoTIFF. If not set, the input GeoTIFF is replaced. Not used for the formats that get a .prj file.";

    @Execute
    public void process() throws Exception {
        checkNull(inRaster, pCode);

        File inFile = new File(inRaster).getAbsoluteFile();
        if (!inFile.exists()) {
            throw new ModelsIllegalargumentException("The input raster doesn't exist: " + inRaster, this, pm);
        }
        if (CoverageUtilities.isGrass(inRaster)) {
            throw new ModelsIllegalargumentException(
                    "GRASS rasters are not supported, their coordinate reference system is the one of the location.", this,
                    pm);
        }

        CoordinateReferenceSystem crs = HMCrsRegistry.INSTANCE.getCrs(pCode);

        if (isGeotiff(inFile)) {
            assignToGeotiff(inFile, crs);
        } else {
            File prjFile = FileUtilities.substituteExtention(inFile, "prj");
            FileUtilities.writeFile(crs.toWKT(), prjFile);
            pm.message("Written the prj file: " + prjFile.getName());
        }
    }

    private void assignToGeotiff( File inFile, CoordinateReferenceSystem crs ) throws Exception {
        File outFile = outRaster == null || outRaster.isBlank() ? inFile : new File(outRaster).getAbsoluteFile();
        if (!isGeotiff(outFile)) {
            throw new ModelsIllegalargumentException("The output of a GeoTIFF has to be a GeoTIFF: " + outFile.getName(), this,
                    pm);
        }
        boolean inPlace = outFile.equals(inFile);

        GridCoverage2D inCoverage = OmsRasterReader.readRaster(inFile.getAbsolutePath());
        GridGeometry2D inGridGeometry = inCoverage.getGridGeometry();
        // same grid and image, only the crs changes
        GridGeometry2D gridGeometry = new GridGeometry2D(inGridGeometry.getGridRange(), inGridGeometry.getGridToCRS(), crs);
        GridCoverage2D outCoverage = new GridCoverageFactory().create(inCoverage.getName(), inCoverage.getRenderedImage(),
                gridGeometry, inCoverage.getSampleDimensions(), null, inCoverage.getProperties());

        File writeFile = inPlace ? File.createTempFile("crsassign_", ".tif", inFile.getParentFile()) : outFile;
        try {
            OmsRasterWriter.writeRaster(writeFile.getAbsolutePath(), outCoverage);
            // release the reader of the input before replacing it
            inCoverage.dispose(true);
            if (inPlace) {
                FileUtilities.moveFile(writeFile, inFile);
            }
        } finally {
            if (inPlace && writeFile.exists()) {
                FileUtilities.deleteFileOrDir(writeFile);
            }
        }

        // a prj next to the tiff would win over the crs inside it
        File prjFile = FileUtilities.substituteExtention(outFile, "prj");
        if (prjFile.exists()) {
            FileUtilities.writeFile(crs.toWKT(), prjFile);
            pm.message("Updated the existing prj file: " + prjFile.getName());
        }
        pm.message("Assigned " + pCode + " to: " + outFile.getName());
    }

    private static boolean isGeotiff( File file ) {
        String name = file.getName().toLowerCase();
        return name.endsWith("." + HMConstants.GEOTIFF) || name.endsWith("." + HMConstants.GEOTIF);
    }
}
