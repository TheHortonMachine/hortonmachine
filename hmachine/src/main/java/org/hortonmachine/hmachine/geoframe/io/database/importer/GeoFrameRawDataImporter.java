/**
 * TODO 
 *This class provides a temporary solution for importing data into the database. 
 *The prerequisite is that the input data must be in the standard GeoFrame CSV format. 
 *
 *
 *nb this is related to the OMSGeoframeInputbuilder where create the stream gauge table.
 *
 *
 *
*/

package org.hortonmachine.hmachine.geoframe.io.database.importer;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameGeoTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.HydroMeteoSationSchema.HydroMeteoSation;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.HydroMeteoSationSchema.StationType;
import org.locationtech.jts.geom.Geometry;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description("Populate the db with raw data e.g. meteo srtation data, stream gauge data etc.")
@Author(name = "Daniele Andreis")
@Keywords("time series, iterator, basin, value, database")
@Name("GeoFrameRawDataImporter")
@Status(40)
@UI(HMConstants.HIDE_UI_HINT)
@License("General Public License Version 3 (GPLv3)")
public class GeoFrameRawDataImporter extends HMModel {

	@Description("A colum with the measurement point id")
	@In
	public StationType stationType = null;

	@Description("A colum with the measurement point id")
	@In
	public String inMeasurementPointIdFieldName = null;

	@Description("Input station/measured location vector point.")
	@UI(HMConstants.FILEIN_UI_HINT_VECTOR)
	@In
	public String inMeasurementsPointFilePath = null;

	@Description("Input csv file with data to import.")
	@UI(HMConstants.FILEIN_UI_HINT_CSV)
	@In
	public String inMeasurementDataFilePath = null;

	@Description("Input database path")
	@UI(HMConstants.FILEIN_UI_HINT_DBF)
	@In
	public String inGeoframeDBPath = null;

	@In
	public String inIdField = null;

	@In
	public String inElevationField = null;

	public boolean doOverWrite = true;

	public void process() {

		if (inGeoframeDBPath == null) {
			throw new IllegalArgumentException();
		}
		try {
			ASpatialDb inGeoframeDb = EDb.GEOPACKAGE.getSpatialDb();
			inGeoframeDb.open(inGeoframeDBPath);
			int[] ids = null;
			if (inMeasurementsPointFilePath != null) {
				SimpleFeatureCollection lakesFC = null;
				lakesFC = getVector(inMeasurementsPointFilePath);
				ids = new int[lakesFC.size()];
				var builder = GeoFrameGeoTable.HYDRO_METEO_STATION.getSchema()
						.getSFBuilder(lakesFC.getSchema().getCoordinateReferenceSystem());
				DefaultFeatureCollection outFC = new DefaultFeatureCollection();
				try (SimpleFeatureIterator iterator = lakesFC.features()) {
					while (iterator.hasNext()) {
						SimpleFeature sourceFeature = iterator.next();
						Geometry geom = (Geometry) sourceFeature.getDefaultGeometry();
						Integer id = (Integer) sourceFeature.getAttribute(inIdField);
						Double elevation = (Double) sourceFeature.getAttribute(inElevationField);

						builder.reset();
						builder.add(geom);
						builder.add(id);
						builder.add(elevation);
						builder.add(null); // basin_id
						builder.add(stationType.name()); // type

						SimpleFeature newFeature = builder.buildFeature(null);
						outFC.add(newFeature);
					}
				}

				var table = GeoFrameGeoTable.HYDRO_METEO_STATION.getSchema().getSQLName();
				SpatialDbsImportUtils.createTableFromSchema(inGeoframeDb, outFC.getSchema(), table, null, false);
				SpatialDbsImportUtils.importFeatureCollection(inGeoframeDb, outFC, table, -1, false, pm);

			} else {
				QueryResult result = inGeoframeDb.getTableRecordsMapFromRawSql("select * from "
						+ GeoFrameGeoTable.HYDRO_METEO_STATION.tableName() + " where type='" + stationType.name() + "'",
						-1);
				int idIndex = result.names.indexOf(HydroMeteoSation.ID.columnName());

				var rows = result.data;
				int l = result.data.size();
				ids = new int[l];
				for (int i = 0; i < l; i++) {
					ids[i] = ((Number) rows.get(i)[idIndex]).intValue();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
