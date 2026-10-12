import org.hortonmachine.modules.RasterToNetcdf

// the folder with the elevation model: change it to yours
var folder = "/data/flanginec/"

// the elevation model as a NetCDF file, a single raster without time
var toNetcdf = new RasterToNetcdf()
toNetcdf.inRaster = folder + "dtm_flanginec.tif"
toNetcdf.pVariableName = "elevation"
toNetcdf.pLongName = "terrain elevation"
toNetcdf.pUnits = "m"
toNetcdf.outNetcdf = folder + "dtm_flanginec.nc"
toNetcdf.process()
