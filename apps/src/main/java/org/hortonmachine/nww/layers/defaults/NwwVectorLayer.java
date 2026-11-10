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
package org.hortonmachine.nww.layers.defaults;

import org.geotools.api.style.Style;
import org.hortonmachine.style.SimpleStyle;

/**
 * Common interface for layers.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public interface NwwVectorLayer extends NwwLayer {
    public enum GEOMTYPE {
        POINT, LINE, POLYGON, UNKNOWN
    };

    public GEOMTYPE getType();

    public void setStyle(SimpleStyle style);

    public SimpleStyle getStyle();

    /**
     * Draw the features with an SLD style: each with the look of the rule it matches, with the
     * labels of the style. The features are loaded again.
     *
     * @param style the style, <code>null</code> to go back to the one simple style of
     *          {@link #setStyle(SimpleStyle)}.
     * @throws UnsupportedOperationException if the layer can't draw SLD styles.
     */
    default void setSldStyle( Style style ) {
        throw new UnsupportedOperationException("The layer can't draw SLD styles: " + this);
    }

    /**
     * @return the SLD style given with {@link #setSldStyle(Style)}, or <code>null</code>.
     */
    default Style getSldStyle() {
        return null;
    }
}
