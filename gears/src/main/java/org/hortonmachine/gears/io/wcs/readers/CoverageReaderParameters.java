package org.hortonmachine.gears.io.wcs.readers;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.io.wcs.IWebCoverageService;
import org.hortonmachine.gears.io.wcs.WcsUtils;
import org.hortonmachine.gears.io.wcs.wcs201.WebCoverageService201;
import org.hortonmachine.gears.io.wcs.wcs201.models.DescribeCoverage;
import org.hortonmachine.gears.io.wcs.wcs201.models.ServiceMetadata;
import org.hortonmachine.gears.utils.CrsUtilities;

/**
 * A builder class for getCoverage call parameters.
 */
public class CoverageReaderParameters {
    public String identifier = null;
    private String wcsVersion = null;
    public String method = "Get";
    public ReferencedEnvelope bbox = null;
    public String format = null;
    public Double scaleFactor = null;
    public int[] rowsCols = null;
    public Integer outputSrid = null;


    private IWebCoverageService service;


    /**
     * Constructs a new instance of CoverageReaderParameters with the given parameters.
     *
     * @param service the web coverage service to use for reading the coverage
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
     * <p>Note that if the requested envelope is not in the same CRS as the data, it will be transformed to the data CRS.</p>
     * 
     * @param requestedEnvelope the envelope requested.
     * @return the builder instance.
     */
    public CoverageReaderParameters bbox(ReferencedEnvelope requestedEnvelope ){
        bbox = requestedEnvelope;
        return this;
    }

    /**
     * Set the format of the output coverage.
     * 
     * <p>{@link ServiceMetadata#getSupportedFormats()} can be used to get the list of supported formats.</p>
     * <p>{@link DescribeCoverage#nativeFormat} can be used to get the native format of the coverage.</p>
     * 
     * @param format the format of the output.
     * @return the builder instance.
     */
    public CoverageReaderParameters format(String format){
        this.format = format;
        return this;
    }

    /**
     * Set a scalefactor to be applied to the coverage.
     * 
     * <p>Scalefactor is a double value that is applied to the coverage. It is used to reduce the size of the output coverage.</p>
     * <p>Scalefactor is supported by versions: 2.0.1.</p>
     * <p>A scalefactor of 0.5 will reduce the size to half and hence double the resolution.</p>
     * <p>Scalefactor is not a parameter that gives fine grained control over the output coverage. If you need more control, use {@link #rowsCols(int[])}.</p>
     * 
     * @param scaleFactor the scalefactor to be applied.
     * @return the builder instance.
     */
    public CoverageReaderParameters scaleFactor(Double scaleFactor){
        this.scaleFactor = scaleFactor;
        return this;
    }

    /**
     * Define the number of rows (height) and columns (width) of the output coverage.
     * 
     * <p>Setting this parameter will override the scalefactor parameter.</p>
     * 
     * @param rowsCols an array containing the number of rows and columns.
     * @return the builder instance.
     */
    public CoverageReaderParameters rowsCols(int[] rowsCols){
        this.rowsCols = rowsCols;
        return this;
    }

    /**
     * Set the output CRS of the coverage.
     * 
     * @param outputSrid the srid of the output CRS.
     * @return the builder instance.
     */
    public CoverageReaderParameters outputSrid(Integer outputSrid){
        this.outputSrid = outputSrid;
        return this;
    }

    /**
     * Constructs a URL string for a WCS GetCoverage request based on the parameters set in this object.
     * 
     * @param additionalParams a HashMap containing additional parameters to include in the URL
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
        } else if (this.wcsVersion.equals("1.1.0")) {
            url += "&identifier=" + this.identifier;
            if (this.bbox != null)
                url += "&boundingbox=" + this.bbox;

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

    private String build201Url(String url) throws Exception {
        WebCoverageService201 wcs = (WebCoverageService201) service;
        url += "&COVERAGEID=" + this.identifier;
        if (this.format != null)
            url += "&format=" + this.format;
        if (this.bbox != null){
            // need to get axis labels
            DescribeCoverage describeCoverage = wcs.getDescribeCoverage(this.identifier);
            ReferencedEnvelope dataEnvelope = describeCoverage.envelope;
            ReferencedEnvelope requestEnvelope = bbox;
            if(!CRS.equalsIgnoreMetadata(dataEnvelope.getCoordinateReferenceSystem(), bbox.getCoordinateReferenceSystem())){
                requestEnvelope = bbox.transform(dataEnvelope.getCoordinateReferenceSystem(), true);
            }

            String[] axisLabels = describeCoverage.gridAxisLabels;
            String[] lonLatLabelsOrdered = WcsUtils.orderLabels(axisLabels);
            url += "&subset=" + lonLatLabelsOrdered[0] + "(" + requestEnvelope.getMinX() + "," + requestEnvelope.getMaxX() + ")";
            url += "&subset=" + lonLatLabelsOrdered[1] + "(" + requestEnvelope.getMinY() + "," + requestEnvelope.getMaxY() + ")";

            Integer usedSrid = CrsUtilities.getSrid(requestEnvelope.getCoordinateReferenceSystem());
            if (usedSrid != null){
                String sridNs = WcsUtils.nsCRS_WCS2(usedSrid);
                url += "&SUBSETTINGCRS=" + sridNs;
            }

        }
        // scalefactor and row/cols excludes each other, give priority to row/cols
        boolean hasRowCols = false;
        if (this.rowsCols != null){
            // we need to check if the service supports scaling
            boolean supportsScaling = wcs.getCapabilities().getIdentification().supportsScaling();
            if(supportsScaling){
                DescribeCoverage describeCoverage = wcs.getDescribeCoverage(this.identifier);
                String[] gridAxisLabels = describeCoverage.gridAxisLabels;
                int[] lonLatPositions = WcsUtils.getLonLatPositions(gridAxisLabels);

                String[] axisLabels = describeCoverage.axisLabels;
                url += "&SCALESIZE=" + axisLabels[lonLatPositions[0]] + "(" + this.rowsCols[1] + "),"+ axisLabels[lonLatPositions[1]] + "(" + this.rowsCols[0] + ")";
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
