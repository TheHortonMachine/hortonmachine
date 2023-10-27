package org.hortonmachine.gears.io.stac;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.utils.time.ETimeUtilities;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The items from a collection.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("unchecked")
public class HMStacItem {
    private String id;
    private Geometry geometry;
    private SimpleFeature feature;
    private Integer epsg;
    private Date dateCet;
    private Date creationDateCet;

    private HMStacItem() {
    }

    public static HMStacItem fromSimpleFeature( SimpleFeature feature ) throws Exception {
        HMStacItem stacItem = new HMStacItem();
        stacItem.feature = feature;
        Map<Object, Object> userData = feature.getUserData();
        if (userData != null) {
            Map<String, JsonNode> top = (Map<String, JsonNode>) userData.get(GeoJSONReader.TOP_LEVEL_ATTRIBUTES);
            if (top != null) {
                JsonNode idNode = top.get("id");
                if (idNode != null) {
                    stacItem.id = idNode.textValue();
                }
            }
        }
        if (stacItem.id == null) {
            String fid = feature.getID();
            stacItem.id = fid;
        }

        Object datetimeObject = feature.getAttribute("datetime");
        if (datetimeObject != null) {
            String dateCetStr = datetimeObject.toString();
            if (dateCetStr != null) {
                stacItem.dateCet = HMStacUtils.dateFormatter.parse(dateCetStr);
            }
        }
        Object createdObject = feature.getAttribute("created");
        if (createdObject != null) {
            String creationDateCetStr = createdObject.toString();
            if (creationDateCetStr != null) {
                stacItem.creationDateCet = HMStacUtils.dateFormatter.parse(creationDateCetStr);
            }
        }
        stacItem.geometry = (Geometry) feature.getDefaultGeometry();

        Object epsgObj = feature.getAttribute("proj:epsg");
        if (epsgObj instanceof Integer) {
            stacItem.epsg = (Integer) epsgObj;

            CoordinateReferenceSystem geometryCrs = feature.getFeatureType().getCoordinateReferenceSystem();
            CoordinateReferenceSystem itemCRS = CRS.decode("EPSG:" + stacItem.epsg);
            
            if(!CRS.equalsIgnoreMetadata(geometryCrs, itemCRS)) {
                // update geometry with the data crs geometry
                MathTransform transform = CRS.findMathTransform(geometryCrs, itemCRS);
                stacItem.geometry = JTS.transform(stacItem.geometry, transform);
            }
            
        }

        return stacItem;
    }

    public String getId() {
        return id;
    }

    public String getTimestamp() {
        return ETimeUtilities.INSTANCE.TIME_FORMATTER_UTC.format(dateCet);
    }

    public String getCreationTimestamp() {
        return ETimeUtilities.INSTANCE.TIME_FORMATTER_UTC.format(creationDateCet);
    }

    /**
     * @return the data geometry in the data CRS (from {@link #getEpsg()}).
     */
    public Geometry getGeometry() {
        return geometry;
    }

    /**
     * @return the epsg code defined in proj:epsg. 
     */
    public Integer getEpsg() {
        return epsg;
    }

    public List<HMStacAsset> getAssets() {
        List<HMStacAsset> assetsList = new ArrayList<>();
        Map<Object, Object> userData = feature.getUserData();
        if (userData != null) {
            Map<String, JsonNode> top = (Map<String, JsonNode>) userData.get(GeoJSONReader.TOP_LEVEL_ATTRIBUTES);
            if (top != null) {
                ObjectNode assets = (ObjectNode) top.get("assets");

                if (assets != null) {
                    Iterator<JsonNode> assetsIterator = assets.elements();
                    while( assetsIterator.hasNext() ) {
                        JsonNode assetNode = assetsIterator.next();
                        HMStacAsset hmAsset = new HMStacAsset(assetNode);
                        if (hmAsset.isValid()) {
                            assetsList.add(hmAsset);
                        }
                    }
                }
            }
        }
        return assetsList;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("id = " + id).append("\n");
        sb.append("geom = " + geometry).append("\n");
        sb.append("timestamp = " + getTimestamp()).append("\n");
        sb.append("creation timestamp = " + getCreationTimestamp()).append("\n");
        String metadataSummary = getMetadataSummary(feature, "");
        sb.append(metadataSummary);
        return sb.toString();
    }

    public HMStacAsset getAssetForBand( String bandName ) {
        return getAssets().stream().filter(as -> as.getTitle().equals(bandName)).findFirst().get();
    }

    private String getMetadataSummary( SimpleFeature f, String indent ) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent + getAttribute(f, "updated"));
        sb.append(indent + getAttribute(f, "platform"));
        sb.append(indent + getAttribute(f, "constellation"));
        sb.append(indent + getAttribute(f, "proj:epsg"));
        return sb.toString();
    }

    private String getAttribute( SimpleFeature f, String name ) {
        Object attribute = f.getAttribute(name);
        if (attribute != null) {
            return name + " = " + attribute.toString() + "\n";
        }
        return "";
    }

    public static HashMap<String, Object> getAttributesMap( SimpleFeature f ) {
        HashMap<String, Object> map = new HashMap<>();
        Collection<Property> properties = f.getProperties();
        for( Property property : properties ) {
            map.put(property.getName().getLocalPart(), property.getValue());
        }
        return map;
    }

}
