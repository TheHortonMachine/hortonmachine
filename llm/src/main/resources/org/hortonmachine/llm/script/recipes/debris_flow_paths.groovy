// Runout paths of debris flows from trigger points, along the drainage directions (DebrisVandre)
// keywords: debris, debris flow, runout, paths, triggers, vandre, burton, bathurst, landslide, hazard
import org.hortonmachine.modules.*

// the inputs come from the hydrological chain (pit, drainage directions) and from Slope;
// the triggers are a raster with a value on the trigger cells
def folder = "/path/to/folder/"
def pit = folder + "pit.tif"
def drain = folder + "drain.tif"
def slope = folder + "slope.tif"
def triggers = folder + "triggers.tif"

def debrisvandre = new DebrisVandre()
debrisvandre.inElev = pit
debrisvandre.inFlow = drain
debrisvandre.inSlope = slope
debrisvandre.inTriggers = triggers
debrisvandre.pMode = 0 // 0 Burton and Bathurst, 1 the modified criteria
debrisvandre.outPaths = folder + "debris_paths.shp"
debrisvandre.outIndexedTriggers = folder + "debris_triggers.shp"
debrisvandre.process()
