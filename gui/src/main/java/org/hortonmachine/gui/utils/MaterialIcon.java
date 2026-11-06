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
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.swing.CellRendererPane;
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

    /**
     * How much the em square is enlarged over the icon size: 24/20 removes the built-in padding.
     */
    private static final float GLYPH_ENLARGEMENT = 24f / 20f;

    private static Font baseFont;
    private static boolean fontLoaded = false;

    private final char glyph;
    private final int size;
    private final IconColor iconColor;

    private transient Shape outline;
    private transient int outlineSize;

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
        Color selectionColor = getRendererSelectionColor(c);
        paint(g, x, y, selectionColor != null ? selectionColor : role.getColor());
    }

    /**
     * Inside a selected tree, table or list cell the icon takes the selection text color,
     * else an accent colored glyph vanishes on the accent colored selection.
     *
     * @return the selection foreground if c is a renderer of a selected cell, else <code>null</code>.
     */
    private static Color getRendererSelectionColor( Component c ) {
        if (c == null || !(c.getParent() instanceof CellRendererPane)) {
            return null;
        }
        Color fg = c.getForeground();
        if (fg == null) {
            return null;
        }
        for( String prefix : new String[]{"Tree", "Table", "List"} ) {
            Color selFg = UIManager.getColor(prefix + ".selectionForeground");
            Color normalFg = UIManager.getColor(prefix + ".foreground");
            if (selFg != null && !selFg.equals(normalFg) && selFg.getRGB() == fg.getRGB()) {
                return fg;
            }
        }
        return null;
    }

    private void paint( Graphics g, int x, int y, Color color ) {
        Shape glyphOutline = getOutline();
        if (glyphOutline == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setColor(color);
            // the font has no hinting: snap the outline to the device pixels (after the HiDPI scale),
            // else at most sizes its edges fall between two pixels and the icon looks blurry
            AffineTransform toDevice = g2.getTransform();
            toDevice.translate(x, y);
            Shape deviceOutline = snapToPixels(toDevice.createTransformedShape(glyphOutline));
            g2.setTransform(new AffineTransform());
            g2.fill(deviceOutline);
        } finally {
            g2.dispose();
        }
    }

    /**
     * @return the outline of the glyph, filling the icon box at the current ui scale, or
     *         <code>null</code> if the icon font is not available.
     */
    private Shape getOutline() {
        int s = getIconWidth();
        if (outline == null || outlineSize != s) {
            Font font = getBaseFont();
            if (font == null) {
                return null;
            }
            // the glyphs are drawn on a 24 unit grid with a 2 unit empty border (20/24 of the em square):
            // enlarge the em square so that the shape fills the icon box, as the other icons of the theme
            float em = s * GLYPH_ENLARGEMENT;
            float offset = (s - em) / 2f;
            GlyphVector glyphVector = font.deriveFont(em).createGlyphVector(new FontRenderContext(null, true, true),
                    String.valueOf(glyph));
            // the em square sits on the baseline
            outline = glyphVector.getOutline(offset, offset + em);
            outlineSize = s;
        }
        return outline;
    }

    /**
     * Round all the points of a shape to whole pixels. The straight edges of the glyphs (on whole
     * units of their 24 unit grid) so become sharp pixel borders, at any size.
     */
    private static Shape snapToPixels( Shape shape ) {
        Path2D.Float path = new Path2D.Float(Path2D.WIND_NON_ZERO);
        float[] c = new float[6];
        for( PathIterator it = shape.getPathIterator(null); !it.isDone(); it.next() ) {
            int type = it.currentSegment(c);
            for( int i = 0; i < c.length; i++ ) {
                c[i] = Math.round(c[i]);
            }
            switch( type ) {
            case PathIterator.SEG_MOVETO -> path.moveTo(c[0], c[1]);
            case PathIterator.SEG_LINETO -> path.lineTo(c[0], c[1]);
            case PathIterator.SEG_QUADTO -> path.quadTo(c[0], c[1], c[2], c[3]);
            case PathIterator.SEG_CUBICTO -> path.curveTo(c[0], c[1], c[2], c[3], c[4], c[5]);
            case PathIterator.SEG_CLOSE -> path.closePath();
            default -> throw new IllegalStateException("Unknown path segment: " + type);
            }
        }
        return path;
    }

    /**
     * Renders the icon with its normal color, for code that needs an {@link Image}
     * (for example frame icons). The image has variants for HiDPI screens, each snapped
     * to its own pixels.
     */
    @Override
    public Image getImage() {
        int s = getIconWidth();
        double[] scales = {1, 1.25, 1.5, 2, 3};
        Image[] variants = new Image[scales.length];
        for( int i = 0; i < scales.length; i++ ) {
            int px = (int) Math.ceil(s * scales[i]);
            BufferedImage image = new BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.scale(scales[i], scales[i]);
            paint(g, 0, 0, iconColor.getColor());
            g.dispose();
            variants[i] = image;
        }
        return new BaseMultiResolutionImage(variants);
    }

    /**
     * @param newSize the unscaled size in pixels.
     * @return the same glyph and color at another size.
     */
    public MaterialIcon withSize( int newSize ) {
        return newSize == size ? this : new MaterialIcon(glyph, newSize, iconColor);
    }

    @Override
    public Icon getDisabledIcon() {
        return new MaterialIcon(glyph, size, IconColor.DISABLED);
    }
}
