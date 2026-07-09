package org.hortonmachine.hmachine.geoframe;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameGeoTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameSimpleTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.BasinDataSchema.BasinDataField;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.StationSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.StationSchema.Station;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema;
import org.hortonmachine.hmachine.geoframe.utils.KrigingAtCentroid;

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

@Description("Prepares raster and topological data for the ERM/GeoFrame water budget model pipeline.")
@Author(name = "Daniele Andreis", contact = "")
@Keywords("ERM, GeoFrame, Kriging")
@Label("GeoFrame")
@Name("ermKriging")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class ErmKriging extends HMModel {
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

	@Description("Delete existing data.")
	@In
	public boolean doDeleteExistingData = false;

	@Execute
	public void process() throws Exception {
		try (ASpatialDb db = EDb.GEOPACKAGE.getSpatialDb();) {
			db.open(inGpkg);
			int maxId = db.getLong("select max(" + Station.ID.columnName() + ") from " + //
					GeoFrameGeoTable.HYDRO_METEO_STATION.tableName() + " WHERE " + //
					Station.TYPE.columnName() + " = '" + StationSchema.StationType.METEO + "'").intValue();

			pm.beginTask("Processing temperature data...", 1);
			int type = 4;
			int typeId = VarSchema.EnvironmentalVariableType.TEMPERATURE.getId();
			if (doDeleteExistingData) {
				db.executeInsertUpdateDeleteSql(
						"DELETE FROM " + GeoFrameSimpleTable.BASINDATA.tableName() + " WHERE " + //
								BasinDataField.VAR_ID.columnName() + " = " + typeId);
			}
			processKriging(db, maxId, type, typeId);
			pm.done();

			pm.beginTask("Processing precipitation data...", 1);
			type = 2; // TODO is this the same as below?
			typeId = VarSchema.EnvironmentalVariableType.PRECIPITATION.getId();
			if (doDeleteExistingData) {
				db.executeInsertUpdateDeleteSql(
						"DELETE FROM " + GeoFrameSimpleTable.BASINDATA.tableName() + " WHERE " + //
								BasinDataField.VAR_ID.columnName() + " = " + typeId);
			}
			processKriging(db, maxId, type, typeId);
			pm.done();

		}
	}

	private void processKriging(ASpatialDb db, int maxId, int type, int typeId) throws Exception {
		var valueReader = new GeoframeEnvDatabaseIterator();
		valueReader.db = db;
		valueReader.pParameterId = type; // temperature
		valueReader.tStart = pStartTimestamp;
		valueReader.tEnd = pEndTimestamp;
		valueReader.doRawData = true;
		valueReader.pMaxId = maxId;
		valueReader.preCacheData();

		var krigingInterpolator = new KrigingAtCentroid();
		krigingInterpolator.inGeoframeDBPath = inGpkg;
		krigingInterpolator.inVariableType = typeId;
		krigingInterpolator.variableReader = valueReader;
		krigingInterpolator.cutoffDivide = 10;
		krigingInterpolator.init();
		krigingInterpolator.process();
	}

	public static void main(String[] args) throws Exception {
		ErmKriging ek = new ErmKriging();
		ek.inGpkg = "/home/hydrologis/development/hm_models_testdata/geoframe/newage/noce/workspace/outputs/geoframe_data.gpkg";
		ek.pStartTimestamp = ErmCommonData.START_TIMESTAMP;
		ek.pEndTimestamp = ErmCommonData.END_TIMESTAMP;
		ek.doDeleteExistingData = true;
		ek.process();
	}

}
