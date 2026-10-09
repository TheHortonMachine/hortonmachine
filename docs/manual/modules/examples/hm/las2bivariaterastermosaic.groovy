import org.hortonmachine.lesto.modules.raster.Las2BivariateRasterMosaic

// the folder with the lidar survey: change it to yours
var folder = "/data/flanginec/"

// the surface of the first returns in tiles of 100 m with cells of 0.5 m, each cell from
// the quadratic surface fitted to the points within 2 cells; the plot fits in one tile
new File(folder + "las_mosaic").mkdirs()
var mosaic = new Las2BivariateRasterMosaic()
mosaic.inLas = folder + "uni_bz_plot777.las"
mosaic.pTilesize = 100.0
mosaic.pRes = 0.5
mosaic.pBuffer = 2
mosaic.pImpulse = 1
mosaic.outFolder = folder + "las_mosaic"
mosaic.process()
