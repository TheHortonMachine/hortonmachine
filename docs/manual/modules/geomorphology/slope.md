# Slope

The slope of each cell along its drainage direction: the drop to the downstream cell over their distance. It is the slope the water actually follows, used by the hydrological indices like the [topographic index](../basin/topindex.md), while the [Gradient](gradient.md) is the steepest slope in any direction.

The drainage directions should come from the same depitted elevation map, with [FlowDirections](flowdirections.md) or [DrainDir](draindir.md): otherwise the downstream cell can be higher, and the slope negative.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/slope.groovy
:language: groovy
```

:::{figure} ../../images/modules/Slope_slope.png
:alt: The slope of the sample area along the drainage directions of DrainDir, as the tangent of the slope angle.

The slope of the sample area along the drainage directions of DrainDir, as the tangent of the slope angle.
:::

## Reference

```{include} ../generated/Slope.md
```
