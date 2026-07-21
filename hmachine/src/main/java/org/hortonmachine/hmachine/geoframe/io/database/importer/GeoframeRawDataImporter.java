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
import java.util.List;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils;
import org.hortonmachine.hmachine.geoframe.io.database.TableUtils;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameGeoTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameSimpleTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.GeoframeGeoTableSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.BasinPolygonSchema.BasinMultiPolygonField;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.StationSchema.Station;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.StationSchema.StationType;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.EnvironmentalVariable;
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
public class GeoframeRawDataImporter extends HMModel {

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
	public boolean isStreamGauge = false;

	// TODO maybe to infer from data
	@In
	public TimeResolution timeResolution = null;

	public boolean doOverWrite = false;

	private ASpatialDb inGeoframeDb = null;

	@Execute
	public void process() {

		if (inGeoframeDBPath == null) {
			throw new IllegalArgumentException();
		}
		try {
			inGeoframeDb = EDb.GEOPACKAGE.getSpatialDb();
			inGeoframeDb.open(inGeoframeDBPath);

			int[] ids = null;
			var stationTable = GeoFrameGeoTable.HYDRO_METEO_STATION.getSchema().getSQLName();
			var dataTable = GeoFrameSimpleTable.STATIONDATA.getSchema().getSQLName();
			var varTable = GeoFrameSimpleTable.VARIABLE.getSchema().getSQLName();

			if (doOverWrite) {
				if (inGeoframeDb.hasTable(varTable)) {
					inGeoframeDb.executeInsertUpdateDeleteSql("DROP TABLE " + varTable);
				}
				if (inGeoframeDb.hasTable(dataTable)) {
					inGeoframeDb.executeInsertUpdateDeleteSql("DROP TABLE " + dataTable);
				}
				if (inGeoframeDb.hasTable(stationTable)) {
					if (inGeoframeDb instanceof GeopackageCommonDb gpDb) {
						gpDb.deleteGeoTable(stationTable);
					} else {
						inGeoframeDb.executeInsertUpdateDeleteSql("DROP TABLE " + stationTable);
					}
				}
			}

			// make sure the output tables exist
			if (!inGeoframeDb.hasTable(GeoFrameSimpleTable.VARIABLE.getSchema().getSQLName())) {
				String sql = GeoFrameSimpleTable.VARIABLE.getSchema().createTableSql();
				inGeoframeDb.executeInsertUpdateDeleteSql(sql);
				String insertSql = GeoFrameSimpleTable.VARIABLE.getSchema().buildInsertAll();

				List<EnvironmentalVariable> fixedEnviramentalVariables = TableUtils
						.getFixedEnviramentalVariable(timeResolution);
				for (EnvironmentalVariable var : fixedEnviramentalVariables) {
					inGeoframeDb.executeInsertUpdateDeletePreparedSql(insertSql,
							new Object[] { var.varId(), var.name(), var.unit(), var.description() });
				}
			}
			if (!inGeoframeDb.hasTable(GeoFrameSimpleTable.STATIONDATA.getSchema().getSQLName())) {
				String sql = GeoFrameSimpleTable.STATIONDATA.getSchema().createTableSql();
				inGeoframeDb.executeInsertUpdateDeleteSql(sql);
			}

			if (inMeasurementsPointFilePath != null) {
				SimpleFeatureCollection basinsFC = null;
				if (inGeoframeDb.hasTable(GeoFrameSimpleTable.BASINDATA.getSchema().getSQLName())) {
					basinsFC = SpatialDbsImportUtils.tableToFeatureFCollection(inGeoframeDb,
							GeoFrameGeoTable.BASIN.getSchema().getSQLName(), -1, -1, null, null);
				}
				SimpleFeatureCollection inMeasurementPoints = getVector(inMeasurementsPointFilePath);
				ids = new int[inMeasurementPoints.size()];
				var builder = GeoFrameGeoTable.HYDRO_METEO_STATION.getSchema()
						.getSFBuilder(inMeasurementPoints.getSchema().getCoordinateReferenceSystem());
				DefaultFeatureCollection outFC = new DefaultFeatureCollection();
				int i = 0;

				try (SimpleFeatureIterator iterator = inMeasurementPoints.features()) {
					while (iterator.hasNext()) {
						SimpleFeature sourceFeature = iterator.next();
						Geometry geom = (Geometry) sourceFeature.getDefaultGeometry();
						Object idObj =  sourceFeature.getAttribute(inIdField);
						int id;
						if (idObj instanceof Number num) {
							id = num.intValue();
						} else {
							continue;
						}
						Double elevation = null;
						if (inElevationField != null) {
							elevation = (Double) sourceFeature.getAttribute(inElevationField);
						}
						builder.reset();
						builder.set(Station.GEOM.columnName(), geom);
						builder.set(Station.ID.columnName(), id);
						builder.set(Station.ELEVATION.columnName(), elevation);
						Integer basinId = this.getIntersectedBAsinId(basinsFC, geom,
								BasinMultiPolygonField.BASIN_ID.columnName());

						if (basinId != null && isStreamGauge) {
							String sql = String.format(
								    "UPDATE %s SET %s = %d WHERE %s = %d",
								    GeoFrameGeoTable.BASIN.tableName(),
								    BasinMultiPolygonField.ID.columnName(),
								    id,
								    BasinMultiPolygonField.BASIN_ID.columnName(),
								    basinId
								);
							inGeoframeDb.executeInsertUpdateDeleteSql(sql);
						}

						builder.set(Station.BASIN_ID.columnName(), basinId); // basin_id
						builder.set(Station.TYPE.columnName(), stationType.name()); // type

						SimpleFeature newFeature = builder.buildFeature(null);
						outFC.add(newFeature);
						ids[i] = id;
						i++;
					}
				}
				if (!inGeoframeDb.hasTable(stationTable)) {
					SpatialDbsImportUtils.createTableFromSchema(inGeoframeDb, outFC.getSchema(), stationTable, null,
							false);
				}

				SpatialDbsImportUtils.importFeatureCollection(inGeoframeDb, outFC, stationTable, -1, false, pm);

			} else {
				QueryResult result = inGeoframeDb.getTableRecordsMapFromRawSql("select * from "
						+ GeoFrameGeoTable.HYDRO_METEO_STATION.tableName() + " where type='" + stationType.name() + "'",
						-1);
				int idIndex = result.names.indexOf(Station.ID.columnName());

				var rows = result.data;
				int l = result.data.size();
				ids = new int[l];
				for (int i = 0; i < l; i++) {
					ids[i] = ((Number) rows.get(i)[idIndex]).intValue();
				}
			}
			if (inMeasurementDataFilePath != null) {
				var reader = new OmsTimeSeriesIteratorReader();
				var formatter = HMConstants.utcDateFormatterYYYYMMDDHHMM;
				reader.file = inMeasurementDataFilePath;
				reader.idfield = "ID";
				reader.tStart = inStartDate;
				reader.tEnd = inEndDate;
				reader.fileNovalue = "-9999";
				// TODO this is the right way for hourly and daily but not for weekly or and
				// yearly
				reader.tTimestep = timeResolution.toMinutes();
				reader.initProcess();

				String insertSql = GeoFrameSimpleTable.STATIONDATA.getSchema().buildInsertAll();
				int[] _ids = ids;
				inGeoframeDb.execOnConnection(conn -> {
					boolean autoCommit = conn.getAutoCommit();
					conn.setAutoCommit(false);
					try (IHMPreparedStatement pStmt = conn.prepareStatement(insertSql)) {
						while (reader.doProcess) {
							reader.nextRecord();
							HashMap<Integer, double[]> values = reader.outData;
							for (int id : _ids) {
								double[] data = values.get(id);
								if (data != null) {
									long ts = formatter.parseMillis(reader.tCurrent);
									pStmt.setLong(1, ts);
									pStmt.setInt(2, id);
									pStmt.setInt(3, inVariableType);
									pStmt.setDouble(4, data[0]);
									pStmt.addBatch();
								}
							}
						}
						pStmt.executeBatch();
						conn.commit();
						conn.setAutoCommit(autoCommit);
					}
					return null;
				});

				reader.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// TODO check on CRS??? first attempt with filter but doesn't work
	private Integer getIntersectedBAsinId(SimpleFeatureCollection basinsFC, Geometry station, String idFiledName) {
		if (basinsFC != null && station != null && idFiledName != null) {
			try (SimpleFeatureIterator it = basinsFC.features()) {
				while (it.hasNext()) {
					SimpleFeature basin = it.next();
					Geometry basinGeom = (Geometry) basin.getDefaultGeometry();
					if (basinGeom == null) {
						continue;
					}
					boolean covers = basinGeom.covers(station);
					Object idValue = basin.getAttribute(idFiledName);

					if (covers) {
						if (idValue == null) {
							return null;
						}
						if (idValue instanceof Number number) {
							return number.intValue();
						} else {
							return Integer.parseInt(idValue.toString());
						}
					}
				}
			}
		}
		return null;
	}

}
