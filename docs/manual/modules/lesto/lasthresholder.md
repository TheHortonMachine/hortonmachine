# LasThresholder

LasThresholder keeps the points of a LAS file with an intensity, or an elevation, between a lower and an upper threshold, and writes them to a new LAS file. Either threshold can be left out, to keep everything over or under the other.

## Details

```{include} ../generated/LasThresholder_details.md
```

## Parameters

```{include} ../generated/LasThresholder.md
```

## Example

The points of the sample survey brighter than the peak of its [histogram](lashistogram.md), with an intensity of 150 or more, converted to a vector by [LasConverter](../vector-processing/lasconverter.md) to see them on a map:

```{literalinclude} ../examples/hm/lasthresholder.groovy
:language: groovy
```

:::{figure} ../../images/modules/LasThresholder_las_bright_intensity.png
:alt: The points with an intensity of 150 or more, colored by their intensity, over the hillshade of the elevation model: they lie mostly in the gaps between the crowns and on the road at the north-west corner, a few very bright points are scattered among the trees.

The points with an intensity of 150 or more, colored by their intensity, over the hillshade of the elevation model: they lie mostly in the gaps between the crowns and on the road at the north-west corner, a few very bright points are scattered among the trees.
:::
