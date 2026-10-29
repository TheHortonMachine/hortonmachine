// Reproject a geometry given as WKT, or a single coordinate, from one projection to another
// keywords: reproject, transform, projection, crs, epsg, wkt, geometry, coordinate, point, convert coordinates, hmcrstransformer
import org.hortonmachine.gears.utils.crs.HMCrsTransformer
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.io.WKTReader

// a polygon in web mercator, x then y
def wkt = "POLYGON ((1230000 5790000, 1240000 5790000, 1240000 5800000, 1230000 5800000, 1230000 5790000))"
def fromEpsg = "EPSG:3857"
def toEpsg = "EPSG:4326"

// the last two arguments ask for longitude first, also for EPSG:4326
def transformer = new HMCrsTransformer(fromEpsg, toEpsg, true, true)
// accept datums without an exact conversion, with a small error
transformer.setAcceptLenientDatumShift(true)

def geometry = new WKTReader().read(wkt)
println transformer.transform(geometry)

// a single coordinate
println transformer.transform(new Coordinate(1235000, 5795000))
