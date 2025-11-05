package org.hortonmachine.gears.io.wcs.readers;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gears.io.wcs.ICoverageSummary;
import org.hortonmachine.gears.io.wcs.IDescribeCoverage;
import org.hortonmachine.gears.io.wcs.IWebCoverageService;
import org.hortonmachine.gears.io.wcs.WcsUtils;
import org.hortonmachine.gears.io.wcs.wcs100.WebCoverageService100;
import org.hortonmachine.gears.io.wcs.wcs111.WebCoverageService111;
import org.hortonmachine.gears.io.wcs.wcs201.WebCoverageService201;
import org.locationtech.jts.geom.Envelope;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;

/**
 * A builder class for getCoverage call parameters.
 */
public class CoverageReaderParameters {
    public String identifier = null;
    private String wcsVersion = null;
    public String method = "Get";
    public Envelope requestedEnvelope = null;
    public Integer requestedEnvelopeSrid = null;
    public String format = null;
    public Double scaleFactor = null;
    public int[] rowsCols = null;
    public Integer outputSrid = null;
    public boolean useExtendedAxisUrl = false;

    private IWebCoverageService service;

    /**
     * Constructs a new instance of CoverageReaderParameters with the given
     * parameters.
     *
     * @param service    the web coverage service to use for reading the coverage
     * @param coverageId the identifier of the coverage to read
     * @throws Exception if an error occurs while constructing the parameters
     */
    public CoverageReaderParameters(final IWebCoverageService service, String coverageId) throws Exception {
        this.service = service;
        this.wcsVersion = service.getVersion();
        this.identifier = coverageId;
    }

    /**
     * Set a request bounding box.
     * 
     * <p>
     * Note that if the requested envelope is not in the same CRS as the data, it
     * will be transformed to the data CRS.
     * </p>
     * 
     * @param requestedEnvelope the envelope requested.
     * @return the builder instance.
     */
    public CoverageReaderParameters bbox(Envelope requestedEnvelope, Integer requestedEnvelopeSrid) {
        this.requestedEnvelope = requestedEnvelope;
        this.requestedEnvelopeSrid = requestedEnvelopeSrid;
        return this;
    }

    /**
     * Set the format of the output coverage.
     * 
     * <p>
     * Supported by versions: 1.1.1, 2.0.1.
     * </p>
     * 
     * @param format the format of the output.
     * @return the builder instance.
     */
    public CoverageReaderParameters format(String format) {
        this.format = format;
        return this;
    }

    /**
     * Set a scalefactor to be applied to the coverage.
     * 
     * <p>
     * Scalefactor is a double value that is applied to the coverage. It is used to
     * reduce the size of the output coverage.
     * </p>
     * <p>
     * Scalefactor is supported by versions: 2.0.1.
     * </p>
     * <p>
     * A scalefactor of 0.5 will reduce the size to half and hence double the
     * resolution.
     * </p>
     * <p>
     * Scalefactor is not a parameter that gives fine grained control over the
     * output coverage. If you need more control, use {@link #rowsCols(int[])}.
     * </p>
     * 
     * @param scaleFactor the scalefactor to be applied.
     * @return the builder instance.
     */
    public CoverageReaderParameters scaleFactor(Double scaleFactor) {
        this.scaleFactor = scaleFactor;
        return this;
    }

    /**
     * Define the number of rows (height) and columns (width) of the output
     * coverage.
     * 
     * <p>
     * Setting this parameter will override the scalefactor parameter.
     * </p>
     * <p>
     * Supported by versions: 1.1.1, 2.0.1.
     * </p>
     * 
     * @param rowsCols an array containing the number of rows and columns.
     * @return the builder instance.
     */
    public CoverageReaderParameters rowsCols(int rows, int cols) {
        this.rowsCols = new int[]{rows, cols};
        return this;
    }

    /**
     * Sets whether to use the extended axis URL syntax for GridAxes.
     * 
     * <p>Some mapservers require the axis label to be specified with the 
     * url syntax, e.g. "http://www.opengis.net/def/axis/OGC/1/j" instead of just 
     * j.
     *
     * @param useExtendedAxisUrl whether to use the extended axis URL
     * @return this CoverageReaderParameters instance
     */
    public CoverageReaderParameters useExtendedAxisUrl(boolean useExtendedAxisUrl) {
        this.useExtendedAxisUrl = useExtendedAxisUrl;
        return this;
    }

    /**
     * Set the output CRS of the coverage.
     * 
     * <p>
     * Supported by versions: 2.0.1.
     * </p>
     * 
     * @param outputSrid the srid of the output CRS.
     * @return the builder instance.
     */
    public CoverageReaderParameters outputSrid(Integer outputSrid) {
        this.outputSrid = outputSrid;
        return this;
    }

    /**
     * Constructs a URL string for a WCS GetCoverage request based on the parameters
     * set in this object.
     * 
     * @param additionalParams a HashMap containing additional parameters to include
     *                         in the URL
     * @return a URL string for a WCS GetCoverage request
     * @throws Exception if something goes wrong while constructing the URL
     */
    public String toUrl(HashMap<String, String> additionalParams) throws Exception {
        String url = "";
        url += "service=WCS";
        url += "&version=" + this.wcsVersion;
        url += "&request=GetCoverage";
        if (this.wcsVersion.equals("2.0.1")) {
            url = build201Url(url);
        } else if (this.wcsVersion.equals("1.1.2") || this.wcsVersion.equals("1.1.1") || this.wcsVersion.equals("1.1.0")) {
            url = build111(url);
        } else if (this.wcsVersion.equals("1.0.0")) {
            url = build100(url);
        } else {
            throw new UnsupportedEncodingException("Unsupported WCS version: " + this.wcsVersion);
        }
        if (additionalParams != null) {
            for (String key : additionalParams.keySet()) {
                url += "&" + key + "=" + additionalParams.get(key);
            }
        }
        return url;

    }

    private String build100(String url)
            throws Exception, NoSuchAuthorityCodeException, FactoryException, TransformException {
        WebCoverageService100 wcs = (WebCoverageService100) service;

        url += "&coverage=" + this.identifier;
        if (this.format != null)
            url += "&format=" + this.format;

        Envelope finalRequestEnvelope = requestedEnvelope;
        Integer finalRequestEnvelopeSrid = requestedEnvelopeSrid;
        ICoverageSummary coverageSummary = wcs.getCoverageSummary(this.identifier);
        ReferencedEnvelope dataEnvelopeWgs84 = coverageSummary.getWgs84BoundingBox();
        if (this.requestedEnvelope != null) {
            ReferencedEnvelope requestEnvelopeWgs84 = null;
            // TODO check this next, should not be necessary
            ReferencedEnvelope requestedReferenceEnvelope = new ReferencedEnvelope(requestedEnvelope,
                    CRS.decode("EPSG:" + requestedEnvelopeSrid));
            if (requestedEnvelopeSrid != 4326) {
                requestEnvelopeWgs84 = requestedReferenceEnvelope.transform(DefaultGeographicCRS.WGS84, true);
//                finalRequestEnvelopeSrid = 4326;
            } else {
                requestEnvelopeWgs84 = requestedReferenceEnvelope;
            }
            // if the requested envelope is partially outside the data envelope, we need to
            // clip it
            if (!dataEnvelopeWgs84.contains((Envelope) requestEnvelopeWgs84)) {
                Logger.INSTANCE.w(
                        "Requested envelope is partially outside the data envelope. Clipping requested envelope to data envelope.");
                finalRequestEnvelope = finalRequestEnvelope.intersection(dataEnvelopeWgs84);
            }
        } else {
            // since bbox is mandatory, we use the data envelope
            finalRequestEnvelope = dataEnvelopeWgs84;
            finalRequestEnvelopeSrid = 4326;
        }

        double minx = finalRequestEnvelope.getMinX();
        double miny = finalRequestEnvelope.getMinY();
        double maxx = finalRequestEnvelope.getMaxX();
        double maxy = finalRequestEnvelope.getMaxY();
        url += "&BBOX=" + minx + "," + miny + "," + maxx + "," + maxy;

        if (finalRequestEnvelopeSrid != null)
            url += "&CRS=EPSG:" + finalRequestEnvelopeSrid;

        if (this.rowsCols != null) {
            url += "&WIDTH=" + this.rowsCols[1];
            url += "&HEIGHT=" + this.rowsCols[0];
        }
        return url;
    }

    private String build111(String url)
            throws Exception, NoSuchAuthorityCodeException, FactoryException, TransformException {
        WebCoverageService111 wcs = (WebCoverageService111) service;

        url += "&identifiers=" + this.identifier;
        url += "&identifier=" + this.identifier;
        if (this.format != null)
            url += "&format=" + this.format;

        // BOUNDINGBOX is a mandatory parameter (at least until TIME is not implemented)
        Envelope finalRequestEnvelope = requestedEnvelope;
        ICoverageSummary coverageSummary = wcs.getCoverageSummary(this.identifier);
        ReferencedEnvelope dataEnvelope = coverageSummary.getWgs84BoundingBox();
        if (this.requestedEnvelope != null) {
            if (requestedEnvelopeSrid != 4326) {
                ReferencedEnvelope requestedReferenceEnvelope = new ReferencedEnvelope(requestedEnvelope,
                        CRS.decode("EPSG:" + requestedEnvelopeSrid));
                finalRequestEnvelope = requestedReferenceEnvelope.transform(DefaultGeographicCRS.WGS84, true);
            }
            // if the requested envelope is partially outside the data envelope, we need to
            // clip it
            if (!dataEnvelope.contains(finalRequestEnvelope)) {
                Logger.INSTANCE.w(
                        "Requested envelope is partially outside the data envelope. Clipping requested envelope to data envelope.");
                finalRequestEnvelope = finalRequestEnvelope.intersection(dataEnvelope);
            }
        } else {
            // since bbox is mandatory, we use the data envelope
            finalRequestEnvelope = dataEnvelope;
        }

        double minx = finalRequestEnvelope.getMinX();
        double miny = finalRequestEnvelope.getMinY();
        double maxx = finalRequestEnvelope.getMaxX();
        double maxy = finalRequestEnvelope.getMaxY();
        url += "&BOUNDINGBOX=" + minx + "," + miny + "," + maxx + "," + maxy + ",urn:ogc:def:crs:EPSG::4326";

        if (this.requestedEnvelopeSrid != null)
            url += "&CRS=EPSG:" + this.requestedEnvelopeSrid;

        if (this.rowsCols != null) {
            url += "&WIDTH=" + this.rowsCols[1];
            url += "&HEIGHT=" + this.rowsCols[0];
        }
        return url;
    }

    private String build201Url(String url) throws Exception {
        WebCoverageService201 wcs = (WebCoverageService201) service;
        url += "&COVERAGEID=" + this.identifier;
        if (this.format != null)
            url += "&format=" + this.format;
        IDescribeCoverage describeCoverage = wcs.getDescribeCoverage(this.identifier);
        if (this.requestedEnvelope == null) {
            ICoverageSummary coverageSummary = wcs.getCoverageSummary(this.identifier);
            Envelope boundingBox = coverageSummary.getBoundingBox();
            if (boundingBox != null) {
                this.requestedEnvelope = boundingBox;
            } else {
                this.requestedEnvelope = describeCoverage.getCoverageEnvelope();
            }

            Integer boundingBoxSrid = coverageSummary.getBoundingBoxSrid();
            if (boundingBoxSrid != null) {
                this.requestedEnvelopeSrid = boundingBoxSrid;
            } else {
                this.requestedEnvelopeSrid = describeCoverage.getCoverageEnvelopeSrid();
            }
        }
        if (this.requestedEnvelope != null) {
            // Envelope dataEnvelope = describeCoverage.getCoverageEnvelope();
            Envelope finalRequestEnvelope = requestedEnvelope;
            // if (describeCoverage.getCoverageEnvelopeSrid() != null
            // && requestedEnvelopeSrid != describeCoverage.getCoverageEnvelopeSrid()) {
            // ReferencedEnvelope requestedReferenceEnvelope = new
            // ReferencedEnvelope(requestedEnvelope,
            // CRS.decode("EPSG:" + requestedEnvelopeSrid));
            // CoordinateReferenceSystem finalRequestCrs = CRS
            // .decode("EPSG:" + describeCoverage.getCoverageEnvelopeSrid());
            // finalRequestEnvelope = requestedReferenceEnvelope.transform(finalRequestCrs,
            // true);

            // String sridNs =
            // WcsUtils.nsCRS_WCS2(describeCoverage.getCoverageEnvelopeSrid());
            // url += "&SUBSETTINGCRS=" + sridNs;
            // }
            String sridNs = WcsUtils.nsCRS_WCS2(requestedEnvelopeSrid);
            url += "&SUBSETTINGCRS=" + sridNs;

            // if the requested envelope is partially outside the data envelope, we need to
            // clip it
            // if (!dataEnvelope.contains(finalRequestEnvelope)) {
            // Logger.INSTANCE.w(
            // "Requested envelope is partially outside the data envelope. Clipping
            // requested envelope to data envelope.");
            // finalRequestEnvelope = finalRequestEnvelope.intersection(dataEnvelope);
            // }

            // need to get axis labels to create a subset properly
            String[] axisLabels = describeCoverage.getWorldAxisLabels();
            String[] lonLatLabelsOrdered = WcsUtils.orderLabels(axisLabels);
            url += "&SUBSET=" + lonLatLabelsOrdered[0] + "(" + finalRequestEnvelope.getMinX() + ","
                    + finalRequestEnvelope.getMaxX() + ")";
            url += "&SUBSET=" + lonLatLabelsOrdered[1] + "(" + finalRequestEnvelope.getMinY() + ","
                    + finalRequestEnvelope.getMaxY() + ")";

        }
        // scalefactor and row/cols excludes each other, give priority to row/cols
        boolean hasRowCols = false;
        if (this.rowsCols != null) {
            // we need to check if the service supports scaling
            boolean supportsScaling = wcs.getCapabilities().getIdentification().supportsScaling();
            if (supportsScaling) {
                if (describeCoverage == null)
                    describeCoverage = wcs.getDescribeCoverage(this.identifier);
                String[] worldAxisLabels = describeCoverage.getWorldAxisLabels();
                int[] lonLatPositions = WcsUtils.getLonLatPositions(worldAxisLabels);

                String[] gridAxisLabels = describeCoverage.getGridAxisLabels();
                String firstAxis = gridAxisLabels[lonLatPositions[0]];
                String secondAxis = gridAxisLabels[lonLatPositions[1]];
                if (this.useExtendedAxisUrl) {
                    firstAxis = WcsUtils.nsGRIDAXIS_WCS2(firstAxis);
                    secondAxis = WcsUtils.nsGRIDAXIS_WCS2(secondAxis);
                }
                url += "&SCALESIZE=" + firstAxis + "(" + this.rowsCols[1] + "),"
                        + secondAxis + "(" + this.rowsCols[0] + ")";
                hasRowCols = true;
            }
        }
        if (this.scaleFactor != null && !hasRowCols)
            url += "&SCALEFACTOR=" + this.scaleFactor;

        if (this.outputSrid != null) {
            String sridNs = WcsUtils.nsCRS_WCS2(this.outputSrid);
            url += "&OUTPUTCRS=" + sridNs;
        }
        return url;
    }

}
