// Reproject all the vector files of a folder (and subfolders) and collect them as layers of one GeoPackage
// keywords: reproject, transform, projection, crs, epsg, vectors, layers, folder, gpkg, geopackage, shapefile, collect, merge, batch
import org.hortonmachine.* // the HM helper
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader
import org.hortonmachine.gears.utils.crs.HMCrsRegistry
import org.hortonmachine.gears.utils.crs.HMCrsTransformer

def folder = "/path/to/vectors/"
def extension = ".shp"
def outGpkg = "/path/to/reprojected.gpkg"
def toCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:5070")

(folder as File).eachFileRecurse { file ->
    if (file.isFile() && file.name.endsWith(extension)) {
        try {
            def features = OmsVectorReader.readVector(file.absolutePath)
            def fromCrs = features.getSchema().getCoordinateReferenceSystem()
            def reprojected = new HMCrsTransformer(fromCrs, toCrs).transform(features)
            // path#layer writes a layer of the GeoPackage, named after the file
            def layer = file.name.substring(0, file.name.lastIndexOf("."))
            HM.dumpVector(reprojected, outGpkg + "#" + layer)
            println "Reprojected " + file.name
        } catch (Exception e) {
            println "Error with " + file.name + ": " + e.message
        }
    }
}
