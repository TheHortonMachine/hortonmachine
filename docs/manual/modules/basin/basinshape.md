# BasinShape

Converts a raster of numbered basins, as the sub-basins of [NetNumbering](../network/netnumbering.md), into polygons, with the area, the perimeter and the elevation statistics of each basin in the attributes table. The polygons bring the sub-basins into the vector tools: to label and query them, to join other data to them, or to use them as the hydrological response units of the semi-distributed models.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/basinshape.groovy
:language: groovy
```

:::{figure} ../../images/modules/BasinShape_subbasins_avgelev.png
:alt: The sub-basins of NetNumbering as polygons, colored by their mean elevation in meters.

The sub-basins of NetNumbering as polygons, colored by their mean elevation in meters.
:::

## Reference

```{include} ../generated/BasinShape.md
```
