import org.hortonmachine.modules.NetcdfGridDumper

// the folder with the MODIS tile: change it to yours
var folder = "/data/modis/"

// the daytime land surface temperature of the tile, in kelvin, resampled to lat/long WGS84
var dumper = new NetcdfGridDumper()
dumper.inPath = folder + "MOD11A2.A2024201.h18v04.061.2024211134014.hdf"
dumper.pGridName = "MODIS_Grid_8Day_1km_LST/Data_Fields/LST_Day_1km"
dumper.outFolder = folder + "modis_lst"
dumper.process()
