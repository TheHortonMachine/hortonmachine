package org.hortonmachine.gears.io.stac;

import com.fasterxml.jackson.databind.JsonNode;
import it.geosolutions.imageio.core.BasicAuthURI;
import it.geosolutions.imageio.plugins.cog.CogImageReadParam;
import it.geosolutions.imageioimpl.plugins.cog.*;
import org.apache.commons.io.FilenameUtils;
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
        if (assetNode.has("title")) {
            title = assetNode.get("title").textValue();
        }
        JsonNode typeNode = assetNode.get("type");
        assetUrl = assetNode.get("href").textValue();
        boolean isAcceptedType = false;
        if (typeNode != null) {
            type = typeNode.textValue();
            isAcceptedType = HMStacUtils.ACCEPTED_TYPES.contains(type.toLowerCase().replace(" ", ""));
        } else {
            isAcceptedType = HMStacUtils.ACCEPTED_EXTENSIONS.contains(FilenameUtils.getExtension(assetUrl));
        }
        if (isAcceptedType) {
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
    public GridCoverage2D readRaster( RegionMap region, String user, String password, S3Client s3Client, String s3Endpoint, boolean turnIntoHttp ) throws Exception {
        String url = assetUrl;
        if (assetUrl.startsWith("s3://") && turnIntoHttp) {
            if (s3Endpoint == null) {
                throw new Exception("No S3 endpoint defined.");
            }
            url = convertS3URLToHTTP(assetUrl, s3Endpoint);
        }

        BasicAuthURI cogUri = new BasicAuthURI(url, false);
        if (user != null && password != null) {
            cogUri.setUser(user);
            cogUri.setPassword(password);
        }
        GeoTiffReader reader;
        if (url.startsWith("s3://")) {
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
        return readRaster(region, null, null, null, null, false );
    }

    public GridCoverage2D readRaster( RegionMap region, S3Client s3Client ) throws Exception {
        return readRaster(region, null, null, s3Client, AWS_ENDPOINT, false);
    }

    public GridCoverage2D readRaster( RegionMap region, S3Client s3Client, String s3Endpoint ) throws Exception {
        return readRaster(region, null, null, s3Client, s3Endpoint, true );
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

    final public static String AWS_ENDPOINT = "s3.amazonaws.com";

    /**
     * Checks if the given url is a valid S3 endpoint
     * @param url to be analyzed
     * @return true if it is a valid S3 endpoint
     */
    public static boolean isS3Endpoint(String url) {
        return url.startsWith("s3://");
    }

    /**
     * Translates an S3 URL into to HTTP. It does not take the region into account.
     * Example: "s3://bucket/object" -> "https://bucket.s3.amazonaws.com/object"
     * @param s3url URL for S3 protocol
     * @param s3Server server of the S3 bucket
     * @return The equivalent URL for an HTTP endpoint
     */
    public static String convertS3URLToHTTP(String s3url, String s3Server) throws Exception {
        if (!isS3Endpoint(s3url)) {
            throw new Exception(String.format("The URL %s does not utilize the S3 protocol", s3url));
        }
        String[] bucketAndObject = s3url.split("://")[1].split("/", 2);
        return "https://" + bucketAndObject[0] + "." + s3Server + "/" + bucketAndObject[1];
    }

    /**
     * Translates an S3 URL into to HTTP. It does not take the region into account.
     * The default value for the s3Server is AWS.
     * Example: "s3://bucket/object" -> "https://bucket.s3.amazonaws.com/object"
     * @param s3url URL for S3 protocol
     * @return The equivalent URL for an HTTP endpoint
     */
    public static String convertS3URLToHTTP(String s3url) throws Exception {
        return convertS3URLToHTTP(s3url, AWS_ENDPOINT);
    }

}
