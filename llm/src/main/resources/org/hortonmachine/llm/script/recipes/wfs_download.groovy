// Download the features of a layer of a WFS (Web Feature Service) server in a region, and save them to a GeoPackage
// keywords: wfs, web feature service, ogc, features, layer, download, vector, remote, server, url, region, bbox, gpkg
import org.hortonmachine.* // the HM helper
import org.hortonmachine.gears.io.wfs.Wfs
import org.locationtech.jts.geom.Envelope

def url = "https://example.org/geoserver/wfs"
def layerName = "workspace:layer"
def outGpkg = "/path/to/features.gpkg"
// the region, in the projection and axis order of the server (before any swapping of the
// coordinates: for EPSG:4326 layers many servers want latitude first): minx, maxx, miny, maxy
def region = new Envelope(10.5, 10.8, 46.0, 46.3)

def wfs = new Wfs(url, layerName)
wfs.forceNormalizeGeometryName()
wfs.connect()
try {
    println "Version: " + wfs.version
    println "Layers: " + wfs.typeNames
    def schema = wfs.simpleFeatureType
    println "Attributes: " + schema.attributeDescriptors*.localName
    println "Projection: " + schema.coordinateReferenceSystem
    println "Bounds: " + wfs.bounds

    def features = wfs.getFeatureCollection(region)
    println "Downloaded ${features.size()} features"
    HM.dumpVector(features, outGpkg + "#" + layerName.replace(":", "_"))
} finally {
    wfs.close()
}
