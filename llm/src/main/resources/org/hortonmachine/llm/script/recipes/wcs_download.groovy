// Download a raster coverage of a region from a WCS (Web Coverage Service) server as GeoTIFF
// keywords: wcs, web coverage service, ogc, coverage, download, raster, remote, server, url, region, bbox, geotiff
import org.hortonmachine.* // the HM helper
import org.hortonmachine.gears.io.wcs.Wcs

def url = "https://example.org/geoserver/wcs"
def coverageId = "workspace:coverage"
def outPath = "/path/to/coverage.tif"
// the region, with the EPSG code of its projection, and the size of the output
def srid = 32632
def region = Wcs.evelope(650000.0, 5100000.0, 670000.0, 5120000.0) // minx, miny, maxx, maxy
def rows = 800
def cols = 1000

def wcs = new Wcs(url, "1.0.0")
println "Coverages: " + wcs.ids()
def describe = wcs.describe(coverageId)
println "Formats: " + describe.supportedFormats.join(", ")
println "Srids: " + describe.supportedSrids

def parameters = wcs.getReaderParameters(coverageId)
parameters.bbox(region, srid)
parameters.rowsCols(rows, cols)
parameters.format("GeoTIFF")
println "Request: " + wcs.dumpCoverage(outPath, parameters)

// shown in the console output
return HM.toImage(outPath)
