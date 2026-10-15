# SaintGeo

SaintGeo is a 1D hydraulic model of a river: it solves the equations of de Saint-Venant along the sections of [RiverSectionsExtractor](riversectionsextractor.md), and gives the water level and the discharge of each section, in time, for a discharge entering at the head of the river.

The simulation starts from the steady flow of the first discharge, found in each section with the formula of Gauckler-Strickler. Then each value of the head discharge is a step of the model: a semi-implicit finite difference scheme gives the new water levels of all the sections, and from them the discharges between the sections. Downstream, the flow passes through the critical depth or, when a downstream level is given, takes that level. Lateral inflows and outflows, and the inflows of confluences, can be added at given sections.

The length of the steps is set by the model, for the stability of the scheme, from the distance between the sections and the velocity of the water: the steps are not of a fixed time, and the timestamps of the discharge file only give the order of the values.

:::{warning}
The model is meant for the subcritical flow of lowland rivers. In steep streams, where the flow is supercritical, the scheme diverges, or gives velocities without physical meaning: the mountain stream of the sample area is far too steep for it. The model stops with an error when the levels are no longer numbers, but check that the results make sense. The module is experimental.
:::

## Details

```{include} ../generated/SaintGeo_details.md
```

## Parameters

```{include} ../generated/SaintGeo.md
```

## Example

The example builds a lowland river on the grid of the sample area, and follows all the steps from the elevation model to the flood: the network, the main channel, its sections and the simulation.

The river is made with [Mapcalc](../raster-processing/mapcalc.md): a meandering channel 3 m deep and 30 m wide, sloping by 0.2%, in a floodplain 500 m wide. Its network is extracted as for any elevation model, with [Pitfiller](../dem-manipulation/pitfiller.md), [FlowDirections](../geomorphology/flowdirections.md), [DrainDir](../geomorphology/draindir.md), [Markoutlets](../dem-manipulation/markoutlets.md), [ExtractNetwork](../network/extractnetwork.md) and [NetworkAttributesBuilder](../network/networkattributesbuilder.md):

```{literalinclude} ../examples/hm/saintgeo_river.groovy
:language: groovy
```

:::{figure} ../../images/modules/SaintGeo_river.png
:alt: The synthetic river: the meandering channel in its floodplain, between the sides of the valley.

The synthetic river: the meandering channel in its floodplain, between the sides of the valley.
:::

:::{figure} ../../images/modules/SaintGeo_river_net.png
:alt: Its network, along the channel: with a high threshold, only the main channel.

Its network, along the channel: with a high threshold, only the main channel.
:::

The main channel, the reaches of Hack order 1, is smoothed and simplified, so that its sections cross the valley, and the sections are extracted with [RiverSectionsExtractor](riversectionsextractor.md), and written also for HEC-RAS with [HecrasInputBuilder](hecrasinputbuilder.md):

```{literalinclude} ../examples/hm/saintgeo_sections.groovy
:language: groovy
```

The network has also a short reach along the southern border, where the cells of the last row drain along it: RiverSectionsExtractor warns that the lines do not form a single line, and uses the longest one.

:::{figure} ../../images/modules/SaintGeo_river_sections.png
:alt: The sections, in red, every 100 m along the smoothed channel, in blue, across the floodplain and up the sides of the valley.

The sections, in red, every 100 m along the smoothed channel, in blue, across the floodplain and up the sides of the valley.
:::

The simulation sends a flood wave of 400 m³/s down the river, over a base flow of 50 m³/s. The discharge is written as an OMS time series, the format of the input files of the module, and two tables of the results are extracted from the output GeoPackage for the charts:

```{literalinclude} ../examples/hm/saintgeo.groovy
:language: groovy
```

:::{figure} ../../images/modules/SaintGeo_flood_levels.png
:alt: Along the river: the bottom of the sections, the level of the steady flow of 50 m³/s at the start, and the highest level of the flood.

Along the river: the bottom of the sections, the level of the steady flow of 50 m³/s at the start, and the highest level of the flood, which rises over the edge of the channel, 3 m over its bottom, into the floodplain. At the last section the level drops to the critical depth.
:::

:::{figure} ../../images/modules/SaintGeo_flood_discharge.png
:alt: The discharge at the head and at the end of the river, at each step of the model: the wave arrives later and lower, spread by the storage of the floodplain.

The discharge at the head and at the end of the river, at each step of the model: the wave arrives later and lower, spread by the storage of the floodplain. The steps are not of a fixed time, so the shape of the waves is distorted where the velocities change.
:::
