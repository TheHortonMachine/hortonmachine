import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter
import org.hortonmachine.gears.utils.crs.HMCrsRegistry
import org.hortonmachine.gears.utils.features.FeatureUtilities
import org.hortonmachine.modules.VectorCleaner
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.util.AffineTransformation
import org.locationtech.jts.io.WKTReader

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// some parcels digitized by hand, with the usual errors, in meters from a corner
var parcels = [
    // the outline crosses itself
    "MULTIPOLYGON (((20 20, 120 120, 120 20, 20 120, 20 20)))",
    // a small hole of 36 m2, a narrow one of 2 m and a big one
    "MULTIPOLYGON (((150 20, 300 20, 300 140, 150 140, 150 20), (170 40, 176 40, 176 46, 170 46, 170 40), "
            + "(190 80, 192 80, 192 130, 190 130, 190 80), (220 60, 260 60, 260 100, 220 100, 220 60)))",
    // a spike of 2 m
    "MULTIPOLYGON (((20 160, 120 160, 120 200, 165 200, 165 202, 120 202, 120 260, 20 260, 20 160)))",
    // a notch of 2 m
    "MULTIPOLYGON (((170 160, 300 160, 300 260, 170 260, 170 212, 260 212, 260 210, 170 210, 170 160)))",
    // two neighbors that overlap by 4 m
    "MULTIPOLYGON (((20 290, 140 290, 140 380, 20 380, 20 290)))",
    "MULTIPOLYGON (((136 290, 260 290, 260 380, 136 380, 136 290)))",
    // two parts with a gap of 1.5 m between them
    "MULTIPOLYGON (((280 290, 330 290, 330 380, 280 380, 280 290)), ((331.5 290, 380 290, 380 380, 331.5 380, 331.5 290)))"
]
var reader = new WKTReader()
// moved into the area of the elevation model
var shift = AffineTransformation.translationInstance(1638500, 5112500)
var geometries = parcels.withIndex().collect { wkt, i ->
    var geometry = shift.transform(reader.read(wkt))
    // the number of the parcel, written in the userdata field
    geometry.userData = i + 1
    geometry
} as Geometry[]
var crs = HMCrsRegistry.INSTANCE.getCrs("EPSG:3003")
OmsVectorWriter.writeVector(folder + "parcels.gpkg#parcels", FeatureUtilities.featureCollectionFromGeometry(crs, geometries))

var cleaner = new VectorCleaner()
cleaner.inMap = folder + "parcels.gpkg#parcels"
// fill the holes up to 50 m2
cleaner.pMaxHoleArea = 50
// cut the spikes and fill the holes, notches and gaps narrower than 5 m, if longer than 30 m
cleaner.pMinWidth = 5
cleaner.pMinSliverLength = 30
// the larger of two neighbors keeps their overlap
cleaner.pOverlaps = "largest"
cleaner.outMap = folder + "parcels.gpkg#parcels_cleaned"
cleaner.outChanges = folder + "parcels.gpkg#parcels_changes"
cleaner.process()
