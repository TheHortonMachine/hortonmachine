/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.hortonmachine.gears.utils.style;

/**
 * Enumeration of possible vendor options.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum VendorOptions {
    VENDOROPTION_MAXDISPLACEMENT("maxDisplacement"), // //$NON-NLS-1$
    VENDOROPTION_AUTOWRAP("autoWrap"), // //$NON-NLS-1$
    VENDOROPTION_SPACEAROUND("spaceAround"), // //$NON-NLS-1$
    VENDOROPTION_REPEAT("repeat"), // //$NON-NLS-1$
    VENDOROPTION_MAXANGLEDELTA("maxAngleDelta"), // //$NON-NLS-1$
    VENDOROPTION_FOLLOWLINE("followLine"), // //$NON-NLS-1$
    VENDOROPTION_OTHER("other"); //$NON-NLS-1$

    private String defString = null;
    VendorOptions( String defString ) {
        this.defString = defString;
    }

    /**
     * Return the vendoroption based on the definition string.
     * 
     * @param defString the option definition string.
     * @return the {@link VendorOptions} or null.
     */
    public static VendorOptions toVendorOption( String defString ) {
        VendorOptions[] values = values();
        for( VendorOptions vendorOptions : values ) {
            if (defString.equals(vendorOptions.toString())) {
                return vendorOptions;
            }
        }
        return VendorOptions.VENDOROPTION_OTHER;
    }

    public String toString() {
        return defString;
    }

}
