package org.hortonmachine.hmachine.geoframe.ermworkflow;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameSimpleTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.BasinDataSchema.BasinDataField;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.EnvironmentalVariableType;
import org.hortonmachine.hmachine.geoframe.utils.IWaterBudgetSimulationRunner;
import org.hortonmachine.hmachine.geoframe.utils.PrestleyETAtCentroid;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

/**
 * GeoFrame-ERM workflow step that computes potential evapotranspiration
 * (ET0) at each basin's centroid using the Priestley-Taylor (1972) method,
 * and persists it to the geoframe database.
 *
 * <p>
 * Physically it applies the Priestley-Taylor equilibrium evapotranspiration
 * formula (see
 * {@link org.hortonmachine.hmachine.modules.hydrogeomorphology.etp.OmsPresteyTaylorEtpModel#compute}):
 *
 * <pre>ET = alpha * Delta * (Rn - G) / ((gamma + Delta) * lambda)</pre>
 *
 * where Delta is the slope of the saturation vapor pressure curve at air
 * temperature, gamma is the psychrometric constant (derived from atmospheric
 * pressure, defaulted here since no pressure reader is wired in), lambda is
 * the latent heat of vaporization, Rn is net radiation, and G is the soil
 * heat flux, approximated as a simple fraction of Rn via {@link #pGmorn}
 * (daylight, 6h-18h) or {@link #pGnight} (night). {@link #pAlpha} is the
 * Priestley-Taylor coefficient (~1.26 for well-watered surfaces).
 */
@Description("Radiation calculator.")
@Author(name = "Daniele Andreis", contact = "")
@Keywords("ERM, GeoFrame, ET")
@Label("GeoFrame")
@Name("ermPrestleyEt")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class ErmPrestleyEt extends HMModel {
	@Description("Input geoframe data geopackage.")
	@UI(HMConstants.FILEIN_UI_HINT_VECTOR)
	@In
	public String inGpkg;

	@Description("Data import start timestamp in format yyyy-MM-dd HH:mm.")
	@In
	public String pStartTimestamp;

	@Description("Data import end timestamp in format yyyy-MM-dd HH:mm.")
	@In
	public String pEndTimestamp;

	@Description("If true, existing output files are overwritten.")
	@In
	public boolean doOverwrite = false;

	@Execute
	public void process() throws Exception {
		try (ASpatialDb db = EDb.GEOPACKAGE.getSpatialDb()) {
			db.open(inGpkg);

			if (doOverwrite) {
				db.executeInsertUpdateDeleteSql("DELETE FROM " + GeoFrameSimpleTable.BASINDATA.tableName() + " WHERE " + //
						BasinDataField.VAR_ID.columnName() + " = "
						+ VarSchema.EnvironmentalVariableType.EVAPOTRANSPIRATION.getId());
			}

			var temperatureReader = new GeoframeEnvDatabaseIterator();
			temperatureReader.db = db;
			temperatureReader.pParameterId = EnvironmentalVariableType.TEMPERATURE.getId();
			temperatureReader.pMaxId = IWaterBudgetSimulationRunner.getMaxBasinId(db);
			temperatureReader.tStart = pStartTimestamp + ":00";
			temperatureReader.tEnd = pEndTimestamp + ":00";
			temperatureReader.doRawData = false;
			temperatureReader.preCacheData();

			var netReader = new GeoframeEnvDatabaseIterator();
			netReader.db = db;
			netReader.pParameterId = EnvironmentalVariableType.RADIATION.getId();
			netReader.pMaxId = IWaterBudgetSimulationRunner.getMaxBasinId(db);
			netReader.tStart = pStartTimestamp + ":00";
			netReader.tEnd = pEndTimestamp + ":00";
			netReader.doRawData = false;
			netReader.preCacheData();

			var ptEt = new PrestleyETAtCentroid();
			ptEt.inGeoframeDB = db;
			ptEt.isHourly = true;
			ptEt.pAlpha = 1.26;
			ptEt.inTempReader = temperatureReader;
			ptEt.inNetReader = netReader;
			ptEt.pGmorn = 0.35;
			ptEt.pGnight = 0.75;
			ptEt.init();
			ptEt.process();

		}
	}

	public static void main(String[] args) throws Exception {
		new ErmPrestleyEt();
		String workspacePath = "/home/andreisd/Documents/project/data_hm/vermiglio_dtm/inputs/";
		ErmPrestleyEt ept = new ErmPrestleyEt();
		ept.inGpkg = workspacePath + "outputs/geoframe_data.gpkg";
		ept.pStartTimestamp = ErmCommonData.START_TIMESTAMP;
		ept.pEndTimestamp = ErmCommonData.END_TIMESTAMP;
		ept.doOverwrite = true;
		ept.process();
	}

}
