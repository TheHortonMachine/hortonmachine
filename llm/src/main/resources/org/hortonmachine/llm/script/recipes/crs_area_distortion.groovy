// Compare the areas of the polygons of vector files in two projections, to see the distortion of each
// keywords: area, distortion, projection, crs, epsg, compare, polygons, equal area, accuracy, check, vectors
import org.hortonmachine.gears.utils.crs.HMCrsTransformer

// the vector files; a GeoPackage with several layers needs the layer as path#layer
def vectors = ["/path/to/parcels.shp", "/path/to/data.gpkg#lakes"]
def epsg1 = "EPSG:32632"
def epsg2 = "EPSG:3035"

def results = []
vectors.each { path ->
    // the area in the first projection, in the second and their difference
    def areas = HMCrsTransformer.evaluateAreaDistortion(path, epsg1, epsg2)
    results << [path: path, area1: areas[0], area2: areas[1], percent: areas[2] / areas[0] * 100]
}

results.sort { -it.percent }.each {
    println "${it.path}: ${it.area1} m² in ${epsg1}, ${it.area2} m² in ${epsg2}, ${it.percent} % difference"
}
