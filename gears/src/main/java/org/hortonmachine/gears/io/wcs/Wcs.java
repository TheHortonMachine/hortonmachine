package org.hortonmachine.gears.io.wcs;

import java.math.BigDecimal;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.wcs.readers.CoverageReaderParameters;
import org.locationtech.jts.geom.Envelope;

public class Wcs {

    private String url;
    private String version;
    private IWebCoverageService service;

    public Wcs(String url, String version) throws Exception {
        this.url = url;
        this.version = version;
        init();
    }
    
    public Wcs(String url) throws Exception {
        this(url, null);
    }

    private void init() throws Exception{
        service = IWebCoverageService.getServiceForVersion(url, version);
    }

    public static Envelope evelope(Double minX, Double minY, Double maxX, Double maxY) {
        return new Envelope(minX, maxX, minY, maxY);
    }

    public static Envelope evelope(double... wsen) {
    	return new Envelope(wsen[0], wsen[1], wsen[2], wsen[3]);
    }
    
    public static Envelope evelope(BigDecimal minX, BigDecimal minY, BigDecimal maxX, BigDecimal maxY) {
    	return new Envelope(minX.doubleValue(), maxX.doubleValue(), minY.doubleValue(), maxY.doubleValue());
    }
    
    public static Envelope evelope(double minX, double minY, double maxX, double maxY) {
    	return new Envelope(minX, maxX, minY, maxY);
    }
    
    public String version() throws Exception {
        return service.getVersion();
    }

    public String capabilitiesUrl() {
        return service.getCapabilitiesUrl();
    }

    public List<String> ids() throws Exception {
        return service.getCoverageIds();
    }

    public ICoverageSummary summary(String coverageId) throws Exception {
        return service.getCoverageSummary(coverageId);
    }
    
    public String describeUrl(String coverageId) throws Exception {
        return service.getDescribeCoverageUrl(coverageId);
    }

    public IDescribeCoverage describe(String coverageId) throws Exception {
        return service.getDescribeCoverage(coverageId);
    }

    public List<String> formats() throws Exception {
        return service.getSupportedFormats();
    }

    public int[] srids() throws Exception {
        return service.getSupportedSrids();
    }

    /**
     * Retrieves a grid coverage for a given coverage ID, bounding box, SRID, width and height.
     *
     * @param coverageId the ID of the coverage to retrieve
     * @param bbox the bounding box of the coverage
     * @param srid the SRID of the coverage
     * @param width the width of the coverage (columns)
     * @param height the height of the coverage (rows)
     * @return the retrieved grid coverage
     * @throws Exception if an error occurs while retrieving the coverage
     */
    public GridCoverage2D getCoverage(String coverageId, Envelope bbox, int srid, int width, int height) throws Exception {
        CoverageReaderParameters parameters = new CoverageReaderParameters(service, coverageId);
        parameters.bbox(bbox, srid);
        parameters.rowsCols(width, height );
        return service.getCoverage(parameters, null);
    }

    /**
     * Retrieves a grid coverage for a given coverage ID, bounding box, SRID, width and height.
     *
     * @param coverageId the ID of the coverage to retrieve
     * @param bbox the bounding box of the coverage
     * @param srid the SRID of the coverage
     * @param width the width of the coverage (columns)
     * @param height the height of the coverage (rows)
     * @return the retrieved grid coverage
     * @throws Exception if an error occurs while retrieving the coverage
     */
    public GridCoverage2D getCoverage(String coverageId, Envelope bbox, int srid) throws Exception {
        CoverageReaderParameters parameters = new CoverageReaderParameters(service, coverageId);
        parameters.bbox(bbox, srid);
        return service.getCoverage(parameters, null);
    }

    public CoverageReaderParameters getReaderParameters(String coverageId) throws Exception{
        CoverageReaderParameters parameters =  new CoverageReaderParameters(service, coverageId);
        parameters.format("image/tiff");
        return parameters;
    }

    public String dumpCoverage(String path, CoverageReaderParameters readerParameters) throws Exception {
    	return service.getCoverage(path, readerParameters, null);
    }




    


    
}
