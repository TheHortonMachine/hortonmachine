# VectorFilter

Keeps the features of a vector that satisfy a filter, written in ECQL: a condition on their fields, as the sub-basins larger than a hectare, or on their geometry, as the features inside a box or near a point.

## Example

The example keeps the cells of the grid of [RasterCatToFeatureAttribute](rastercattofeatureattribute.md) that are high, with a mean elevation above 1900 m and no part below 1700 m:

```{literalinclude} ../examples/hm/vectorfilter.groovy
:language: groovy
```

Of the 49 cells, 3 satisfy both conditions: two on the ridge along the northern border and one on the eastern border:

```{image} ../../images/modules/VectorFilter_grid_high_elev_avg.png
:alt: The three cells of the grid above 1900 m
```

## Reference

```{include} ../generated/VectorFilter.md
```
