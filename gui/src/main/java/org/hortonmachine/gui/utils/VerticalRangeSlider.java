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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * A vertical slider with two knobs, defining a range of values between a minimum, at the bottom,
 * and a maximum, at the top.
 *
 * <p>The knobs are dragged to change the lower and upper value, the band between them is dragged
 * to move the range keeping its width. The mouse wheel moves the range by a hundredth of the
 * total, a double click selects all.</p>
 *
 * <p>The knobs can be locked together with {@link #setLocked(boolean)}: the range then keeps its
 * width, as a slice that moves dragging any knob or the band, clicking on the track or with the
 * mouse wheel, which then moves by half of the width.</p>
 */
public class VerticalRangeSlider extends JComponent {
    private static final long serialVersionUID = 1L;

    private static final int KNOB_RADIUS = 7;
    private static final int TRACK_WIDTH = 6;
    private static final int PADDING = KNOB_RADIUS + 4;

    private enum Drag {
        NONE, LOW, HIGH, BAND
    }

    private final EventListenerList listeners = new EventListenerList();

    private double minimum = 0;
    private double maximum = 1;
    private double lowValue = 0;
    private double highValue = 1;
    private boolean adjusting = false;
    private String format = "%.2f";
    /** If the knobs move together, keeping the width of the range. */
    private boolean locked = false;

    private Drag drag = Drag.NONE;
    private double dragStartValue;
    private double dragStartLow;
    private double dragStartHigh;

    public VerticalRangeSlider() {
        setToolTipText("<html>Drag the knobs to set the lowest and highest value shown,<br>"
                + "drag the band between them to move the range, or use the mouse wheel.<br>"
                + "Double click to show all.</html>");
        MouseAdapter mouse = new MouseAdapter(){
            @Override
            public void mousePressed( MouseEvent e ) {
                if (!isEnabled()) {
                    return;
                }
                int y = e.getY();
                int lowY = toY(lowValue);
                int highY = toY(highValue);
                if (locked) {
                    boolean onSlice = y >= highY - KNOB_RADIUS - 2 && y <= lowY + KNOB_RADIUS + 2;
                    if (!onSlice) {
                        // a press on the track centers the slice there
                        double width = highValue - lowValue;
                        double newLow = clamp(toValue(y) - width / 2.0, minimum, maximum - width);
                        adjusting = true;
                        lowValue = newLow;
                        highValue = newLow + width;
                        repaint();
                        fireStateChanged();
                    }
                    drag = Drag.BAND;
                } else if (Math.abs(y - highY) <= KNOB_RADIUS + 2) {
                    drag = Drag.HIGH;
                } else if (Math.abs(y - lowY) <= KNOB_RADIUS + 2) {
                    drag = Drag.LOW;
                } else if (y > highY && y < lowY) {
                    drag = Drag.BAND;
                } else {
                    // a press on the track moves the nearest knob there
                    drag = Math.abs(y - highY) < Math.abs(y - lowY) ? Drag.HIGH : Drag.LOW;
                    adjusting = true;
                    moveDragged(y);
                }
                dragStartValue = toValue(y);
                dragStartLow = lowValue;
                dragStartHigh = highValue;
                adjusting = true;
            }

            @Override
            public void mouseDragged( MouseEvent e ) {
                if (drag != Drag.NONE) {
                    moveDragged(e.getY());
                }
            }

            @Override
            public void mouseReleased( MouseEvent e ) {
                if (drag != Drag.NONE) {
                    drag = Drag.NONE;
                    adjusting = false;
                    fireStateChanged();
                }
            }

            @Override
            public void mouseClicked( MouseEvent e ) {
                if (isEnabled() && !locked && e.getClickCount() == 2) {
                    setRange(minimum, maximum);
                }
            }

            @Override
            public void mouseMoved( MouseEvent e ) {
                int y = e.getY();
                boolean onKnob = Math.abs(y - toY(lowValue)) <= KNOB_RADIUS + 2
                        || Math.abs(y - toY(highValue)) <= KNOB_RADIUS + 2;
                setCursor(Cursor.getPredefinedCursor(onKnob ? Cursor.N_RESIZE_CURSOR : Cursor.DEFAULT_CURSOR));
            }

            @Override
            public void mouseWheelMoved( MouseWheelEvent e ) {
                if (!isEnabled()) {
                    return;
                }
                double width = highValue - lowValue;
                // a locked slice moves by half of its width, to scan the values with overlapping slices
                double unit = locked ? width / 2.0 : (maximum - minimum) / 100.0;
                double step = unit * -e.getWheelRotation();
                double newLow = clamp(lowValue + step, minimum, maximum - width);
                setRange(newLow, newLow + width);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
    }

    private void moveDragged( int y ) {
        double value = toValue(y);
        switch( drag ) {
        case LOW:
            lowValue = clamp(value, minimum, highValue);
            break;
        case HIGH:
            highValue = clamp(value, lowValue, maximum);
            break;
        case BAND:
            double width = dragStartHigh - dragStartLow;
            double newLow = clamp(dragStartLow + value - dragStartValue, minimum, maximum - width);
            lowValue = newLow;
            highValue = newLow + width;
            break;
        default:
            return;
        }
        repaint();
        fireStateChanged();
    }

    private static double clamp( double value, double min, double max ) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Set the limits of the slider, selecting all, or, if locked, keeping the width of the range
     * from the bottom.
     *
     * @param minimum the value at the bottom.
     * @param maximum the value at the top.
     */
    public void setLimits( double minimum, double maximum ) {
        if (!(maximum > minimum)) {
            // a single value, or no value: a range around it, to keep the slider usable
            double center = Double.isFinite(minimum) ? minimum : 0;
            minimum = center - 0.5;
            maximum = center + 0.5;
        }
        double width = highValue - lowValue;
        this.minimum = minimum;
        this.maximum = maximum;
        this.lowValue = minimum;
        this.highValue = locked ? minimum + Math.min(width, maximum - minimum) : maximum;
        repaint();
        fireStateChanged();
    }

    /**
     * Set the selected range, clamped to the limits. If locked, the range keeps its width and
     * starts at the given low value.
     */
    public void setRange( double low, double high ) {
        double l;
        double h;
        if (locked) {
            double width = highValue - lowValue;
            l = clamp(low, minimum, maximum - width);
            h = l + width;
        } else {
            l = clamp(Math.min(low, high), minimum, maximum);
            h = clamp(Math.max(low, high), minimum, maximum);
        }
        if (l == lowValue && h == highValue) {
            return;
        }
        lowValue = l;
        highValue = h;
        repaint();
        fireStateChanged();
    }

    /**
     * Lock or unlock the knobs together. When locked, the range keeps its width.
     */
    public void setLocked( boolean locked ) {
        this.locked = locked;
        repaint();
    }

    public boolean isLocked() {
        return locked;
    }

    /**
     * Set the width of the range from its low value, moving it down if it would pass the maximum.
     *
     * @param width the width, at most the one of the limits.
     */
    public void setRangeWidth( double width ) {
        if (!(width > 0)) {
            return;
        }
        width = Math.min(width, maximum - minimum);
        double l = clamp(lowValue, minimum, maximum - width);
        double h = l + width;
        if (l == lowValue && h == highValue) {
            return;
        }
        lowValue = l;
        highValue = h;
        repaint();
        fireStateChanged();
    }

    public double getMinimum() {
        return minimum;
    }

    public double getMaximum() {
        return maximum;
    }

    public double getLowValue() {
        return lowValue;
    }

    public double getHighValue() {
        return highValue;
    }

    /**
     * @return <code>true</code> if all the values are selected.
     */
    public boolean isFullRange() {
        return lowValue <= minimum && highValue >= maximum;
    }

    /**
     * @return <code>true</code> while a knob or the band is being dragged.
     */
    public boolean getValueIsAdjusting() {
        return adjusting;
    }

    /**
     * @param format the format of the values shown next to the knobs, as for {@link String#format(String, Object...)}.
     */
    public void setValueFormat( String format ) {
        this.format = format;
        repaint();
    }

    public void addChangeListener( ChangeListener listener ) {
        listeners.add(ChangeListener.class, listener);
    }

    public void removeChangeListener( ChangeListener listener ) {
        listeners.remove(ChangeListener.class, listener);
    }

    private void fireStateChanged() {
        ChangeEvent event = new ChangeEvent(this);
        for( ChangeListener listener : listeners.getListeners(ChangeListener.class) ) {
            listener.stateChanged(event);
        }
    }

    private int trackTop() {
        return PADDING;
    }

    private int trackBottom() {
        return getHeight() - PADDING;
    }

    private int toY( double value ) {
        double fraction = (value - minimum) / (maximum - minimum);
        return (int) Math.round(trackBottom() - fraction * (trackBottom() - trackTop()));
    }

    private double toValue( int y ) {
        double fraction = (trackBottom() - y) / (double) (trackBottom() - trackTop());
        return clamp(minimum + fraction * (maximum - minimum), minimum, maximum);
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int labelWidth = Math.max(fm.stringWidth(String.format(format, maximum)), fm.stringWidth(String.format(format, minimum)));
        return new Dimension(2 * KNOB_RADIUS + 10 + labelWidth + 4, 200);
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension preferred = getPreferredSize();
        return new Dimension(preferred.width, 4 * PADDING);
    }

    @Override
    protected void paintComponent( Graphics g ) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color track = uiColor("Slider.trackColor", new Color(200, 200, 200));
            Color selected = uiColor("Slider.trackValueColor", uiColor("Component.accentColor", new Color(38, 117, 191)));
            Color thumb = uiColor("Slider.thumbColor", selected);
            Color text = uiColor("Label.foreground", Color.DARK_GRAY);
            if (!isEnabled()) {
                selected = track;
                thumb = track.darker();
                text = uiColor("Label.disabledForeground", Color.GRAY);
            }

            int x = KNOB_RADIUS + 2;
            int top = trackTop();
            int bottom = trackBottom();
            g2.setColor(track);
            g2.fillRoundRect(x - TRACK_WIDTH / 2, top, TRACK_WIDTH, bottom - top, TRACK_WIDTH, TRACK_WIDTH);

            int lowY = toY(lowValue);
            int highY = toY(highValue);
            g2.setColor(selected);
            // a locked slice is drawn wider, as a single piece
            int bandWidth = locked ? TRACK_WIDTH + 6 : TRACK_WIDTH;
            g2.fillRoundRect(x - bandWidth / 2, highY, bandWidth, lowY - highY, bandWidth, bandWidth);

            for( int y : new int[]{lowY, highY} ) {
                g2.setColor(thumb);
                g2.fillOval(x - KNOB_RADIUS, y - KNOB_RADIUS, 2 * KNOB_RADIUS, 2 * KNOB_RADIUS);
                g2.setColor(uiColor("Panel.background", Color.WHITE));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(x - KNOB_RADIUS, y - KNOB_RADIUS, 2 * KNOB_RADIUS, 2 * KNOB_RADIUS);
            }

            // the values next to the knobs, moved apart if they would overlap
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int textX = x + KNOB_RADIUS + 6;
            int ascent = fm.getAscent();
            int lowTextY = lowY + ascent / 2 - 1;
            int highTextY = highY + ascent / 2 - 1;
            if (lowTextY - highTextY < fm.getHeight()) {
                int middle = (lowTextY + highTextY) / 2;
                highTextY = middle - fm.getHeight() / 2;
                lowTextY = highTextY + fm.getHeight();
            }
            g2.setColor(text);
            g2.drawString(String.format(format, highValue), textX, highTextY);
            g2.drawString(String.format(format, lowValue), textX, lowTextY);
        } finally {
            g2.dispose();
        }
    }

    private static Color uiColor( String key, Color fallback ) {
        Color color = UIManager.getColor(key);
        return color != null ? color : fallback;
    }
}
