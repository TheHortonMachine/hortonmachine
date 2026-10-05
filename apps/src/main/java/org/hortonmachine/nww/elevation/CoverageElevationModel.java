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
package org.hortonmachine.nww.elevation;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.nww.utils.LonLatGrid;
import org.hortonmachine.nww.utils.LonLatGrid.Registration;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import gov.nasa.worldwind.terrain.LocalElevationModel;

/**
 * Elevation models for the NWW terrain from a DEM coverage, in any CRS.
 *
 * <p>The DEM is resampled in memory (bilinear) on a regular longitude/latitude grid with about its
 * own resolution, since the elevation models of NWW work in geographic coordinates.</p>
 */
public class CoverageElevationModel {
    private static final double MISSING = -32768.0;
    private static final int MAX_SIZE = 8192;

    private CoverageElevationModel() {
    }

    /**
     * Create the elevation model of a DEM, with the given terrain around it.
     *
     * <p>Under everything there is a flat terrain at the lowest elevation of the DEM. So without a
     * surrounding model the DEM stands on a plain instead of on a cliff down to the sea level, and
     * with a surrounding model that loads its data progressively, as the Copernicus one, the parts
     * not loaded yet are at that level.</p>
     *
     * @param dem the DEM.
     * @param surrounding the elevation model outside of the DEM, e.g. the Copernicus one, or
     *            <code>null</code> for a flat terrain.
     * @return the elevation model.
     */
    public static ElevationModel withSurrounding( GridCoverage2D dem, ElevationModel surrounding ) throws Exception {
        LocalElevationModel demModel = fromCoverage(dem);
        CompoundElevationModel compound = new CompoundElevationModel();
        compound.addElevationModel(new ConstantElevationModel(demModel.getMinElevation()));
        if (surrounding != null) {
            compound.addElevationModel(surrounding);
        }
        compound.addElevationModel(demModel);
        return compound;
    }

    /**
     * Create the elevation model of a DEM.
     *
     * @param dem the DEM.
     * @return the elevation model, defined in the geographic extent of the DEM.
     */
    public static LocalElevationModel fromCoverage( GridCoverage2D dem ) throws Exception {
        LonLatGrid grid = LonLatGrid.resample(dem, Registration.GRID, true, 1, MAX_SIZE);
        int width = grid.getWidth();
        int height = grid.getHeight();
        ByteBuffer buffer = ByteBuffer.allocate(width * height * 4).order(ByteOrder.BIG_ENDIAN);
        double minElevation = Double.POSITIVE_INFINITY;
        double maxElevation = Double.NEGATIVE_INFINITY;
        for( int j = 0; j < height; j++ ) {
            for( int i = 0; i < width; i++ ) {
                double value = grid.getValue(i, j);
                if (Double.isNaN(value)) {
                    buffer.putFloat((float) MISSING);
                } else {
                    buffer.putFloat((float) value);
                    minElevation = Math.min(minElevation, value);
                    maxElevation = Math.max(maxElevation, value);
                }
            }
        }
        buffer.rewind();

        AVList params = new AVListImpl();
        params.setValue(AVKey.DATA_TYPE, AVKey.FLOAT32);
        params.setValue(AVKey.BYTE_ORDER, AVKey.BIG_ENDIAN);
        params.setValue(AVKey.MISSING_DATA_SIGNAL, MISSING);
        if (minElevation <= maxElevation) {
            params.setValue(AVKey.ELEVATION_MIN, minElevation);
            params.setValue(AVKey.ELEVATION_MAX, maxElevation);
        }
        LocalElevationModel model = new LocalElevationModel();
        model.setMissingDataSignal(MISSING);
        model.addElevations(buffer, grid.getSector(), width, height, params);
        return model;
    }
}
