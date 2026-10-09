import org.hortonmachine.modules.Mapcalc
import org.hortonmachine.modules.Morpher

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the mask of the steep slopes, above 1 m/m (45 degrees), from the slope of Slope:
// the other cells are novalue, the background of the morphology
var mapcalc = new Mapcalc()
mapcalc.inRaster1 = folder + "slope.tif"
mapcalc.pFunction = """
images {
    slope = read;
    steep = write;
}
steep = con(slope > 1, 1, null);
"""
mapcalc.outRaster = folder + "steep.tif"
mapcalc.process()

// the opening removes the isolated cells and the thin strips, keeping the larger areas
var morpher = new Morpher()
morpher.inMap = folder + "steep.tif"
morpher.pMode = "open"
morpher.outMap = folder + "steep_open.tif"
morpher.process()
