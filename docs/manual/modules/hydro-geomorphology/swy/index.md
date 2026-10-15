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
| 6 | optional, with other modules | [the water at an outlet](#the-water-at-an-outlet): the volumes of the year in the basin of a closure point |

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

## The water at an outlet

The modules don't give the discharge in a point of the network, but their maps can be summed over the basin of a closure point, to get the water of the year that leaves it there.

**The units.** The maps of the modules are of two kinds:

- the precipitation, the quickflow, the actual evapotranspiration, the local recharge and the baseflow are depths, in mm over the cell, as the measure of a rain gauge: the water of the year of the cell, or of the month for the quickflow. A depth times the area of the cell is a volume: on the cells of 15 m of the example, 1 mm is 0.001 × 225 = 0.225 m³. The volume of a basin is the sum of its cells times the same factor, and the mean depth over the basin is the sum divided by the number of cells.
- the accumulated recharge {math}`L_{sum}` and {math}`B_{sum}` are sums of the depths of the cells upslope: their unit is mm summed over the cells, not a depth. A value of 5 million is not 5 km of water, it is 5 million × 0.225 = 1.1 million m³ of the year from upslope. Divided by the number of cells upslope it is their mean depth.

**The water balance.** Over the year the water of each cell goes to the quickflow, the evapotranspiration or the recharge, {math}`P = QF + AET + L`, which the definition of L in [the model](#the-model) keeps exactly. What is not evapotranspired, the water yield {math}`QF + L = P - AET`, leaves the basin at its outlet, if nothing is stored from one year to the next and no water leaves through a deep aquifer or the ground to another basin. Summed over the basin of the outlet and divided by the seconds of the year, it is the mean discharge of the year.

The baseflow B is not part of this balance. With γ = 1, as in the example, it is the recharge of the cells where it is positive, {math}`B = \max(L, 0)`, and it leaves out the negative recharge of the streams and of the other cells that evapotranspire more than their own water. Summed over the basin, it is more than the recharge. Use it to see where the baseflow comes from, and the recharge for the balance at the outlet.

**Volumes and discharge.** A volume of the year divided by the seconds of the year, 31.5 million, is the mean discharge of the year in m³/s: the constant discharge that would carry the same volume in a year, the mean annual discharge given for the gauges of the rivers. It is not the discharge of any day: the real one is higher in the rainy seasons and lower in the dry ones. In the same way the quickflow of a month divided by the seconds of the month is its mean discharge in that month.

**The script.** It runs on the results of the steps before:

- [ExtractBasin](../../dem-manipulation/extractbasin.md) gives the basin of the outlet from the flowdirections. The outlet must be a cell of the flowdirections, here the cell where the Gura leaves the map at its northern border;
- [Mapcalc](../../raster-processing/mapcalc.md) sums the precipitation of the months into the one of the year;
- [CutOut](../../raster-processing/cutout.md) cuts each map to the basin, in the folder `basin`, and the [RasterSummary](../../raster-processing/rastersummary.md) of the cut maps gives their sums;
- the sums give the mean depths over the basin, the volumes and the mean discharge, of the year and of the quickflow of each month.

```{literalinclude} ../../examples/swy/06_outlet.groovy
:language: groovy
```

On the sample data, the basin of the outlet has 473114 cells, 106.45 km², all the map but the cells at the border that drain out of it elsewhere:

| | depth, mm in the year | volume, million m³ in the year | mean discharge of the year, m³/s |
|---|---|---|---|
| precipitation P | 1570.5 | 167.18 | 5.30 |
| quickflow QF | 89.6 | 9.54 | 0.30 |
| actual evapotranspiration AET | 1108.4 | 117.99 | 3.74 |
| local recharge L | 372.4 | 39.65 | 1.26 |
| **water yield QF + L = P - AET** | **462.0** | **49.18** | **1.56** |
| baseflow B | 387.6 | 41.26 | 1.31 |

:::{figure} ../../../images/modules/Swy_p_year-aet.png
:alt: The water yield of the year of each cell of the basin, P - AET, in mm.

The water yield of the year of each cell of the basin, {math}`P - AET = QF + L`, in mm: what each cell gives to the outlet, at the north-eastern tip of the map. The yield is high in the wet west and low in the east. It is highest on the network, about 850 mm on average, and on the unpaved roads and urban areas, which evapotranspire little.
:::

The precipitation less the quickflow, the evapotranspiration and the recharge is 0.000 mm, a check of the run. About 70% of the precipitation is evapotranspired, and of the water that leaves the basin about a fifth is quickflow and four fifths are recharge. The baseflow is 15.2 mm more than the recharge: it is the sum of the positive recharge, and leaves out the negative one, 13.5 mm of which are the evapotranspiration of the 8979 cells of the network.

The quickflow of the months follows the two rainy seasons of the Gura:

| month | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| depth, mm in the month | 3.1 | 2.6 | 6.1 | 26.5 | 18.3 | 2.2 | 1.6 | 1.7 | 2.0 | 12.5 | 9.4 | 3.7 |
| mean discharge of the month, m³/s | 0.12 | 0.12 | 0.24 | 1.09 | 0.73 | 0.09 | 0.06 | 0.07 | 0.08 | 0.50 | 0.39 | 0.15 |

:::{warning}
**These are the volumes of a water budget, not the discharge of a river.** Before using them, keep in mind that:

- **there is no time in them.** The model has no routing and no storage: the recharge and the baseflow are of the year only, and the model doesn't say in which months they reach the outlet. The quickflow of a month is its mean over the month, with no peaks and no floods. The quickflow of a month plus a twelfth of the recharge is not the discharge of that month;
- **it is the year of the climate given.** With monthly means of many years, as in the sample data, it is a mean year, not a real one, and it assumes that nothing is stored or released from one year to the next;
- **the recharge reaches the outlet only if it stays in the basin.** The balance assumes that all the recharge comes out in the streams of the basin. With γ less than 1, or where the water goes to a deep aquifer or to other basins through the ground, less water reaches the outlet;
- **the baseflow is not a volume at the outlet.** Its sum over the basin leaves out the water evapotranspired by the streams, and is more than what can leave the basin;
- **the coefficients are not calibrated.** The curve numbers and the crop coefficients of the sample data are global means. InVEST itself suggests the model mostly to compare scenarios and to find the areas that matter, not for absolute volumes. The absolute values are an order of magnitude, until they are compared with the measures of a gauge;
- **only the annual balance gives them.** The single intervals give about 50% more recharge, see above;
- **the outlet is a cell of the D8 flowdirections.** The basin is the one of the flowdirections of the model, which can differ from the real one on flat areas and at the borders of the map.
:::

### How the values compare with measures

The values of the [sample data](#the-sample-data) of InVEST, used in the examples, are of the right order for the highlands of Kenya, but they are not measures of the Gura. Over the basin they change mostly with the elevation, from the east, at about 1700 m, to the Aberdare range in the west, above 2800 m:

| elevation, m | 1600-1800 | 1800-2000 | 2000-2200 | 2200-2400 | 2400-2600 | 2600-2800 | above 2800 |
|---|---|---|---|---|---|---|---|
| cells | 55649 | 157462 | 101001 | 60333 | 40436 | 34864 | 23369 |
| precipitation P, mm | 1345 | 1473 | 1608 | 1672 | 1714 | 1764 | 1803 |
| reference evapotranspiration ET0, mm | 1346 | 1309 | 1257 | 1216 | 1177 | 1128 | 1091 |
| quickflow QF, mm | 115 | 116 | 108 | 52 | 40 | 46 | 22 |
| actual evapotranspiration AET, mm | 1077 | 1094 | 1112 | 1159 | 1145 | 1106 | 1076 |
| local recharge L, mm | 154 | 263 | 388 | 462 | 530 | 612 | 705 |
| water yield (QF + L) / P | 20% | 26% | 31% | 31% | 33% | 37% | 40% |

- **The precipitation**, from the CHELSA climatology of the sample data, grows with the elevation, from about 1150 to 1870 mm, with the peaks of the two rainy seasons in April and November. The precipitation of the Aberdare range goes from about 1000 mm on its drier north-western slopes to 3000 mm in the south-east (BirdLife International): the values of the sample data are inside this range.
- **The reference evapotranspiration**, from the modified Hargreaves method, goes from about 1350 mm in the east to 1090 mm on the highest cells. At the synoptic station of Nyeri, at the eastern foot of the Aberdare range, the Penman-Monteith method gives 1488 ± 52 mm in the year (Musyimi et al., 2022): the values of the lowest cells of the sample data, at a similar elevation, are about 10% less. Less reference evapotranspiration gives more recharge.
- **The actual evapotranspiration, the water yield and the quickflow** can be compared with the long measures of the Lagan catchment near Kericho, a montane rain forest at 2200 m in the west of Kenya (Blackie, 1972). In 11 years it had 2050 mm of rain, 721 mm of streamflow and about 1300 mm of evapotranspiration, with a surface runoff of about 1% of the rain. The forest cells of the sample data, at 2515 m on average, have similar shares:

| | precipitation, mm | evapotranspiration, mm | water yield, mm | quickflow / precipitation |
|---|---|---|---|---|
| Lagan, measured | 2050 | about 1300 (63%) | 721 (35%) | about 1% |
| forest cells of the sample data | 1719 | 1136 (66%) | 584 (34%) | 2.6% |
| whole basin of the sample data | 1570 | 1108 (71%) | 462 (29%) | 5.7% |

The share of the water yield of the forest is the measured one. The quickflow of the curve numbers is two to three times the measured surface runoff of the forest, and it is higher on the agriculture (15% of the precipitation), on the unpaved roads (24%) and on the urban areas (19%), where the crop coefficient of 0.4 also gives little evapotranspiration and a water yield of about 65% of the precipitation. The basin has more coffee and tea in its lower, drier part, with a water yield of 14% and 26% of the precipitation.

The mean discharge of 1.56 m³/s at the outlet is not compared with measures here: the sample data don't come with the discharges of the Gura. Before using the absolute values of a basin, compare them with the records of a gauge of the basin.

**References**

- BirdLife International. Important Bird Area factsheet: Aberdare mountains, Kenya. https://datazone.birdlife.org/site/factsheet/aberdare-mountains-iba-kenya
- Blackie, J. R. (1972). Hydrological effects of a change in land use from rain forest to tea plantation in Kenya. In Symposium on the Results of Research on Representative and Experimental Basins, Wellington, IAHS Publication 97, 312-329. https://iahs.info/uploads/dms/3652.312-329-97-Blackie-opt.pdf
- Musyimi, P. K., Székely, B., Weidinger, T. (2022). Reference evapotranspiration estimation and influence of coffee on real evapotranspiration in humid climatic regions of Kenya. EGU General Assembly 2022, EGU22-388. https://meetingorganizer.copernicus.org/EGU22/EGU22-388.html

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
