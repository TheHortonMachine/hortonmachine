import org.hortonmachine.modules.RangeLookup
import org.hortonmachine.modules.RasterTransitionClasses

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the elevation belts of RangeLookup after a rise of 200 m of their limits, as of a treeline
var rangeLookup = new RangeLookup()
rangeLookup.inRaster = folder + "pit.tif"
rangeLookup.pRanges = "[null 1500),[1500 1900),[1900 null)"
rangeLookup.pClasses = "1,2,3"
rangeLookup.outRaster = folder + "elevation_belts_risen.tif"
rangeLookup.process()

// the cells that moved from a belt to another
var transitions = new RasterTransitionClasses()
transitions.inPreviousRaster = folder + "elevation_belts.tif"
transitions.inNextRaster = folder + "elevation_belts_risen.tif"
transitions.outRaster = folder + "belts_transitions.tif"
// the code, the previous and next class and the count of cells of each transition
transitions.outTransition = folder + "belts_transitions.csv"
transitions.process()
