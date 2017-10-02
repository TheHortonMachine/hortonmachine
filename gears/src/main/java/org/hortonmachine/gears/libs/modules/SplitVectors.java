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
package org.hortonmachine.gears.libs.modules;

/**
 * Support class to simulate structure to help the C port
 * 
 * @author Andrea Antonello - www.hydrologis.com
 */
public class SplitVectors {
    public double[][] splitValues1 = null;
    public double[][] splitValues2 = null;
    public double[] splitIndex = null;

    public SplitVectors() {
    }

    public void initValues( int rows, int cols ) {
        splitValues1 = new double[rows][cols];
        splitValues2 = new double[rows][cols];
    }

    public void initIndex( int count ) {
        splitIndex = new double[count];
    }

}
