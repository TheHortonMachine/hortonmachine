# RasterExtruder

Puts the values of a numeric field of polygons into a raster. The cells inside each polygon either get the value of the polygon, replacing theirs, or, with the add option, get it added to theirs. The cells outside the polygons are not changed.

Adding is the extrusion of the name: with the heights of the buildings, an elevation model of the terrain becomes one of the surface, with the buildings standing on it, for the analyses of visibility, shadows or flooding in urban areas. Replacing sets the cells to a given value, as the level of a lake.

Unlike [BobTheBuilder](bobthebuilder.md), which interpolates the elevations of the artifact from points, the extruded polygon follows the shape of the terrain under it, raised by the same height everywhere.

## Example

The example raises the polygon of the dam of [BobTheBuilder](bobthebuilder.md), `dam.geojson` in `docs/manual/modules/maps/data`, by its field `height`, 20 m, over the sample elevation model of the manual:

```{literalinclude} ../examples/hm/rasterextruder.groovy
:language: groovy
```

:::{figure} ../../images/modules/RasterExtruder_dtm_wall-dtm_flanginec.png
:alt: The difference between the extruded and the original elevation model: the polygon of the dam is 20 m higher everywhere, also on the slopes, where BobTheBuilder left the terrain unchanged.

The difference between the extruded and the original elevation model: the polygon of the dam is 20 m higher everywhere, also on the slopes, where BobTheBuilder left the terrain unchanged.
:::

## Reference

```{include} ../generated/RasterExtruder.md
```
