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
package org.hortonmachine.nww.layers.defaults.raster;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.function.DoubleFunction;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.nww.layers.defaults.NwwLayer;
import org.hortonmachine.nww.utils.LonLatGrid;
import org.hortonmachine.nww.utils.LonLatGrid.Registration;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.SurfaceImage;

/**
 * An in memory raster draped on the terrain, colored by a function of its values, with novalues
 * transparent.
 *
 * <p>Differently from {@link GridCoverageNwwLayer}, which renders tiles of raster files on demand
 * with an SLD style and caches them on disk, the raster is kept in memory as a single image on a
 * longitude/latitude grid. It is meant for rasters of moderate size that change often, as the
 * results of a model run.</p>
 */
public class ColoredGridSurfaceLayer extends RenderableLayer implements NwwLayer {
    private static final int MAX_SIZE = 4096;

    private final Sector sector;

    /**
     * @param name the name of the layer.
     * @param coverage the raster.
     * @param colorFunction the color of a value, <code>null</code> for transparent.
     * @param oversampling the image pixels per cell size, more for sharper cell edges.
     */
    public ColoredGridSurfaceLayer( String name, GridCoverage2D coverage, DoubleFunction<Color> colorFunction,
            double oversampling ) throws Exception {
        setName(name);
        setPickEnabled(false);
        LonLatGrid grid = LonLatGrid.resample(coverage, Registration.PIXEL, false, oversampling, MAX_SIZE);
        int width = grid.getWidth();
        int height = grid.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for( int j = 0; j < height; j++ ) {
            for( int i = 0; i < width; i++ ) {
                double value = grid.getValue(i, j);
                if (!Double.isNaN(value)) {
                    Color color = colorFunction.apply(value);
                    if (color != null) {
                        image.setRGB(i, j, color.getRGB());
                    }
                }
            }
        }
        sector = grid.getSector();
        addRenderable(new SurfaceImage(image, sector));
    }

    public Sector getSector() {
        return sector;
    }

    @Override
    public Coordinate getCenter() {
        return new Coordinate(sector.getCentroid().longitude.degrees, sector.getCentroid().latitude.degrees);
    }

    @Override
    public Envelope getBounds() {
        return new Envelope(sector.getMinLongitude().degrees, sector.getMaxLongitude().degrees,
                sector.getMinLatitude().degrees, sector.getMaxLatitude().degrees);
    }

    @Override
    public String toString() {
        return getName();
    }
}
