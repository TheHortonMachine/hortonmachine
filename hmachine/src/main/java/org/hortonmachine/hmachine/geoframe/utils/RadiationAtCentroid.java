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
import org.hortonmachine.hmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.hmachine.geoframe.io.database.TableUtils;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameGeoTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameSimpleTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.BasinSchema.BasinCentroidField;
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

@Description("Populate the db with radiation data at centroid")
@Author(name = "Daniele Andreis")
@Keywords("time series, iterator, basin, value, database")
@Name("VariableEvaluatorAtCentroid")
@Status(40)
@UI(HMConstants.HIDE_UI_HINT)
@License("General Public License Version 3 (GPLv3)")
public class RadiationAtCentroid extends HMModel {
	@Description("Input database path")
	@UI(HMConstants.FILEIN_UI_HINT_DBF)
	@In
	public String inGeoframeDBPath = null;
	public double x = 0.7;
	public double y = 5.95;
	public double epsilon = 0.98;
	public double aCloud = 0;
	public double bCloud = 1;
	public boolean doHourly = true;
	public double alpha = 0.26;
	public double pCmO3 = 0.6;
	public double pAlphagp = 0.9;
	public double visibility = 80;
	public String tStartDate;
	public String lwrvModeel;
	public String dem;

	public String inSkyview;
	@Description("reader")
	@In
	public GeoframeEnvDatabaseIterator variableReader;
	private Lwrb lwrb = new Lwrb();
	private ShortwaveRadiationBalancePointCase swrb = new ShortwaveRadiationBalancePointCase();
	private NetRadiationPointCase nrpc = new NetRadiationPointCase();
	private ASpatialDb inGeoframeDb;
	private int timestepIndex = 0;
	private IHMConnection conn;
	private IHMPreparedStatement ps;

	@Initialize
	public void init() throws Exception {
		if (tStartDate == null || tStartDate.isEmpty() || lwrvModeel == null || lwrvModeel.isEmpty()) {
			throw new IllegalArgumentException();
		}

		if (!(inGeoframeDb.hasTable(GeoFrameGeoTable.BASIN_POINT.tableName())
				&& inGeoframeDb.hasTable(GeoFrameSimpleTable.HYDROMETEO.tableName())
				&& inGeoframeDb.hasTable(GeoFrameSimpleTable.RAW_METEO.tableName()))) {
			throw new DataSourceException("no suitable tables are present in db check");
		}

		inGeoframeDb = EDb.GEOPACKAGE.getSpatialDb();
		inGeoframeDb.open(inGeoframeDBPath);
		var inPoint = SpatialDbsImportUtils.tableToFeatureFCollection(inGeoframeDb,
				GeoFrameGeoTable.BASIN_POINT.getSchema().getSQLName(), -1, -1, null, null);

		var skyview = getRaster(inSkyview);
		lwrb.X = x;
		lwrb.Y = y;
		lwrb.epsilonS = epsilon;
		lwrb.A_Cloud = aCloud;
		lwrb.B_Cloud = bCloud;
		lwrb.model = lwrvModeel;
		lwrb.fStationsid = BasinCentroidField.BASIN_ID.columnName();
		lwrb.inStations = inPoint;
		lwrb.inSkyview = skyview;

		List<Integer> ids = new ArrayList<>();

		try (SimpleFeatureIterator it = inPoint.features()) {
			while (it.hasNext()) {
				SimpleFeature feature = it.next();

				Integer id = ((Number) feature.getAttribute(BasinCentroidField.BASIN_ID.columnName())).intValue();
				ids.add(id);
			}
		}

		int[] basinsId = ids.stream().mapToInt(Integer::intValue).toArray();

		HashMap<Integer, double[]> inNan = TableUtils.getLegacyHMInputNaN(basinsId);
		lwrb.inClearnessIndexValues = inNan;
		lwrb.inHumidityValues = inNan;

		swrb.doHourly = doHourly;
		swrb.fStationsid = BasinCentroidField.BASIN_ID.columnName();
		swrb.inStations = inPoint;
		swrb.inDem = getRaster(dem);
		swrb.inSkyview = skyview;
		swrb.inHumidityValues = inNan;
		nrpc.alfa = alpha;
	}

	@Execute
	public void process() throws Exception {

		if (variableReader.isPreCachingMode()) {
			double[] variableData = variableReader.getCached(timestepIndex);
			while (variableData != null) {
				var h = TableUtils.getLegacyHMInput(variableData, inGeoframeDb);
				processTimestep(h, variableReader.currentT);
				timestepIndex++;
				variableData = variableReader.getCached(timestepIndex);

			}
		} else {
			while (variableReader.next()) {

				double[] variableData = variableReader.outData;
				var h = TableUtils.getLegacyHMInput(variableData, inGeoframeDb);

				processTimestep(h, variableReader.currentT);
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

	private void processTimestep(HashMap<Integer, double[]> h, long t) {
		lwrb.inAirTemperatureValues = h;
		lwrb.inSoilTempratureValues = h;
		swrb.inTemperatureValues = h;
		try {
			lwrb.process();

			swrb.process();
			nrpc.inShortwaveValues = swrb.outHMtotal;
			nrpc.inDownwellingValues = lwrb.outHMlongwaveDownwelling;
			nrpc.inUpwellingValues = lwrb.outHMlongwaveUpwelling;
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
