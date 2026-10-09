import org.hortonmachine.lesto.modules.raster.LasTriangulation2Dsm

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// the surface of the triangles of the points, without the triangles with a
// difference of elevation of more than 3 m between their vertexes
var triangulation = new LasTriangulation2Dsm()
triangulation.inLas = folder + "uni_bz_plot777.las"
triangulation.inDtm = folder + "dtm_777.tif"
triangulation.pElevThres = 3.0
triangulation.outRaster = folder + "dsm_triangles.tif"
triangulation.process()
