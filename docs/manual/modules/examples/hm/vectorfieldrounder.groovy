import org.hortonmachine.modules.VectorFieldRounder

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var rounder = new VectorFieldRounder()
// the cells with the elevation of RasterCatToFeatureAttribute
rounder.inVector = folder + "grid_elev.shp"
// the mean elevation to the meter
rounder.fRound = "elev_avg"
rounder.pPattern = "0"
rounder.outVector = folder + "grid_elev_round.shp"
rounder.process()
