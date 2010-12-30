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
package org.jgrasstools.gears.io.rasterreader;

import static org.jgrasstools.gears.libs.modules.JGTConstants.AIG;
import static org.jgrasstools.gears.libs.modules.JGTConstants.ESRIGRID;
import static org.jgrasstools.gears.libs.modules.JGTConstants.GEOTIF;
import static org.jgrasstools.gears.libs.modules.JGTConstants.GEOTIFF;
import static org.jgrasstools.gears.libs.modules.JGTConstants.GRASSRASTER;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.COLS;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.EAST;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.NORTH;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.ROWS;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.SOUTH;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.WEST;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.XRES;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.YRES;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.buildCoverage;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.createDoubleWritableRaster;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.createGridGeometryGeneralParameter;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.getRegionParamsFromGridCoverage;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.gridGeometryFromRegionParams;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.makeRegionParamsMap;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.ViewType;
import org.geotools.coverage.processing.Operations;
import org.geotools.coverageio.gdal.BaseGDALGridCoverage2DReader;
import org.geotools.coverageio.gdal.aig.AIGReader;
import org.geotools.factory.Hints;
import org.geotools.gce.arcgrid.ArcGridReader;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.grassraster.GrassCoverageReader;
import org.geotools.gce.grassraster.JGrassMapEnvironment;
import org.geotools.gce.grassraster.JGrassRegion;
import org.geotools.gce.grassraster.format.GrassCoverageFormat;
import org.geotools.gce.grassraster.format.GrassCoverageFormatFactory;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.jgrasstools.gears.io.grasslegacy.GrassLegacyReader;
import org.jgrasstools.gears.io.grasslegacy.utils.GrassLegacyUtilities;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.math.NumericsUtilities;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;

@Description("Generic geotools coverage reader.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Coverage, Raster, Reading")
@Status(Status.CERTIFIED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class RasterReader extends JGTModel {
    @Description("The coverage file path.")
    @In
    public String file = null;

    @Description("The file novalue.")
    @In
    public Double fileNovalue = -9999.0;

    @Description("The novalue wanted in the coverage.")
    @In
    public Double geodataNovalue = doubleNovalue;

    @Description("The optional requested boundary coordinates as array of [n, s, w, e].")
    @In
    public double[] pBounds = null;

    @Description("The optional requested resolution in x and y as [xres, yres].")
    @In
    public double[] pRes = null;

    @Description("The optional requested numer of rows and columns as [rows, cols].")
    @In
    public double[] pRowcol = null;

    @Description("The novalue wanted in the coverage.")
    @In
    public String pType = null;

    @Description("Flag to read only envelope (if true, the output geodata is null).")
    @In
    public boolean doEnvelope = false;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The read output coverage map.")
    @Out
    public GridCoverage2D geodata = null;

    @Description("The original envelope of the coverage.")
    @Out
    public GeneralEnvelope originalEnvelope;

    private GeneralParameterValue[] generalParameter = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(geodata == null, doReset)) {
            return;
        }

        if (pBounds != null && (pRes == null && pRowcol == null)) {
            throw new RuntimeException("If bounds are requested, also a resolution or number of rows/cols has to be supplied.");
        }

        if (pType == null) {
            // try to guess from the extension
            if (file.toLowerCase().endsWith(ESRIGRID)) {
                pType = ESRIGRID;
            } else if (file.toLowerCase().endsWith(AIG)) {
                pType = AIG;
            } else if (file.toLowerCase().endsWith(GEOTIFF) || file.toLowerCase().endsWith(GEOTIF)) {
                pType = GEOTIFF;
            } else if (isGrass(file)) {
                pType = GRASSRASTER;
            } else
                throw new ModelsIllegalargumentException("A coverage type to read has to be supplied.", this.getClass()
                        .getSimpleName());
        }

        File mapFile = new File(file);
        if (pType.equals(ESRIGRID)) {
            readArcGrid(mapFile);
        } else if (pType.equals(GEOTIFF)) {
            readGeotiff(mapFile);
        } else if (pType.equals(AIG) || pType.endsWith("w001001x.adf")) {
            readAig(mapFile);
        } else if (pType.equals(GRASSRASTER)) {
            readGrass(mapFile);
        } else {
            throw new ModelsIllegalargumentException("Data type not supported: " + pType, this.getClass().getSimpleName());
        }

    }

    private boolean isGrass( String path ) {
        File file = new File(path);
        File cellFolderFile = file.getParentFile();
        File mapsetFile = cellFolderFile.getParentFile();
        File windFile = new File(mapsetFile, "WIND");

        return cellFolderFile.getName().toLowerCase().equals("cell") && windFile.exists();
    }

    private void resample() {
        if (pRes == null && pRowcol == null && pBounds == null) {
            return;
        }

        HashMap<String, Double> envelopeParams = getRegionParamsFromGridCoverage(geodata);
        double west = envelopeParams.get(WEST);
        double south = envelopeParams.get(SOUTH);
        double east = envelopeParams.get(EAST);
        double north = envelopeParams.get(NORTH);
        double xres = envelopeParams.get(XRES);
        double yres = envelopeParams.get(YRES);

        if (pBounds == null) {
            pBounds = new double[]{north, south, west, east};
        }

        if (pRes == null && pRowcol == null) {
            // means only bounds are set -> set resolution and recalc rows/cols
            pRes = new double[]{xres, yres};
        }

        double n = pBounds[0];
        double s = pBounds[1];
        double w = pBounds[2];
        double e = pBounds[3];
        if (pRes != null || pRowcol != null) {
            int newRows = 0;
            int newCols = 0;
            if (pRowcol != null) {
                newRows = (int) pRowcol[0];
                newCols = (int) pRowcol[1];
                if (pRes == null) {
                    pRes = new double[2];
                }
                pRes[0] = (e - w) / (double) newCols;
                pRes[1] = (n - s) / (double) newRows;
            } else if (pRes != null) {
                pRowcol = new double[2];
                newRows = (int) Math.round((n - s) / pRes[1]);
                newCols = (int) Math.round((e - w) / pRes[0]);
            }
            pRowcol[0] = newRows;
            pRowcol[1] = newCols;
        }

        HashMap<String, Double> newParams = makeRegionParamsMap(n, s, w, e, pRes[0], pRes[1], (int) pRowcol[1], (int) pRowcol[0]);
        CoordinateReferenceSystem crs = geodata.getCoordinateReferenceSystem();
        GridGeometry2D gg = gridGeometryFromRegionParams(newParams, crs);
        geodata = (GridCoverage2D) Operations.DEFAULT.resample(geodata, crs, gg, null);
    }

    private void readGrass( File mapFile ) throws Exception {
        JGrassMapEnvironment mapEnvironment = new JGrassMapEnvironment(new File(file));
        CoordinateReferenceSystem crs = mapEnvironment.getCoordinateReferenceSystem();
        JGrassRegion jGrassRegion = mapEnvironment.getActiveRegion();
        JGrassRegion fileRegion = mapEnvironment.getFileRegion();

        Envelope env = fileRegion.getEnvelope();
        originalEnvelope = new GeneralEnvelope(new ReferencedEnvelope(env.getMinX(), env.getMaxX(), env.getMinY(), env.getMaxY(),
                crs));

        // if bounds supplied, use them as region
        if (pBounds != null) {
            // n, s, w, e
            double n = pBounds[0];
            double s = pBounds[1];
            double w = pBounds[2];
            double e = pBounds[3];
            if (pRes != null) {
                jGrassRegion = new JGrassRegion(w, e, s, n, pRes[0], pRes[1]);
            } else if (pRowcol != null) {
                jGrassRegion = new JGrassRegion(w, e, s, n, pRowcol[0], pRowcol[1]);
            }
        }

        if (!doEnvelope) {
            int r = jGrassRegion.getRows();
            int c = jGrassRegion.getCols();
            if (!JGTConstants.doesOverFlow(r, c)) {
                if (generalParameter == null) {
                    generalParameter = createGridGeometryGeneralParameter(jGrassRegion.getCols(), jGrassRegion.getRows(),
                            jGrassRegion.getNorth(), jGrassRegion.getSouth(), jGrassRegion.getEast(), jGrassRegion.getWest(), crs);
                }
                GrassCoverageFormat format = new GrassCoverageFormatFactory().createFormat();
                GrassCoverageReader reader = format.getReader(mapEnvironment.getCELL());
                geodata = (GridCoverage2D) reader.read(generalParameter);
            } else {
                GrassLegacyReader reader = new GrassLegacyReader();
                reader.file = file;
                reader.inWindow = GrassLegacyUtilities.jgrassRegion2legacyWindow(jGrassRegion);
                reader.readCoverage();
                geodata = reader.outGC;
            }
            checkNovalues();
        }
    }

    private void readAig( File mapFile ) throws IllegalArgumentException, IOException {
        final ImageLayout l = new ImageLayout();
        l.setTileGridXOffset(0).setTileGridYOffset(0).setTileHeight(512).setTileWidth(512);

        Hints hints = new Hints();
        hints.add(new RenderingHints(JAI.KEY_IMAGE_LAYOUT, l));

        final URL url = mapFile.toURI().toURL();
        final Object source = url;
        final BaseGDALGridCoverage2DReader reader = new AIGReader(source, hints);
        originalEnvelope = reader.getOriginalEnvelope();
        if (!doEnvelope) {
            GridCoverage2D coverage = (GridCoverage2D) reader.read(generalParameter);
            geodata = coverage.view(ViewType.GEOPHYSICS);

            resample();
            checkNovalues();
        }
    }

    private void readGeotiff( File mapFile ) throws IOException {
        GeoTiffReader geoTiffReader = new GeoTiffReader(mapFile);
        originalEnvelope = geoTiffReader.getOriginalEnvelope();
        if (!doEnvelope) {
            GridCoverage2D coverage = geoTiffReader.read(generalParameter);
            geodata = coverage.view(ViewType.GEOPHYSICS);

            resample();
            checkNovalues();
        }
    }

    private void readArcGrid( File mapFile ) throws IllegalArgumentException, IOException {
        ArcGridReader arcGridReader = new ArcGridReader(mapFile);
        originalEnvelope = arcGridReader.getOriginalEnvelope();
        if (!doEnvelope) {
            GridCoverage2D coverage = arcGridReader.read(generalParameter);
            geodata = coverage.view(ViewType.GEOPHYSICS);

            resample();
            checkNovalues();
        }
    }

    private void checkNovalues() {
        // TODO make this nice, this can't be the way
        if (fileNovalue == null || geodataNovalue == null) {
            return;
        }
        if (isNovalue(fileNovalue) && isNovalue(geodataNovalue)) {
            return;
        }
        if (!NumericsUtilities.dEq(fileNovalue, geodataNovalue)) {
            HashMap<String, Double> params = getRegionParamsFromGridCoverage(geodata);
            int height = params.get(ROWS).intValue();
            int width = params.get(COLS).intValue();
            WritableRaster tmpWR = createDoubleWritableRaster(width, height, null, null, null);
            WritableRandomIter tmpIter = RandomIterFactory.createWritable(tmpWR, null);
            RenderedImage readRI = geodata.getRenderedImage();
            RandomIter readIter = RandomIterFactory.create(readRI, null);
            int minX = readRI.getMinX();
            int minY = readRI.getMinY();
            for( int r = 0; r < height; r++ ) {
                for( int c = 0; c < width; c++ ) {
                    double value = readIter.getSampleDouble(c + minX, r + minY, 0);
                    if (isNovalue(value) || value == fileNovalue) {
                        tmpIter.setSample(c, r, 0, geodataNovalue);
                    } else {
                        tmpIter.setSample(c, r, 0, value);
                    }
                }
            }
            geodata = buildCoverage("newcoverage", tmpWR, params, geodata.getCoordinateReferenceSystem());
        }
    }

    /**
     * Utility method to quickly read a grid in default mode.
     * 
     * @param path the path to the file.
     * @return the read coverage.
     * @throws Exception
     */
    public static GridCoverage2D readCoverage( String path ) throws Exception {
        RasterReader reader = new RasterReader();
        reader.file = path;
        reader.process();
        GridCoverage2D geodata = reader.geodata;
        return geodata;
    }

}
