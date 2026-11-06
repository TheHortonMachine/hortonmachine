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
package org.hortonmachine.gui.utils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

import org.hortonmachine.dbs.log.Logger;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.UIScale;

/**
 * An icon drawn from a glyph of the bundled Material Symbols font.
 *
 * <p>The color is taken from the current look and feel at paint time, so the icon follows
 * theme changes. It is scaled with the FlatLaf ui scale.</p>
 *
 * <p>It extends {@link ImageIcon} to stay compatible with code that expects the icons of the
 * {@link ImageCache}.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class MaterialIcon extends ImageIcon implements FlatLaf.DisabledIconProvider {
    private static final long serialVersionUID = 1L;

    private static final String FONT_PATH = "/org/hortonmachine/gui/icons/MaterialSymbolsOutlined-hm.ttf";

    /**
     * The role of an icon, which defines its color through a theme key.
     */
    public enum IconColor {
        /** Normal icons. */
        DEFAULT("HM.icon.foreground", null, null),
        /** Run and start actions. */
        RUN("HM.icon.runColor", new Color(0x43A047), new Color(0x66BB6A)),
        /** Stop and cancel actions. */
        STOP("HM.icon.stopColor", new Color(0xD32F2F), new Color(0xEF5350)),
        /** Highlighted icons. */
        ACCENT("HM.icon.accentColor", null, null),
        /** Disabled icons. */
        DISABLED("Label.disabledForeground", Color.GRAY, Color.GRAY);

        private final String key;
        private final Color lightFallback;
        private final Color darkFallback;

        private IconColor( String key, Color lightFallback, Color darkFallback ) {
            this.key = key;
            this.lightFallback = lightFallback;
            this.darkFallback = darkFallback;
        }

        /**
         * @return the color of the role for the current look and feel.
         */
        public Color getColor() {
            Color color = UIManager.getColor(key);
            if (color != null) {
                return color;
            }
            switch( this ) {
            case DEFAULT:
                return fallback(UIManager.getColor("Label.foreground"), Color.DARK_GRAY);
            case ACCENT:
                return fallback(UIManager.getColor("Component.accentColor"), new Color(0x1976D2));
            default:
                return FlatLaf.isLafDark() ? darkFallback : lightFallback;
            }
        }

        private static Color fallback( Color color, Color fallback ) {
            return color != null ? color : fallback;
        }
    }

    private static Font baseFont;
    private static boolean fontLoaded = false;

    private final char glyph;
    private final int size;
    private final IconColor iconColor;

    /**
     * Create a new icon.
     *
     * @param glyph the codepoint of the glyph in the Material Symbols font.
     * @param size the unscaled size of the icon in pixels.
     * @param iconColor the role that defines the color.
     */
    public MaterialIcon( char glyph, int size, IconColor iconColor ) {
        this.glyph = glyph;
        this.size = size;
        this.iconColor = iconColor;
    }

    /**
     * @return <code>true</code> if the icon font could be loaded.
     */
    public static boolean isAvailable() {
        return getBaseFont() != null;
    }

    private static synchronized Font getBaseFont() {
        if (!fontLoaded) {
            fontLoaded = true;
            try (InputStream in = MaterialIcon.class.getResourceAsStream(FONT_PATH)) {
                if (in != null) {
                    baseFont = Font.createFont(Font.TRUETYPE_FONT, in);
                }
            } catch (Exception e) {
                Logger.INSTANCE.insertError("MaterialIcon", "Unable to load the icon font " + FONT_PATH, e);
            }
        }
        return baseFont;
    }

    @Override
    public int getIconWidth() {
        return UIScale.scale(size);
    }

    @Override
    public int getIconHeight() {
        return UIScale.scale(size);
    }

    @Override
    public void paintIcon( Component c, Graphics g, int x, int y ) {
        IconColor role = c != null && !c.isEnabled() ? IconColor.DISABLED : iconColor;
        paint(g, x, y, role.getColor());
    }

    private void paint( Graphics g, int x, int y, Color color ) {
        Font font = getBaseFont();
        if (font == null) {
            return;
        }
        int s = getIconWidth();
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setFont(font.deriveFont((float) s));
            g2.setColor(color);
            // the glyphs fill the em square, which sits on the baseline
            g2.drawString(String.valueOf(glyph), x, y + s);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Renders the icon with its normal color, for code that needs an {@link Image}
     * (for example frame icons).
     */
    @Override
    public Image getImage() {
        int s = getIconWidth();
        BufferedImage image = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();
        paint(g, 0, 0, iconColor.getColor());
        g.dispose();
        return image;
    }

    @Override
    public Icon getDisabledIcon() {
        return new MaterialIcon(glyph, size, IconColor.DISABLED);
    }
}
