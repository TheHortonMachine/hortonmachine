# Labeler

Gives a number to every group of valid cells that touch each other, the connected components of the raster, also called patches or regions. The value of the cells doesn't matter, only if they are valid or novalue: a raster of classes is first cut to the class of interest, for example with the inverse switch of [RasterNull](rasternull.md).

The labels turn a mask into separate objects, as the single landslides, lakes or tree crowns of a map, that can then be counted, measured with [RasterSummary](rastersummary.md) or vectorized.

## Details

```{include} ../generated/Labeler_details.md
```

## Parameters

```{include} ../generated/Labeler.md
```

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones. It labels the patches of the middle elevation belt cut by [RasterNull](rasternull.md):

```{literalinclude} ../examples/hm/labeler.groovy
:language: groovy
```

:::{figure} ../../images/modules/Labeler_belt_middle_patches.png
:alt: The separate patches of the middle elevation belt, each with its own number and color.

The separate patches of the middle elevation belt, each with its own number and color.
:::
