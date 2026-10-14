# SWYRechargeRouting

The fourth step of the [Seasonal Water Yield](index.md) workflow: the actual evapotranspiration and the local recharge of the year, routing the recharge available to the cells downslope along the flowdirections (see [the model](index.md#the-model)).

The rasters of the months are given as folders, with a name prefix for each input: `prcp_` finds `prcp_1.tif` to `prcp_12.tif`, ordered by their number. The three folders, which can also be the same one as in the example, need the rasters of the same months. From scripts they can also be given as lists of rasters. Without them, the module runs on a single interval (see [one year, or single intervals](index.md#one-year-or-single-intervals)).

The routing visits the cells along the flow paths, so the module reads the precipitation, the quickflow and the potential evapotranspiration of all the months in memory: two rasters of 8 bytes per cell for each month, 225 MB for the 1.2 million cells of the example.

The network of the old versions of the module is no longer needed: the cells of the streams follow the same balance as all the others. It is ignored, with a warning.

```{literalinclude} ../../examples/swy/04_recharge.groovy
:language: groovy
```

:::{figure} ../../../images/modules/SWYRechargeRouting_aet.png
:alt: The actual evapotranspiration of the year, in mm.

The actual evapotranspiration of the year, in mm.
:::

:::{figure} ../../../images/modules/SWYRechargeRouting_l.png
:alt: The local recharge of the year, in mm.

The local recharge of the year, in mm: high in the wet west, low in the east, and negative on the streams, which evapotranspire the water from upslope.
:::

```{include} ../../generated/SWYRechargeRouting.md
```
