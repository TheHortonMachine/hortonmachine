import org.hortonmachine.modules.Insolation

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the direct radiation of the summer solstice
var insolation = new Insolation()
insolation.inElev = folder + "dtm_flanginec.tif"
insolation.tStartDate = "2025-06-21"
insolation.tEndDate = "2025-06-21"
insolation.outIns = folder + "insolation.tif"
insolation.process()
