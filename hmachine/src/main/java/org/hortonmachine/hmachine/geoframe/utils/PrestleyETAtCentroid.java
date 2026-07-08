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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_pAlpha_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_pGmorn_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSPRESTEYTAYLORETPMODEL_pGnight_DESCRIPTION;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.geotools.api.data.DataSourceException;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.geoframe.core.TopologyNode;
import org.hortonmachine.hmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameGeoTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameSimpleTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.EnvironmentalVariableType;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.etp.OmsPresteyTaylorEtpModel;

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

@Description("Populate the db with hydrometeo data at centroid")
@Author(name = "Daniele Andreis")
@Keywords("time series, iterator, basin, value, database")
@Name("VariableEvaluatorAtCentroid")
@Status(40)
@UI(HMConstants.HIDE_UI_HINT)
@License("General Public License Version 3 (GPLv3)")
public class PrestleyETAtCentroid extends HMModel {

	// TODO it's betetr the string (to use also in OMS console or the db object)
	@Description("Input database path")
	@UI(HMConstants.FILEIN_UI_HINT_DBF)
	@In
	public String inGeoframeDBPath = null;

	@Description("reader")
	@In
	public GeoframeEnvDatabaseIterator inNetReader;

	@Description("reader")
	@In
	public GeoframeEnvDatabaseIterator inTempReader;

	@Description("reader")
	@In
	public GeoframeEnvDatabaseIterator inPressurReader;

	@In
	public boolean isHourly;

	@Description(OMSPRESTEYTAYLORETPMODEL_pAlpha_DESCRIPTION)
	@In
	@Unit("m")
	public double pAlpha = 0;

	@Description(OMSPRESTEYTAYLORETPMODEL_pGmorn_DESCRIPTION)
	@In
	public double pGmorn = 0;

	@Description(OMSPRESTEYTAYLORETPMODEL_pGnight_DESCRIPTION)
	@In
	public double pGnight = 0;
	private ASpatialDb inGeoframeDb = null;

	private IHMPreparedStatement ps = null;

	private IHMConnection conn;

	private int timestepIndex = 0;

	private int threadPoolSize = 4;

	private TopologyNode rootNode;

	@Initialize
	public void init() {

		if (inGeoframeDBPath == null) {
			throw new IllegalArgumentException();
		}

		// make sure the output table exists
		try {
			inGeoframeDb = EDb.GEOPACKAGE.getSpatialDb();
			inGeoframeDb.open(inGeoframeDBPath);

			check();
		} catch (Exception e) {
		}
	}

	@Execute
	public void process() throws Exception {
		// TODO for now I not implements the radiation part we have to choose te model.
		// I have use for ET0 a modle from hortonmachin (but other model are similar)
		// for radiation seesms me thatr models are not the same.
		rootNode = TopologyUtilities.getRootNodeFromDb(inGeoframeDb);

		if (inNetReader.isPreCachingMode()) {
			double[] inNetData = inNetReader.getCached(timestepIndex);
			double[] tempData = inTempReader.getCached(timestepIndex);
			double[] pressureData = null;
			if (inPressurReader != null && inPressurReader.isPreCachingMode()) {
				pressureData = inPressurReader.getCached(timestepIndex);
			}
			// double[] pressureData = pressureReader.getCached(timestepIndex);
			long timestep = inTempReader.getCachedTimestamp(timestepIndex);
			while (inNetData != null && tempData != null) {

				processTimestep(inNetData, tempData, pressureData, timestep);
				timestepIndex++;
				inNetData = inNetReader.getCached(timestepIndex);
				tempData = inTempReader.getCached(timestepIndex);

				if (inPressurReader != null && inPressurReader.isPreCachingMode()) {
					pressureData = inPressurReader.getCached(timestepIndex);
				} else {
					pressureData = null;
				}
				timestep = inTempReader.getCachedTimestamp(timestepIndex);

			}
		} else {
			while (inNetReader.next() && inTempReader.next()) {

				double[] inNetData = inNetReader.outData;
				double[] tempData = inTempReader.outData;
				// double[] pressureData = pressureReader.getCached(timestepIndex);
				double[] pressureData = null;
				if (inPressurReader != null && inPressurReader.next()) {
					pressureData = inTempReader.outData;
				}

				processTimestep(inNetData, tempData, pressureData, inTempReader.currentT);
			}
		}

	}

	private void processTimestep(double[] inNetMap, double[] temperatureMap, double[] pressureMap, long t)
			throws Exception {
		String pTs = inNetReader.tCurrent;

		// reset nodes values
		rootNode.visitUpstream(node -> {
			node.value = Double.NaN;
			node.accumulatedValue = Double.NaN;
		});

		Set<TopologyNode> nodes = new HashSet<>();

		// do parallel processing with threadPoolSize threads
		TopologyNode.collectAllUpstreamRecursive(rootNode, nodes);

		ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
		CountDownLatch latch = new CountDownLatch(nodes.size());
		for (TopologyNode node : nodes) {
			executor.submit(() -> {
				try {
					try {
						processTimestepForBasin(inNetMap, temperatureMap, pressureMap, node, t);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} finally {
					latch.countDown();
				}
			});
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while waiting for basin parallel operation to finish", e);
		} finally {
			executor.shutdown();
		}

	}

	private void processTimestepForBasin(double[] inNetMap, double[] temperatureMap, double[] pressureMap,
			TopologyNode node, long t) throws SQLException, Exception {
		int basinId = node.basinId;
		double net = inNetMap != null ? inNetMap[basinId] : -9999.0;

		double temperature = temperatureMap != null ? temperatureMap[basinId] : -9999.0;
		double pressure = pressureMap != null ? pressureMap[basinId] : -9999.0;
		double et0 = OmsPresteyTaylorEtpModel.getET(pGmorn, pGnight, pAlpha, net,
				OmsPresteyTaylorEtpModel.DEFAULT_HOURLY_NET_RADIATION, temperature,
				OmsPresteyTaylorEtpModel.DEFAULT_TEMPERATURE, pressure, OmsPresteyTaylorEtpModel.DEFAULT_PRESSURE,
				isHourly, inNetReader.tCurrent);

		try {
			// insert(EnvironmentalVariableType.RADIATION.getId(), basinId,
			// variableReader.currentT, radiation);
			String insertSql = GeoFrameSimpleTable.HYDROMETEO.getSchema().buildInsertAll();
			inGeoframeDb.execOnConnection(conn -> {
				boolean autoCommit = conn.getAutoCommit();
				conn.setAutoCommit(false);
				try (IHMPreparedStatement pStmt = conn.prepareStatement(insertSql)) {
					pStmt.setLong(1, t);
					pStmt.setInt(2, basinId);
					pStmt.setInt(3, EnvironmentalVariableType.EVAPOTRANSPIRATION.getId());
					pStmt.setDouble(4, et0);
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
							.withZone(ZoneId.of("Europe/Rome"));
					Instant instant = Instant.ofEpochMilli(t);
					String data = formatter.format(instant);
					System.out.println("date" + data + "badin " + basinId + "value " + et0);
					pStmt.addBatch();

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
			if (inNetReader == null || inTempReader == null) {
				throw new DataSourceException("no data reader for netradiation or temperature, check files name");
			}
			if (!inGeoframeDb.hasTable(GeoFrameSimpleTable.HYDROMETEO.getSchema().getSQLName())) {
				String sql = GeoFrameSimpleTable.HYDROMETEO.getSchema().createTableSql();
				inGeoframeDb.executeInsertUpdateDeleteSql(sql);
			}
			if (!inGeoframeDb.hasTable(GeoFrameGeoTable.BASIN.tableName())) {
				throw new DataSourceException("no suitable tables are present in db check");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



}
