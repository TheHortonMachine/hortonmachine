# H2cd

The hillslope to channel distance is the length of the path the water follows on a hillslope before reaching the network. With the velocity of the water on the hillslopes, it gives the time the water of each cell needs to reach a channel, and its distribution over a basin is one of the components of the hydrological response, with the distance along the network.

The distance is measured along the drainage directions, in number of cells or in meters, also in 3D with the elevation. The network cells get 0, as the cells whose path leaves the map without reaching the network.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/h2cd.groovy
:language: groovy
```

:::{figure} ../../images/modules/H2cd_h2cd.png
:alt: The hillslope to channel distance of the sample area, in meters, measured in 3D, with the network of ExtractNetwork.

The hillslope to channel distance of the sample area, in meters, measured in 3D, with the network of ExtractNetwork.
:::

## Reference

```{include} ../generated/H2cd.md
```
