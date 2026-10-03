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
package org.hortonmachine.utils;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

/**
 * A lightweight slippy map (OpenStreetMap tiles in web mercator) to show extents, footprints and image overlays
 * and to draw query regions. All geometries are expected in WGS84 lon/lat.
 *
 * <ul>
 * <li>drag: pan</li>
 * <li>wheel: zoom</li>
 * <li>shift+drag or drag in draw mode: draw a bbox</li>
 * <li>click: select the footprint under the mouse</li>
 * </ul>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
@SuppressWarnings("serial")
public class SlippyMapPanel extends JPanel {
    private static final String TILE_URL = "https://tile.openstreetmap.org/%d/%d/%d.png";
    private static final String USER_AGENT = "HortonMachine/1.0 (https://github.com/moovida/hortonmachine)";
    private static final int TILE_SIZE = 256;
    private static final int MIN_ZOOM = 1;
    private static final int MAX_ZOOM = 18;
    private static final double MAX_LAT = 85.05112878;

    private static final Color EXTENT_COLOR = new Color(30, 90, 200);
    private static final Color BBOX_COLOR = new Color(240, 130, 0);
    private static final Color FOOTPRINT_COLOR = new Color(20, 150, 70);
    private static final Color SELECTED_COLOR = new Color(220, 30, 30);

    private int zoom = 2;
    /** Top left corner of the view in world pixels at the current zoom. */
    private double viewX = 0;
    private double viewY = 0;

    private final Map<String, BufferedImage> tileCache = Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75f, true){
        protected boolean removeEldestEntry( Map.Entry<String, BufferedImage> eldest ) {
            return size() > 600;
        }
    });
    private final Set<String> pendingTiles = ConcurrentHashMap.newKeySet();
    private final Set<String> failedTiles = ConcurrentHashMap.newKeySet();
    private final ExecutorService tileLoader = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "SlippyMapPanel tile loader");
        t.setDaemon(true);
        return t;
    });
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL).build();

    private List<Envelope> extents = new ArrayList<>();
    private Envelope queryBbox;
    private List<Geometry> footprints = new ArrayList<>();
    private int selectedFootprint = -1;

    private BufferedImage overlayImage;
    private Envelope overlayEnvelope;
    private boolean overlayMercator;
    private float overlayOpacity = 1f;

    private boolean drawMode = false;
    private Point dragStart;
    private Point dragCurrent;
    private boolean drawingBbox = false;
    private double[] dragStartView;

    private Consumer<Envelope> bboxListener;
    private Consumer<Integer> footprintSelectionListener;
    private Consumer<String> positionListener;

    public SlippyMapPanel() {
        setPreferredSize(new Dimension(700, 450));
        setBackground(new Color(170, 211, 223));

        MouseAdapter mouse = new MouseAdapter(){
            @Override
            public void mousePressed( MouseEvent e ) {
                if (!SwingUtilities.isLeftMouseButton(e))
                    return;
                dragStart = e.getPoint();
                dragCurrent = null;
                drawingBbox = drawMode || e.isShiftDown();
                dragStartView = new double[]{viewX, viewY};
            }

            @Override
            public void mouseDragged( MouseEvent e ) {
                if (dragStart == null)
                    return;
                dragCurrent = e.getPoint();
                if (!drawingBbox) {
                    viewX = dragStartView[0] - (dragCurrent.x - dragStart.x);
                    viewY = dragStartView[1] - (dragCurrent.y - dragStart.y);
                    clampView();
                }
                repaint();
            }

            @Override
            public void mouseReleased( MouseEvent e ) {
                if (dragStart == null)
                    return;
                Point end = e.getPoint();
                boolean isClick = dragStart.distance(end) < 4;
                if (isClick) {
                    selectFootprintAt(end);
                } else if (drawingBbox) {
                    double[] ll1 = screenToLonLat(dragStart.x, dragStart.y);
                    double[] ll2 = screenToLonLat(end.x, end.y);
                    Envelope env = new Envelope(ll1[0], ll2[0], ll1[1], ll2[1]);
                    setQueryBbox(env);
                    if (bboxListener != null)
                        bboxListener.accept(env);
                }
                dragStart = null;
                dragCurrent = null;
                drawingBbox = false;
                repaint();
            }

            @Override
            public void mouseMoved( MouseEvent e ) {
                if (positionListener != null) {
                    double[] ll = screenToLonLat(e.getX(), e.getY());
                    positionListener.accept(String.format("lon %.5f  lat %.5f  zoom %d", ll[0], ll[1], zoom));
                }
            }

            @Override
            public void mouseWheelMoved( MouseWheelEvent e ) {
                int newZoom = zoom - (int) Math.signum(e.getPreciseWheelRotation());
                zoomAround(newZoom, e.getX(), e.getY());
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);

        addComponentListener(new ComponentAdapter(){
            private boolean initialized = false;
            @Override
            public void componentResized( ComponentEvent e ) {
                if (getWidth() <= 0 || getHeight() <= 0)
                    return;
                if (!initialized) {
                    // the initial view needs the real size of the panel
                    initialized = true;
                    zoomToEnvelope(new Envelope(-170, 170, -60, 75));
                } else {
                    clampView();
                    repaint();
                }
            }
        });
    }

    public void setBboxListener( Consumer<Envelope> bboxListener ) {
        this.bboxListener = bboxListener;
    }

    public void setFootprintSelectionListener( Consumer<Integer> footprintSelectionListener ) {
        this.footprintSelectionListener = footprintSelectionListener;
    }

    public void setPositionListener( Consumer<String> positionListener ) {
        this.positionListener = positionListener;
    }

    /**
     * @param drawMode if true, a plain drag draws a bbox instead of panning.
     */
    public void setDrawMode( boolean drawMode ) {
        this.drawMode = drawMode;
        setCursor(drawMode ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) : Cursor.getDefaultCursor());
    }

    public void setExtents( List<Envelope> extents ) {
        this.extents = extents == null ? new ArrayList<>() : new ArrayList<>(extents);
        repaint();
    }

    public void setQueryBbox( Envelope queryBbox ) {
        this.queryBbox = queryBbox;
        repaint();
    }

    public void setFootprints( List<Geometry> footprints ) {
        this.footprints = footprints == null ? new ArrayList<>() : new ArrayList<>(footprints);
        selectedFootprint = -1;
        repaint();
    }

    public void setSelectedFootprint( int index ) {
        selectedFootprint = index;
        repaint();
    }

    /**
     * Set an image to draw over the base map, below the other geometries.
     *
     * @param image the image or null to remove it.
     * @param lonLatEnvelope the area covered by the image, in WGS84 lon/lat.
     * @param mercator true if the image rows are in web mercator (EPSG:3857), false if they are
     *          linear in latitude (EPSG:4326), in which case the image is warped to the map.
     */
    public void setOverlay( BufferedImage image, Envelope lonLatEnvelope, boolean mercator ) {
        this.overlayImage = image;
        this.overlayEnvelope = lonLatEnvelope;
        this.overlayMercator = mercator;
        repaint();
    }

    public void setOverlayOpacity( float opacity ) {
        this.overlayOpacity = Math.max(0f, Math.min(1f, opacity));
        repaint();
    }

    /**
     * @return the size in screen pixels that an envelope has at the current zoom.
     */
    public int[] getPixelSize( Envelope lonLatEnvelope ) {
        int w = (int) Math.round(sx(lonLatEnvelope.getMaxX()) - sx(lonLatEnvelope.getMinX()));
        int h = (int) Math.round(sy(lonLatEnvelope.getMinY()) - sy(lonLatEnvelope.getMaxY()));
        return new int[]{Math.max(1, w), Math.max(1, h)};
    }

    /**
     * @return the currently visible area in lon/lat.
     */
    public Envelope getViewEnvelope() {
        double[] ul = screenToLonLat(0, 0);
        double[] lr = screenToLonLat(getWidth(), getHeight());
        return new Envelope(Math.max(-180, ul[0]), Math.min(180, lr[0]), lr[1], ul[1]);
    }

    public void zoomToEnvelope( Envelope env ) {
        if (env == null || env.isNull())
            return;
        int w = Math.max(getWidth(), 100);
        int h = Math.max(getHeight(), 100);
        double minLat = Math.max(env.getMinY(), -MAX_LAT);
        double maxLat = Math.min(env.getMaxY(), MAX_LAT);
        int z = MAX_ZOOM;
        for( ; z > MIN_ZOOM; z-- ) {
            double dx = lonToX(env.getMaxX(), z) - lonToX(env.getMinX(), z);
            double dy = latToY(minLat, z) - latToY(maxLat, z);
            if (dx <= w * 0.9 && dy <= h * 0.9)
                break;
        }
        zoom = z;
        double cx = (lonToX(env.getMinX(), z) + lonToX(env.getMaxX(), z)) / 2.0;
        double cy = (latToY(minLat, z) + latToY(maxLat, z)) / 2.0;
        viewX = cx - w / 2.0;
        viewY = cy - h / 2.0;
        clampView();
        repaint();
    }

    public void dispose() {
        tileLoader.shutdownNow();
    }

    private void zoomAround( int newZoom, int sx, int sy ) {
        newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
        if (newZoom == zoom)
            return;
        double[] ll = screenToLonLat(sx, sy);
        zoom = newZoom;
        viewX = lonToX(ll[0], zoom) - sx;
        viewY = latToY(ll[1], zoom) - sy;
        clampView();
        repaint();
    }

    private void clampView() {
        double worldSize = worldSize(zoom);
        int h = getHeight();
        if (worldSize < h) {
            viewY = (worldSize - h) / 2.0;
        } else {
            viewY = Math.max(0, Math.min(worldSize - h, viewY));
        }
    }

    private void selectFootprintAt( Point p ) {
        double[] ll = screenToLonLat(p.x, p.y);
        Geometry point = new org.locationtech.jts.geom.GeometryFactory().createPoint(new Coordinate(ll[0], ll[1]));
        int best = -1;
        double bestArea = Double.MAX_VALUE;
        for( int i = 0; i < footprints.size(); i++ ) {
            Geometry g = footprints.get(i);
            if (g != null && g.intersects(point) && g.getArea() < bestArea) {
                best = i;
                bestArea = g.getArea();
            }
        }
        selectedFootprint = best;
        repaint();
        if (footprintSelectionListener != null)
            footprintSelectionListener.accept(best);
    }

    // ---------------- coordinate conversions ----------------

    private static double worldSize( int z ) {
        return TILE_SIZE * Math.pow(2, z);
    }

    private static double lonToX( double lon, int z ) {
        return (lon + 180.0) / 360.0 * worldSize(z);
    }

    private static double latToY( double lat, int z ) {
        lat = Math.max(-MAX_LAT, Math.min(MAX_LAT, lat));
        double rad = Math.toRadians(lat);
        return (1 - Math.log(Math.tan(rad) + 1 / Math.cos(rad)) / Math.PI) / 2.0 * worldSize(z);
    }

    private static double xToLon( double x, int z ) {
        return x / worldSize(z) * 360.0 - 180.0;
    }

    private static double yToLat( double y, int z ) {
        double n = Math.PI - 2.0 * Math.PI * y / worldSize(z);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    private double[] screenToLonLat( double sx, double sy ) {
        return new double[]{xToLon(viewX + sx, zoom), yToLat(viewY + sy, zoom)};
    }

    private double sx( double lon ) {
        return lonToX(lon, zoom) - viewX;
    }

    private double sy( double lat ) {
        return latToY(lat, zoom) - viewY;
    }

    // ---------------- painting ----------------

    @Override
    protected void paintComponent( Graphics g ) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paintTiles(g2);
        paintOverlay(g2);

        for( Envelope ext : extents ) {
            Shape s = envelopeShape(ext);
            g2.setColor(withAlpha(EXTENT_COLOR, 25));
            g2.fill(s);
            g2.setColor(EXTENT_COLOR);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{8f, 5f}, 0f));
            g2.draw(s);
        }

        g2.setStroke(new BasicStroke(1.2f));
        for( int i = 0; i < footprints.size(); i++ ) {
            if (i == selectedFootprint)
                continue;
            Shape s = geometryShape(footprints.get(i));
            if (s == null)
                continue;
            g2.setColor(withAlpha(FOOTPRINT_COLOR, 30));
            g2.fill(s);
            g2.setColor(FOOTPRINT_COLOR);
            g2.draw(s);
        }
        if (selectedFootprint >= 0 && selectedFootprint < footprints.size()) {
            Shape s = geometryShape(footprints.get(selectedFootprint));
            if (s != null) {
                g2.setColor(withAlpha(SELECTED_COLOR, 60));
                g2.fill(s);
                g2.setColor(SELECTED_COLOR);
                g2.setStroke(new BasicStroke(2.5f));
                g2.draw(s);
            }
        }

        if (queryBbox != null) {
            Shape s = envelopeShape(queryBbox);
            g2.setColor(withAlpha(BBOX_COLOR, 40));
            g2.fill(s);
            g2.setColor(BBOX_COLOR);
            g2.setStroke(new BasicStroke(2.5f));
            g2.draw(s);
        }

        if (drawingBbox && dragStart != null && dragCurrent != null) {
            Rectangle2D r = new Rectangle2D.Double(Math.min(dragStart.x, dragCurrent.x), Math.min(dragStart.y, dragCurrent.y),
                    Math.abs(dragCurrent.x - dragStart.x), Math.abs(dragCurrent.y - dragStart.y));
            g2.setColor(withAlpha(BBOX_COLOR, 50));
            g2.fill(r);
            g2.setColor(BBOX_COLOR);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 4f}, 0f));
            g2.draw(r);
        }

        paintAttribution(g2);
        g2.dispose();
    }

    private void paintTiles( Graphics2D g2 ) {
        int n = 1 << zoom;
        int firstTx = (int) Math.floor(viewX / TILE_SIZE);
        int firstTy = Math.max(0, (int) Math.floor(viewY / TILE_SIZE));
        int lastTx = (int) Math.floor((viewX + getWidth()) / TILE_SIZE);
        int lastTy = Math.min(n - 1, (int) Math.floor((viewY + getHeight()) / TILE_SIZE));
        for( int ty = firstTy; ty <= lastTy; ty++ ) {
            for( int tx = firstTx; tx <= lastTx; tx++ ) {
                int wrappedTx = ((tx % n) + n) % n;
                int px = (int) Math.round(tx * TILE_SIZE - viewX);
                int py = (int) Math.round(ty * TILE_SIZE - viewY);
                BufferedImage tile = getTile(zoom, wrappedTx, ty);
                if (tile != null) {
                    g2.drawImage(tile, px, py, TILE_SIZE, TILE_SIZE, null);
                } else {
                    g2.setColor(new Color(225, 225, 225));
                    g2.fillRect(px, py, TILE_SIZE, TILE_SIZE);
                    g2.setColor(new Color(205, 205, 205));
                    g2.drawRect(px, py, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }

    private void paintOverlay( Graphics2D g2 ) {
        if (overlayImage == null || overlayEnvelope == null)
            return;
        Graphics2D og = (Graphics2D) g2.create();
        og.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        og.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayOpacity));
        int x1 = (int) Math.round(sx(overlayEnvelope.getMinX()));
        int x2 = (int) Math.round(sx(overlayEnvelope.getMaxX()));
        int iw = overlayImage.getWidth();
        int ih = overlayImage.getHeight();
        if (overlayMercator) {
            int y1 = (int) Math.round(sy(overlayEnvelope.getMaxY()));
            int y2 = (int) Math.round(sy(overlayEnvelope.getMinY()));
            og.drawImage(overlayImage, x1, y1, x2, y2, 0, 0, iw, ih, null);
        } else {
            // rows are linear in latitude: draw horizontal strips, each placed at its mercator position.
            // The strips overlap by a pixel to avoid seams, so they are warped opaque into a buffer
            // and the buffer is drawn with the opacity (else the overlaps would show as darker lines)
            BufferedImage warped = new BufferedImage(Math.max(1, getWidth()), Math.max(1, getHeight()), BufferedImage.TYPE_INT_ARGB);
            Graphics2D wg = warped.createGraphics();
            wg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            double north = overlayEnvelope.getMaxY();
            double latPerRow = overlayEnvelope.getHeight() / ih;
            int strip = Math.max(1, ih / 256);
            for( int row = 0; row < ih; row += strip ) {
                int rowEnd = Math.min(ih, row + strip);
                int y1 = (int) Math.round(sy(north - row * latPerRow));
                int y2 = (int) Math.round(sy(north - rowEnd * latPerRow));
                if (y2 < 0 || y1 > getHeight())
                    continue;
                wg.drawImage(overlayImage, x1, y1, x2, Math.max(y2, y1) + 1, 0, row, iw, rowEnd, null);
            }
            wg.dispose();
            og.drawImage(warped, 0, 0, null);
        }
        og.dispose();
    }

    private void paintAttribution( Graphics2D g2 ) {
        String text = "© OpenStreetMap contributors";
        g2.setFont(getFont().deriveFont(Font.PLAIN, 11f));
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(text) + 8;
        int h = fm.getHeight() + 2;
        int x = getWidth() - w;
        int y = getHeight() - h;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
        g2.setColor(Color.WHITE);
        g2.fillRect(x, y, w, h);
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setColor(Color.DARK_GRAY);
        g2.drawString(text, x + 4, y + fm.getAscent() + 1);
    }

    private BufferedImage getTile( int z, int x, int y ) {
        String key = z + "/" + x + "/" + y;
        BufferedImage tile = tileCache.get(key);
        if (tile != null || failedTiles.contains(key))
            return tile;
        if (pendingTiles.add(key)) {
            tileLoader.submit(() -> {
                try {
                    if (z != zoom) // skip tiles of zoom levels no longer shown
                        return;
                    HttpRequest request = HttpRequest.newBuilder(URI.create(String.format(TILE_URL, z, x, y)))
                            .header("User-Agent", USER_AGENT).timeout(Duration.ofSeconds(20)).GET().build();
                    HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    try (InputStream is = response.body()) {
                        if (response.statusCode() == 200) {
                            BufferedImage img = ImageIO.read(is);
                            if (img != null)
                                tileCache.put(key, img);
                        } else {
                            failedTiles.add(key);
                        }
                    }
                    repaint();
                } catch (Exception e) {
                    failedTiles.add(key);
                } finally {
                    pendingTiles.remove(key);
                }
            });
        }
        return null;
    }

    private Shape envelopeShape( Envelope env ) {
        double x1 = sx(env.getMinX());
        double x2 = sx(env.getMaxX());
        double y1 = sy(env.getMaxY());
        double y2 = sy(env.getMinY());
        return new Rectangle2D.Double(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
    }

    private Shape geometryShape( Geometry geometry ) {
        if (geometry == null || geometry.isEmpty())
            return null;
        Path2D path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        for( int i = 0; i < geometry.getNumGeometries(); i++ ) {
            Geometry part = geometry.getGeometryN(i);
            if (part instanceof Polygon polygon) {
                addRing(path, polygon.getExteriorRing());
                for( int r = 0; r < polygon.getNumInteriorRing(); r++ ) {
                    addRing(path, polygon.getInteriorRingN(r));
                }
            } else if (part instanceof LineString line) {
                addRing(path, line);
            } else {
                Coordinate c = part.getCoordinate();
                if (c != null)
                    path.append(new Rectangle2D.Double(sx(c.x) - 3, sy(c.y) - 3, 6, 6), false);
            }
        }
        return path;
    }

    private void addRing( Path2D path, LineString ring ) {
        Coordinate[] coords = ring.getCoordinates();
        for( int i = 0; i < coords.length; i++ ) {
            double x = sx(coords[i].x);
            double y = sy(coords[i].y);
            if (i == 0)
                path.moveTo(x, y);
            else
                path.lineTo(x, y);
        }
        if (ring.isClosed())
            path.closePath();
    }

    private static Color withAlpha( Color c, int alpha ) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }
}
