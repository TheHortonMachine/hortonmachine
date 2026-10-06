import org.hortonmachine.modules.HackLength

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var hackLength = new HackLength()
hackLength.inFlow = folder + "drain_marked.tif"
hackLength.inTca = folder + "tca.tif"
hackLength.outHacklength = folder + "hacklength.tif"
hackLength.process()
