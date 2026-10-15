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
package org.hortonmachine.gears.utils.geometry;

/**
 * The precision of the coordinates, as the size on the ground of the grid they are rounded to.
 *
 * <p>See {@link HMPrecision} for how it is turned into the grid of a coordinate reference system,
 * also in degrees.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public enum EPrecision {
    /** A millimeter. */
    MM(0.001),
    /** A centimeter. */
    CM(0.01),
    /** A decimeter. */
    DM(0.1),
    /** A meter. */
    M(1.0);

    private final double meters;

    private EPrecision( double meters ) {
        this.meters = meters;
    }

    /**
     * @return the size of the grid, in meters.
     */
    public double getMeters() {
        return meters;
    }

    /**
     * @param name the name of the precision, as mm or CM, ignoring the case.
     * @return the precision.
     * @throws IllegalArgumentException if the name is not one of the precisions.
     */
    public static EPrecision forName( String name ) {
        for( EPrecision precision : values() ) {
            if (precision.name().equalsIgnoreCase(name.trim())) {
                return precision;
            }
        }
        throw new IllegalArgumentException("Unknown precision: " + name + ". Use one of mm, cm, dm or m.");
    }
}
