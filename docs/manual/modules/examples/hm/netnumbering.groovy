import org.hortonmachine.modules.NetNumbering

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var netNumbering = new NetNumbering()
netNumbering.inFlow = folder + "drain_basin.tif"
netNumbering.inTca = folder + "tca.tif"
netNumbering.inNet = folder + "net_basin.tif"
netNumbering.outNetnum = folder + "netnum.tif"
netNumbering.outBasins = folder + "subbasins.tif"
netNumbering.process()
