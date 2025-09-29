package org.hortonmachine.gears.io.stac;

import java.io.InputStream;
import java.util.Iterator;

import com.github.davidmoten.aws.lw.client.Client;
import it.geosolutions.imageioimpl.plugins.cog.*;
import org.apache.commons.io.FilenameUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.fasterxml.jackson.databind.JsonNode;

import it.geosolutions.imageio.core.BasicAuthURI;
import it.geosolutions.imageio.plugins.cog.CogImageReadParam;

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

    public HMStacAsset(String id, JsonNode assetNode) {
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
            assetUrl = assetNode.get("href").textValue();

            JsonNode rasterBandNode = assetNode.get("raster:bands");
            if (rasterBandNode != null && !rasterBandNode.isEmpty()) {
                Iterator<JsonNode> rbIterator = rasterBandNode.elements();
                while (rbIterator.hasNext()) {
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
     * @param client an optional client in case of S3 authentication
     * @return the read raster from the asset's url
     * @throws Exception 
     */
    public GridCoverage2D readRaster( RegionMap region, String user, String password, Client client ) throws Exception {
        BasicAuthURI cogUri = new BasicAuthURI(assetUrl, false);
        if (user != null && password != null) {
            cogUri.setUser(user);
            cogUri.setPassword(password);
        }
        GeoTiffReader reader;
        if (assetUrl.startsWith("s3://")) { // TODO manage multiple S3 servers
            InputStream inputProvider = readS3Raster(cogUri, client);
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
        }        CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();

        GeneralParameterValue[] generalParameter = null;
        if (region != null) {
            generalParameter = CoverageUtilities.createGridGeometryGeneralParameter(region, crs);
        }
        GridCoverage2D coverage = reader.read(generalParameter);
        return coverage;
    }

    public InputStream readS3Raster(BasicAuthURI cogUri, Client client ) {
        String[] bucketAndObject = assetUrl.split("://")[1].split("/", 2);

        return client.path(bucketAndObject[0], bucketAndObject[1]).responseInputStream();
    }
    public GridCoverage2D readRaster( RegionMap region ) throws Exception {
        return readRaster(region, null, null, null);
    }

    public GridCoverage2D readRaster( RegionMap region, String user, String password ) throws Exception {
        return readRaster(region, user, password, null);
    }

    public GridCoverage2D readRaster( RegionMap region, Client client ) throws Exception {
        return readRaster(region, null, null, client);
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
