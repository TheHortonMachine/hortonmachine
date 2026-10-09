import org.hortonmachine.gears.utils.files.FileUtilities
import org.hortonmachine.modules.Profile

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// a section across the valley, along the crest of the dam of BobTheBuilder
var section = "1638168,5112472,1638492,5112148"

var terrain = new Profile()
terrain.inRaster = folder + "dtm_flanginec.tif"
terrain.inCoordinates = section
terrain.process()

var withDam = new Profile()
withDam.inRaster = folder + "dtm_dam.tif"
withDam.inCoordinates = section
withDam.process()

// each row of the profiles: progressive distance, elevation, easting, northing
var csv = new StringBuilder("distance,with_dam,terrain\n")
for (int i = 0; i < terrain.outProfile.length; i++) {
    csv.append(String.format("%.1f,%.1f,%.1f%n", terrain.outProfile[i][0], withDam.outProfile[i][1],
            terrain.outProfile[i][1]))
}
FileUtilities.writeFile(csv.toString(), new File(folder + "dam_section.csv"))
