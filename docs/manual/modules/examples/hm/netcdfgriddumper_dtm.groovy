import org.hortonmachine.modules.NetcdfGridDumper

// the folder with the NetCDF of the elevation model, written by RasterToNetcdf: change it to yours
var folder = "/data/flanginec/"

// the elevation back to a GeoTIFF, in its own projection, since the file has its crs
var dumper = new NetcdfGridDumper()
dumper.inPath = folder + "dtm_flanginec.nc"
dumper.pGridName = "elevation"
dumper.outFolder = folder + "dtm_back"
dumper.process()
