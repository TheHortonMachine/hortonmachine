import org.hortonmachine.modules.LinesPolygonizer
import org.hortonmachine.modules.VectorMerger
import org.hortonmachine.modules.VectorReshaper

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the outline of the basin of ExtractBasin as a line, with the same fields as the contours
var reshaper = new VectorReshaper()
reshaper.inVector = folder + "basin.shp"
reshaper.pCql = """the_geom=boundary(the_geom)
value=0.0"""
reshaper.pRemove = "area"
reshaper.outVector = folder + "basin_outline.shp"
reshaper.process()

// together with the contour lines cut to the basin by VectorClipper
var merger = new VectorMerger()
merger.inVector1 = folder + "contours_basin.shp"
merger.inVector2 = folder + "basin_outline.shp"
merger.outVector = folder + "basin_lines.shp"
merger.process()

// the faces between the contours and the divide: the elevation belts of the basin
var polygonizer = new LinesPolygonizer()
polygonizer.inMap = folder + "basin_lines.shp"
polygonizer.fNewId = "belt"
polygonizer.outMap = folder + "basin_belts.shp"
polygonizer.process()
