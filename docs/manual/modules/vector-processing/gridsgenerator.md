# GridsGenerator

Generates a regular grid of cells, of their borders or of their corners: on the bounds of a vector, on those of a raster or from a corner with a cell size. The cells are the units of many analyses by area, as the counts of points, the statistics of a raster with [RasterCatToFeatureAttribute](rastercattofeatureattribute.md) or the sheets of a map series.

## Details

```{include} ../generated/GridsGenerator_details.md
```

## Parameters

```{include} ../generated/GridsGenerator.md
```

## Example

The example puts 7 x 7 cells of 500 m on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, from its lower left corner:

```{literalinclude} ../examples/hm/gridsgenerator.groovy
:language: groovy
```

The 49 cells cover 3.5 km on each side, a little less than the elevation model, so its northern and eastern borders stay out of the grid:

```{image} ../../images/modules/GridsGenerator_grid.png
:alt: The 500 m cells of the grid over the sample elevation model
```
