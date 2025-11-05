/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.gears.io.wfs;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.Query;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.data.wfs.WFSServiceInfo;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.utils.features.CoordinateSwappingFeatureCollection;
import org.hortonmachine.gears.utils.features.FeatureGeometrySubstitutor;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * A simple WFS client wrapper class.
 * 
 * 
 * Example usage:
 * 
 * <code>
 *      final String wfsUrl =
 *            "https://visualizador.ideam.gov.co/gisserver/services/Vulnerabilidad_Susceptibilidad_Ambiental/MapServer/WFSServer?service=WFS&request=GetCapabilities";
 *        final String desiredLayerName = "Categorizacion_de_SZH_por_Evaluacion_Integrada_ENA_2014";
 *        
 *        Wfs wfs = new Wfs(wfsUrl, desiredLayerName);
 *    	// wfs.forceVersion("1.0.0");
 *        wfs.forceNormalizeGeometryName();
 *        wfs.connect();
 *        try {
 *        	System.out.println("Version: " + wfs.getVersion());
 *        	
 *        	List<String> typeNames = wfs.getTypeNames();
 *            System.out.println("Available type names:");
 *            for (String tn : typeNames) {
 *                System.out.println("  - " + tn);
 *            }
 *
 *            SimpleFeatureType schema = wfs.getSimpleFeatureType();
 *            CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
 *            System.out.println("Schema name: " + schema.getTypeName());
 *            System.out.println("Attributes:");
 *            schema.getAttributeDescriptors().forEach(ad ->
 *                System.out.println("  - " + ad.getLocalName() + " : " + ad.getType().getBinding().getSimpleName()));
 *            System.out.println("CRS: " + (crs != null ? CRS.toSRS(crs) : "unknown"));
 *            System.out.println("Bounds (from server): " + wfs.getBounds());
 *
 *            Envelope env = new Envelope(-76, -75., 8.0, 9.0);
 *            SimpleFeatureCollection fc = wfs.getFeatureCollection(env);
 *            
 *            OmsVectorWriter.writeVector("/home/hydrologis/TMP/KLAB/wfs_test/test.shp", fc);
 *
 *        } finally {
 *            wfs.close();
 *        }
 * </code>
 * 
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class Wfs implements AutoCloseable {

	private String wfsUrl;
	private String typeName;
	private DataStore dataStore;
	private SimpleFeatureSource dataSource;
	private String version;
	private boolean arcCompatibility = false;
	private CoordinateReferenceSystem forceCrs;
	private boolean forceXYSwap = false;
	private boolean normalizeGeomName = false;
	private SimpleFeatureType schema;

	public Wfs(String wfsUrl) {
		this.wfsUrl = wfsUrl;
	}
	
	public Wfs(String wfsUrl, String typeName) {
		this.wfsUrl = wfsUrl;
		this.typeName = typeName;
		checkTypeName();
	}
	
	/**
	 * Force the WFS version to use.
	 * 
	 * <b>This needs to be called before {@link #connect()}.</b>
	 * 
	 * @param version the version string, e.g. "1.0.0", "1.1.0" or "2.0.0".
	 */
	public void forceVersion(String version) {
		this.version = version;
	}
	
	/**
	 * Force ArcGIS compatibility mode.
	 * 
	 * <b>This needs to be called before {@link #connect()}.</b>
	 */
	public void forceArcgisCompatibility() {
		this.arcCompatibility  = true;
	}
	
	/**
	 * Force a custom CRS.
	 * 
	 * @param crs
	 */
	public void forceCrs(CoordinateReferenceSystem crs) {
		this.forceCrs = crs;
	}
	
	/**
	 * Forces a manual swapping of the coordinates. To be used as last option, if the wfs 
	 * client does not work properly.
	 */
	public void forceCoordinateSwapping() {
		this.forceXYSwap  = true;
	}
	
	/**
	 * Converts the geometry name to work with geotools file io libs.
	 */
	public void forceNormalizeGeometryName() {
		this.normalizeGeomName  = true;
	}
	
	/**
	 * Connects to the WFS and checks the type name.
	 * 
	 * @throws Exception
	 */
	public void connect() throws Exception {
		Map<String, Object> params = new HashMap<>();
		params.put(WFSDataStoreFactory.TIMEOUT.key, 30_000); // ms
		params.put(WFSDataStoreFactory.MAXFEATURES.key, 10_000); // just a ceiling
		// nudge protocol selection if needed:
		// params.put(WFSDataStoreFactory.PROTOCOL.key,
		// WFSDataStoreFactory.Protocol.AUTO); // or GET/POST
		
		// Force GET to avoid ArcGIS 400s on POST
		params.put(WFSDataStoreFactory.PROTOCOL.key, "GET");

		// ArcGIS compatibility and leniency
		if (arcCompatibility) {
			params.put(WFSDataStoreFactory.WFS_STRATEGY.key, "arcgis");
			params.put(WFSDataStoreFactory.LENIENT.key, true);
		}
		
		if (version != null) {
			// tweak url to add version if not present
			if (!wfsUrl.toLowerCase(Locale.ROOT).contains("version=")) {
				if(version.equals("1.0.0")) {
					if (wfsUrl.contains("?")) {
						wfsUrl += "&version=" + version;
					} else {
						wfsUrl += "?version=" + version;
					}
				} else if (version.equals("1.1.0")|| version.equals("2.0.0")) {
					if (wfsUrl.contains("?")) {
						wfsUrl += "&acceptVersions=" + version;
					} else {
						wfsUrl += "?acceptVersions=" + version;
					}
				}
			}
		}
		params.put("WFSDataStoreFactory:GET_CAPABILITIES_URL", wfsUrl);

		dataStore = DataStoreFinder.getDataStore(params);
		if (dataStore == null) {
			throw new IllegalStateException("Could not connect to WFS. Check URL and network.");
		}

		if (typeName == null) {
			// no type name to check
			return;
		}
		// check type name
		List<String> typeNames = getTypeNames();
		// Try to find a matching type name (exact or suffix match without namespace)
		String chosenTypeName = null;
		for (String tn : typeNames) {
			if (tn.equalsIgnoreCase(typeName)) {
				chosenTypeName = tn;
				break;
			}
		}
		if (chosenTypeName == null) {
			// try matching without namespace (e.g., ns:LayerName -> LayerName)
			for (String tn : typeNames) {
				String bare = tn.contains(":") ? tn.substring(tn.indexOf(':') + 1) : tn;
				if (bare.equalsIgnoreCase(typeName)) {
					chosenTypeName = tn;
					break;
				}
			}
		}
		if (chosenTypeName == null) {
			// fall back to a contains match to help discover the exact name
			for (String tn : typeNames) {
				if (tn.toLowerCase(Locale.ROOT).contains(typeName.toLowerCase(Locale.ROOT))) {
					chosenTypeName = tn;
					break;
				}
			}
		}

		if (chosenTypeName == null) {
			throw new NoSuchElementException("Could not find a type matching: " + typeName + ". Check the list above.");
		}
		this.typeName = chosenTypeName;
	}
	
	/**
	 * Get the WFS version.
	 * 
	 * @return the version string or null.
	 */
	public String getVersion() {
		if (dataStore != null) {
			ServiceInfo info = dataStore.getInfo();
			if( info instanceof WFSServiceInfo ) {
				WFSServiceInfo wfsInfo = (WFSServiceInfo) info;
				return wfsInfo.getVersion();
			}
		}
		return null;
	}
	
	/**
	 * Gets the available type names.
	 * 
	 * @return the list of type names.
	 * @throws IOException
	 */
	public List<String> getTypeNames() throws IOException {
		return Arrays.asList(dataStore.getTypeNames());
	}
	
	private void checkTypeName() {
		if (typeName == null) {
			throw new IllegalStateException("No type name specified. Cannot proceed.");
		}
	}
	
	private void getDataSource() throws IOException {
		dataSource = (SimpleFeatureSource) dataStore.getFeatureSource(typeName);
	}
	
	/**
	 * 
	 * Gets the simple feature type of the connected type name.
	 * 
	 * @return the simple feature type (schema).
	 * @throws IOException
	 */
	public SimpleFeatureType getSimpleFeatureType() throws IOException {
		if( schema != null) {
			return schema;
		}
		checkTypeName();
		getDataSource();
		schema = dataSource.getSchema();
        return schema;
	}


	/**
	 * Gets a feature iterator for the connected type name, with optional filter.
	 * 
	 * <b>Note:</b> remember to close the iterator after use.
	 * 
	 * @param optionalFilter the optional filter, can be null.
	 * @return the feature iterator.
	 * @throws IOException
	 */
	public SimpleFeatureIterator getFeatureIterator(Query optionalFilter) throws IOException {
		checkTypeName();
		getDataSource();
		if (optionalFilter != null) {
			SimpleFeatureCollection featureCollection = dataSource.getFeatures(optionalFilter);
			if(forceCrs != null) {
				featureCollection = new ReprojectingFeatureCollection(featureCollection, forceCrs);
				schema = featureCollection.getSchema();
			}
			return featureCollection.features();
		}
		SimpleFeatureCollection featureCollection = dataSource.getFeatures();
		if(forceCrs != null) {
			featureCollection = new ReprojectingFeatureCollection(featureCollection, forceCrs);
			schema = featureCollection.getSchema();
		}
		return featureCollection.features();
	}
	
	/**
	 * Gets a feature iterator for the connected type name, with optional envelope filter.
	 * 
	 * <b>Note:</b> remember to close the iterator after use.
	 * 
	 * @param envelope the optional envelope to filter on, can be null. Has to be 
	 * 			in the same CRS as the data.
	 * @return the feature iterator.
	 * @throws Exception
	 */
	public SimpleFeatureIterator getFeatureIterator(Envelope envelope) throws Exception {
		checkTypeName();
		getDataSource();
		Query query = new Query(typeName); // default: no filter
		if (envelope != null) {
			SimpleFeatureType schema = dataSource.getSchema();
	        String geomName = schema.getGeometryDescriptor().getLocalName(); // e.g. "Shape"
	        FilterFactory ff = CommonFactoryFinder.getFilterFactory();
	        if(forceXYSwap) {
	        	// swap x and y in the bbox
	        	query.setFilter(ff.bbox(ff.property(geomName),
	        			envelope.getMinY(), envelope.getMinX(),
	        			envelope.getMaxY(), envelope.getMaxX(),
	        			null)); //
	        } else {
	        	query.setFilter(ff.bbox(ff.property(geomName),
	        			envelope.getMinX(), envelope.getMinY(),
	        			envelope.getMaxX(), envelope.getMaxY(),
	        			null)); // 
	        }
		} else {
			// include
			query.setFilter(Filter.INCLUDE);
		}
		return getFeatureIterator(query);
	}
	
	/**
	 * Gets a feature collection for the connected type name, with optional envelope filter.
	 * 
	 * @param envelope the optional envelope to filter on, can be null. Has to be 
	 * 		in the same CRS as the data.
	 * 		<b>The envelope filter has to be applied to the datasource, i.e. before any coordinate swapping,
	 * 		so one might need to fix the envelope accordingly..</b> 
	 * @return the feature collection.
	 * @throws Exception
	 */
	public SimpleFeatureCollection getFeatureCollection(Envelope envelope) throws Exception {
		checkTypeName();
		SimpleFeatureIterator featureIterator = getFeatureIterator(envelope);
		DefaultFeatureCollection fc = new DefaultFeatureCollection();
		
		try {
			FeatureGeometrySubstitutor substitutor = null;
			while (featureIterator.hasNext()) {
				SimpleFeature feature = featureIterator.next();
				Geometry defaultGeometry = (Geometry) feature.getDefaultGeometry();
				if (forceXYSwap) {
					defaultGeometry = CoordinateSwappingFeatureCollection.swapXY(defaultGeometry);
				}
				if (normalizeGeomName || forceXYSwap) {
					if(substitutor == null) {
						substitutor = new FeatureGeometrySubstitutor(feature.getFeatureType(), defaultGeometry.getClass());
					}
					feature = substitutor.substituteGeometry(feature, defaultGeometry);
				}
				fc.add(feature);
			}
		} finally {
			featureIterator.close();
		}
		
		return fc;
	}
	
	
	/**
	 * Gets the bounds of the connected type name.
	 * 
	 * @return the bounds.
	 * @throws IOException
	 */
	public ReferencedEnvelope getBounds() throws IOException {
		getDataSource();
		return dataSource.getBounds();
	}

	@Override
	public void close() throws Exception {
		if (dataStore != null) {
			dataStore.dispose();
		}
	}
	
	

}
