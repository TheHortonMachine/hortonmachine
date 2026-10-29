// Download daily meteo raster maps (precipitation, temperature, radiation, evapotranspiration) of Copernicus AgERA5 for a period and a region, as GeoTIFF
// keywords: copernicus, precipitation, rain, rainfall, temperature, radiation, evapotranspiration, etp, meteo, meteorological, weather, climate, daily, days, period, agera5, era5, cds, download, maps, rasters, agrometeorological
import org.hortonmachine.* // the HM helper
import org.hortonmachine.gears.io.copernicus.*
import java.time.LocalDate

// the CDS token and the cache folder are read from the HortonMachine preferences, or from the
// environment variables hm_pref_copernicus_api_token and COPERNICUS_REPO_FOLDER:
// never write the token in a script
def apiToken = CopernicusDailyAgrometeoManagerBase.getApiToken()
def cacheFolder = CopernicusDailyAgrometeoManagerBase.getRepoFolder()
def outFolder = "/path/to/precipitation/"
def firstDay = LocalDate.parse("2023-06-01")
def lastDay = LocalDate.parse("2023-06-10")
// the region, in longitude and latitude
def north = 47.0
def south = 45.5
def west = 10.5
def east = 12.0

(outFolder as File).mkdirs()
for (def day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) {
    // also CopernicusDailyTemperatureManager (pAggregation MEAN, MIN or MAX, in Kelvin),
    // CopernicusDailyRadiationManager, CopernicusDailyEvapotranspirationManager
    def manager = new CopernicusDailyPrecipitationManager()
    manager.pApiToken = apiToken
    manager.pRepoFolder = cacheFolder
    manager.pTimestamp = day.toString() // yyyy-MM-dd
    manager.pNorth = north
    manager.pSouth = south
    manager.pWest = west
    manager.pEast = east
    // the whole world of the day is downloaded once into the cache, then only cut
    manager.process()
    def outPath = outFolder + "precipitation_" + day + ".tif"
    HM.dumpRaster(manager.outputRaster, outPath)
    println day.toString() + (manager.wasCached ? " from the cache: " : " downloaded: ") + outPath
}
