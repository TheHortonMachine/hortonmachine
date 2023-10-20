package org.hortonmachine.gears.io.wcs;

import org.geotools.geometry.jts.ReferencedEnvelope;

public interface ICoverageSummary extends XmlHelper.XmlVisitor {

    ReferencedEnvelope getBoundingBox();

    ReferencedEnvelope getWgs84BoundingBox();

    String getCoverageId();

    String getTitle();

    String getAbstract();

    String toString();

}