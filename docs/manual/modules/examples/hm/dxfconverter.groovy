import org.hortonmachine.modules.DxfConverter

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

var dxf = new DxfConverter()
// a drawing with the outline of the basin and the river network, in the layers basin and network
dxf.file = folder + "flanginec.dxf"
// the dxf has no prj file next to it: its crs
dxf.pCode = "EPSG:3003"
dxf.pointsVector = folder + "dxf_points.shp"
dxf.lineVector = folder + "dxf_lines.shp"
dxf.polygonVector = folder + "dxf_polygons.shp"
dxf.process()
