import org.hortonmachine.modules.DebrisFlow

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var debrisFlow = new DebrisFlow()
// the depitted elevation, so that the paths are not stopped by the depressions
debrisFlow.inElev = folder + "pit.tif"
// the start point, where the channel opens into the valley
debrisFlow.pEasting = 1638705.0
debrisFlow.pNorthing = 5112805.0
// a debris flow of 50000 m3
debrisFlow.pVolume = 50000
debrisFlow.pMontecarlo = 100
debrisFlow.outMcs = folder + "debris_probability.tif"
debrisFlow.outDepo = folder + "debris_deposit.tif"
debrisFlow.process()
