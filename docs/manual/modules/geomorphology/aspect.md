# Aspect

The aspect is the direction a slope faces, clockwise from north: 0 for the slopes facing north, 90 east, 180 south and 270 west. With the slope, it sets how much sun a slope receives, and so its temperature, its snow cover and its vegetation.

The aspect is computed from the differences of elevation of the four neighbours of each cell. It is not defined on flat cells, and the cells at the border of the map, without all the neighbours, are left as no-data.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/aspect.groovy
:language: groovy
```

:::{figure} ../../images/modules/Aspect_aspect.png
:alt: The aspect of the sample area, clockwise from north, with the cyclic colortable of the aspect: north (0 and 360) is light, south (180) dark.

The aspect of the sample area, clockwise from north, with the cyclic colortable of the aspect: north (0 and 360) is light, south (180) dark.
:::

## Reference

```{include} ../generated/Aspect.md
```
