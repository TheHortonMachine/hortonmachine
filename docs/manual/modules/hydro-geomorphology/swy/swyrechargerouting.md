# SWYRechargeRouting

The fourth step of the [Seasonal Water Yield](index.md) workflow: the actual evapotranspiration and the local recharge of the year, routing the recharge available to the cells downslope along the flowdirections (see [the model](index.md#the-model)).

The rasters of the months are given as folders, with a name prefix for each input: `prcp_` finds `prcp_1.tif` to `prcp_12.tif`, ordered by their number. The three folders, which can also be the same one as in the example, need the rasters of the same months. From scripts they can also be given as lists of rasters. Without them, the module runs on a single interval (see [one year, or single intervals](index.md#one-year-or-single-intervals)).

The routing visits the cells along the flow paths, so the module reads the precipitation, the quickflow and the potential evapotranspiration of all the months in memory: two rasters of 8 bytes per cell for each month, 225 MB for the 1.2 million cells of the example.

The network of the old versions of the module is no longer needed: the cells of the streams follow the same balance as all the others. It is ignored, with a warning.

## Details

```{include} ../../generated/SWYRechargeRouting_details.md
```

## Parameters

```{include} ../../generated/SWYRechargeRouting.md
```

## Example

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

## The values

All the maps are depths of water in the year, in mm over the cell, except the upslope subsidy. On the sample data:

- the **actual evapotranspiration** is at most the potential one, and is 1187 mm in the middle of the cells, 1108 mm on average, 71% of the precipitation. It is about 520 mm on the urban areas and the roads, whose crop coefficient of 0.4 limits it, and up to about 1440 mm on the cells with the highest potential evapotranspiration. On the cells of the network it is about 710 mm on average, all taken from the upslope water, since there all the precipitation is quickflow;
- the **local recharge** is the precipitation less the quickflow and the actual evapotranspiration. It goes from about 150 mm in the year on average in the lower east of the basin to 700 mm above 2800 m, and up to about 1400 mm where little is evapotranspired. It is negative, down to about -1400 mm, on the 8979 cells of the network, where it is minus their evapotranspiration, and on about 6900 other cells that evapotranspire more than their own water with the water from upslope: most are coffee, with a crop coefficient of 1.05, in the drier east, and some are open water. Over the basin it is 372 mm, 24% of the precipitation;
- the **available recharge** is the recharge times γ, where it is positive: with γ = 1, as in the example, it is the same as the recharge;
- the **upslope subsidy** {math}`L_{sum.avail}` is not a depth of water that reaches the cell. It is the mean, over the cells that drain into it, of their available recharge plus their own upslope subsidy: along a flow path it adds up the recharge of the cells upslope, so it grows downslope, to about 870 mm in the middle and up to 18000 mm on the cells with long flow paths upslope. Of it, only the fraction α β is used in each month, and the evapotranspiration remains at most the potential one: large values only mean that the cell is never short of water.

The recharge is the water left to the ground in a mean year, not the recharge of an aquifer measured in a well: it includes the water that flows in the soil to the streams in a few days or weeks. For the comparison with the measured evapotranspiration of a forest catchment of Kenya, see [how the values compare with measures](index.md#how-the-values-compare-with-measures).
