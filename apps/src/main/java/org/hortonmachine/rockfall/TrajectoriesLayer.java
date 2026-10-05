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
package org.hortonmachine.rockfall;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.StoneTrajectory;
import org.hortonmachine.nww.layers.defaults.NwwLayer;
import org.locationtech.jts.geom.Coordinate;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.Path;

/**
 * The 3D trajectories of the STONE boulders, colored by velocity.
 *
 * <p>The points are placed over the rendered terrain at the height of the boulders over the
 * terrain of the simulation, so that rolling boulders lie on the surface even where the rendered
 * terrain differs a bit from the DEM.</p>
 */
public class TrajectoriesLayer extends RenderableLayer implements NwwLayer {
    /** Lift over the surface, against the flickering of rolling paths into the terrain [m]. */
    private static final double SURFACE_OFFSET = 0.3;

    private Coordinate center;

    /**
     * @param name the name of the layer.
     * @param trajectories the trajectories, in the coordinates of the maps of the simulation.
     * @param crs the CRS of the maps of the simulation.
     * @param speedColor the color of a velocity [m/s].
     */
    public TrajectoriesLayer( String name, List<StoneTrajectory> trajectories, CoordinateReferenceSystem crs,
            DoubleFunction<Color> speedColor ) throws Exception {
        setName(name);
        setPickEnabled(false);
        MathTransform toLonLat = CRS.findMathTransform(crs, HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true), true);

        BasicShapeAttributes attributes = new BasicShapeAttributes();
        attributes.setOutlineWidth(2);
        attributes.setOutlineMaterial(Material.WHITE);
        attributes.setOutlineOpacity(0.9);

        double sumLon = 0, sumLat = 0;
        long count = 0;
        for( StoneTrajectory trajectory : trajectories ) {
            int size = trajectory.size();
            double[] coordinates = new double[size * 2];
            for( int i = 0; i < size; i++ ) {
                coordinates[i * 2] = trajectory.getX(i);
                coordinates[i * 2 + 1] = trajectory.getY(i);
            }
            toLonLat.transform(coordinates, 0, coordinates, 0, size);

            List<Position> positions = new ArrayList<>(size);
            Color[] colors = new Color[size];
            for( int i = 0; i < size; i++ ) {
                double lon = coordinates[i * 2];
                double lat = coordinates[i * 2 + 1];
                double height = Math.max(0, trajectory.getHeight(i)) + SURFACE_OFFSET;
                positions.add(Position.fromDegrees(lat, lon, height));
                colors[i] = speedColor.apply(trajectory.getSpeed(i));
                sumLon += lon;
                sumLat += lat;
                count++;
            }

            Path path = new Path(positions);
            path.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
            path.setPathType(AVKey.LINEAR);
            path.setFollowTerrain(false);
            path.setShowPositions(false);
            path.setAttributes(attributes);
            path.setPositionColors(( position, ordinal ) -> colors[ordinal]);
            addRenderable(path);
        }
        center = count > 0 ? new Coordinate(sumLon / count, sumLat / count) : new Coordinate(0, 0);
    }

    @Override
    public Coordinate getCenter() {
        return center;
    }

    @Override
    public String toString() {
        return getName();
    }
}
