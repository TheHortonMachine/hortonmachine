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

import java.awt.Color;

import com.jogamp.opengl.GL;

import gov.nasa.worldwind.StereoOptionSceneController;
import gov.nasa.worldwind.render.DrawContext;

/**
 * The scene controller of the viewer, which draws the surface shapes (the polygons and lines of
 * the vector layers) independently of the opacity of the image layers and can paint the space
 * around the globe with a color other than black.
 *
 * <p>World Wind draws the surface shapes of all layers together, after all layers, modulated by
 * the current OpenGL color. A tiled image layer drawn with an opacity below 1 leaves that opacity
 * in the current color, despite saving and restoring it, so the vectors got the opacity of the
 * raster above them. The color is set to opaque white before the shapes are drawn.</p>
 *
 * <p>The frame is cleared with the clear color of the draw context, which is also the color that
 * the picking reads as "no object", so it can't be changed. The background color is instead
 * painted once per frame, just before the layers are drawn, which happens only in the drawing
 * pass and not in the picking one.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class NwwSceneController extends StereoOptionSceneController {

    /** The color of the space around the globe, <code>null</code> for the default black. */
    private volatile Color backgroundColor = null;
    /** If the background color has already been painted in the current frame. */
    private boolean backgroundPainted = false;

    /**
     * @return the color of the space around the globe, or <code>null</code> for the default black.
     */
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Set the color of the space around the globe.
     *
     * @param backgroundColor the color, or <code>null</code> for the default black.
     */
    public void setBackgroundColor( Color backgroundColor ) {
        this.backgroundColor = backgroundColor;
    }

    @Override
    protected void clearFrame( DrawContext dc ) {
        super.clearFrame(dc);
        backgroundPainted = false;
    }

    @Override
    protected void draw( DrawContext dc ) {
        // the 2D continuous globe draws the layers more times per frame, the background must be
        // painted only before the first
        if (!backgroundPainted) {
            backgroundPainted = true;
            Color bg = backgroundColor;
            if (bg != null) {
                GL gl = dc.getGL();
                gl.glClearColor(bg.getRed() / 255f, bg.getGreen() / 255f, bg.getBlue() / 255f, bg.getAlpha() / 255f);
                gl.glClear(GL.GL_COLOR_BUFFER_BIT);
            }
        }
        super.draw(dc);
    }

    @Override
    protected void drawOrderedSurfaceRenderables( DrawContext dc ) {
        dc.getGL().getGL2().glColor4f(1f, 1f, 1f, 1f);
        super.drawOrderedSurfaceRenderables(dc);
    }
}
