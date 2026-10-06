import org.hortonmachine.modules.ExtractBasin
import org.hortonmachine.modules.Mosaic12
import org.hortonmachine.modules.RasterResizer

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the polygon of the basin of the outlet of Wateroutlet
var extractBasin = new ExtractBasin()
extractBasin.inFlow = folder + "drain.tif"
extractBasin.pEast = 1639885.0
extractBasin.pNorth = 5110955.0
extractBasin.outBasin = folder + "outlet_basin2.tif"
extractBasin.outVectorBasin = folder + "outlet_basin2.shp"
extractBasin.process()

// the elevation cut to the extent of that basin
var resizer = new RasterResizer()
resizer.inRaster = folder + "dtm_flanginec.tif"
resizer.inVector = folder + "outlet_basin2.shp"
resizer.outRaster = folder + "dtm_outlet_basin.tif"
resizer.process()

// patch it with the elevation cut to the extent of the basin of ExtractBasin by RasterResizer
var mosaic = new Mosaic12()
mosaic.inMap1 = folder + "dtm_basin.tif"
mosaic.inMap2 = folder + "dtm_outlet_basin.tif"
mosaic.outMap = folder + "dtm_mosaic.tif"
mosaic.process()
