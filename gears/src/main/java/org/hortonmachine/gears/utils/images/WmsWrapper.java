package org.hortonmachine.gears.utils.images;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;

import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.StyleImpl;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.WMSUtils;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.data.wms.response.GetMapResponse;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.ServiceException;

public class WmsWrapper {

    private WebMapServer wms = null;
    private WMSCapabilities capabilities;

    public WmsWrapper( String urlString ) {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            // will not happen
        }

        try {
            wms = new WebMapServer(url);
        } catch (IOException e) {
            // There was an error communicating with the server
            // For example, the server is down
            e.printStackTrace();
        } catch (ServiceException e) {
            // The server returned a ServiceException (unusual in this case)
            e.printStackTrace();
        }
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
            System.out.println("Layer: (" + i + ")" + layers[i].getName());
            System.out.println("       " + layers[i].getTitle());
            System.out.println("       " + layers[i].getChildren().length);
            System.out.println("       " + layers[i].getBoundingBoxes());
            CRSEnvelope env = layers[i].getLatLonBoundingBox();
            System.out.println("       " + env.getLowerCorner() + " x " + env.getUpperCorner());

            // Get layer styles
            List<StyleImpl> styles = layers[i].getStyles();
            for( StyleImpl elem : styles ) {
                // Print style info
                System.out.println("Style:");
                System.out.println("  Name:" + elem.getName());
                System.out.println("  Title:" + elem.getTitle());
            }
        }
    }

    public BufferedImage getImage( Layer layer, String format, String srs, int imageWidth, int imageHeight,
            ReferencedEnvelope bbox, String version ) throws Exception {

        GetMapRequest request = wms.createGetMapRequest();

        if (format == null)
            format = "image/png";
        if (srs == null)
            srs = "EPSG:4326";
        if (imageWidth < 0) {
            imageWidth = 256;
        }
        if (imageHeight < 0) {
            imageHeight = 256;
        }
        if (version == null) {
            version = "1.1.1";
        }
        request.setFormat(format);
        request.setDimensions(imageWidth, imageHeight);
        request.setTransparent(true);
        request.setSRS(srs);
        request.setVersion(version);

        request.setBBox(bbox);
        request.addLayer(layer);

        GetMapResponse response = (GetMapResponse) wms.issueRequest(request);
        BufferedImage image = ImageIO.read(response.getInputStream());
        if (image == null) {
            URL finalURL = request.getFinalURL();
            System.err.println("no image for: " + finalURL);
        }
        return image;
    }
}
