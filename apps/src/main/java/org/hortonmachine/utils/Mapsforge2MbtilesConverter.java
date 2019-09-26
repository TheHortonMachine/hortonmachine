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
package org.hortonmachine.utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.geotools.data.crs.ReprojectFeatureResults;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.tmsgenerator.GlobalMercator;
import org.hortonmachine.gears.modules.r.tmsgenerator.MBTilesHelper;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.nww.layers.defaults.raster.OsmTilegenerator;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.datastore.MultiMapDataStore.DataPolicy;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.labels.TileBasedLabelStore;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import oms3.annotations.Execute;

public class Mapsforge2MbtilesConverter extends HMModel {
    private static final int TILESIZE = 256;

    public File[] inMapFiles = null;

    public File mbtilesFile = null;

    public File roiFile = null;

    public File limitsFile = null;

    public int pTilesize = TILESIZE;

    public float pScaleFactor = 1.5f;

    public int minZoom = 13;

    public int maxZoom = 17;

    public int maxZoomLimit = 21;

    public Double pRoiBuffer = 2.0;

    public String datasetName = null;

    private MBTilesHelper mbtilesHelper;

    private String format;

    private volatile int imageIndex = 0;
    private volatile int ignoredIndex = 0;
    private volatile int totalPossibleCount = 0;
    private PreparedGeometry preparedROI = null;
    private PreparedGeometry preparedLimits = null;

    private boolean doParallel = false;

    @Execute
    public void process() throws Exception {
        if (pTilesize < 256) {
            pTilesize = TILESIZE;
        }
        if (pScaleFactor < 1)
            pScaleFactor = 1.5f;

        if (datasetName == null && roiFile != null) {
            datasetName = FileUtilities.getNameWithoutExtention(roiFile);
        }

        if (maxZoomLimit == -1)
            maxZoomLimit = maxZoom;

        CoordinateReferenceSystem latLongCrs = DefaultGeographicCRS.WGS84;

        ReferencedEnvelope llBounds = null;
        if (roiFile != null) {
            SimpleFeatureCollection roiVector = getVector(roiFile.getAbsolutePath());
            roiVector = new ReprojectFeatureResults(roiVector, latLongCrs);
            List<Geometry> roiGeomList = FeatureUtilities.featureCollectionToGeometriesList(roiVector, true, null);
            if (pRoiBuffer != null) {
                roiGeomList = roiGeomList.stream().map(g -> g.buffer(pRoiBuffer)).collect(Collectors.toList());
            }
            Geometry roiGeomUnion = CascadedPolygonUnion.union(roiGeomList);
            preparedROI = PreparedGeometryFactory.prepare(roiGeomUnion);
            llBounds = roiVector.getBounds();
        }

        if (limitsFile != null) {
            SimpleFeatureCollection limitsVector = getVector(limitsFile.getAbsolutePath());
            limitsVector = new ReprojectFeatureResults(limitsVector, latLongCrs);
            List<Geometry> limitsGeomList = FeatureUtilities.featureCollectionToGeometriesList(limitsVector, true, null);
            Geometry limitsGeomUnion = CascadedPolygonUnion.union(limitsGeomList);
            preparedLimits = PreparedGeometryFactory.prepare(limitsGeomUnion);
        }

        if (llBounds == null) {
            llBounds = new ReferencedEnvelope(latLongCrs);
        }
        OsmTilegenerator osmTilegenerator = getGenerator(llBounds);

        format = "png";

        GlobalMercator gm = new GlobalMercator();
        mbtilesHelper = new MBTilesHelper();
        mbtilesHelper.open(mbtilesFile);
        mbtilesHelper.createTables(false);

        Envelope totalBounds = new Envelope();

        // List<Runnable> generationList = new ArrayList<>();
        double w = llBounds.getMinX();
        double e = llBounds.getMaxX();
        double s = llBounds.getMinY();
        double n = llBounds.getMaxY();

        IntStream rangeStream = IntStream.rangeClosed(minZoom, maxZoomLimit);
        rangeStream.forEach(z -> {
            totalBounds.expandToInclude(w, s);
            totalBounds.expandToInclude(e, n);

            int[] llTileXY = gm.GoogleTile(s, w, z);
            int[] urTileXY = gm.GoogleTile(n, e, z);
            llTileXY = gm.TMSTileFromGoogleTile(llTileXY[0], llTileXY[1], z);
            urTileXY = gm.TMSTileFromGoogleTile(urTileXY[0], urTileXY[1], z);

            int startXTile = Math.min(llTileXY[0], urTileXY[0]);
            int endXTile = Math.max(llTileXY[0], urTileXY[0]);
            int startYTile = Math.min(llTileXY[1], urTileXY[1]);
            int endYTile = Math.max(llTileXY[1], urTileXY[1]);

            int tileCols = endXTile - startXTile + 1;
            int tileRows = endYTile - startYTile + 1;

            totalPossibleCount += tileCols * tileRows;

            pm.beginTask("Collecting tiles at zoom level: " + z + " with tiles: " + tileCols + "x" + tileRows,
                    (endXTile - startXTile + 1));
            for( int x = startXTile; x <= endXTile; x++ ) {
                for( int y = startYTile; y <= endYTile; y++ ) {

                    Envelope bb = gm.TileLatLonBounds(x, y, z);
                    // Envelope bb = MBTilesHelper.tile2boundingBox(x, y, z);
                    Polygon tilePolygon = GeometryUtilities.createPolygonFromEnvelope(bb);
                    if (preparedROI != null && !preparedROI.intersects(tilePolygon)) {
                        ignoredIndex++;
                        continue;
                    }
                    if (preparedLimits != null && z > maxZoom && z < maxZoomLimit && !preparedLimits.intersects(tilePolygon)) {
                        ignoredIndex++;
                        continue;
                    }

                    int[] mapsforgeTilesNum = gm.TMSTileFromGoogleTile(x, y, z);
                    BufferedImage tileImage = osmTilegenerator.getImage(z, mapsforgeTilesNum[0], mapsforgeTilesNum[1]);
                    // try {
                    // ImageIO.write(tileImage, format,
                    // new File("/home/hydrologis/TMP/tile_" + z + "_" + x + "_" + y + ".png"));
                    // } catch (IOException ex) {
                    // ex.printStackTrace();
                    // }

                    try {
                        boolean doBatch = false;
                        if (imageIndex > 0 && imageIndex % 500 == 0) {
                            doBatch = true;
                            pm.message("Images inserted in db: " + imageIndex + " ignored: " + ignoredIndex + " of possible "
                                    + totalPossibleCount);
                        }
                        mbtilesHelper.addTileBatch(x, y, z, tileImage, format, doBatch);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    imageIndex++;
                }
                pm.worked(1);
            }

            mbtilesHelper.triggerLastBatch();
            pm.done();

        });

        // pm.beginTask("Generating tiles...", generationList.size());
        // generationList.stream().forEach(r -> {
        // r.run();
        // pm.worked(1);
        // });
        // pm.done();

        mbtilesHelper.fillMetadata((float) totalBounds.getMaxY(), (float) totalBounds.getMinY(), (float) totalBounds.getMinX(),
                (float) totalBounds.getMaxX(), datasetName, format, minZoom, maxZoomLimit);
        mbtilesHelper.createIndexes();
        mbtilesHelper.close();
    }

    private OsmTilegenerator getGenerator( ReferencedEnvelope llBounds ) {
//        MapWorkerPool.NUMBER_OF_THREADS = 4;
//        // Map buffer size
//        ReadBuffer.setMaximumBufferSize(6500000);
//        // Square frame buffer
//        FrameBufferController.setUseSquareFrameBuffer(false);

        DisplayModel model = new DisplayModel();
        model.setUserScaleFactor(pScaleFactor);
        model.setFixedTileSize(pTilesize);

        DataPolicy dataPolicy = DataPolicy.RETURN_ALL;
        MultiMapDataStore mapDatabase = new MultiMapDataStore(dataPolicy);
        for( int i = 0; i < inMapFiles.length; i++ )
            mapDatabase.addMapDataStore(new MapFile(inMapFiles[i]), false, false);

        if (llBounds != null) {
            BoundingBox bb = mapDatabase.boundingBox();
            llBounds.expandToInclude(new Envelope(bb.minLongitude, bb.maxLongitude, bb.minLatitude, bb.maxLatitude));
        }

        InMemoryTileCache tileCache = new InMemoryTileCache(200);
        DatabaseRenderer renderer = new DatabaseRenderer(mapDatabase, AwtGraphicFactory.INSTANCE, tileCache,
                new TileBasedLabelStore(tileCache.getCapacityFirstLevel()), true, true, null);
        InternalRenderTheme xmlRenderTheme = InternalRenderTheme.DEFAULT;
        RenderThemeFuture theme = new RenderThemeFuture(AwtGraphicFactory.INSTANCE, xmlRenderTheme, model);
        // super important!! without the following line, all rendering
        // activities will block until the theme is created.
        new Thread(theme).start();
        OsmTilegenerator osmTilegenerator = new OsmTilegenerator(mapDatabase, renderer, theme, model, pTilesize);
        return osmTilegenerator;
    }

    public static void main( String[] args ) throws Exception {
        File mapFile = new File("/home/hydrologis/data/italy.map");
        File outFile = new File("/home/hydrologis/TMP/italy7.mbtiles");
        File boundsFile = new File("/home/hydrologis/TMP/drivehome.shp");
        File limitsFile = new File("/home/hydrologis/TMP/drivehomelimits.shp");
        // File mapFile = new File(
        // "/home/hydrologis/Dropbox/hydrologis/lavori/2017_08_consulenza_app_FAO/data/countries/zimbabwe/zimbabwe.map");
        // File outFile = new File("/home/hydrologis/TMP/zimbabwe.mbtiles");
        // File boundsFile = new File(
        // "/home/hydrologis/Dropbox/hydrologis/lavori/2017_08_consulenza_app_FAO/data/zimbawe_sampling_grid_20171010/hexagons.shp");
        // File limitsFile = new File(
        // "/home/hydrologis/Dropbox/hydrologis/lavori/2017_08_consulenza_app_FAO/data/zimbawe_sampling_grid_20171010/hexagons.shp");

        Mapsforge2MbtilesConverter conv = new Mapsforge2MbtilesConverter();
        conv.inMapFiles = new File[]{mapFile};
        conv.mbtilesFile = outFile;
        conv.roiFile = null;// boundsFile;
        conv.limitsFile = null;// limitsFile;
        conv.minZoom = 7;
        conv.maxZoom = 7;
        conv.maxZoomLimit = -1;
        conv.pRoiBuffer = null;
        conv.datasetName = "italy7";

        conv.process();
    }

}
