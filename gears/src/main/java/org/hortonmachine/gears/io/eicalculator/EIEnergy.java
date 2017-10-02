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
package org.hortonmachine.gears.io.eicalculator;


/**
 * Container for Energy data in the {@link EnergyIndexCalculator}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class EIEnergy {

    /**
     * The basin id.
     */
    public int basinId;
    /**
     * The id of the energetic band.
     */
    public int energeticBandId;
    /**
     * The virtual month.
     * 
     * The virtual months are divided as follows:  <br>
     * 0: 22 DICEMBRE - 20 GENNAIO <br>
     * 1: 21 GENNAIO - 20 FEBBRAIO <br>
     * 2: 21 FEBBRAIO - 22 MARZO <br>
     * 3: 23 MARZO - 22 APRILE <br>
     * 4: 23 APRILE - 22 MAGGIO <br>
     * 5: 23 MAGGIO - 22 GIUGNO
     */
    public int virtualMonth;
    /**
     * The value of energy.
     */
    public double energyValue;
}
