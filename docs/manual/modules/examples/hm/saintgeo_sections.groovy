import org.hortonmachine.modules.HecrasInputBuilder
import org.hortonmachine.modules.LineSmootherMcMaster
import org.hortonmachine.modules.RiverSectionsExtractor
import org.hortonmachine.modules.VectorFilter

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the main channel: the reaches of Hack order 1
var filter = new VectorFilter()
filter.inVector = folder + "river_net.shp"
filter.pCql = "hack = 1"
filter.outVector = folder + "river_main.shp"
filter.process()

// the network follows the cells in steps of 45 degrees: smooth it, so that the
// sections, perpendicular to the line, cross the valley
var smoother = new LineSmootherMcMaster()
smoother.inVector = folder + "river_main.shp"
smoother.pLookahead = 41
// then simplified, so that also its ends follow the direction of the river and not the cells
smoother.pSimplify = 20
smoother.outVector = folder + "river_main_smooth.shp"
smoother.process()

// a section every 100 m, reaching 350 m on each side: over the whole floodplain,
// up the sides of the valley, but not so far that they cross each other
var extractor = new RiverSectionsExtractor()
extractor.inElev = folder + "river.tif"
extractor.inRiver = folder + "river_main_smooth.shp"
extractor.pSectionsIntervalDistance = 100
extractor.pSectionsWidth = 350
extractor.outSections = folder + "river_sections.shp"
extractor.outSectionPoints = folder + "river_section_points.shp"
extractor.outRiverPoints = folder + "river_points.shp"
extractor.process()

// the same sections, also for HEC-RAS
var hecras = new HecrasInputBuilder()
hecras.inRiverPoints = folder + "river_points.shp"
hecras.inSections = folder + "river_sections.shp"
hecras.inSectionPoints = folder + "river_section_points.shp"
hecras.pTitle = "river"
hecras.outHecras = folder + "river.sdf"
hecras.process()
