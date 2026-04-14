package org.hortonmachine.gears.utils.crs;

import org.eclipse.imagen.Interpolation;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.libs.modules.HMRaster;
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
	
	public SimpleFeatureCollection transform(SimpleFeatureCollection featureCollection) throws Exception {
		init();
		return new ReprojectingFeatureCollection(featureCollection, toCrs);
	}
	
	public SimpleFeature transform(SimpleFeature feature) throws Exception {
		init();
		feature.setDefaultGeometry(transform((Geometry) feature.getDefaultGeometry()));
		return feature;
	}
	
	public HMRaster transform(HMRaster raster, GridGeometry2D gridGeometry, Interpolation interpolation) throws Exception {
		init();
		GridCoverage2D outRaster = (GridCoverage2D) Operations.DEFAULT.resample(raster.buildCoverage(), toCrs, gridGeometry, interpolation);
		return HMRaster.fromGridCoverage(outRaster);
	}
	
	public HMRaster transform(HMRaster raster) throws Exception {
		return transform(raster, null, Interpolation.getInstance(Interpolation.INTERP_NEAREST));
	}
}
