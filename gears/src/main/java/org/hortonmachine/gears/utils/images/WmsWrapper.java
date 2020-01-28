package org.hortonmachine.gears.utils.images;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.wms.CRSEnvelope;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.StyleImpl;
import org.geotools.ows.wms.WMSCapabilities;
import org.geotools.ows.wms.WMSUtils;
import org.geotools.ows.wms.WebMapServer;
import org.geotools.ows.wms.request.GetMapRequest;
import org.geotools.ows.wms.response.GetMapResponse;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.utils.StringUtilities;

public class WmsWrapper {

    private WebMapServer wms = null;
    private WMSCapabilities capabilities;

    public WmsWrapper( String urlString ) throws Exception {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            // will not happen
        }

        wms = new WebMapServer(url);
    }

    public WMSCapabilities getCapabilities() {
        if (capabilities == null)
            capabilities = wms.getCapabilities();
        return capabilities;
    }

    public Layer[] getLayers() {
        Layer[] layers = WMSUtils.getNamedLayers(capabilities);
        return layers;
    }

    public List<String> getFormats() {
        return getCapabilities().getRequest().getGetMap().getFormats();
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
        WMSCapabilities capabilities = getCapabilities();
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

    public BufferedImage getImage( GetMapRequest request ) throws Exception {
        GetMapResponse response = (GetMapResponse) wms.issueRequest(request);
        InputStream inputStream = response.getInputStream();
        BufferedImage image = ImageIO.read(inputStream);
        return image;
    }

    public String getMessage( GetMapRequest request ) throws Exception {
        GetMapResponse response = (GetMapResponse) wms.issueRequest(request);
        InputStream inputStream = response.getInputStream();
        Scanner scanner = StringUtilities.streamToScanner(inputStream, "\n");
        StringBuilder sb = new StringBuilder();
        while( scanner.hasNext() ) {
            sb.append(scanner.next()).append("\n");
        }
        return sb.toString();
    }

    public URL getUrl( GetMapRequest request ) {
        URL finalURL = request.getFinalURL();
        return finalURL;
    }

    public GetMapRequest getMapRequest( Layer layer, String format, String srs, int imageWidth, int imageHeight,
            ReferencedEnvelope bbox, String version, StyleImpl style ) throws Exception {
        GetMapRequest request = wms.createGetMapRequest();
        if (imageWidth < 0) {
            imageWidth = 256;
        }
        if (imageHeight < 0) {
            imageHeight = 256;
        }
        if (format == null)
            format = "image/png";

        request.setFormat(format);
        request.setDimensions(imageWidth, imageHeight);
        request.setTransparent(true);
        request.setSRS(srs);
        if (version != null)
            request.setVersion(version);

        request.setBBox(bbox);

        if (style != null) {
            request.addLayer(layer, style);
        } else {
            request.addLayer(layer);
        }
        return request;
    }

//    public static void main( String[] args ) throws Exception {
////        String url = "https://idt2-geoserver.regione.veneto.it/geoserver/ows?request=GetCapabilities";
//        String url = "http://ows.dgterritorio.pt/wss/service/ortos2004-2006-wms/guest?language=por&SERVICE=WMS&REQUEST=GetCapabilities";
//        String wmscode = "EPSG:4326";
//        int width = 1000;
//        int height = 1000;
//        String outputImage = "/home/hydrologis/Dropbox/hydrologis/presentazioni/2019_02_gfoss_it/wms.jpg";
//
//        String path = "/home/hydrologis/Dropbox/hydrologis/presentazioni/2019_02_gfoss_it/padova_roi_ll.shp";
//        ReferencedEnvelope wmsEnv = OmsVectorReader.readEnvelope(path);
//
//        WmsWrapper ww = new WmsWrapper(url);
//        ww.printInfo();
//        Layer[] layers = ww.getLayers();
//        for( Layer layer : layers ) {
//            String name = layer.getName();
//            if (name.equals("Ortos2004-2006-RGB")) {
////                CRSEnvelope latLonBoundingBox = layer.getLatLonBoundingBox();
////                double w = latLonBoundingBox.getMinX();
////                double e = latLonBoundingBox.getMaxX();
////                double s = latLonBoundingBox.getMinY();
////                double n = latLonBoundingBox.getMaxY();
////                ReferencedEnvelope env = new ReferencedEnvelope(w, e, s, n, CRS.decode("EPSG:4326"));
////
////                ReferencedEnvelope wmsEnv = env.transform(CRS.decode(wmscode), false);
//                String format = "jpg";
//                if (outputImage.toLowerCase().endsWith("png")) {
//                    format = "png";
//                }
//                BufferedImage image = ww.getImage(layer, "image/jpg", wmscode, width, height, wmsEnv, "1.1.1");
//                ImageIO.write(image, format, new File(outputImage));
//                break;
//            }
//        }
//    }
    public static void main( String[] args ) throws Exception {
        String url = "https://gis.stmk.gv.at/arcgis/services/OGD/als_schummerung/MapServer/WmsServer?request=GetCapabilities&service=WMS";
        String wmscode = "EPSG:4326";
        int width = 1000;
        int height = 1000;
        String outputImage = "/home/hydrologis/TMP/VIENNA/wms.png";

        WmsWrapper ww = new WmsWrapper(url);
        ww.printInfo();
        Layer[] layers = ww.getLayers();
        for( Layer layer : layers ) {
            String name = layer.getName();
            if (name.equals("Digitales_Oberflaechenmodell_DOM")) {
                CRSEnvelope latLonBoundingBox = layer.getLatLonBoundingBox();
                double w = latLonBoundingBox.getMinX();
                double e = latLonBoundingBox.getMaxX();
                double s = latLonBoundingBox.getMinY();
                double n = latLonBoundingBox.getMaxY();
                ReferencedEnvelope env = new ReferencedEnvelope(w, e, s, n, CRS.decode("EPSG:4326"));

                ReferencedEnvelope wmsEnv = env.transform(CRS.decode(wmscode), false);
                BufferedImage image = ww.getImage(ww.getMapRequest(layer, null, wmscode, width, height, wmsEnv, null, null));
                String format = "jpg";
                if (outputImage.toLowerCase().endsWith("png")) {
                    format = "png";
                }
                ImageIO.write(image, format, new File(outputImage));
                break;
            }
        }
    }
}
