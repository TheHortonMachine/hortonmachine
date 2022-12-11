package org.hortonmachine.gears.io.stac;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geotools.data.geojson.GeoJSONReader;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The items from a collection.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
@SuppressWarnings("unchecked")
public class HMStacItem {
    private String id;
    private String timestamp;
    private Geometry geometry;
    private SimpleFeature feature;
    private Integer epsg;

    HMStacItem( SimpleFeature feature ) throws Exception {
        this.feature = feature;
        Map<String, JsonNode> top = (Map<String, JsonNode>) feature.getUserData().get(GeoJSONReader.TOP_LEVEL_ATTRIBUTES);
        id = top.get("id").textValue();
        String createdDateCet = feature.getAttribute("created").toString();
        Date dateCet = HMStacUtils.dateFormatter.parse(createdDateCet);
        timestamp = HMStacUtils.fileNameDateFormatter.format(dateCet);
        Object epsgObj = feature.getAttribute("proj:epsg");
        if (epsgObj instanceof Integer) {
            epsg = (Integer) epsgObj;
        }
        geometry = (Geometry) feature.getDefaultGeometry();
    }

    public String getId() {
        return id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public Integer getEpsg() {
        return epsg;
    }

    public List<HMStacAsset> getAssets() {
        List<HMStacAsset> assetsList = new ArrayList<>();
        Map<String, JsonNode> top = (Map<String, JsonNode>) feature.getUserData().get(GeoJSONReader.TOP_LEVEL_ATTRIBUTES);
        ObjectNode assets = (ObjectNode) top.get("assets");

        Iterator<JsonNode> assetsIterator = assets.elements();
        while( assetsIterator.hasNext() ) {
            JsonNode assetNode = assetsIterator.next();
            HMStacAsset hmAsset = new HMStacAsset(assetNode);
            if (hmAsset.isValid()) {
                assetsList.add(hmAsset);
            }
        }
        return assetsList;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("id = " + id).append("\n");
        sb.append("geom = " + geometry).append("\n");
        sb.append("timestamp = " + timestamp).append("\n");
        String metadataSummary = getMetadataSummary(feature, "");
        sb.append(metadataSummary);
        return sb.toString();
    }

    private String getMetadataSummary( SimpleFeature f, String indent ) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent + getAttribute(f, "updated"));
        sb.append(indent + getAttribute(f, "datetime"));
        sb.append(indent + getAttribute(f, "platform"));
        sb.append(indent + getAttribute(f, "constellation"));
        sb.append(indent + getAttribute(f, "eo:cloud_cover"));
        sb.append(indent + getAttribute(f, "proj:epsg"));
        sb.append(indent + getAttribute(f, "view:sun_azimuth"));
        sb.append(indent + getAttribute(f, "view:sun_elevation"));
        sb.append(indent + getAttribute(f, "s2:nodata_pixel_percentage"));
        sb.append(indent + getAttribute(f, "s2:cloud_shadow_percentage"));
        sb.append(indent + getAttribute(f, "s2:water_percentage"));
        return sb.toString();
    }

    private String getAttribute( SimpleFeature f, String name ) {
        Object attribute = f.getAttribute(name);
        if (attribute != null) {
            return name + " = " + attribute.toString() + "\n";
        }
        return "";
    }

}
