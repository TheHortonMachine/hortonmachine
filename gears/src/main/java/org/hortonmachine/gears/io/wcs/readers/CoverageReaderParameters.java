package org.hortonmachine.gears.io.wcs.readers;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gears.io.wcs.IDescribeCoverage;
import org.hortonmachine.gears.io.wcs.IWebCoverageService;
import org.hortonmachine.gears.io.wcs.WcsUtils;
import org.hortonmachine.gears.io.wcs.wcs201.WebCoverageService201;
import org.hortonmachine.gears.io.wcs.wcs201.models.DescribeCoverage;
import org.hortonmachine.gears.io.wcs.wcs201.models.ServiceMetadata;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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
    public CoverageReaderParameters bbox(Envelope requestedEnvelope, Integer requestedEnvelopeSrid ){
        this.requestedEnvelope = requestedEnvelope;
        this.requestedEnvelopeSrid = requestedEnvelopeSrid;
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
            if (this.requestedEnvelope != null)
                url += "&boundingbox=" + this.requestedEnvelope;

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
        IDescribeCoverage describeCoverage = null;
        if (this.requestedEnvelope != null){
            // need to get axis labels
            describeCoverage = wcs.getDescribeCoverage(this.identifier);
            Envelope dataEnvelope = describeCoverage.getCoverageEnvelope();
            Envelope finalRequestEnvelope = requestedEnvelope;
            if(describeCoverage.getCoverageEnvelopeSrid()!=null && requestedEnvelopeSrid != describeCoverage.getCoverageEnvelopeSrid()){
                ReferencedEnvelope requestedReferenceEnvelope = new ReferencedEnvelope(requestedEnvelope, CRS.decode("EPSG:" + requestedEnvelopeSrid));
                CoordinateReferenceSystem finalRequestCrs = CRS.decode("EPSG:" + describeCoverage.getCoverageEnvelopeSrid());
                finalRequestEnvelope = requestedReferenceEnvelope.transform(finalRequestCrs, true);
                
                String sridNs = WcsUtils.nsCRS_WCS2(describeCoverage.getCoverageEnvelopeSrid());
                url += "&SUBSETTINGCRS=" + sridNs;
            }

            // if the requested envelope is partially outside the data envelope, we need to clip it
            if (!dataEnvelope.contains(finalRequestEnvelope)) {
                Logger.INSTANCE.w("Requested envelope is partially outside the data envelope. Clipping requested envelope to data envelope.");
                finalRequestEnvelope = finalRequestEnvelope.intersection(dataEnvelope);
            }

            String[] axisLabels = describeCoverage.getGridAxisLabels();
            String[] lonLatLabelsOrdered = WcsUtils.orderLabels(axisLabels);
            url += "&subset=" + lonLatLabelsOrdered[0] + "(" + finalRequestEnvelope.getMinX() + "," + finalRequestEnvelope.getMaxX() + ")";
            url += "&subset=" + lonLatLabelsOrdered[1] + "(" + finalRequestEnvelope.getMinY() + "," + finalRequestEnvelope.getMaxY() + ")";


        }
        // scalefactor and row/cols excludes each other, give priority to row/cols
        boolean hasRowCols = false;
        if (this.rowsCols != null){
            // we need to check if the service supports scaling
            boolean supportsScaling = wcs.getCapabilities().getIdentification().supportsScaling();
            if(supportsScaling){
                if(describeCoverage == null)
                    describeCoverage = wcs.getDescribeCoverage(this.identifier);
                String[] gridAxisLabels = describeCoverage.getGridAxisLabels();
                int[] lonLatPositions = WcsUtils.getLonLatPositions(gridAxisLabels);

                String[] axisLabels = describeCoverage.getAxisLabels();
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
