import org.hortonmachine.modules.PotentialEvapotranspiredWaterVolume

// the folder with the gura folder of the sample data: change it to yours
var folder = "/data/"
var monthly = folder + "gura/monthly/"

// the potential evapotranspiration of each month: crop coefficient times reference evapotranspiration
for (month in 1..12) {
    var pet = new PotentialEvapotranspiredWaterVolume()
    pet.inCropCoefficient = monthly + "kc_" + month + ".tif"
    pet.inReferenceEtp = monthly + "et0_" + month + ".tif"
    pet.outputPet = monthly + "pet_" + month + ".tif"
    pet.process()
}
