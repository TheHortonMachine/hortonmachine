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
package org.hortonmachine.gears.ui;

import static org.hortonmachine.gears.libs.modules.HMConstants.OTHER;

import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.RenderedImage;
import java.io.File;

import javax.media.jai.Interpolation;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.geotools.map.FeatureLayer;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.MapContent;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SLD;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.swing.JMapFrame;
import org.geotools.util.factory.GeoTools;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OmsMapsViewer.OMSMAPSVIEWER_DESCRIPTION)
@Documentation(OmsMapsViewer.OMSMAPSVIEWER_DOCUMENTATION)
@Author(name = OmsMapsViewer.OMSMAPSVIEWER_AUTHORNAMES, contact = OmsMapsViewer.OMSMAPSVIEWER_AUTHORCONTACTS)
@Keywords(OmsMapsViewer.OMSMAPSVIEWER_KEYWORDS)
@Label(OmsMapsViewer.OMSMAPSVIEWER_LABEL)
@Name(OmsMapsViewer.OMSMAPSVIEWER_NAME)
@Status(OmsMapsViewer.OMSMAPSVIEWER_STATUS)
@License(OmsMapsViewer.OMSMAPSVIEWER_LICENSE)
@UI(OmsMapsViewer.OMSMAPSVIEWER_UI)
public class OmsMapsViewer extends HMModel {

    @Description(OMSMAPSVIEWER_IN_RASTERS_DESCRIPTION)
    @In
    public String[] inRasters = null;

    public String[] inImageMosaics = null;

    @Description(OMSMAPSVIEWER_IN_VECTORS_DESCRIPTION)
    @In
    public String[] inVectors = null;

    public static final String OMSMAPSVIEWER_DESCRIPTION = "A simple geodata viewer.";
    public static final String OMSMAPSVIEWER_DOCUMENTATION = "OmsMapsViewer.html";
    public static final String OMSMAPSVIEWER_KEYWORDS = "Coverage, Raster, Viewer, UI";
    public static final String OMSMAPSVIEWER_LABEL = OTHER;
    public static final String OMSMAPSVIEWER_NAME = "mapsviewer";
    public static final int OMSMAPSVIEWER_STATUS = 40;
    public static final String OMSMAPSVIEWER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSMAPSVIEWER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSMAPSVIEWER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSMAPSVIEWER_UI = "hide";
    public static final String OMSMAPSVIEWER_IN_RASTERS_DESCRIPTION = "The rasters to visualize.";
    public static final String OMSMAPSVIEWER_IN_RASTER_DESCRIPTION = "The raster to visualize.";
    public static final String OMSMAPSVIEWER_IN_VECTORS_DESCRIPTION = "The feature collections to visualize.";
    public static final String OMSMAPSVIEWER_IN_VECTOR_DESCRIPTION = "The feature collection to visualize.";
    public static final String OMSMAPSVIEWER_IN_S_L_D_DESCRIPTION = "The feature collections style layer.";

    private StyleFactory sf = null;
    private FilterFactory ff = null;
    private StyleBuilder sb = null;

    @Execute
    public void displayMaps() throws Exception {
        sf = CommonFactoryFinder.getStyleFactory(GeoTools.getDefaultHints());
        ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());
        sb = new StyleBuilder(sf, ff);

        final MapContent map = new MapContent();
        map.setTitle("Maps Viewer");

        addImageMosaic(map);

        addCoverages(map);

        addFeatureCollections(map);

        map.getViewport().setCoordinateReferenceSystem(DefaultGeographicCRS.WGS84);

        // Create a JMapFrame with a menu to choose the display style for the
        final JMapFrame frame = new JMapFrame(map);
        frame.setSize(1800, 1200);
        frame.enableStatusBar(true);
        frame.enableTool(JMapFrame.Tool.ZOOM, JMapFrame.Tool.PAN, JMapFrame.Tool.RESET);
        frame.enableToolBar(true);
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter(){
            public void windowClosing( WindowEvent e ) {
                frame.setVisible(false);
            }
        });

        while( frame.isVisible() ) {
            Thread.sleep(300);
        }
    }

    private void addImageMosaic( MapContent map ) throws Exception {
        if (inImageMosaics != null) {
            RasterSymbolizer sym = sf.getDefaultRasterSymbolizer();
            Style style = SLD.wrapSymbolizers(sym);

            final ParameterValue<Color> inTransp = AbstractGridFormat.INPUT_TRANSPARENT_COLOR.createValue();
            inTransp.setValue(Color.white);

            final ParameterValue<Color> outTransp = ImageMosaicFormat.OUTPUT_TRANSPARENT_COLOR.createValue();
            outTransp.setValue(Color.white);
            final ParameterValue<Color> backColor = ImageMosaicFormat.BACKGROUND_COLOR.createValue();
            backColor.setValue(Color.RED);
            final ParameterValue<Boolean> fading = ImageMosaicFormat.FADING.createValue();
            fading.setValue(true);

            final ParameterValue<Interpolation> interpol = ImageMosaicFormat.INTERPOLATION.createValue();
            interpol.setValue(new javax.media.jai.InterpolationBilinear());

            final ParameterValue<Boolean> resol = ImageMosaicFormat.ACCURATE_RESOLUTION.createValue();
            resol.setValue(true);

            
            final ParameterValue<Boolean> multiThread= ImageMosaicFormat.ALLOW_MULTITHREADING.createValue();
            multiThread.setValue(true);

            final ParameterValue<Boolean> usejai = ImageMosaicFormat.USE_JAI_IMAGEREAD.createValue();
            usejai.setValue(false);

            final ParameterValue<double[]> bkg = ImageMosaicFormat.BACKGROUND_VALUES.createValue();
            bkg.setValue(new double[]{0});

            GeneralParameterValue[] gp = new GeneralParameterValue[]{inTransp, multiThread};

            for( String imageMosaicPath : inImageMosaics ) {
                ImageMosaicReader imr = new ImageMosaicReader(new File(imageMosaicPath));
                GridReaderLayer layer = new GridReaderLayer(imr, style, gp);
                map.addLayer(layer);
            }
        }

    }

    private void addFeatureCollections( MapContent map ) throws Exception {
        if (inVectors == null) {
            return;
        }
        for( String path : inVectors ) {
            SimpleFeatureCollection fc = OmsVectorReader.readVector(path);
            GeometryDescriptor geometryDescriptor = fc.getSchema().getGeometryDescriptor();
            EGeometryType type = EGeometryType.forGeometryDescriptor(geometryDescriptor);

            File file = new File(path);
            Style style = SldUtilities.getStyleFromFile(file);

            switch( type ) {
            case MULTIPOLYGON:
            case POLYGON:
                if (style == null) {
                    Stroke polygonStroke = sf.createStroke(ff.literal(Color.BLUE), ff.literal(2));
                    Fill polygonFill = sf.createFill(ff.literal(Color.BLUE), ff.literal(0.0));

                    Rule polygonRule = sf.createRule();
                    PolygonSymbolizer polygonSymbolizer = sf.createPolygonSymbolizer(polygonStroke, polygonFill, null);
                    polygonRule.symbolizers().add(polygonSymbolizer);

                    FeatureTypeStyle polygonFeatureTypeStyle = sf.createFeatureTypeStyle();
                    polygonFeatureTypeStyle.rules().add(polygonRule);

                    style = sf.createStyle();
                    style.featureTypeStyles().add(polygonFeatureTypeStyle);
                    style.setName("polygons");
                }
                break;
            case MULTIPOINT:
            case POINT:
                if (style == null) {
                    Mark circleMark = sf.getCircleMark();
                    Fill fill = sf.createFill(ff.literal(Color.RED));
                    circleMark.setFill(fill);
                    // circleMark.setStroke(null);

                    Graphic gr = sf.createDefaultGraphic();
                    gr.graphicalSymbols().clear();
                    gr.graphicalSymbols().add(circleMark);
                    Expression size = ff.literal(6);
                    gr.setSize(size);

                    Rule pointRule = sf.createRule();
                    PointSymbolizer pointSymbolizer = sf.createPointSymbolizer(gr, null);
                    pointRule.symbolizers().add(pointSymbolizer);

                    FeatureTypeStyle pointsFeatureTypeStyle = sf.createFeatureTypeStyle();
                    pointsFeatureTypeStyle.rules().add(pointRule);

                    style = sf.createStyle();
                    style.featureTypeStyles().add(pointsFeatureTypeStyle);
                    style.setName("points");
                }
                break;
            case MULTILINESTRING:
            case LINESTRING:
                if (style == null) {
                    Stroke lineStroke = sf.createStroke(ff.literal(Color.RED), ff.literal(2));

                    Rule lineRule = sf.createRule();
                    LineSymbolizer lineSymbolizer = sf.createLineSymbolizer(lineStroke, null);
                    lineRule.symbolizers().add(lineSymbolizer);

                    FeatureTypeStyle lineFeatureTypeStyle = sf.createFeatureTypeStyle();
                    lineFeatureTypeStyle.rules().add(lineRule);

                    style = sf.createStyle();
                    style.featureTypeStyles().add(lineFeatureTypeStyle);
                    style.setName("lines");
                }
                break;

            default:
                break;
            }

            FeatureLayer layer = new FeatureLayer(fc, style);
            map.addLayer(layer);

        }

    }

    private void addCoverages( final MapContent map ) throws Exception {
        if (inRasters == null) {
            return;
        }
        RasterSymbolizer rasterSym = sf.createRasterSymbolizer();
        ColorMap colorMap = sf.createColorMap();

        for( String rasterPath : inRasters ) {
            GridCoverage2D readRaster = OmsRasterReader.readRaster(rasterPath);
            RenderedImage renderedImage = readRaster.getRenderedImage();
            double max = Double.NEGATIVE_INFINITY;
            double min = Double.POSITIVE_INFINITY;
            RectIter iter = RectIterFactory.create(renderedImage, null);
            do {
                do {
                    double value = iter.getSampleDouble();
                    if (value > max) {
                        max = value;
                    }
                    if (value < min) {
                        min = value;
                    }
                } while( !iter.nextPixelDone() );
                iter.startPixels();
            } while( !iter.nextLineDone() );

            // red to blue
            Color fromColor = Color.blue;
            Color midColor = Color.green;
            Color toColor = Color.red;
            Expression fromColorExpr = sb
                    .colorExpression(new java.awt.Color(fromColor.getRed(), fromColor.getGreen(), fromColor.getBlue(), 255));
            Expression midColorExpr = sb
                    .colorExpression(new java.awt.Color(midColor.getRed(), midColor.getGreen(), midColor.getBlue(), 255));
            Expression toColorExpr = sb
                    .colorExpression(new java.awt.Color(toColor.getRed(), toColor.getGreen(), toColor.getBlue(), 255));
            Expression fromExpr = sb.literalExpression(min);
            Expression midExpr = sb.literalExpression(min + (max - min) / 2);
            Expression toExpr = sb.literalExpression(max);

            ColorMapEntry entry = sf.createColorMapEntry();
            entry.setQuantity(fromExpr);
            entry.setColor(fromColorExpr);
            colorMap.addColorMapEntry(entry);

            entry = sf.createColorMapEntry();
            entry.setQuantity(midExpr);
            entry.setColor(midColorExpr);
            colorMap.addColorMapEntry(entry);

            entry = sf.createColorMapEntry();
            entry.setQuantity(toExpr);
            entry.setColor(toColorExpr);
            colorMap.addColorMapEntry(entry);

            rasterSym.setColorMap(colorMap);

            Style rasterStyle = SLD.wrapSymbolizers(rasterSym);

            GridCoverageLayer layer = new GridCoverageLayer(readRaster, rasterStyle);

            map.addLayer(layer);
        }
    }

    public static void main( String[] args ) throws Exception {
        OmsMapsViewer mv = new OmsMapsViewer();
        mv.inVectors = new String[]{"/media/hydrologis/Samsung_T3/IMAGEMOSAICTEST/ctr10k.shp"};
        mv.inImageMosaics = new String[]{"/media/hydrologis/Samsung_T3/IMAGEMOSAICTEST/ctr10k/ctr10k.shp"};
        mv.displayMaps();

    }

}
