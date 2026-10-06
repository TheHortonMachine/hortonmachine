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
package org.hortonmachine.docs;

import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import javax.imageio.ImageIO;

import org.eclipse.imagen.iterator.RandomIter;
import org.eclipse.imagen.iterator.WritableRandomIter;
import org.geotools.api.style.ColorMapEntry;
import org.geotools.api.style.RasterSymbolizer;
import org.geotools.api.style.Style;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.map.FeatureLayer;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.SLD;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.modules.Hillshade;

import groovy.lang.GroovyShell;

/**
 * Generates the map images of the module pages of the manual.
 *
 * <p>
 * The maps are described in a spec file (docs/manual/modules/maps/maps.txt), one per line:
 *
 * <pre>
 * Module | example script | raster to draw | colortable | options | class names
 * </pre>
 *
 * The sample data of the data folder next to the spec file are copied into a work folder, the
 * example scripts are run in the order of the file, each on the outputs of the previous ones,
 * and each raster is drawn over the hillshade of the sample elevation, with a legend, into
 * <code>Module_output.png</code> of the images folder, named after the module and the drawn
 * raster, since a module can have several outputs, each in its own line.
 *
 * <p>
 * The scripts are the ones shown in the manual: their line <code>var folder = "..."</code> is
 * replaced by the work folder before running them. The script can be <code>-</code> when the
 * raster was already produced by a previous one.
 *
 * <p>
 * The raster can be the difference of two rasters, written as <code>a.tif - b.tif</code>, or a
 * vector of lines (<code>.shp</code>), drawn in the strongest color of the colortable, without
 * legend.
 * The colortable is one of {@link EColorTables}: <b>rainbow</b> is used if it is not.
 * The options, separated by commas, are:
 * <ul>
 * <li><b>positive</b>: draw only the cells with values above 0;</li>
 * <li><b>classes</b>: the legend shows the classes of the colortable, instead of a color bar;</li>
 * <li><b>nolegend</b>: no legend, for example for a single class;</li>
 * <li><b>clip</b>: the colors span from the 2nd to the 98th percentile of the values, for rasters
 * with a few extreme values; the cells outside get the colors of the ends.</li>
 * </ul>
 * The optional class names, as <code>1=stable;2=unstable</code>, replace the values in the legend
 * of the classes.
 *
 * <p>
 * Usage: <code>ModuleMapsGenerator &lt;maps spec file&gt; &lt;images folder&gt; &lt;work folder&gt;</code>
 *
 * @author Andrea Antonello (www.g-ant.eu)
 */
public class ModuleMapsGenerator {

    private static final int MAP_WIDTH = 800;
    private static final int LEGEND_WIDTH = 160;
    private static final double OPACITY = 0.75;
    private static final Font FONT = new Font("SansSerif", Font.PLAIN, 14);
    /** The rings of border cells that are 0 in the hillshade. */
    private static final int BORDER = 2;

    public static final String HILLSHADE = "hillshade.tif";
    public static final String ELEVATION = "dtm_flanginec.tif";

    public record MapSpec( String module, String script, String raster, String colortable, Set<String> options,
            Map<Double, String> classNames ) {
    }

    private final File specFile;
    private final File imagesFolder;
    private final File workFolder;

    public ModuleMapsGenerator( File specFile, File imagesFolder, File workFolder ) {
        this.specFile = specFile;
        this.imagesFolder = imagesFolder;
        this.workFolder = workFolder;
    }

    public static List<MapSpec> readSpecs( File specFile ) throws Exception {
        List<MapSpec> specs = new ArrayList<>();
        for( String line : Files.readAllLines(specFile.toPath(), StandardCharsets.UTF_8) ) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] split = line.split("\\|");
            if (split.length < 4) {
                throw new IllegalArgumentException("Wrong map spec, too few fields: " + line);
            }
            Set<String> options = new HashSet<>();
            if (split.length > 4) {
                for( String option : split[4].split(",") ) {
                    if (!option.isBlank()) {
                        options.add(option.trim());
                    }
                }
            }
            Map<Double, String> classNames = new HashMap<>();
            if (split.length > 5) {
                for( String pair : split[5].split(";") ) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        classNames.put(Double.parseDouble(keyValue[0].trim()), keyValue[1].trim());
                    }
                }
            }
            specs.add(new MapSpec(split[0].trim(), split[1].trim(), split[2].trim(), split[3].trim(), options, classNames));
        }
        return specs;
    }

    public void generate() throws Exception {
        prepareWorkFolder();
        imagesFolder.mkdirs();

        Hillshade hillshade = new Hillshade();
        hillshade.inElev = path(ELEVATION);
        hillshade.pAzimuth = 315;
        hillshade.pElev = 45;
        hillshade.outHill = path(HILLSHADE);
        hillshade.process();

        for( MapSpec spec : readSpecs(specFile) ) {
            if (!spec.script().equals("-")) {
                runScript(new File(specFile.getParentFile(), spec.script()));
            }
            File imageFile = new File(imagesFolder, imageName(spec));
            ImageIO.write(drawMap(spec), "png", imageFile);
            System.out.println("Created " + imageFile);
        }
    }

    /**
     * @return the name of the image of the map: the module and the name of the drawn raster.
     */
    public static String imageName( MapSpec spec ) {
        String raster = spec.raster().split(" - ")[0].trim();
        return spec.module() + "_" + raster.replaceFirst("\\.[^.]+$", "") + ".png";
    }

    private void prepareWorkFolder() throws Exception {
        if (workFolder.exists()) {
            for( File file : workFolder.listFiles() ) {
                file.delete();
            }
        }
        workFolder.mkdirs();
        File dataFolder = new File(specFile.getParentFile(), "data");
        for( File file : dataFolder.listFiles() ) {
            Files.copy(file.toPath(), new File(workFolder, file.getName()).toPath());
        }
    }

    private String path( String name ) {
        return new File(workFolder, name).getAbsolutePath();
    }

    private void runScript( File scriptFile ) throws Exception {
        String script = Files.readString(scriptFile.toPath(), StandardCharsets.UTF_8);
        String folderLine = "var folder = \"" + workFolder.getAbsolutePath() + File.separator + "\"";
        String toRun = script.replaceFirst("(?m)^var folder = .*$", Matcher.quoteReplacement(folderLine));
        if (toRun.equals(script)) {
            throw new IllegalArgumentException("The script has no line var folder = \"...\": " + scriptFile);
        }
        System.out.println("Running " + scriptFile.getName());
        new GroovyShell(ModuleMapsGenerator.class.getClassLoader()).evaluate(toRun, scriptFile.getName());
    }

    private BufferedImage drawMap( MapSpec spec ) throws Exception {
        if (spec.raster().endsWith(".shp")) {
            return drawVectorMap(spec);
        }
        GridCoverage2D raster = readRaster(spec);
        double[] minMax = spec.options().contains("clip") ? percentiles(raster, 0.02, 0.98) : minMax(raster);
        if (minMax[0] == minMax[1]) {
            // a single value, as a mask, gets the strongest color of the colortable
            minMax[0] = minMax[1] - 1;
        }

        String colortable = Arrays.stream(EColorTables.values()).anyMatch(t -> t.name().equals(spec.colortable()))
                ? spec.colortable()
                : EColorTables.rainbow.name();
        // the few cells drawn with positive are shown opaque, to be visible
        double opacity = spec.options().contains("positive") ? 1.0 : OPACITY;
        Style style = RasterStyleUtilities.createStyleForColortable(colortable, minMax[0], minMax[1], opacity);

        GridCoverage2D hillshade = readHillshade();
        double[] hillMinMax = minMax(hillshade);
        Style hillStyle = RasterStyleUtilities.createStyleForColortable(EColorTables.greyscale.name(), hillMinMax[0],
                hillMinMax[1], 1.0);

        ReferencedEnvelope envelope = new ReferencedEnvelope(raster.getEnvelope2D());
        int mapHeight = (int) Math.round(MAP_WIDTH * envelope.getHeight() / envelope.getWidth());

        BufferedImage image = new BufferedImage(MAP_WIDTH + legendWidth(spec), mapHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());

        MapContent content = new MapContent();
        try {
            content.addLayer(new GridCoverageLayer(hillshade, hillStyle));
            content.addLayer(new GridCoverageLayer(raster, style));
            StreamingRenderer renderer = new StreamingRenderer();
            renderer.setMapContent(content);
            renderer.paint(g, new Rectangle(0, 0, MAP_WIDTH, mapHeight), envelope);
        } finally {
            content.dispose();
        }
        // the renderer leaves its clip on the graphics
        g.setClip(null);

        List<ColorMapEntry> entries = legendEntries(style, minMax);
        if (spec.options().contains("classes")) {
            drawClassesLegend(g, entries, spec.classNames(), MAP_WIDTH + 20, 20);
        } else if (!spec.options().contains("nolegend")) {
            drawBarLegend(g, entries, MAP_WIDTH + 20, 20, Math.min(mapHeight - 40, 400));
        }
        g.dispose();
        return image;
    }

    /**
     * Draws the lines of a vector over the hillshade, on the extent of the elevation.
     */
    private BufferedImage drawVectorMap( MapSpec spec ) throws Exception {
        GridCoverage2D hillshade = readHillshade();
        double[] hillMinMax = minMax(hillshade);
        Style hillStyle = RasterStyleUtilities.createStyleForColortable(EColorTables.greyscale.name(), hillMinMax[0],
                hillMinMax[1], 1.0);

        String colortable = Arrays.stream(EColorTables.values()).anyMatch(t -> t.name().equals(spec.colortable()))
                ? spec.colortable()
                : EColorTables.rainbow.name();
        Style colorsStyle = RasterStyleUtilities.createStyleForColortable(colortable, 0, 1, 1.0);
        List<ColorMapEntry> entries = legendEntries(colorsStyle, new double[]{0, 1});
        Color lineColor = color(entries.get(entries.size() - 1));
        Style lineStyle = SLD.createLineStyle(lineColor, 1.5f);

        ReferencedEnvelope envelope = new ReferencedEnvelope(hillshade.getEnvelope2D());
        int mapHeight = (int) Math.round(MAP_WIDTH * envelope.getHeight() / envelope.getWidth());
        BufferedImage image = new BufferedImage(MAP_WIDTH + LEGEND_WIDTH, mapHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());

        SimpleFeatureCollection features = OmsVectorReader.readVector(path(spec.raster()));
        MapContent content = new MapContent();
        try {
            content.addLayer(new GridCoverageLayer(hillshade, hillStyle));
            content.addLayer(new FeatureLayer(features, lineStyle));
            StreamingRenderer renderer = new StreamingRenderer();
            renderer.setMapContent(content);
            renderer.paint(g, new Rectangle(0, 0, MAP_WIDTH, mapHeight), envelope);
        } finally {
            content.dispose();
        }
        g.dispose();
        return image;
    }

    private GridCoverage2D readRaster( MapSpec spec ) throws Exception {
        String[] names = spec.raster().split(" - ");
        GridCoverage2D raster = OmsRasterReader.readRaster(path(names[0].trim()));
        GridCoverage2D subtract = names.length > 1 ? OmsRasterReader.readRaster(path(names[1].trim())) : null;
        boolean onlyPositive = spec.options().contains("positive");
        if (subtract == null && !onlyPositive) {
            return raster;
        }

        RegionMap region = CoverageUtilities.getRegionParamsFromGridCoverage(raster);
        WritableRaster outWR = CoverageUtilities.createWritableRaster(region.getCols(), region.getRows(), null, null,
                doubleNovalue);
        RandomIter iter = CoverageUtilities.getRandomIterator(raster);
        RandomIter subtractIter = subtract != null ? CoverageUtilities.getRandomIterator(subtract) : null;
        WritableRandomIter outIter = CoverageUtilities.getWritableRandomIterator(outWR);
        for( int r = 0; r < region.getRows(); r++ ) {
            for( int c = 0; c < region.getCols(); c++ ) {
                double value = iter.getSampleDouble(c, r, 0);
                if (subtractIter != null) {
                    double other = subtractIter.getSampleDouble(c, r, 0);
                    value = isNovalue(value) || isNovalue(other) ? doubleNovalue : value - other;
                }
                if (onlyPositive && !(value > 0)) {
                    value = doubleNovalue;
                }
                outIter.setSample(c, r, 0, value);
            }
        }
        return CoverageUtilities.buildCoverage("map", outWR, region, raster.getCoordinateReferenceSystem());
    }

    /**
     * @return the hillshade, without the two rings of cells at the border of the elevation, which are 0
     *         and would draw a black frame.
     */
    private GridCoverage2D readHillshade() throws Exception {
        GridCoverage2D hillshade = OmsRasterReader.readRaster(path(HILLSHADE));
        GridCoverage2D elevation = OmsRasterReader.readRaster(path(ELEVATION));
        RegionMap region = CoverageUtilities.getRegionParamsFromGridCoverage(hillshade);
        int cols = region.getCols();
        int rows = region.getRows();
        WritableRaster outWR = CoverageUtilities.renderedImage2WritableRaster(hillshade.getRenderedImage(), false);
        WritableRandomIter outIter = CoverageUtilities.getWritableRandomIterator(outWR);
        RandomIter elevIter = CoverageUtilities.getRandomIterator(elevation);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                boolean border = false;
                for( int dr = -BORDER; dr <= BORDER && !border; dr++ ) {
                    for( int dc = -BORDER; dc <= BORDER && !border; dc++ ) {
                        int nr = r + dr;
                        int nc = c + dc;
                        border = nr < 0 || nr >= rows || nc < 0 || nc >= cols || isNovalue(elevIter.getSampleDouble(nc, nr, 0));
                    }
                }
                if (border) {
                    outIter.setSample(c, r, 0, doubleNovalue);
                }
            }
        }
        return CoverageUtilities.buildCoverage("hillshade", outWR, region, hillshade.getCoordinateReferenceSystem());
    }

    private static double[] percentiles( GridCoverage2D raster, double low, double high ) {
        RegionMap region = CoverageUtilities.getRegionParamsFromGridCoverage(raster);
        RandomIter iter = CoverageUtilities.getRandomIterator(raster);
        double[] values = new double[region.getRows() * region.getCols()];
        int count = 0;
        for( int r = 0; r < region.getRows(); r++ ) {
            for( int c = 0; c < region.getCols(); c++ ) {
                double value = iter.getSampleDouble(c, r, 0);
                if (!isNovalue(value) && !Double.isNaN(value)) {
                    values[count++] = value;
                }
            }
        }
        Arrays.sort(values, 0, count);
        return new double[]{values[(int) (low * (count - 1))], values[(int) (high * (count - 1))]};
    }

    private static double[] minMax( GridCoverage2D raster ) {
        RegionMap region = CoverageUtilities.getRegionParamsFromGridCoverage(raster);
        RandomIter iter = CoverageUtilities.getRandomIterator(raster);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for( int r = 0; r < region.getRows(); r++ ) {
            for( int c = 0; c < region.getCols(); c++ ) {
                double value = iter.getSampleDouble(c, r, 0);
                if (!isNovalue(value) && !Double.isNaN(value)) {
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
            }
        }
        return new double[]{min, max};
    }

    /**
     * @return the entries of the colormap of the style within the range of the raster, with
     *         the closest one outside on each side, the no-data entry excluded.
     */
    private static List<ColorMapEntry> legendEntries( Style style, double[] minMax ) {
        RasterSymbolizer symbolizer = (RasterSymbolizer) style.featureTypeStyles().get(0).rules().get(0).symbolizers()
                .get(0);
        List<ColorMapEntry> all = new ArrayList<>();
        for( ColorMapEntry entry : symbolizer.getColorMap().getColorMapEntries() ) {
            if (quantity(entry) != doubleNovalue) {
                all.add(entry);
            }
        }
        int first = 0;
        int last = all.size() - 1;
        while( first < last - 1 && quantity(all.get(first + 1)) <= minMax[0] ) {
            first++;
        }
        while( last > first + 1 && quantity(all.get(last - 1)) >= minMax[1] ) {
            last--;
        }
        return new ArrayList<>(all.subList(first, last + 1));
    }

    private static double quantity( ColorMapEntry entry ) {
        return entry.getQuantity().evaluate(null, Double.class);
    }

    private static Color color( ColorMapEntry entry ) {
        return entry.getColor().evaluate(null, Color.class);
    }

    /**
     * A vertical color bar, with the entries evenly spaced, so that also the colortables with
     * logarithmic steps are readable.
     */
    private static void drawBarLegend( Graphics2D g, List<ColorMapEntry> entries, int x, int y, int height ) {
        int barWidth = 24;
        int steps = entries.size() - 1;
        double stepHeight = (double) height / steps;
        for( int i = 0; i < steps; i++ ) {
            // top is the highest value
            int y1 = y + (int) Math.round(i * stepHeight);
            int y2 = y + (int) Math.round((i + 1) * stepHeight);
            Color top = color(entries.get(steps - i));
            Color bottom = color(entries.get(steps - i - 1));
            g.setPaint(new GradientPaint(x, y1, top, x, y2, bottom));
            g.fillRect(x, y1, barWidth, y2 - y1);
        }
        g.setColor(Color.DARK_GRAY);
        g.setStroke(new BasicStroke(1f));
        g.drawRect(x, y, barWidth, height);

        g.setFont(FONT);
        FontMetrics metrics = g.getFontMetrics();
        int labelEvery = Math.max(1, (int) Math.ceil(steps / 8.0));
        for( int i = 0; i <= steps; i++ ) {
            if (i % labelEvery != 0 && i != steps) {
                continue;
            }
            int yLabel = y + (int) Math.round(i * stepHeight);
            g.drawLine(x + barWidth, yLabel, x + barWidth + 4, yLabel);
            g.drawString(format(quantity(entries.get(steps - i))), x + barWidth + 8, yLabel + metrics.getAscent() / 2 - 1);
        }
    }

    private static void drawClassesLegend( Graphics2D g, List<ColorMapEntry> entries, Map<Double, String> classNames, int x,
            int y ) {
        g.setFont(FONT);
        FontMetrics metrics = g.getFontMetrics();
        int box = 18;
        int rowHeight = 26;
        List<Double> drawn = new ArrayList<>();
        int row = 0;
        for( ColorMapEntry entry : entries ) {
            double value = quantity(entry);
            if (drawn.contains(value)) {
                continue;
            }
            drawn.add(value);
            int yRow = y + row * rowHeight;
            g.setColor(color(entry));
            g.fillRect(x, yRow, box, box);
            g.setColor(Color.DARK_GRAY);
            g.drawRect(x, yRow, box, box);
            String label = classNames.getOrDefault(value, format(value));
            g.drawString(label, x + box + 8, yRow + box / 2 + metrics.getAscent() / 2 - 1);
            row++;
        }
    }

    /**
     * @return the width of the legend, larger than the default when the class names need it.
     */
    private static int legendWidth( MapSpec spec ) {
        int width = LEGEND_WIDTH;
        if (!spec.classNames().isEmpty()) {
            Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
            FontMetrics metrics = g.getFontMetrics(FONT);
            for( String name : spec.classNames().values() ) {
                // the box, its gap and the margins
                width = Math.max(width, metrics.stringWidth(name) + 66);
            }
            g.dispose();
        }
        return width;
    }

    private static String format( double value ) {
        if (value == Math.rint(value) && Math.abs(value) < 1e9) {
            return String.format("%,d", (long) value);
        }
        if (Math.abs(value) >= 100) {
            return String.format("%,.0f", value);
        }
        return String.format("%.3g", value);
    }

    public static void main( String[] args ) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: ModuleMapsGenerator <maps spec file> <images folder> <work folder>");
            System.exit(1);
        }
        new ModuleMapsGenerator(new File(args[0]), new File(args[1]), new File(args[2])).generate();
    }
}
