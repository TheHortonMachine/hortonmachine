# NetworkAttributesBuilder

NetworkAttributesBuilder converts the raster network into a vector of its links, the stretches of channel between two confluences, with their attributes:

- the **Hack order**: 1 for the main stream, from the outlet to the source draining the largest area, 2 for the streams flowing into it, and so on;
- the **Strahler order** (Strahler, 1957): 1 for the first links, increasing by one where two links of the same order meet;
- the **Pfafstetter code** (Verdin and Verdin, 1999), which numbers the basins and the streams hierarchically;
- with the elevation, the elevation of the start and end of each link.

It can also write the map of the Hack orders, the input of [SplitSubbasins](../dem-manipulation/splitsubbasins.md). The drainage directions must have the outlet marked with the value 10, for example with [MarkOutlets](../dem-manipulation/markoutlets.md).

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/networkattributesbuilder.groovy
:language: groovy
```

:::{figure} ../../images/modules/NetworkAttributesBuilder_net_attributes_hack.png
:alt: The vector of the network of the basin of ExtractBasin, colored by the Hack order of the links: 1 is the main stream.

The vector of the network of the basin of ExtractBasin, colored by the Hack order of the links: 1 is the main stream.
:::

:::{figure} ../../images/modules/NetworkAttributesBuilder_net_attributes_strahler.png
:alt: The same network colored by the Strahler order of the links: the order grows by one where two links of the same order meet.

The same network colored by the Strahler order of the links: the order grows by one where two links of the same order meet.
:::

## Reference

```{include} ../generated/NetworkAttributesBuilder.md
```
