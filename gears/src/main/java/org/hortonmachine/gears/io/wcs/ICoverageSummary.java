package org.hortonmachine.gears.io.wcs;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Envelope;

public interface ICoverageSummary extends XmlHelper.XmlVisitor {

    Envelope getBoundingBox();

    Integer getBoundingBoxSrid();

    ReferencedEnvelope getWgs84BoundingBox();

    String getCoverageId();

    String getTitle();

    String getAbstract();

    String toString();

}