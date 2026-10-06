import org.hortonmachine.modules.Mapcalc

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the depth of the depressions filled by Pitfiller
var mapcalc = new Mapcalc()
mapcalc.inRaster1 = folder + "pit.tif"
mapcalc.inRaster2 = folder + "dtm_flanginec.tif"
// the maps are known by their file name, and declared in the images block
mapcalc.pFunction = """
images {
    pit = read;
    dtm_flanginec = read;
    filled = write;
}
filled = pit - dtm_flanginec;
"""
mapcalc.outRaster = folder + "filled.tif"
mapcalc.process()
