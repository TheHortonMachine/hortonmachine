# NetcdfGridDumper

Dumps a grid of a NetCDF file to GeoTIFFs, one for every timestep of a range, for example the days of a climate scenario. The values come out in their physical units, with the scale factor and offset of the file applied and the missing values as novalue.

The grids whose crs is written in the file, as those of GDAL and of [RasterToNetcdf](rastertonetcdf.md), keep their projection and cells unchanged. All the others are resampled to lat/long WGS84 with the nearest cell, as the lat/long grids of most climate data and the grids whose projection has no EPSG code, as the sinusoidal tiles of MODIS.

## Example

The example dumps the daytime land surface temperature of the sample MODIS tile, with the grid name printed by [NetcdfInfo](netcdfinfo.md):

```{literalinclude} ../examples/hm/netcdfgriddumper.groovy
:language: groovy
```

The tile has no time axis, so a single GeoTIFF is written, named after the date of the tile: `MOD11A2.A2024201.h18v04.061.2024211134014__20240719_000000__MODIS_Grid_8Day_1km_LST_Data_Fields_LST_Day_1km.tif` in the folder `modis_lst`. The sinusoidal tile, resampled to lat/long, has slanted east and west borders, outside which the cells are novalue. The Alps are the coolest land, the Po valley and the coasts the hottest; the sea and the cloudy cells, without data, are white:

```{image} ../../images/modules/NetcdfGridDumper_MOD11A2.A2024201.h18v04.061.2024211134014__20240719_000000__MODIS_Grid_8Day_1km_LST_Data_Fields_LST_Day_1km.png
:alt: The daytime land surface temperature in kelvin of the MODIS tile over northern Italy
```

The values are in kelvin: [Mapcalc](../raster-processing/mapcalc.md) converts them to degrees Celsius by subtracting 273.15.

The second example reads back the NetCDF of the sample elevation model written by [RasterToNetcdf](rastertonetcdf.md). The file has its crs, so the GeoTIFF `dtm_back/dtm_flanginec__elevation.tif` has the projection, the cells and the values of the original elevation model:

```{literalinclude} ../examples/hm/netcdfgriddumper_dtm.groovy
:language: groovy
```

```{image} ../../images/modules/NetcdfGridDumper_dtm_flanginec__elevation.png
:alt: The elevation model read back from its NetCDF file
```

## Reference

```{include} ../generated/NetcdfGridDumper.md
```
