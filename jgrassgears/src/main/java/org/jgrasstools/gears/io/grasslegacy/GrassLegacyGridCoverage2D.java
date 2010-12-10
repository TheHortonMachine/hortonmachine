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
package org.jgrasstools.gears.io.grasslegacy;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.util.HashMap;
import java.util.Set;

import javax.media.jai.Interpolation;

import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.ViewType;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.jgrasstools.gears.io.grasslegacy.utils.GrassLegacyUtilities;
import org.jgrasstools.gears.io.grasslegacy.utils.Window;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * This is a brute force hack to get at least for GRASS rasters the 
 * coverage work even for rasters of size > {@link Integer#MAX_VALUE}.
 * 
 * <p>Since this backs on a double matrix, many of the methods are 
 * not supported. It is an ugly hack, but since the code would anyways 
 * break because of integer overflow, this is the last try to get it
 * working anyways. So the data read are grass rasters and the size 
 * would overflow, this {@link GridCoverage2D} will be presented. If the client 
 * uses it properly, it will be able to do its work. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GrassLegacyGridCoverage2D extends GridCoverage2D {

    private static final long serialVersionUID = 1L;

    private Window window;
    private double[][] data;
    private CoordinateReferenceSystem jgCrs;
    private HashMap<String, Double> regionParamsMap;
    private GridGeometry2D jgGridGeometry;

    public GrassLegacyGridCoverage2D( CharSequence name, GridCoverage2D coverage ) {
        super(name, coverage);
    }

    public GrassLegacyGridCoverage2D( Window window, double[][] data, CoordinateReferenceSystem crs ) {
        super("dummy", CoverageUtilities.buildDummyCoverage());
        this.window = window;
        this.data = data;
        jgCrs = crs;

        regionParamsMap = CoverageUtilities.makeRegionParamsMap(window.getNorth(), window.getSouth(), window.getWest(),
                window.getEast(), window.getWEResolution(), window.getNSResolution(), window.getCols(), window.getRows());
        jgGridGeometry = CoverageUtilities.gridGeometryFromRegionParams(regionParamsMap, jgCrs);
    }

    public boolean isDataEditable() {
        return true;
    }

    public GridGeometry2D getGridGeometry() {
        return jgGridGeometry;
    }

    public Envelope getEnvelope() {
        com.vividsolutions.jts.geom.Envelope envelope = window.getEnvelope();
        Envelope env = new ReferencedEnvelope(envelope, jgCrs);
        return env;
    }

    public Envelope2D getEnvelope2D() {
        return jgGridGeometry.getEnvelope2D();
    }

    public CoordinateReferenceSystem getCoordinateReferenceSystem2D() {
        return jgGridGeometry.getCoordinateReferenceSystem2D();
    }

    public int getNumSampleDimensions() {
        return 1;
    }

    public GridSampleDimension getSampleDimension( final int index ) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public GridSampleDimension[] getSampleDimensions() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public Interpolation getInterpolation() {
        return Interpolation.getInstance(Interpolation.INTERP_NEAREST);
    }

    private double getValue( Coordinate coordinate ) {
        int[] rowCol = GrassLegacyUtilities.coordinateToNearestRowCol(window, coordinate);
        return getValue(rowCol[0], rowCol[1]);
    }
    private double getValue( int x, int y ) {
        if (y < 0 || y >= data.length) {
            return Double.NaN;
        }
        if (x < 0 || x >= data[0].length) {
            return Double.NaN;
        }
        return data[y][x];
    }
    private double getValue( final DirectPosition point ) {
        double[] coordinateArray = point.getCoordinate();
        Coordinate coordinate = new Coordinate(coordinateArray[0], coordinateArray[1]);
        return getValue(coordinate);
    }
    private double getValue( final Point2D coord ) {
        Coordinate coordinate = new Coordinate(coord.getX(), coord.getY());
        return getValue(coordinate);
    }

    private double getValue( final GridCoordinates2D coord ) {
        return getValue(coord.x, coord.y);
    }

    public Object evaluate( final DirectPosition point ) throws CannotEvaluateException {
        return getValue(point);
    }

    public byte[] evaluate( final DirectPosition coord, byte[] dest ) throws CannotEvaluateException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public int[] evaluate( final DirectPosition coord, int[] dest ) throws CannotEvaluateException {
        if (dest == null) {
            dest = new int[0];
        }
        dest[0] = (int) getValue(coord);
        return dest;
    }

    public float[] evaluate( final DirectPosition coord, float[] dest ) throws CannotEvaluateException {
        if (dest == null) {
            dest = new float[0];
        }
        dest[0] = (float) getValue(coord);
        return dest;
    }

    public double[] evaluate( final DirectPosition coord, double[] dest ) throws CannotEvaluateException {
        if (dest == null) {
            dest = new double[0];
        }
        dest[0] = getValue(coord);
        return dest;
    }

    public int[] evaluate( final Point2D coord, int[] dest ) throws CannotEvaluateException {
        if (dest == null) {
            dest = new int[0];
        }
        dest[0] = (int) getValue(coord);
        return dest;
    }

    public float[] evaluate( final Point2D coord, float[] dest ) throws CannotEvaluateException {
        if (dest == null) {
            dest = new float[0];
        }
        dest[0] = (float) getValue(coord);
        return dest;
    }

    public double[] evaluate( final Point2D coord, double[] dest ) throws CannotEvaluateException {
        if (dest == null) {
            dest = new double[0];
        }
        dest[0] = getValue(coord);
        return dest;
    }

    public int[] evaluate( final GridCoordinates2D coord, int[] dest ) {
        if (dest == null) {
            dest = new int[0];
        }
        dest[0] = (int) getValue(coord);
        return dest;
    }

    public float[] evaluate( final GridCoordinates2D coord, float[] dest ) {
        if (dest == null) {
            dest = new float[0];
        }
        dest[0] = (float) getValue(coord);
        return dest;
    }

    public double[] evaluate( final GridCoordinates2D coord, double[] dest ) {
        if (dest == null) {
            dest = new double[0];
        }
        dest[0] = getValue(coord);
        return dest;
    }

    public synchronized String getDebugString( final DirectPosition coord ) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public int[] getOptimalDataBlockSizes() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public RenderedImage getRenderedImage() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public RenderableImage getRenderableImage( final int xAxis, final int yAxis ) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void show( String title, final int xAxis, final int yAxis ) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void show( final String title ) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void prefetch( final Rectangle2D area ) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public GridCoverage2D geophysics( final boolean geo ) {
        return this;
    }

    public GridCoverage2D view( final ViewType type ) {
        return this;
    }

    public synchronized Set<ViewType> getViewTypes() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public synchronized boolean dispose( final boolean force ) {
        data = null;
        return true;
    }

    public String toString() {
        return window.toString();
    }

    /**
     * Getter for the data matrix that is backed by this {@link GridCoverage2D}.
     * 
     * @return the data matrix.
     */
    public double[][] getData() {
        return data;
    }

}
