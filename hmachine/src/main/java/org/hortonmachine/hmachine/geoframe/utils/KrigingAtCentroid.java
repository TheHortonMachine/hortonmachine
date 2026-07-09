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

package org.hortonmachine.hmachine.geoframe.utils;

import java.util.HashMap;
import java.util.Map;

import org.geotools.api.data.DataSourceException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.libs.monitor.LogProgressMonitor;
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils;
import org.hortonmachine.hmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.hmachine.geoframe.io.database.TableUtils;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameGeoTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameSimpleTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.BasinPolygonSchema.BasinMultiPolygonField;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.StationSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.StationSchema.Station;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.StationSchema.StationType;
import org.hortonmachine.hmachine.modules.statistics.kriging.pointcase.KrigingPointCase;
import org.hortonmachine.hmachine.modules.statistics.kriging.primarylocation.StationsSelection;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.SingleStepVariogramEvaluator;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Initialize;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description("Populate the db with hydrometeo data at centroid")
@Author(name = "Daniele Andreis")
@Keywords("time series, iterator, basin, value, database")
@Name("GeoFrameRawDataImporter")
@Status(40)
@UI(HMConstants.HIDE_UI_HINT)
@License("General Public License Version 3 (GPLv3)")
public class KrigingAtCentroid extends HMModel {

	// TODO it's betetr the string (to use also in OMS console or the db object)
	@Description("Input database path")
	@UI(HMConstants.FILEIN_UI_HINT_VECTOR)
	@In
	public String inGeoframeDBPath = null;

	@In
	public int inVariableType = -1;

	@Description("reader")
	@In
	public GeoframeEnvDatabaseIterator variableReader;

	@In
	public boolean doLogarithmic = false;

	// @In
	private String variogramType;

	@Description("The progress monitor.")
	@In
	public IHMProgressMonitor pm = new LogProgressMonitor();

	@Description("Include zeros in computations (default is true).")
	@In
	public boolean doIncludeZero = true;

	@Description("Include zeros in computations (default is true).")
	@In
	public int cutoffDivide = 0;

	@Description("Switch for detrended mode.")
	@In
	public boolean doDetrended = false;

	@Description("Specified cutoff")
	@In
	public double cutoffInput = 0.0;

	public boolean doOverWrite = false;

	public boolean boundToZero = false;

	private ASpatialDb inGeoframeDb = null;

	private int timestepIndex = 0;

	@Initialize
	public void init() {

		if (inGeoframeDBPath == null) {
			throw new IllegalArgumentException();
		}
		try {
			inGeoframeDb = EDb.GEOPACKAGE.getSpatialDb();
			inGeoframeDb.open(inGeoframeDBPath);
			check();

			// make sure the output table exists
			if (!inGeoframeDb.hasTable(GeoFrameSimpleTable.BASINDATA.getSchema().getSQLName())) {
				String sql = GeoFrameSimpleTable.BASINDATA.getSchema().createTableSql();
				inGeoframeDb.executeInsertUpdateDeleteSql(sql);
			}
		} catch (Exception e) {
		}
	}

	@Execute
	public void process() throws Exception {
		HashMap<Integer, double[]> h = null;

		SimpleFeatureCollection inStations = SpatialDbsImportUtils.tableToFeatureFCollection(inGeoframeDb,
				GeoFrameGeoTable.HYDRO_METEO_STATION.getSchema().getSQLName(), -1, -1, null,
				Station.TYPE.columnName() + "='" + StationType.METEO + "'");
		var stations = new StationsSelection();

		stations.inStations = inStations;
		stations.doIncludezero = doIncludeZero;
		stations.fStationsid = Station.ID.columnName();
		stations.fStationsZ = Station.ELEVATION.columnName();
		stations.doLogarithmic = doLogarithmic;
		stations.pm = new DummyProgressMonitor();

		int[] ids = TableUtils.getIntIdArray(inGeoframeDb, GeoFrameGeoTable.HYDRO_METEO_STATION.tableName(),
				StationSchema.Station.ID.columnName()," where type='" + StationType.METEO + "'");
		if (variableReader.isPreCachingMode()) {
			double[] variableData = variableReader.getCached(timestepIndex);
			long timestep = variableReader.getCachedTimestamp(timestepIndex);
			while (variableData != null) {
				h = TableUtils.getLegacyHMInput(variableData, ids);

				processTimestep(variableData, stations, inStations, h, timestep);
				timestepIndex++;
				variableData = variableReader.getCached(timestepIndex);
				timestep = variableReader.getCachedTimestamp(timestepIndex);

			}
		} else {
			while (variableReader.next()) {

				double[] variableData = variableReader.outData;
				h = TableUtils.getLegacyHMInput(variableData, ids);

				processTimestep(variableData, stations, inStations, h, variableReader.currentT);
			}
		}

	}

	private void processTimestep(double[] variableMap, StationsSelection stations, SimpleFeatureCollection inStations,
			HashMap<Integer, double[]> h, long currentT) throws Exception {

		var variogram = SingleStepVariogramEvaluator.createVariogram(stations, h, doDetrended, doIncludeZero,
				doLogarithmic, variogramType, cutoffDivide, cutoffInput);

		KrigingPointCase kriging = new KrigingPointCase();
		SimpleFeatureCollection inBasinsFC = SpatialDbsImportUtils.tableToFeatureFCollection(inGeoframeDb,
				GeoFrameGeoTable.BASIN.getSchema().getSQLName(), -1, -1, null, null);
		kriging.inStations = inStations;
		kriging.fStationsid = Station.ID.columnName();
		kriging.fStationsZ = Station.ELEVATION.columnName();
		kriging.inInterpolate = inBasinsFC;
		kriging.fInterpolateid = BasinMultiPolygonField.BASIN_ID.columnName();
		kriging.fPointZ = BasinMultiPolygonField.AVG_ELEVATION_M.columnName();

		kriging.inNumCloserStations = 8;
		kriging.doDetrended = doDetrended;
		kriging.doIncludeZero = doIncludeZero;
		kriging.boundedToZero = boundToZero;
		kriging.parallelComputation = true;

		kriging.inData = h;
		kriging.inTheoreticalVariogram = variogram;
		kriging.execute();

		try {
			String insertSql = GeoFrameSimpleTable.BASINDATA.getSchema().buildInsertAll();
			HashMap<Integer, double[]> out = kriging.outData;

			inGeoframeDb.execOnConnection(conn -> {
				boolean autoCommit = conn.getAutoCommit();
				conn.setAutoCommit(false);
				try (IHMPreparedStatement pStmt = conn.prepareStatement(insertSql)) {
					for (Map.Entry<Integer, double[]> entry : out.entrySet()) {
						int basinId = entry.getKey();
						double value = entry.getValue()[0];
						pStmt.setLong(1, currentT);
						pStmt.setInt(2, basinId);
						pStmt.setInt(3, inVariableType);
						pStmt.setDouble(4, value);
						pStmt.addBatch();
					}
					pStmt.executeBatch();
					conn.commit();
					conn.setAutoCommit(autoCommit);
				}
				return null;
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void check() {
		try {
			if (!(inGeoframeDb.hasTable(GeoFrameGeoTable.BASIN.tableName())
					&& inGeoframeDb.hasTable(GeoFrameGeoTable.HYDRO_METEO_STATION.tableName())
					&& inGeoframeDb.hasTable(GeoFrameSimpleTable.STATIONDATA.tableName()))) {
				throw new DataSourceException("no suitable tables are present in db check");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
