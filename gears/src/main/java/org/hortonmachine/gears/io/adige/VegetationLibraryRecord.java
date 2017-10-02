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

import java.util.Arrays;

/**
 * Container for vegetation library data.
 * 
 * <p><b>Note that months start from 1 (jan=1, dec=12).</b></p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class VegetationLibraryRecord {  
    private final int id;
    private final double architecturalResistance;
    private final double minStomatalResistanc;
    private final double[] laiMonths;
    private final double[] albedoMonths;
    private final double[] roughMonths;
    private final double[] displMonths;
    private final double windHeight;
    private final double windAtten;
    private final double rgl;
    private final double radAtten;
    private final double trunkRatio;

    public VegetationLibraryRecord( int id, double architecturalResistance,
            double minStomatalResistanc, double[] laiMonths, double[] albedoMonths,
            double[] roughMonths, double[] displMonths, double windHeight, double windAtten,
            double rgl, double radAtten, double trunkRatio ) {
        this.id = id;
        this.architecturalResistance = architecturalResistance;
        this.minStomatalResistanc = minStomatalResistanc;
        this.laiMonths = laiMonths;
        this.albedoMonths = albedoMonths;
        this.roughMonths = roughMonths;
        this.displMonths = displMonths;
        this.windHeight = windHeight;
        this.windAtten = windAtten;
        this.rgl = rgl;
        this.radAtten = radAtten;
        this.trunkRatio = trunkRatio;
    }

    public int getId() {
        return id;
    }

    public double getArchitecturalResistance() {
        return architecturalResistance;
    }

    public double getMinStomatalResistance() {
        return minStomatalResistanc;
    }

    /**
     * Getter.
     * 
     * @param month the month (jan = 1, dec = 12)
     * @return lai for the given month.
     */
    public double getLai( int month ) {
        return laiMonths[month - 1];
    }

    /**
     * Getter.
     * 
     * @param month the month (jan = 1, dec = 12)
     * @return albedo for the given month.
     */
    public double getAlbedo( int month ) {
        return albedoMonths[month - 1];
    }

    /**
     * Getter.
     * 
     * @param month the month (jan = 1, dec = 12)
     * @return roughness for the given month.
     */
    public double getRoughness( int month ) {
        return roughMonths[month - 1];
    }

    /**
     * Getter.
     * 
     * @param month the month (jan = 1, dec = 12)
     * @return displ for the given month.
     */
    public double getDisplacement( int month ) {
        return displMonths[month - 1];
    }

    public double getWindHeight() {
        return windHeight;
    }

    public double getWindAtten() {
        return windAtten;
    }

    public double getRgl() {
        return rgl;
    }

    public double getRadAtten() {
        return radAtten;
    }

    public double getTrunkRatio() {
        return trunkRatio;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("VegetationLibraryRecord [\n\talbedoMonths=");
        builder.append(Arrays.toString(albedoMonths));
        builder.append(", \n\tarchitecturalResistance=");
        builder.append(architecturalResistance);
        builder.append(", \n\tdisplMonths=");
        builder.append(Arrays.toString(displMonths));
        builder.append(", \n\tid=");
        builder.append(id);
        builder.append(", \n\tlaiMonths=");
        builder.append(Arrays.toString(laiMonths));
        builder.append(", \n\tminStomatalResistanc=");
        builder.append(minStomatalResistanc);
        builder.append(", \n\tradAtten=");
        builder.append(radAtten);
        builder.append(", \n\trgl=");
        builder.append(rgl);
        builder.append(", \n\troughMonths=");
        builder.append(Arrays.toString(roughMonths));
        builder.append(", \n\ttrunkRatio=");
        builder.append(trunkRatio);
        builder.append(", \n\twindAtten=");
        builder.append(windAtten);
        builder.append(", \n\twindHeight=");
        builder.append(windHeight);
        builder.append("\n]");
        return builder.toString();
    }

}
