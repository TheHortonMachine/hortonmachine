# RasterToNetcdf

Writes rasters to a NetCDF file that follows the CF conventions: a single raster, or a time series of rasters on the same grid, as the daily outputs of a model, with a time axis. The file keeps the crs of the rasters, so GDAL, QGIS, the other CF readers and [NetcdfGridDumper](netcdfgriddumper.md) place it right.

For a time series, the rasters are the ones of a folder, in the order of their file names, and the timestamps are given one per line in the same order, as `2024-07-01 00:00`, in UTC. The rasters are written one at a time, so a long series doesn't need to fit in memory.

## Example

The example writes the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, as a single raster without time:

```{literalinclude} ../examples/hm/rastertonetcdf.groovy
:language: groovy
```

[NetcdfInfo](netcdfinfo.md) shows the result: the grid `elevation`, with its long name and units, and the projection of the elevation model, Monte Mario / Italy zone 1, a transverse Mercator that netcdf-java reads from the CF parameters of the grid mapping variable `crs`, with the axes converted to kilometers:

```text
0) elevation:
	Attributes:
		long_name: terrain elevation
		units: m
		_FillValue: -9999.0
		grid_mapping: crs
	Projection: TransverseMercatorProjection
	Proj X Axis (x): 1637.145 -> 1640.685   (355)  extent: 3.5399999999999636
	Proj Y Axis (y): 5110.835 -> 5114.435   (361)  extent: 3.600000000000364
	Longitude: 10.775456546318322 -> 10.822330421671925  extent: 0.046873875353602656
	Latitude: 46.13570831830624 -> 46.16881288768088  extent: 0.03310456937463613
```

The second example of [NetcdfGridDumper](netcdfgriddumper.md) reads the file back to the same elevation model.

## Reference

```{include} ../generated/RasterToNetcdf.md
```
