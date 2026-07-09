package org.hortonmachine.hmachine.geoframe.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.api.data.DataSourceException;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils;
import org.hortonmachine.gears.utils.time.UtcTimeUtilities;
import org.hortonmachine.hmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.hmachine.geoframe.io.database.TableUtils;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameGeoTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameSimpleTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.BasinPolygonSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.BasinPolygonSchema.BasinMultiPolygonField;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.EnvironmentalVariableType;
import org.hortonmachine.hmachine.geoframe.utils.radiation.NetRadiationPointCase;
import org.hortonmachine.hmachine.geoframe.utils.radiation.LwrbPointCase.Lwrb;
import org.hortonmachine.hmachine.geoframe.utils.radiation.swrbPointCase.ShortwaveRadiationBalancePointCase;

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
import oms3.annotations.Unit;

@Description("Populate the db with radiation data at centroid")
@Author(name = "Daniele Andreis")
@Keywords("time series, iterator, basin, value, database")
@Name("VariableEvaluatorAtCentroid")
@Status(40)
@UI(HMConstants.HIDE_UI_HINT)
@License("General Public License Version 3 (GPLv3)")

//TODO this is a simple example to improve
public class RadiationAtCentroid extends HMModel {
	@Description("Input database path")
	@UI(HMConstants.FILEIN_UI_HINT_DBF)
	@In
	public String inGeoframeDBPath = null;
	@Description("X parameter of the literature formulation")
	@In
	public double x = 0.7;
	@Description("Y parameter of the literature formulation")
	@In
	public double y = 5.95;
	// TODO check
	@Description("Y parameter of the literature formulation")
	@In
	public double z = 0;

	@Description("Soil emissivity")
	@Unit("-")
	@In
	public double epsilonS = 0.98;
	@Description("Coefficient to take into account the cloud cover," + "set equal to 0 for clear sky conditions ")
	@In
	public double aCloud = 0;
	@Description("Exponent  to take into account the cloud cover," + "set equal to 1 for clear sky conditions")
	@In
	public double bCloud = 1;
	public boolean doHourly = true;
	public double alpha = 0.26;
	@Description("Ozone layer thickness in cm")
	@In
	public double pCmO3 = 0.6;
	@Description("The soil albedo.")
	@In
	public double pAlphagp = 0.9;

	@Description(" For aerosol attenuation (5 < vis < 180 Km) [km].")
	@In
	@Unit("km")
	public double visibility = 80;

	@Description("String containing the number of the model: " + " 1: Angstrom [1918];" + " 2: Brunt's [1932];"
			+ " 3: Swinbank [1963];" + " 4: Idso and Jackson [1969];" + " 5: Brutsaert [1975];" + " 6: Idso [1981];"
			+ " 7: Monteith and Unsworth [1990];" + " 8: Konzelman [1994];" + " 9: Prata [1996];"
			+ " 10: Dilley and O'Brien [1998];" + " 11: To be implemented")
	@In
	public String lwrvModeel;

	@Description("The path to the map of the Digital Elevation Model")
	@In
	public String dem;
	@Description("The path to the skyviewfactor")
	@In
	public String inSkyview;
	@Description("reader")
	@In
	public GeoframeEnvDatabaseIterator inTemperatureReader;
	@Description("reader")
	@In
	public GeoframeEnvDatabaseIterator inHumifidtyReader = null;
	@Description("reader")
	@In
	public GeoframeEnvDatabaseIterator inClearSkyReader;

	private Lwrb lwrb;
	private ShortwaveRadiationBalancePointCase swrb;
	private NetRadiationPointCase nrpc = new NetRadiationPointCase();
	private ASpatialDb inGeoframeDb;
	private int timestepIndex = 0;
	private HashMap<Integer, double[]> inNan;

	@Initialize
	public void init() throws Exception {
		if (lwrvModeel == null || lwrvModeel.isEmpty()) {
			throw new IllegalArgumentException();
		}
		if (inGeoframeDBPath == null) {
			throw new IllegalArgumentException();
		}

		inGeoframeDb = EDb.GEOPACKAGE.getSpatialDb();
		inGeoframeDb.open(inGeoframeDBPath);

		swrb = new ShortwaveRadiationBalancePointCase();
		if (!(inGeoframeDb.hasTable(GeoFrameGeoTable.BASIN.tableName())
				&& inGeoframeDb.hasTable(GeoFrameSimpleTable.BASINDATA.tableName())
				&& inGeoframeDb.hasTable(GeoFrameSimpleTable.STATIONDATA.tableName()))) {
			throw new DataSourceException("no suitable tables are present in db check");
		}

		if (!inGeoframeDb.hasTable(GeoFrameSimpleTable.BASINDATA.getSchema().getSQLName())) {
			String sql = GeoFrameSimpleTable.BASINDATA.getSchema().createTableSql();
			inGeoframeDb.executeInsertUpdateDeleteSql(sql);
		}
		lwrb = new Lwrb();
		;
		var inBasinsFC = SpatialDbsImportUtils.tableToFeatureFCollection(inGeoframeDb,
				GeoFrameGeoTable.BASIN.getSchema().getSQLName(), -1, -1, null, null);

		var skyview = getRaster(inSkyview);
		lwrb.X = x;
		lwrb.Y = y;
		lwrb.Z = z;

		lwrb.epsilonS = epsilonS;
		lwrb.aCloud = aCloud;
		lwrb.bCloud = bCloud;
		lwrb.model = lwrvModeel;
		lwrb.fStationsID = BasinMultiPolygonField.BASIN_ID.columnName();
		lwrb.inStationsFC = inBasinsFC;
		lwrb.inSkyviewGC = skyview;

		List<Integer> ids = new ArrayList<>();

		try (SimpleFeatureIterator it = inBasinsFC.features()) {
			while (it.hasNext()) {
				SimpleFeature feature = it.next();

				Integer id = ((Number) feature.getAttribute(BasinMultiPolygonField.BASIN_ID.columnName())).intValue();
				ids.add(id);
			}
		}

		int[] basinsId = ids.stream().mapToInt(Integer::intValue).toArray();

		inNan = TableUtils.getLegacyHMInputNaN(basinsId);

		swrb.doHourly = doHourly;
		swrb.fStationsid = BasinMultiPolygonField.BASIN_ID.columnName();
		swrb.inStationsFC = inBasinsFC;
		swrb.inDem = getRaster(dem);
		swrb.inSkyview = skyview;
		swrb.pAlphag = pAlphagp;
		swrb.pCmO3 = pCmO3;
		swrb.pVisibility = visibility;
		nrpc.alfa = alpha;

	}

	@Execute
	public void process() throws Exception {
		int[] ids = TableUtils.getIntIdArray(inGeoframeDb, GeoFrameGeoTable.BASIN.tableName(),
				BasinPolygonSchema.BasinMultiPolygonField.BASIN_ID.columnName(), null);
		pm.beginTask("Processing radiation data...", -1);
		int iteration = 0;
		if (inTemperatureReader.isPreCachingMode()) {
			double[] variableData = inTemperatureReader.getCached(timestepIndex);
			long timestep = inTemperatureReader.getCachedTimestamp(timestepIndex);

			while (variableData != null) {
				var h = TableUtils.getLegacyHMInput(variableData, ids);
				HashMap<Integer, double[]> humidity = inNan;
				if (inHumifidtyReader != null && inHumifidtyReader.isPreCachingMode()) {
					double[] humidityData = inHumifidtyReader.getCached(timestepIndex);
					humidity = TableUtils.getLegacyHMInput(humidityData, ids);
				}

				HashMap<Integer, double[]> clearSky = inNan;

				if (inClearSkyReader != null && inClearSkyReader.isPreCachingMode()) {
					double[] clearSkyData = inClearSkyReader.getCached(timestepIndex);
					clearSky = TableUtils.getLegacyHMInput(clearSkyData, ids);
				}
				if (iteration++ % 1000 == 0) {
					pm.message("Processing timestep " + UtcTimeUtilities.quickToString(timestep) + "...");
				}
				processTimestep(h, humidity, clearSky, timestep);
				timestepIndex++;
				variableData = inTemperatureReader.getCached(timestepIndex);
				timestep = inTemperatureReader.getCachedTimestamp(timestepIndex);

			}
		} else {
			while (inTemperatureReader.next()) {
				double[] variableData = inTemperatureReader.outData;
				var temperature = TableUtils.getLegacyHMInput(variableData, ids);

				HashMap<Integer, double[]> humidity = inNan;
				if (inHumifidtyReader != null) {
					double[] humidityData = inHumifidtyReader.outData;
					humidity = TableUtils.getLegacyHMInput(humidityData, ids);
				}
				HashMap<Integer, double[]> clearSky = inNan;
				if (inClearSkyReader != null) {
					double[] clearSkyData = inClearSkyReader.outData;
					clearSky = TableUtils.getLegacyHMInput(clearSkyData, ids);
				}
				lwrb.inHumidityValuesHM = inNan;
				swrb.inHumidityValues = inNan;

				if (iteration++ % 1000 == 0) {
					pm.message("Processed timestep " + UtcTimeUtilities.quickToString(inTemperatureReader.currentT)
							+ "...");
				}
				processTimestep(temperature, humidity, clearSky, inTemperatureReader.currentT);

			}
		}
		pm.done();

	}

	private void processTimestep(HashMap<Integer, double[]> temperature, HashMap<Integer, double[]> humidity,
			HashMap<Integer, double[]> clearSky, long t) throws Exception {
		lwrb.inAirTemperatureValuesHM = temperature;
		lwrb.inSoilTempratureValuesHM = temperature;
		lwrb.inHumidityValuesHM = humidity;
		lwrb.inClearnessIndexValuesHM = clearSky;
		swrb.inHumidityValues = humidity;
		swrb.inTemperatureValues = temperature;

		swrb.tCurrentDateString = GeoframeEnvDatabaseIterator.ts2str(t);
		lwrb.process();

		swrb.process();
		nrpc.inShortwaveValues = swrb.outHMtotal;
		nrpc.inDownwellingValues = lwrb.outHMlongwaveDownwellingHM;
		nrpc.inUpwellingValues = lwrb.outHMlongwaveUpwellingHM;
		nrpc.process();
		HashMap<Integer, double[]> out = nrpc.outHMnetRad;
		String insertSql = GeoFrameSimpleTable.BASINDATA.getSchema().buildInsertAll();
		inGeoframeDb.execOnConnection(conn -> {
			boolean autoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			try (IHMPreparedStatement pStmt = conn.prepareStatement(insertSql)) {
				for (Map.Entry<Integer, double[]> entry : out.entrySet()) {
					int basinId = entry.getKey();
					double value = entry.getValue()[0];
					pStmt.setLong(1, t);
					pStmt.setInt(2, basinId);
					pStmt.setInt(3, EnvironmentalVariableType.RADIATION.getId());
					pStmt.setDouble(4, value);
					pStmt.addBatch();
				}
				pStmt.executeBatch();
				conn.commit();
				conn.setAutoCommit(autoCommit);
			}
			return null;
		});

	}

}
