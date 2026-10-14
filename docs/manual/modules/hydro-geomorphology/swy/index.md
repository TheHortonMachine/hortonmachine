# Seasonal Water Yield

The Seasonal Water Yield modules implement the model of the same name of InVEST, the toolset of the Natural Capital Project that maps the services that nature gives to people (see the references of the module pages). The model tells how much each cell of a watershed contributes to the water of the streams: to the **quickflow**, the fast runoff of the rain events, and to the **baseflow**, the water that infiltrates and reaches the streams slowly, through the ground, and keeps them flowing between the rains. It is not a model of the discharge in time: it gives the annual recharge and baseflow of each cell, to compare scenarios of land use and management, and to see which areas matter most for the dry season flows.

## The workflow

The modules run in order, from the monthly climate and the maps of the watershed:

| Step | Module | Result |
|---|---|---|
| 1 | preparation, with other modules | the flowdirections and the network, the curve number, and for each month the precipitation, the reference evapotranspiration, the crop coefficient and the number of rain events, on the grid of the elevation model |
| 2 | [PotentialEvapotranspiredWaterVolume](potentialevapotranspiredwatervolume.md) | the potential evapotranspiration of each month |
| 3 | [SWYQuickflow](swyquickflow.md) | the quickflow of each month |
| 4 | [SWYRechargeRouting](swyrechargerouting.md) | the actual evapotranspiration and the local recharge of the year |
| 5 | [SWYBaseflowRouting](swybaseflowrouting.md) | the baseflow of each cell and its share of the total recharge |

The modules can be run from the [Spatial Toolbox](../../../apps/spatialtoolbox.md), in the Hydro-Geomorphology folder, or with scripts in the [Geoscript Console](../../../apps/geoscript.md). The page of each module has the script of its step: together they are a complete run on the sample data of InVEST, and only the `folder` at the top of each script needs to be changed to run them on other data.

## The model

**Quickflow.** The quickflow of each month comes from the curve number method of the Soil Conservation Service, applied to the rain events of the month. The depth of the events is assumed exponentially distributed around their mean, and the quickflow is the expected value of the runoff:

```{math}
QF_{i,m} = 25.4 \, n_m \left[ (a_{i,m} - S_i) \, e^{-0.2 S_i / a_{i,m}} + \frac{S_i^2}{a_{i,m}} \, e^{0.8 S_i / a_{i,m}} \, E_1\!\left(\frac{S_i}{a_{i,m}}\right) \right]
```

with {math}`n_m` the number of rain events of month m, {math}`S_i = 1000 / CN_i - 10` the potential retention of cell i from its curve number, {math}`a_{i,m} = P_{i,m} / (25.4 \, n_m)` the mean depth of an event, both in inches, and {math}`E_1` the exponential integral. On the streams all the precipitation becomes quickflow.

**Local recharge.** The water left by the quickflow is evapotranspired, up to the potential evapotranspiration of the month, also using a part of the water that comes from the cells upslope, the upslope subsidy {math}`L_{sum.avail}`. What is left over the year is the local recharge {math}`L_i`:

```{math}
AET_i = \sum_m \min\left(PET_{i,m},\; P_{i,m} - QF_{i,m} + \alpha_m \, \beta \, L_{sum.avail,i}\right)
\qquad
L_i = \sum_m \left(P_{i,m} - QF_{i,m}\right) - AET_i
```

The fraction γ of the recharge is available to the cells downslope, {math}`L_{avail,i} = \min(\gamma L_i, L_i)`, and the upslope subsidy of a cell is the mean, over the cells that drain into it, of their available recharge and of their own upslope subsidy. The parameters are:

- α, the part of the upslope subsidy available in each month: 1/12 by default, the same in every month;
- β, the part of the upslope subsidy that can be evapotranspired, from the topography and the geology: 1 by default;
- γ, the part of the recharge available to the cells downslope, from how much of it goes into a local, shallow aquifer instead of a deep one: 1 by default.

The recharge is negative where a cell evapotranspires more than what is left by its own quickflow, as on the streams, which get the upslope water.

**Baseflow.** The recharge accumulates along the flowdirections, {math}`L_{sum}`. Walking up from the streams, each cell gets the part of its accumulated recharge that reaches the streams, without being evapotranspired by the cells downslope on the way:

```{math}
B_{sum,i} = L_{sum,i} \left(1 - \frac{L_{avail,j}}{L_{sum,j}}\right) \frac{B_{sum,j}}{L_{sum,j} - L_j}
```

with j the cell downslope of i; {math}`B_{sum,i} = L_{sum,i}` where j is a stream or i drains out of the map. The baseflow of the cell is its share of {math}`B_{sum}`, never negative, {math}`B_i = \max(B_{sum,i} \, L_i / L_{sum,i}, 0)`, and its values index is its share of the total recharge, {math}`VRI_i = L_i / \sum L`.

## One year, or single intervals

SWYRechargeRouting runs in two modes, which are not the same model:

- the **annual balance**, with the rasters of all the months, as folders with name prefixes or, from scripts, as lists: this is the model of InVEST;
- the **single interval**, with the rasters of one interval only, as a month: each interval is computed by itself, as it comes.

:::{warning}
**The annual balance and the single intervals give different results, and the single intervals don't add up to the year of InVEST.**

| | Annual balance | Single interval |
|---|---|---|
| inputs | the precipitation, quickflow and potential evapotranspiration of all the months of the year, together | those of one interval |
| upslope subsidy | from the recharge of the whole year, shared among the months by α (1/12 each by default) | from the recharge of the same interval only, with α 1 by default |
| water carried over | yes: in the dry months the cells can evapotranspire the recharge of the wet months from upslope | none: each interval knows nothing of the others |
| needs | the whole year, also the months after the one of interest | only the data of the interval: it can run month after month, as the data come, as in simulation frameworks that move forward in time |
| results | the same as InVEST | a different model, not validated against InVEST or measures |

On the sample data the sum of the recharge of the 12 single months is about 50% more than the recharge of the year (558 mm against 364 mm), and the baseflow about 49% more: in the single months the recharge of the wet months can't be evapotranspired in the dry ones, so more of it remains as recharge.

Use the annual balance to compare with InVEST or with its applications, and for annual water budgets. Use the single intervals only where the future intervals are not known, and compare their results only among themselves, not with those of InVEST.
:::

## Comparison with InVEST

The modules were checked against InVEST 3.20.2:

- on the synthetic data of the D8 regression test of InVEST, which the tests of the HortonMachine rebuild, all the results are the same, cell by cell;
- on the sample data of InVEST, run in D8 mode, with the flowdirections and the streams of InVEST, all the results of all the cells are the same, to the precision of the InVEST rasters (32 bit floats): the mean recharge is 363.75 mm in both;
- on the same data, with the flowdirections and the network of the HortonMachine of the example, the results change only where the flow does: see the [baseflow page](swybaseflowrouting.md).

The differences from InVEST are:

- only the D8 flowdirections are supported, not the multiple flow directions of InVEST;
- the cells that drain out of the map get the whole accumulated recharge as {math}`B_{sum}`, while InVEST writes 0 there; the baseflow B of the cells is the same;
- the results are not aggregated on watershed polygons: the total recharge and the sum of the values index are those of the whole map, which can be cut to the watershed first;
- the climate zones of InVEST are given directly as maps of the number of rain events of each month, and the monthly α as the list of the α of the months.

## The sample data

The example uses the sample data of InVEST, the Gura watershed in Kenya, in `docs/manual/modules/maps/data/gura` with the description of their sources. They are an example only: their coefficients in particular are globally averaged values, not to be used in real projects.

The first step prepares the inputs of the modules from the original data:

- the flowdirections and the network, with channels where at least 1000 cells drain, as in InVEST, with [Pitfiller](../../dem-manipulation/pitfiller.md), [FlowDirections](../../geomorphology/flowdirections.md), [DrainDir](../../geomorphology/draindir.md) and [ExtractNetwork](../../network/extractnetwork.md);
- the curve number of each cell from its land use and soil group, and the crop coefficient of each month from its land use, with [Mapcalc](../../raster-processing/mapcalc.md) scripts written from the biophysical table;
- the monthly precipitation and reference evapotranspiration, rasters of 1 km cells, resampled on the 15 m grid of the elevation model, with the nearest value, as InVEST does;
- the number of rain events of each month, the same in every cell, from the table of the events.

```{literalinclude} ../../examples/swy/01_preparation.groovy
:language: groovy
```

:::{figure} ../../../images/modules/Swy_cn.png
:alt: The curve number of the cells, from their land use and soil group.

The curve number of the cells, from their land use and soil group.
:::

:::{figure} ../../../images/modules/Swy_net.png
:alt: The network, with channels where at least 1000 cells drain.

The network, with channels where at least 1000 cells drain.
:::

:::{figure} ../../../images/modules/Swy_prcp_4.png
:alt: The precipitation of April, the wettest month, in mm, resampled from 1 km cells.

The precipitation of April, the wettest month, in mm, resampled from 1 km cells.
:::

```{toctree}
:maxdepth: 1

potentialevapotranspiredwatervolume
swyquickflow
swyrechargerouting
swybaseflowrouting
```
