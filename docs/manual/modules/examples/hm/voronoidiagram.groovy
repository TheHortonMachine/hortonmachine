import org.hortonmachine.modules.VoronoiDiagram

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var voronoi = new VoronoiDiagram()
// the centroids of the sub-basins, of the DelaunayTriangulation example
voronoi.inMap = folder + "subbasins_centroids.shp"
voronoi.outMap = folder + "voronoi.shp"
voronoi.process()
