package org.hortonmachine.nww.utils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hortonmachine.nww.layers.objects.LayerInfo;

import gov.nasa.worldwind.Factory;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.ogc.wms.WMSLayerCapabilities;
import gov.nasa.worldwind.ogc.wms.WMSLayerStyle;
import gov.nasa.worldwind.util.WWUtil;

public class WmsHandler {

    private String[][] wmsPaths;

    public WmsHandler(String[][] wmsPaths) {
        this.wmsPaths = wmsPaths;
    }

    private Object createComponent(WMSCapabilities caps, AVList params) {
        AVList configParams = params.copy(); // Copy to insulate changes from the caller.

        // Some wms servers are slow, so increase the timeouts and limits used by world wind's retrievers.
        configParams.setValue(AVKey.URL_CONNECT_TIMEOUT, 30000);
        configParams.setValue(AVKey.URL_READ_TIMEOUT, 30000);
        configParams.setValue(AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT, 60000);

        try {
            String factoryKey = getFactoryKeyForCapabilities(caps);
            Factory factory = (Factory) WorldWind.createConfigurationComponent(factoryKey);
            return factory.createFromConfigSource(caps, configParams);
        } catch (Exception e) {
            // Ignore the exception, and just return null.
        }

        return null;
    }

    private String getFactoryKeyForCapabilities(WMSCapabilities caps) {
        boolean hasApplicationBilFormat = false;

        Set<String> formats = caps.getImageFormats();
        for (String s : formats) {
            if (s.contains("application/bil")) {
                hasApplicationBilFormat = true;
                break;
            }
        }

        return hasApplicationBilFormat ? AVKey.ELEVATION_MODEL_FACTORY : AVKey.LAYER_FACTORY;
    }

    public List<Layer> getWmsLayers() throws Exception {

        List<Layer> wmsLayersList = new ArrayList<>();

        for (int i = 0; i < wmsPaths.length; i++) {
            String[] data = wmsPaths[i];
            String layerTitle = data[0];
            String layerWMSName = data[1];
            String layerUrl = data[2];

            URL url = new URL(layerUrl);
            WMSCapabilities caps = WMSCapabilities.retrieve(url.toURI());
            caps.parse();
            // Gather up all the named layers and make a world wind layer for each.
            final List<WMSLayerCapabilities> namedLayerCaps = caps.getNamedLayers();
            if (namedLayerCaps == null)
                continue;
            try {
                for (WMSLayerCapabilities lc : namedLayerCaps) {
                    Set<WMSLayerStyle> styles = lc.getStyles();
                    Object component = null;
                    if (styles == null || styles.size() == 0) {
                        LayerInfo layerInfo = createLayerInfo(caps, lc, null);
                        component = createComponent(layerInfo.caps, layerInfo.params);
                    } else {
                        for (WMSLayerStyle style : styles) {
                            LayerInfo layerInfo = createLayerInfo(caps, lc, style);
                            component = createComponent(layerInfo.caps, layerInfo.params);
                        }
                    }
                    if (component instanceof Layer) {
                        Layer layer = (Layer) component;
                        String name = layer.getName();
                        System.out.println(name);
                        if (name.contains(layerWMSName)) {
                            layer.setName(layerTitle);
                            layer.setEnabled(false);
                            wmsLayersList.add(layer);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }

        return wmsLayersList;
    }

    private LayerInfo createLayerInfo(WMSCapabilities caps, WMSLayerCapabilities layerCaps, WMSLayerStyle style) {
        // Create the layer info specified by the layer's capabilities entry and the selected style.

        LayerInfo linfo = new LayerInfo();
        linfo.caps = caps;
        linfo.params = new AVListImpl();
        linfo.params.setValue(AVKey.LAYER_NAMES, layerCaps.getName());
        if (style != null)
            linfo.params.setValue(AVKey.STYLE_NAMES, style.getName());
        String abs = layerCaps.getLayerAbstract();
        if (!WWUtil.isEmpty(abs))
            linfo.params.setValue(AVKey.LAYER_ABSTRACT, abs);

        linfo.params.setValue(AVKey.DISPLAY_NAME, makeTitle(caps, linfo));

        return linfo;
    }

    private String makeTitle(WMSCapabilities caps, LayerInfo layerInfo) {
        String layerNames = layerInfo.params.getStringValue(AVKey.LAYER_NAMES);
        String styleNames = layerInfo.params.getStringValue(AVKey.STYLE_NAMES);
        String[] lNames = layerNames.split(",");
        String[] sNames = styleNames != null ? styleNames.split(",") : null;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lNames.length; i++) {
            if (sb.length() > 0)
                sb.append(", ");

            String layerName = lNames[i];
            WMSLayerCapabilities lc = caps.getLayerByName(layerName);
            String layerTitle = lc.getTitle();
            sb.append(layerTitle != null ? layerTitle : layerName);

            if (sNames == null || sNames.length <= i)
                continue;

            String styleName = sNames[i];
            WMSLayerStyle style = lc.getStyleByName(styleName);
            if (style == null)
                continue;

            sb.append(" : ");
            String styleTitle = style.getTitle();
            sb.append(styleTitle != null ? styleTitle : styleName);
        }

        return sb.toString();
    }

}
