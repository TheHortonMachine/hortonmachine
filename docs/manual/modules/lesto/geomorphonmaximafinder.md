# GeomorphonMaximaFinder

GeomorphonMaximaFinder finds the tops of the trees as the peaks of the surface model, classified into landforms by [Geomorphon](../geomorphology/geomorphon.md): a crown is a small hill, and its top is the highest cell of its peak. The tops keep their height over the ground. Unlike [RasterMaximaFinder](rastermaximafinder.md), it has no threshold on the height: the bumps of the ground and of the shrubs are peaks too.

## Details

```{include} ../generated/GeomorphonMaximaFinder_details.md
```

## Parameters

```{include} ../generated/GeomorphonMaximaFinder.md
```

## Example

The tops of the surface of the highest points of [LasOnRasterMapper](lasonrastermapper.md), with geomorphons of 3 m, discarding the tops whose heights span more than 5 m in the cells around them:

```{literalinclude} ../examples/hm/geomorphonmaximafinder.groovy
:language: groovy
```

:::{figure} ../../images/modules/GeomorphonMaximaFinder_tops_geomorphon.png
:alt: The 108 tops, red dots, over the canopy height model in green: most crowns have a top, the taller ones near their center, and some tops are on the open ground between the trees.

The 108 tops, red dots, over the canopy height model in green: most crowns have a top, the taller ones near their center, and some tops are on the open ground between the trees.
:::

With the default threshold of 1 m on the heights around a top, only 3 tops are left on this survey of 0.5 m cells.
