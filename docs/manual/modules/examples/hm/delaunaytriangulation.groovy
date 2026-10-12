import org.hortonmachine.modules.DelaunayTriangulation
import org.hortonmachine.modules.VectorReshaper

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the centroids of the sub-basins of BasinShape, with their mean elevation
var reshaper = new VectorReshaper()
reshaper.inVector = folder + "subbasins.shp"
reshaper.pCql = "the_geom=centroid(the_geom)"
reshaper.outVector = folder + "subbasins_centroids.shp"
reshaper.process()

var delaunay = new DelaunayTriangulation()
delaunay.inMap = folder + "subbasins_centroids.shp"
// the elevation of the vertices of the triangles
delaunay.fElev = "avgelev"
delaunay.outMap = folder + "triangles.shp"
delaunay.process()
