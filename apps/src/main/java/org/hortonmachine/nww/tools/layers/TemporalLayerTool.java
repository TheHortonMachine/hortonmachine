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
package org.hortonmachine.nww.tools.layers;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.time.ZoneOffset;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import org.hortonmachine.gears.utils.colors.RasterStyleUtilities;
import org.hortonmachine.gears.utils.time.DatedFilesScanner.DatedFile;
import org.hortonmachine.gui.utils.ImageCache;
import org.hortonmachine.nww.layers.defaults.raster.TemporalRasterNwwLayer;
import org.hortonmachine.nww.tools.INwwLayerTool;
import org.hortonmachine.nww.tools.NwwContext;
import org.hortonmachine.nww.tools.features.RasterValues;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;

/**
 * The panel of a {@link TemporalRasterNwwLayer}: the date shown, chosen with a slider or played
 * as an animation, and the chart of the values of a cell through all the dates, of the point
 * clicked on the map in the select mode.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TemporalLayerTool implements INwwLayerTool {

    private final Map<Layer, TimePanel> panels = new IdentityHashMap<>();

    @Override
    public String getName() {
        return "Time";
    }

    @Override
    public boolean canHandle( Layer layer ) {
        return layer instanceof TemporalRasterNwwLayer;
    }

    @Override
    public JComponent createPanel( NwwContext context, Layer layer ) {
        TimePanel panel = new TimePanel(context, (TemporalRasterNwwLayer) layer);
        panels.put(layer, panel);
        return panel;
    }

    @Override
    public void onClose( Layer layer ) {
        TimePanel panel = panels.remove(layer);
        if (panel != null) {
            panel.close();
        }
    }

    /**
     * The panel of a series.
     */
    private static class TimePanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private final NwwContext context;
        private final TemporalRasterNwwLayer layer;
        private final int count;

        private final JLabel dateLabel = new JLabel(" ");
        private final JSlider slider;
        private final JButton previousButton = new JButton();
        private final JButton playButton = new JButton();
        private final JButton nextButton = new JButton();
        private final JSpinner speedSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 30.0, 0.1));
        private final JCheckBox loopCheck = new JCheckBox("Loop", true);
        private final Timer playTimer;

        private final JLabel pointLabel = new JLabel();
        private final JButton copyButton = new JButton("Copy values");
        private final TimeSeries series = new TimeSeries("values");
        private final ChartPanel chartPanel;
        private final ValueMarker dateMarker = new ValueMarker(0);

        /** The mark of the point of the chart on the map, not listed in the layers panel. */
        private final RenderableLayer pointLayer = new RenderableLayer();
        private final RasterValues.Reader reader = new RasterValues.Reader();
        /** The values of the point at the dates, <code>null</code> if none read. */
        private double[] values;
        private String pointText;
        /** The count of the reads of points, of which only the last is shown. */
        private volatile int pointRequest;

        private final IntConsumer dateListener = this::onDateShown;
        private final Consumer<Position> pointListener = this::readPoint;

        TimePanel( NwwContext context, TemporalRasterNwwLayer layer ) {
            super(new BorderLayout(0, 8));
            this.context = context;
            this.layer = layer;
            this.count = layer.getFiles().size();
            setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

            JLabel info = new JLabel("<html>" + count + " dates in " + layer.getFolder().getAbsolutePath() + ", pattern "
                    + layer.getPattern() + "</html>");
            info.setForeground(UIManager.getColor("Label.disabledForeground"));
            dateLabel.setFont(dateLabel.getFont().deriveFont(Font.BOLD, dateLabel.getFont().getSize2D() * 1.4f));

            slider = new JSlider(0, count - 1, layer.getIndex());
            slider.addChangeListener(e -> onSliderMoved());
            previousButton.setIcon(ImageCache.getInstance().getImage(ImageCache.ZOOM_TO_PREVIOUS));
            previousButton.setToolTipText("The previous date");
            previousButton.addActionListener(e -> step(-1));
            nextButton.setIcon(ImageCache.getInstance().getImage(ImageCache.ZOOM_TO_NEXT));
            nextButton.setToolTipText("The next date");
            nextButton.addActionListener(e -> step(1));
            playTimer = new Timer(delay(), e -> onPlayTick());
            playButton.addActionListener(e -> togglePlay());
            speedSpinner.setToolTipText("The seconds each date is shown while playing");
            speedSpinner.addChangeListener(e -> playTimer.setDelay(delay()));
            loopCheck.setToolTipText("Start again from the first date after the last one");
            updatePlayButton();

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            buttons.add(previousButton);
            buttons.add(playButton);
            buttons.add(nextButton);
            buttons.add(new JLabel("s/date"));
            buttons.add(speedSpinner);
            buttons.add(loopCheck);

            JPanel north = new JPanel();
            north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
            for( JComponent component : new JComponent[]{info, dateLabel, slider, buttons} ) {
                component.setAlignmentX(LEFT_ALIGNMENT);
                north.add(component);
            }
            add(north, BorderLayout.NORTH);

            chartPanel = createChart();
            pointLabel.setText("<html>Click the map in the select mode to chart the values of a cell through the dates.</html>");
            copyButton.setIcon(ImageCache.getInstance().getImage(ImageCache.COPY));
            copyButton.setToolTipText("Copy the dates and values of the point to the clipboard, tab separated");
            copyButton.setEnabled(false);
            copyButton.addActionListener(e -> copyValues());
            JPanel chartButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            chartButtons.add(copyButton);
            JPanel chartBox = new JPanel(new BorderLayout(0, 4));
            chartBox.setBorder(BorderFactory.createTitledBorder("Values through time"));
            chartBox.add(pointLabel, BorderLayout.NORTH);
            chartBox.add(chartPanel, BorderLayout.CENTER);
            chartBox.add(chartButtons, BorderLayout.SOUTH);
            add(chartBox, BorderLayout.CENTER);

            pointLayer.setName("hide temporal point");
            pointLayer.setPickEnabled(false);

            layer.addDateListener(dateListener);
            layer.addPointListener(pointListener);
            onDateShown(layer.getIndex());
        }

        private ChartPanel createChart() {
            JFreeChart chart = ChartFactory.createTimeSeriesChart(null, null, null, new TimeSeriesCollection(series), false,
                    true, false);
            Color background = UIManager.getColor("Panel.background");
            Color foreground = UIManager.getColor("Label.foreground");
            Color grid = UIManager.getColor("Separator.foreground");
            Color accent = UIManager.getColor("Component.accentColor");
            if (accent == null) {
                accent = new Color(0x2675bf);
            }
            chart.setBackgroundPaint(background);
            XYPlot plot = chart.getXYPlot();
            plot.setBackgroundPaint(background);
            plot.setDomainGridlinePaint(grid);
            plot.setRangeGridlinePaint(grid);
            plot.setOutlineVisible(false);
            for( var axis : new org.jfree.chart.axis.ValueAxis[]{plot.getDomainAxis(), plot.getRangeAxis()} ) {
                axis.setTickLabelPaint(foreground);
                axis.setLabelPaint(foreground);
                axis.setAxisLinePaint(grid);
                axis.setTickMarkPaint(grid);
            }
            ((org.jfree.chart.axis.NumberAxis) plot.getRangeAxis()).setAutoRangeIncludesZero(false);
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
            renderer.setSeriesPaint(0, accent);
            renderer.setSeriesStroke(0, new BasicStroke(1.5f));
            renderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-2.5, -2.5, 5, 5));
            plot.setRenderer(renderer);
            dateMarker.setPaint(UIManager.getColor("Component.error.focusedBorderColor") != null
                    ? UIManager.getColor("Component.error.focusedBorderColor")
                    : Color.RED);
            dateMarker.setStroke(new BasicStroke(1.2f));
            plot.addDomainMarker(dateMarker);

            ChartPanel panel = new ChartPanel(chart);
            panel.setPreferredSize(new Dimension(300, 220));
            panel.setMinimumDrawWidth(100);
            panel.setMinimumDrawHeight(80);
            panel.setMaximumDrawWidth(4000);
            panel.setMaximumDrawHeight(3000);
            panel.setMouseZoomable(false);
            panel.setToolTipText("Click a date to show it on the map");
            panel.addChartMouseListener(new ChartMouseListener(){
                @Override
                public void chartMouseClicked( ChartMouseEvent event ) {
                    showDateAt(event.getTrigger().getX());
                }

                @Override
                public void chartMouseMoved( ChartMouseEvent event ) {
                }
            });
            return panel;
        }

        private static long millis( DatedFile datedFile ) {
            return datedFile.dateTime().toInstant(ZoneOffset.UTC).toEpochMilli();
        }

        /**
         * Show the date nearest to a point of the chart.
         */
        private void showDateAt( int screenX ) {
            Rectangle2D dataArea = chartPanel.getScreenDataArea();
            if (dataArea == null || dataArea.getWidth() <= 0) {
                return;
            }
            XYPlot plot = chartPanel.getChart().getXYPlot();
            double time = plot.getDomainAxis().java2DToValue(screenX, dataArea, plot.getDomainAxisEdge());
            int nearest = 0;
            double best = Double.MAX_VALUE;
            for( int i = 0; i < count; i++ ) {
                double distance = Math.abs(millis(layer.getFiles().get(i)) - time);
                if (distance < best) {
                    best = distance;
                    nearest = i;
                }
            }
            slider.setValue(nearest);
        }

        private int delay() {
            return (int) Math.round(((Number) speedSpinner.getValue()).doubleValue() * 1000);
        }

        private void onSliderMoved() {
            int index = slider.getValue();
            if (index != layer.getIndex()) {
                dateLabel.setText(layer.getDateLabel(index) + "  (" + (index + 1) + "/" + count + ", loading...)");
            }
            layer.setIndex(index);
        }

        private void step( int delta ) {
            int index = slider.getValue() + delta;
            if (index >= count) {
                index = loopCheck.isSelected() ? 0 : count - 1;
            } else if (index < 0) {
                index = loopCheck.isSelected() ? count - 1 : 0;
            }
            slider.setValue(index);
        }

        private void togglePlay() {
            if (playTimer.isRunning()) {
                playTimer.stop();
            } else {
                if (slider.getValue() == count - 1 && !loopCheck.isSelected()) {
                    slider.setValue(0);
                }
                playTimer.start();
            }
            updatePlayButton();
        }

        private void updatePlayButton() {
            boolean playing = playTimer.isRunning();
            playButton.setIcon(ImageCache.getInstance().getImage(playing ? ImageCache.STOP : ImageCache.RUN));
            playButton.setToolTipText(playing ? "Stop" : "Play the dates");
        }

        /**
         * The next date, once the current one is shown: the dates whose layer takes long to make
         * are not skipped.
         */
        private void onPlayTick() {
            if (layer.getIndex() != slider.getValue()) {
                return;
            }
            if (slider.getValue() == count - 1 && !loopCheck.isSelected()) {
                playTimer.stop();
                updatePlayButton();
                return;
            }
            step(1);
        }

        private void onDateShown( int index ) {
            if (index == layer.getRequestedIndex() && slider.getValue() != index) {
                // the date was changed elsewhere, as by a script: the slider follows
                slider.setValue(index);
            }
            String text = layer.getDateLabel(index) + "  (" + (index + 1) + "/" + count + ")";
            if (slider.getValue() != index) {
                text = layer.getDateLabel(slider.getValue()) + "  (" + (slider.getValue() + 1) + "/" + count + ", loading...)";
            }
            dateLabel.setText(text);
            dateMarker.setValue(millis(layer.getFiles().get(index)));
            updatePointLabel();
        }

        /**
         * Read the values of a point at all the dates, in the background, and chart them.
         */
        private void readPoint( Position position ) {
            int request = ++pointRequest;
            double lon = position.getLongitude().degrees;
            double lat = position.getLatitude().degrees;
            showPointMark(position);
            pointText = String.format(Locale.ROOT, "%.5f, %.5f", lon, lat);
            pointLabel.setText("<html><b>" + pointText + "</b>: reading...</html>");
            copyButton.setEnabled(false);
            int band = Math.max(0, RasterStyleUtilities.getGrayChannel(layer.getStyle()));
            new Thread(() -> {
                double[] read = new double[count];
                boolean inside = false;
                try {
                    for( int i = 0; i < count; i++ ) {
                        if (request != pointRequest) {
                            return;
                        }
                        File file = layer.getFiles().get(i).file();
                        RasterValues cell = reader.read(file, lon, lat);
                        double value = Double.NaN;
                        if (cell.isInside() && band < cell.values().length && !cell.isNovalue(cell.values()[band])) {
                            value = cell.values()[band];
                        }
                        inside |= cell.isInside();
                        read[i] = value;
                        if (i % 10 == 9) {
                            int done = i + 1;
                            SwingUtilities.invokeLater(() -> {
                                if (request == pointRequest) {
                                    pointLabel.setText("<html><b>" + pointText + "</b>: reading " + done + "/" + count + "...</html>");
                                }
                            });
                        }
                    }
                    reader.close();
                    boolean wasInside = inside;
                    SwingUtilities.invokeLater(() -> {
                        if (request == pointRequest) {
                            showValues(wasInside ? read : null);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    reader.close();
                    SwingUtilities.invokeLater(() -> {
                        if (request == pointRequest) {
                            values = null;
                            series.clear();
                            pointLabel.setText("<html><b>" + pointText + "</b>: unable to read the values: " + e.getMessage()
                                    + "</html>");
                        }
                    });
                }
            }, "hm-temporal-point").start();
        }

        private void showValues( double[] read ) {
            values = read;
            series.clear();
            if (read == null) {
                pointLabel.setText("<html><b>" + pointText + "</b> is outside of the rasters.</html>");
                return;
            }
            for( int i = 0; i < count; i++ ) {
                if (!Double.isNaN(read[i])) {
                    series.addOrUpdate(new FixedMillisecond(millis(layer.getFiles().get(i))), read[i]);
                }
            }
            copyButton.setEnabled(true);
            updatePointLabel();
        }

        private void updatePointLabel() {
            if (values == null || pointText == null) {
                return;
            }
            int index = layer.getIndex();
            double value = values[index];
            String valueText = Double.isNaN(value) ? "novalue" : RasterLayerTool.numberText(value);
            pointLabel.setText("<html><b>" + pointText + "</b>: " + valueText + " at " + layer.getDateLabel(index) + "</html>");
        }

        private void copyValues() {
            if (values == null) {
                return;
            }
            StringBuilder sb = new StringBuilder("date\tvalue\n");
            for( int i = 0; i < count; i++ ) {
                sb.append(layer.getDateLabel(i)).append('\t');
                sb.append(Double.isNaN(values[i]) ? "" : RasterLayerTool.numberText(values[i])).append('\n');
            }
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
        }

        private void showPointMark( Position position ) {
            PointPlacemarkAttributes attributes = new PointPlacemarkAttributes();
            attributes.setUsePointAsDefaultImage(true);
            attributes.setLineMaterial(new Material(Color.MAGENTA));
            attributes.setScale(12.0);
            PointPlacemark placemark = new PointPlacemark(
                    Position.fromDegrees(position.getLatitude().degrees, position.getLongitude().degrees, 0));
            placemark.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
            placemark.setAttributes(attributes);
            pointLayer.removeAllRenderables();
            pointLayer.addRenderable(placemark);
            LayerList layers = context.getNwwPanel().getWwd().getModel().getLayers();
            layers.remove(pointLayer);
            layers.add(pointLayer);
            context.getNwwPanel().getWwd().redraw();
        }

        /**
         * Stop playing and drop the listeners and the mark of the point.
         */
        void close() {
            playTimer.stop();
            pointRequest++;
            layer.removeDateListener(dateListener);
            layer.removePointListener(pointListener);
            context.getNwwPanel().getWwd().getModel().getLayers().remove(pointLayer);
            reader.close();
        }
    }
}
