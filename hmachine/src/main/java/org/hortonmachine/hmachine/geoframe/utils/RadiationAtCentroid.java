package org.hortonmachine.hmachine.geoframe.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.geotools.api.data.DataSourceException;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils;
import org.hortonmachine.gears.utils.time.ETimeUtilities;
import org.hortonmachine.hmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.hmachine.geoframe.io.database.TableUtils;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameGeoTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameSimpleTable;
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

	public double aCloud = 0;
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
	public GeoframeEnvDatabaseIterator temperatureReader;
	@Description("reader")
	@In
	public GeoframeEnvDatabaseIterator humifidtyReader = null;
	@Description("reader")
	@In
	public GeoframeEnvDatabaseIterator clearSkyReader;

	private Lwrb lwrb = new Lwrb();
	private ShortwaveRadiationBalancePointCase swrb = new ShortwaveRadiationBalancePointCase();
	private NetRadiationPointCase nrpc = new NetRadiationPointCase();
	private ASpatialDb inGeoframeDb;
	private int timestepIndex = 0;
	private IHMConnection conn;
	private IHMPreparedStatement ps;
	private HashMap<Integer, double[]> inNan;

	@Initialize
	public void init() throws Exception {
		if (lwrvModeel == null || lwrvModeel.isEmpty()) {
			throw new IllegalArgumentException();
		}

		if (!(inGeoframeDb.hasTable(GeoFrameGeoTable.BASIN.tableName())
				&& inGeoframeDb.hasTable(GeoFrameSimpleTable.HYDROMETEO.tableName())
				&& inGeoframeDb.hasTable(GeoFrameSimpleTable.RAW_METEO.tableName()))) {
			throw new DataSourceException("no suitable tables are present in db check");
		}

		inGeoframeDb = EDb.GEOPACKAGE.getSpatialDb();
		inGeoframeDb.open(inGeoframeDBPath);
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
		nrpc.alfa = alpha;
	}

	@Execute
	public void process() throws Exception {

		if (temperatureReader.isPreCachingMode()) {
			double[] variableData = temperatureReader.getCached(timestepIndex);
			while (variableData != null) {
				var h = TableUtils.getLegacyHMInput(variableData, inGeoframeDb);
			//	processTimestep(h, temperatureReader.currentT);
				timestepIndex++;
				variableData = temperatureReader.getCached(timestepIndex);

			}
		} else {
			while (temperatureReader.next()) {

				double[] variableData = temperatureReader.outData;
				var temperature = TableUtils.getLegacyHMInput(variableData, inGeoframeDb);

				HashMap<Integer, double[]> humidity = inNan;
				if (humifidtyReader != null) {
					double[] humidityData = humifidtyReader.outData;
					humidity = TableUtils.getLegacyHMInput(humidityData, inGeoframeDb);
				}

				HashMap<Integer, double[]> clearSky = inNan;

				if (clearSkyReader != null) {
					double[] clearSkyData = clearSkyReader.outData;
					clearSky = TableUtils.getLegacyHMInput(clearSkyData, inGeoframeDb);
				}
				lwrb.inHumidityValuesHM = inNan;
				swrb.inHumidityValues = inNan;

				processTimestep(temperature, humidity, clearSky, temperatureReader.currentT);
			}
		}

	}

	private void ensureOpen() throws Exception {
		if (ps != null) {
			return;
		}
		// make sure the output table exists
		String sql = GeoFrameSimpleTable.HYDROMETEO.getSchema().createTableSql();
		inGeoframeDb.executeInsertUpdateDeleteSql(sql);
		String insertSql = GeoFrameSimpleTable.HYDROMETEO.getSchema().buildInsertAll();
		conn = inGeoframeDb.getConnectionInternal();
		ps = conn.prepareStatement(insertSql);
	}

	private void processTimestep(HashMap<Integer, double[]> temperature, HashMap<Integer, double[]> humidity,
			HashMap<Integer, double[]> clearSky, long t) {
		lwrb.inAirTemperatureValuesHM = temperature;
		lwrb.inSoilTempratureValuesHM = temperature;
		lwrb.inHumidityValuesHM = humidity;
		lwrb.inClearnessIndexValuesHM = clearSky;
		swrb.inHumidityValues = humidity;
		swrb.inTemperatureValues = temperature;

		swrb.tCurrentDateString = GeoframeEnvDatabaseIterator.ts2str(t);
		try {
			lwrb.process();

			swrb.process();
			nrpc.inShortwaveValues = swrb.outHMtotal;
			nrpc.inDownwellingValues = lwrb.outHMlongwaveDownwellingHM;
			nrpc.inUpwellingValues = lwrb.outHMlongwaveUpwellingHM;
			nrpc.process();
			var out = nrpc.outHMnetRad;

			for (Entry<Integer, double[]> entry : out.entrySet()) {
				Integer basinId = entry.getKey();
				double value = entry.getValue()[0];
				insert(t, basinId, value);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void insert(long currentT, int basinId, double value) throws Exception {
		ensureOpen();
		conn.enableAutocommit(false);
		ps.setLong(1, currentT);
		ps.setInt(2, basinId);
		ps.setInt(3, EnvironmentalVariableType.RADIATION.getId());
		ps.setDouble(4, value);

		ps.addBatch();
		ps.executeBatch();
		conn.commit();
		conn.enableAutocommit(true);
	}

}
