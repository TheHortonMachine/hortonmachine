package org.hortonmachine.gears.utils.crs;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

public class HMCrsTransformer {
	
	private CoordinateReferenceSystem fromCrs;
	private CoordinateReferenceSystem toCrs;
	private boolean acceptLenientDatumShift = true;
	private MathTransform mathTransform;

	public HMCrsTransformer(CoordinateReferenceSystem fromCrs, CoordinateReferenceSystem toCrs) {
		this.fromCrs = fromCrs;
		this.toCrs = toCrs;
	}
	
	public HMCrsTransformer(String fromEpsgCode, String toEpsgCode, boolean longitudeFirst) throws Exception {
		this.fromCrs = HMCrsRegistry.INSTANCE.getCrs(fromEpsgCode, longitudeFirst);
		this.toCrs = HMCrsRegistry.INSTANCE.getCrs(toEpsgCode, longitudeFirst);
	}
	
	public HMCrsTransformer(String fromEpsgCode, String toEpsgCode) throws Exception {
		this(fromEpsgCode, toEpsgCode, false);
	}
	
	public void setAcceptLenientDatumShift(boolean acceptLenientDatumShift) {
		this.acceptLenientDatumShift = acceptLenientDatumShift;
	}
	
	private void init() throws Exception {
		if (mathTransform == null) {
			mathTransform = CRS.findMathTransform(fromCrs, toCrs, acceptLenientDatumShift);
		}
	}
	
	public MathTransform getMathTransform() throws Exception {
		init();
		return mathTransform;
	}
	
	public Geometry transform(Geometry geometry) throws Exception {
		init();
		return JTS.transform(geometry, mathTransform);
	}
	
	public Coordinate transform(Coordinate coordinate) throws Exception {
		init();
		return JTS.transform(coordinate, null, mathTransform);
	}
	
	public Envelope transform(Envelope envelope) throws Exception {
		init();
		return JTS.transform(envelope, mathTransform);
	}
	
	public Envelope transformDensified(Envelope envelope, int numDensificationPoints) throws Exception {
		init();
		return JTS.transform(envelope, null, mathTransform, numDensificationPoints);
	}
	
	
	

}
