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
package org.hortonmachine.nww.layers.defaults.other;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.stream.IntStream;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.hortonmachine.nww.layers.defaults.NwwLayer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.OGLStackHandler;

/**
 * A layer of many colored points, as a lidar point cloud, drawn as OpenGL points.
 *
 * <p>Every point has a value, e.g. its elevation or intensity, and only the points with a value
 * in the range set with {@link #setValueRange(double, double)} are drawn. The graphics card keeps
 * the indexes of the points sorted by value, so the points of a range are contiguous and changing
 * the range is immediate, also with millions of points.</p>
 *
 * <p>While the view moves, only a part of the points is drawn, at most
 * {@link #setMovingPointsLimit(int)}, evenly taken among them, to keep the movement smooth. All the
 * points are drawn again as soon as the view stops.</p>
 *
 * <p>The points are not pickable.</p>
 */
public class PointCloudLayer extends AbstractLayer implements NwwLayer {

    /** The default maximum number of points drawn while the view moves. */
    public static final int DEFAULT_MOVING_POINTS_LIMIT = 5_000_000;
    /** The time after the last change of the view, after which all the points are drawn again. */
    private static final int MOVING_DELAY_MS = 300;

    /**
     * The positions of the points, in the order they were given. Immutable.
     *
     * <p>The positions are kept as floats relative to the center, which halves the memory and still
     * leaves a precision under the millimeter on an extent of kilometers.</p>
     */
    private static class Positions {
        final float[] lons;
        final float[] lats;
        final float[] elevs;
        final double centerLon;
        final double centerLat;
        final double centerElev;
        final double minLon;
        final double maxLon;
        final double minLat;
        final double maxLat;

        Positions( double[] lons, double[] lats, double[] elevs ) {
            int n = lons.length;
            double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
            for( int i = 0; i < n; i++ ) {
                minX = Math.min(minX, lons[i]);
                maxX = Math.max(maxX, lons[i]);
                minY = Math.min(minY, lats[i]);
                maxY = Math.max(maxY, lats[i]);
            }
            minLon = minX;
            maxLon = maxX;
            minLat = minY;
            maxLat = maxY;
            centerLon = (minX + maxX) / 2.0;
            centerLat = (minY + maxY) / 2.0;
            this.lons = new float[n];
            this.lats = new float[n];
            for( int i = 0; i < n; i++ ) {
                this.lons[i] = (float) (lons[i] - centerLon);
                this.lats[i] = (float) (lats[i] - centerLat);
            }
            centerElev = center(elevs);
            this.elevs = relative(elevs, centerElev);
        }

        /**
         * The same horizontal positions with other elevations.
         */
        Positions( Positions other, double[] elevs ) {
            lons = other.lons;
            lats = other.lats;
            centerLon = other.centerLon;
            centerLat = other.centerLat;
            minLon = other.minLon;
            maxLon = other.maxLon;
            minLat = other.minLat;
            maxLat = other.maxLat;
            centerElev = center(elevs);
            this.elevs = relative(elevs, centerElev);
        }

        private static double center( double[] values ) {
            double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
            for( double value : values ) {
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
            return (min + max) / 2.0;
        }

        private static float[] relative( double[] values, double center ) {
            float[] relative = new float[values.length];
            for( int i = 0; i < values.length; i++ ) {
                relative[i] = (float) (values[i] - center);
            }
            return relative;
        }

        int size() {
            return lons.length;
        }
    }

    /**
     * The values and colors of the points. Immutable.
     */
    private static class Coloring {
        /** The values, sorted. */
        final float[] sortedValues;
        /** The index of the point of every sorted value. */
        final int[] order;
        /** The colors of the points, as rgb bytes, in the order of the points. */
        final ByteBuffer colors;
        /** The part of the sorted values and indexes drawn while moving, <code>null</code> if all are drawn. */
        final float[] movingValues;
        final int[] movingOrder;

        Coloring( float[] sortedValues, int[] order, ByteBuffer colors, int movingLimit ) {
            this.sortedValues = sortedValues;
            this.order = order;
            this.colors = colors;
            int n = order.length;
            if (movingLimit > 0 && n > movingLimit) {
                // every step-th of the sorted points: still sorted, and spread among all
                int step = (int) Math.ceil(n / (double) movingLimit);
                int m = (n + step - 1) / step;
                movingValues = new float[m];
                movingOrder = new int[m];
                for( int i = 0; i < m; i++ ) {
                    movingValues[i] = sortedValues[i * step];
                    movingOrder[i] = order[i * step];
                }
            } else {
                movingValues = null;
                movingOrder = null;
            }
        }
    }

    private static class Data {
        final Positions positions;
        final Coloring coloring;

        Data( Positions positions, Coloring coloring ) {
            this.positions = positions;
            this.coloring = coloring;
        }
    }

    private final String title;

    private volatile Data data;
    private volatile double lowValue = Double.NEGATIVE_INFINITY;
    private volatile double highValue = Double.POSITIVE_INFINITY;
    private volatile double pointSize = 2.0;
    private volatile boolean drawOverTerrain = false;
    private volatile int movingPointsLimit = DEFAULT_MOVING_POINTS_LIMIT;

    /*
     * The state of the vertices, used only in the rendering thread: the vertices are relative to
     * the reference point, to keep the precision of floats, and depend on the globe and the
     * vertical exaggeration.
     */
    private Positions verticesPositions;
    private Globe verticesGlobe;
    private double verticesExaggeration = Double.NaN;
    private Vec4 referencePoint;
    /** The vertices still to upload, released once in the graphics card. */
    private FloatBuffer vertices;
    /*
     * The buffers in the graphics card: vertices, colors, sorted indexes and the sorted indexes
     * drawn while moving. They are owned by the layer and not kept in the GPU resource cache of
     * WorldWind, since a large cloud (e.g. 90 million points, more than 1 GB) does not fit in it:
     * refused by the cache, the buffers would be created and uploaded again at every frame.
     */
    private int[] vbos;
    /** The context the buffers were created in, to create them again if it changes. */
    private GLContext vbosContext;
    /** The coloring in the buffers. */
    private Coloring uploadedColoring;
    /** The data whose upload failed, e.g. for lack of memory, not tried again. */
    private Data failedData;

    /* The movement of the view, to draw less points while it moves. */
    private Matrix lastModelview;
    private long lastMoveTime;
    private Timer stopTimer;

    /**
     * @param title the name of the layer.
     */
    public PointCloudLayer( String title ) {
        this.title = title;
        setName(title);
        setPickEnabled(false);
    }

    /**
     * Set the points to draw, replacing the existing ones. Can be called from any thread, all the
     * arrays have the size of the number of points and are not kept.
     *
     * @param lons the longitudes, in degrees.
     * @param lats the latitudes, in degrees.
     * @param elevs the elevations, in meters.
     * @param values the values used to filter the points with the value range.
     * @param rgbs the colors as packed rgb, as given by {@link java.awt.Color#getRGB()}.
     */
    public void setPoints( double[] lons, double[] lats, double[] elevs, float[] values, int[] rgbs ) {
        int n = lons.length;
        if (lats.length != n || elevs.length != n || values.length != n || rgbs.length != n) {
            throw new IllegalArgumentException("All the arrays of the points need to have the same size.");
        }
        Positions positions = new Positions(lons, lats, elevs);
        data = new Data(positions, createColoring(values, rgbs));
    }

    /**
     * Change the elevations of the points, keeping their horizontal position, values and colors.
     *
     * @param elevs the elevations, in meters, in the order of the points.
     */
    public void setElevations( double[] elevs ) {
        Data d = data;
        if (d == null || elevs.length != d.positions.size()) {
            throw new IllegalArgumentException("The elevations need to be as many as the points.");
        }
        data = new Data(new Positions(d.positions, elevs), d.coloring);
    }

    /**
     * Change values and colors of the points, keeping their position, which is faster than
     * setting the points again.
     *
     * @param values the values used to filter the points with the value range, in the order of the points.
     * @param rgbs the colors as packed rgb, in the order of the points.
     */
    public void setValuesAndColors( float[] values, int[] rgbs ) {
        Data d = data;
        if (d == null || values.length != d.positions.size() || rgbs.length != d.positions.size()) {
            throw new IllegalArgumentException("The values and colors need to be as many as the points.");
        }
        data = new Data(d.positions, createColoring(values, rgbs));
    }

    private Coloring createColoring( float[] values, int[] rgbs ) {
        int n = values.length;
        // sort by value through keys that have the sortable bits of the value in the high part and
        // the index in the low part: a sort of primitives, fast also for millions of points
        long[] keys = new long[n];
        for( int i = 0; i < n; i++ ) {
            int bits = Float.floatToIntBits(values[i]);
            bits ^= (bits >> 31) & 0x7fffffff;
            keys[i] = ((long) bits << 32) | (i & 0xffffffffL);
        }
        Arrays.parallelSort(keys);

        int[] order = new int[n];
        float[] sortedValues = new float[n];
        for( int i = 0; i < n; i++ ) {
            int index = (int) keys[i];
            order[i] = index;
            sortedValues[i] = values[index];
        }
        keys = null;
        ByteBuffer colors = Buffers.newDirectByteBuffer(n * 3);
        for( int i = 0; i < n; i++ ) {
            int rgb = rgbs[i];
            colors.put((byte) ((rgb >> 16) & 0xff));
            colors.put((byte) ((rgb >> 8) & 0xff));
            colors.put((byte) (rgb & 0xff));
        }
        colors.rewind();
        return new Coloring(sortedValues, order, colors, movingPointsLimit);
    }

    /**
     * Remove all the points.
     */
    public void clear() {
        data = null;
    }

    /**
     * @return the number of points of the layer.
     */
    public int getPointsCount() {
        Data d = data;
        return d == null ? 0 : d.positions.size();
    }

    /**
     * @return the minimum and maximum value of the points, or <code>null</code> if there are no points.
     */
    public double[] getValueBounds() {
        Data d = data;
        if (d == null || d.positions.size() == 0) {
            return null;
        }
        float[] values = d.coloring.sortedValues;
        return new double[]{values[0], values[values.length - 1]};
    }

    /**
     * Draw only the points with value between the two given, included.
     *
     * @param low the lowest value to draw.
     * @param high the highest value to draw.
     */
    public void setValueRange( double low, double high ) {
        lowValue = low;
        highValue = high;
    }

    /**
     * Draw all the points, whatever their value.
     */
    public void resetValueRange() {
        setValueRange(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /**
     * @return the number of points with a value in the current range.
     */
    public int getVisiblePointsCount() {
        Data d = data;
        if (d == null) {
            return 0;
        }
        return visibleRange(d.coloring.sortedValues)[1];
    }

    /**
     * @return the number of points with a value in the current range drawn while the view moves.
     */
    public int getVisibleMovingPointsCount() {
        Data d = data;
        if (d == null) {
            return 0;
        }
        float[] values = d.coloring.movingValues != null ? d.coloring.movingValues : d.coloring.sortedValues;
        return visibleRange(values)[1];
    }

    /**
     * @param movingPointsLimit the maximum number of points drawn while the view moves, 0 to always
     *            draw all.
     */
    public void setMovingPointsLimit( int movingPointsLimit ) {
        this.movingPointsLimit = Math.max(0, movingPointsLimit);
        Data d = data;
        if (d != null) {
            Coloring c = d.coloring;
            data = new Data(d.positions, new Coloring(c.sortedValues, c.order, c.colors, this.movingPointsLimit));
        }
    }

    public int getMovingPointsLimit() {
        return movingPointsLimit;
    }

    /**
     * @param pointSize the size of the points, in pixels.
     */
    public void setPointSize( double pointSize ) {
        this.pointSize = pointSize;
    }

    public double getPointSize() {
        return pointSize;
    }

    /**
     * @param drawOverTerrain if <code>true</code>, the points are never hidden by the terrain, as
     *            when their elevations and the ones of the terrain come from different sources.
     *            The points still hide each other.
     */
    public void setDrawOverTerrain( boolean drawOverTerrain ) {
        this.drawOverTerrain = drawOverTerrain;
    }

    public boolean isDrawOverTerrain() {
        return drawOverTerrain;
    }

    @Override
    public Coordinate getCenter() {
        Data d = data;
        if (d == null) {
            return null;
        }
        return new Coordinate(d.positions.centerLon, d.positions.centerLat);
    }

    @Override
    public Envelope getBounds() {
        Data d = data;
        if (d == null) {
            return null;
        }
        Positions p = d.positions;
        return new Envelope(p.minLon, p.maxLon, p.minLat, p.maxLat);
    }

    /**
     * Remove the points and free the memory they take, also in the graphics card. To be called in
     * the UI thread, when the layer is removed.
     */
    @Override
    public void freeResources() {
        data = null;
        if (stopTimer != null) {
            stopTimer.stop();
        }
        GLContext context = vbosContext;
        int[] ids = vbos;
        vbos = null;
        vbosContext = null;
        vertices = null;
        verticesPositions = null;
        uploadedColoring = null;
        if (context == null || ids == null) {
            return;
        }
        // the layer is not rendered any more: delete the buffers in its context
        try {
            boolean wasCurrent = context.isCurrent();
            if (wasCurrent || context.makeCurrent() != GLContext.CONTEXT_NOT_CURRENT) {
                try {
                    context.getGL().glDeleteBuffers(ids.length, ids, 0);
                } finally {
                    if (!wasCurrent) {
                        context.release();
                    }
                }
            }
        } catch (Exception e) {
            Logging.logger().warning("Unable to free the graphics memory of the layer " + title + ": " + e.getMessage());
        }
    }

    @Override
    public void dispose() {
        freeResources();
        super.dispose();
    }

    /**
     * @return the first index and the number of the sorted values in the current range.
     */
    private int[] visibleRange( float[] values ) {
        // first index with value >= low
        int first = lowerBound(values, lowValue);
        // first index with value > high
        int end = upperBound(values, highValue);
        return new int[]{first, Math.max(0, end - first)};
    }

    private static int lowerBound( float[] values, double value ) {
        int low = 0;
        int high = values.length;
        while( low < high ) {
            int mid = (low + high) >>> 1;
            if (values[mid] < value) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    private static int upperBound( float[] values, double value ) {
        int low = 0;
        int high = values.length;
        while( low < high ) {
            int mid = (low + high) >>> 1;
            if (values[mid] <= value) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    @Override
    protected void doRender( DrawContext dc ) {
        GL2 gl = dc.getGL().getGL2();
        GLContext context = dc.getGLContext();
        if (vbos != null && context != vbosContext) {
            // the buffers belonged to a context that is gone: create and fill them again
            vbos = null;
            verticesPositions = null;
            uploadedColoring = null;
        }
        Data d = data;
        if (d == null || d.positions.size() == 0) {
            releaseBuffers(gl);
            return;
        }
        if (dc.isPickingMode() || d == failedData) {
            return;
        }

        boolean reduced = isViewMoving(dc) && d.coloring.movingOrder != null;
        int[] range = visibleRange(reduced ? d.coloring.movingValues : d.coloring.sortedValues);
        if (reduced) {
            // draw all the points once the view stops
            scheduleFullRedraw();
        }
        if (range[1] == 0) {
            return;
        }

        updateVertices(dc, d.positions);
        if (!upload(gl, d)) {
            return;
        }
        vbosContext = context;

        OGLStackHandler stack = new OGLStackHandler();
        stack.pushAttrib(gl, GL2.GL_CURRENT_BIT | GL2.GL_POINT_BIT | GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_ENABLE_BIT);
        stack.pushClientAttrib(gl, GL2.GL_CLIENT_VERTEX_ARRAY_BIT);
        dc.getView().pushReferenceCenter(dc, referencePoint);
        try {
            gl.glDisable(GL.GL_TEXTURE_2D);
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glDisable(GL.GL_BLEND);
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glDepthMask(true);
            if (drawOverTerrain) {
                // forget the depth of the terrain, keeping the one among the points
                gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
            }
            gl.glPointSize((float) pointSize);
            if (pointSize > 1.5) {
                gl.glEnable(GL2.GL_POINT_SMOOTH);
            }

            gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbos[0]);
            gl.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbos[1]);
            gl.glColorPointer(3, GL.GL_UNSIGNED_BYTE, 0, 0);
            gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, reduced ? vbos[3] : vbos[2]);
            gl.glDrawElements(GL.GL_POINTS, range[1], GL.GL_UNSIGNED_INT, (long) range[0] * Buffers.SIZEOF_INT);
        } finally {
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
            gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
            dc.getView().popReferenceCenter(dc);
            stack.pop(gl);
        }
    }

    /**
     * @return <code>true</code> if the view changed in the last {@link #MOVING_DELAY_MS}.
     */
    private boolean isViewMoving( DrawContext dc ) {
        Matrix modelview = dc.getView().getModelviewMatrix();
        long now = System.currentTimeMillis();
        if (lastModelview == null) {
            lastModelview = modelview;
            return false;
        }
        if (modelview != null && !modelview.equals(lastModelview)) {
            lastModelview = modelview;
            lastMoveTime = now;
        }
        return now - lastMoveTime < MOVING_DELAY_MS;
    }

    /**
     * Ask a redraw a bit after the view stopped, to draw all the points.
     */
    private void scheduleFullRedraw() {
        SwingUtilities.invokeLater(() -> {
            if (stopTimer == null) {
                stopTimer = new Timer(MOVING_DELAY_MS + 50, e -> firePropertyChange(AVKey.LAYER, null, this));
                stopTimer.setRepeats(false);
            }
            stopTimer.restart();
        });
    }

    /**
     * Compute the vertices of the points, if the points, the globe or the vertical exaggeration
     * changed, leaving them in {@link #vertices} to be uploaded.
     */
    private void updateVertices( DrawContext dc, Positions p ) {
        Globe globe = dc.getGlobe();
        double exaggeration = dc.getVerticalExaggeration();
        if (p == verticesPositions && globe == verticesGlobe && exaggeration == verticesExaggeration) {
            return;
        }
        Vec4 ref = globe.computePointFromPosition(Angle.fromDegrees(p.centerLat), Angle.fromDegrees(p.centerLon),
                p.centerElev * exaggeration);
        int n = p.size();
        FloatBuffer buffer = Buffers.newDirectFloatBuffer(n * 3);
        // in parallel, since with millions of points it takes seconds; every point writes its own
        // positions of the buffer, without touching the shared position
        IntStream.range(0, n).parallel().forEach(i -> {
            Vec4 v = globe.computePointFromPosition(Angle.fromDegrees(p.centerLat + p.lats[i]),
                    Angle.fromDegrees(p.centerLon + p.lons[i]), (p.centerElev + p.elevs[i]) * exaggeration);
            int index = i * 3;
            buffer.put(index, (float) (v.x - ref.x));
            buffer.put(index + 1, (float) (v.y - ref.y));
            buffer.put(index + 2, (float) (v.z - ref.z));
        });
        buffer.limit(n * 3);
        referencePoint = ref;
        vertices = buffer;
        verticesPositions = p;
        verticesGlobe = globe;
        verticesExaggeration = exaggeration;
    }

    /**
     * Upload in the graphics card what changed: the vertices and the colors and sorted indexes.
     *
     * @return <code>true</code> if the buffers are ready.
     */
    private boolean upload( GL2 gl, Data d ) {
        boolean uploadColoring = d.coloring != uploadedColoring;
        if (vertices == null && !uploadColoring) {
            return true;
        }
        // drop pending errors, to check only the ones of the upload
        while( gl.glGetError() != GL.GL_NO_ERROR ) {
        }
        if (vbos == null) {
            vbos = new int[4];
            gl.glGenBuffers(vbos.length, vbos, 0);
        }
        if (vertices != null) {
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbos[0]);
            gl.glBufferData(GL.GL_ARRAY_BUFFER, (long) vertices.limit() * Buffers.SIZEOF_FLOAT, vertices.rewind(),
                    GL.GL_STATIC_DRAW);
            vertices = null;
        }
        if (uploadColoring) {
            Coloring c = d.coloring;
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbos[1]);
            gl.glBufferData(GL.GL_ARRAY_BUFFER, c.colors.limit(), c.colors.rewind(), GL.GL_STATIC_DRAW);
            uploadIndexes(gl, vbos[2], c.order);
            uploadIndexes(gl, vbos[3], c.movingOrder != null ? c.movingOrder : new int[0]);
            uploadedColoring = c;
        }
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        int error = gl.glGetError();
        if (error != GL.GL_NO_ERROR) {
            failedData = d;
            releaseBuffers(gl);
            Logging.logger().severe(String.format(
                    "Unable to load the %,d points of the layer %s in the graphics card (OpenGL error 0x%x, %s): "
                            + "load less points, e.g. with a higher sampling.",
                    d.positions.size(), title, error, error == GL.GL_OUT_OF_MEMORY ? "out of memory" : "error"));
            return false;
        }
        return true;
    }

    private static void uploadIndexes( GL2 gl, int vbo, int[] indexes ) {
        IntBuffer buffer = Buffers.newDirectIntBuffer(Math.max(1, indexes.length));
        buffer.put(indexes);
        buffer.flip();
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vbo);
        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, (long) indexes.length * Buffers.SIZEOF_INT, buffer,
                GL.GL_STATIC_DRAW);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    /**
     * Delete the buffers in the graphics card, if any.
     */
    private void releaseBuffers( GL2 gl ) {
        if (vbos != null) {
            gl.glDeleteBuffers(vbos.length, vbos, 0);
            vbos = null;
            vbosContext = null;
        }
        vertices = null;
        verticesPositions = null;
        uploadedColoring = null;
    }

    @Override
    public String toString() {
        return title;
    }
}
