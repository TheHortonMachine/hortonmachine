package org.hortonmachine.hmachine.geoframe.ermworkflow;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.modules.r.transformer.OmsRasterResolutionResampler;
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
 * GeoFrame-ERM workflow step that computes net radiation at each basin's
 * centroid and persists it to the geoframe database.
 *
 * <p>
 * Physically, net radiation is Rn = (1 - albedo)*SW&#8595; + LW&#8595; -
 * LW&#8593;. The shortwave balance follows Corripio (2002/2003): solar
 * geometry, clear-sky atmospheric transmittance (Rayleigh scattering, ozone,
 * water vapor, aerosols) and terrain shading/sky-view-factor derived from the
 * DTM. The longwave balance uses an empirical clear-sky atmospheric
 * emissivity model - fixed here to Idso [1981] (model "6") - corrected for
 * cloud cover and sky obstruction. Only temperature is read from the
 * database; humidity and the atmospheric clearness index are left at their
 * clear-sky defaults, since this launcher assumes clear-sky conditions with
 * only temperature known.
 */
@Description("Radiation calculator.")
@Author(name = "Daniele Andreis", contact = "")
@Keywords("ERM, GeoFrame, Radiation")
@Label("GeoFrame")
@Name("ermRadiation")
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

	@Description("Data import start timestamp in format yyyy-MM-dd HH:mm.")
	@In
	public String pStartTimestamp;

	@Description("Data import end timestamp in format yyyy-MM-dd HH:mm.")
	@In
	public String pEndTimestamp;

	@Description("If true, existing output files are overwritten.")
	@In
	public boolean doOverwrite = false;
	
	@Description("Downscale factor for the DTM and skyview rasters. If greater than 1, the rasters will be downscaled by this factor. This helps to speed up the processing and reduce memory usage. Default is 1 (no downscaling).")
	@In
	public int downscaleFactor = 1;
	
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
			
			var dtm = getRaster(p.dtm);
			var skyview = getRaster(p.skyview);
			if (downscaleFactor > 1) {
				dtm = downscaleRaster(dtm, downscaleFactor);
				skyview = downscaleRaster(skyview, downscaleFactor);
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
			radiation.inGeoframeDb = db;
			radiation.inTemperatureReader = temperatureReader;
			radiation.dem =  dtm; // TODO Daniele, why where you using the pit here?
			radiation.inSkyview = skyview;
			radiation.lwrvModeel = "6";
			radiation.doHourly = true;
			radiation.init();
			radiation.process();

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
		String workspacePath = "/home/andreisd/Documents/project/data_hm/vermiglio_dtm/inputs/";
		ErmRadiation er = new ErmRadiation();
		er.inDtm = workspacePath + "dtm.tif";
		er.inGpkg = workspacePath + "outputs/geoframe_data.gpkg";
		er.pStartTimestamp = ErmCommonData.START_TIMESTAMP;
		er.pEndTimestamp = ErmCommonData.END_TIMESTAMP;
		er.doOverwrite = true;
		er.downscaleFactor = 1;
		er.process();
	}

}
