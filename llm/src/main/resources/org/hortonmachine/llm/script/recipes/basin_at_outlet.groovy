// Basin (watershed) draining to an outlet point given in longitude and latitude, as raster and vector, with its area
// keywords: basin, watershed, catchment, outlet, point, coordinate, coordinates, longitude, latitude, extractbasin, area, delineate
import org.hortonmachine.modules.*
import org.hortonmachine.gears.libs.modules.HMRaster
import org.hortonmachine.gears.utils.crs.*
import org.locationtech.jts.geom.Coordinate

// the drainage directions, from FlowDirections or DrainDir (see the hydrological chain from a DEM)
def folder = "/path/to/folder/"
def drain = folder + "drain.tif"
def basin = folder + "basin.tif"
def basinVector = folder + "basin.shp"
// the outlet: it must be on a channel of the network, or the basin is just a few cells
def lon = 10.8
def lat = 46.1

// the modules take the outlet in the projection of the map; the projection is taken from the
// map itself, since prj files often have no EPSG code
def raster = HMRaster.fromFile(drain)
def mapCrs = raster.getCrs()
raster.close()
def lonLatCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true) // longitude first
def outlet = new HMCrsTransformer(lonLatCrs, mapCrs).transform(new Coordinate(lon, lat))
println "Outlet in the projection of the map: ${outlet.x}, ${outlet.y}"

def extractbasin = new ExtractBasin()
extractbasin.inFlow = drain
extractbasin.pEast = outlet.x
extractbasin.pNorth = outlet.y
extractbasin.outBasin = basin
extractbasin.outVectorBasin = basinVector
extractbasin.process()
println "Basin area: " + extractbasin.outArea + " square map units"
