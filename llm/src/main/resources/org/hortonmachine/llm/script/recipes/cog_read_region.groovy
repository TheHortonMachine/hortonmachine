// Read a region of a remote cloud optimized GeoTIFF (COG) by its URL, at a chosen resolution, and save it
// keywords: cog, cloud optimized, geotiff, remote, url, http, https, s3, download, region, bounds, bbox, clip, resolution, read raster
import org.hortonmachine.* // the HM helper
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader

// the URL of the COG and the file to write
def url = "https://example.org/path/to/dem_cog.tif"
def outPath = "/path/to/dem_region.tif"
// the region and the cell size, in the projection of the COG (here degrees, for an EPSG:4326 one)
def north = 46.5
def south = 46.0
def west = 11.0
def east = 11.5
def resolution = 0.001

// only the needed part is read from the server
def reader = new OmsRasterReader()
reader.file = url
reader.pNorth = north
reader.pSouth = south
reader.pWest = west
reader.pEast = east
reader.pXres = resolution
reader.pYres = resolution
reader.process()
HM.dumpRaster(reader.outRaster, outPath)
