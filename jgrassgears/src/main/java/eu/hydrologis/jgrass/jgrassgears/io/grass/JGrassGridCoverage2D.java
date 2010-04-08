/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package eu.hydrologis.jgrass.jgrassgears.io.grass;

import java.awt.Color;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

import javax.media.jai.RasterFactory;

import org.geotools.coverage.Category;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.Envelope2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.hydrologis.jgrass.jgrassgears.io.grass.core.color.JGrassColorTable;

/**
 * Wrapper class for {@link GridCoverage2D} rasters.
 * <p>
 * This implements the builder pattern to create GridCoverages starting from a
 * {@link WritableRaster}.
 * </p>
 * <p>
 * Note: read carefully the various build options, since they will influence the resulting coverage.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 3.0
 * @see GrassCoverageReader
 * @see GridCoverage2D
 * @see WritableRaster
 * @see JGrassRegion
 */
public class JGrassGridCoverage2D {
    public static int DOUBLE = DataBuffer.TYPE_DOUBLE;
    public static int FLOAT = DataBuffer.TYPE_FLOAT;
    public static int INT = DataBuffer.TYPE_INT;

    private WritableRaster raster = null;
    private RenderedImage renderedImage = null;
    private GridCoverage2D coverage2D = null;

    /**
     * Builder of {@link JGrassGridCoverage2D} from a writable raster.
     */
    public static class WritableGridCoverageBuilder {
        private String name = "coverage";
        private WritableRaster raster = null;
        private int databufferType = JGrassGridCoverage2D.DOUBLE;
        private JGrassRegion writeRegion = null;
        private double[] dataRange = new double[]{0.0, 4000.0};
        private List<String> colorRulesList = null;
        private CoordinateReferenceSystem crs = null;

        /**
         * Sets the {@link WritableRaster raster} that the {@link GridCoverage2D} will wrap.
         * <p>
         * Note: If not set, at least the writeRegion has to be set, so that the raster of the
         * proper dimension can be created.
         * </p>
         * 
         * @param raster the raster with the data for the coverage.
         */
        public WritableGridCoverageBuilder( WritableRaster raster ) {
            this.raster = raster;
        }

        /**
         * Sets the {@linkplain JGrassRegion write region}.
         * <p>
         * Note: This is a mandatory parameter to create a proper {@link GridCoverage2D}.
         * </p>
         * 
         * @param writeRegion the write region
         * @return the builder.
         */
        public WritableGridCoverageBuilder writeRegion( JGrassRegion writeRegion ) {
            this.writeRegion = writeRegion;
            return this;
        }

        /**
         * Sets the name of the coverage.
         * <p>
         * Note: This is an optional parameter.
         * </p>
         * 
         * @param name a name for the coverage.
         * @return the builder.
         */
        public WritableGridCoverageBuilder name( String name ) {
            this.name = name;
            return this;
        }

        /**
         * Sets the data type to be used for the coverage.
         * <p>
         * Note: If not supplied, it defaults to double.
         * </p>
         * 
         * @param databufferType the data type as defined in {@link JGrassGridCoverage2D#DOUBLE},
         *        {@link JGrassGridCoverage2D#FLOAT}, {@link JGrassGridCoverage2D#INT}.
         * @return the builder.
         */
        public WritableGridCoverageBuilder dataType( int databufferType ) {
            this.databufferType = databufferType;
            return this;
        }

        /**
         * Sets the data range for the coverage.
         * <p>
         * Note: This is a mandatory parameter to create a proper {@link GridCoverage2D}.
         * </p>
         * 
         * @param dataRange an array holding the max and min value.
         * @return the builder.
         */
        public WritableGridCoverageBuilder dataRange( double[] dataRange ) {
            this.dataRange = dataRange;
            return this;
        }

        /**
         * Sets the color rules to create the categories of the coverage.
         * <p>
         * Note: If this is not set, but a range is supplied, a default colortable is created
         * anyway.
         * </p>
         * 
         * @param colorRulesList a list of colorrules.
         * @return the builder.
         */
        public WritableGridCoverageBuilder colorRules( List<String> colorRulesList ) {
            this.colorRulesList = colorRulesList;
            return this;
        }

        /**
         * Sets the {@link CoordinateReferenceSystem} for the coverage.
         * <p>
         * Note: This is a mandatory parameter to create a proper {@link GridCoverage2D}.
         * </p>
         * 
         * @param crs the coordinate reference system.
         * @return the builder.
         */
        public WritableGridCoverageBuilder crs( CoordinateReferenceSystem crs ) {
            this.crs = crs;
            return this;
        }

        /**
         * Builds the {@link JGrassGridCoverage2D}.
         * 
         * @return the JGrassGridCoverage2D.
         */
        public JGrassGridCoverage2D build() {
            return new JGrassGridCoverage2D(this);
        }

    }

    /**
     * Builder of {@link JGrassGridCoverage2D}.
     */
    public static class GridCoverageBuilder {
        private String name = "coverage";
        private RenderedImage raster = null;
        private int databufferType = JGrassGridCoverage2D.DOUBLE;
        private JGrassRegion writeRegion = null;
        private double[] dataRange = new double[]{0.0, 4000.0};
        private List<String> colorRulesList = null;
        private CoordinateReferenceSystem crs = null;

        /**
         * Constructor for the builder.
         * 
         * @param raster the raster with the data for the coverage.
         */
        public GridCoverageBuilder( RenderedImage raster ) {
            this.raster = raster;
        }

        /**
         * Sets the {@linkplain JGrassRegion write region}.
         * <p>
         * Note: This is a mandatory parameter to create a proper {@link GridCoverage2D}.
         * </p>
         * 
         * @param writeRegion the write region
         * @return the builder.
         */
        public GridCoverageBuilder writeRegion( JGrassRegion writeRegion ) {
            this.writeRegion = writeRegion;
            return this;
        }

        /**
         * Sets the name of the coverage.
         * <p>
         * Note: This is an optional parameter.
         * </p>
         * 
         * @param name a name for the coverage.
         * @return the builder.
         */
        public GridCoverageBuilder name( String name ) {
            this.name = name;
            return this;
        }

        /**
         * Sets the data type to be used for the coverage.
         * <p>
         * Note: If not supplied, it defaults to double.
         * </p>
         * 
         * @param databufferType the data type as defined in {@link JGrassGridCoverage2D#DOUBLE},
         *        {@link JGrassGridCoverage2D#FLOAT}, {@link JGrassGridCoverage2D#INT}.
         * @return the builder.
         */
        public GridCoverageBuilder dataType( int databufferType ) {
            this.databufferType = databufferType;
            return this;
        }

        /**
         * Sets the data range for the coverage.
         * <p>
         * Note: This is a mandatory parameter to create a proper {@link GridCoverage2D}.
         * </p>
         * 
         * @param dataRange an array holding the max and min value.
         * @return the builder.
         */
        public GridCoverageBuilder dataRange( double[] dataRange ) {
            this.dataRange = dataRange;
            return this;
        }

        /**
         * Sets the color rules to create the categories of the coverage.
         * <p>
         * Note: If this is not set, but a range is supplied, a default colortable is created
         * anyway.
         * </p>
         * 
         * @param colorRulesList a list of colorrules.
         * @return the builder.
         */
        public GridCoverageBuilder colorRules( List<String> colorRulesList ) {
            this.colorRulesList = colorRulesList;
            return this;
        }

        /**
         * Sets the {@link CoordinateReferenceSystem} for the coverage.
         * <p>
         * Note: This is a mandatory parameter to create a proper {@link GridCoverage2D}.
         * </p>
         * 
         * @param crs the coordinate reference system.
         * @return the builder.
         */
        public GridCoverageBuilder crs( CoordinateReferenceSystem crs ) {
            this.crs = crs;
            return this;
        }

        /**
         * Builds the {@link JGrassGridCoverage2D}.
         * 
         * @return the JGrassGridCoverage2D.
         */
        public JGrassGridCoverage2D build() {
            return new JGrassGridCoverage2D(this);
        }

    }

    /**
     * Private constructor to be called by the builder {@link WritableGridCoverageBuilder}.
     * 
     * @param builder the builder.
     */
    private JGrassGridCoverage2D( WritableGridCoverageBuilder builder ) {

        double[] dataRange = builder.dataRange;

        int rows = -1;
        int cols = -1;
        double west = -1.0;
        double east = -1.0;
        double north = -1.0;
        double south = -1.0;
        raster = builder.raster;
        JGrassRegion writeRegion = builder.writeRegion;
        if (writeRegion != null) {
            west = writeRegion.getWest();
            east = writeRegion.getEast();
            north = writeRegion.getNorth();
            south = writeRegion.getSouth();
            rows = writeRegion.getRows();
            cols = writeRegion.getCols();
        } else {
            if (raster == null) {
                throw new IllegalArgumentException(
                        "At least one of Raster or JGrassRegion have to be supplied.");
            }
            rows = raster.getHeight();
            cols = raster.getWidth();
            west = 0;
            east = cols;
            south = 0;
            north = rows;
        }

        if (raster == null)
            raster = RasterFactory.createBandedRaster(builder.databufferType, cols, rows, 1, null);

        List<String> colorRulesList = builder.colorRulesList;
        String name = builder.name;
        GridSampleDimension band = null;
        CoordinateReferenceSystem crs = builder.crs;

        boolean makeDummyGridCoverage2D = false;

        if (crs == null) {
            makeDummyGridCoverage2D = true;
        } else {

            if (colorRulesList == null && dataRange == null) {
                makeDummyGridCoverage2D = true;
            } else {
                if (colorRulesList == null && dataRange != null) {
                    colorRulesList = JGrassColorTable.createDefaultColorTable(dataRange, 255);
                }
                int rulesNum = colorRulesList.size();
                int COLORNUM = 60000;

                if (colorRulesList.size() > COLORNUM) {
                    COLORNUM = colorRulesList.size() + 1;
                }
                if (COLORNUM > 65500) {
                    COLORNUM = 65500;
                }

                List<Category> catsList = new ArrayList<Category>();

                double[][] values = new double[rulesNum][2];
                Color[][] colors = new Color[rulesNum][2];
                for( int i = 0; i < rulesNum; i++ ) {
                    String colorRule = colorRulesList.get(i);
                    JGrassColorTable.parseColorRule(colorRule, values[i], colors[i]);
                }

                Category noData = new Category(
                        "novalue", new Color(Color.WHITE.getRed(), Color.WHITE //$NON-NLS-1$
                                .getGreen(), Color.WHITE.getBlue(), 0), 0);
                catsList.add(noData);

                double a = (values[values.length - 1][1] - values[0][0]) / (double) (COLORNUM - 1);
                double pmin = 1;
                double scale = a;
                double offSet = values[0][0] - scale * pmin;

                for( int i = 0; i < rulesNum; i++ ) {
                    StringBuilder sB = new StringBuilder();
                    sB.append(name);
                    sB.append("_"); //$NON-NLS-1$
                    sB.append(i);

                    int lower = (int) ((values[i][0] - values[0][0]) / scale + pmin);
                    int upper = (int) ((values[i][1] - values[0][0]) / scale + pmin);
                    if (lower == upper)
                        upper = upper + 1;
                    Category dataCategory = new Category(sB.toString(), colors[i], lower, upper,
                            scale, offSet);

                    catsList.add(dataCategory);
                }
                Category[] array = (Category[]) catsList.toArray(new Category[catsList.size()]);
                band = new GridSampleDimension(name, array, null);
            }
        }

        if (makeDummyGridCoverage2D) {
            band = new GridSampleDimension(name, new Category[]{}, null);
        }

        band = band.geophysics(true);

        Envelope2D writeEnvelope = new Envelope2D(crs, west, south, east - west, north - south);
        GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);

        coverage2D = factory.create(name, (WritableRaster) raster, writeEnvelope,
                new GridSampleDimension[]{band});

    }

    private JGrassGridCoverage2D( GridCoverageBuilder builder ) {

        renderedImage = builder.raster;
        if (renderedImage == null) {
            throw new IllegalArgumentException(
                    "At least one of Raster or JGrassRegion have to be supplied.");
        }
        double[] dataRange = builder.dataRange;

        int rows = -1;
        int cols = -1;
        double west = -1.0;
        double east = -1.0;
        double north = -1.0;
        double south = -1.0;
        JGrassRegion writeRegion = builder.writeRegion;
        if (writeRegion != null) {
            west = writeRegion.getWest();
            east = writeRegion.getEast();
            north = writeRegion.getNorth();
            south = writeRegion.getSouth();
            // rows = writeRegion.getRows();
            // cols = writeRegion.getCols();
        } else {
            rows = renderedImage.getHeight();
            cols = renderedImage.getWidth();
            west = 0;
            east = cols;
            south = 0;
            north = rows;
        }

        List<String> colorRulesList = builder.colorRulesList;
        String name = builder.name;
        GridSampleDimension band = null;
        CoordinateReferenceSystem crs = builder.crs;

        boolean makeDummyGridCoverage2D = false;

        if (crs == null) {
            makeDummyGridCoverage2D = true;
        } else {

            if (colorRulesList == null && dataRange == null) {
                makeDummyGridCoverage2D = true;
            } else {
                if (colorRulesList == null && dataRange != null) {
                    colorRulesList = JGrassColorTable.createDefaultColorTable(dataRange, 255);
                }
                int rulesNum = colorRulesList.size();
                int COLORNUM = 60000;

                if (colorRulesList.size() > COLORNUM) {
                    COLORNUM = colorRulesList.size() + 1;
                }
                if (COLORNUM > 65500) {
                    COLORNUM = 65500;
                }

                List<Category> catsList = new ArrayList<Category>();

                double[][] values = new double[rulesNum][2];
                Color[][] colors = new Color[rulesNum][2];
                for( int i = 0; i < rulesNum; i++ ) {
                    String colorRule = colorRulesList.get(i);
                    JGrassColorTable.parseColorRule(colorRule, values[i], colors[i]);
                }

                Category noData = new Category(
                        "novalue", new Color(Color.WHITE.getRed(), Color.WHITE //$NON-NLS-1$
                                .getGreen(), Color.WHITE.getBlue(), 0), 0);
                catsList.add(noData);

                double a = (values[values.length - 1][1] - values[0][0]) / (double) (COLORNUM - 1);
                double pmin = 1;
                double scale = a;
                double offSet = values[0][0] - scale * pmin;

                for( int i = 0; i < rulesNum; i++ ) {
                    StringBuilder sB = new StringBuilder();
                    sB.append(name);
                    sB.append("_"); //$NON-NLS-1$
                    sB.append(i);

                    int lower = (int) ((values[i][0] - values[0][0]) / scale + pmin);
                    int upper = (int) ((values[i][1] - values[0][0]) / scale + pmin);
                    if (lower == upper)
                        upper = upper + 1;
                    Category dataCategory = new Category(sB.toString(), colors[i], lower, upper,
                            scale, offSet);

                    catsList.add(dataCategory);
                }
                Category[] array = (Category[]) catsList.toArray(new Category[catsList.size()]);
                band = new GridSampleDimension(name, array, null);
            }
        }

        if (makeDummyGridCoverage2D) {
            band = new GridSampleDimension(name, new Category[]{}, null);
        }

        band = band.geophysics(true);

        Envelope2D writeEnvelope = new Envelope2D(crs, west, south, east - west, north - south);
        GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);

        coverage2D = factory.create(name, renderedImage, writeEnvelope);

    }

    public boolean hasWritableRaster() {
        if (raster != null) {
            return true;
        }
        return false;
    }

    /**
     * Getter for the {@link WritableRaster raster} to be used to write onto.
     * <p>
     * Note: check with {@link JGrassGridCoverage2D#hasWritableRaster()} to see if there coverage is
     * backed by a writable raster.
     * </p>
     * 
     * @return the raster on which the {@link GridCoverage2D} is wrapped.
     */
    public WritableRaster getWritableRaster() {
        return raster;

        // RenderedImage image = coverage2D.getRenderedImage();

        // Following cast you work if the original image was writable.
        // WritableRenderedImage wri = (WritableRenderedImage) image;

        // To be very strict, we should looks at how many tiles
        // are in that image. In the case where we know that the
        // original image was a BufferedImage and because I'm lazy,
        // lets assume that is only one big tile:
        // WritableRaster raster = wri.getWritableTile(0,0);

        // do you computation here, and when you are done:
        // wri.releaseWritableTile(0,0);
    }

    /**
     * Getter for the geotools {@link GridCoverage2D grid coverage}.
     * 
     * @return the grid coverage.
     */
    public GridCoverage2D getGridCoverage2D() {
        return coverage2D;
    }

}
