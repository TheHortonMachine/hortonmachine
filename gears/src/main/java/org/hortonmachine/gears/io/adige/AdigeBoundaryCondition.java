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
package org.hortonmachine.gears.io.adige;

/**
 * Initial conditions for the Adige model.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class AdigeBoundaryCondition {
    private int basinId = -1;
    private double discharge = Double.NaN;
    private double dischargeSub = Double.NaN;
    private double s1 = Double.NaN;
    private double s2 = Double.NaN;

    public int getBasinId() {
        return basinId;
    }
    public void setBasinId(int basinId) {
        this.basinId = basinId;
    }
    public double getDischarge() {
        return discharge;
    }
    public void setDischarge(double discharge) {
        this.discharge = discharge;
    }
    public double getDischargeSub() {
        return dischargeSub;
    }
    public void setDischargeSub(double dischargeSub) {
        this.dischargeSub = dischargeSub;
    }
    public double getS1() {
        return s1;
    }
    public void setS1(double s1) {
        this.s1 = s1;
    }
    public double getS2() {
        return s2;
    }
    public void setS2(double s2) {
        this.s2 = s2;
    }
}
