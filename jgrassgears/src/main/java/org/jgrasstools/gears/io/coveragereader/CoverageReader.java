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
package org.jgrasstools.gears.io.coveragereader;

import static org.jgrasstools.gears.libs.modules.HMConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.HMConstants.isNovalue;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import oms3.annotations.Role;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.ViewType;
import org.geotools.coverageio.gdal.BaseGDALGridCoverage2DReader;
import org.geotools.coverageio.gdal.aig.AIGReader;
import org.geotools.factory.Hints;
import org.geotools.gce.arcgrid.ArcGridReader;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.HMModel;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.files.FilesFinder;
@Description("Generic geotools coverage reader.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Coverage, Raster, Reading")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class CoverageReader extends HMModel {
    @Description("The coverage file path or a data folder, which will be browsed.")
    @In
    public String file = null;

    @Role(Role.PARAMETER)
    @Description("The file novalue.")
    @In
    public double fileNovalue = -9999.0;

    @Role(Role.PARAMETER)
    @Description("The novalue wanted in the coverage.")
    @In
    public double geodataNovalue = doubleNovalue;

    @Role(Role.PARAMETER)
    @Description("The novalue wanted in the coverage.")
    @In
    public String pType = null;

    @Description("The read output coverage map.")
    @Out
    public GridCoverage2D geodata = null;

    @Description("In case of data folder the list of read output coverage maps.")
    @Out
    public List<GridCoverage2D> geodatalist = new ArrayList<GridCoverage2D>();

    public static final String AIG = "adf";
    public static final String ESRIGRID = "asc";
    public static final String GEOTIFF = "tiff";

    @Execute
    public void process() throws Exception {
        if (!concatOr(geodata == null, geodatalist.size() == 0, doReset)) {
            return;
        }
        if (pType == null) {
            throw new ModelsIllegalargumentException("A coverage type to read has to be supplied.",
                    this.getClass().getSimpleName());
        }

        File mapFile = new File(file);
        if (pType.equals(ESRIGRID)) {
            if (!mapFile.isDirectory()) {
                readArcGrid(mapFile, false);
            } else {
                List<File> filesList = new FilesFinder(mapFile, pType).process();
                for( File file : filesList ) {
                    readArcGrid(file, true);
                }
            }
        } else if (pType.equals(GEOTIFF)) {
            if (!mapFile.isDirectory()) {
                readGeotiff(mapFile, false);
            } else {
                List<File> filesList = new FilesFinder(mapFile, pType).process();
                for( File file : filesList ) {
                    readGeotiff(file, true);
                }
            }
        } else if (pType.equals(AIG) || pType.endsWith("w001001x.adf")) {
            if (!mapFile.isDirectory()) {
                readAig(mapFile, false);
            } else {
                List<File> filesList = new FilesFinder(mapFile, pType).process();
                List<File> decimatedList = new ArrayList<File>();
                for( File file : filesList ) {
                    if (file.getName().equals("w001001x.adf")) {
                        decimatedList.add(file);
                    }
                }
                
                for( File file : decimatedList ) {
                    readAig(file, true);
                }
            }
        } else {
            throw new ModelsIllegalargumentException("Data type not supported: " + pType, this
                    .getClass().getSimpleName());
        }

    }

    private void readAig( File mapFile, boolean addToList ) throws IllegalArgumentException,
            IOException {
        final ImageLayout l = new ImageLayout();
        l.setTileGridXOffset(0).setTileGridYOffset(0).setTileHeight(512).setTileWidth(512);

        Hints hints = new Hints();
        hints.add(new RenderingHints(JAI.KEY_IMAGE_LAYOUT, l));

        final URL url = mapFile.toURI().toURL();
        final Object source = url;
        final BaseGDALGridCoverage2DReader reader = new AIGReader(source, hints);
        GridCoverage2D coverage = (GridCoverage2D) reader.read(null);

        if (addToList) {
            geodatalist.add(coverage);
        } else {
            geodata = coverage;
        }

    }

    private void readGeotiff( File mapFile, boolean addToList ) throws IOException {
        GeoTiffReader geoTiffReader = new GeoTiffReader(new File(file));
        GridCoverage2D coverage = geoTiffReader.read(null);
        coverage = coverage.view(ViewType.GEOPHYSICS);
        if (addToList) {
            geodatalist.add(coverage);
        } else {
            geodata = coverage;
        }
    }

    private void readArcGrid( File mapFile, boolean addToList ) throws IllegalArgumentException,
            IOException {
        ArcGridReader arcGridReader = new ArcGridReader(mapFile);
        GridCoverage2D coverage = arcGridReader.read(null);
        coverage = coverage.view(ViewType.GEOPHYSICS);

        if (isNovalue(fileNovalue) && isNovalue(geodataNovalue)) {
            if (addToList) {
                geodatalist.add(coverage);
            } else {
                geodata = coverage;
            }
            return;
        }
        if (fileNovalue != geodataNovalue) {
            // need to adapt it, for now do it dirty
            HashMap<String, Double> params = CoverageUtilities
                    .getRegionParamsFromGridCoverage(coverage);
            int height = params.get(CoverageUtilities.ROWS).intValue();
            int width = params.get(CoverageUtilities.COLS).intValue();
            WritableRaster tmpWR = CoverageUtilities.createDoubleWritableRaster(width, height,
                    null, null, null);
            WritableRandomIter tmpIter = RandomIterFactory.createWritable(tmpWR, null);
            RenderedImage readRI = coverage.getRenderedImage();
            RandomIter readIter = RandomIterFactory.create(readRI, null);
            for( int r = 0; r < height; r++ ) {
                for( int c = 0; c < width; c++ ) {
                    double value = readIter.getSampleDouble(c, r, 0);

                    if (isNovalue(value) || value == fileNovalue) {
                        tmpIter.setSample(c, r, 0, geodataNovalue);
                    } else {
                        tmpIter.setSample(c, r, 0, value);
                    }
                }
            }
            coverage = CoverageUtilities.buildCoverage("newcoverage", tmpWR, params, coverage //$NON-NLS-1$
                    .getCoordinateReferenceSystem());
        }
        if (addToList) {
            geodatalist.add(coverage);
        } else {
            geodata = coverage;
        }
    }

}
