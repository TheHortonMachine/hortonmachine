# SWYQuickflow

The third step of the [Seasonal Water Yield](index.md) workflow: the quickflow of each month, from its precipitation, the curve number of the cells and the number of rain events of the month (see [the model](index.md#the-model)).

The number of rain events is a map, so that it can change in space: a single value for the whole watershed, as in the example, or one value for each climate zone. On the cells of the network all the precipitation becomes quickflow, also where the curve number is not defined, as in InVEST.

```{literalinclude} ../../examples/swy/03_quickflow.groovy
:language: groovy
```

:::{figure} ../../../images/modules/SWYQuickflow_qf_4.png
:alt: The quickflow of April, in mm.

The quickflow of April, in mm: the highest on the network, which gets all the precipitation, and on the soils with a high curve number of the east.
:::

```{include} ../../generated/SWYQuickflow.md
```
