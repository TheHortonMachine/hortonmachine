// Create an area of interest (AOI, ROI) polygon from bounds in longitude and latitude, also reprojected, as GeoPackage layers
// keywords: aoi, roi, area of interest, region of interest, bounds, bbox, bounding box, polygon, rectangle, extent, create vector, longitude, latitude
import org.hortonmachine.* // the HM helper
import org.hortonmachine.gears.utils.crs.HMCrsRegistry
import org.hortonmachine.gears.utils.crs.HMCrsTransformer
import org.hortonmachine.gears.utils.features.FeatureUtilities
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.GeometryFactory

def outGpkg = "/path/to/aoi.gpkg"
def west = 10.5
def east = 10.8
def south = 46.0
def north = 46.3
def toEpsg = "EPSG:32632"

def lonLatCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true) // longitude first
def polygon = new GeometryFactory().toGeometry(new Envelope(west, east, south, north))
HM.dumpVector(FeatureUtilities.featureCollectionFromGeometry(lonLatCrs, polygon), outGpkg + "#aoi_4326")

def projected = new HMCrsTransformer("EPSG:4326", toEpsg, true, true).transform(polygon)
def toCrs = HMCrsRegistry.INSTANCE.getCrs(toEpsg)
HM.dumpVector(FeatureUtilities.featureCollectionFromGeometry(toCrs, projected), outGpkg + "#aoi_" + toEpsg.replace("EPSG:", ""))
println "AOI written to " + outGpkg
