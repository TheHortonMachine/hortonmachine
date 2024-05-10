package org.hortonmachine.gears.modules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hortonmachine.gears.io.stac.HMStacAsset;
import org.hortonmachine.gears.utils.HMTestCase;

public class TestStacAsset extends HMTestCase {

    protected void setUp() throws Exception {

    }

    // Asset JSONs obtained from the documentation, reformatted and modified
    // https://github.com/stac-extensions/raster/blob/main/examples/item-sentinel2.json#L177

    public void testCreateValidStacAsset() throws JsonProcessingException {
        String assetJSON = "{\"title\":\"Band 1 (coastal) BOA reflectance\",\"type\":\"image/tiff; application=geotiff; profile=cloud-optimized\",\"roles\":[\"data\"],\"gsd\":60,\"eo:bands\":[{\"name\":\"B01\",\"common_name\":\"coastal\",\"center_wavelength\":0.4439,\"full_width_half_max\":0.027}],\"href\":\"https://sentinel-cogs.s3.us-west-2.amazonaws.com/sentinel-s2-l2a-cogs/33/S/VB/2021/2/S2B_33SVB_20210221_0_L2A/B01.tif\",\"proj:shape\":[1830,1830],\"proj:transform\":[60,0,399960,0,-60,4200000,0,0,1],\"raster:bands\":[{\"data_type\":\"uint16\",\"spatial_resolution\":60,\"bits_per_sample\":15,\"nodata\":0,\"statistics\":{\"minimum\":1,\"maximum\":20567,\"mean\":2339.4759595597,\"stddev\":3026.6973619954,\"valid_percent\":99.83}}]}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(assetJSON);

        HMStacAsset asset = new HMStacAsset("B01", node);

        assertTrue(asset.isValid());
        assertEquals("B01", asset.getId());
        assertEquals("Band 1 (coastal) BOA reflectance", asset.getTitle());
        assertEquals("image/tiff; application=geotiff; profile=cloud-optimized", asset.getType());
        assertEquals("https://sentinel-cogs.s3.us-west-2.amazonaws.com/sentinel-s2-l2a-cogs/33/S/VB/2021/2/S2B_33SVB_20210221_0_L2A/B01.tif", asset.getAssetUrl());
        assertEquals(0.0, asset.getNoValue());
    }

    public void testCreateInvalidStacAssetTypeInformationNotAvailable() throws JsonProcessingException {
        String assetJSON = "{\"title\":\"Band 1 (coastal) BOA reflectance\",\"roles\":[\"data\"],\"gsd\":60,\"eo:bands\":[{\"name\":\"B01\",\"common_name\":\"coastal\",\"center_wavelength\":0.4439,\"full_width_half_max\":0.027}],\"href\":\"https://sentinel-cogs.s3.us-west-2.amazonaws.com/sentinel-s2-l2a-cogs/33/S/VB/2021/2/S2B_33SVB_20210221_0_L2A/B01.tif\",\"proj:shape\":[1830,1830],\"proj:transform\":[60,0,399960,0,-60,4200000,0,0,1],\"raster:bands\":[{\"data_type\":\"uint16\",\"spatial_resolution\":60,\"bits_per_sample\":15,\"nodata\":0,\"statistics\":{\"minimum\":1,\"maximum\":20567,\"mean\":2339.4759595597,\"stddev\":3026.6973619954,\"valid_percent\":99.83}}]}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(assetJSON);

        HMStacAsset asset = new HMStacAsset("B01", node);

        assertFalse(asset.isValid());
        assertEquals("type information not available", asset.getNonValidReason());
    }

    public void testCreateInvalidStacAssetNotACOG() throws JsonProcessingException {
        String assetJSON = "{\"title\":\"Band 1 (coastal) BOA reflectance\",\"type\":\"image/tiff;\",\"roles\":[\"data\"],\"gsd\":60,\"eo:bands\":[{\"name\":\"B01\",\"common_name\":\"coastal\",\"center_wavelength\":0.4439,\"full_width_half_max\":0.027}],\"href\":\"https://sentinel-cogs.s3.us-west-2.amazonaws.com/sentinel-s2-l2a-cogs/33/S/VB/2021/2/S2B_33SVB_20210221_0_L2A/B01.tif\",\"proj:shape\":[1830,1830],\"proj:transform\":[60,0,399960,0,-60,4200000,0,0,1],\"raster:bands\":[{\"data_type\":\"uint16\",\"spatial_resolution\":60,\"bits_per_sample\":15,\"nodata\":0,\"statistics\":{\"minimum\":1,\"maximum\":20567,\"mean\":2339.4759595597,\"stddev\":3026.6973619954,\"valid_percent\":99.83}}]}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(assetJSON);

        HMStacAsset asset = new HMStacAsset("B01", node);

        assertFalse(asset.isValid());
        assertEquals("not a COG", asset.getNonValidReason());
    }

    public void testCreateStacAssetRasterBandsMetadataMissingIsValid() throws JsonProcessingException {
        String assetJSON = "{\"title\":\"Band 1 (coastal) BOA reflectance\",\"type\":\"image/tiff; application=geotiff; profile=cloud-optimized\",\"roles\":[\"data\"],\"gsd\":60,\"eo:bands\":[{\"name\":\"B01\",\"common_name\":\"coastal\",\"center_wavelength\":0.4439,\"full_width_half_max\":0.027}],\"href\":\"https://sentinel-cogs.s3.us-west-2.amazonaws.com/sentinel-s2-l2a-cogs/33/S/VB/2021/2/S2B_33SVB_20210221_0_L2A/B01.tif\",\"proj:shape\":[1830,1830],\"proj:transform\":[60,0,399960,0,-60,4200000,0,0,1],\"raster:bands\":[]}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(assetJSON);

        HMStacAsset asset = new HMStacAsset("B01", node);

        assertTrue(asset.isValid());
    }

}
