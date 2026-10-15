# NetcdfInfo

Prints what a NetCDF file, or any other file or url that netcdf-java opens, contains: its variables, the coordinate systems with their axes and projections, and its grids, with the extent of each one and its time axis. It is the first module to run on a new dataset, to find the names of the grids to give to [NetcdfGridDumper](netcdfgriddumper.md) and to check their projection, units and fill values.

## Details

```{include} ../generated/NetcdfInfo_details.md
```

## Parameters

```{include} ../generated/NetcdfInfo.md
```

## Example

The example prints the content of the sample MODIS tile:

```{literalinclude} ../examples/hm/netcdfinfo.groovy
:language: groovy
```

The file is an HDF-EOS file with 12 grids: the daytime and nighttime temperature, their quality flags, and the times and view angles of the observations. The first one, as printed in the grid definitions at the end of the report:

```text
0) MODIS_Grid_8Day_1km_LST/Data_Fields/LST_Day_1km:
	Attributes:
		long_name: 8-day daytime 1km grid Land-surface Temperature
		units: K
		Number_Type: uint16
		valid_range: 7500 65535
		_FillValue: 0
		LST: LST data * scale_factor
		scale_factor: 0.02
		...
	Projection: Sinusoidal{earthRadius=6371.007181, centMeridian=0.0, falseEasting=0.0, falseNorthing=0.0}
	Proj X Axis (MODIS_Grid_8Day_1km_LST/Data_Fields/XDim): 0.0 -> 1111.0238943338609   (1200)  extent: 1111.0238943338609
	Proj Y Axis (MODIS_Grid_8Day_1km_LST/Data_Fields/YDim): 4448.728704499139 -> 5559.752598833001   (1200)  extent: 1111.0238943338618
	Longitude: 0.0 -> 15.544273903388074  extent: 15.544273903388074
	Latitude: 40.0083333333325 -> 50.00000000000346  extent: 9.991666666670959
```

The name to use is the full one, with the groups of the HDF file. The temperature is stored as integers to multiply by the scale factor of 0.02 to get kelvin, and 0 marks the cells without data, as the sea and the clouds. The grid is 1200 x 1200 cells on the sinusoidal projection of MODIS, with the axes in kilometers; the tile has no time axis, its date, 19 July 2024, is in the global attribute `_MODIS_Date`.
