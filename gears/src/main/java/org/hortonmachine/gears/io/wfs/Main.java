package org.hortonmachine.gears.io.wfs;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.locationtech.jts.geom.Envelope;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class Main {
    public static void main(String[] args) throws Exception {
        final String wfsUrl =
            "https://visualizador.ideam.gov.co/gisserver/services/Vulnerabilidad_Susceptibilidad_Ambiental/MapServer/WFSServer?service=WFS&request=GetCapabilities";
        final String desiredLayerName = "Categorizacion_de_SZH_por_Evaluacion_Integrada_ENA_2014";
        
        Wfs wfs = new Wfs(wfsUrl, desiredLayerName);
    	// wfs.forceVersion("1.0.0");
        wfs.forceNormalizeGeometryName();
        wfs.connect();
        try {
        	System.out.println("Version: " + wfs.getVersion());
        	
        	List<String> typeNames = wfs.getTypeNames();
            System.out.println("Available type names:");
            for (String tn : typeNames) {
                System.out.println("  - " + tn);
            }

            SimpleFeatureType schema = wfs.getSimpleFeatureType();
            CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
            System.out.println("Schema name: " + schema.getTypeName());
            System.out.println("Attributes:");
            schema.getAttributeDescriptors().forEach(ad ->
                System.out.println("  - " + ad.getLocalName() + " : " + ad.getType().getBinding().getSimpleName()));
            System.out.println("CRS: " + (crs != null ? CRS.toSRS(crs) : "unknown"));
            System.out.println("Bounds (from server): " + wfs.getBounds());

            Envelope env = new Envelope(-76, -75., 8.0, 9.0);
            SimpleFeatureCollection fc = wfs.getFeatureCollection(env);
            
            OmsVectorWriter.writeVector("/home/hydrologis/TMP/KLAB/wfs_test/test.shp", fc);

        } finally {
            wfs.close();
        }
    }
}
