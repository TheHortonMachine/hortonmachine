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
import org.hortonmachine.hmachine.geoframe.utils.RadiationAtCentroid;

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
 * Simple launcher to get radiation.
 * 
 * It take all parameter possible as default and in clear square condition. Only
 * temperature are known no humidity and clearness index.
 */
@Description("Radiation calculator.")
@Author(name = "Daniele Andreis", contact = "")
@Keywords("ERM, GeoFrame, Kriging")
@Label("GeoFrame")
@Name("ermKriging")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class ErmRadiation extends HMModel {
	@Description("Input dtm.")
	@UI(HMConstants.FILEIN_UI_HINT_RASTER)
	@In
	public String inDtm;

	@Description("Input geoframe data geopackage.")
	@UI(HMConstants.FILEIN_UI_HINT_VECTOR)
	@In
	public String inGpkg;

	@Description("Data import start timestamp.")
	@In
	public String pStartTimestamp;

	@Description("Data import end timestamp.")
	@In
	public String pEndTimestamp;

	@Description("If true, existing output files are overwritten.")
	@In
	public boolean doOverwrite = false;

	@Execute
	public void process() throws Exception {
		Paths p = new Paths(inDtm, doOverwrite);

		try (ASpatialDb db = EDb.GEOPACKAGE.getSpatialDb()) {
			db.open(inGpkg);

			if (doOverwrite) {
				db.executeInsertUpdateDeleteSql("DELETE FROM " + GeoFrameSimpleTable.BASINDATA.tableName() + " WHERE " + //
						BasinDataField.VAR_ID.columnName() + " = "
						+ VarSchema.EnvironmentalVariableType.RADIATION.getId());
			}

			var temperatureReader = new GeoframeEnvDatabaseIterator();
			temperatureReader.db = db;
			temperatureReader.pParameterId = EnvironmentalVariableType.TEMPERATURE.getId(); // temperature
			temperatureReader.pMaxId = IWaterBudgetSimulationRunner.getMaxBasinId(db);
			temperatureReader.tStart = pStartTimestamp + ":00";
			temperatureReader.tEnd = pEndTimestamp + ":00";
			temperatureReader.doRawData = false;
			temperatureReader.preCacheData();

			var radiation = new RadiationAtCentroid();
			radiation.inGeoframeDBPath = inGpkg;
			radiation.inTemperatureReader = temperatureReader;
			radiation.dem = p.dtm; // TODO Daniele, why where you using the pit here?
			radiation.inSkyview = p.skyview;
			radiation.lwrvModeel = "6";
			radiation.doHourly = true;
			radiation.init();
			radiation.process();

		}
	}

	public static void main(String[] args) throws Exception {
		String workspacePath = "/home/hydrologis/development/hm_models_testdata/geoframe/newage/noce/workspace/";
		ErmRadiation er = new ErmRadiation();
		er.inDtm = workspacePath + "dtm.tif";
		er.inGpkg = workspacePath + "outputs/geoframe_data.gpkg";
		er.pStartTimestamp = ErmCommonData.START_TIMESTAMP;
		er.pEndTimestamp = ErmCommonData.END_TIMESTAMP;
		er.doOverwrite = true;
		er.process();
	}

}
