package org.hortonmachine.hmachine.geoframe.ermworkflow;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.gears.io.copernicus.CopernicusDailyAgrometeoManagerBase;
import org.hortonmachine.gears.io.copernicus.CopernicusDailyTemperatureManager;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.modules.r.transformer.OmsRasterResolutionResampler;
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameGeoTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameSimpleTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.BasinDataSchema.BasinDataField;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.BasinPolygonSchema.BasinMultiPolygonField;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema;

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

@Description("Downloads and averages Copernicus data on the geoframe basins.")
@Author(name = "Andrea Antonello", contact = "https://g-ant.eu")
@Keywords("ERM, GeoFrame, Copernicus, Meteo")
@Label("GeoFrame")
@Name("ermCopernicusImporter")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class ErmCopernicusImporter extends HMModel {
	@Description("Input geoframe data geopackage.")
	@UI(HMConstants.FILEIN_UI_HINT_VECTOR)
	@In
	public String inGpkg;
	
	@Description("Input dtm used for resolution calculation.")
	@UI(HMConstants.FILEIN_UI_HINT_RASTER)
	@In
	public String inDtm;

	@Description("Data import start timestamp in format YYYY-MM-DD.")
	@In
	public String pStartTimestamp;

	@Description("Data import end timestamp in format YYYY-MM-DD.")
	@In
	public String pEndTimestamp;

	@Description("Delete existing data.")
	@In
	public boolean doDeleteExistingData = false;
	
	@Description("Downscale factor for the DTM rasters. If greater than 1, the raster will be downscaled by this factor. This helps to speed up the processing and reduce memory usage. Default is 1 (no downscaling).")
	@In
	public int downscaleFactor = 1;

	@Execute
	public void process() throws Exception {
		checkNull(inGpkg, pStartTimestamp, pEndTimestamp);
		Paths p = new Paths(inDtm, doDeleteExistingData);
		
		var dtm = getRaster(p.basinPit);
		if (downscaleFactor > 1) {
			dtm = downscaleRaster(dtm, downscaleFactor);
		}
		var dtmRaster = HMRaster.fromGridCoverage(dtm);
		HMCrsTransformer transformer = new HMCrsTransformer(dtmRaster.getCrs(), DefaultGeographicCRS.WGS84); 
		dtmRaster = transformer.transform(dtmRaster);
		var dtmRegionMap = dtmRaster.getRegionMap();
		
		String apiToken = CopernicusDailyAgrometeoManagerBase.getApiToken();
		String repo = CopernicusDailyAgrometeoManagerBase.getRepoFolder();

		LocalDate startDate = LocalDate.parse(pStartTimestamp);
		LocalDate endDate = LocalDate.parse(pEndTimestamp);

		try (ASpatialDb db = EDb.GEOPACKAGE.getSpatialDb();) {
			db.open(inGpkg);

			var basinTable = GeoFrameGeoTable.BASIN.getSchema().getSQLName();
			var basinFC = SpatialDbsImportUtils.tableToFeatureFCollection(db, basinTable, -1, -1, null);
			var basinFC4326 = new ReprojectingFeatureCollection(basinFC, DefaultGeographicCRS.WGS84);

			pm.message("Processing temperature data...");
			int typeId = VarSchema.EnvironmentalVariableType.TEMPERATURE.getId();
			if (doDeleteExistingData && db.hasTable(GeoFrameSimpleTable.BASINDATA.getSchema().getSQLName())) {
				db.executeInsertUpdateDeleteSql("DELETE FROM " + GeoFrameSimpleTable.BASINDATA.tableName() + " WHERE " + //
						BasinDataField.VAR_ID.columnName() + " = " + typeId);
			}

			for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
				String currentTStr = date.toString();
				long currentT = HMConstants.utcDateFormatterYYYYMMDDHHMMSS.parseDateTime(currentTStr + " 12:00:00").getMillis();
				pm.message("Downloading and processing temperature data for date: " + currentTStr);
				var tempManager = new CopernicusDailyTemperatureManager();
				tempManager.pAggregation = "MEAN";
				tempManager.pApiToken = apiToken;
				tempManager.pTimestamp = date.toString();
				tempManager.pWest = dtmRegionMap.getWest();
				tempManager.pEast = dtmRegionMap.getEast();
				tempManager.pSouth = dtmRegionMap.getSouth();
				tempManager.pNorth = dtmRegionMap.getNorth();
				tempManager.pXres = dtmRaster.getXRes();
				tempManager.pYres = dtmRaster.getYRes();
				tempManager.pRepoFolder = repo;
				tempManager.process();
				try (var raster = HMRaster.fromGridCoverage(tempManager.outputRaster)) {
					HashMap<Integer, double[]> zonalStats = raster.getZonalStats(pm, basinFC4326,
							BasinMultiPolygonField.ID.columnName());
					try {
						String insertSql = GeoFrameSimpleTable.BASINDATA.getSchema().buildInsertAll();
						db.execOnConnection(conn -> {
							boolean autoCommit = conn.getAutoCommit();
							conn.setAutoCommit(false);
							try (IHMPreparedStatement pStmt = conn.prepareStatement(insertSql)) {
								for (Map.Entry<Integer, double[]> entry : zonalStats.entrySet()) {
									int basinId = entry.getKey();
									double value = entry.getValue()[2]; // avg tmp
									pStmt.setLong(1, currentT);
									pStmt.setInt(2, basinId);
									pStmt.setInt(3, typeId);
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

			}

		}
	}
	
	private GridCoverage2D downscaleRaster(GridCoverage2D dtm, int downscaleFactor2) throws Exception {
		HMRaster r = HMRaster.fromGridCoverage(dtm);
		double xRes = r.getXRes();
		double yRes = r.getYRes();
		double newXRes = xRes * downscaleFactor2;
		double newYRes = yRes * downscaleFactor2;
		OmsRasterResolutionResampler resampler = new OmsRasterResolutionResampler();
		resampler.inGeodata = dtm;
		resampler.pXres = newXRes;
		resampler.pYres = newYRes;
		resampler.process();
		return resampler.outGeodata;
	}

	public static void main(String[] args) throws Exception {
		ErmCopernicusImporter ek = new ErmCopernicusImporter();
		ek.inGpkg = "/home/hydrologis/storage/lavori_tmp/JAPAN/TOKYO/ERM_SIMULATION/outputs_akikawa/geoframe_data_akikawa.gpkg";
		ek.pStartTimestamp = "2018-01-01";
		ek.pEndTimestamp = "2018-03-01";
		ek.doDeleteExistingData = true;
		ek.inDtm = "/home/hydrologis/storage/lavori_tmp/JAPAN/TOKYO/ERM_SIMULATION/dem_akigawa.tif";
		ek.process();
	}

}
