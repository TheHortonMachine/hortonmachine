import org.hortonmachine.modules.NetcdfInfo

// the folder with the MODIS tile: change it to yours
var folder = "/data/modis/"

// the variables, coordinate systems and grids of the MODIS land surface temperature tile
var info = new NetcdfInfo()
info.inPath = folder + "MOD11A2.A2024201.h18v04.061.2024211134014.hdf"
info.process()
