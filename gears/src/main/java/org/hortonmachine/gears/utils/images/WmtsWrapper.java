package org.hortonmachine.gears.utils.images;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.ServiceException;
import org.geotools.ows.wms.CRSEnvelope;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.StyleImpl;
import org.geotools.ows.wmts.WebMapTileServer;
import org.geotools.ows.wmts.model.WMTSCapabilities;
import org.geotools.ows.wmts.model.WMTSLayer;
import org.geotools.ows.wmts.request.GetTileRequest;

public class WmtsWrapper {

    private WebMapTileServer wmts = null;
    private WMTSCapabilities capabilities;

    public WmtsWrapper( String urlString ) {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            // will not happen
        }

        try {
            wmts = new WebMapTileServer(url);
        } catch (IOException e) {
            // There was an error communicating with the server
            // For example, the server is down
            e.printStackTrace();
        } catch (ServiceException e) {
            // The server returned a ServiceException (unusual in this case)
            e.printStackTrace();
        }
    }

    public WMTSCapabilities getCapabilities() {
        if (capabilities == null)
            capabilities = wmts.getCapabilities();
        return capabilities;
    }

    public WMTSLayer[] getLayers() {
        List<WMTSLayer> layerList = capabilities.getLayerList();
        WMTSLayer[] layers = layerList.toArray(new WMTSLayer[0]);
        return layers;
    }

    public Layer getLayer( String name ) {
        Layer[] layers = getLayers();
        for( Layer layer : layers ) {
            if (layer.getName().equals(name)) {
                return layer;
            }
        }
        return null;
    }

    public void printInfo() {
        WMTSCapabilities capabilities = getCapabilities();
        String serverName = capabilities.getService().getName();
        String serverTitle = capabilities.getService().getTitle();
        System.out.println("Capabilities retrieved from server: " + serverName + " (" + serverTitle + ")");

        Layer[] layers = getLayers();
        for( int i = 0; i < layers.length; i++ ) {
            // Print layer info
            System.out.println("=================================================================");
            System.out.println("Layer: (" + i + ")" + layers[i].getName());
            System.out.println("       " + layers[i].getTitle());
            System.out.println("       " + layers[i].getChildren().length);
            Map<String, CRSEnvelope> boundingBoxes = layers[i].getBoundingBoxes();
            System.out.println("-----------------------------------------------------------------");
            System.out.println("Supported CRS and bounds: ");
            for( Entry<String, CRSEnvelope> item : boundingBoxes.entrySet() ) {
                String epsg = item.getKey();
                CRSEnvelope crsEnv = item.getValue();
                System.out.println("\t" + epsg + ": " + crsEnv);
            }
            System.out.println("-----------------------------------------------------------------");
            // Get layer styles
            List<StyleImpl> styles = layers[i].getStyles();
            System.out.println("Styles:");
            for( StyleImpl elem : styles ) {
                // Print style info
                System.out.println("  Name:" + elem.getName() + "  Title:" + elem.getTitle());
            }
        }
    }

    public BufferedImage getImage( WMTSLayer layer, String format, String srs, int imageWidth, int imageHeight,
            ReferencedEnvelope bbox, String version ) throws Exception {

//        System.setProperty("org.geotools.referencing.forceXY", "true");

        GetTileRequest request = wmts.createGetTileRequest();
        request.setLayer(layer);

//        if (srs == null || srs.equalsIgnoreCase("EPSG:4326"))
//            srs = "CRS:84";
//        if (imageWidth < 0) {
//            imageWidth = 256;
//        }
//        if (imageHeight < 0) {
//            imageHeight = 256;
//        }
//        if (format == null)
//            format = "image/png";
//        request.setFormat(format);
//        request.setDimensions(imageWidth, imageHeight);
//        request.setTransparent(true);
//        request.setSRS(srs);
//        if (version != null)
//            request.setVersion(version);
//
//        request.setBBox(bbox);
//        request.addLayer(layer);
//
//        GetMapResponse response = (GetMapResponse) wmts.issueRequest(request);
//        BufferedImage image = ImageIO.read(response.getInputStream());
//        URL finalURL = request.getFinalURL();
//        System.out.println(finalURL);
//
//        // BBOX=xmin,ymin,xmax,ymax NON-FLIPPED
//        // BBOX=ymin,xmin,ymax,xmax FLIPPED
//        if (image == null) {
//            System.err.println("no image for found for: " + finalURL);
//        }
//        return image;

        return null;
    }

    public static void main( String[] args ) throws Exception {
        String url = "https://idt2.regione.veneto.it/gwc/service/wmts?request=GetCapabilities";
        String wmscode = "EPSG:4326";
        int width = 1000;
        int height = 1000;
        String outputImage = "/home/hydrologis/TMP/VIENNA/wms.png";

        WmtsWrapper ww = new WmtsWrapper(url);
        ww.printInfo();
        WMTSLayer[] layers = ww.getLayers();
        for( WMTSLayer layer : layers ) {
            String name = layer.getName();
//            if (name.equals("Digitales_Oberflaechenmodell_DOM")) {
//                CRSEnvelope latLonBoundingBox = layer.getLatLonBoundingBox();
//                double w = latLonBoundingBox.getMinX();
//                double e = latLonBoundingBox.getMaxX();
//                double s = latLonBoundingBox.getMinY();
//                double n = latLonBoundingBox.getMaxY();
//                ReferencedEnvelope env = new ReferencedEnvelope(w, e, s, n, CRS.decode("EPSG:4326"));
//
//                ReferencedEnvelope wmsEnv = env.transform(CRS.decode(wmscode), false);
//                BufferedImage image = ww.getImage(layer, null, wmscode, width, height, wmsEnv, null);
//                String format = "jpg";
//                if (outputImage.toLowerCase().endsWith("png")) {
//                    format = "png";
//                }
//                ImageIO.write(image, format, new File(outputImage));
//                break;
//            }
        }
    }
//    public static void main( String[] args ) throws Exception {
//        String url = "https://gis.stmk.gv.at/arcgis/services/OGD/als_schummerung/MapServer/WmsServer?request=GetCapabilities&service=WMS";
//        String wmscode = "EPSG:4326";
//        int width = 1000;
//        int height = 1000;
//        String outputImage = "/home/hydrologis/TMP/VIENNA/wms.png";
//        
//        WmsWrapper ww = new WmsWrapper(url);
//        ww.printInfo();
//        Layer[] layers = ww.getLayers();
//        for( Layer layer : layers ) {
//            String name = layer.getName();
//            if (name.equals("Digitales_Oberflaechenmodell_DOM")) {
//                CRSEnvelope latLonBoundingBox = layer.getLatLonBoundingBox();
//                double w = latLonBoundingBox.getMinX();
//                double e = latLonBoundingBox.getMaxX();
//                double s = latLonBoundingBox.getMinY();
//                double n = latLonBoundingBox.getMaxY();
//                ReferencedEnvelope env = new ReferencedEnvelope(w, e, s, n, CRS.decode("EPSG:4326"));
//                
//                ReferencedEnvelope wmsEnv = env.transform(CRS.decode(wmscode), false);
//                BufferedImage image = ww.getImage(layer, null, wmscode, width, height, wmsEnv, null);
//                String format = "jpg";
//                if (outputImage.toLowerCase().endsWith("png")) {
//                    format = "png";
//                }
//                ImageIO.write(image, format, new File(outputImage));
//                break;
//            }
//        }
//    }
}
