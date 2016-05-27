package org.jgrasstools.nww.layers.defaults;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;

import com.jogamp.opengl.util.texture.TextureData;
import com.vividsolutions.jts.geom.Coordinate;

import org.jgrasstools.gears.modules.r.tmsgenerator.MBTilesHelper;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.TextureTile;
import gov.nasa.worldwind.layers.mercator.BasicMercatorTiledImageLayer;
import gov.nasa.worldwind.layers.mercator.MercatorSector;
import gov.nasa.worldwind.layers.mercator.MercatorTextureTile;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.OGLUtil;

/**
 * Trying to add support to TileMill generated tiles (mbtiles files). <br>
 * <br>
 * MBTiles are simple sqlite databases so we need to add dependency to
 * sqlite-jdbc driver
 * 
 * @author Alessio Iannone - alessio.iannone@nais-solutions.it
 */
public class MBTileLayer extends BasicMercatorTiledImageLayer implements NwwLayer {

    public final static String DEFAULT_TEST_CLASS = "org.sqlite.JDBC";
    public final static String TEST_CLASS_PROPERTY = "testClass";
    /**
     * Test class to use for check existence of JDBC drivers.
     */
    protected String testClass = DEFAULT_TEST_CLASS;
    private boolean driverFound;
    private Connection conn;
    private Statement stat;

    private ReentrantLock dbLock;
    private WorldWindow wwd;
    private final static BufferedImage emptyBuffer = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
    private MBTilesHelper mbTilesHelper;

    /**
     * 
     * @param serviceName
     *            - used for the properties prefix (not yet)
     * @param prettyName
     *            - the pretty name to be shown on the layer tree or similar
     *            components
     * @param wwd
     *            - the parent WorldWindow object
     */
    public MBTileLayer(File mbtilesFile, String prettyName, WorldWindow wwd) {
        super(makeLevels(mbtilesFile));
        setName(prettyName);
        this.wwd = wwd;

        dbLock = new ReentrantLock();

        // just for debug enable tile id rendering
        setDrawTileIDs(true);
        initDBConnection(mbtilesFile);
    }

    /**
     * Initialize connection with the sql lite db. An MBTile is a sql lite
     * database
     * 
     * @param mbtilesFile
     */
    private void initDBConnection(File mbtilesFile) {
        mbTilesHelper = new MBTilesHelper();
        try {
            mbTilesHelper.open(mbtilesFile);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //        try {
        //            Class.forName(testClass);
        //            driverFound = true;
        //        } catch (Exception e) {
        //            Logging.logger().warning("can't locate sqlite JDBC components");
        //            driverFound = false;
        //        }
        //        if (root == null || root.length() == 0) {
        //            System.out.println("No rootDir has been defined");
        //            return;
        //        }
        //        try {
        //            conn = DriverManager.getConnection(root);
        //        } catch (SQLException e) {
        //            System.out.println("Unable to open connection with rootDir:" + root);
        //        }
        //
        //        try {
        //            stat = conn.createStatement();
        //        } catch (SQLException e) {
        //            System.out.println("Unable to create statemnt from connection");
        //        }
    }

    private static LevelSet makeLevels(File dbFile) {
        String format = ".png";
        try {
            MBTilesHelper mbTilesHelper = new MBTilesHelper();
            mbTilesHelper.open(dbFile);
            String imageFormat = mbTilesHelper.getImageFormat();
            format = "." + imageFormat;
            mbTilesHelper.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        AVList params = new AVListImpl();

        params.setValue(AVKey.TILE_WIDTH, 256);
        params.setValue(AVKey.TILE_HEIGHT, 256);
        params.setValue(AVKey.DATA_CACHE_NAME, "Earth/MBTile/" + dbFile.getName());
        // params.setValue(AVKey.SERVICE, service);
        params.setValue(AVKey.DATASET_NAME, "h");
        params.setValue(AVKey.FORMAT_SUFFIX, format);
        params.setValue(AVKey.NUM_LEVELS, 18);
        params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(22.5d), Angle.fromDegrees(45d)));
        //		params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(36d), Angle.fromDegrees(36d)));
        params.setValue(AVKey.SECTOR, new MercatorSector(-1.0, 1.0, Angle.NEG180, Angle.POS180));
        // params.setValue(AVKey.TILE_URL_BUILDER, new URLBuilder());

        return new LevelSet(params);
    }

    private BufferedImage getBuffered(int zoomLevel, int x, int y) {
        //		System.out.println("MBTileLayer.getBuffered() zoomLevel:" + zoomLevel+ " x:" + x + " y:" + y);
        if (!driverFound) {
            return emptyBuffer;
        }
        int level = zoomLevel + 3;
        int row = y;
        //row= ((1 << (zoomLevel + 3)) - 1 - y) ;

        //        StringBuilder statement = new StringBuilder("select tile_data,map.tile_id from map, images where");
        //        statement.append(" zoom_level = ").append(level);
        //        statement.append(" and tile_column = ").append(x);
        //        statement.append(" and tile_row = ").append(row);
        //        statement.append(" and map.tile_id = images.tile_id;");
        //		System.out.println("SQL:" + statement);
        BufferedImage bi = null;

        try {
            BufferedImage tile = mbTilesHelper.getTile(x, y, level);
            if (tile != null) {
                return tile;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // dbLock.lock();
        // try {
        //// stat = conn.createStatement();
        // stat.closeOnCompletion();
        // } catch (SQLException e1) {
        // // TODO Auto-generated catch block
        // e1.printStackTrace();
        // }
        //        try (ResultSet rs = stat.executeQuery(statement.toString())) {
        //            // "select zoom_level, tile_column, tile_row, tile_data from map,
        //            // images where map.tile_id = images.tile_id";
        //            while (rs.next()) {
        //                byte[] imageBytes = rs.getBytes(1);
        //
        //                boolean orig = ImageIO.getUseCache();
        //                ImageIO.setUseCache(false);
        //                ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
        //                bi = ImageIO.read(bis);
        //                ImageIO.setUseCache(orig);
        //
        //                if (bi != null) {
        //                    System.out.println("##### Found Image !!!! ##### -  sql:" + statement.toString());
        //                    return bi;
        //                }
        //
        //            }
        //            rs.close();
        //
        //        } catch (Exception e) {
        //            System.out.println("something went wrong fetching image from database: " + e.getMessage());
        //            e.printStackTrace();
        //        } finally {
        //            // dbLock.unlock();
        //        }
        return emptyBuffer;
    }

    /**
     * Load the TextureData relative to the given {@link MercatorTextureTile}.
     * 
     * @param tile
     * @return If tile data is not available return false, true otherwise
     */
    public boolean loadTexture(MercatorTextureTile tile) {
        // Retrieve the unprocessed BufferedImage
        BufferedImage unprocessedImg = getBuffered(tile.getLevelNumber(), tile.getColumn(), tile.getRow());

        // We don't have tile data for that tile location so getBuffered has
        // returned emptyBuffer, we can skip other processing and return false
        if (unprocessedImg == emptyBuffer) {
            return false;
        }
        BufferedImage image = transform(unprocessedImg, tile.getMercatorSector());

        try (ByteArrayOutputStream os = new ByteArrayOutputStream();) {
            ImageIO.write(image, "png", os);
            InputStream is = new ByteArrayInputStream(os.toByteArray());
            TextureData td = OGLUtil.newTextureData(Configuration.getMaxCompatibleGLProfile(), is, isUseMipMaps());
            if (td != null) {
                tile.setTextureData(td);
            }

            if (tile.getLevelNumber() != 0 || !this.isRetainLevelZeroTiles())
                this.addTileToCache(tile);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Load tile data for the given {@link MercatorTextureTile}. If no tile data
     * is available an BufferedImage is returned by the invoked
     * {@link #getBuffered(int, int, int)} method
     */
    @Override
    protected void forceTextureLoad(MercatorTextureTile tile) {
        BufferedImage image =
            transform(getBuffered(tile.getLevelNumber(), tile.getColumn(), tile.getRow()), tile.getMercatorSector());

        try (ByteArrayOutputStream os = new ByteArrayOutputStream();) {
            ImageIO.write(image, "png", os);
            InputStream is = new ByteArrayInputStream(os.toByteArray());
            TextureData td = OGLUtil.newTextureData(Configuration.getMaxCompatibleGLProfile(), is, isUseMipMaps());
            if (td != null) {
                tile.setTextureData(td);
            }

            if (tile.getLevelNumber() != 0 || !this.isRetainLevelZeroTiles())
                this.addTileToCache(tile);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private BufferedImage transform(BufferedImage image, MercatorSector sector) {
        // Force to be INT_ARGB
        //		if(true){
        //			return image;
        //		}
        BufferedImage trans = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        double miny = sector.getMinLatPercent();
        double maxy = sector.getMaxLatPercent();
        for (int y = 0; y < image.getHeight(); y++) {
            double sy = 1.0 - y / (double) (image.getHeight() - 1);
            Angle lat = Angle.fromRadians(sy * sector.getDeltaLatRadians() + sector.getMinLatitude().radians);
            double dy = 1.0 - (MercatorSector.gudermannianInverse(lat) - miny) / (maxy - miny);
            dy = Math.max(0.0, Math.min(1.0, dy));
            int iy = (int) (dy * (image.getHeight() - 1));

            for (int x = 0; x < image.getWidth(); x++) {
                trans.setRGB(x, y, image.getRGB(x, iy));
            }
        }
        return trans;
    }

    @Override
    protected void requestTexture(DrawContext dc, MercatorTextureTile tile) {

        // If tile is already in memory cache simply skip. I have to understand
        // if this is needed or not...
        if (isTileInCache(tile)) {
            return;
        }
        Vec4 centroid = tile.getCentroidPoint(dc.getGlobe());
        if (this.getReferencePoint() != null)
            tile.setPriority(centroid.distanceTo3(this.getReferencePoint()));

        // Using the request queue seems to not show any tile, instead invoking
        // directly forceTextureLoad method is working, but with a no smooth
        // rendering. So
        // actually I am unable to load tile in background.
        // enqueueRequest(tile, dc);
        forceTextureLoad(tile);

    }

    /**
     * 
     * @param tile
     * @param dc
     */
    private void enqueueRequest(MercatorTextureTile tile, DrawContext dc) {
        RequestTask task = new RequestTask(tile, this, dc);
        this.getRequestQ().add(task);
    }

    private static class RequestTask implements Runnable, Comparable<RequestTask> {

        private final MBTileLayer layer;
        private final MercatorTextureTile tile;
        private final DrawContext dc;

        private RequestTask(MercatorTextureTile tile, MBTileLayer layer, DrawContext dc) {
            this.layer = layer;
            this.tile = tile;
            this.dc = dc;
        }

        public void run() {
            if (Thread.currentThread().isInterrupted())
                return; // the task was cancelled because it's a duplicate or
            // for some other reason
            // System.out.println(
            // "MBTileLayer.RequestTask.run() Level:" + tile.getLevelNumber() +
            // " Column:" + tile.getColumn());
            // SwingUtilities.invokeLater(new Runnable(){
            // public void run(){
            if (layer.loadTexture(tile)) {
                // layer.forceTextureLoad(tile);
                System.out.println("MBTileLayer.RequestTask.run() Texture Loaded. Key:" + tile.getTileKey().toString());
                layer.getLevels().unmarkResourceAbsent(tile);
                layer.firePropertyChange(AVKey.LAYER, null, this);

            } else {
                layer.getLevels().markResourceAbsent(tile);
            }
            // }
            // });

        }

        /**
         * @param that
         *            the task to compare
         *
         * @return -1 if <code>this</code> less than <code>that</code>, 1 if
         *         greater than, 0 if equal
         *
         * @throws IllegalArgumentException
         *             if <code>that</code> is null
         */
        public int compareTo(RequestTask that) {
            if (that == null) {
                String msg = Logging.getMessage("nullValue.RequestTaskIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }
            return this.tile.getPriority() == that.tile.getPriority() ? 0
                : this.tile.getPriority() < that.tile.getPriority() ? -1 : 1;
        }

        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            final RequestTask that = (RequestTask) o;

            // Don't include layer in comparison so that requests are shared
            // among layers
            return !(tile != null ? !tile.equals(that.tile) : that.tile != null);
        }

        public int hashCode() {
            return (tile != null ? tile.hashCode() : 0);
        }

        public String toString() {
            return this.tile.toString();
        }
    }

    private void addTileToCache(TextureTile tile) {
        TextureTile.getMemoryCache().add(tile.getTileKey(), tile);
    }

    /**
     * 
     * @param tile
     * @return
     */
    private boolean isTileInCache(TextureTile tile) {
        return TextureTile.getMemoryCache().contains(tile.getTileKey());
    }

    @Override
    public Coordinate getCenter() {
        try {
            double[] wsen = mbTilesHelper.getBounds();

            double centerX = wsen[0] + (wsen[2] - wsen[0]) / 2.0;
            double centerY = wsen[1] + (wsen[3] - wsen[1]) / 2.0;
            return new Coordinate(centerX, centerY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
