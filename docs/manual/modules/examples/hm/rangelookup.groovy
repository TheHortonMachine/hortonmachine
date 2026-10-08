import org.hortonmachine.modules.RangeLookup

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var rangeLookup = new RangeLookup()
rangeLookup.inRaster = folder + "pit.tif"
// three elevation belts: below 1300 m, from 1300 to 1700 m, above 1700 m
rangeLookup.pRanges = "[null 1300),[1300 1700),[1700 null)"
rangeLookup.pClasses = "1,2,3"
rangeLookup.outRaster = folder + "elevation_belts.tif"
rangeLookup.process()
