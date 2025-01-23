package org.hortonmachine.gears.io.stac;

import com.fasterxml.jackson.databind.JsonNode;
import it.geosolutions.imageio.core.BasicAuthURI;
import it.geosolutions.imageio.plugins.cog.CogImageReadParam;
import it.geosolutions.imageioimpl.plugins.cog.*;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * An asset from a stac item.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class HMStacAsset {

    private String id;
    private String title;
    private String type;
    private String nonValidReason;
    private boolean isValid = true;
    private String assetUrl;
    private double noValue = HMConstants.doubleNovalue;
    private double resolution;

    public HMStacAsset( String id, JsonNode assetNode ) {
        this.id = id;
        JsonNode typeNode = assetNode.get("type");
        if (typeNode != null) {
            type = typeNode.textValue();
            // we only check cloud optimized datasets here
            JsonNode titleNode = assetNode.get("title");
            title = "undefined title";
            if (titleNode != null) {
                title = titleNode.textValue();
            }
            if (HMStacUtils.ACCEPTED_TYPES.contains(type.toLowerCase().replace(" ", ""))) {
                assetUrl = assetNode.get("href").textValue();

                JsonNode rasterBandNode = assetNode.get("raster:bands");
                assetUrl = assetNode.get("href").textValue();
                if (rasterBandNode != null && !rasterBandNode.isEmpty()) {
                    Iterator<JsonNode> rbIterator = rasterBandNode.elements();
                    while( rbIterator.hasNext() ) {
                        JsonNode rbNode = rbIterator.next();
                        JsonNode noValueNode = rbNode.get("nodata");
                        if (noValueNode != null) {
                            noValue = noValueNode.asDouble();
                        }
                        JsonNode resolNode = rbNode.get("spatial_resolution");
                        if (resolNode != null) {
                            resolution = resolNode.asDouble();
                        }
                    }
                }
            } else {
                isValid = false;
                nonValidReason = "not a valid type";
            }
        } else {
            nonValidReason = "type information not available";
            isValid = false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("title = " + title).append("\n");
        sb.append("type = " + type).append("\n");
        sb.append("url = " + assetUrl).append("\n");
        sb.append("isValid = " + isValid).append("\n");
        if (!isValid) {
            sb.append("nonValidReason = " + nonValidReason).append("\n");
        }
        return sb.toString();
    }

    /**
     * Read the asset's coverage into a local raster.
     *
     * @param region and optional region to read from.
     * @param user an optional user in case of authentication.
     * @param password an optional password in case of authentication.
     * @return the read raster from the asset's url.
     * @throws Exception
     */
    public GridCoverage2D readRaster( RegionMap region, String user, String password, S3Client s3Client ) throws Exception {
        BasicAuthURI cogUri = new BasicAuthURI(assetUrl, false);
        if (user != null && password != null) {
            cogUri.setUser(user);
            cogUri.setPassword(password);
        }
        GeoTiffReader reader;
        if (assetUrl.startsWith("s3://")) {
            InputStream inputProvider = readS3Raster(cogUri, s3Client);
            reader = new GeoTiffReader(inputProvider);
        } else {
            if (PlanetaryComputerMicrosoft.isAzureBlob(assetUrl)) {
                String accessibleHref = PlanetaryComputerMicrosoft.getHrefWithToken(assetUrl);
                cogUri = new BasicAuthURI(accessibleHref, false);
            }
            RangeReader rangeReader = new HttpRangeReader(cogUri.getUri(), CogImageReadParam.DEFAULT_HEADER_LENGTH);
            CogSourceSPIProvider inputProvider = new CogSourceSPIProvider(cogUri, new CogImageReaderSpi(),
                    new CogImageInputStreamSpi(), rangeReader.getClass().getName());
            reader = new GeoTiffReader(inputProvider);
        }
        CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();

        GeneralParameterValue[] generalParameter = null;
        if (region != null) {
            generalParameter = CoverageUtilities.createGridGeometryGeneralParameter(region, crs);
        }
        GridCoverage2D coverage = reader.read(generalParameter);
        return coverage;
    }

    public InputStream readS3Raster(BasicAuthURI cogUri, S3Client s3Client ) throws IOException {
        String[] bucketAndObject = assetUrl.split("://")[1].split("/", 2);

        return s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(bucketAndObject[0])
                        .key(bucketAndObject[1])
                        .build()
        );
    }

    public GridCoverage2D readRaster( RegionMap region ) throws Exception {
        return readRaster(region, null, null, null );
    }

    public GridCoverage2D readRaster( RegionMap region, S3Client s3Client ) throws Exception {
        return readRaster(region, null, null, s3Client );
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public boolean isValid() {
        return isValid;
    }

    public String getNonValidReason() {
        return nonValidReason;
    }

    public String getAssetUrl() {
        return assetUrl;
    }

    public double getNoValue() {
        return noValue;
    }

    public double getResolution() {
        return resolution;
    }
}
