// Print the information of a raster: bounds, resolution, rows and columns, novalue, projection, and its bounds as a polygon
// keywords: raster, info, information, bounds, extent, envelope, resolution, cell size, rows, cols, novalue, crs, projection, epsg, region, polygon
import org.hortonmachine.gears.libs.modules.HMRaster
import org.hortonmachine.gears.utils.crs.HMCrsRegistry
import org.locationtech.jts.geom.GeometryFactory

def path = "/path/to/raster.tif"

def raster = HMRaster.fromFile(path)
def region = raster.getRegionMap()
println "Bounds: north ${region.getNorth()}, south ${region.getSouth()}, west ${region.getWest()}, east ${region.getEast()}"
println "Resolution: ${region.getXres()} x ${region.getYres()}"
println "Columns and rows: ${region.getCols()} x ${region.getRows()}"
println "Novalue: " + raster.getNovalue()
// prj files often have no EPSG code: then the name of the projection
def crs = raster.getCrs()
println "Projection: " + (HMCrsRegistry.getCodeFromCrs(crs) ?: crs.getName())

def boundsPolygon = new GeometryFactory().toGeometry(region.toEnvelope())
println "Bounds polygon: " + boundsPolygon
raster.close()
