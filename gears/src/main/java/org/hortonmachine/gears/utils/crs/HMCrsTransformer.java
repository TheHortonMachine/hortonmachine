package org.hortonmachine.gears.utils.crs;

import java.io.File;

import org.eclipse.imagen.Interpolation;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
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

	public static double[] evaluateAreaDistortion(Object obj, String epsgCode1, String epsgCode2) throws Exception {
		CoordinateReferenceSystem sourceCrs;
		double area1;
		double area2;

		if (obj instanceof String) {
			obj = OmsVectorReader.readVector((String) obj);
		} else if (obj instanceof File) {
			obj = OmsVectorReader.readVector(((File) obj).getAbsolutePath());
		}

		if (obj instanceof SimpleFeatureCollection) {
			SimpleFeatureCollection featureCollection = (SimpleFeatureCollection) obj;
			sourceCrs = featureCollection.getSchema().getCoordinateReferenceSystem();
			if (sourceCrs == null) {
				throw new IllegalArgumentException("The supplied feature collection has no coordinate reference system.");
			}
			area1 = evaluateAreaInProjection(featureCollection, sourceCrs, epsgCode1);
			area2 = evaluateAreaInProjection(featureCollection, sourceCrs, epsgCode2);
		} else if (obj instanceof SimpleFeature) {
			SimpleFeature feature = (SimpleFeature) obj;
			sourceCrs = feature.getFeatureType().getCoordinateReferenceSystem();
			if (sourceCrs == null) {
				throw new IllegalArgumentException("The supplied feature has no coordinate reference system.");
			}
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			area1 = evaluateAreaInProjection(geometry, sourceCrs, epsgCode1);
			area2 = evaluateAreaInProjection(geometry, sourceCrs, epsgCode2);
		} else if (obj instanceof Geometry) {
			Geometry geometry = (Geometry) obj;
			sourceCrs = null;
			Object userData = geometry.getUserData();
			if (userData instanceof CoordinateReferenceSystem) {
				sourceCrs = (CoordinateReferenceSystem) userData;
			}
			if (sourceCrs == null && geometry.getSRID() > 0) {
				sourceCrs = CrsUtilities.getCrsFromSrid(geometry.getSRID());
			}
			if (sourceCrs == null) {
				throw new IllegalArgumentException("The supplied geometry has no CRS in userData and no valid SRID.");
			}
			area1 = evaluateAreaInProjection(geometry, sourceCrs, epsgCode1);
			area2 = evaluateAreaInProjection(geometry, sourceCrs, epsgCode2);
		} else {
			throw new IllegalArgumentException("Unsupported input type for area distortion evaluation: " + obj);
		}

		double areaDiff = Math.abs(area1 - area2);
		return new double[]{area1, area2, areaDiff};
	}

	private static double evaluateAreaInProjection(SimpleFeatureCollection featureCollection, CoordinateReferenceSystem sourceCrs,
			String targetEpsgCode) throws Exception {
		CoordinateReferenceSystem targetCrs = HMCrsRegistry.INSTANCE.getCrs(targetEpsgCode, true);
		HMCrsTransformer transformer = new HMCrsTransformer(sourceCrs, targetCrs);
		double totalArea = 0.0;
		try (SimpleFeatureIterator iterator = featureCollection.features()) {
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				Geometry geometry = (Geometry) feature.getDefaultGeometry();
				if (geometry == null || geometry.isEmpty()) {
					continue;
				}
				totalArea += transformer.transform(geometry).getArea();
			}
		}
		return totalArea;
	}

	private static double evaluateAreaInProjection(Geometry geometry, CoordinateReferenceSystem sourceCrs, String targetEpsgCode)
			throws Exception {
		if (geometry == null || geometry.isEmpty()) {
			return 0.0;
		}
		CoordinateReferenceSystem targetCrs = HMCrsRegistry.INSTANCE.getCrs(targetEpsgCode, true);
		HMCrsTransformer transformer = new HMCrsTransformer(sourceCrs, targetCrs);
		return transformer.transform(geometry).getArea();
	}
}
