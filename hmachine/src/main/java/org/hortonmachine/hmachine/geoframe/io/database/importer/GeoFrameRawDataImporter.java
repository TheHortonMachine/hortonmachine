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

import java.util.HashMap;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameGeoTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameSimpleTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.HydroMeteoSationSchema.HydroMeteoSation;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.HydroMeteoSationSchema.StationType;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.TimeResolution;
import org.locationtech.jts.geom.Geometry;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
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

	@In
	public String inStartDate = null;

	@In
	public String inEndDate = null;

	@In
	public int inVariableType = -1;

	@In
	public TimeResolution timeResolution = null;

	public boolean doOverWrite = true;

	private ASpatialDb inGeoframeDb = null;

	private IHMPreparedStatement ps = null;

	private IHMConnection conn;

	@Execute
	public void process() {

		if (inGeoframeDBPath == null) {
			throw new IllegalArgumentException();
		}
		try {
			inGeoframeDb = EDb.GEOPACKAGE.getSpatialDb();
			inGeoframeDb.open(inGeoframeDBPath);
			int[] ids = null;
			if (inMeasurementsPointFilePath != null) {
				SimpleFeatureCollection lakesFC = null;
				lakesFC = getVector(inMeasurementsPointFilePath);
				ids = new int[lakesFC.size()];
				var builder = GeoFrameGeoTable.HYDRO_METEO_STATION.getSchema()
						.getSFBuilder(lakesFC.getSchema().getCoordinateReferenceSystem());
				DefaultFeatureCollection outFC = new DefaultFeatureCollection();
				int i = 0;
				try (SimpleFeatureIterator iterator = lakesFC.features()) {
					while (iterator.hasNext()) {
						SimpleFeature sourceFeature = iterator.next();
						Geometry geom = (Geometry) sourceFeature.getDefaultGeometry();
						Long id = (Long) sourceFeature.getAttribute(inIdField);
						Double elevation = (Double) sourceFeature.getAttribute(inElevationField);

						builder.reset();
						builder.add(geom);
						builder.add(id);
						builder.add(elevation);
						builder.add(null); // basin_id
						builder.add(stationType.name()); // type

						SimpleFeature newFeature = builder.buildFeature(null);
						outFC.add(newFeature);
						ids[i] = id.intValue();
						i++;
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

			var reader = new OmsTimeSeriesIteratorReader();
			var formatter = HMConstants.utcDateFormatterYYYYMMDDHHMM;

			reader.file = inMeasurementDataFilePath;
			reader.idfield = "ID";
			reader.tStart = inStartDate;
			reader.tEnd = inStartDate;
			reader.fileNovalue = "-9999";

			// TODO this is the right way for hourly and daily but not for weekly or and
			// yearly
			reader.tTimestep = timeResolution.toMinutes();
			reader.initProcess();
			while (reader.doProcess) {
				reader.nextRecord();
				HashMap<Integer, double[]> values = reader.outData;
				System.out.println(reader.tCurrent);
				for (int id : ids) {
					double[] data = values.get(id);
					if (data != null) {
						long ts = formatter.parseMillis(reader.tCurrent);
						insert(ts, id, data[0]);
					}
				}
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// TODO copied from the simulation writer
	private void ensureOpen() throws Exception {
		if (ps != null) {
			return;
		}
		// make sure the output table exists
		String sql = GeoFrameSimpleTable.RAW_METEO.getSchema().createTableSql();
		inGeoframeDb.executeInsertUpdateDeleteSql(sql);
		String insertSql = GeoFrameSimpleTable.RAW_METEO.getSchema().buildInsertAll();
		conn = inGeoframeDb.getConnectionInternal();
		ps = conn.prepareStatement(insertSql);
	}

	public void insert(long currentT, int stationId, double value) throws Exception {
		ensureOpen();
		conn.enableAutocommit(false);
		ps.setLong(1, currentT);
		ps.setInt(2, stationId);
		ps.setInt(3, inVariableType);
		ps.setDouble(4, value);

		ps.addBatch();
		ps.executeBatch();
		conn.commit();
		conn.enableAutocommit(true);
	}

}
