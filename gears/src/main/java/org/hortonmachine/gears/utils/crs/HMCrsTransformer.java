package org.hortonmachine.gears.utils.crs;

import java.io.File;

import org.eclipse.imagen.Interpolation;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.crs.ProjectedCRS;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.projection.ProjectionException;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * Utility class for transforming geometries, features, and rasters between different
 * coordinate reference systems (CRS).
 */
public class HMCrsTransformer {
	private static final double SAFE_MAX_LAT = 89.9999;
	private static final double SAFE_MAX_LON = 179.9999;
	private static final Geometry SAFE_GEOGRAPHIC_EXTENT = new GeometryFactory().toGeometry(
	        new Envelope(-SAFE_MAX_LON, SAFE_MAX_LON, -SAFE_MAX_LAT, SAFE_MAX_LAT));
	
	private CoordinateReferenceSystem fromCrs;
	private CoordinateReferenceSystem toCrs;
	private boolean acceptLenientDatumShift = true;
	private MathTransform mathTransform;

	/**
	 * Creates a new HMCrsTransformer for transforming between the specified source and target CRS.
	 * 
	 * @param fromCrs
	 * @param toCrs
	 */
	public HMCrsTransformer(CoordinateReferenceSystem fromCrs, CoordinateReferenceSystem toCrs) {
		this.fromCrs = fromCrs;
		this.toCrs = toCrs;
	}
	
	/**
	 * Creates a new HMCrsTransformer for transforming between the specified source 
	 * and target CRS, with options for longitude-first axis order. 
	 * 
	 * @param fromEpsgCode
	 * @param toEpsgCode
	 * @param fromLongitudeFirst setting to true will attempt to retrieve the source CRS 
	 * 			with longitude-first axis order.
	 * @param toLongitudeFirst setting to true will attempt to retrieve the target CRS 
	 * 			with longitude-first axis order.
	 * @throws Exception
	 */
	public HMCrsTransformer(String fromEpsgCode, String toEpsgCode, boolean fromLongitudeFirst, boolean toLongitudeFirst) throws Exception {
		this.fromCrs = HMCrsRegistry.INSTANCE.getCrs(fromEpsgCode, fromLongitudeFirst);
		this.toCrs = HMCrsRegistry.INSTANCE.getCrs(toEpsgCode, toLongitudeFirst);
	}

	/**
	 * Creates a new HMCrsTransformer for transforming between the specified source and target CRS.
	 * The CRS will be retrieved with the default axis order.
	 * 
	 * @param fromEpsgCode
	 * @param toEpsgCode
	 * @throws Exception
	 */
	public HMCrsTransformer(String fromEpsgCode, String toEpsgCode) throws Exception {
		this(fromEpsgCode, toEpsgCode, false, false);
	}
	
	/**
	 * Sets whether to accept lenient datum shifts during transformation.
	 * 
	 * @param acceptLenientDatumShift
	 */
	public void setAcceptLenientDatumShift(boolean acceptLenientDatumShift) {
		this.acceptLenientDatumShift = acceptLenientDatumShift;
	}
	
	/**
	 * Initializes the math transform if it has not been initialized yet.
	 * 
	 * @throws Exception
	 */
	private void init() throws Exception {
		if (mathTransform == null) {
			mathTransform = CRS.findMathTransform(fromCrs, toCrs, acceptLenientDatumShift);
		}
	}
	
	/**
	 * Returns the MathTransform object for transforming between the source and target CRS.
	 * 
	 * @return
	 * @throws Exception
	 */
	public MathTransform getMathTransform() throws Exception {
		init();
		return mathTransform;
	}
	
	public Geometry transform(Geometry geometry) throws Exception {
	    init();
	    try {
	        return JTS.transform(geometry, mathTransform);
	    } catch (ProjectionException e) {
	        if (!(fromCrs instanceof GeographicCRS) || !(toCrs instanceof ProjectedCRS)) {
	            throw e; // not a pole/antimeridian issue, rethrow
	        }
	        Geometry clipped = geometry.intersection(SAFE_GEOGRAPHIC_EXTENT);
	        if (clipped.isEmpty()) {
	            throw e; // geometry was entirely outside safe extent, rethrow original
	        }
	        return JTS.transform(clipped, mathTransform);
	    }
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
