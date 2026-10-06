# SplitSubbasins

Splits a basin into the subbasins of its streams, up to a Hack order: with order 2, the main stream (order 1) and the streams flowing into it (order 2) get each their subbasin, numbered, and so does their network. The streams of higher order are part of the subbasin of the stream they flow into.

The Hack orders come from [NetworkAttributesBuilder](../network/networkattributesbuilder.md).

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones:

```{literalinclude} ../examples/hm/splitsubbasins.groovy
:language: groovy
```

:::{figure} ../../images/modules/SplitSubbasins_split_subbasins.png
:alt: The subbasins of the streams up to Hack order 2 of the basin of ExtractBasin.

The subbasins of the streams up to Hack order 2 of the basin of ExtractBasin.
:::

## Reference

```{include} ../generated/SplitSubbasins.md
```
