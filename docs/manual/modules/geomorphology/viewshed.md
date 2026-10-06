# Viewshed

Calculates the viewshed of one or more viewpoints: the cells of the elevation model visible from them, with the number of viewpoints that see each cell. Each viewpoint is at a height above the elevation model, taken from a field of the points or from the default height, as the eyes of a person or the top of a tower. The cells not visible are no-data. The viewpoints on the border of the map are ignored.

It answers questions as what a planned building or antenna would be seen from, or which areas a set of observation points covers.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/viewshed.groovy
:language: groovy
```

:::{figure} ../../images/modules/Viewshed_viewshed.png
:alt: The cells visible from the outlet of the basin of ExtractBasin, 2 m above the ground: the valley up to the ridges, and the slopes in front.

The cells visible from the outlet of the basin of ExtractBasin, 2 m above the ground: the valley up to the ridges, and the slopes in front.
:::

## Reference

```{include} ../generated/Viewshed.md
```
