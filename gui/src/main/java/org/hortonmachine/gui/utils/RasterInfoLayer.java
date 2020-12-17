package org.hortonmachine.gui.utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
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
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.operation.TransformException;

public class RasterInfoLayer extends DirectLayer {

    private GridCoverageLayer layer;
    private GridCoverage2D raster;
    private RegionMap regionMap;
    private static final String DEFAULT_NUMFORMAT = "0.0";

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

                for( int r = fromR; r < toR; r++ ) {
                    if (r < 0 || r >= rows)
                        continue;
                    for( int c = fromC; c < toC; c++ ) {
                        if (c < 0 || c >= cols)
                            continue;
                        double value = CoverageUtilities.getValue(raster, c, r);

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

                            g2d.setColor(Color.white);
                            g2d.setStroke(new BasicStroke(2));
                            g2d.draw(path);
                            g2d.setColor(Color.black);
                            g2d.setStroke(new BasicStroke(1));
                            g2d.draw(path);

                        }

                        if (showtext) {
                            FontMetrics fontMetrics = g2d.getFontMetrics();

                            g2d.setColor(Color.BLACK);

                            CoverageUtilities.getValue(raster, c, r);
                            String text1 = "c:" + c;
                            String text2 = "r:" + r;
                            String text3 = "v:" + f.format(value);

                            Rectangle2D stringBounds = fontMetrics.getStringBounds(text3, g2d);
                            int stringW = (int) stringBounds.getWidth();
                            int stringH = (int) stringBounds.getHeight();
                            int cellW = (int) (p3.getX() - p2.getX());
                            int cellH = (int) (p2.getY() - p1.getY());

                            int posX = (int) (p1.getX() + (cellW - stringW) / 2);
                            int posY = (int) (p2.getY() + stringH);
                            g2d.drawString(text1, posX, posY);
                            g2d.drawString(text2, posX, posY + stringH);
                            g2d.drawString(text3, posX, posY + 2 * stringH);
                        }
                    }
                }

            }
        } catch (InvalidGridGeometryException | TransformException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    @Override
    public ReferencedEnvelope getBounds() {
        if (raster != null) {
            return new ReferencedEnvelope(regionMap.toEnvelope(), raster.getCoordinateReferenceSystem());
        }
        return null;
    }

}
