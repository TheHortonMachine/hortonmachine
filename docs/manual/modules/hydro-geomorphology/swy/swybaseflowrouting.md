# SWYBaseflowRouting

The last step of the [Seasonal Water Yield](index.md) workflow: the accumulated recharge, the part of it that reaches the streams and the baseflow of each cell, with its values index (see [the model](index.md#the-model)).

The module also gives the total recharge `outQb`, the sum of the recharge of all the cells in mm: divided by the number of cells it is the mean recharge `qb` of InVEST, times the cell area a volume. The sum of the values index `outVriSum` is 1, a check of the run.

## Details

```{include} ../../generated/SWYBaseflowRouting_details.md
```

## Parameters

```{include} ../../generated/SWYBaseflowRouting.md
```

## Example

```{literalinclude} ../../examples/swy/05_baseflow.groovy
:language: groovy
```

:::{figure} ../../../images/modules/SWYBaseflowRouting_l_sum.png
:alt: The recharge accumulated along the flowdirections, in mm.

The recharge accumulated along the flowdirections, in mm, on a logarithmic scale.
:::

:::{figure} ../../../images/modules/SWYBaseflowRouting_b.png
:alt: The baseflow of each cell, in mm.

The baseflow of each cell, in mm: the part of its recharge that reaches the streams, 0 on the streams, where the recharge is negative.
:::

## The results compared with InVEST

With the flowdirections and the network of the [preparation step](index.md#the-sample-data), instead of those of InVEST, the results differ only where the flow does:

| | HortonMachine | InVEST |
|---|---|---|
| cells with results | 473499 | 480456 |
| mean recharge | 372.5 mm | 363.8 mm |
| total recharge | 39.7 million m³ | 39.3 million m³ |
| mean baseflow | 387.7 mm | 379.5 mm |

The curve number, the precipitation, the reference evapotranspiration and the crop coefficients are the same in every cell, and the quickflow differs only on the 5131 cells where the network of the HortonMachine and the streams of InVEST are not the same. The flowdirections of the HortonMachine leave out the cells at the border of the elevation model, which drain out of it. The total recharge differs by 1%.
