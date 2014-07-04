package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import oms3.annotations.Description;
import oms3.annotations.Out;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.io.vectorwriter.OmsVectorWriter;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;

public class LW08_NetworBufferMergerHolesRemover extends JGTModel implements LWFields {
    
    @Description("The input polygon layer with the inundation polygons.")
    @Out
    public SimpleFeatureCollection inInundationArea = null;

    @Description("The output polygon layer with the merged and without holes inundation polygons.")
    @Out
    public SimpleFeatureCollection outInundationArea = null;



    private void process() {

        List<Geometry> inBufferGeomsList = FeatureUtilities.featureCollectionToGeometriesList(inInundationArea, false, null);

        Geometry union = CascadedPolygonUnion.union(inBufferGeomsList);
        List<Geometry> removedHoles = removeHoles(union);

        SimpleFeatureCollection outMergedAreaFC = FeatureUtilities.featureCollectionFromGeometry(inInundationArea.getBounds()
                .getCoordinateReferenceSystem(), removedHoles.toArray(GeometryUtilities.TYPE_POLYGON));
        
        ((DefaultFeatureCollection) outInundationArea).addAll(outMergedAreaFC);
    }


    /*
     * remove holes in mreged polygons
     */
    private List<Geometry> removeHoles( Geometry cleanPolygon ) {
        ArrayList<Geometry> gl = new ArrayList<Geometry>();
        for( int i = 0; i < cleanPolygon.getNumGeometries(); i++ ) {
            Polygon geometryN = (Polygon) cleanPolygon.getGeometryN(i);
            LineString exteriorRing = geometryN.getExteriorRing();
            Coordinate[] ringCoordinates = exteriorRing.getCoordinates();
            Polygon polygon = gf.createPolygon(ringCoordinates);
            gl.add(polygon);
        }
        return gl;
    }

    public static void main( String[] args ) throws IOException {

        String inInundatedPolyShp = "D:/lavori_tmp/gsoc/floodpolygon.shp";
        String outInundatedPolyShp = "D:/lavori_tmp/gsoc/floodpolygon_merged.shp";
        
        LW08_NetworBufferMergerHolesRemover networkBufferMergerHolesRemover = new LW08_NetworBufferMergerHolesRemover();
        networkBufferMergerHolesRemover.inInundationArea = OmsVectorReader.readVector(inInundatedPolyShp);
        
        networkBufferMergerHolesRemover.process();
        
        SimpleFeatureCollection outInundationAreaFC = networkBufferMergerHolesRemover.outInundationArea;

        OmsVectorWriter.writeVector(outInundatedPolyShp, outInundationAreaFC);
        
        
    }


}
