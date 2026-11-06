# RasterTransitionClasses

Compares two rasters of classes of different times and gives each change from a class to another its own code. It maps what changed into what, as the forest that became pasture or the pasture that became urban between two land covers, and with its table of the transitions it gives the area of each change, as a transition matrix. To compare the values of two continuous rasters instead, use [RasterDiff](rasterdiff.md).

## Details

```{include} ../generated/RasterTransitionClasses_details.md
```

## Parameters

```{include} ../generated/RasterTransitionClasses.md
```

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, on the outputs of the [RangeLookup](rangelookup.md) example. It raises the limits of the three elevation belts by 200 m, as for a rising treeline, and maps the cells that moved from a belt to another:

```{literalinclude} ../examples/hm/rastertransitionclasses.groovy
:language: groovy
```

:::{figure} ../../images/modules/RasterTransitionClasses_belts_transitions.png
:alt: The transitions between the elevation belts after a rise of 200 m of their limits.

The transitions between the elevation belts after a rise of their limits: the cells that moved to a lower belt and the ones that stayed in their belt.
:::
