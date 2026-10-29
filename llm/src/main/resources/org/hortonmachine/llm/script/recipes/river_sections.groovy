// Extract cross sections of a river from an elevation model at a regular distance, optionally keeping surveyed sections
// keywords: river, sections, cross sections, transects, profiles, channel, hydraulics, survey, dem, riversectionsextractor
import org.hortonmachine.* // the HM helper
import org.hortonmachine.hmachine.modules.hydrogeomorphology.riversections.OmsRiverSectionsExtractor

def folder = "/path/to/folder/"
def dem = folder + "dem.tif"
// the river axis as a line, drawn from upstream to downstream
def river = folder + "river.shp"
// optional surveyed sections, kept as they are, or null
def surveyedSections = null
def distance = 10.0 // between the sections, in map units
def width = 15.0 // of the sections

// this class takes the data, not paths: read them first
def extractor = new OmsRiverSectionsExtractor()
extractor.inElev = HM.readRaster(dem)
extractor.inRiver = HM.readVectorFc(river)
if (surveyedSections != null) {
    extractor.inSections = HM.readVectorFc(surveyedSections)
}
extractor.pSectionsIntervalDistance = distance
extractor.pSectionsWidth = width
extractor.process()

HM.dumpVector(extractor.outSections, folder + "sections.shp")
HM.dumpVector(extractor.outSectionPoints, folder + "section_points.shp")
HM.dumpVector(extractor.outRiverPoints, folder + "river_points.shp")
