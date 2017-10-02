/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.gui.spatialtoolbox.core;

/**
 * Helper class to check on field type.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class TypeCheck {
    /**
     * If <code>true</code> this is a file (might still be folder).
     */
    boolean isFile;

    /**
     * If <code>true</code>, the file is a folder.
     */
    boolean isFolder;
    /**
     * If <code>true</code>, the file is to be saved/created.
     */
    boolean isOutput;

    /**
     * If <code>true</code>, a crs epsg is expected.
     */
    boolean isCrs;

    boolean isProcessingYres;

    boolean isProcessingXres;

    boolean isProcessingRows;

    boolean isProcessingCols;

    boolean isProcessingEast;

    boolean isProcessingWest;

    boolean isProcessingSouth;

    boolean isProcessingNorth;

    boolean isGrassfile;

    boolean isMapcalc;

    boolean isNorthing;

    boolean isEasting;

    boolean isEastingNorthing;
}