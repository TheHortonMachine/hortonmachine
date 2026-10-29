// Browse a STAC catalog: list its collections, items and assets, and download the vector assets of some collections clipped to a region
// keywords: stac, catalog, collections, items, assets, download, search, remote, server, api, spatiotemporal, vectors, gpkg, hmstacmanager
import org.hortonmachine.* // the HM helper
import org.hortonmachine.gears.io.stac.HMStacManager
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.factory.CommonFactoryFinder
import org.geotools.geometry.jts.ReferencedEnvelope
import org.locationtech.jts.geom.Envelope

def catalogUrl = "https://example.org/api/stac/v1/"
// the collections to download, empty to only list them all
def collectionIds = ["collection.id.one", "collection.id.two"]
def outGpkg = "/path/to/assets.gpkg"
// the region to keep, in the projection of the data
def region = new Envelope(2600000, 2620000, 1200000, 1220000) // minx, maxx, miny, maxy

def stac = new HMStacManager(catalogUrl, null)
stac.open()
try {
    for (collection in stac.getCollections()) {
        println collection.id + ": " + collection.title
        if (!collectionIds.contains(collection.id)) {
            continue
        }
        for (item in collection.searchItems()) {
            println "  item " + item.id + " (EPSG:" + item.epsg + ")"
            for (asset in item.getAssets()) {
                println "    asset " + asset.id + ": " + asset.type
                def handler = asset.getHandler()
                if (handler == null) {
                    continue // no reader for this kind of asset
                }
                def layers = handler.readAll(SimpleFeatureCollection.class, new DummyProgressMonitor())
                layers.each { name, features ->
                    def clipped = clip(features, region)
                    HM.dumpVector(clipped, outGpkg + "#" + name, item.epsg)
                    println "      wrote ${clipped.size()} of ${features.size()} features to the layer " + name
                }
            }
        }
    }
} finally {
    stac.close()
}

// the features that intersect the region
def clip(features, envelope) {
    def filterFactory = CommonFactoryFinder.getFilterFactory()
    def schema = features.getSchema()
    def bbox = new ReferencedEnvelope(envelope, schema.getCoordinateReferenceSystem())
    def filter = filterFactory.bbox(filterFactory.property(schema.getGeometryDescriptor().getLocalName()), bbox)
    return features.subCollection(filter)
}
