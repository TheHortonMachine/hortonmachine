# Hillshade

The hillshade shows the terrain as lit by the sun from a direction (the azimuth, clockwise from north) and an elevation over the horizon. Unlike the simple hillshades, it also casts the shadows of the terrain: the slopes hidden from the sun by the surrounding mountains are dark, even if they face it.

The usual hillshade for maps, as the one under the maps of this manual, lights the terrain from the northwest (azimuth 315) at 45 degrees. A low sun gives long shadows, which show the relief of flat areas better.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/hillshade.groovy
:language: groovy
```

:::{figure} ../../images/modules/Hillshade_shade.png
:alt: The hillshade of the sample area with a low sun from the west (azimuth 270, elevation 25): the long shadows of the ridges fall on the slopes to the east. The cells at the border of the map, without all the neighbours, are 0.

The hillshade of the sample area with a low sun from the west (azimuth 270, elevation 25): the long shadows of the ridges fall on the slopes to the east. The cells at the border of the map, without all the neighbours, are 0.
:::

## Reference

```{include} ../generated/Hillshade.md
```
