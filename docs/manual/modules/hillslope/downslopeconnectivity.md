# DownSlopeConnectivity

Calculates the downslope component of the index of sediment connectivity of Cavalli et al. (2013): how easily the sediment detached in a cell can reach the network. Following the flow path of the cell down to the first channel cell, it sums the length `d` of each step divided by the weight `W` and the slope `S` of the cell it leaves: `D = Σ d / (W · S)`.

Long, gentle and rough paths give high values, poorly connected cells; short and steep paths low values. The cells of the network have 0.

The weight is the impedance to the sediment flow, from 0 to 1. Cavalli et al. derive it from the roughness of the surface, as 1 - roughness / maximum roughness; any other map of the land cover can be used. Without a map, a constant weight gives a connectivity that depends only on the flow lengths and the slopes. The slopes below 0.005 are raised to 0.005, as in the paper, to avoid infinite values in the flat areas.

The full index also has an upslope component, the mean slope and weight of the area that drains into the cell, which this module does not compute.

## Parameters

```{include} ../generated/DownSlopeConnectivity.md
```

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/downslopeconnectivity.groovy
:language: groovy
```

:::{figure} ../../images/modules/DownSlopeConnectivity_connectivity.png
:alt: The downslope connectivity of the sample area, with a constant weight: low values near the channels and on the steep slopes, high values on the gentle terrain far from the network.

The downslope connectivity of the sample area, with a constant weight: low values near the channels and on the steep slopes, high values on the gentle terrain far from the network. The cells whose path leaves the map before reaching the network have no value.
:::
