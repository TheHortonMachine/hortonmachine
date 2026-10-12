# RasterCatToFeatureAttribute

Adds to the features of a vector the values of a raster below them. The points and the lines take the value of a cell, as the elevation of the wells or the land use at the start of the roads; the polygons take the minimum, maximum, mean and sum of the cells inside them, as the mean elevation of the sub-basins or the rain volume on a municipality.

## Example

The example adds the elevation statistics of the sample elevation model to the cells of [GridsGenerator](gridsgenerator.md):

```{literalinclude} ../examples/hm/rastercattofeatureattribute.groovy
:language: groovy
```

Each cell of 500 m has 2500 cells of the elevation model of 10 m inside it. The first cells, along the southern border from the west:

| id | elev_min | elev_max | elev_avg | elev_sum |
|---|---|---|---|---|
| 0 | 961.2 | 1198.8 | 1078.6 | 2696518 |
| 1 | 1137.4 | 1452.8 | 1303.8 | 3259448 |
| 2 | 1341.7 | 1639.6 | 1520.5 | 3801347 |
| 3 | 1612.8 | 1847.3 | 1743.0 | 4357500 |

The cells colored by their mean elevation:

```{image} ../../images/modules/RasterCatToFeatureAttribute_grid_elev_elev_avg.png
:alt: The cells of the grid colored by their mean elevation
```

## Reference

```{include} ../generated/RasterCatToFeatureAttribute.md
```
