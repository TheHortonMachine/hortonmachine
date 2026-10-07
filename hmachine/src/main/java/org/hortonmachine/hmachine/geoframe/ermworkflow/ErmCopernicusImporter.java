package org.hortonmachine.hmachine.geoframe.ermworkflow;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.gears.io.copernicus.CopernicusDailyAgrometeoManagerBase;
import org.hortonmachine.gears.io.copernicus.CopernicusDailyEvapotranspirationManager;
import org.hortonmachine.gears.io.copernicus.CopernicusDailyPrecipitationManager;
import org.hortonmachine.gears.io.copernicus.CopernicusDailyTemperatureManager;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.hmraster.HMRasterZones;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.modules.r.transformer.OmsRasterResolutionResampler;
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameGeoTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameSimpleTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.BasinDataSchema.BasinDataField;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.BasinPolygonSchema.BasinMultiPolygonField;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.EnvironmentalVariableType;

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

@Description("Alternative to the steps 3 to 5 of the ERM/GeoFrame water budget workflow (ErmKriging, ErmRadiation and ErmPrestleyEt): downloads from Copernicus the daily AgERA5 mean temperature, precipitation and reference evapotranspiration (FAO-56 Penman-Monteith) and stores their mean over each sub-basin. AgERA5 data are daily, so the workflow has to run with a daily time step. The Copernicus API token and the folder where the downloaded data are kept are taken from the HortonMachine settings.")
@Author(name = "Andrea Antonello", contact = "https://g-ant.eu")
@Keywords("ERM, GeoFrame, Copernicus, AgERA5, Meteo")
@Label("GeoFrame")
@Name("ermCopernicusImporter")
@Status(10)
@License("General Public License Version 3 (GPLv3)")
public class ErmCopernicusImporter extends HMModel {
	@Description("The GeoFrame database, with the sub-basins of the data preparation.")
	@UI(HMConstants.FILEIN_UI_HINT_GPKG)
	@In
	public String inGpkg;

	@Description("The digital elevation model used for the data preparation: the resolution of the basin elevation raster of its outputs folder is the one at which the Copernicus data are averaged over the sub-basins.")
	@UI(HMConstants.FILEIN_UI_HINT_RASTER)
	@In
	public String inDtm;

	@Description("Start of the period to import. Format: yyyy-MM-dd HH:mm, in UTC. Each day gets this time of the day: it has to be the same as the one of the station data.")
	@In
	public String pStartTimestamp;

	@Description("End of the period to import. Format: yyyy-MM-dd HH:mm, in UTC.")
	@In
	public String pEndTimestamp;

	@Description("If true, the temperatures, precipitations and evapotranspirations already in the database are deleted first.")
	@In
	public boolean doDeleteExistingData = false;

	@Description("Downscale factor for the resolution at which the data are averaged over the sub-basins. If greater than 1, the resolution of the basin elevation raster is divided by this factor: faster, but small sub-basins could get no cells.")
	@In
	public int downscaleFactor = 1;

	private static final double KELVIN_TO_CELSIUS = 273.15;
	private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

	/** The sub-basins rasterized on the grid of the Copernicus rasters, done with the first one. */
	private HMRasterZones basinZones;
	/** The ids of the sub-basins. */
	private TreeSet<Integer> basinIds;

	@Execute
	public void process() throws Exception {
		checkNull(inGpkg, inDtm, pStartTimestamp, pEndTimestamp);
		long startTs = HMConstants.utcDateFormatterYYYYMMDDHHMMSS.parseDateTime(pStartTimestamp + ":00").getMillis();
		long endTs = HMConstants.utcDateFormatterYYYYMMDDHHMMSS.parseDateTime(pEndTimestamp + ":00").getMillis();
		if (endTs < startTs) {
			throw new ModelsIllegalargumentException("The end of the period is before its start.", this, pm);
		}

		String apiToken = CopernicusDailyAgrometeoManagerBase.getApiToken();
		String repo = CopernicusDailyAgrometeoManagerBase.getRepoFolder();

		// the region and resolution of the data, in WGS84, from the basin elevation raster
		Paths p = new Paths(inDtm, false);
		var dtm = getRaster(p.basinPit);
		if (downscaleFactor > 1) {
			dtm = downscaleRaster(dtm, downscaleFactor);
		}
		RegionMap region;
		double xRes;
		double yRes;
		try (var dtmRaster = HMRaster.fromGridCoverage(dtm)) {
			HMCrsTransformer transformer = new HMCrsTransformer(dtmRaster.getCrs(), DefaultGeographicCRS.WGS84);
			try (var dtm4326 = transformer.transform(dtmRaster)) {
				region = dtm4326.getRegionMap();
				xRes = dtm4326.getXRes();
				yRes = dtm4326.getYRes();
			}
		}

		try (ASpatialDb db = EDb.GEOPACKAGE.getSpatialDb();) {
			db.open(inGpkg);

			var basinTable = GeoFrameGeoTable.BASIN.getSchema().getSQLName();
			var basinFC = SpatialDbsImportUtils.tableToFeatureFCollection(db, basinTable, -1, -1, null);

			var basinDataTable = GeoFrameSimpleTable.BASINDATA.getSchema().getSQLName();
			if (!db.hasTable(basinDataTable)) {
				db.executeInsertUpdateDeleteSql(GeoFrameSimpleTable.BASINDATA.getSchema().createTableSql());
			} else if (doDeleteExistingData) {
				db.executeInsertUpdateDeleteSql("DELETE FROM " + GeoFrameSimpleTable.BASINDATA.tableName() + " WHERE "
						+ BasinDataField.VAR_ID.columnName() + " IN (" + EnvironmentalVariableType.TEMPERATURE.getId() + ","
						+ EnvironmentalVariableType.PRECIPITATION.getId() + ","
						+ EnvironmentalVariableType.EVAPOTRANSPIRATION.getId() + ")");
			}

			int days = (int) ((endTs - startTs) / DAY_MILLIS) + 1;
			pm.beginTask("Importing the Copernicus AgERA5 data...", days);
			for (long ts = startTs; ts <= endTs; ts += DAY_MILLIS) {
				String date = Instant.ofEpochMilli(ts).atZone(ZoneOffset.UTC).toLocalDate().toString();

				var temperatureManager = new CopernicusDailyTemperatureManager();
				temperatureManager.pAggregation = "MEAN";
				Map<Integer, Double> temperature = basinMeans(
						download(temperatureManager, date, apiToken, repo, region, xRes, yRes), basinFC, "temperature",
						date);
				temperature.replaceAll((id, kelvin) -> kelvin - KELVIN_TO_CELSIUS);
				Map<Integer, Double> precipitation = basinMeans(
						download(new CopernicusDailyPrecipitationManager(), date, apiToken, repo, region, xRes, yRes),
						basinFC, "precipitation", date);
				Map<Integer, Double> evapotranspiration = basinMeans(
						download(new CopernicusDailyEvapotranspirationManager(), date, apiToken, repo, region, xRes, yRes),
						basinFC, "evapotranspiration", date);

				insert(db, ts, temperature, precipitation, evapotranspiration);
				pm.worked(1);
			}
			pm.done();
		}
	}

	/**
	 * Get a day of a variable, from the local repository or from Copernicus.
	 */
	private GridCoverage2D download(CopernicusDailyAgrometeoManagerBase manager, String date, String apiToken,
			String repo, RegionMap region, double xRes, double yRes) throws Exception {
		manager.pm = pm;
		manager.pApiToken = apiToken;
		manager.pRepoFolder = repo;
		manager.pTimestamp = date;
		manager.pWest = region.getWest();
		manager.pEast = region.getEast();
		manager.pSouth = region.getSouth();
		manager.pNorth = region.getNorth();
		manager.pXres = xRes;
		manager.pYres = yRes;
		manager.process();
		return manager.outputRaster;
	}

	/**
	 * Average a Copernicus raster over the sub-basins.
	 *
	 * <p>The sub-basins are rasterized on the grid of the first raster only: the following ones
	 * have the same region and resolution.</p>
	 *
	 * @return the mean of each sub-basin, by id.
	 */
	private Map<Integer, Double> basinMeans(GridCoverage2D coverage, SimpleFeatureCollection basinFC, String variable,
			String date) throws Exception {
		try (HMRaster raster = HMRaster.fromGridCoverage(coverage)) {
			if (basinZones == null) {
				rasterizeBasins(raster, basinFC);
			}
			HashMap<Integer, double[]> stats = raster.getZonalStats(new DummyProgressMonitor(), basinZones);
			Map<Integer, Double> means = new HashMap<>();
			for (int id : basinIds) {
				double[] basinStats = stats.get(id);
				if (basinStats == null) {
					throw new ModelsRuntimeException(
							"Copernicus has no " + variable + " data over the sub-basin " + id + " on " + date + ".", this);
				}
				means.put(id, basinStats[2]);
			}
			return means;
		}
	}

	/**
	 * Rasterize the sub-basins on the grid of a Copernicus raster, checking that each one gets cells.
	 */
	private void rasterizeBasins(HMRaster raster, SimpleFeatureCollection basinFC) throws Exception {
		String idField = BasinMultiPolygonField.ID.columnName();
		var reprojectedFC = new ReprojectingFeatureCollection(basinFC, raster.getCrs());

		basinIds = new TreeSet<>();
		try (SimpleFeatureIterator iterator = reprojectedFC.features()) {
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				basinIds.add(((Number) feature.getAttribute(idField)).intValue());
			}
		}

		basinZones = raster.rasterizeZones(new DummyProgressMonitor(), reprojectedFC, idField);
		List<Integer> missing = basinIds.stream().filter(id -> !basinZones.getZoneIds().contains(id)).toList();
		if (!missing.isEmpty()) {
			throw new ModelsRuntimeException("The sub-basins " + missing
					+ " cover no cell of the Copernicus rasters: use a smaller downscaleFactor.", this);
		}
	}

	/**
	 * Insert the values of a day of all the sub-basins, in a single transaction.
	 */
	private void insert(ASpatialDb db, long ts, Map<Integer, Double> temperature, Map<Integer, Double> precipitation,
			Map<Integer, Double> evapotranspiration) throws Exception {
		String insertSql = GeoFrameSimpleTable.BASINDATA.getSchema().buildInsertAll();
		db.execOnConnection(conn -> {
			boolean autoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			try (IHMPreparedStatement pStmt = conn.prepareStatement(insertSql)) {
				for (int id : basinIds) {
					addValue(pStmt, ts, id, EnvironmentalVariableType.TEMPERATURE, temperature.get(id));
					addValue(pStmt, ts, id, EnvironmentalVariableType.PRECIPITATION, precipitation.get(id));
					addValue(pStmt, ts, id, EnvironmentalVariableType.EVAPOTRANSPIRATION, evapotranspiration.get(id));
				}
				pStmt.executeBatch();
				conn.commit();
			} finally {
				conn.setAutoCommit(autoCommit);
			}
			return null;
		});
	}

	private static void addValue(IHMPreparedStatement pStmt, long ts, int basinId, EnvironmentalVariableType type,
			double value) throws Exception {
		pStmt.setLong(1, ts);
		pStmt.setInt(2, basinId);
		pStmt.setInt(3, type.getId());
		pStmt.setDouble(4, value);
		pStmt.addBatch();
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
		ek.pStartTimestamp = "2018-03-02 01:00";
		ek.pEndTimestamp = "2019-03-01 01:00";
		ek.doDeleteExistingData = false;
		ek.inDtm = "/home/hydrologis/storage/lavori_tmp/JAPAN/TOKYO/ERM_SIMULATION/dem_akigawa.tif";
		ek.process();
	}

}
