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
package org.hortonmachine.gui.utils;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;

/**
 * An icon with a small badge, another icon scaled down, in its bottom-right
 * corner.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class BadgedIcon implements Icon {

    /** Badge size as a fraction of the base icon's height. */
    private static final double BADGE_FRACTION = 0.6;

    private final Icon base;
    private final Icon badge;

    public BadgedIcon( Icon base, Icon badge ) {
        this.base = base;
        this.badge = badge;
    }

    @Override
    public void paintIcon( Component c, Graphics g, int x, int y ) {
        base.paintIcon(c, g, x, y);
        int size = (int) Math.round(base.getIconHeight() * BADGE_FRACTION);
        if (size <= 0 || badge.getIconWidth() <= 0 || badge.getIconHeight() <= 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.translate(x + base.getIconWidth() - size, y + base.getIconHeight() - size);
            g2.scale(size / (double) badge.getIconWidth(), size / (double) badge.getIconHeight());
            badge.paintIcon(c, g2, 0, 0);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        return base.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return base.getIconHeight();
    }
}
