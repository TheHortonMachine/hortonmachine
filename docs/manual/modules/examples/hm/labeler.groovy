import org.hortonmachine.modules.Labeler

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the middle elevation belt of the RasterNull example, split by the valleys and the ridges:
// every separate patch of it gets its own number
var labeler = new Labeler()
labeler.inMap = folder + "belt_middle.tif"
labeler.outMap = folder + "belt_middle_patches.tif"
labeler.process()
