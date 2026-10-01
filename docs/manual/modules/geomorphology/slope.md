# Slope

The slope of each cell along its drainage direction: the drop to the downstream cell over their distance. It is the slope the water actually follows, used by the hydrological indices like the [topographic index](../basin/topindex.md), while the [Gradient](gradient.md) is the steepest slope in any direction.

The drainage directions should come from the same depitted elevation map, with [FlowDirections](flowdirections.md) or [DrainDir](draindir.md): otherwise the downstream cell can be higher, and the slope negative.

```{include} ../generated/Slope.md
```
