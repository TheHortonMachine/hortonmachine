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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.function.DoubleFunction;

import javax.swing.JComponent;
import javax.swing.UIManager;

import org.hortonmachine.gears.utils.colors.ColorInterpolator;

/**
 * A legend for a color ramp, e.g. of a raster map: a title, a horizontal color bar and tick labels.
 *
 * <p>The legend is hidden until a ramp is set.</p>
 */
public class ColorRampLegend extends JComponent {
    private static final long serialVersionUID = 1L;
    private static final int TICKS = 5;
    private static final int BAR_HEIGHT = 14;
    private static final int MARGIN = 10;
    /** The inset of the legend in the background of {@link #toImage(int, Color)}. */
    private static final int IMAGE_PADDING = 12;

    private String title;
    private DoubleFunction<Color> colors;
    private double min;
    private double max;
    private boolean logarithmic;
    private boolean openEnded;

    public ColorRampLegend() {
        setPreferredSize(new Dimension(300, 56));
        // without a UI delegate the minimum size would be the current size, 0 before the first
        // layout, and layouts short of space (e.g. GridBagLayout) would collapse the legend
        setMinimumSize(new Dimension(150, 56));
        setVisible(false);
    }

    /**
     * Set the ramp of a color interpolator, between its minimum and maximum.
     *
     * @param title the title, e.g. the quantity and its unit.
     * @param interpolator the colors.
     * @param openEnded if <code>true</code>, values beyond the maximum exist and get its color, the
     *            last label is marked with a "+".
     */
    public void setRamp( String title, ColorInterpolator interpolator, boolean openEnded ) {
        setRamp(title, interpolator::getColorFor, interpolator.getMin(), interpolator.getMax(), false, openEnded);
    }

    /**
     * Set the ramp to show and make the legend visible.
     *
     * @param title the title, e.g. the quantity and its unit.
     * @param colors the color of a value.
     * @param min the value at the left end.
     * @param max the value at the right end.
     * @param logarithmic if <code>true</code>, the bar and the ticks are on a logarithmic scale (min
     *            has to be positive), e.g. for counts spanning orders of magnitude.
     * @param openEnded if <code>true</code>, values beyond the maximum exist and get its color, the
     *            last label is marked with a "+".
     */
    public void setRamp( String title, DoubleFunction<Color> colors, double min, double max, boolean logarithmic,
            boolean openEnded ) {
        if (logarithmic && !(min > 0)) {
            throw new IllegalArgumentException("A logarithmic legend needs a positive minimum.");
        }
        this.title = title;
        this.colors = colors;
        this.min = min;
        this.max = max;
        this.logarithmic = logarithmic;
        this.openEnded = openEnded;
        if (!isVisible()) {
            // a component becoming visible needs a new layout of its container to get its space
            setVisible(true);
            revalidate();
        }
        repaint();
    }

    /**
     * @param fraction the position along the bar, from 0 to 1.
     * @return the value at that position.
     */
    private double valueAt( double fraction ) {
        if (logarithmic) {
            double logMin = Math.log10(min);
            double logMax = Math.log10(max);
            return Math.pow(10, logMin + (logMax - logMin) * fraction);
        }
        return min + (max - min) * fraction;
    }

    /**
     * Render the legend in an image, e.g. to overlay it on a map, on a rounded semi-transparent
     * background. A ramp has to be set.
     *
     * @param width the width of the image.
     * @param background the background color, with its alpha.
     * @return the image.
     */
    public BufferedImage toImage( int width, Color background ) {
        int padding = IMAGE_PADDING;
        Dimension size = getPreferredSize();
        BufferedImage image = new BufferedImage(width, size.height + 2 * padding, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(background);
            g.fillRoundRect(0, 0, width, image.getHeight(), 16, 16);
            // the legend inside the background, with an inset on all sides
            g.translate(padding, padding);
            Font font = getFont() != null ? getFont() : UIManager.getFont("Label.font");
            if (font != null) {
                g.setFont(font);
            }
            paintLegend(g, width - 2 * padding, Color.BLACK);
        } finally {
            g.dispose();
        }
        return image;
    }

    @Override
    protected void paintComponent( Graphics g ) {
        paintLegend(g, getWidth(), getForeground() != null ? getForeground() : Color.BLACK);
    }

    private void paintLegend( Graphics g, int componentWidth, Color textColor ) {
        if (colors == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            FontMetrics metrics = g2.getFontMetrics();
            int width = componentWidth - 2 * MARGIN;
            int titleY = metrics.getAscent();
            int barY = titleY + 4;
            int labelY = barY + BAR_HEIGHT + 4 + metrics.getAscent();

            g2.setColor(textColor);
            g2.drawString(title, MARGIN, titleY);

            for( int x = 0; x < width; x++ ) {
                double value = valueAt(x / (double) Math.max(1, width - 1));
                g2.setColor(opaque(colors.apply(value)));
                g2.drawLine(MARGIN + x, barY, MARGIN + x, barY + BAR_HEIGHT);
            }
            g2.setColor(textColor);
            g2.drawRect(MARGIN, barY, width - 1, BAR_HEIGHT);

            for( int i = 0; i < TICKS; i++ ) {
                double fraction = i / (double) (TICKS - 1);
                double value = valueAt(fraction);
                int x = MARGIN + (int) Math.round((width - 1) * fraction);
                g2.drawLine(x, barY + BAR_HEIGHT, x, barY + BAR_HEIGHT + 3);
                String label = format(value) + (openEnded && i == TICKS - 1 ? "+" : "");
                int labelWidth = metrics.stringWidth(label);
                int labelX = Math.max(0, Math.min(componentWidth - labelWidth, x - labelWidth / 2));
                g2.drawString(label, labelX, labelY);
            }
        } finally {
            g2.dispose();
        }
    }

    private static Color opaque( Color color ) {
        return color == null ? Color.WHITE : new Color(color.getRed(), color.getGreen(), color.getBlue());
    }

    private static String format( double value ) {
        if (Math.abs(value) >= 10) {
            return String.valueOf(Math.round(value));
        }
        return String.format("%.1f", value);
    }
}
