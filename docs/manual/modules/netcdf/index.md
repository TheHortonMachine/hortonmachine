# NetCDF

The modules to read and write NetCDF, the format of most climate, weather and remote sensing data: grids of one or more variables, often with a time axis, described by the attributes of the CF conventions. They are read with the Unidata netcdf-java library, which opens much more than NetCDF 3 and 4: also HDF4 and HDF-EOS, as the MODIS products, HDF5, GRIB, and remote datasets through OPeNDAP and THREDDS urls.

The reading examples run on a MODIS tile in the sample data of the manual, `MOD11A2.A2024201.h18v04.061.2024211134014.hdf` in `docs/manual/modules/maps/data`. It is the land surface temperature of the Terra satellite (product MOD11A2, collection 6.1) at 1 km, as the average of the 8 days from 19 July 2024, on the tile h18v04 of the sinusoidal MODIS grid, which covers northern Italy and the Alps, from 40° to 50° north. The original HDF files of MODIS can be downloaded without registration from the STAC catalog of the [Microsoft Planetary Computer](https://planetarycomputer.microsoft.com/dataset/modis-11A2-061), which also has every band as a Cloud Optimized GeoTIFF; NASA distributes them through its CMR search with an Earthdata login.

The writing example converts the sample elevation model `dtm_flanginec.tif` and reads it back.

```{toctree}
:maxdepth: 1

netcdfgriddumper
netcdfinfo
rastertonetcdf
```
