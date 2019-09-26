package org.hortonmachine.utils;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.awt.util.JavaPreferences;
import org.mapsforge.map.awt.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.debug.TileCoordinatesLayer;
import org.mapsforge.map.layer.debug.TileGridLayer;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

public final class MapsforgeSimpleViewer {
    private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
    private static final boolean SHOW_DEBUG_LAYERS = true;
    private static final boolean SHOW_RASTER_MAP = false;

    private static final String MESSAGE = "Are you sure you want to exit the application?";
    private static final String TITLE = "Confirm close";

    public static void main( String[] args ) {
        File mapFile = new File(
                "/home/hydrologis/data/mapsforge/italy.map");
        List<File> mapFiles = Arrays.asList(mapFile);
        final MapView mapView = createMapView();
        final BoundingBox boundingBox = addLayers(mapView, mapFiles);

        final PreferencesFacade preferencesFacade = new JavaPreferences(Preferences.userNodeForPackage(MapsforgeSimpleViewer.class));

        final JFrame frame = new JFrame();
        frame.setTitle("Mapsforge Simple Viewer");
        frame.add(mapView);
        frame.pack();
        frame.setSize(new Dimension(800, 600));
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing( WindowEvent e ) {
                int result = JOptionPane.showConfirmDialog(frame, MESSAGE, TITLE, JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    mapView.getModel().save(preferencesFacade);
                    mapView.destroyAll();
                    AwtGraphicFactory.clearResourceMemoryCache();
                    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                }
            }

            @Override
            public void windowOpened( WindowEvent e ) {
                final Model model = mapView.getModel();
                model.init(preferencesFacade);
                if (model.mapViewPosition.getZoomLevel() == 0 || !boundingBox.contains(model.mapViewPosition.getCenter())) {
                    byte zoomLevel = LatLongUtils.zoomForBounds(model.mapViewDimension.getDimension(), boundingBox,
                            model.displayModel.getTileSize());
                    model.mapViewPosition.setMapPosition(new MapPosition(boundingBox.getCenterPoint(), zoomLevel));
                }
            }
        });
        frame.setVisible(true);
    }

    private static BoundingBox addLayers( MapView mapView, List<File> mapFiles ) {
        Layers layers = mapView.getLayerManager().getLayers();

        int tileSize = SHOW_RASTER_MAP ? 256 : 512;

        // Tile cache
        TileCache tileCache = createTileCache(tileSize, mapView.getModel().frameBufferModel.getOverdrawFactor(), 1024,
                new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString()));

        final BoundingBox boundingBox;
        if (SHOW_RASTER_MAP) {
            // Raster
            mapView.getModel().displayModel.setFixedTileSize(tileSize);
            TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
            TileDownloadLayer tileDownloadLayer = createTileDownloadLayer(tileCache, mapView.getModel().mapViewPosition,
                    tileSource);
            layers.add(tileDownloadLayer);
            tileDownloadLayer.start();
            mapView.setZoomLevelMin(tileSource.getZoomLevelMin());
            mapView.setZoomLevelMax(tileSource.getZoomLevelMax());
            boundingBox = new BoundingBox(LatLongUtils.LATITUDE_MIN, LatLongUtils.LONGITUDE_MIN, LatLongUtils.LATITUDE_MAX,
                    LatLongUtils.LONGITUDE_MAX);
        } else {
            // Vector
            mapView.getModel().displayModel.setFixedTileSize(tileSize);
            MultiMapDataStore mapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);
            for( File file : mapFiles ) {
                mapDataStore.addMapDataStore(new MapFile(file), false, false);
            }
            TileRendererLayer tileRendererLayer = createTileRendererLayer(tileCache, mapDataStore,
                    mapView.getModel().mapViewPosition);
            layers.add(tileRendererLayer);
            boundingBox = mapDataStore.boundingBox();
        }

        // Debug
        if (SHOW_DEBUG_LAYERS) {
            layers.add(new TileGridLayer(GRAPHIC_FACTORY, mapView.getModel().displayModel));
            layers.add(new TileCoordinatesLayer(GRAPHIC_FACTORY, mapView.getModel().displayModel));
        }

        return boundingBox;
    }

    private static MapView createMapView() {
        MapView mapView = new MapView();
        mapView.getMapScaleBar().setVisible(true);
        if (SHOW_DEBUG_LAYERS) {
            mapView.getFpsCounter().setVisible(true);
        }

        return mapView;
    }

    @SuppressWarnings("unused")
    private static TileDownloadLayer createTileDownloadLayer( TileCache tileCache, IMapViewPosition mapViewPosition,
            TileSource tileSource ) {
        return new TileDownloadLayer(tileCache, mapViewPosition, tileSource, GRAPHIC_FACTORY){
            @Override
            public boolean onTap( LatLong tapLatLong, Point layerXY, Point tapXY ) {
                System.out.println("Tap on: " + tapLatLong);
                return true;
            }
        };
    }

    private static TileRendererLayer createTileRendererLayer( TileCache tileCache, MapDataStore mapDataStore,
            IMapViewPosition mapViewPosition ) {
        TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore, mapViewPosition, false, true, false,
                GRAPHIC_FACTORY){
            @Override
            public boolean onTap( LatLong tapLatLong, Point layerXY, Point tapXY ) {
                System.out.println("Tap on: " + tapLatLong);
                return true;
            }
        };
        tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.DEFAULT);
        return tileRendererLayer;
    }

    /**
     * Utility function to create a two-level tile cache with the right size, using the size of the screen.
     * <p>
     * Combine with <code>FrameBufferController.setUseSquareFrameBuffer(false);</code>
     *
     * @param tileSize       the tile size
     * @param overdrawFactor the overdraw factor applied to the map view
     * @param capacity       the maximum number of entries in file cache
     * @param cacheDirectory the directory where cached tiles will be stored
     * @return a new cache created on the file system
     */
    public static TileCache createTileCache( int tileSize, double overdrawFactor, int capacity, File cacheDirectory ) {
        int cacheSize = getMinimumCacheSize(tileSize, overdrawFactor);
        TileCache firstLevelTileCache = new InMemoryTileCache(cacheSize);
        TileCache secondLevelTileCache = new FileSystemTileCache(capacity, cacheDirectory, AwtGraphicFactory.INSTANCE);
        return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
    }

    /**
     * Compute the minimum cache size for a view, using the size of the screen.
     * <p>
     * Combine with <code>FrameBufferController.setUseSquareFrameBuffer(false);</code>
     *
     * @param tileSize       the tile size
     * @param overdrawFactor the overdraw factor applied to the map view
     * @return the minimum cache size for the view
     */
    public static int getMinimumCacheSize( int tileSize, double overdrawFactor ) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return (int) Math.max(4, Math.round((2 + screenSize.getWidth() * overdrawFactor / tileSize)
                * (2 + screenSize.getHeight() * overdrawFactor / tileSize)));
    }

}