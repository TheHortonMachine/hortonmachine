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
package eu.hydrologis.jgrass.jgrassgears.utils.coverage;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.media.jai.RasterFactory;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.parameter.Parameter;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;

import eu.hydrologis.jgrass.jgrassgears.libs.modules.HMConstants;

/**
 * <p>
 * A class of utilities bound to raster analysis
 * </p>
 * 
 * @author Andrea Antonello - www.hydrologis.com
 * @since 1.1.0
 */
public class CoverageUtilities {
    public static final String NORTH = "NORTH";
    public static final String SOUTH = "SOUTH";
    public static final String WEST = "WEST";
    public static final String EAST = "EAST";
    public static final String XRES = "XRES";
    public static final String YRES = "YRES";
    public static final String ROWS = "ROWS";
    public static final String COLS = "COLS";

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
    public static WritableRaster createDoubleWritableRaster( int width, int height,
            Class< ? > dataClass, SampleModel sampleModel, Double value ) {
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
    }

    /**
     * Get the parameters of the region covered by the {@link GridCoverage2D coverage}. 
     * 
     * @param gridCoverage the coverage.
     * @return the {@link HashMap map} of parameters. ( {@link #NORTH} and the 
     *          other static vars can be used to retrieve them.
     */
    public static HashMap<String, Double> getRegionParamsFromGridCoverage(
            GridCoverage2D gridCoverage ) {
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

    public HashMap<String, Double> generalParameterValues2RegionParamsMap(
            GeneralParameterValue[] params ) {
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

    public static HashMap<String, Double> makeRegionParamsMap( double north, double south,
            double west, double east, double xRes, double yRes, int width, int height ) {
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

    public static GridGeometry2D gridGeometryFromRegionParams(
            HashMap<String, Double> envelopeParams, CoordinateReferenceSystem crs ) {

        double west = envelopeParams.get(WEST);
        double south = envelopeParams.get(SOUTH);
        double east = envelopeParams.get(EAST);
        double north = envelopeParams.get(NORTH);
        int rows = envelopeParams.get(ROWS).intValue();
        int cols = envelopeParams.get(COLS).intValue();

        // GridToEnvelopeMapper g2eMapper = new GridToEnvelopeMapper();
        // g2eMapper.setEnvelope(envelope);
        // g2eMapper.setGridRange(gridRange);
        // g2eMapper.setPixelAnchor(ModelsEngine.DEFAULTPIXELANCHOR);
        // MathTransform gridToEnvelopeTransform = g2eMapper.createTransform();
        // g2eMapper.getG
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
     * @param crs the {@link CoordinateReferenceSystem}. 
     * @return the {@link GeneralParameterValue array of parameters}.
     */
    public static GeneralParameterValue[] createGridGeometryGeneralParameter( int width,
            int height, double north, double south, double east, double west,
            CoordinateReferenceSystem crs ) {
        GeneralParameterValue[] readParams = new GeneralParameterValue[1];
        Parameter<GridGeometry2D> readGG = new Parameter<GridGeometry2D>(
                AbstractGridFormat.READ_GRIDGEOMETRY2D);
        GridEnvelope2D gridEnvelope = new GridEnvelope2D(0, 0, width, height);
        ReferencedEnvelope env = new ReferencedEnvelope(west, east, south, north, crs);
        readGG.setValue(new GridGeometry2D(gridEnvelope, env));
        readParams[0] = readGG;

        return readParams;
    }

    /**
     * Create a {@link WritableRaster} from a double matrix.
     * 
     * @param matrix the matrix to take the data from.
     * @return the produced raster.
     */
    public static WritableRaster createWritableRasterFromMatrix( double[][] matrix ) {
        int height = matrix.length;
        int width = matrix[0].length;
        WritableRaster writableRaster = createDoubleWritableRaster(width, height, null, null, null);

        WritableRandomIter disckRandomIter = RandomIterFactory.createWritable(writableRaster, null);
        for( int x = 0; x < width; x++ ) {
            for( int y = 0; y < height; y++ ) {
                disckRandomIter.setSample(x, y, 0, matrix[y][x]);
            }
        }
        disckRandomIter.done();

        return writableRaster;
    }

    /**
     * Creates a {@link GridCoverage2D coverage} from a double[][] matrix and the necessary geographic Information.
     * 
     * @param name the name of the coverage.
     * @param dataMatrix the matrix containing the data.
     * @param envelopeParams the map of boundary parameters.
     * @param crs the {@link CoordinateReferenceSystem}.
     * @return the {@link GridCoverage2D coverage}.
     */
    public static GridCoverage2D buildCoverage( String name, double[][] dataMatrix,
            HashMap<String, Double> envelopeParams, CoordinateReferenceSystem crs ) {
        WritableRaster writableRaster = createWritableRasterFromMatrix(dataMatrix);
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
    public static GridCoverage2D buildCoverage( String name, RenderedImage renderedImage,
            HashMap<String, Double> envelopeParams, CoordinateReferenceSystem crs ) {

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

        double west = envelopeParams.get(WEST);
        double south = envelopeParams.get(SOUTH);
        double east = envelopeParams.get(EAST);
        double north = envelopeParams.get(NORTH);
        Envelope2D writeEnvelope = new Envelope2D(crs, west, south, east - west, north - south);
        GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);

        GridCoverage2D coverage2D = factory.create(name, writableRaster, writeEnvelope);
        return coverage2D;
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
        WritableRaster writableRaster = createWritableRasterFromMatrix(dataMatrix);
        return buildCoverage("dummy", writableRaster, envelopeParams, DefaultGeographicCRS.WGS84);
    }

    /**
     * Creates a compatible {@link WritableRaster} from a {@link RenderedImage}.
     * 
     * @param renderedImage the image to convert.
     * @param nullBorders a flag that indicates if the borders should be set to null.
     * @return the converted writable raster.
     */
    public static WritableRaster renderedImage2WritableRaster( RenderedImage renderedImage,
            boolean nullBorders ) {
        int width = renderedImage.getWidth();
        int height = renderedImage.getHeight();

        Raster data = renderedImage.getData();
        WritableRaster writableRaster = data.createCompatibleWritableRaster();
        writableRaster.setDataElements(0, 0, data);
        if (nullBorders) {
            for( int c = 0; c < width; c++ ) {
                writableRaster.setSample(c, 0, 0, HMConstants.doubleNovalue);
                writableRaster.setSample(c, height - 1, 0, HMConstants.doubleNovalue);
            }
            for( int r = 0; r < height; r++ ) {
                writableRaster.setSample(0, r, 0, HMConstants.doubleNovalue);
                writableRaster.setSample(width - 1, r, 0, HMConstants.doubleNovalue);
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
            raster.setSample(c, 0, 0, HMConstants.doubleNovalue);
            raster.setSample(c, height - 1, 0, HMConstants.doubleNovalue);
        }
        for( int r = 0; r < height; r++ ) {
            raster.setSample(0, r, 0, HMConstants.doubleNovalue);
            raster.setSample(width - 1, r, 0, HMConstants.doubleNovalue);
        }
    }

    /**
     * Calculates the profile of a raster map between two given
     * {@link Coordinate coordinates}.
     * 
     * @param x1
     *            the easting of the first coordinate
     * @param y1
     *            the northing of the first coordinate
     * @param x2
     *            the easting of the final coordinate
     * @param y2
     *            the northing of the final coordinate
     * @param xres
     *            the x resolution to consider
     * @param yres
     *            the y resolution to consider
     * @param coverage
     *            the raster from which to take the elevations
     * @return a list of double arrays that contain for every point of the
     *         profile progressive, elevation, easting, northing
     * @throws Exception 
     */
    public static List<Double[]> doProfile( double x1, double y1, double x2, double y2,
            double xres, double yres, GridCoverage2D coverage ) throws Exception {
        GridGeometry2D gridGeometry = coverage.getGridGeometry();
        RenderedImage renderedImage = coverage.getRenderedImage();
        RandomIter iter = RandomIterFactory.create(renderedImage, null);

        Coordinate start = new Coordinate(x1, y1);
        Coordinate end = new Coordinate(x2, y2);
        LineSegment pline = new LineSegment(start, end);

        double lenght = pline.getLength();

        List<Double[]> distanceValueAbsolute = new ArrayList<Double[]>();
        double progressive = 0.0;

        // ad the first point
        GridCoordinates2D gridCoords = gridGeometry.worldToGrid(new DirectPosition2D(start.x,
                start.y));
        double value = iter.getSampleDouble(gridCoords.x, gridCoords.y, 0);

        Double[] d = {0.0, value, start.x, start.y};
        distanceValueAbsolute.add(d);
        progressive = progressive + xres;

        while( progressive < lenght ) {

            Coordinate c = pline.pointAlong(progressive / lenght);
            gridCoords = gridGeometry.worldToGrid(new DirectPosition2D(c.x, c.y));
            value = iter.getSampleDouble(gridCoords.x, gridCoords.y, 0);
            Double[] v = {progressive, value, c.x, c.y};
            distanceValueAbsolute.add(v);
            progressive = progressive + xres;
        }

        // add the last point
        gridCoords = gridGeometry.worldToGrid(new DirectPosition2D(end.x, end.y));
        value = iter.getSampleDouble(gridCoords.x, gridCoords.y, 0);
        Double[] v = {lenght, value, end.x, end.y};
        distanceValueAbsolute.add(v);

        iter.done();

        return distanceValueAbsolute;
    }

}
