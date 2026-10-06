import org.hortonmachine.modules.Markoutlets

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// mark with 10 the outlet of the drainage directions cut to the basin
var markoutlets = new Markoutlets()
markoutlets.inFlow = folder + "drain_basin.tif"
markoutlets.outFlow = folder + "drain_marked.tif"
markoutlets.process()
