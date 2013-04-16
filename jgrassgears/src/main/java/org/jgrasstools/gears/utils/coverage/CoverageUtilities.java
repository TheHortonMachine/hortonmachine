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
package org.jgrasstools.gears.utils.coverage;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doesOverFlow;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RasterFactory;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.parameter.Parameter;
import org.geotools.process.ProcessException;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.jgrasstools.gears.io.grasslegacy.GrassLegacyGridCoverage2D;
import org.jgrasstools.gears.io.grasslegacy.GrassLegacyRandomIter;
import org.jgrasstools.gears.io.grasslegacy.GrassLegacyWritableRaster;
import org.jgrasstools.gears.io.grasslegacy.utils.Window;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.features.FastLiteShape;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

/**
 * <p>
 * A class of utilities bound to raster analysis
 * </p>
 * 
 * @author Andrea Antonello - www.hydrologis.com
 * @since 0.1
 */
public class CoverageUtilities {
    public static final String NORTH = "NORTH"; //$NON-NLS-1$
    public static final String SOUTH = "SOUTH"; //$NON-NLS-1$
    public static final String WEST = "WEST"; //$NON-NLS-1$
    public static final String EAST = "EAST"; //$NON-NLS-1$
    public static final String XRES = "XRES"; //$NON-NLS-1$
    public static final String YRES = "YRES"; //$NON-NLS-1$
    public static final String ROWS = "ROWS"; //$NON-NLS-1$
    public static final String COLS = "COLS"; //$NON-NLS-1$

    /**
     * Creates a {@link RandomIter} for the given {@link GridCoverage2D}.
     * 
     * <p>It is important to use this method since it supports also 
     * large GRASS rasters.
     * 
     * @param coverage the coverage on which to wrap a {@link RandomIter}.
     * @return the iterator.
     */
    public static RandomIter getRandomIterator( GridCoverage2D coverage ) {
        if (coverage instanceof GrassLegacyGridCoverage2D) {
            GrassLegacyGridCoverage2D grassGC = (GrassLegacyGridCoverage2D) coverage;
            GrassLegacyRandomIter iter = new GrassLegacyRandomIter(grassGC.getData());
            return iter;
        }
        RenderedImage renderedImage = coverage.getRenderedImage();
        RandomIter iter = RandomIterFactory.create(renderedImage, null);
        return iter;
    }

    /**
     * Creates a {@link WritableRandomIter}.
     * 
     * <p>It is important to use this method since it supports also 
     * large GRASS rasters.
     * 
     * <p>If the size would throw an integer overflow, a {@link GrassLegacyRandomIter}
     * will be proposed to try to save the saveable.
     * 
     * @param raster the coverage on which to wrap a {@link WritableRandomIter}.
     * @return the iterator.
     */
    public static WritableRandomIter getWritableRandomIterator( int width, int height ) {
        if (doesOverFlow(width, height)) {
            GrassLegacyRandomIter iter = new GrassLegacyRandomIter(new double[height][width]);
            return iter;
        }
        WritableRaster pitRaster = CoverageUtilities.createDoubleWritableRaster(width, height, null, null, null);
        WritableRandomIter iter = RandomIterFactory.createWritable(pitRaster, null);
        return iter;
    }

    /**
     * Creates a {@link WritableRandomIter}.
     * 
     * <p>It is important to use this method since it supports also 
     * large GRASS rasters.
     * 
     * <p>If the size would throw an integer overflow, a {@link GrassLegacyRandomIter}
     * will be proposed to try to save the saveable.
     * 
     * @param raster the coverage on which to wrap a {@link WritableRandomIter}.
     * @return the iterator.
     */
    public static WritableRandomIter getWritableRandomIterator( WritableRaster raster ) {
        if (raster instanceof GrassLegacyWritableRaster) {
            GrassLegacyWritableRaster wRaster = (GrassLegacyWritableRaster) raster;
            double[][] data = wRaster.getData();
            getWritableRandomIterator(data[0].length, data.length);
        }
        WritableRandomIter iter = RandomIterFactory.createWritable(raster, null);
        return iter;
    }

    /**
     * Creates a {@link WritableRaster writable raster}.
     * 
     * @param width width of the raster to create.
     * @param height height of the raster to create.
     * @param dataClass data type for the raster. If <code>null</code>, defaults to double.
     * @param sampleModel the samplemodel to use. If <code>null</code>, defaults to 
     *                  <code>new ComponentSampleModel(dataType, width, height, 1, width, new int[]{0});</code>.
     * @param value value to which to set the raster to. If null, the default of the raster creation is 
     *                  used, which is 0.
     * @return a {@link WritableRaster writable raster}.
     */
    public static WritableRaster createDoubleWritableRaster( int width, int height, Class< ? > dataClass,
            SampleModel sampleModel, Double value ) {
        int dataType = DataBuffer.TYPE_DOUBLE;
        if (dataClass != null) {
            if (dataClass.isAssignableFrom(Integer.class)) {
                dataType = DataBuffer.TYPE_INT;
            } else if (dataClass.isAssignableFrom(Float.class)) {
                dataType = DataBuffer.TYPE_FLOAT;
            } else if (dataClass.isAssignableFrom(Byte.class)) {
                dataType = DataBuffer.TYPE_BYTE;
            }
        }

        if (!doesOverFlow(width, height)) {
            if (sampleModel == null) {
                sampleModel = new ComponentSampleModel(dataType, width, height, 1, width, new int[]{0});
            }

            WritableRaster raster = RasterFactory.createWritableRaster(sampleModel, null);
            if (value != null) {
                // autobox only once
                double v = value;

                for( int y = 0; y < height; y++ ) {
                    for( int x = 0; x < width; x++ ) {
                        raster.setSample(x, y, 0, v);
                    }
                }
            }
            return raster;
        } else {
            WritableRaster raster = new GrassLegacyWritableRaster(new double[height][width]);
            return raster;
        }
    }

    /**
     * Creates a new {@link GridCoverage2D} using an existing as template.
     * 
     * @param template the template to use.
     * @param value the value to set the new raster to, if not <code>null</code>.
     * @return the new coverage.
     */
    public static GridCoverage2D createCoverageFromTemplate( GridCoverage2D template, Double value ) {
        RegionMap regionMap = getRegionParamsFromGridCoverage(template);

        double west = regionMap.getWest();
        double south = regionMap.getSouth();
        double east = regionMap.getEast();
        double north = regionMap.getNorth();
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        ComponentSampleModel sampleModel = new ComponentSampleModel(DataBuffer.TYPE_DOUBLE, cols, rows, 1, cols, new int[]{0});

        WritableRaster raster = RasterFactory.createWritableRaster(sampleModel, null);
        if (value != null) {
            // autobox only once
            double v = value;
            for( int y = 0; y < rows; y++ ) {
                for( int x = 0; x < cols; x++ ) {
                    raster.setSample(x, y, 0, v);
                }
            }
        }
        Envelope2D writeEnvelope = new Envelope2D(template.getCoordinateReferenceSystem(), west, south, east - west, north
                - south);
        GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
        GridCoverage2D coverage2D = factory.create("newraster", raster, writeEnvelope);
        return coverage2D;
    }

    /**
     * Get the parameters of the region covered by the {@link GridCoverage2D coverage}. 
     * 
     * @param gridCoverage the coverage.
     * @return the {@link HashMap map} of parameters. ( {@link #NORTH} and the 
     *          other static vars can be used to retrieve them.
     */
    public static RegionMap getRegionParamsFromGridCoverage( GridCoverage2D gridCoverage ) {
        RegionMap envelopeParams = new RegionMap();

        Envelope envelope = gridCoverage.getEnvelope();

        DirectPosition lowerCorner = envelope.getLowerCorner();
        double[] westSouth = lowerCorner.getCoordinate();
        DirectPosition upperCorner = envelope.getUpperCorner();
        double[] eastNorth = upperCorner.getCoordinate();

        GridGeometry2D gridGeometry = gridCoverage.getGridGeometry();
        GridEnvelope2D gridRange = gridGeometry.getGridRange2D();
        int height = gridRange.height;
        int width = gridRange.width;

        AffineTransform gridToCRS = (AffineTransform) gridGeometry.getGridToCRS();
        double xRes = XAffineTransform.getScaleX0(gridToCRS);
        double yRes = XAffineTransform.getScaleY0(gridToCRS);

        envelopeParams.put(NORTH, eastNorth[1]);
        envelopeParams.put(SOUTH, westSouth[1]);
        envelopeParams.put(WEST, westSouth[0]);
        envelopeParams.put(EAST, eastNorth[0]);
        envelopeParams.put(XRES, xRes);
        envelopeParams.put(YRES, yRes);
        envelopeParams.put(ROWS, (double) height);
        envelopeParams.put(COLS, (double) width);

        return envelopeParams;
    }

    /**
     * Get the array of region parameters covered by the {@link GridCoverage2D coverage}. 
     * 
     * @param gridCoverage the coverage.
     * @return the array of region parameters as [n, s, w, e, xres, yres, cols, rows]
     */
    public static double[] getRegionArrayFromGridCoverage( GridCoverage2D gridCoverage ) {
        Envelope envelope = gridCoverage.getEnvelope();
        DirectPosition lowerCorner = envelope.getLowerCorner();
        double[] westSouth = lowerCorner.getCoordinate();
        DirectPosition upperCorner = envelope.getUpperCorner();
        double[] eastNorth = upperCorner.getCoordinate();

        GridGeometry2D gridGeometry = gridCoverage.getGridGeometry();
        GridEnvelope2D gridRange = gridGeometry.getGridRange2D();
        int height = gridRange.height;
        int width = gridRange.width;

        AffineTransform gridToCRS = (AffineTransform) gridGeometry.getGridToCRS();
        double xRes = XAffineTransform.getScaleX0(gridToCRS);
        double yRes = XAffineTransform.getScaleY0(gridToCRS);

        double[] params = new double[]{eastNorth[1], westSouth[1], westSouth[0], eastNorth[0], xRes, yRes, width, height};

        return params;
    }

    /**
     * Get the array of rows and cols. 
     * 
     * @param gridCoverage the coverage.
     * @return the array as [cols, rows]
     */
    public static int[] getRegionColsRows( GridCoverage2D gridCoverage ) {
        GridGeometry2D gridGeometry = gridCoverage.getGridGeometry();
        GridEnvelope2D gridRange = gridGeometry.getGridRange2D();
        int height = gridRange.height;
        int width = gridRange.width;
        int[] params = new int[]{width, height};
        return params;
    }

    /**
     * Get the cols and rows ranges to use to loop the original gridcoverage.
     * 
     * @param gridCoverage the coverage.
     * @param subregion the sub region of the coverage to get the cols and rows to loop on.
     * @return the array of looping values in the form [minCol, maxCol, minRow, maxRow].
     * @throws Exception
     */
    public static int[] getLoopColsRowsForSubregion( GridCoverage2D gridCoverage, Envelope2D subregion ) throws Exception {
        GridGeometry2D gridGeometry = gridCoverage.getGridGeometry();
        GridEnvelope2D subRegionGrid = gridGeometry.worldToGrid(subregion);
        int minCol = subRegionGrid.x;
        int maxCol = subRegionGrid.x + subRegionGrid.width;
        int minRow = subRegionGrid.y;
        int maxRow = subRegionGrid.y + subRegionGrid.height;
        return new int[]{minCol, maxCol, minRow, maxRow};
    }

    public static HashMap<String, Double> generalParameterValues2RegionParamsMap( GeneralParameterValue[] params ) {
        GridGeometry2D gg = null;
        if (params != null) {
            for( int i = 0; i < params.length; i++ ) {
                final ParameterValue< ? > param = (ParameterValue< ? >) params[i];
                final String name = param.getDescriptor().getName().getCode();
                if (name.equals(AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString())) {
                    gg = (GridGeometry2D) param.getValue();
                    break;
                }
            }
        }
        if (gg == null) {
            throw new IllegalArgumentException("No gridgeometry present"); //$NON-NLS-1$
        }
        HashMap<String, Double> regionParams = gridGeometry2RegionParamsMap(gg);
        return regionParams;
    }

    public static RegionMap gridGeometry2RegionParamsMap( GridGeometry2D gridGeometry ) {
        RegionMap envelopeParams = new RegionMap();

        Envelope envelope = gridGeometry.getEnvelope2D();
        DirectPosition lowerCorner = envelope.getLowerCorner();
        double[] westSouth = lowerCorner.getCoordinate();
        DirectPosition upperCorner = envelope.getUpperCorner();
        double[] eastNorth = upperCorner.getCoordinate();

        GridEnvelope2D gridRange = gridGeometry.getGridRange2D();
        int height = gridRange.height;
        int width = gridRange.width;

        AffineTransform gridToCRS = (AffineTransform) gridGeometry.getGridToCRS();
        double xRes = XAffineTransform.getScaleX0(gridToCRS);
        double yRes = XAffineTransform.getScaleY0(gridToCRS);

        envelopeParams.put(NORTH, eastNorth[1]);
        envelopeParams.put(SOUTH, westSouth[1]);
        envelopeParams.put(WEST, westSouth[0]);
        envelopeParams.put(EAST, eastNorth[0]);
        envelopeParams.put(XRES, xRes);
        envelopeParams.put(YRES, yRes);
        envelopeParams.put(ROWS, (double) height);
        envelopeParams.put(COLS, (double) width);

        return envelopeParams;
    }

    public static RegionMap makeRegionParamsMap( double north, double south, double west, double east, double xRes, double yRes,
            int width, int height ) {
        RegionMap envelopeParams = new RegionMap();
        envelopeParams.put(NORTH, north);
        envelopeParams.put(SOUTH, south);
        envelopeParams.put(WEST, west);
        envelopeParams.put(EAST, east);
        envelopeParams.put(XRES, xRes);
        envelopeParams.put(YRES, yRes);
        envelopeParams.put(ROWS, (double) height);
        envelopeParams.put(COLS, (double) width);
        return envelopeParams;
    }

    public static GridGeometry2D gridGeometryFromRegionParams( HashMap<String, Double> envelopeParams,
            CoordinateReferenceSystem crs ) {
        double west = envelopeParams.get(WEST);
        double south = envelopeParams.get(SOUTH);
        double east = envelopeParams.get(EAST);
        double north = envelopeParams.get(NORTH);
        int rows = envelopeParams.get(ROWS).intValue();
        int cols = envelopeParams.get(COLS).intValue();

        return gridGeometryFromRegionValues(north, south, east, west, cols, rows, crs);
    }

    public static GridGeometry2D gridGeometryFromRegionValues( double north, double south, double east, double west, int cols,
            int rows, CoordinateReferenceSystem crs ) {
        Envelope envelope = new Envelope2D(crs, west, south, east - west, north - south);
        GridEnvelope2D gridRange = new GridEnvelope2D(0, 0, cols, rows);
        GridGeometry2D gridGeometry2D = new GridGeometry2D(gridRange, envelope);
        return gridGeometry2D;
    }

    /**
     * Utility method to create read parameters for {@link GridCoverageReader} 
     * 
     * @param width the needed number of columns.
     * @param height the needed number of columns.
     * @param north the northern boundary.
     * @param south the southern boundary.
     * @param east the eastern boundary.
     * @param west the western boundary.
     * @param crs the {@link CoordinateReferenceSystem}. Can be null, even if it should not.
     * @return the {@link GeneralParameterValue array of parameters}.
     */
    public static GeneralParameterValue[] createGridGeometryGeneralParameter( int width, int height, double north, double south,
            double east, double west, CoordinateReferenceSystem crs ) {
        GeneralParameterValue[] readParams = new GeneralParameterValue[1];
        Parameter<GridGeometry2D> readGG = new Parameter<GridGeometry2D>(AbstractGridFormat.READ_GRIDGEOMETRY2D);
        GridEnvelope2D gridEnvelope = new GridEnvelope2D(0, 0, width, height);
        Envelope env;
        if (crs != null) {
            env = new ReferencedEnvelope(west, east, south, north, crs);
        } else {
            DirectPosition2D minDp = new DirectPosition2D(west, south);
            DirectPosition2D maxDp = new DirectPosition2D(east, north);
            env = new Envelope2D(minDp, maxDp);
        }
        readGG.setValue(new GridGeometry2D(gridEnvelope, env));
        readParams[0] = readGG;

        return readParams;
    }

    /**
     * Utility method to create read parameters for {@link GridCoverageReader} 
     * 
     * @param xres the X resolution.
     * @param yres the Y resolution.
     * @param north the northern boundary.
     * @param south the southern boundary.
     * @param east the eastern boundary.
     * @param west the western boundary.
     * @param crs the {@link CoordinateReferenceSystem}. Can be null, even if it should not.  
     * @return the {@link GeneralParameterValue array of parameters}.
     */
    public static GeneralParameterValue[] createGridGeometryGeneralParameter( double xres, double yres, double north,
            double south, double east, double west, CoordinateReferenceSystem crs ) {
        // make sure the resolution gives integer rows and cols
        int height = (int) Math.round((north - south) / yres);
        if (height < 1)
            height = 1;
        int width = (int) Math.round((east - west) / xres);
        if (width < 1)
            width = 1;

        GeneralParameterValue[] generalParameter = createGridGeometryGeneralParameter(width, height, north, south, east, west,
                crs);

        return generalParameter;
    }

    /**
     * Create a {@link WritableRaster} from a double matrix.
     * 
     * @param matrix the matrix to take the data from.
     * @param matrixIsRowCol a flag to tell if the matrix has rowCol or colRow order.
     * @return the produced raster.
     */
    public static WritableRaster createWritableRasterFromMatrix( double[][] matrix, boolean matrixIsRowCol ) {
        int height = matrix.length;
        int width = matrix[0].length;
        if (!matrixIsRowCol) {
            int tmp = height;
            height = width;
            width = tmp;
        }
        WritableRaster writableRaster = createDoubleWritableRaster(width, height, null, null, null);

        WritableRandomIter rasterIter = RandomIterFactory.createWritable(writableRaster, null);
        for( int x = 0; x < width; x++ ) {
            for( int y = 0; y < height; y++ ) {
                if (matrixIsRowCol) {
                    rasterIter.setSample(x, y, 0, matrix[y][x]);
                } else {
                    rasterIter.setSample(x, y, 0, matrix[x][y]);
                }
            }
        }
        rasterIter.done();

        return writableRaster;
    }

    /**
     * Create a {@link WritableRaster} from a float matrix.
     * 
     * @param matrix the matrix to take the data from.
     * @param matrixIsRowCol a flag to tell if the matrix has rowCol or colRow order.
     * @return the produced raster.
     */
    public static WritableRaster createWritableRasterFromMatrix( float[][] matrix, boolean matrixIsRowCol ) {
        int height = matrix.length;
        int width = matrix[0].length;
        if (!matrixIsRowCol) {
            int tmp = height;
            height = width;
            width = tmp;
        }
        WritableRaster writableRaster = createDoubleWritableRaster(width, height, null, null, null);

        WritableRandomIter disckRandomIter = RandomIterFactory.createWritable(writableRaster, null);
        for( int x = 0; x < width; x++ ) {
            for( int y = 0; y < height; y++ ) {
                if (matrixIsRowCol) {
                    disckRandomIter.setSample(x, y, 0, matrix[y][x]);
                } else {
                    disckRandomIter.setSample(x, y, 0, matrix[x][y]);
                }
            }
        }
        disckRandomIter.done();

        return writableRaster;
    }

    public static WritableRaster createWritableRasterFromMatrix( int[][] matrix, boolean matrixIsRowCol ) {
        int height = matrix.length;
        int width = matrix[0].length;
        if (!matrixIsRowCol) {
            int tmp = height;
            height = width;
            width = tmp;
        }
        WritableRaster writableRaster = createDoubleWritableRaster(width, height, null, null, null);

        WritableRandomIter disckRandomIter = RandomIterFactory.createWritable(writableRaster, null);
        for( int x = 0; x < width; x++ ) {
            for( int y = 0; y < height; y++ ) {
                if (matrixIsRowCol) {
                    disckRandomIter.setSample(x, y, 0, matrix[y][x]);
                } else {
                    disckRandomIter.setSample(x, y, 0, matrix[x][y]);
                }
            }
        }
        disckRandomIter.done();

        return writableRaster;
    }

    /**
     * Create a {@link WritableRaster} from a int array.
     * 
     * @param width the width of the raster to create.
     * @param height the height of the raster to create.
     * @param pixels the array of data.
     * @return the produced raster.
     */
    public static WritableRaster createWritableRasterFromArray( int width, int height, int[] pixels ) {
        WritableRaster writableRaster = createDoubleWritableRaster(width, height, null, null, null);
        int index = 0;
        for( int y = 0; y < height; y++ ) {
            for( int x = 0; x < width; x++ ) {
                double value = (double) pixels[index];
                if (value == 0) {
                    value = doubleNovalue;
                }
                writableRaster.setSample(x, y, 0, value);
                index++;
            }
        }
        return writableRaster;
    }

    /**
     * Creates a {@link GridCoverage2D coverage} from a double[][] matrix and the necessary geographic Information.
     * 
     * @param name the name of the coverage.
     * @param dataMatrix the matrix containing the data.
     * @param envelopeParams the map of boundary parameters.
     * @param crs the {@link CoordinateReferenceSystem}.
     * @param matrixIsRowCol a flag to tell if the matrix has rowCol or colRow order.
     * @return the {@link GridCoverage2D coverage}.
     */
    public static GridCoverage2D buildCoverage( String name, double[][] dataMatrix, HashMap<String, Double> envelopeParams,
            CoordinateReferenceSystem crs, boolean matrixIsRowCol ) {
        WritableRaster writableRaster = createWritableRasterFromMatrix(dataMatrix, matrixIsRowCol);
        return buildCoverage(name, writableRaster, envelopeParams, crs);
    }

    /**
     * Creates a {@link GridCoverage2D coverage} from a float[][] matrix and the necessary geographic Information.
     * 
     * @param name the name of the coverage.
     * @param dataMatrix the matrix containing the data.
     * @param envelopeParams the map of boundary parameters.
     * @param crs the {@link CoordinateReferenceSystem}.
     * @param matrixIsRowCol a flag to tell if the matrix has rowCol or colRow order.
     * @return the {@link GridCoverage2D coverage}.
     */
    public static GridCoverage2D buildCoverage( String name, float[][] dataMatrix, HashMap<String, Double> envelopeParams,
            CoordinateReferenceSystem crs, boolean matrixIsRowCol ) {
        WritableRaster writableRaster = createWritableRasterFromMatrix(dataMatrix, matrixIsRowCol);
        return buildCoverage(name, writableRaster, envelopeParams, crs);
    }

    public static GridCoverage2D buildCoverage( String name, int[][] dataMatrix, HashMap<String, Double> envelopeParams,
            CoordinateReferenceSystem crs, boolean matrixIsRowCol ) {
        WritableRaster writableRaster = createWritableRasterFromMatrix(dataMatrix, matrixIsRowCol);
        return buildCoverage(name, writableRaster, envelopeParams, crs);
    }

    /**
     * Creates a {@link GridCoverage2D coverage} from the {@link RenderedImage image} and the necessary geographic Information.
     * 
     * @param name the name of the coverage.
     * @param renderedImage the image containing the data.
     * @param envelopeParams the map of boundary parameters.
     * @param crs the {@link CoordinateReferenceSystem}.
     * @return the {@link GridCoverage2D coverage}.
     */
    public static GridCoverage2D buildCoverage( String name, RenderedImage renderedImage, HashMap<String, Double> envelopeParams,
            CoordinateReferenceSystem crs ) {

        double west = envelopeParams.get(WEST);
        double south = envelopeParams.get(SOUTH);
        double east = envelopeParams.get(EAST);
        double north = envelopeParams.get(NORTH);
        Envelope2D writeEnvelope = new Envelope2D(crs, west, south, east - west, north - south);
        GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);

        GridCoverage2D coverage2D = factory.create(name, renderedImage, writeEnvelope);
        return coverage2D;
    }

    /**
     * Creates a {@link GridCoverage2D coverage} from the {@link WritableRaster writable raster} and the necessary geographic Information.
     * 
     * @param name the name of the coverage.
     * @param writableRaster the raster containing the data.
     * @param envelopeParams the map of boundary parameters.
     * @param crs the {@link CoordinateReferenceSystem}.
     * @return the {@link GridCoverage2D coverage}.
     */
    public static GridCoverage2D buildCoverage( String name, WritableRaster writableRaster,
            HashMap<String, Double> envelopeParams, CoordinateReferenceSystem crs ) {
        if (writableRaster instanceof GrassLegacyWritableRaster) {
            GrassLegacyWritableRaster wRaster = (GrassLegacyWritableRaster) writableRaster;
            double west = envelopeParams.get(WEST);
            double south = envelopeParams.get(SOUTH);
            double east = envelopeParams.get(EAST);
            double north = envelopeParams.get(NORTH);
            int rows = envelopeParams.get(ROWS).intValue();
            int cols = envelopeParams.get(COLS).intValue();
            Window window = new Window(west, east, south, north, rows, cols);
            GrassLegacyGridCoverage2D coverage2D = new GrassLegacyGridCoverage2D(window, wRaster.getData(), crs);
            return coverage2D;
        } else {
            double west = envelopeParams.get(WEST);
            double south = envelopeParams.get(SOUTH);
            double east = envelopeParams.get(EAST);
            double north = envelopeParams.get(NORTH);
            Envelope2D writeEnvelope = new Envelope2D(crs, west, south, east - west, north - south);
            GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);

            GridCoverage2D coverage2D = factory.create(name, writableRaster, writeEnvelope);
            return coverage2D;
        }
    }

    /**
     * Creates a useless {@link GridCoverage2D} that might be usefull as placeholder.
     * 
     * @return the dummy grod coverage.
     */
    public static GridCoverage2D buildDummyCoverage() {
        HashMap<String, Double> envelopeParams = new HashMap<String, Double>();
        envelopeParams.put(NORTH, 1.0);
        envelopeParams.put(SOUTH, 0.0);
        envelopeParams.put(WEST, 0.0);
        envelopeParams.put(EAST, 1.0);
        envelopeParams.put(XRES, 1.0);
        envelopeParams.put(YRES, 1.0);
        envelopeParams.put(ROWS, 1.0);
        envelopeParams.put(COLS, 1.0);
        double[][] dataMatrix = new double[1][1];
        dataMatrix[0][0] = 0;
        WritableRaster writableRaster = createWritableRasterFromMatrix(dataMatrix, true);
        return buildCoverage("dummy", writableRaster, envelopeParams, DefaultGeographicCRS.WGS84); //$NON-NLS-1$
    }

    /**
     * Creates a compatible {@link WritableRaster} from a {@link RenderedImage}.
     * 
     * @param renderedImage the image to convert.
     * @param nullBorders a flag that indicates if the borders should be set to null.
     * @return the converted writable raster.
     */
    public static WritableRaster renderedImage2WritableRaster( RenderedImage renderedImage, boolean nullBorders ) {
        int width = renderedImage.getWidth();
        int height = renderedImage.getHeight();

        Raster data = renderedImage.getData();
        WritableRaster writableRaster = data.createCompatibleWritableRaster();
        writableRaster.setDataElements(0, 0, data);
        if (nullBorders) {
            for( int c = 0; c < width; c++ ) {
                writableRaster.setSample(c, 0, 0, doubleNovalue);
                writableRaster.setSample(c, height - 1, 0, doubleNovalue);
            }
            for( int r = 0; r < height; r++ ) {
                writableRaster.setSample(0, r, 0, doubleNovalue);
                writableRaster.setSample(width - 1, r, 0, doubleNovalue);
            }
        }

        return writableRaster;
    }

    /**
     * Transform a rendered image in its array representation.
     * 
     * @param renderedImage the rendered image to transform.
     * @return the array holding the data.
     */
    public static double[] renderedImage2DoubleArray( RenderedImage renderedImage ) {
        int width = renderedImage.getWidth();
        int height = renderedImage.getHeight();

        double[] values = new double[width * height];
        RandomIter imageIter = RandomIterFactory.create(renderedImage, null);
        int index = 0;;
        for( int x = 0; x < width; x++ ) {
            for( int y = 0; y < height; y++ ) {
                double sample = imageIter.getSampleDouble(x, y, 0);
                values[index++] = sample;
            }
        }
        imageIter.done();
        return values;
    }

    /**
     * Transform a double values rendered image in its integer array representation by scaling the values.
     * 
     * @param renderedImage the rendered image to transform.
     * @param multiply value by which to multiply the values before casting to integer.
     * @return the array holding the data.
     */
    public static int[] renderedImage2IntegerArray( RenderedImage renderedImage, double multiply ) {
        int width = renderedImage.getWidth();
        int height = renderedImage.getHeight();

        int[] values = new int[width * height];
        RandomIter imageIter = RandomIterFactory.create(renderedImage, null);
        int index = 0;;
        for( int x = 0; x < width; x++ ) {
            for( int y = 0; y < height; y++ ) {
                double sample = imageIter.getSampleDouble(x, y, 0);
                sample = sample * multiply;
                values[index++] = (int) sample;
            }
        }
        imageIter.done();
        return values;
    }

    /**
     * Transform a double values rendered image in its byte array.
     * 
     * <p>No check is done if the double value fits in a byte.</p> 
     * 
     * @param renderedImage the rendered image to transform.
     * @param doRowsThenCols if <code>true</code>, rows are processed in the outer loop.
     * @return the array holding the data.
     */
    public static byte[] renderedImage2ByteArray( RenderedImage renderedImage, boolean doRowsThenCols ) {
        int width = renderedImage.getWidth();
        int height = renderedImage.getHeight();

        byte[] values = new byte[width * height];
        RandomIter imageIter = RandomIterFactory.create(renderedImage, null);
        int index = 0;
        if (doRowsThenCols) {
            for( int y = 0; y < height; y++ ) {
                for( int x = 0; x < width; x++ ) {
                    double sample = imageIter.getSampleDouble(x, y, 0);
                    values[index++] = (byte) sample;
                }
            }
        } else {
            for( int x = 0; x < width; x++ ) {
                for( int y = 0; y < height; y++ ) {
                    double sample = imageIter.getSampleDouble(x, y, 0);
                    values[index++] = (byte) sample;
                }
            }
        }
        imageIter.done();
        return values;
    }

    /**
     * Transforms an array of integer values into a {@link WritableRaster}.
     * 
     * @param array the values to transform.
     * @param divide the factor by which to divide the values.
     * @param width the width of the resulting image.
     * @param height the height of the resulting image.
     * @return the raster.
     */
    public static WritableRaster integerArray2WritableRaster( int[] array, double divide, int width, int height ) {
        WritableRaster writableRaster = createDoubleWritableRaster(width, height, null, null, null);
        int index = 0;;
        for( int x = 0; x < width; x++ ) {
            for( int y = 0; y < height; y++ ) {
                double value = (double) array[index++] / divide;
                writableRaster.setSample(x, y, 0, value);
            }
        }
        return writableRaster;
    }

    /**
     * Transforms an array of values into a {@link WritableRaster}.
     * 
     * @param array the values to transform.
     * @param divide the factor by which to divide the values.
     * @param width the width of the resulting image.
     * @param height the height of the resulting image.
     * @return the raster.
     */
    public static WritableRaster doubleArray2WritableRaster( double[] array, int width, int height ) {
        WritableRaster writableRaster = createDoubleWritableRaster(width, height, null, null, null);
        int index = 0;;
        for( int x = 0; x < width; x++ ) {
            for( int y = 0; y < height; y++ ) {
                writableRaster.setSample(x, y, 0, array[index++]);
            }
        }
        return writableRaster;
    }

    /**
     * Creates a border of novalues.
     * 
     * @param raster the raster to process.
     */
    public static void setNovalueBorder( WritableRaster raster ) {
        int width = raster.getWidth();
        int height = raster.getHeight();

        for( int c = 0; c < width; c++ ) {
            raster.setSample(c, 0, 0, doubleNovalue);
            raster.setSample(c, height - 1, 0, doubleNovalue);
        }
        for( int r = 0; r < height; r++ ) {
            raster.setSample(0, r, 0, doubleNovalue);
            raster.setSample(width - 1, r, 0, doubleNovalue);
        }
    }

    /**
     * Calculates the profile of a raster map between given {@link Coordinate coordinates}.
     * 
     * @param coverage the coverage from which to extract the profile.
     * @param coordinates the coordinates to use to trace the profile.
     * @return the list of {@link ProfilePoint}s.
     * @throws Exception
     */
    public static List<ProfilePoint> doProfile( GridCoverage2D coverage, Coordinate... coordinates ) throws Exception {
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(coverage);

        GridGeometry2D gridGeometry = coverage.getGridGeometry();
        RenderedImage renderedImage = coverage.getRenderedImage();
        RandomIter iter = RandomIterFactory.create(renderedImage, null);

        List<ProfilePoint> profilePointsList = doProfile(iter, regionMap, gridGeometry, coordinates);

        iter.done();
        return profilePointsList;
    }

    /**
     * Calculates the profile of a raster map between given {@link Coordinate coordinates}.
     * 
     * @param mapIter the {@link RandomIter map iterator}.
     * @param regionMap the region map.
     * @param gridGeometry the gridgeometry of the map.
     * @param coordinates the {@link Coordinate}s to create the profile on.
     * @return the list of {@link ProfilePoint}s.
     * @throws TransformException
     */
    public static List<ProfilePoint> doProfile( RandomIter mapIter, RegionMap regionMap, GridGeometry2D gridGeometry,
            Coordinate... coordinates ) throws TransformException {
        List<ProfilePoint> profilePointsList = new ArrayList<ProfilePoint>();
        Envelope2D envelope2d = gridGeometry.getEnvelope2D();
        double xres = regionMap.getXres();
        double yres = regionMap.getYres();
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double step = Math.min(xres, yres);

        LineString line = GeometryUtilities.gf().createLineString(coordinates);
        double lineLength = line.getLength();
        LengthIndexedLine indexedLine = new LengthIndexedLine(line);

        double progressive = 0.0;
        GridCoordinates2D gridCoords;
        while( progressive < lineLength + step ) { // run over by a step to make sure we get the
                                                   // last coord back from the extractor
            Coordinate c = indexedLine.extractPoint(progressive);
            gridCoords = gridGeometry.worldToGrid(new DirectPosition2D(c.x, c.y));
            double value = JGTConstants.doubleNovalue;
            if (envelope2d.contains(c.x, c.y) && isInside(cols, rows, gridCoords)) {
                value = mapIter.getSampleDouble(gridCoords.x, gridCoords.y, 0);
            }
            ProfilePoint profilePoint = new ProfilePoint(progressive, value, c.x, c.y);
            profilePointsList.add(profilePoint);
            progressive = progressive + step;
        }
        return profilePointsList;
    }

    private static boolean isInside( int cols, int rows, GridCoordinates2D gridCoords ) {
        return gridCoords.x >= 0 && gridCoords.x <= cols && gridCoords.y >= 0 && gridCoords.y <= rows;
    }
    /**
     * Utility to tranform row/col to easting/westing.
     * 
     * @param gridGeometry
     * @param x
     * @param y
     * @return the world easting and northing.
     * @throws InvalidGridGeometryException
     * @throws TransformException
     */
    public static Point2D gridToWorld( GridGeometry2D gridGeometry, int x, int y ) throws InvalidGridGeometryException,
            TransformException {
        final Point2D worldPosition = new Point2D.Double(x, y);
        gridGeometry.getGridToCRS2D().transform(worldPosition, worldPosition);
        return worldPosition;
    }

    /**
     * Replace the current internal novalue with a given value.
     * 
     * @param renderedImage a {@link RenderedImage}.
     * @param newValue the value to put in instead of the novalue.
     * @return the rendered image with the substituted novalue. 
     */
    public static WritableRaster replaceNovalue( RenderedImage renderedImage, double newValue ) {
        WritableRaster tmpWR = (WritableRaster) renderedImage.getData();
        RandomIter pitTmpIterator = RandomIterFactory.create(renderedImage, null);

        int height = renderedImage.getHeight();
        int width = renderedImage.getWidth();
        for( int y = 0; y < height; y++ ) {
            for( int x = 0; x < width; x++ ) {
                if (isNovalue(pitTmpIterator.getSampleDouble(x, y, 0))) {
                    tmpWR.setSample(x, y, 0, newValue);
                }
            }
        }
        pitTmpIterator.done();
        return tmpWR;
    }

    /**
     * Utility method for transforming a geometry ROI into the raster space, using the provided affine transformation.
     * 
     * @param roi a {@link Geometry} in model space.
     * @param mt2d an {@link AffineTransform} that maps from raster to model space. This is already referred to the pixel corner.
     * @return a {@link ROI} suitable for using with JAI.
     * @throws ProcessException in case there are problems with ivnerting the provided {@link AffineTransform}. Very unlikely to happen.
     */
    public static ROI prepareROI( Geometry roi, AffineTransform mt2d ) throws Exception {
        // transform the geometry to raster space so that we can use it as a ROI source
        Geometry rasterSpaceGeometry = JTS.transform(roi, new AffineTransform2D(mt2d.createInverse()));

        // simplify the geometry so that it's as precise as the coverage, excess coordinates
        // just make it slower to determine the point in polygon relationship
        Geometry simplifiedGeometry = DouglasPeuckerSimplifier.simplify(rasterSpaceGeometry, 1);

        // build a shape using a fast point in polygon wrapper
        return new ROIShape(new FastLiteShape(simplifiedGeometry));
    }

    /**
     * Read the min, max, mean, sdev, valid cells count from a coverage.
     * 
     * @param coverage the coverage to browse.
     * @param mask an optional mask to apply.
     * @return the min, max, mean, sdev, count.
     */
    public static double[] getMinMaxMeanSdevCount( GridCoverage2D coverage, GridCoverage2D mask ) {
        double[] minMaxMeanSdevCount = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, 0.0, 0.0};
        RandomIter coverageIter = getRandomIterator(coverage);
        RenderedImage coverageRI = coverage.getRenderedImage();
        RandomIter maskIter = null;
        if (mask != null)
            maskIter = getRandomIterator(mask);
        int cellCount = 0;
        double sum = 0;
        double sumSquare = 0;
        for( int i = 0; i < coverageRI.getWidth(); i++ ) {
            for( int j = 0; j < coverageRI.getHeight(); j++ ) {
                double value = coverageIter.getSampleDouble(i, j, 0);
                if (isNovalue(value)) {
                    continue;
                }
                if (maskIter != null) {
                    double maskValue = maskIter.getSampleDouble(i, j, 0);
                    if (isNovalue(maskValue)) {
                        continue;
                    }
                }
                if (value < minMaxMeanSdevCount[0]) {
                    minMaxMeanSdevCount[0] = value;
                }
                if (value > minMaxMeanSdevCount[1]) {
                    minMaxMeanSdevCount[1] = value;
                }
                cellCount++;
                sum = sum + value;
                sumSquare = sum + value * value;
            }
        }
        minMaxMeanSdevCount[2] = sum / cellCount;
        minMaxMeanSdevCount[3] = Math.sqrt(sumSquare / cellCount - minMaxMeanSdevCount[2] * minMaxMeanSdevCount[2]);
        minMaxMeanSdevCount[4] = cellCount;

        return minMaxMeanSdevCount;
    }

    /**
     * Checks if the given path is a GRASS raster file.
     * 
     * <p>Note that there is no check on the existence of the file.
     * 
     * @param path the path to check.
     * @return true if the file is a grass raster.
     */
    public static boolean isGrass( String path ) {
        File file = new File(path);
        File cellFolderFile = file.getParentFile();
        File mapsetFile = cellFolderFile.getParentFile();
        File windFile = new File(mapsetFile, "WIND");
        return cellFolderFile.getName().toLowerCase().equals("cell") && windFile.exists();
    }

    /**
     * Utility method to get col and row of a coordinate from a {@link GridGeometry2D}.
     * 
     * @param coordinate the coordinate to transform.
     * @param gridGeometry the gridgeometry to use.
     * @param point if not <code>null</code>, the row col values are put inside the supplied point's x and y.
     * @return the array with [col, row] or <code>null</code> if something went wrong.
     */
    public static int[] colRowFromCoordinate( Coordinate coordinate, GridGeometry2D gridGeometry, Point point ) {
        try {
            DirectPosition pos = new DirectPosition2D(coordinate.x, coordinate.y);
            GridCoordinates2D worldToGrid = gridGeometry.worldToGrid(pos);
            if (point != null) {
                point.x = worldToGrid.x;
                point.y = worldToGrid.y;
            }
            return new int[]{worldToGrid.x, worldToGrid.y};
        } catch (InvalidGridGeometryException e) {
            e.printStackTrace();
        } catch (TransformException e) {
            e.printStackTrace();
        }

        point.x = Integer.MAX_VALUE;
        point.y = Integer.MAX_VALUE;
        return null;
    }

    /**
     * Utility method to get the coordinate of a col and row from a {@link GridGeometry2D}.
     * 
     * @param col the col to transform.
     * @param row the row to transform.
     * @param gridGeometry the gridgeometry to use.
     * @return the coordinate or <code>null</code> if something went wrong.
     */
    public static Coordinate coordinateFromColRow( int col, int row, GridGeometry2D gridGeometry ) {
        try {
            GridCoordinates2D pos = new GridCoordinates2D(col, row);
            DirectPosition gridToWorld = gridGeometry.gridToWorld(pos);
            double[] coord = gridToWorld.getCoordinate();
            return new Coordinate(coord[0], coord[1]);
        } catch (InvalidGridGeometryException e) {
            e.printStackTrace();
        } catch (TransformException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Mappes the values of a map (valuesMap) into the valid pixels of the second map (maskMap).
     * 
     * @param valuesMap the map holding the values that are needed in the resulting map.
     * @param maskMap the map to use as mask for the values.
     * @return the map containing the values of the valuesMap, but only in the places in which the maskMap is valid.
     */
    public static GridCoverage2D coverageValuesMapper( GridCoverage2D valuesMap, GridCoverage2D maskMap ) {
        RegionMap valuesRegionMap = getRegionParamsFromGridCoverage(valuesMap);
        int cs = valuesRegionMap.getCols();
        int rs = valuesRegionMap.getRows();
        RegionMap maskRegionMap = getRegionParamsFromGridCoverage(maskMap);
        int tmpcs = maskRegionMap.getCols();
        int tmprs = maskRegionMap.getRows();

        if (cs != tmpcs || rs != tmprs) {
            throw new IllegalArgumentException("The raster maps have to be of equal size to be mapped.");
        }

        RandomIter valuesIter = RandomIterFactory.create(valuesMap.getRenderedImage(), null);
        RandomIter maskIter = RandomIterFactory.create(maskMap.getRenderedImage(), null);
        WritableRaster writableRaster = createDoubleWritableRaster(cs, rs, null, null, JGTConstants.doubleNovalue);
        WritableRandomIter outIter = RandomIterFactory.createWritable(writableRaster, null);

        for( int c = 0; c < cs; c++ ) {
            for( int r = 0; r < rs; r++ ) {
                if (!isNovalue(maskIter.getSampleDouble(c, r, 0))) {
                    // if not nv, put the value from the valueMap in the new map
                    double value = valuesIter.getSampleDouble(c, r, 0);
                    if (!isNovalue(value))
                        outIter.setSample(c, r, 0, value);
                }
            }
        }

        GridCoverage2D outCoverage = buildCoverage(
                "mapped", writableRaster, maskRegionMap, valuesMap.getCoordinateReferenceSystem()); //$NON-NLS-1$
        return outCoverage;
    }

    /**
     * Coverage merger.
     * 
     * <p>Values from valuesMap are placed into the onMap coverage, if they are valid.</p>
     * 
     * @param valuesMap the map from which to take teh valid values to place in the output map.
     * @param onMap the base map on which to place the valuesMap values.
     * @return the merged map of valuesMap over onMap.
     */
    public static GridCoverage2D mergeCoverages( GridCoverage2D valuesMap, GridCoverage2D onMap ) {
        RegionMap valuesRegionMap = getRegionParamsFromGridCoverage(valuesMap);
        int cs = valuesRegionMap.getCols();
        int rs = valuesRegionMap.getRows();
        RegionMap onRegionMap = getRegionParamsFromGridCoverage(onMap);
        int tmpcs = onRegionMap.getCols();
        int tmprs = onRegionMap.getRows();

        if (cs != tmpcs || rs != tmprs) {
            throw new IllegalArgumentException("The raster maps have to be of equal size to be mapped.");
        }

        RandomIter valuesIter = RandomIterFactory.create(valuesMap.getRenderedImage(), null);
        WritableRaster outWR = renderedImage2WritableRaster(onMap.getRenderedImage(), false);
        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);

        for( int c = 0; c < cs; c++ ) {
            for( int r = 0; r < rs; r++ ) {
                double value = valuesIter.getSampleDouble(c, r, 0);
                if (!isNovalue(value))
                    outIter.setSample(c, r, 0, value);
            }
        }

        GridCoverage2D outCoverage = buildCoverage("merged", outWR, onRegionMap, valuesMap.getCoordinateReferenceSystem()); //$NON-NLS-1$
        return outCoverage;
    }

    /**
     * Calculates the hypsographic curve for the given raster, using the supplied bins.
     * 
     * @param elevationCoverage the elevation raster.
     * @param bins the bins to use.
     * @param pm the monitor.
     * @return the matrix containing the hypsographic curve in [elev, area] pairs per row.
     */
    public static double[][] calculateHypsographic( GridCoverage2D elevationCoverage, int bins, IJGTProgressMonitor pm ) {
        if (pm == null) {
            pm = new DummyProgressMonitor();
        }
        RegionMap regionMap = getRegionParamsFromGridCoverage(elevationCoverage);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double xres = regionMap.getXres();
        double yres = regionMap.getYres();

        /*
         * calculate the maximum and minimum value of the raster data
         */
        RandomIter elevIter = getRandomIterator(elevationCoverage);
        double maxRasterValue = Double.NEGATIVE_INFINITY;
        double minRasterValue = Double.POSITIVE_INFINITY;
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double value = elevIter.getSampleDouble(c, r, 0);
                if (isNovalue(value)) {
                    continue;
                }
                maxRasterValue = max(maxRasterValue, value);
                minRasterValue = min(minRasterValue, value);
            }
        }

        /*
         * subdivide the whole value range in bins and count the number of pixels in each bin
         */
        double binWidth = (maxRasterValue - minRasterValue) / (bins);
        double[] pixelPerBinCount = new double[bins];
        double[] areaAtGreaterElevation = new double[bins];

        pm.beginTask("Performing calculation of hypsographic curve...", rows);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double value = elevIter.getSampleDouble(c, r, 0);
                if (isNovalue(value)) {
                    continue;
                }
                for( int k = 0; k < pixelPerBinCount.length; k++ ) {
                    double thres = minRasterValue + k * binWidth;
                    if (value >= thres) {
                        pixelPerBinCount[k] = pixelPerBinCount[k] + 1;
                        areaAtGreaterElevation[k] = areaAtGreaterElevation[k] + (yres * xres);
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        double[][] hypso = new double[pixelPerBinCount.length][3];
        for( int j = 0; j < hypso.length; j++ ) {
            hypso[j][0] = minRasterValue + (j * binWidth) + (binWidth / 2.0);
            hypso[j][1] = areaAtGreaterElevation[j] / 1000000.0;
        }

        return hypso;
    }

    /**
     * Simple method to get a value from a single band raster.
     * 
     * <p>Note that this method does always return a value. If invalid, a novalue is returned.</p>
     * 
     * @param raster the single band raster.
     * @param col the column.
     * @param row the row.
     * @return the value in the [col, row] of the first band.
     */
    public static double getValue( GridCoverage2D raster, int col, int row ) {
        double[] values = null;
        try {
            values = raster.evaluate(new GridCoordinates2D(col, row), (double[]) null);
        } catch (Exception e) {
            return doubleNovalue;
        }
        return values[0];
    }

    /**
     * Simple method to get a value from a single band raster.
     * 
     * <p>Note that this method does always return a value. If invalid, a novalue is returned.</p>
     * 
     * @param raster
     * @param easting
     * @param northing
     * @return
     */
    public static double getValue( GridCoverage2D raster, double easting, double northing ) {
        double[] values = null;
        try {
            values = raster.evaluate(new Point2D.Double(easting, northing), (double[]) null);
        } catch (Exception e) {
            return doubleNovalue;
        }
        return values[0];
    }

    public static double getValue( GridCoverage2D raster, Coordinate coordinate ) {
        return getValue(raster, coordinate.x, coordinate.y);
    }

    public static GridCoverage2D invert( GridCoverage2D raster, double max ) {
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(raster);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();

        RandomIter rasterIter = CoverageUtilities.getRandomIterator(raster);

        WritableRaster outWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);
        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);
        for( int r = 0; r < nRows; r++ ) {
            for( int c = 0; c < nCols; c++ ) {
                double value = rasterIter.getSampleDouble(c, r, 0);
                if (!isNovalue(value)) {
                    outIter.setSample(c, r, 0, max - value);
                }
            }
        }
        GridCoverage2D invertedRaster = CoverageUtilities.buildCoverage("inverted", outWR, regionMap,
                raster.getCoordinateReferenceSystem());
        return invertedRaster;
    }

}
