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

    public String time = null;
    public String store = null;
    public String rangesubset = null;
    public String gridbaseCRS = null;
    public String gridtype = null;
    public String gridCS = null;
    public String gridorigin = null;
    public String gridoffsets = null;
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



    public String toUrl(HashMap<String, String> additionalParams) throws Exception {
        String url = "";
        url += "service=WCS";
        url += "&version=" + this.wcsVersion;
        url += "&request=GetCoverage";
        if (this.wcsVersion.equals("2.0.1")) {
            url += "&COVERAGEID=" + this.identifier;
            if (this.format != null)
                url += "&format=" + this.format;
            if (this.bbox != null){
                // need to get axis labels
                DescribeCoverage describeCoverage = ((WebCoverageService201) service).getDescribeCoverage(this.identifier);
                ReferencedEnvelope dataEnvelope = describeCoverage.envelope;
                ReferencedEnvelope requestEnvelope = bbox;
                if(!CRS.equalsIgnoreMetadata(dataEnvelope.getCoordinateReferenceSystem(), bbox.getCoordinateReferenceSystem())){
                    requestEnvelope = bbox.transform(dataEnvelope.getCoordinateReferenceSystem(), true);
                }

                String[] axisLabels = describeCoverage.gridAxisLabels;
                 //     &subset=Lat(34.54889,37.31744)
                //     &subset=Long(26.51071,29.45505)
                String[] lonLatLabelsOrdered = WcsUtils.orderLabels(axisLabels);
                url += "&subset=" + lonLatLabelsOrdered[0] + "(" + requestEnvelope.getMinX() + "," + requestEnvelope.getMaxX() + ")";
                url += "&subset=" + lonLatLabelsOrdered[1] + "(" + requestEnvelope.getMinY() + "," + requestEnvelope.getMaxY() + ")";

                String usedSrid = CrsUtilities.getCodeFromCrs(requestEnvelope.getCoordinateReferenceSystem());
                if (usedSrid != null)
                    url += "&SUBSETTINGCRS=" + usedSrid;

            }
            if (this.scaleFactor != null)
                url += "&SCALEFACTOR=" + this.scaleFactor;
            

            
        } else if (this.wcsVersion.equals("1.1.0")) {
            url += "&identifier=" + this.identifier;
            if (this.bbox != null)
                url += "&boundingbox=" + this.bbox;
            if (this.time != null)
                url += "&timesequence=" + this.time;
            if (this.format != null)
                url += "&format=" + this.format;
            if (this.store != null)
                url += "&store=" + this.store;
            if (this.rangesubset != null)
                url += "&RangeSubset=" + this.rangesubset;
            if (this.gridbaseCRS != null)
                url += "&gridbaseCRS=" + this.gridbaseCRS;
            if (this.gridtype != null)
                url += "&gridtype=" + this.gridtype;
            if (this.gridCS != null)
                url += "&gridCS=" + this.gridCS;
            if (this.gridorigin != null)
                url += "&gridorigin=" + this.gridorigin;
            if (this.gridoffsets != null)
                url += "&gridoffsets=" + this.gridoffsets;

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
