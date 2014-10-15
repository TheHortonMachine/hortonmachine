package org.jgrasstools.lesto.modules.buildings;
import static java.lang.Math.round;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.jgrasstools.gears.io.las.ALasDataManager;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.v.smoothing.OmsLineSmootherMcMaster;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.densify.Densifier;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

public class G02_BuildingSmootherAndRemover extends JGTModel {
    String pattern = "fino";

    public G02_BuildingSmootherAndRemover() throws Exception {
        double densifyResolution = 0.5;
        double buildingsBuffer = -0.5;

        final String lasPath = "/media/lacntfs/geologico/las/oltre_1400/index.lasfolder";
        final String demPath = "/home/moovida/geologico_2013/geologico2013_grassdb/dtm/cell/dtm_oltre1400";
        String inFolder = "/media/lacntfs/geologico/buildings/results_oltre1400/";
        String outFolder = "/media/lacntfs/geologico/buildings/results_oltre1400_checked/";
        String outCheckedFolder = "/media/lacntfs/geologico/buildings/results_oltre1400_checked/checked/";

        File outFolderFile = new File(outFolder);
        File outCheckedFolderFile = new File(outCheckedFolder);
        File inFolderFile = new File(inFolder);
        File[] shpFiles = inFolderFile.listFiles(new FilenameFilter(){
            public boolean accept( File dir, String name ) {
                return name.endsWith("shp");
            }
        });

        GridCoverage2D dem = getRaster(demPath);

        for( File shpFile : shpFiles ) {
            String name = shpFile.getName();
            System.out.println("*****************************************");
            System.out.println("PROCESSING: " + name);
            System.out.println("*****************************************");

            File outFile = new File(outFolderFile, name);
            if (outFile.exists()) {
                continue;
            }
            File tmpoutFile = new File(outCheckedFolderFile, name);
            if (tmpoutFile.exists()) {
                continue;
            }
            // smooth
            SimpleFeatureCollection smoothedFC = smoothBuildings(densifyResolution, shpFile.getAbsolutePath());

            // remove trees
            DefaultFeatureCollection buildings = removeNonBuildings(smoothedFC, lasPath, dem, buildingsBuffer);

            dumpVector(buildings, outFile.getAbsolutePath());
        }

    }

    public DefaultFeatureCollection removeNonBuildings( SimpleFeatureCollection buildingsFC, String lasPath, GridCoverage2D dem,
            double buildingsBuffer ) throws Exception {

        final List<SimpleFeature> buildingsList = FeatureUtilities.featureCollectionToList(buildingsFC);

        final ALasDataManager lasHandler = ALasDataManager.getDataManager(new File(lasPath), dem, 0, null);
        lasHandler.open();

        final List<SimpleFeature> checkedBuildings = new ArrayList<SimpleFeature>();
        pm.beginTask("Processing...", buildingsList.size());
        for( int i = 0; i < buildingsList.size(); i++ ) {
            SimpleFeature building = buildingsList.get(i);
            Geometry buildingGeom = (Geometry) building.getDefaultGeometry();

            Geometry bufferedGeom = buildingGeom.buffer(buildingsBuffer);
            List<LasRecord> points = lasHandler.getPointsInGeometry(bufferedGeom, false);
            int percOfOnes = checkReturnNum(points, bufferedGeom);
            if (percOfOnes >= 96) {
                checkedBuildings.add(building);
            }
            pm.worked(1);
        }
        pm.done();

        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        fc.addAll(checkedBuildings);

        lasHandler.close();

        return fc;
    }

    private int checkReturnNum( List<LasRecord> points, Geometry bufferedGeom ) {

        int count = 0;
        for( LasRecord l : points ) {
            // System.out.print(l.returnNumber);
            if (l.returnNumber == 1) {
                count++;
            }
        }

        double perc = count * 100.0 / points.size();
        return (int) round(perc);
    }

    private SimpleFeatureCollection smoothBuildings( double densifyResolution, final String shpPath ) throws Exception {
        SimpleFeatureCollection buildingsFC = getVector(shpPath);
        final List<SimpleFeature> buildingsList = FeatureUtilities.featureCollectionToList(buildingsFC);

        List<Polygon> newPolygons = new ArrayList<Polygon>();
        for( final SimpleFeature building : buildingsList ) {
            Geometry buildingGeom = (Geometry) building.getDefaultGeometry();

            buildingGeom = Densifier.densify(buildingGeom, densifyResolution);
            Coordinate[] coordinates = buildingGeom.getCoordinates();
            List<Coordinate> newCoords = new ArrayList<Coordinate>();
            for( int i = 0; i < coordinates.length - 1; i++ ) {
                Coordinate from = coordinates[i];
                Coordinate to = coordinates[i + 1];

                LineSegment l = new LineSegment(from, to);
                Coordinate newCoord = l.pointAlong(0.5);
                newCoords.add(newCoord);
            }

            newCoords.add(newCoords.get(0));

            Polygon newPolygon = gf.createPolygon(newCoords.toArray(new Coordinate[0]));

            // newPolygon = (Polygon) DouglasPeuckerSimplifier.simplify(newPolygon, 0.1);
            newPolygons.add(newPolygon);
        }

        SimpleFeatureCollection fc = FeatureUtilities.featureCollectionFromGeometry(buildingsFC.getBounds()
                .getCoordinateReferenceSystem(), newPolygons.toArray(GeometryUtilities.TYPE_POLYGON));

        OmsLineSmootherMcMaster smoother = new OmsLineSmootherMcMaster();
        smoother.pm = pm;
        smoother.pLimit = 10;
        smoother.inVector = fc;
        smoother.pLookahead = 5;
        // smoother.pSlide = 1;
        // smoother.pDensify = 0.2;
        // smoother.pSimplify = 0.1;
        smoother.process();

        fc = smoother.outVector;
        List<Geometry> geomsList = FeatureUtilities.featureCollectionToGeometriesList(fc, true, null);
        List<Geometry> newGeomsList = new ArrayList<Geometry>();
        for( Geometry geometry : geomsList ) {
            Polygon polygon = gf.createPolygon(geometry.getCoordinates());
            polygon = (Polygon) TopologyPreservingSimplifier.simplify(polygon, 0.4);
            newGeomsList.add(polygon);
        }
        fc = FeatureUtilities.featureCollectionFromGeometry(buildingsFC.getBounds().getCoordinateReferenceSystem(),
                newGeomsList.toArray(GeometryUtilities.TYPE_POLYGON));
        return fc;
    }

    public static void main( String[] args ) throws Exception {
        new G02_BuildingSmootherAndRemover();

    }
}
