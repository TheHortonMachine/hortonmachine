package org.hortonmachine.gears.io.wcs;

import java.util.List;

import org.locationtech.jts.geom.Envelope;

public interface IDescribeCoverage extends XmlHelper.XmlVisitor {

    /**
     * Returns the spatial extent of the coverage.
     *
     * @return the spatial extent of the coverage
     */
    Envelope getCoverageEnvelope();

    /**
     * Gets the Spatial Reference Identifier (SRID) of the coverage envelope.
     *
     * @return the SRID of the coverage envelope
     */
    Integer getCoverageEnvelopeSrid();

    /**
     * Returns an array of axis labels in their right order.
     * 
     * <p>This is necessary for version > 1 in order to properly 
     * evaluate axes order and labels for getCoverage requests.</p>
     *
     * @return an array of axis labels.
     */
    String[] getWorldAxisLabels();
    
    /**
     * Returns an array of labels for the grid axes of the coverage in their right order.
     *
     * <p>This is necessary for version > 1 in order to properly 
     * evaluate axes order and labels for getCoverage requests.</p>
     * 
     * @return an array of labels for the grid axes of the coverage
     */
    String[] getGridAxisLabels();

    /**
     * Returns a list of supported formats for the coverage data.
     * 
     * <p>This returns null for versions in which the formats are not in the 
     * DescribeCoverage document, but in the capabilities document.</p>
     *
     * @return a list of supported formats
     * @throws Exception if an error occurs while retrieving the supported formats
     */
    public List<String> getSupportedFormats() throws Exception;

    /**
     * Returns the native format of the coverage data.
     * 
     * 
     *
     * @return the native format of the coverage data
     * @throws Exception if an error occurs while retrieving the native format
     */
    String getNativeFormat() throws Exception;
    
    /**
     * Returns an array of supported SRIDs.
     * 
     * <p>This returns null for versions in which the formats are not in the 
     * DescribeCoverage document, but in the capabilities document.</p>
     *
     * @return an array of supported SRIDs
     * @throws Exception if an error occurs while retrieving the supported SRIDs
     */
    int[] getSupportedSrids() throws Exception;

}