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
package org.hortonmachine.gears.modules.r.mosaic;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.libs.modules.Variables.BICUBIC;
import static org.hortonmachine.gears.libs.modules.Variables.BILINEAR;
import static org.hortonmachine.gears.libs.modules.Variables.NEAREST_NEIGHTBOUR;
import static org.hortonmachine.gears.modules.r.mosaic.OmsMosaic.OMSMOSAIC_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.mosaic.OmsMosaic.OMSMOSAIC_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.mosaic.OmsMosaic.OMSMOSAIC_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.mosaic.OmsMosaic.OMSMOSAIC_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.mosaic.OmsMosaic.OMSMOSAIC_KEYWORDS;
import static org.hortonmachine.gears.modules.r.mosaic.OmsMosaic.OMSMOSAIC_LABEL;
import static org.hortonmachine.gears.modules.r.mosaic.OmsMosaic.OMSMOSAIC_LICENSE;
import static org.hortonmachine.gears.modules.r.mosaic.OmsMosaic.OMSMOSAIC_NAME;
import static org.hortonmachine.gears.modules.r.mosaic.OmsMosaic.OMSMOSAIC_STATUS;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.EAST;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.NORTH;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.SOUTH;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.WEST;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSMOSAIC_DESCRIPTION)
@Documentation(OMSMOSAIC_DOCUMENTATION)
@Author(name = OMSMOSAIC_AUTHORNAMES, contact = OMSMOSAIC_AUTHORCONTACTS)
@Keywords(OMSMOSAIC_KEYWORDS)
@Label(OMSMOSAIC_LABEL)
@Name(OMSMOSAIC_NAME)
@Status(OMSMOSAIC_STATUS)
@License(OMSMOSAIC_LICENSE)
public class OmsMosaic extends HMModel {

    @Description(OMSMOSAIC_IN_FILES_DESCRIPTION)
    @In
    public List<File> inFiles;

    @Description(OMSMOSAIC_IN_COVERAGES_DESCRIPTION)
    @In
    public List<GridCoverage2D> inCoverages;

    @Description(OMSMOSAIC_P_INTERPOLATION_DESCRIPTION)
    @UI("combo:" + NEAREST_NEIGHTBOUR + "," + BILINEAR + "," + BICUBIC)
    @In
    public String pInterpolation = NEAREST_NEIGHTBOUR;

    @Description(OMSMOSAIC_doFindSmallesresolution)
    @In
    public boolean doFindSmallesresolution = false;

    @Description(OMSMOSAIC_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster = null;

    public static final String OMSMOSAIC_DESCRIPTION = "Module for raster patching.";
    public static final String OMSMOSAIC_DOCUMENTATION = "OmsMosaic.html";
    public static final String OMSMOSAIC_KEYWORDS = "OmsMosaic, Raster";
    public static final String OMSMOSAIC_LABEL = RASTERPROCESSING;
    public static final String OMSMOSAIC_NAME = "mosaic";
    public static final int OMSMOSAIC_STATUS = 40;
    public static final String OMSMOSAIC_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSMOSAIC_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSMOSAIC_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSMOSAIC_IN_FILES_DESCRIPTION = "An optional list of map files that have to be patched.";
    public static final String OMSMOSAIC_IN_COVERAGES_DESCRIPTION = "An optional list of rasters that have to be patched.";
    public static final String OMSMOSAIC_P_INTERPOLATION_DESCRIPTION = "The interpolation type to use";
    public static final String OMSMOSAIC_doFindSmallesresolution = "Force the reading of all coverages to find the smalles resolution for the final patched raster.";
    public static final String OMSMOSAIC_OUT_RASTER_DESCRIPTION = "The patched map.";

    private CoordinateReferenceSystem crs;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outRaster == null, doReset)) {
            return;
        }

        if (inFiles == null && inCoverages == null) {
            throw new ModelsIllegalargumentException("No input data have been provided.", this, pm);
        }
        int count = 0;
        List<FileOrRaster> dataList = new ArrayList<>();
        if (inFiles != null) {
            for( File file : inFiles ) {
                dataList.add(new FileOrRaster(file));
                count++;
            }
        }
        if (inCoverages != null) {
            for( GridCoverage2D coverage : inCoverages ) {
                dataList.add(new FileOrRaster(HMRaster.fromGridCoverage(coverage)));
                count++;
            }
        }

        if (dataList.size() < 2) {
            throw new ModelsIllegalargumentException("The patching module needs at least two maps to be patched.", this, pm);
        }

        HMRaster referenceRaster = null;

        pm.beginTask("Calculating final bounds...", count);
        double xRes = 0;
        double yRes = 0;
        Envelope totalEnvelope = new Envelope();
        for( FileOrRaster data : dataList ) {
            Envelope worldEnv;
            if (referenceRaster == null) {
                HMRaster raster = data.getRaster();

                worldEnv = raster.getRegionMap().toEnvelope();
                // take the first as reference
                crs = raster.getCrs();
                referenceRaster = raster;

                RegionMap regionMap = raster.getRegionMap();
                xRes = regionMap.getXres();
                yRes = regionMap.getYres();

                pm.message("Using crs: " + CrsUtilities.getCodeFromCrs(crs));
            } else {
                worldEnv = data.getEnvelope();
            }

            totalEnvelope.expandToInclude(worldEnv);

            pm.worked(1);
        }
        pm.done();

        int endWidth = (int) (totalEnvelope.getWidth() / xRes);
        int endHeight = (int) (totalEnvelope.getHeight() / yRes);
        RegionMap newRegion = RegionMap.fromEnvelopeAndGrid(totalEnvelope, endWidth, endHeight);
        HMRaster outHMRaster = new HMRaster.HMRasterWritableBuilder().setCrs(crs).setName("mosaic").setRegion(newRegion).build();

        pm.message(MessageFormat.format("Output raster will have {0} cols and {1} rows.", endWidth, endHeight));

        int index = 1;
        for( FileOrRaster data : dataList ) {
            HMRaster raster = data.getRaster();

            pm.message("Patch map " + index++);
            outHMRaster.mapRaster(pm, raster, true);

        }
        outHMRaster.applyCountAverage(pm);
        outRaster = outHMRaster.buildCoverage();

    }

    static class FileOrRaster {
        private File file;
        private HMRaster raster;

        public FileOrRaster( File file ) {
            this.file = file;
        }
        public Envelope getEnvelope() throws Exception {
            if (file != null) {
                return OmsRasterReader.readEnvelope(file.getAbsolutePath());
            } else {
                return raster.getRegionMap().toEnvelope();
            }
        }
        public HMRaster getRaster() throws Exception {
            if (file != null) {
                return HMRaster.fromGridCoverage(OmsRasterReader.readRaster(file.getAbsolutePath()));
            }
            return raster;
        }
        public FileOrRaster( HMRaster raster ) {
            this.raster = raster;
        }
    }

}
