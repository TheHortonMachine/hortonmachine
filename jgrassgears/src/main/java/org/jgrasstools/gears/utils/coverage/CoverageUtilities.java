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
package org.jgrasstools.gears.utils.coverage;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doesOverFlow;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
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
import org.jgrasstools.gears.utils.features.FastLiteShape;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineSegment;
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
     * @param coverage the coverage on which to wrap a {@link WritableRandomIter}.
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
     * @param coverage the coverage on which to wrap a {@link WritableRandomIter}.
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
     * Get the parameters of the region covered by the {@link GridCoverage2D coverage}. 
     * 
     * @param gridCoverage the coverage.
     * @return the {@link HashMap map} of parameters. ( {@link #NORTH} and the 
     *          other static vars can be used to retrieve them.
     */
    public static HashMap<String, Double> getRegionParamsFromGridCoverage( GridCoverage2D gridCoverage ) {
        HashMap<String, Double> envelopeParams = new HashMap<String, Double>();

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

    public static HashMap<String, Double> gridGeometry2RegionParamsMap( GridGeometry2D gridGeometry ) {
        HashMap<String, Double> envelopeParams = new HashMap<String, Double>();

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

    public static HashMap<String, Double> makeRegionParamsMap( double north, double south, double west, double east, double xRes,
            double yRes, int width, int height ) {
        HashMap<String, Double> envelopeParams = new HashMap<String, Double>();
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
     * Calculates the profile of a raster map between two given {@link Coordinate coordinates}.
     * 
     * @param start the first coordinate.
     * @param end the last coordinate.
     * @param coverage the coverage from which to extract the profile.
     * @return the list of {@link ProfilePoint}s.
     * @throws Exception
     */
    public static List<ProfilePoint> doProfile( Coordinate start, Coordinate end, GridCoverage2D coverage ) throws Exception {
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(coverage);
        double xres = regionMap.get(CoverageUtilities.XRES);
        GridGeometry2D gridGeometry = coverage.getGridGeometry();
        RenderedImage renderedImage = coverage.getRenderedImage();
        RandomIter iter = RandomIterFactory.create(renderedImage, null);

        LineSegment pline = new LineSegment(start, end);
        double lenght = pline.getLength();

        List<ProfilePoint> profilePointsList = new ArrayList<ProfilePoint>();
        double progressive = 0.0;

        // ad the first point
        GridCoordinates2D gridCoords = gridGeometry.worldToGrid(new DirectPosition2D(start.x, start.y));
        double value = iter.getSampleDouble(gridCoords.x, gridCoords.y, 0);

        ProfilePoint profilePoint = new ProfilePoint(0.0, value, start.x, start.y);
        profilePointsList.add(profilePoint);
        progressive = progressive + xres;

        while( progressive < lenght ) {
            Coordinate c = pline.pointAlong(progressive / lenght);
            gridCoords = gridGeometry.worldToGrid(new DirectPosition2D(c.x, c.y));
            value = iter.getSampleDouble(gridCoords.x, gridCoords.y, 0);
            profilePoint = new ProfilePoint(progressive, value, c.x, c.y);
            profilePointsList.add(profilePoint);
            progressive = progressive + xres;
        }

        // add the last point
        gridCoords = gridGeometry.worldToGrid(new DirectPosition2D(end.x, end.y));
        value = iter.getSampleDouble(gridCoords.x, gridCoords.y, 0);
        profilePoint = new ProfilePoint(lenght, value, end.x, end.y);
        profilePointsList.add(profilePoint);

        iter.done();

        return profilePointsList;
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
     * @return the min, max, mean, sdev, count.
     */
    public static double[] getMinMaxMeanSdevCount( GridCoverage2D coverage ) {
        double[] minMaxMeanSdevCount = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, 0.0, 0.0};
        RandomIter coverageIter = getRandomIterator(coverage);
        RenderedImage coverageRI = coverage.getRenderedImage();
        int cellCount = 0;
        double sum = 0;
        double sumSquare = 0;
        for( int i = 0; i < coverageRI.getWidth(); i++ ) {
            for( int j = 0; j < coverageRI.getHeight(); j++ ) {
                double value = coverageIter.getSampleDouble(i, j, 0);
                if (isNovalue(value)) {
                    continue;
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

}
