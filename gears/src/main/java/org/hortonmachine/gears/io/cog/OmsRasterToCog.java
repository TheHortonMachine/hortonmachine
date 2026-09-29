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
package org.hortonmachine.gears.io.cog;

import java.io.File;

import org.hortonmachine.gears.io.cog.HMCogWriter.Compression;
import org.hortonmachine.gears.io.cog.HMCogWriter.Resampling;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;

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

@Description(OmsRasterToCog.DESCRIPTION)
@Author(name = OmsRasterToCog.AUTHORNAMES, contact = OmsRasterToCog.AUTHORCONTACTS)
@Keywords(OmsRasterToCog.KEYWORDS)
@Label(OmsRasterToCog.LABEL)
@Name(OmsRasterToCog.NAME)
@Status(OmsRasterToCog.STATUS)
@License(OmsRasterToCog.LICENSE)
public class OmsRasterToCog extends HMModel {
    @Description(inRaster_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster;

    @Description(pResampling_DESCRIPTION)
    @UI("combo:" + AUTO + "," + AVERAGE + "," + NEAREST + "," + MODE)
    @In
    public String pResampling = AUTO;

    @Description(pCompression_DESCRIPTION)
    @UI("combo:" + DEFLATE + "," + LZW)
    @In
    public String pCompression = DEFLATE;

    @Description(pDeflateLevel_DESCRIPTION)
    @In
    public int pDeflateLevel = 6;

    @Description(doPredictor_DESCRIPTION)
    @In
    public boolean doPredictor = true;

    @Description(pTileSize_DESCRIPTION)
    @In
    public int pTileSize = 512;

    @Description(pThreads_DESCRIPTION)
    @In
    public int pThreads = getDefaultThreadsNum();

    @Description(outCog_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outCog;

    public static final String AUTO = "auto";
    public static final String AVERAGE = "average";
    public static final String NEAREST = "nearest";
    public static final String MODE = "mode";
    public static final String DEFLATE = "deflate";
    public static final String LZW = "lzw";

    public static final String DESCRIPTION = "Converts a raster to a Cloud Optimized GeoTIFF (COG): tiled, with overviews, directories before the data and lossless compression. Large GeoTIFFs are read by windows, so they don't need to fit in memory.";
    public static final String KEYWORDS = "IO, COG, Cloud Optimized GeoTIFF, Raster, Overviews, Writing";
    public static final String LABEL = HMConstants.RASTERPROCESSING;
    public static final String NAME = "rastertocog";
    public static final int STATUS = Status.EXPERIMENTAL;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String AUTHORNAMES = "Andrea Antonello";
    public static final String AUTHORCONTACTS = "https://g-ant.eu";
    public static final String inRaster_DESCRIPTION = "The raster to convert.";
    public static final String pResampling_DESCRIPTION = "How the overviews are computed from 2x2 cells: auto (average for floating point data, mode for integer data like codes or classes), average, nearest (upper left cell) or mode (most frequent value).";
    public static final String pCompression_DESCRIPTION = "The lossless compression: deflate (default) or lzw.";
    public static final String pDeflateLevel_DESCRIPTION = "The deflate level, from 1 (fastest) to 9 (smallest), default is 6.";
    public static final String doPredictor_DESCRIPTION = "Use the TIFF predictor that fits the data type (horizontal for integers, floating point for floats), usually giving smaller files (default is true).";
    public static final String pTileSize_DESCRIPTION = "The size of the tiles, a multiple of 16 (default is 512).";
    public static final String pThreads_DESCRIPTION = "The number of threads compressing the tiles (defaults to the number of available processors).";
    public static final String outCog_DESCRIPTION = "The output COG file.";

    @Execute
    public void process() throws Exception {
        checkNull(inRaster, outCog);
        if (new File(inRaster).getAbsoluteFile().equals(new File(outCog).getAbsoluteFile())) {
            throw new ModelsIllegalargumentException("The output file can't be the input file.", this, pm);
        }
        if (pDeflateLevel < 1 || pDeflateLevel > 9) {
            throw new ModelsIllegalargumentException("The deflate level has to be between 1 and 9.", this, pm);
        }
        Resampling resampling;
        switch( pResampling == null ? AUTO : pResampling.toLowerCase() ) {
        case AVERAGE:
            resampling = Resampling.AVERAGE;
            break;
        case NEAREST:
            resampling = Resampling.NEAREST;
            break;
        case MODE:
            resampling = Resampling.MODE;
            break;
        case AUTO:
            resampling = Resampling.AUTO;
            break;
        default:
            throw new ModelsIllegalargumentException("Unknown resampling: " + pResampling, this, pm);
        }
        Compression compression;
        if (pCompression == null || pCompression.equalsIgnoreCase(DEFLATE)) {
            compression = Compression.DEFLATE;
        } else if (pCompression.equalsIgnoreCase(LZW)) {
            compression = Compression.LZW;
        } else {
            throw new ModelsIllegalargumentException("Unknown compression: " + pCompression, this, pm);
        }

        new HMCogWriter().setResampling(resampling).setCompression(compression).setDeflateLevel(pDeflateLevel)
                .setUsePredictor(doPredictor).setTileSize(pTileSize).setThreads(pThreads).convert2Cog(pm, inRaster, outCog);
    }
}
