# SWYQuickflow

The third step of the [Seasonal Water Yield](index.md) workflow: the quickflow of each month, from its precipitation, the curve number of the cells and the number of rain events of the month (see [the model](index.md#the-model)).

The number of rain events is a map, so that it can change in space: a single value for the whole watershed, as in the example, or one value for each climate zone. On the cells of the network all the precipitation becomes quickflow, also where the curve number is not defined, as in InVEST.

## Details

```{include} ../../generated/SWYQuickflow_details.md
```

## Parameters

```{include} ../../generated/SWYQuickflow.md
```

## Example

```{literalinclude} ../../examples/swy/03_quickflow.groovy
:language: groovy
```

:::{figure} ../../../images/modules/SWYQuickflow_qf_4.png
:alt: The quickflow of April, in mm.

The quickflow of April, in mm: the highest on the network, which gets all the precipitation, and on the soils with a high curve number of the east.
:::

## The values

The quickflow is a depth of water in the month, in mm over the cell: the part of the precipitation of the month that runs off fast, in the rain events, and doesn't infiltrate. It can't be more than the precipitation of the month.

On the sample data:

- on the **cells of the network**, all the precipitation is quickflow: up to 343 mm in April. These are the highest values of the maps, and the colors of the other cells are better seen without them;
- on the **other cells** the quickflow of April, the wettest month, is about 21 mm in the middle, of 311 mm of precipitation, and in the dry months, from June to September, it is less than half a millimeter in the middle. In the year it is about 1-3% of the precipitation on the forest and the high cells, 5-6% on the tea and the coffee, and 15-25% on the agriculture, the urban areas and the roads, with the higher curve numbers;
- over the **whole basin** it is 89.6 mm in the year, 5.7% of the precipitation, with three quarters of it in April, May, October and November.

The quickflow grows more than the precipitation: a month with twice the precipitation, with the same number of events, has more than twice the quickflow, since each event must first fill the retention of the soil. With fewer, bigger events, the same precipitation gives more quickflow.

The curve numbers of the sample, between 43 and 99, are global means from the literature, not measured in the basin. On the forest the quickflow is two to three times the surface runoff measured in a forest catchment of Kenya (see [how the values compare with measures](index.md#how-the-values-compare-with-measures)).
