/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow;

/**
 * @author Andrea Antonello - www.hydrologis.com
 */
public class EffectsBox {

    private double[][] ampi = null;
    private double[][] ampi_sub = null;

    private boolean isSubsuperficial = false;
    private boolean rainDataExists;

    /**
     * empty constructor. This is just a box which holds some check on existend and default value
     */
    public EffectsBox() {
    }

    public boolean isSubsuperficial() {
        return isSubsuperficial;
    }

    public void setSubsuperficial( boolean isSubsuperficial ) {
        this.isSubsuperficial = isSubsuperficial;
    }

    /*
     * SUPERFICIAL
     */
    public boolean ampiExists() {
        if (ampi != null) {
            return true;
        }
        return false;
    }

    public double[][] getAmpi() {
        return ampi;
    }

    public void setAmpi( double[][] ampi ) {
        this.ampi = ampi;
    }

    public boolean ampi_subExists() {
        if (ampi_sub != null) {
            return true;
        }
        return false;
    }

    public double[][] getAmpi_sub() {
        return ampi_sub;
    }

    public void setAmpi_sub( double[][] ampi_sub ) {
        this.ampi_sub = ampi_sub;
    }

    public void setRainDataExists( boolean rainDataExists ) {
        this.rainDataExists = rainDataExists;

    }
    public boolean rainDataExists() {
        return rainDataExists;
    }

}
