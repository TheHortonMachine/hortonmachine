package org.hortonmachine.lesto.modules.vegetation.watershed;

/*
 * Watershed algorithm
 *
 * Copyright (c) 2003 by Christopher Mei (christopher.mei@sophia.inria.fr)
 *
 * This plugin is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this plugin; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;

/**
 *  WatershedStructure contains the pixels
 *  of the image ordered according to their
 *  grayscale value with a direct access to their
 *  neighbours.
 *  
 **/

public class WatershedStructure {
    private List<WatershedPixel> watershedStructure;

    public WatershedStructure( byte[] pixels, int cols, int rows, IHMProgressMonitor pm ) {
        int offset, topOffset, bottomOffset, i;

        watershedStructure = new ArrayList<WatershedPixel>(cols * rows);

        /** The structure is filled with the pixels of the image. **/
        pm.beginTask("Fill Watershed structure...", rows);
        for( int r = 0; r < rows; r++ ) {
            offset = r * cols;
            for( int c = 0; c < cols; c++ ) {
                i = offset + c;

                int indiceY = r;
                int indiceX = c;

                watershedStructure.add(new WatershedPixel(indiceX, indiceY, pixels[i]));
            }
            pm.worked(1);
        }
        pm.done();

        /** The WatershedPixels are then filled with the reference to their neighbours. **/
        pm.beginTask("Add neighbours references...", rows);
        for( int r = 0; r < rows; r++ ) {

            offset = r * cols;
            topOffset = offset + cols;
            bottomOffset = offset - cols;

            for( int c = 0; c < cols; c++ ) {
                WatershedPixel currentPixel = (WatershedPixel) watershedStructure.get(c + offset);

                if (c + 1 < cols) {
                    currentPixel.addNeighbour((WatershedPixel) watershedStructure.get(c + 1 + offset));

                    if (r - 1 >= 0)
                        currentPixel.addNeighbour((WatershedPixel) watershedStructure.get(c + 1 + bottomOffset));

                    if (r + 1 < rows)
                        currentPixel.addNeighbour((WatershedPixel) watershedStructure.get(c + 1 + topOffset));
                }

                if (c - 1 >= 0) {
                    currentPixel.addNeighbour((WatershedPixel) watershedStructure.get(c - 1 + offset));

                    if (r - 1 >= 0)
                        currentPixel.addNeighbour((WatershedPixel) watershedStructure.get(c - 1 + bottomOffset));

                    if (r + 1 < rows)
                        currentPixel.addNeighbour((WatershedPixel) watershedStructure.get(c - 1 + topOffset));
                }

                if (r - 1 >= 0)
                    currentPixel.addNeighbour((WatershedPixel) watershedStructure.get(c + bottomOffset));

                if (r + 1 < rows)
                    currentPixel.addNeighbour((WatershedPixel) watershedStructure.get(c + topOffset));
            }
            pm.worked(1);
        }
        pm.done();

        Collections.sort(watershedStructure);
    }

    public String toString() {
        StringBuffer ret = new StringBuffer();

        for( int i = 0; i < watershedStructure.size(); i++ ) {
            ret.append(((WatershedPixel) watershedStructure.get(i)).toString());
            ret.append("\n");
            ret.append("Neighbours :\n");

            List<WatershedPixel> neighbours = watershedStructure.get(i).getNeighbours();

            for( int j = 0; j < neighbours.size(); j++ ) {
                ret.append(neighbours.get(j).toString());
                ret.append("\n");
            }
            ret.append("\n");
        }
        return ret.toString();
    }

    public int size() {
        return watershedStructure.size();
    }

    public WatershedPixel get( int i ) {
        return (WatershedPixel) watershedStructure.get(i);
    }
}
