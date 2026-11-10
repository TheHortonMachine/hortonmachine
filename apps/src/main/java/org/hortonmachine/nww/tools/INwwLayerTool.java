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
package org.hortonmachine.nww.tools;

import javax.swing.JComponent;

import gov.nasa.worldwind.layers.Layer;

/**
 * A tool for a kind of layers, as the point clouds or the vector tables: its panel is shown in
 * the "Layer" tab of the viewer when a layer it handles is the current one, selected in the
 * layers panel.
 *
 * <p>Each layer gets a panel of its own, kept while the layer is in the viewer.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public interface INwwLayerTool {

    /**
     * @return the name of the tool, the title of the tab while its panel is shown.
     */
    String getName();

    /**
     * @param layer the layer.
     * @return <code>true</code> if the tool has a panel for the layer.
     */
    boolean canHandle( Layer layer );

    /**
     * Create the panel of a layer, called the first time the layer is the current one.
     *
     * @param context the access to the viewer.
     * @param layer the layer.
     * @return the panel.
     */
    JComponent createPanel( NwwContext context, Layer layer );

    /**
     * Called when the panel of a layer is dropped: the layer was removed or the viewer closed.
     *
     * @param layer the layer.
     */
    default void onClose( Layer layer ) {
    }
}
