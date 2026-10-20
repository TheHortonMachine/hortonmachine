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
package org.hortonmachine.gears.utils.display;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Renders images as they are.
 *
 * <p>Images that are not {@link BufferedImage}s are copied into one.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class ImageDisplayRenderer implements IDisplayRenderer {

    @Override
    public boolean canRender( Object value ) {
        return value instanceof Image;
    }

    @Override
    public List<DisplayPart> render( Object value, DisplayOptions options ) throws Exception {
        if (value instanceof BufferedImage) {
            return List.of(new DisplayImage((BufferedImage) value));
        }
        Image image = (Image) value;
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        if (width <= 0 || height <= 0) {
            return List.of(new DisplayText("Image not loaded yet."));
        }
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = buffered.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return List.of(new DisplayImage(buffered));
    }
}
