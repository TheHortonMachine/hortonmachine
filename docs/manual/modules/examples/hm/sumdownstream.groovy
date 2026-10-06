import org.hortonmachine.modules.Mapcalc
import org.hortonmachine.modules.SumDownStream

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// 1 for the cells unstable in the map of Shalstab, 0 for the others
var mapcalc = new Mapcalc()
mapcalc.inRaster1 = folder + "shalstab.tif"
mapcalc.pFunction = """
images {
    shalstab = read;
    unstable = write;
}
unstable = con(shalstab == 1 || shalstab == 4, 1, 0);
"""
mapcalc.outRaster = folder + "unstable.tif"
mapcalc.process()

// the number of unstable cells upstream, in the basin of ExtractBasin
var sum = new SumDownStream()
sum.inFlow = folder + "drain_marked.tif"
sum.inToSum = folder + "unstable.tif"
sum.outSummed = folder + "unstable_upstream.tif"
sum.process()
