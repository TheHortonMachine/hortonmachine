# Carver

Carves lines and polygons into an elevation model: the cells they cover are lowered by a depth, given for each feature in a field or as a single value. It is the opposite of [BobTheBuilder](bobthebuilder.md), and it is used to:

- burn the known rivers into the terrain, so that the flow directions follow them also where the elevation model misses them, as in flat valley floors, under bridges or along embankments;
- model excavations, as a channel, a basin or a quarry.

The depth is subtracted from the elevation of each cell, so the bottom of a carved channel keeps the slope of the terrain, a few meters lower: it is not leveled. Where a polygon and a line overlap, the depth of the polygon is used.

## Details

```{include} ../generated/Carver_details.md
```

## Parameters

```{include} ../generated/Carver.md
```

## Example

The example runs on the sample elevation model of the manual, `dtm_flanginec.tif` in `docs/manual/modules/maps/data`, each module on the outputs of the previous ones. It burns the network of the basin, extracted by [NetworkAttributesBuilder](../network/networkattributesbuilder.md), 3 m into the terrain:

```{literalinclude} ../examples/hm/carver.groovy
:language: groovy
```

:::{figure} ../../images/modules/Carver_dtm_flanginec-dtm_carved.png
:alt: The difference between the original and the carved elevation model: the cells of the channels of the basin, lowered by 3 m.

The difference between the original and the carved elevation model: the cells of the channels of the basin, lowered by 3 m.
:::
