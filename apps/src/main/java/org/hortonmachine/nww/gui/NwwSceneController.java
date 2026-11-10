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
package org.hortonmachine.nww.gui;

import gov.nasa.worldwind.StereoOptionSceneController;
import gov.nasa.worldwind.render.DrawContext;

/**
 * The scene controller of the viewer, which draws the surface shapes (the polygons and lines of
 * the vector layers) independently of the opacity of the image layers.
 *
 * <p>World Wind draws the surface shapes of all layers together, after all layers, modulated by
 * the current OpenGL color. A tiled image layer drawn with an opacity below 1 leaves that opacity
 * in the current color, despite saving and restoring it, so the vectors got the opacity of the
 * raster above them. The color is set to opaque white before the shapes are drawn.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class NwwSceneController extends StereoOptionSceneController {

    @Override
    protected void drawOrderedSurfaceRenderables( DrawContext dc ) {
        dc.getGL().getGL2().glColor4f(1f, 1f, 1f, 1f);
        super.drawOrderedSurfaceRenderables(dc);
    }
}
