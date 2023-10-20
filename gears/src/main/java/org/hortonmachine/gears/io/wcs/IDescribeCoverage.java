package org.hortonmachine.gears.io.wcs;

import org.locationtech.jts.geom.Envelope;

public interface IDescribeCoverage extends XmlHelper.XmlVisitor {

    Envelope getCoverageEnvelope();

    Integer getCoverageEnvelopeSrid();

    String[] getAxisLabels();
    
    String[] getGridAxisLabels();
}