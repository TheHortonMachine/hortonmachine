# Peakflow

Peakflow is a semi-distributed rainfall-runoff model for single flood events. It computes the hydrograph at the outlet of a basin with the geomorphological instantaneous unit hydrograph (GIUH): the response of the basin follows from the distribution of the distances of its cells from the outlet, the width function.

## How it works

The water reaches the outlet faster in the channels than on the hillslopes. The rescaled distance of each cell, from the [RescaledDistance](../basin/rescaleddistance.md) module, measures the path to the outlet as if it were all in the channels, multiplying the hillslope part by the ratio r between the channel and the hillslope velocities. The width function, the number of cells at each rescaled distance, divided by the channel velocity, gives the time each part of the basin needs to reach the outlet.

The runoff is formed as in the Dunne mechanism: the surface runoff comes only from the saturated areas, while the rest of the basin contributes only through the slower subsurface flow. Peakflow uses two width functions:

- the **surface** one, on the saturated areas, with a low ratio r (5 to 20);
- the optional **subsurface** one, on the other areas, with a high ratio r (50 to 200).

The saturated areas are the cells with the highest topographic index, as many as the saturated percentage of the basin (usually 40 to 60% in floods, more in small basins), or are given as a map.

Peakflow runs in two modes:

- **statistical rainfall**, with the parameters a and n of the rainfall depth-duration-frequency curve h = a·tⁿ for a return period: the rainfall intensity is constant during the event, and Peakflow finds its duration that gives the largest discharge. The critical duration and the peak discharge are reported in the log;
- **measured rainfall**, used when a and n are not given: a CSV file with the rainfall intensity of an event, in mm/h, at a constant time step, gives the hydrograph of the event.

The output is a CSV file with the discharge at the outlet, in m³/s. In the statistical mode, the times of the hydrograph start from the time of the run.

## Preparing the inputs

All the maps must cover only the basin, cut with [CutOut](../raster-processing/cutout.md) on the basin mask, and not computed again on a cut elevation model, which could change the drainage directions:

1. [Pitfiller](../dem-manipulation/pitfiller.md), [FlowDirections](../geomorphology/flowdirections.md) and [DrainDir](../geomorphology/draindir.md) for the drainage directions and the contributing areas, [Slope](../geomorphology/slope.md) for the slope along them, and [ExtractNetwork](../network/extractnetwork.md) for the network;
2. [ExtractBasin](../dem-manipulation/extractbasin.md) for the basin of the outlet;
3. [TopIndex](../basin/topindex.md) for the topographic index. It is not defined where the slope is 0: these cells inside the basin are usually set to the highest value of the map, with the [Map Calculator](../../apps/mapcalc.md);
4. [RescaledDistance](../basin/rescaleddistance.md) twice, with a low and a high ratio r, for the surface and the subsurface rescaled distances.

```{include} ../generated/Peakflow.md
```
