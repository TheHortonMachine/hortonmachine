import org.hortonmachine.modules.LineSmootherMcMaster
import org.hortonmachine.modules.RiverSectionsExtractor
import org.hortonmachine.modules.VectorFilter

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the main channel of the basin: the reaches of Hack order 1 of NetworkAttributesBuilder
var filter = new VectorFilter()
filter.inVector = folder + "net_attributes.shp"
filter.pCql = "hack = 1"
filter.outVector = folder + "main_channel.shp"
filter.process()

// the network follows the cells in steps of 45 degrees: smooth it, so that the
// sections, perpendicular to the line, cross the valley
var smoother = new LineSmootherMcMaster()
smoother.inVector = folder + "main_channel.shp"
smoother.pLookahead = 41
// then simplified, so that also its ends follow the direction of the valley and not the cells
smoother.pSimplify = 20
smoother.outVector = folder + "main_channel_smooth.shp"
smoother.process()

// a section every 50 m along the channel, reaching 60 m on each side, its points taken on the elevation model
var extractor = new RiverSectionsExtractor()
extractor.inElev = folder + "dtm_flanginec.tif"
extractor.inRiver = folder + "main_channel_smooth.shp"
extractor.pSectionsIntervalDistance = 50
extractor.pSectionsWidth = 60
extractor.outSections = folder + "sections.shp"
extractor.outSectionPoints = folder + "section_points.shp"
extractor.outRiverPoints = folder + "river_points.shp"
extractor.process()
