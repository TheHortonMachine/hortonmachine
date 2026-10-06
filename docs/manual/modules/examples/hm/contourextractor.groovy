import org.hortonmachine.modules.ContourExtractor

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// a contour line every 100 m
var contours = new ContourExtractor()
contours.inCoverage = folder + "dtm_flanginec.tif"
contours.pMin = 900.0
contours.pMax = 2100.0
contours.pInterval = 100.0
contours.outGeodata = folder + "contours.shp"
contours.process()
