import org.hortonmachine.modules.HazardClassifier

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the intensities of the three floods of IntensityClassifierFlood
var hazard = new HazardClassifier()
hazard.inIntensityTr30 = folder + "intensity_tr30.tif"
hazard.inIntensityTr100 = folder + "intensity_tr100.tif"
hazard.inIntensityTr200 = folder + "intensity_tr200.tif"
hazard.outHazardIP1 = folder + "hazard_ip1.tif"
hazard.outHazardIP2 = folder + "hazard_ip2.tif"
hazard.process()
