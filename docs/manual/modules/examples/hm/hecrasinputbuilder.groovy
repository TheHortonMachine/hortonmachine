import org.hortonmachine.modules.HecrasInputBuilder

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the sections of RiverSectionsExtractor, written for the GIS import of HEC-RAS
var hecras = new HecrasInputBuilder()
hecras.inRiverPoints = folder + "river_points.shp"
hecras.inSections = folder + "sections.shp"
hecras.inSectionPoints = folder + "section_points.shp"
hecras.pTitle = "flanginec"
hecras.outHecras = folder + "flanginec.sdf"
hecras.process()
