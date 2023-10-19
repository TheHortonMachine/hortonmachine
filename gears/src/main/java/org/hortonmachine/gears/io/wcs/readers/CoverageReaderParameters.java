package org.hortonmachine.gears.io.wcs.readers;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.io.wcs.IWebCoverageService;
import org.hortonmachine.gears.io.wcs.WcsUtils;
import org.hortonmachine.gears.io.wcs.wcs201.WebCoverageService201;
import org.hortonmachine.gears.io.wcs.wcs201.models.DescribeCoverage;
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


    private IWebCoverageService service;


    public CoverageReaderParameters(final IWebCoverageService service, String coverageId) throws Exception {
        this.service = service;
        this.wcsVersion = service.getVersion();
        this.identifier = coverageId;
    }

    public CoverageReaderParameters bbox(ReferencedEnvelope requestedEnvelope ){
        bbox = requestedEnvelope;
        return this;
    }

    public CoverageReaderParameters format(String format){
        this.format = format;
        return this;
    }

    public CoverageReaderParameters scaleFactor(Double scaleFactor){
        this.scaleFactor = scaleFactor;
        return this;
    }

    public CoverageReaderParameters rowsCols(int[] rowsCols){
        this.rowsCols = rowsCols;
        return this;
    }



    public String toUrl(HashMap<String, String> additionalParams) throws Exception {
        String url = "";
        url += "service=WCS";
        url += "&version=" + this.wcsVersion;
        url += "&request=GetCoverage";
        if (this.wcsVersion.equals("2.0.1")) {
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

                String usedSrid = CrsUtilities.getCodeFromCrs(requestEnvelope.getCoordinateReferenceSystem());
                if (usedSrid != null)
                    url += "&SUBSETTINGCRS=" + usedSrid;

            }
            if (this.scaleFactor != null)
                url += "&SCALEFACTOR=" + this.scaleFactor;
            if (this.rowsCols != null){
                // we need to check if the service supports scaling
                boolean supportsScaling = wcs.getCapabilities().getIdentification().supportsScaling();
                
                
                if(supportsScaling){
                    DescribeCoverage describeCoverage = wcs.getDescribeCoverage(this.identifier);
                    String[] gridAxisLabels = describeCoverage.gridAxisLabels;
                    int[] lonLatPositions = WcsUtils.getLonLatPositions(gridAxisLabels);

                    String[] axisLabels = describeCoverage.axisLabels;
                    url += "&SCALESIZE=" + axisLabels[lonLatPositions[0]] + "(" + this.rowsCols[1] + "),"+ axisLabels[lonLatPositions[1]] + "(" + this.rowsCols[0] + ")";
                    // url += "&WIDTH=" + this.rowsCols[1];
                    // url += "&HEIGHT=" + this.rowsCols[0];
                }
            }
            

            
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


}
