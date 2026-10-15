# VectorMerger

Merges two vectors with the same fields into a single one, as the maps of two adjacent areas or the results of two filters on the same vector.

## Details

```{include} ../generated/VectorMerger_details.md
```

## Parameters

```{include} ../generated/VectorMerger.md
```

## Example

The example filters the lowest cells of the grid of [RasterCatToFeatureAttribute](rastercattofeatureattribute.md), those entirely below 1500 m, and merges them with the highest ones of the [VectorFilter](vectorfilter.md) example:

```{literalinclude} ../examples/hm/vectormerger.groovy
:language: groovy
```

The merged vector has the 3 high cells and the 12 low ones in the valley to the west:

```{image} ../../images/modules/VectorMerger_grid_extremes_elev_avg.png
:alt: The highest and the lowest cells of the grid in a single vector
```
