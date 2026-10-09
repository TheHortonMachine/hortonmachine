# LasOnRasterMapper

LasOnRasterMapper creates a raster with the elevation of the highest, or lowest, point of each cell, on the grid of an elevation model or at another resolution. With the highest points it is the quickest surface model, of the canopy and the roofs, without interpolation: the cells without points stay empty.

## Example

The highest point of each cell of 0.5 m of the sample plot:

```{literalinclude} ../examples/hm/lasonrastermapper.groovy
:language: groovy
```

:::{figure} ../../images/modules/LasOnRasterMapper_dsm_max-dtm_777.png
:alt: The surface of the highest points minus the elevation model: the height of the crowns, up to about 20 m, with single empty cells among them where no point fell.

The surface of the highest points minus the elevation model: the height of the crowns, up to about 20 m, with single empty cells among them where no point fell.
:::

## Reference

```{include} ../generated/LasOnRasterMapper.md
```
