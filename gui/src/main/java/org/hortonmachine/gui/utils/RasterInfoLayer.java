package org.hortonmachine.gui.utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.DirectLayer;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.MapContent;
import org.geotools.map.MapViewport;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.operation.TransformException;

public class RasterInfoLayer extends DirectLayer {

    private GridCoverageLayer layer;
    private GridCoverage2D raster;
    private RegionMap regionMap;
    private static final String DEFAULT_NUMFORMAT = "0.#";

    private boolean showCells = true;
    private boolean showtext = true;
    private GridGeometry2D gg;
    private double cellSize;
    private int cols;
    private int rows;

    public RasterInfoLayer() {
        setTitle("Raster Info Layer");
    }

    public void setRasterLayer( GridCoverageLayer layer ) {
        this.layer = layer;
        if (layer != null) {
            raster = layer.getCoverage();
            regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(raster);
            gg = raster.getGridGeometry();
            cellSize = regionMap.getXres();
            cols = regionMap.getCols();
            rows = regionMap.getRows();
        } else {
            raster = null;
        }
    }

    public GridCoverageLayer getRasterLayer() {
        return layer;
    }

    @Override
    public void draw( Graphics2D g2d, MapContent mapContent, MapViewport viewport ) {
        try {
            if (raster != null) {

                ReferencedEnvelope screenBounds = viewport.getBounds();
                AffineTransform worldToScreen = viewport.getWorldToScreen();

                double w = screenBounds.getMinX();
                double s = screenBounds.getMinY();
                double e = screenBounds.getMaxX();
                double n = screenBounds.getMaxY();

                double half = cellSize / 2;

                GridCoordinates2D llPix = gg.worldToGrid(new DirectPosition2D(w, s));
                GridCoordinates2D urPix = gg.worldToGrid(new DirectPosition2D(e, n));

                int fromC = (int) Math.floor(llPix.getX());
                int toC = (int) Math.ceil(urPix.getX()) + 1;
                int fromR = (int) Math.floor(urPix.getY());
                int toR = (int) Math.ceil(llPix.getY()) + 1;

                int cellCount = (toC - fromC) * (toR - fromR);
                if (cellCount > 1E4) {
                    /// too many cells
                    return;
                }

                DecimalFormat f = new DecimalFormat(DEFAULT_NUMFORMAT);

                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                for( int r = fromR; r < toR; r++ ) {
                    if (r < 0 || r >= rows)
                        continue;
                    for( int c = fromC; c < toC; c++ ) {
                        if (c < 0 || c >= cols)
                            continue;
                        Coordinate cellCenterCoord = CoverageUtilities.coordinateFromColRow(c, r, gg);
                        Point2D p1 = worldToScreen
                                .transform(new Point2D.Double(cellCenterCoord.x - half, cellCenterCoord.y - half), null);
                        Point2D p2 = worldToScreen
                                .transform(new Point2D.Double(cellCenterCoord.x - half, cellCenterCoord.y + half), null);
                        Point2D p3 = worldToScreen
                                .transform(new Point2D.Double(cellCenterCoord.x + half, cellCenterCoord.y + half), null);
                        Point2D p4 = worldToScreen
                                .transform(new Point2D.Double(cellCenterCoord.x + half, cellCenterCoord.y - half), null);
                        if (showCells) {

                            GeneralPath path = new GeneralPath();
                            path.moveTo(p1.getX(), p1.getY());
                            path.lineTo(p2.getX(), p2.getY());
                            path.lineTo(p3.getX(), p3.getY());
                            path.lineTo(p4.getX(), p4.getY());

                            g2d.setColor(Color.black);
                            g2d.setStroke(new BasicStroke(1));
                            g2d.draw(path);

                        }

                        double value = CoverageUtilities.getValue(raster, c, r);
                        if (HMConstants.isNovalue(value)) {
                            continue;
                        }

                        if (showtext) {
                            int cellW = (int) (p3.getX() - p2.getX());
                            int cellH = (int) (p1.getY() - p2.getY());

                            int fontSize = cellH / 5;
                            if (fontSize > 14) {
                                fontSize = 14;
                            }

                            Font font = g2d.getFont();
                            Font newFont = new Font("default", Font.BOLD, fontSize);
                            g2d.setFont(newFont);
                            g2d.setColor(Color.BLACK);
                            FontMetrics fontMetrics = g2d.getFontMetrics();

                            CoverageUtilities.getValue(raster, c, r);
                            String text1 = "col:" + c;
                            String text2 = "row:" + r;
                            String text3 = "value:" + f.format(value);

                            if (!drawStrings(g2d, p1, p2, cellW, cellH, fontMetrics, text1, text2, text3)) {
                                text1 = "c:" + c;
                                text2 = "r:" + r;
                                text3 = "v:" + f.format(value);

                                if (!drawStrings(g2d, p1, p2, cellW, cellH, fontMetrics, text1, text2, text3)) {
                                    text1 = "" + c;
                                    text2 = "" + r;
                                    text3 = "" + f.format(value);
                                    if (!drawStrings(g2d, p1, p2, cellW, cellH, fontMetrics, text1, text2, text3)) {
                                        drawStrings(g2d, p1, p2, cellW, cellH, fontMetrics, text1, text2, null);
                                    }

                                }
                            }
                        }
                    }
                }

            }
        } catch (InvalidGridGeometryException | TransformException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    private boolean drawStrings( Graphics2D g2d, Point2D p1, Point2D p2, int cellW, int cellH, FontMetrics fontMetrics,
            String text1, String text2, String text3 ) {
        Rectangle2D stringBounds = fontMetrics.getStringBounds(text1, g2d);
        Rectangle2D stringBounds2 = fontMetrics.getStringBounds(text2, g2d);
        if (stringBounds2.getWidth() > stringBounds.getWidth()) {
            stringBounds = stringBounds2;
        }
        if (text3 != null) {
            Rectangle2D stringBounds3 = fontMetrics.getStringBounds(text3, g2d);
            if (stringBounds3.getWidth() > stringBounds.getWidth()) {
                stringBounds = stringBounds3;
            }
        }
        int stringW = (int) stringBounds.getWidth();
        int stringH = (int) stringBounds.getHeight();

//        System.out.println(stringW + "/" + cellW);
        if (stringW <= cellW * 0.8 && stringH * 3 <= cellH) {
            int posX = (int) (p1.getX() + (cellW - stringW) / 2);
            int posY = (int) (p2.getY() + stringH);

            FontRenderContext frc = g2d.getFontRenderContext();
            Font font = g2d.getFont();
            GlyphVector gv = font.createGlyphVector(frc, text1);
            Shape o = gv.getOutline(posX, posY);
            g2d.setStroke(new BasicStroke(2.5f));
            g2d.setColor(Color.white);
            g2d.draw(o);
            g2d.setColor(Color.black);
            g2d.drawString(text1, posX, posY);

            g2d.setColor(Color.white);
            gv = font.createGlyphVector(frc, text2);
            o = gv.getOutline(posX, posY + stringH);
            g2d.draw(o);
            g2d.setColor(Color.black);
            g2d.drawString(text2, posX, posY + stringH);
            if (text3 != null) {
                g2d.setColor(Color.white);
                gv = font.createGlyphVector(frc, text3);
                o = gv.getOutline(posX, posY + 2 * stringH);
                g2d.draw(o);
                g2d.setColor(Color.black);
                g2d.drawString(text3, posX, posY + 2 * stringH);
            }
            return true;
        }
        return false;
    }

    @Override
    public ReferencedEnvelope getBounds() {
        if (raster != null) {
            return new ReferencedEnvelope(regionMap.toEnvelope(), raster.getCoordinateReferenceSystem());
        }
        return null;
    }

}
