# Netshape2Flow

Rasterizes a river network drawn as lines into the drainage directions of its cells, on the grid of a given raster. It brings a known network, digitized from maps or surveys, into the raster modules: the flow map can force the drainage directions of [DrainDir](../geomorphology/draindir.md) along the real channels, where the elevation model alone would route the water elsewhere, as on flat valley floors or across embankments.

Besides the flow map, it writes the rasterized network, with the id of each reach, and the points where the lines need to be corrected.

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones. Here the network of [NetworkAttributesBuilder](networkattributesbuilder.md), extracted from the same elevation model, stands in for a digitized one:

```{literalinclude} ../examples/hm/netshape2flow.groovy
:language: groovy
```

:::{figure} ../../images/modules/Netshape2Flow_flownet.png
:alt: The drainage directions of the network lines of NetworkAttributesBuilder, rasterized on the grid of the elevation model.

The drainage directions of the network lines of NetworkAttributesBuilder, rasterized on the grid of the elevation model.
:::

## Reference

```{include} ../generated/Netshape2Flow.md
```
