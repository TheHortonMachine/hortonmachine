package org.hortonmachine.gears.modules;

import org.geotools.data.geojson.GeoJSONReader;
import org.hortonmachine.gears.io.stac.HMStacItem;
import org.hortonmachine.gears.utils.HMTestCase;
import org.opengis.feature.simple.SimpleFeature;

public class TestStacItem extends HMTestCase {
    public void testStacItemWithDatetime() throws Exception {
        // Simplified JSON with just the basic and dates
        String featureJSON = "{\"id\":\"30T-2020\",\"bbox\":[-6,40,0,48],\"type\":\"Feature\",\"links\":[],\"assets\":{\"data\":{\"href\":\"https://test.net/example.tif\",\"type\":\"image/tiff; application=geotiff; profile=cloud-optimized\",\"roles\":[\"data\"]},\"tilejson\":{\"title\":\"TileJSON with default rendering\",\"href\":\"https://planetarycomputer.microsoft.com/api/data/v1/item/tilejson.json?collection=io-lulc-9-class&item=30T-2020&assets=data&colormap_name=io-lulc-9-class&format=png\",\"type\":\"application/json\",\"roles\":[\"tiles\"]},\"rendered_preview\":{\"title\":\"Rendered preview\",\"rel\":\"preview\",\"href\":\"https://planetarycomputer.microsoft.com/api/data/v1/item/preview.png?collection=io-lulc-9-class&item=30T-2020&assets=data&colormap_name=io-lulc-9-class&format=png\",\"roles\":[\"overview\"],\"type\":\"image/png\"}},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[0,10],[10,10],[10,0],[0,0],[0,10]]]},\"collection\":\"io-lulc-9-class\",\"properties\":{\"proj:bbox\":[243903.78657680738,4427766.335981226,756093.7865768074,5320646.335981226],\"proj:epsg\":32630,\"io:tile_id\":\"30T\",\"proj:shape\":[89288,51219],\"proj:transform\":[10,0,243903.78657680738,0,-10,5320646.335981226],\"io:supercell_id\":\"30T\"," +
                "\"datetime\":\"2020-01-01T00:00:00Z\"}}";
        SimpleFeature feature = GeoJSONReader.parseFeature(featureJSON);

        HMStacItem item = HMStacItem.fromSimpleFeature(feature);

        assertEquals("2020-01-01 00:00:00", item.getTimestamp());
        assertNull(item.getStartTimestamp(), item.getEndTimestamp());
    }


    public void testStacItemWithDateRangeReturnsStartDate() throws Exception {
        // Simplified JSON with just the basic and dates
        String featureJSON = "{\"id\":\"30T-2020\",\"bbox\":[-6,40,0,48],\"type\":\"Feature\",\"links\":[],\"assets\":{\"data\":{\"href\":\"https://test.net/example.tif\",\"type\":\"image/tiff; application=geotiff; profile=cloud-optimized\",\"roles\":[\"data\"]},\"tilejson\":{\"title\":\"TileJSON with default rendering\",\"href\":\"https://planetarycomputer.microsoft.com/api/data/v1/item/tilejson.json?collection=io-lulc-9-class&item=30T-2020&assets=data&colormap_name=io-lulc-9-class&format=png\",\"type\":\"application/json\",\"roles\":[\"tiles\"]},\"rendered_preview\":{\"title\":\"Rendered preview\",\"rel\":\"preview\",\"href\":\"https://planetarycomputer.microsoft.com/api/data/v1/item/preview.png?collection=io-lulc-9-class&item=30T-2020&assets=data&colormap_name=io-lulc-9-class&format=png\",\"roles\":[\"overview\"],\"type\":\"image/png\"}},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[0,10],[10,10],[10,0],[0,0],[0,10]]]},\"collection\":\"io-lulc-9-class\",\"properties\":{\"proj:bbox\":[243903.78657680738,4427766.335981226,756093.7865768074,5320646.335981226],\"proj:epsg\":32630,\"io:tile_id\":\"30T\",\"proj:shape\":[89288,51219],\"proj:transform\":[10,0,243903.78657680738,0,-10,5320646.335981226],\"io:supercell_id\":\"30T\"," +
                "\"datetime\":null," +
                "\"start_datetime\":\"2020-01-01T00:00:00Z\"," +
                "\"end_datetime\":\"2021-01-01T00:00:00Z\"}}";
        SimpleFeature feature = GeoJSONReader.parseFeature(featureJSON);

        HMStacItem item = HMStacItem.fromSimpleFeature(feature);

        assertEquals("2020-01-01 00:00:00", item.getTimestamp(), item.getStartTimestamp());
    }

    public void testStacItemWithSingleAndDateRangeReturnsDateTime() throws Exception {
        // Simplified JSON with just the basic and dates
        String featureJSON = "{\"id\":\"30T-2020\",\"bbox\":[-6,40,0,48],\"type\":\"Feature\",\"links\":[],\"assets\":{\"data\":{\"href\":\"https://test.net/example.tif\",\"type\":\"image/tiff; application=geotiff; profile=cloud-optimized\",\"roles\":[\"data\"]},\"tilejson\":{\"title\":\"TileJSON with default rendering\",\"href\":\"https://planetarycomputer.microsoft.com/api/data/v1/item/tilejson.json?collection=io-lulc-9-class&item=30T-2020&assets=data&colormap_name=io-lulc-9-class&format=png\",\"type\":\"application/json\",\"roles\":[\"tiles\"]},\"rendered_preview\":{\"title\":\"Rendered preview\",\"rel\":\"preview\",\"href\":\"https://planetarycomputer.microsoft.com/api/data/v1/item/preview.png?collection=io-lulc-9-class&item=30T-2020&assets=data&colormap_name=io-lulc-9-class&format=png\",\"roles\":[\"overview\"],\"type\":\"image/png\"}},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[0,10],[10,10],[10,0],[0,0],[0,10]]]},\"collection\":\"io-lulc-9-class\",\"properties\":{\"proj:bbox\":[243903.78657680738,4427766.335981226,756093.7865768074,5320646.335981226],\"proj:epsg\":32630,\"io:tile_id\":\"30T\",\"proj:shape\":[89288,51219],\"proj:transform\":[10,0,243903.78657680738,0,-10,5320646.335981226],\"io:supercell_id\":\"30T\"," +
                "\"datetime\":\"2019-01-01T00:00:00Z\"," +
                "\"start_datetime\":\"2020-01-01T00:00:00Z\"," +
                "\"end_datetime\":\"2021-01-01T00:00:00Z\"}}";
        SimpleFeature feature = GeoJSONReader.parseFeature(featureJSON);

        HMStacItem item = HMStacItem.fromSimpleFeature(feature);

        assertEquals("2019-01-01 00:00:00", item.getTimestamp());
        assertNotSame(item.getTimestamp(), item.getStartTimestamp());
    }

    // This case should not happen according to the STAC documentation
    public void testStacItemWithoutDatetimeReturnsNull() throws Exception {
        // Simplified JSON with just the basic and dates
        String featureJSON = "{\"id\":\"30T-2020\",\"bbox\":[-6,40,0,48],\"type\":\"Feature\",\"links\":[],\"assets\":{\"data\":{\"href\":\"https://test.net/example.tif\",\"type\":\"image/tiff; application=geotiff; profile=cloud-optimized\",\"roles\":[\"data\"]},\"tilejson\":{\"title\":\"TileJSON with default rendering\",\"href\":\"https://planetarycomputer.microsoft.com/api/data/v1/item/tilejson.json?collection=io-lulc-9-class&item=30T-2020&assets=data&colormap_name=io-lulc-9-class&format=png\",\"type\":\"application/json\",\"roles\":[\"tiles\"]},\"rendered_preview\":{\"title\":\"Rendered preview\",\"rel\":\"preview\",\"href\":\"https://planetarycomputer.microsoft.com/api/data/v1/item/preview.png?collection=io-lulc-9-class&item=30T-2020&assets=data&colormap_name=io-lulc-9-class&format=png\",\"roles\":[\"overview\"],\"type\":\"image/png\"}},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[0,10],[10,10],[10,0],[0,0],[0,10]]]},\"collection\":\"io-lulc-9-class\",\"properties\":{\"proj:bbox\":[243903.78657680738,4427766.335981226,756093.7865768074,5320646.335981226],\"proj:epsg\":32630,\"io:tile_id\":\"30T\",\"proj:shape\":[89288,51219],\"proj:transform\":[10,0,243903.78657680738,0,-10,5320646.335981226],\"io:supercell_id\":\"30T\"," +
                "\"datetime\":null}}";
        SimpleFeature feature = GeoJSONReader.parseFeature(featureJSON);

        HMStacItem item = HMStacItem.fromSimpleFeature(feature);

        assertNull(item.getTimestamp());
        assertNull(item.getStartTimestamp(), item.getEndTimestamp());
    }

}
