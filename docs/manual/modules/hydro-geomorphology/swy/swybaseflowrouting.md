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

## The values

The maps have three different units. On the sample data:

- the **accumulated recharge** {math}`L_{sum}` is the sum of the recharge of the cell and of all the cells upslope: mm summed over the cells, not a depth. It goes from the recharge of the cell alone, at the ridges, to 176 million at the outlet, the total recharge of the basin, about 1770 in the middle of the cells. Times 0.001 and the area of the cell it is a volume of the year: the 176 million of the outlet are 39.6 million m³. Divided by the number of cells upslope, the total contributing area, it is their mean recharge in mm;
- the **cumulative baseflow** {math}`B_{sum}` has the same unit. With γ = 1, as in the example, it is the same as the accumulated recharge in every cell: all the recharge of the cells upslope reaches the streams, since none of it is evapotranspired on the way;
- the **baseflow** B is a depth of the year, in mm over the cell: the part of the recharge of the cell that reaches the streams. It goes from 0 to about 1420 mm, 406 mm in the middle of the cells and 388 mm on average. With γ = 1 it is the recharge of the cell where it is positive, {math}`B = \max(L, 0)`, and 0 on the network and on the other cells with a negative recharge. With γ less than 1, part of the recharge is evapotranspired by the cells downslope, and the baseflow is less than the recharge;
- the **values index** VRI is a share, without unit: the recharge of the cell divided by the total recharge of the map, so that the sum of all the cells is 1. Its values are tiny, about 1 divided by the number of cells, here 2 millionths. Times the number of cells it is the recharge of the cell divided by the mean one: on the sample data it goes from -3.75 to 3.81, so the cells that give most give almost 4 times the mean, and a value of 1 is a cell that gives the mean. Since it depends on the number of cells, compare the values index of different maps only after multiplying it by their number of cells.

The baseflow tells where the water of the dry season comes from, not how much of it leaves the basin: its sum over the basin is more than the recharge, see [the water at an outlet](index.md#the-water-at-an-outlet).

## The results compared with InVEST

With the flowdirections and the network of the [preparation step](index.md#the-sample-data), instead of those of InVEST, the results differ only where the flow does:

| | HortonMachine | InVEST |
|---|---|---|
| cells with results | 473499 | 480456 |
| mean recharge | 372.5 mm | 363.8 mm |
| total recharge | 39.7 million m³ | 39.3 million m³ |
| mean baseflow | 387.7 mm | 379.5 mm |

The curve number, the precipitation, the reference evapotranspiration and the crop coefficients are the same in every cell, and the quickflow differs only on the 5131 cells where the network of the HortonMachine and the streams of InVEST are not the same. The flowdirections of the HortonMachine leave out the cells at the border of the elevation model, which drain out of it. The total recharge differs by 1%.
