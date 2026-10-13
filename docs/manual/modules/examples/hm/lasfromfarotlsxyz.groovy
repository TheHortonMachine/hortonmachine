import org.hortonmachine.lesto.modules.utilities.LasFromFaroTlsXyz

// the folder with the scan: change it to yours
var folder = "/data/scans/"

// a scan exported by SCENE as xyz scan points, with the scanner placed at
// 681270 E, 5127117 N in UTM 32N, keeping the points within 15 m of it
var converter = new LasFromFaroTlsXyz()
converter.inFile = folder + "scan_points.xyz"
converter.pLongitude = 681270.0
converter.pLatitude = 5127117.0
converter.pMaxDistance = 15.0
converter.pCode = "EPSG:32632"
converter.outLas = folder + "scan.las"
converter.process()
