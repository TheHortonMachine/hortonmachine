# RasterCompare

Compares two rasters: their coordinate reference systems and grids, and, on the same grid, their values cell by cell. It checks that a computation repeated with other settings or another version of a module gives the same result, and where and by how much two rasters differ.

## Details

```{include} ../generated/RasterCompare_details.md
```

## Parameters

```{include} ../generated/RasterCompare.md
```

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/rastercompare.groovy
:language: groovy
```

The report of the sample elevation model and its rounding to the decimeter by [RasterValueRounder](rastervaluerounder.md):

```text
GRID GEOMETRY
================================
The rasters have the same bounds and resolution.

VALUES
================================
Equal cells: 261
Different cells: 123501
Cells with novalue in only one raster: 0
Cells with novalue in both rasters: 4393
Max absolute difference: 0.0500000000001819
Mean absolute difference: 0.025062832886505352
First different cells (raster 1 - raster 2):
	col 7, row 0: 1207.594970703125 - 1207.6
	col 8, row 0: 1217.385986328125 - 1217.4
	col 9, row 0: 1223.239990234375 - 1223.2
```

The rounding moves the values by 2.5 cm on average and by 5 cm at most, as it should.
