package org.hortonmachine.hmachine.geoframe.ermworkflow;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.geoframe.core.TopologyNode;
import org.hortonmachine.hmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.EnvironmentalVariableType;
import org.hortonmachine.hmachine.geoframe.utils.IWaterBudgetSimulationRunner;
import org.hortonmachine.hmachine.geoframe.utils.TopologyUtilities;
import org.hortonmachine.hmachine.geoframe.utils.WaterSimulationRunner;

import oms3.annotations.Description;
import oms3.annotations.In;
import oms3.annotations.UI;
import oms3.annotations.Unit;

public abstract class ErmBase extends HMModel {

	@Description("Geoframe database path (containing all data).")
	@UI(HMConstants.FILEIN_UI_HINT_VECTOR)
	@In
	public String inGeopackagePath;

	@Description("Simulation start timestamp [format: yyyy-MM-dd HH:mm:ss].")
	@In
	public String inFromTimestamp;

	@Description("Simulation end timestamp [format: yyyy-MM-dd HH:mm:ss].")
	@In
	public String inToTimestamp;

	@Description("Time step length (defaults to 60).")
	@Unit("min")
	@In
	public int pTimeStepMinutes = 60;

	@Description("Number of spin-up days (defaults to 365).")
	@Unit("days")
	@In
	public int pSpinUpDays = 365;

	@Description("If true, model state is written to the database at each time step.")
	@In
	public boolean doWriteState = false;

	protected ASpatialDb db;

	protected int maxBasinId;
	protected double[] basinAreas;
	protected TopologyNode rootNode;
	protected double[] observedDischarge;

	protected GeoframeEnvDatabaseIterator precipReader;
	protected GeoframeEnvDatabaseIterator tempReader;
	protected GeoframeEnvDatabaseIterator etpReader;

	protected IWaterBudgetSimulationRunner runner;
	protected int spinUpTimesteps;

	protected void setup() throws Exception {
		db = EDb.GEOPACKAGE.getSpatialDb();
		db.open(inGeopackagePath);

		maxBasinId = IWaterBudgetSimulationRunner.getMaxBasinId(db);
		basinAreas = IWaterBudgetSimulationRunner.getBasinAreas(db, maxBasinId);
		rootNode = TopologyUtilities.getRootNodeFromDb(db);
		observedDischarge = IWaterBudgetSimulationRunner.getObservedDischarge(db, rootNode, inFromTimestamp, inToTimestamp);

		var type = EnvironmentalVariableType.PRECIPITATION.getId();
		precipReader = makeReader(db, maxBasinId, type, inFromTimestamp, inToTimestamp);
		type = EnvironmentalVariableType.TEMPERATURE.getId();
		tempReader = makeReader(db, maxBasinId, type, inFromTimestamp, inToTimestamp);
		type = EnvironmentalVariableType.EVAPOTRANSPIRATION.getId();
		etpReader = makeReader(db, maxBasinId, type, inFromTimestamp, inToTimestamp);

		runner = new WaterSimulationRunner();
		spinUpTimesteps = (24 * 60 / pTimeStepMinutes) * pSpinUpDays;
	}

	protected void teardown() throws Exception {
		if (db != null)
			db.close();
	}

	private static GeoframeEnvDatabaseIterator makeReader(ADb db, int maxBasinId, int parameterId, String fromTS,
			String toTS) throws Exception {
		GeoframeEnvDatabaseIterator r = new GeoframeEnvDatabaseIterator();
		r.db = db;
		r.pMaxId = maxBasinId;
		r.pParameterId = parameterId;
		r.tStart = fromTS;
		r.tEnd = toTS;
		return r;
	}
}
