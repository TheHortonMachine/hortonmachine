# IntensityClassifierFlood

IntensityClassifierFlood classifies the intensity of a flood in three classes, 1 low, 2 medium and 3 high, from the maps of its water depth h and velocity v, as given by a 2D hydraulic model. The intensity of a cell is high where h or the specific discharge v·h is over its upper threshold, medium where one of them is over its lower threshold, low elsewhere. The default thresholds are 0.5 and 1 m for the depth, 0.5 and 1 m²/s for the specific discharge.

The intensity maps of the floods of 30, 100 and 200 years of return period are the input of [HazardClassifier](hazardclassifier.md).

Cells with a depth of 0 are classified as low intensity: set the dry cells to novalue in the maps of depth and velocity, to keep them out of the map.

## Details

```{include} ../generated/IntensityClassifierFlood_details.md
```

## Parameters

```{include} ../generated/IntensityClassifierFlood.md
```

## Example

The example makes up the three floods on the synthetic river of the example of [SaintGeo](saintgeo.md), `river.tif`: water surfaces parallel to the bottom of the valley, 5, 15 and 25 cm over the edge of the floodplain, and the velocity of a uniform flow of each depth. A real study would take the depth and the velocity from a hydraulic model:

```{literalinclude} ../examples/hm/intensityclassifierflood.groovy
:language: groovy
```

:::{figure} ../../images/modules/IntensityClassifierFlood_intensity_tr200.png
:alt: The intensity of the flood of 200 years over the river: high in the channel, deep and fast, a thin band of medium at its edges, low over the shallow water of the floodplain.

The intensity of the flood of 200 years over the river: high in the channel, deep and fast, a thin band of medium at its edges, low over the shallow water of the floodplain.
:::
